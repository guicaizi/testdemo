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
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * 把 {@link GlassRenderer}（渲染）和 {@link GlassBackdrop}（背景）接到 View 生命周期上的胶水层。
 *
 * <p>对应上游的 {@code drawBackdrop} Modifier —— {@code DrawBackdropModifier.kt} 那 388 行里
 * 绝大部分是 Compose 节点机制（{@code ModifierNodeElement} / {@code equals} / {@code inspectableProperties}），
 * 真正有用的只有「定位 → 取背景 → 画玻璃 → 交给内容」这条骨架。
 *
 * <p>三种 View（{@link GlassFrameLayout} / {@link GlassTextView} / {@link GlassButton}）
 * 共用这一个控制器，避免重复。
 */
public final class GlassController implements GlassBackdrop.OnBackdropChanged {

    private final GlassRenderer mRenderer = new GlassRenderer();
    private final Rect mSelfInRoot = new Rect();
    private final Rect mNeeded = new Rect();
    private final Rect mAppliedSelf = new Rect();
    private final int[] mLocation = new int[2];
    private final int[] mRootLocation = new int[2];
    private final Paint mCopyPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final Runnable mCaptureTask = new Runnable() {
        @Override
        public void run() {
            capture();
        }
    };

    private View mHost;
    private GlassBackdrop mBackdrop;
    private GlassBackdrop mListenerBackdrop;
    private Bitmap mOwnBitmap;
    private int mPadInt;
    private int mAppliedCaptureCount = -1;
    private boolean mForceApply = true;
    private boolean mReleased;

    /** 绑定宿主 View。构造时调用一次即可。 */
    public void attach(View host) {
        mHost = host;
    }

    public GlassRenderer renderer() {
        return mRenderer;
    }

    public GlassStyle getStyle() {
        return mRenderer.getStyle();
    }

    /** 换样式。会重新登记扩边并在下一帧重新抓取背景。 */
    public void setStyle(GlassStyle style) {
        if (style == null) {
            return;
        }
        mRenderer.setStyle(style);
        mForceApply = true;
        final View host = mHost;
        if (host != null) {
            host.requestLayout();
            host.invalidate();
        }
    }

    /** 外部数据变化（壁纸、图片）后强制重抓背景。 */
    public void invalidateBackdrop() {
        if (mBackdrop != null) {
            mBackdrop.invalidate();
        }
        final View host = mHost;
        if (host != null) {
            host.removeCallbacks(mCaptureTask);
            host.post(mCaptureTask);
        }
    }

    /** 转发给渲染器的按压高光状态。 */
    public void setPress(boolean active, float progress, float x, float y) {
        mRenderer.setPress(active, progress, x, y);
    }

    /**
     * 在宿主 View 的 {@code onLayout} 末尾调用。
     */
    public void onLayout() {
        final View host = mHost;
        if (host == null || mReleased) {
            return;
        }
        final int width = host.getWidth();
        final int height = host.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        final View root = host.getRootView();
        if (root == null || root == host) {
            // 还没附着到窗口，等下一次 layout 再说
            return;
        }
        host.getLocationInWindow(mLocation);
        root.getLocationInWindow(mRootLocation);
        final int left = mLocation[0] - mRootLocation[0];
        final int top = mLocation[1] - mRootLocation[1];
        mSelfInRoot.set(left, top, left + width, top + height);

        final float padding = mRenderer.getStyle().paddingPx(Math.min(width, height));
        mPadInt = (int) Math.ceil(padding);
        mNeeded.set(left - mPadInt, top - mPadInt,
                left + width + mPadInt, top + height + mPadInt);

        mBackdrop = GlassBackdrop.of(host);
        mBackdrop.request(mNeeded);
        if (mListenerBackdrop != mBackdrop) {
            if (mListenerBackdrop != null) {
                mListenerBackdrop.removeListener(this);
            }
            mBackdrop.addListener(this);
            mListenerBackdrop = mBackdrop;
        }

        host.removeCallbacks(mCaptureTask);
        host.post(mCaptureTask);
    }

    /**
     * 背景位图被换了（尺寸变化或别的玻璃元素扩大了抓取范围）时重绑一次。
     * 通知来自 {@link GlassBackdrop}，可能是任意一个玻璃 View 触发的抓取。
     */
    @Override
    public void onBackdropChanged() {
        final View host = mHost;
        if (host == null || mReleased) {
            return;
        }
        host.removeCallbacks(mCaptureTask);
        host.post(mCaptureTask);
    }

