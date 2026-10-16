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

package com.kyant.liquidglass;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * 把任意内容装进液态玻璃里的容器。
 *
 * <p>玻璃画在内容下面，子 View 照常工作。默认样式是 {@link GlassStyle#spec()}。
 *
 * <pre>{@code
 * GlassFrameLayout card = new GlassFrameLayout(context);
 * card.setGlassStyle(GlassStyle.builder()
 *         .uniformCorner(24f)
 *         .blur(6f)
 *         .vibrancy()
 *         .build());
 * card.addView(titleView);
 * }</pre>
 *
 * <p>注意：玻璃必须叠在「有内容可折」的背景上。纯色背景上折射和色散恒等于输入，
 * 只有边缘高光会显示。
 */
public class GlassFrameLayout extends FrameLayout {

    private final GlassController mController = new GlassController();
    private GlassPressHighlight mPressHighlight;

    public GlassFrameLayout(Context context) {
        this(context, null);
    }

    public GlassFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mController.attach(this);
        // ViewGroup 默认 WILL_NOT_DRAW，不关掉 onDraw 根本不会被调用
        setWillNotDraw(false);
        // 玻璃本身就是背景，原有的背景会在抓取时被折进去形成重影
        setBackground(null);
    }

    public GlassController glassController() {
        return mController;
    }

    public GlassStyle getGlassStyle() {
        return mController.getStyle();
    }

    public void setGlassStyle(GlassStyle style) {
        mController.setStyle(style);
    }

    /** 外部数据变化后强制重抓背景。 */
    public void refreshGlassBackdrop() {
        mController.invalidateBackdrop();
    }

    /** 打开按下跟手高光。宿主会自动变 clickable，否则收不到后续的 MOVE / UP。 */
    public void setInteractiveHighlight(boolean enabled) {
        if (enabled) {
            if (mPressHighlight == null) {
                mPressHighlight = new GlassPressHighlight(this, mController);
            }
            mPressHighlight.attach();
        } else if (mPressHighlight != null) {
            mPressHighlight.detach();
            mPressHighlight = null;
        }
    }

    public GlassPressHighlight getPressHighlight() {
        return mPressHighlight;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mController.onAttached();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mController.onLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mController.draw(canvas, getWidth(), getHeight());
        super.onDraw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // 抓背景时要连子 View 一起跳过，否则会把玻璃里的内容折进去
        if (GlassBackdrop.isCapturing()) {
            return;
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPressHighlight != null) {
            mPressHighlight.detach();
            mPressHighlight = null;
        }
        mController.onDetached();
    }
}
