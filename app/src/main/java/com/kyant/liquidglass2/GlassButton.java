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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * 按钮版液态玻璃。默认是胶囊形 + 一层背景模糊 + vibrancy，
 * 并开启按下跟手高光 —— 这三样合起来最接近 AndroidLiquidGlass 里
 * {@code LiquidButton} 的配方。
 *
 * <pre>{@code
 * GlassButton button = findViewById(R.id.get_started);
 * button.setOnClickListener(v -> { ... });
 * }</pre>
 *
 * <p>按钮通常压在轮廓光、图标或壁纸上，是折射最容易被看出来的地方。
 * 如果发现效果不明显，先确认按钮背后确实有明暗变化的内容。
 */
public class GlassButton extends Button {

    private static GlassStyle sDefaultStyle = GlassStyle.builder()
            .capsule()
            .blur(4f)
            .vibrancy()
            .build();

    private final GlassController mController = new GlassController();
    private GlassPressHighlight mPressHighlight;

    /**
     * 全局默认样式。请在创建任何 {@link GlassButton} 之前调用
     * （比如 {@code Application#onCreate}）。
     */
    public static void setDefaultStyle(GlassStyle style) {
        if (style != null) {
            sDefaultStyle = style;
        }
    }

    public static GlassStyle getDefaultStyle() {
        return sDefaultStyle;
    }

    public GlassButton(Context context) {
        this(context, null);
    }

    public GlassButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.buttonStyle);
    }

    public GlassButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mController.attach(this);
        mController.setStyle(sDefaultStyle);
        setWillNotDraw(false);
        // 玻璃本身就是背景；留着框架默认的按钮背景会盖住玻璃、也会被折进抓取结果
        setBackground(null);
        setInteractiveHighlight(true);
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
        // 抓背景时必须整个跳过，否则文字会被折进玻璃里变成重影
        if (GlassBackdrop.isCapturing()) {
            return;
        }
        mController.draw(canvas, getWidth(), getHeight());
        super.onDraw(canvas);
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
