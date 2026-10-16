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
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 文字版液态玻璃。默认样式就是设计稿规格（{@link GlassStyle#spec()}）：
 * 圆角 8px / 折射 50 / 深度 10 / 色散 45 / 高光 -45° 28% / 填充 15% 白。
 *
 * <p>所以协议行那个 61×26 的 chip 可以纯 XML 落地，不需要写代码：
 *
 * <pre>{@code
 * <com.kyant.liquidglass.GlassTextView
 *     android:id="@+id/agreement_view"
 *     android:layout_width="61px"
 *     android:layout_height="26px"
 *     android:gravity="center"
 *     android:textSize="18px"
 *     android:textColor="#A6FFFFFF"
 *     android:text="@string/privacy_view" />
 * }</pre>
 *
 * <p>换成胶囊形（按钮）时在代码里改一次样式即可：
 *
 * <pre>{@code
 * button.setGlassStyle(GlassStyle.builder()
 *         .capsule()
 *         .blur(2f)
 *         .vibrancy()
 *         .build());
 * button.setInteractiveHighlight(true);
 * }</pre>
 *
 * <p>基类是框架的 {@link TextView} 而不是 {@code AppCompatTextView} ——
 * 这是刻意的：整个工具包因此<b>零第三方依赖</b>，静态库退化成纯 jar，
 * 不触发 AOSP 的资源 / AAR 构建路径（见 Android.mk 的说明）。
 * 设备是 Android 14，AppCompat 那些向后兼容补丁本来也用不上。
 */
public class GlassTextView extends TextView {

    private final GlassController mController = new GlassController();
    private GlassPressHighlight mPressHighlight;
    private Shader mTextShader;

    public GlassTextView(Context context) {
        this(context, null);
    }

    public GlassTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public GlassTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mController.attach(this);
        setWillNotDraw(false);
        // 玻璃本身就是背景；留着原背景会在抓取时被折进去形成重影
        setBackground(null);
    }

    /** 设置文字渐变 shader，每次 onDraw 都会重新应用到 TextPaint（绕过 Layout 缓存） */
    public void setTextShader(Shader shader) {
        mTextShader = shader;
        invalidate();
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

    /** 打开按下跟手高光。 */
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
        // 每次绘制前强制应用文字 shader —— TextView 内部 Layout 会缓存 Paint 状态，
        // 外部 setPaint().setShader() 在 Layout 创建后不生效，必须在 onDraw 里重设
        if (mTextShader != null) {
            getPaint().setShader(mTextShader);
        }
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
