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

import android.graphics.Color;

/**
 * 液态玻璃的参数载体。对应 AndroidLiquidGlass 的 {@code BackdropEffectScope} 里可变的那部分，
 * 外加设计稿面板的映射曲线。
 *
 * <h3>设计面板 → 本类字段</h3>
 * <table border="1">
 *   <tr><th>设计面板</th><th>字段</th><th>映射</th></tr>
 *   <tr><td>折射 50</td><td>{@link #refraction()}</td>
 *       <td>{@code refractionAmount = 0.50 × 0.5 × minDimension}</td></tr>
 *   <tr><td>深度 10</td><td>{@link #depth()}</td>
 *       <td>{@code refractionHeight = 0.10 × 1.0 × minDimension}（折射带宽度）</td></tr>
 *   <tr><td>色散 45</td><td>{@link #dispersion()}</td>
 *       <td>直接作为 {@code chromaticAberration} 强度，0 表示关闭（关闭可省 7 次采样）</td></tr>
 *   <tr><td>高光 -45° 28%</td><td>{@link #highlight()}</td>
 *       <td>{@link GlassHighlight#direction(float, int, float)}</td></tr>
 *   <tr><td>圆角</td><td>{@link #radii()}</td>
 *       <td>四角半径，单位 px</td></tr>
 * </table>
 *
 * <h3>两个折射参数的差别（最容易搞混的一点）</h3>
 * <ul>
 *   <li>{@code refractionAmount} —— <b>偏移量</b>，背景被拉扯多远；</li>
 *   <li>{@code refractionHeight} —— <b>生效带宽</b>，从轮廓往内多深的位置开始不受影响。</li>
 * </ul>
 * 两者都由 {@code size.minDimension} 归一化，所以同一个组件在按钮上和大卡片上折射手感一致。
 * 直接用固定 dp 的话，小控件会折射过猛。
 *
 * <p>本类不可变，通过 {@link #builder()} 构造。
 */
public final class GlassStyle {

    /** 折射滑杆默认值（设计稿 50/100）。 */
    public static final float DEFAULT_REFRACTION = 0.50f;
    /** 深度滑杆默认值（设计稿 10/100）。 */
    public static final float DEFAULT_DEPTH = 0.10f;
    /** 色散滑杆默认值（设计稿 45/100）。 */
    public static final float DEFAULT_DISPERSION = 0.45f;
    /** 默认填充：15% 白。 */
    public static final float DEFAULT_SURFACE_ALPHA = 0.15f;
    /** 默认圆角：8px（对齐 61×26 的 chip）。 */
    public static final float DEFAULT_CORNER_PX = 8f;
    /** 折射映射系数：滑杆值 × 该系数 × minDimension = 偏移量。 */
    public static final float DEFAULT_REFRACTION_SCALE = 0.5f;
    /** 深度映射系数：滑杆值 × 该系数 × minDimension = 折射带宽度。 */
    public static final float DEFAULT_DEPTH_SCALE = 1.0f;

    private static final float[] DEFAULT_RADII = {
            DEFAULT_CORNER_PX, DEFAULT_CORNER_PX, DEFAULT_CORNER_PX, DEFAULT_CORNER_PX
    };

    private static final GlassStyle SPEC = builder().build();

    private final float[] mRadii;
    private final boolean mCapsule;
    private final float mRefraction;
    private final float mDepth;
    private final float mDispersion;
    private final boolean mDepthEffect;
    private final float mRefractionScale;
    private final float mDepthScale;
    private final float mBlurRadiusPx;
    private final float mSaturation;
    private final float mBrightness;
    private final float mContrast;
    private final int mSurfaceColor;
    private final GlassSurface mSurfacePainter;
    private final float mPressBaseAlpha;
    private final float mPressGlowAlpha;
    private final GlassHighlight mHighlight;

