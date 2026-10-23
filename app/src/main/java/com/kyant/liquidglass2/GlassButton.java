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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 按钮版液态玻璃。
 *
 * <p>直接继承 {@link View}（而不是 Button/TextView），因为 TextView 基类在硬件加速 +
 * 软件画布抓背景的组合下会干扰自定义 onDraw 的硬件显示列表，导致玻璃本体不可见。
 * 文字自己用 {@link TextPaint} 居中绘制。
 */
public class GlassButton extends View {

    private static GlassStyle sDefaultStyle = GlassStyle.builder()
            .capsule()
            .blur(4f)
            .vibrancy()
            .build();

    private final GlassController mController = new GlassController();
    private GlassPressHighlight mPressHighlight;

    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private CharSequence mText = "";

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
        this(context, attrs, 0);
    }

    public GlassButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 从 XML 读 android:text
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.text});
            CharSequence t = a.getText(0);
            if (t != null) {
                mText = t;
            }
            a.recycle();
        }
        mController.attach(this);
        mController.setStyle(sDefaultStyle);
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setBackground(null);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(18f * context.getResources().getDisplayMetrics().density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
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

    public void setText(CharSequence text) {
        mText = text == null ? "" : text;
        invalidate();
    }

    public CharSequence getText() {
        return mText;
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    public void setTextSize(float sp) {
        mTextPaint.setTextSize(sp * getResources().getDisplayMetrics().density);
        invalidate();
    }

    /** 外部数据或位置变化后强制重抓背景（重算位置 + 标记脏）。 */
    public void refreshGlassBackdrop() {
        mController.onLayout();
        mController.invalidateBackdrop();
        invalidate();
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
        // 居中画文字
        if (mText != null && mText.length() > 0) {
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            float textY = (getHeight() - fm.ascent - fm.descent) / 2f;
            canvas.drawText(mText.toString(), getWidth() / 2f, textY, mTextPaint);
        }
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