    /**
     * 在宿主 View 的 {@code onDraw} 里调用。抓取中直接跳过，避免把玻璃自身折进背景。
     */
    public void draw(Canvas canvas, int width, int height) {
        if (GlassBackdrop.isCapturing() || mReleased) {
            return;
        }
        mRenderer.draw(canvas, width, height);
    }

    /** 在宿主 View 的 {@code onAttachedToWindow} 里调用。视图被重新挂载时恢复工作。 */
    public void onAttached() {
        mReleased = false;
        mForceApply = true;
        mAppliedCaptureCount = -1;
        mAppliedSelf.setEmpty();
    }

    /** 在宿主 View 的 {@code onDetachedFromWindow} 里调用。 */
    public void onDetached() {
        final View host = mHost;
        if (host != null) {
            host.removeCallbacks(mCaptureTask);
        }
        if (mListenerBackdrop != null) {
            mListenerBackdrop.removeListener(this);
            mListenerBackdrop = null;
        }
        recycleOwnBitmap();
        mRenderer.release();
        mReleased = true;
    }

    // ------------------------------------------------------------------

    private void capture() {
        final View host = mHost;
        if (host == null || mReleased) {
            return;
        }
        final GlassBackdrop backdrop = mBackdrop;
        if (backdrop == null) {
            return;
        }
        backdrop.ensureCaptured(host.getRootView());
        final Bitmap shared = backdrop.getBitmap();
        if (shared == null || shared.isRecycled()) {
            return;
        }
        final Rect region = backdrop.getRegion();
        if (region.isEmpty()) {
            return;
        }

        final Rect mine = new Rect(
                mSelfInRoot.left - mPadInt,
                mSelfInRoot.top - mPadInt,
                mSelfInRoot.right + mPadInt,
                mSelfInRoot.bottom + mPadInt);
        if (!Rect.intersects(region, mine)) {
            return;
        }

        final GlassStyle style = mRenderer.getStyle();
        final boolean needsOwnCopy = style.blurRadiusPx() > 0f || style.hasColorAdjustment();

        if (needsOwnCopy) {
            // 模糊和调色是有状态的操作，不能作用在共享位图上（会影响别的玻璃元素），
            // 所以给自己切一份带扩边的副本再处理。切一份很小的图，成本可忽略。
            final int width = mine.width();
            final int height = mine.height();
            if (width <= 0 || height <= 0) {
                return;
            }
            if (mOwnBitmap == null
                    || mOwnBitmap.getWidth() != width
                    || mOwnBitmap.getHeight() != height) {
                recycleOwnBitmap();
                try {
                    mOwnBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    mOwnBitmap = null;
                    return;
                }
            }
            final Canvas canvas = new Canvas(mOwnBitmap);
            canvas.drawBitmap(shared,
                    region.left - mine.left,
                    region.top - mine.top,
                    mCopyPaint);
            GlassImageFilter.boxBlur(mOwnBitmap, style.blurRadiusPx());
            if (style.hasColorAdjustment()) {
                GlassImageFilter.colorControls(mOwnBitmap,
                        style.brightness(), style.contrast(), style.saturation());
            }
            mRenderer.setBackdrop(mOwnBitmap, -mPadInt, -mPadInt);
        } else {
            recycleOwnBitmap();
            // 直接用共享位图：把位图左上角映射到画布局部坐标
            mRenderer.setBackdrop(shared,
                    region.left - mSelfInRoot.left,
                    region.top - mSelfInRoot.top);
        }

        final int captureCount = backdrop.getCaptureCount();
        final boolean changed = mForceApply
                || captureCount != mAppliedCaptureCount
                || !mSelfInRoot.equals(mAppliedSelf);
        if (changed) {
            mAppliedCaptureCount = captureCount;
            mAppliedSelf.set(mSelfInRoot);
            mForceApply = false;
            host.invalidate();
        }
    }

    private void recycleOwnBitmap() {
        if (mOwnBitmap != null && !mOwnBitmap.isRecycled()) {
            mOwnBitmap.recycle();
        }
        mOwnBitmap = null;
    }
}