    private GlassStyle(Builder b) {
        mRadii = b.mRadii.clone();
        mCapsule = b.mCapsule;
        mRefraction = b.mRefraction;
        mDepth = b.mDepth;
        mDispersion = b.mDispersion;
        mDepthEffect = b.mDepthEffect;
        mRefractionScale = b.mRefractionScale;
        mDepthScale = b.mDepthScale;
        mBlurRadiusPx = b.mBlurRadiusPx;
        mSaturation = b.mSaturation;
        mBrightness = b.mBrightness;
        mContrast = b.mContrast;
        mSurfaceColor = b.mSurfaceColor;
        mSurfacePainter = b.mSurfacePainter;
        mPressBaseAlpha = b.mPressBaseAlpha;
        mPressGlowAlpha = b.mPressGlowAlpha;
        mHighlight = b.mHighlight;
    }

    /**
     * 设计稿默认配置：圆角 8px / 折射 50 / 深度 10 / 色散 45 / 高光 -45° 28% / 填充 15% 白。
     * 该实例是共享常量，可以放心复用。
     */
    public static GlassStyle spec() {
        return SPEC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    /** 四角半径（左上、右上、右下、左下），单位 px。返回副本，可安全修改。 */
    public float[] radii() {
        return mRadii.clone();
    }

    /** 是否为胶囊形（两端半圆）。胶囊形时四角半径按尺寸实时算，覆盖 {@link #radii()}。 */
    public boolean isCapsule() {
        return mCapsule;
    }

    /**
     * 按实际尺寸求四角半径。胶囊形返回 {@code minDimension / 2}，否则返回固定半径。
     * 渲染器一律用这个而不是 {@link #radii()}。
     */
    public float[] radiiFor(float minDimension) {
        if (!mCapsule) {
            return mRadii.clone();
        }
        final float radius = Math.max(0f, minDimension * 0.5f);
        return new float[]{radius, radius, radius, radius};
    }

    public float refraction() {
        return mRefraction;
    }

    public float depth() {
        return mDepth;
    }

    public float dispersion() {
        return mDispersion;
    }

    /** 是否开启整块曲率（设计面板里「深度」的直觉来源）。 */
    public boolean isDepthEffect() {
        return mDepthEffect;
    }

    /** 背景模糊半径（px），0 表示不模糊。 */
    public float blurRadiusPx() {
        return mBlurRadiusPx;
    }

    /** 饱和度，1 为原样，1.5 即上游的 {@code vibrancy()}。 */
    public float saturation() {
        return mSaturation;
    }

    public float brightness() {
        return mBrightness;
    }

    public float contrast() {
        return mContrast;
    }

    /** 玻璃表面的填充色（含 alpha）。默认 15% 白。 */
    public int surfaceColor() {
        return mSurfaceColor;
    }

    /**
     * 表面自定义绘制回调，画在 {@link #surfaceColor()} 之上、按压光之下。
     * 用来叠渐变或多层填充。可能为 null。
     */
    public GlassSurface surfacePainter() {
        return mSurfacePainter;
    }

    /** 按下时整块的提亮强度（0..1），默认 0.08。 */
    public float pressBaseAlpha() {
        return mPressBaseAlpha;
    }

    /** 跟手光斑的强度（0..1），默认 0.15。 */
    public float pressGlowAlpha() {
        return mPressGlowAlpha;
    }

    public GlassHighlight highlight() {
        return mHighlight;
    }

    /** 是否开启色散（开启会让每像素多采样 6 次背景纹理）。 */
    public boolean hasDispersion() {
        return mDispersion > 0f;
    }

    /** 是否需要走 CPU 调色（饱和度 / 亮度 / 对比度任一偏离默认值）。 */
    public boolean hasColorAdjustment() {
        return mSaturation != 1f || mBrightness != 0f || mContrast != 1f;
    }

    /** 折射偏移量（px）。 */
    public float refractionAmountPx(float minDimension) {
        return mRefraction * mRefractionScale * minDimension;
    }

    /** 折射生效带宽（px）。 */
    public float refractionHeightPx(float minDimension) {
        return mDepth * mDepthScale * minDimension;
    }

    /**
     * 背景抓取需要向外扩的边距（px）。
     *
     * <p>逻辑与上游一致，两处叠加：
     * <ol>
     *   <li>模糊是向外扩散的，采样半径不够就会在边缘变暗发灰 —— 所以至少扩 {@code blurRadius}；</li>
     *   <li>折射是<b>向内</b>采样的（{@code refractionAmount} 传负值），会「吃掉」一部分边距，
     *       所以可以把它减掉。</li>
     * </ol>
     * 最终 {@code padding = max(0, blurRadius − refractionHeight)}。
     *
     * <p>这是最容易翻车的地方：效果全对但边缘一圈发暗，八成就是这里没扩边。
     */
    public float paddingPx(float minDimension) {
        float padding = mBlurRadiusPx;
        float refractionHeight = refractionHeightPx(minDimension);
        if (padding > 0f) {
            padding = Math.max(0f, padding - refractionHeight);
        }
        return padding;
    }

    /** 浅拷贝一个四角同半径的 Builder。 */
    public static Builder roundedRect(float cornerPx) {
        return builder().uniformCorner(cornerPx);
    }

    // ---------------------------------------------------------------------

    public static final class Builder {

        private final float[] mRadii = DEFAULT_RADII.clone();
        private boolean mCapsule;
        private float mRefraction = DEFAULT_REFRACTION;
        private float mDepth = DEFAULT_DEPTH;
        private float mDispersion = DEFAULT_DISPERSION;
        private boolean mDepthEffect = true;
        private float mRefractionScale = DEFAULT_REFRACTION_SCALE;
        private float mDepthScale = DEFAULT_DEPTH_SCALE;
        private float mBlurRadiusPx = 0f;
        private float mSaturation = 1f;
        private float mBrightness = 0f;
        private float mContrast = 1f;
        private int mSurfaceColor = GlassHighlight.alphaWhite(DEFAULT_SURFACE_ALPHA);
        private GlassSurface mSurfacePainter;
        private float mPressBaseAlpha = 0.08f;
        private float mPressGlowAlpha = 0.15f;
        private GlassHighlight mHighlight = GlassHighlight.spec();

        Builder() {
        }

        Builder(GlassStyle style) {
            System.arraycopy(style.mRadii, 0, mRadii, 0, 4);
            mCapsule = style.mCapsule;
            mRefraction = style.mRefraction;
            mDepth = style.mDepth;
            mDispersion = style.mDispersion;
            mDepthEffect = style.mDepthEffect;
            mRefractionScale = style.mRefractionScale;
            mDepthScale = style.mDepthScale;
            mBlurRadiusPx = style.mBlurRadiusPx;
            mSaturation = style.mSaturation;
            mBrightness = style.mBrightness;
            mContrast = style.mContrast;
            mSurfaceColor = style.mSurfaceColor;
            mSurfacePainter = style.mSurfacePainter;
            mPressBaseAlpha = style.mPressBaseAlpha;
            mPressGlowAlpha = style.mPressGlowAlpha;
            mHighlight = style.mHighlight;
        }

        /** 四角同半径，单位 px。 */
        public Builder uniformCorner(float cornerPx) {
            mCapsule = false;
            float r = Math.max(0f, cornerPx);
            for (int i = 0; i < 4; i++) {
                mRadii[i] = r;
            }
            return this;
        }

        /** 胶囊形：两端半圆，半径按实际尺寸的 minDimension/2 实时算。 */
        public Builder capsule() {
            mCapsule = true;
            return this;
        }

        /** 圆形。与 {@code capsule()} 等价 —— 正方形上 minDimension/2 就是半径。 */
        public Builder circle() {
            mCapsule = true;
            return this;
        }

        /** 逐角设置：左上、右上、右下、左下，单位 px。 */
        public Builder corners(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            mCapsule = false;
            mRadii[0] = Math.max(0f, topLeft);
            mRadii[1] = Math.max(0f, topRight);
            mRadii[2] = Math.max(0f, bottomRight);
            mRadii[3] = Math.max(0f, bottomLeft);
            return this;
        }

        /** 折射滑杆 0..1（设计稿 0.50）。 */
        public Builder refraction(float slider01) {
            mRefraction = clamp01(slider01);
            return this;
        }

        /** 深度滑杆 0..1（设计稿 0.10）。 */
        public Builder depth(float slider01) {
            mDepth = clamp01(slider01);
            return this;
        }

        /** 色散滑杆 0..1（设计稿 0.45），0 表示关闭。 */
        public Builder dispersion(float slider01) {
            mDispersion = clamp01(slider01);
            return this;
        }

        /**
         * 是否开启整块曲率。开启后法线里会混入从中心指向外的径向向量，
         * 玻璃从「只有边缘一圈在折射」变成「整块像凸起的透镜」。
         */
        public Builder depthEffect(boolean enabled) {
            mDepthEffect = enabled;
            return this;
        }

        /** 背景模糊半径（px）。>0 时会自动扩边，见 {@link GlassStyle#paddingPx(float)}。 */
        public Builder blur(float radiusPx) {
            mBlurRadiusPx = Math.max(0f, radiusPx);
            return this;
        }

        /** 饱和度，1 为原样，1.5 等于上游的 {@code vibrancy()}。 */
        public Builder saturation(float value) {
            mSaturation = Math.max(0f, value);
            return this;
        }

        public Builder brightness(float value) {
            mBrightness = value;
            return this;
        }

        public Builder contrast(float value) {
            mContrast = Math.max(0f, value);
            return this;
        }

        /** 一步开启 vibrancy（饱和度 1.5）。 */
        public Builder vibrancy() {
            return saturation(1.5f);
        }

        /** 表面填充色（含 alpha），例如 {@code 0x26FFFFFF} 表示 15% 白。 */
        public Builder surfaceColor(int color) {
            mSurfaceColor = color;
            return this;
        }

        /** 表面填充：0..1 的不透明白。 */
        public Builder surfaceAlpha(float alpha) {
            mSurfaceColor = GlassHighlight.alphaWhite(alpha);
            return this;
        }

        /** 不填充，只要折射和高光。 */
        public Builder noSurface() {
            mSurfaceColor = Color.TRANSPARENT;
            return this;
        }

        /**
         * 表面自定义绘制。画在 {@link #surfaceColor()} 之上、按压光之下。
         * 用来叠线性渐变或多层填充，见 {@link GlassSurfaces}。
         */
        public Builder onDrawSurface(GlassSurface painter) {
            mSurfacePainter = painter;
            return this;
        }

        /**
         * 按下反馈的强度。默认底光 0.08、跟手光斑 0.15（与上游一致）。
         * 键盘按键这种小控件可以调大一点让反馈更明显。
         */
        public Builder pressTint(float baseAlpha, float glowAlpha) {
            mPressBaseAlpha = Math.max(0f, baseAlpha);
            mPressGlowAlpha = Math.max(0f, glowAlpha);
            return this;
        }

        public Builder highlight(GlassHighlight highlight) {
            mHighlight = highlight == null ? GlassHighlight.none() : highlight;
            return this;
        }

        /**
         * 覆盖折射映射系数（默认 0.5）。用来把设计稿滑杆直接翻译成像素：
         * {@code amount = slider × scale × minDimension}。
         */
        public Builder refractionScale(float scale) {
            mRefractionScale = Math.max(0f, scale);
            return this;
        }

        /** 覆盖深度映射系数（默认 1.0）。 */
        public Builder depthScale(float scale) {
            mDepthScale = Math.max(0f, scale);
            return this;
        }

        public GlassStyle build() {
            return new GlassStyle(this);
        }

        private static float clamp01(float v) {
            return v < 0f ? 0f : (v > 1f ? 1f : v);
        }
    }
}
