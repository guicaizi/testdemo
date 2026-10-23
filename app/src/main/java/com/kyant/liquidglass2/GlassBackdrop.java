/*
   Copyright 2025 Kyant

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.kyant.liquidglass2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * 背景来源。负责「把玻璃背后那一片像素抓下来」，并按窗口共享。
 *
 * <h3>为什么不用截屏</h3>
 * Android 上做液态玻璃最难的一步就是拿到背后的像素。常见方案
 * （{@code PixelCopy}、{@code RenderNode} 录制整棵树）要么异步、要么容易踩死循环。
 * 上游 Compose 版的做法是让背景内容自己录一份 GraphicsLayer；
 * 这里退一步用软件 {@code Canvas} 把 root 画进位图——静态页面上够用，
 * 而且不依赖任何 Compose 机制。
 *
 * <h3>三个关键设计</h3>
 * <ol>
 *   <li><b>同窗口共享一张图。</b>按 root view 缓存，同页面多个玻璃元素只抓一次，
 *       不是每个控件各抓一次。抓取区域取所有玻璃元素所需矩形的并集。</li>
 *   <li><b>扩边。</b>抓取范围比玻璃本身大一圈（{@link GlassStyle#paddingPx(float)}），
 *       否则模糊采样会在边缘不够用、折射越界被 CLAMP 兜住，表现为边缘发暗或像素拉伸。</li>
 *   <li><b>脏检测。</b>只有区域变化或显式 {@link #invalidate()} 才重抓。
 *       每帧重抓会触发 HWUI invalidation 死循环。</li>
 * </ol>
 *
 * <h3>使用前提</h3>
 * 抓取时必须跳过玻璃自身，否则会把玻璃里的内容也折进去（重影）。
 * 所有玻璃 View 在 {@code onDraw}/{@code dispatchDraw} 里都要先问一句
 * {@link #isCapturing()}，为 true 就直接 return。
 *
 * <p>本类不是线程安全的，所有方法都必须在主线程调用。
 */
public final class GlassBackdrop {

    /** 以 root view 为 key 的共享表。key 弱引用，窗口销毁后自动回收。 */
    private static final WeakHashMap<View, GlassBackdrop> REGISTRY = new WeakHashMap<View, GlassBackdrop>();

    /** 抓取中标志。玻璃 View 靠它跳过自身绘制。 */
    private static volatile boolean sCapturing;

    /** 背景被重新抓取时的回调。用于让已经拿过旧位图的玻璃 View 重新绑定。 */
    public interface OnBackdropChanged {
        void onBackdropChanged();
    }

    private final ArrayList<WeakReference<OnBackdropChanged>> mListeners = new ArrayList<WeakReference<OnBackdropChanged>>();

    private Bitmap mBitmap;
    private final Rect mRegion = new Rect();
    private final Rect mRequested = new Rect();
    private boolean mHasRequested;
    private boolean mDirty = true;
    private int mCaptureCount;

    private GlassBackdrop() {
    }

    /**
     * 取（或创建）与该 View 所在窗口共享的背景源。
     *
     * @param anyViewInWindow 窗口内任意 View，通常就是玻璃 View 自己
     */
    public static GlassBackdrop of(View anyViewInWindow) {
        if (anyViewInWindow == null) {
            throw new IllegalArgumentException("view == null");
        }
        View root = anyViewInWindow.getRootView();
        if (root == null) {
            root = anyViewInWindow;
        }
        synchronized (REGISTRY) {
            GlassBackdrop backdrop = REGISTRY.get(root);
            if (backdrop == null) {
                backdrop = new GlassBackdrop();
                REGISTRY.put(root, backdrop);
            }
            return backdrop;
        }
    }

    /** 是否正在抓取背景。玻璃 View 的 onDraw / dispatchDraw 必须检查它。 */
    public static boolean isCapturing() {
        return sCapturing;
    }

