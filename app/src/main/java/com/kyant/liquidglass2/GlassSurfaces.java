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

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * {@link GlassSurface} 的常用工厂。避免在每个容器上写一遍匿名类。
 *
 * <p>所有渐变都按玻璃的实际尺寸实时构建（{@code width}/{@code height} 在回调里才拿得到），
 * 所以同一个 {@link GlassStyle} 可以复用到不同尺寸的容器上而不会拉伸错位。
 */
public final class GlassSurfaces {

    private GlassSurfaces() {
    }

    /** 单色填充。等价于 {@link GlassStyle#surfaceColor()}，但可以和多层叠加混用。 */
    public static GlassSurface solid(final int color) {
        return new GlassSurface() {
            @Override
            public void drawSurface(Canvas canvas, Path shapePath, float width, float height) {
                final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(color);
                canvas.drawPath(shapePath, paint);
            }
        };
    }

    /**
     * 线性渐变填充。
     *
     * @param angleDeg 渐变方向（度）。屏幕坐标 y 轴朝下：
     *                 <ul>
     *                   <li>{@code 90}  → 上到下（最常用）</li>
     *                   <li>{@code 0}   → 左到右</li>
     *                   <li>{@code -45} → 左下到右上</li>
     *                 </ul>
     * @param colors   颜色数组，至少 2 个
     * @param stops    位置数组，null 表示均匀分布
     */
    public static GlassSurface linearGradient(final int[] colors, final float[] stops,
                                              final float angleDeg) {
        return new GlassSurface() {
            @Override
            public void drawSurface(Canvas canvas, Path shapePath, float width, float height) {
                final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setShader(buildLinear(colors, stops, angleDeg, width, height));
                canvas.drawPath(shapePath, paint);
            }
        };
    }

    /** 上下渐变（快捷方式）。 */
    public static GlassSurface vertical(int topColor, int bottomColor) {
        return linearGradient(new int[]{topColor, bottomColor}, null, 90f);
    }

    /** 左右渐变（快捷方式）。 */
    public static GlassSurface horizontal(int leftColor, int rightColor) {
        return linearGradient(new int[]{leftColor, rightColor}, null, 0f);
    }

    /**
     * 把若干个 {@link GlassSurface} 按顺序叠加。
     * 设计稿那种「白 15% + 渐变 + 绿色渐变」直接丢进来即可。
     */
    public static GlassSurface stack(final GlassSurface... layers) {
        return new GlassSurface() {
            @Override
            public void drawSurface(Canvas canvas, Path shapePath, float width, float height) {
                if (layers == null) {
                    return;
                }
                for (int i = 0; i < layers.length; i++) {
                    final GlassSurface layer = layers[i];
                    if (layer != null) {
                        layer.drawSurface(canvas, shapePath, width, height);
                    }
                }
            }
        };
    }

    /**
     * 边缘内发光：中心透明、往边缘渐亮。适合做「玻璃内壁反光」，
     * 和 SDF 高光是两个不同的东西——这个不带方向性。
     *
     * @param color    内发光颜色（含 alpha）
     * @param insetPx  从边缘往内多深开始衰减
     */
    public static GlassSurface innerGlow(final int color, final float insetPx) {
        return new GlassSurface() {
            @Override
            public void drawSurface(Canvas canvas, Path shapePath, float width, float height) {
                final float radius = Math.max(width, height) * 0.5f;
                final float inner = Math.max(0f, radius - Math.max(0f, insetPx));
                final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setShader(new RadialGradient(
                        width * 0.5f, height * 0.5f, radius,
                        new int[]{0x00000000, 0x00000000, color},
                        new float[]{0f, inner / radius, 1f},
                        Shader.TileMode.CLAMP));
                canvas.drawPath(shapePath, paint);
            }
        };
    }

    // ------------------------------------------------------------------

    private static LinearGradient buildLinear(int[] colors, float[] stops, float angleDeg,
                                              float width, float height) {
        final double radians = Math.toRadians(angleDeg);
        final float dx = (float) Math.cos(radians);
        final float dy = (float) Math.sin(radians);
        // 用外接半对角线做投影长度，保证渐变铺满整个容器
        final float cx = width * 0.5f;
        final float cy = height * 0.5f;
        final float half = (Math.abs(dx) * width + Math.abs(dy) * height) * 0.5f;
        return new LinearGradient(
                cx - dx * half, cy - dy * half,
                cx + dx * half, cy + dy * half,
                colors, stops, Shader.TileMode.CLAMP);
    }
}
