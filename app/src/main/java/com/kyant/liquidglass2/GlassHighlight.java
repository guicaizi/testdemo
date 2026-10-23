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
 * 边缘高光的样式描述。对应 AndroidLiquidGlass 的
 * {@code backdrop/highlight/HighlightStyle.kt} 的三个变体。
 *
 * <p>三种形态：
 * <table border="1">
 *   <tr><th>形态</th><th>算法</th><th>用途</th></tr>
 *   <tr><td>{@link #KIND_PLAIN}</td><td>纯色描边，无 shader</td><td>低版本降级、或单纯想要一圈亮边</td></tr>
 *   <tr><td>{@link #KIND_DEFAULT}</td><td>SDF 法线 · 光照方向</td><td>有方向感的镜面光（设计稿默认）</td></tr>
 *   <tr><td>{@link #KIND_AMBIENT}</td><td>SDF 法线，单侧</td><td>无方向底光</td></tr>
 * </table>
 *
 * <p>全部字段都在像素单位下工作。屏幕密度不同时请自行换算 dp → px。
 */
public final class GlassHighlight {

    /** 不画高光。 */
    public static final int KIND_NONE = 0;
    /** 纯色描边，不依赖 RuntimeShader。 */
    public static final int KIND_PLAIN = 1;
    /** SDF 法线 · 方向光，两侧对称（默认）。 */
    public static final int KIND_DEFAULT = 2;
    /** SDF 法线 · 环境光，只亮单侧。 */
    public static final int KIND_AMBIENT = 3;

    /** 设计稿默认：-45°，28% 白。 */
    public static final float DEFAULT_ANGLE_DEG = -45f;
    public static final float DEFAULT_ALPHA = 0.28f;
    public static final float DEFAULT_WIDTH_PX = 1.5f;
    public static final float DEFAULT_FALLOFF = 1f;
    public static final float DEFAULT_BLUR_PX = 0f;

    private final int mKind;
    private final int mColor;
    private final float mWidthPx;
    private final float mBlurPx;
    private final float mAngleDeg;
    private final float mFalloff;

    private GlassHighlight(int kind, int color, float widthPx, float blurPx,
                           float angleDeg, float falloff) {
        mKind = kind;
        mColor = color;
        mWidthPx = widthPx;
        mBlurPx = blurPx;
        mAngleDeg = angleDeg;
        mFalloff = falloff;
    }

    /** 无高光。 */
    public static GlassHighlight none() {
        return new GlassHighlight(KIND_NONE, Color.TRANSPARENT, 0f, 0f, 0f, 1f);
    }

    /**
     * 设计稿默认形态：SDF 法线方向光。
     *
     * @param angleDeg 光照方向（度）。屏幕坐标系下 y 轴朝下，-45° 表示左下→右上的斜光。
     * @param color    含 alpha 的颜色，例如 {@code 0x47FFFFFF} 表示 28% 白。
     * @param widthPx  可见描边宽度（px）。内部会自动取一半内缩，保证整条边落在形状内部。
     */
    public static GlassHighlight direction(float angleDeg, int color, float widthPx) {
        return new GlassHighlight(KIND_DEFAULT, color, widthPx, DEFAULT_BLUR_PX, angleDeg, DEFAULT_FALLOFF);
    }

    public static GlassHighlight direction(float angleDeg, int color, float widthPx,
                                          float falloff, float blurPx) {
        return new GlassHighlight(KIND_DEFAULT, color, widthPx, blurPx, angleDeg, falloff);
    }

    /** 设计稿默认：-45°、28% 白、1.5px。 */
    public static GlassHighlight spec() {
        return direction(DEFAULT_ANGLE_DEG, alphaWhite(DEFAULT_ALPHA), DEFAULT_WIDTH_PX);
    }

    /** 纯色描边，不依赖 shader，任何版本都有效。 */
    public static GlassHighlight plain(int color, float widthPx) {
        return new GlassHighlight(KIND_PLAIN, color, widthPx, DEFAULT_BLUR_PX, 0f, 1f);
    }

    /** 环境光（单侧）。 */
    public static GlassHighlight ambient(float alpha, float widthPx) {
        return new GlassHighlight(KIND_AMBIENT, alphaWhite(alpha), widthPx, DEFAULT_BLUR_PX,
                45f, DEFAULT_FALLOFF);
    }

    /** 把 0..1 的 alpha 换算成不透明白。 */
    public static int alphaWhite(float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return Color.argb(a, 255, 255, 255);
    }

    public int kind() {
        return mKind;
    }

    public int color() {
        return mColor;
    }

    public float widthPx() {
        return mWidthPx;
    }

    public float blurPx() {
        return mBlurPx;
    }

    public float angleDeg() {
        return mAngleDeg;
    }

    public float falloff() {
        return mFalloff;
    }

    public boolean isEnabled() {
        return mKind != KIND_NONE && mWidthPx > 0f && Color.alpha(mColor) > 0;
    }
}