    /** 登记一个背景变更监听者（弱引用持有）。 */
    public synchronized void addListener(OnBackdropChanged listener) {
        if (listener == null) {
            return;
        }
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            final OnBackdropChanged existing = mListeners.get(i).get();
            if (existing == null) {
                mListeners.remove(i);
            } else if (existing == listener) {
                return;
            }
        }
        mListeners.add(new WeakReference<OnBackdropChanged>(listener));
    }

    public synchronized void removeListener(OnBackdropChanged listener) {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            final OnBackdropChanged existing = mListeners.get(i).get();
            if (existing == null || existing == listener) {
                mListeners.remove(i);
            }
        }
    }

    /**
     * 登记「我需要这块区域（root 坐标系，已含扩边）」。
     *
     * <p>只做几件事：并入并集、必要时置脏。真正的抓取发生在
     * {@link #ensureCaptured(View)}。
     */
    public synchronized void request(Rect neededInRoot) {
        if (neededInRoot == null || neededInRoot.isEmpty()) {
            return;
        }
        final Rect before = new Rect(mRequested);
        if (!mHasRequested) {
            mRequested.set(neededInRoot);
            mHasRequested = true;
        } else {
            mRequested.union(neededInRoot);
        }
        if (mBitmap == null || !mRequested.equals(before)) {
            mDirty = true;
        }
    }

    /** 背景内容变了（换了壁纸、图片加载完、数据刷新），下次调用 {@link #ensureCaptured} 时重抓。 */
    public synchronized void invalidate() {
        mDirty = true;
    }

    /** 彻底丢弃。切换页面、释放窗口时调用。 */
    public synchronized void release() {
        recycle();
        mRegion.setEmpty();
        mRequested.setEmpty();
        mHasRequested = false;
        mDirty = true;
    }

    /**
     * 若需要则抓取。抓取成功后返回 true（调用方应据此 invalidate 自己）。
     *
     * <p><b>不要在 onDraw 里调用。</b>要放在 onLayout 之后 post 出来的任务里，
     * 否则等于在整棵树绘制途中又发起一次整棵树的绘制。
     */
    public synchronized boolean ensureCaptured(View root) {
        if (root == null) {
            return false;
        }
        if (!mDirty) {
            return mBitmap != null;
        }
        if (!mHasRequested || mRequested.isEmpty()) {
            return false;
        }
        final Rect region = new Rect(mRequested);
        final int width = region.width();
        final int height = region.height();
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (mBitmap == null || mBitmap.getWidth() != width || mBitmap.getHeight() != height) {
            // 尺寸变了就换一块新位图。旧的那块不主动 recycle ——
            // 别的玻璃 View 可能还持有它，交给 GC 回收更安全（下面会通知它们重新绑定）。
            mBitmap = null;
            try {
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                mBitmap = null;
                return false;
            }
        }
        boolean ok = false;
        final Canvas canvas = new Canvas(mBitmap);
        canvas.translate(-region.left, -region.top);
        sCapturing = true;
        try {
            root.draw(canvas);
            ok = true;
        } catch (RuntimeException e) {
            ok = false;
        } finally {
            sCapturing = false;
        }
        if (!ok) {
            return false;
        }
        // 软件画布 root.draw 会清掉整棵 View 树的 dirty 标志，导致硬件加速 View
        // （特别是自定义 onDraw 的玻璃控件）认为显示列表无需重建、画面空白。
        // 这里强制整树 invalidate，让下一帧硬件渲染重新构建显示列表。
        root.invalidate();
        mRegion.set(region);
        mDirty = false;
        mCaptureCount++;
        notifyListeners();
        return true;
    }

    /** 抓下来的位图，可能为 null。不要自行 recycle。 */
    public synchronized Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * 位图对应的 root 坐标区域。把位图当作 shader 输入时，
     * 用 {@code region.left - viewInRoot.left} 算出 shader 局部坐标的偏移。
     */
    public synchronized Rect getRegion() {
        return new Rect(mRegion);
    }

    /** 累计抓取次数，调试用。 */
    public synchronized int getCaptureCount() {
        return mCaptureCount;
    }

    private void notifyListeners() {
        if (mListeners.isEmpty()) {
            return;
        }
        final ArrayList<OnBackdropChanged> alive = new ArrayList<OnBackdropChanged>(mListeners.size());
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            final OnBackdropChanged listener = mListeners.get(i).get();
            if (listener == null) {
                mListeners.remove(i);
            } else {
                alive.add(listener);
            }
        }
        for (int i = 0; i < alive.size(); i++) {
            alive.get(i).onBackdropChanged();
        }
    }

    private void recycle() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
    }

    /** 仅供内部测试用：清空共享表。 */
    static synchronized void clearRegistry() {
        REGISTRY.clear();
    }
}
