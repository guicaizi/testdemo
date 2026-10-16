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

import android.graphics.Bitmap;

/**
 * 背景位图的 CPU 后处理：模糊 + 调色。
 *
 * <p>上游用 Compose 的 {@code RenderEffect} 链（底层是 GPU）做这两件事。但
 * {@code RenderEffect} <b>不能</b>作用在 {@code Paint} 的 shader 输入上，
 * 而本工具包走的是「软件抓一帧背景 → 当 shader 输入」这条路，
 * 所以改为在抓取时一次性处理 CPU 位图。
 *
 * <p>成本可以接受的原因：玻璃元素通常很小（小控件 61×26、按钮 168×44），
 * 而且只在布局变化时跑一次，不在每帧。假如以后要做全屏玻璃，请自行挪到子线程或降采样。
 */
final class GlassImageFilter {

    private static final int BLUR_PASSES = 3;

    private GlassImageFilter() {
    }

    /**
     * 可分离盒式模糊，跑 3 遍近似高斯。
     *
     * <p>先在**预乘 alpha** 的空间里做，结束后再还原。不预乘的话，
     * 跨过透明区域的插值会把透明像素的黑色混进来，边缘出现暗边。
     * 背景全不透明时预乘等于恒等，没有额外代价。
     *
     * @param bitmap   就地修改
     * @param radiusPx 半径，&lt;1 直接返回
     */
    static void boxBlur(Bitmap bitmap, float radiusPx) {
        int radius = Math.round(radiusPx);
        if (radius < 1 || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        if (width < 2 || height < 2) {
            return;
        }
        // 半径超过半宽没有额外意义，只会白白增加计算量
        radius = Math.min(radius, Math.max(1, Math.min(width, height) / 2));

        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        premultiply(pixels);
        final int[] scratch = new int[pixels.length];
        for (int pass = 0; pass < BLUR_PASSES; pass++) {
            blurHorizontal(pixels, scratch, width, height, radius);
            blurVertical(scratch, pixels, width, height, radius);
        }
        unpremultiply(pixels);

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    /**
     * 亮度 / 对比度 / 饱和度调整。公式与上游 {@code colorControls()} 完全一致
     * （对比度系数 0.213/0.715/0.072 是 Rec.709 亮度权重）。
     */
    static void colorControls(Bitmap bitmap, float brightness, float contrast, float saturation) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        if (brightness == 0f && contrast == 1f && saturation == 1f) {
            return;
        }
        final float invSat = 1f - saturation;
        final float r = 0.213f * invSat;
        final float g = 0.715f * invSat;
        final float b = 0.072f * invSat;

        final float c = contrast;
        final float t = (0.5f - c * 0.5f + brightness) * 255f;
        final float s = saturation;

        final float cr = c * r;
        final float cg = c * g;
        final float cb = c * b;
        final float cs = c * s;

        final float[] matrix = {
                cr + cs, cg, cb, 0f, t,
                cr, cg + cs, cb, 0f, t,
                cr, cg, cb + cs, 0f, t,
                0f, 0f, 0f, 1f, 0f
        };
        applyColorMatrix(bitmap, matrix);
    }

    /** 4×5 颜色矩阵，行优先。就地应用到非预乘的 ARGB 位上。 */
    static void applyColorMatrix(Bitmap bitmap, float[] matrix) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            final int color = pixels[i];
            final int alpha = (color >>> 24) & 0xFF;
            if (alpha == 0) {
                continue;
            }
            final float r = (color >>> 16) & 0xFF;
            final float g = (color >>> 8) & 0xFF;
            final float b = color & 0xFF;

            float nr = matrix[0] * r + matrix[1] * g + matrix[2] * b + matrix[4];
            float ng = matrix[5] * r + matrix[6] * g + matrix[7] * b + matrix[9];
            float nb = matrix[10] * r + matrix[11] * g + matrix[12] * b + matrix[14];

            pixels[i] = (alpha << 24) | (clamp8(nr) << 16) | (clamp8(ng) << 8) | clamp8(nb);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    // ------------------------------------------------------------------ 内部

    private static void premultiply(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            final int c = pixels[i];
            final int a = (c >>> 24) & 0xFF;
            if (a == 255) {
                continue;
            }
            if (a == 0) {
                pixels[i] = 0;
                continue;
            }
            final int r = ((c >>> 16) & 0xFF) * a / 255;
            final int g = ((c >>> 8) & 0xFF) * a / 255;
            final int b = (c & 0xFF) * a / 255;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static void unpremultiply(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            final int c = pixels[i];
            final int a = (c >>> 24) & 0xFF;
            if (a == 255 || a == 0) {
                continue;
            }
            int r = ((c >>> 16) & 0xFF) * 255 / a;
            int g = ((c >>> 8) & 0xFF) * 255 / a;
            int b = (c & 0xFF) * 255 / a;
            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static void blurHorizontal(int[] in, int[] out, int width, int height, int radius) {
        final int divisor = radius * 2 + 1;
        for (int y = 0; y < height; y++) {
            final int row = y * width;
            int sumA = 0, sumR = 0, sumG = 0, sumB = 0;
            for (int i = -radius; i <= radius; i++) {
                final int c = in[row + clampIndex(i, width)];
                sumA += (c >>> 24) & 0xFF;
                sumR += (c >>> 16) & 0xFF;
                sumG += (c >>> 8) & 0xFF;
                sumB += c & 0xFF;
            }
            for (int x = 0; x < width; x++) {
                out[row + x] = pack(sumA / divisor, sumR / divisor, sumG / divisor, sumB / divisor);
                final int add = in[row + clampIndex(x + radius + 1, width)];
                final int sub = in[row + clampIndex(x - radius, width)];
                sumA += ((add >>> 24) & 0xFF) - ((sub >>> 24) & 0xFF);
                sumR += ((add >>> 16) & 0xFF) - ((sub >>> 16) & 0xFF);
                sumG += ((add >>> 8) & 0xFF) - ((sub >>> 8) & 0xFF);
                sumB += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    private static void blurVertical(int[] in, int[] out, int width, int height, int radius) {
        final int divisor = radius * 2 + 1;
        for (int x = 0; x < width; x++) {
            int sumA = 0, sumR = 0, sumG = 0, sumB = 0;
            for (int i = -radius; i <= radius; i++) {
                final int c = in[clampIndex(i, height) * width + x];
                sumA += (c >>> 24) & 0xFF;
                sumR += (c >>> 16) & 0xFF;
                sumG += (c >>> 8) & 0xFF;
                sumB += c & 0xFF;
            }
            for (int y = 0; y < height; y++) {
                out[y * width + x] = pack(sumA / divisor, sumR / divisor, sumG / divisor, sumB / divisor);
                final int add = in[clampIndex(y + radius + 1, height) * width + x];
                final int sub = in[clampIndex(y - radius, height) * width + x];
                sumA += ((add >>> 24) & 0xFF) - ((sub >>> 24) & 0xFF);
                sumR += ((add >>> 16) & 0xFF) - ((sub >>> 16) & 0xFF);
                sumG += ((add >>> 8) & 0xFF) - ((sub >>> 8) & 0xFF);
                sumB += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    /** 边缘用 clamp 延拓，避免采样越界后出现黑边。 */
    private static int clampIndex(int index, int limit) {
        if (index < 0) {
            return 0;
        }
        if (index >= limit) {
            return limit - 1;
        }
        return index;
    }

    private static int pack(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp8(float v) {
        if (v <= 0f) {
            return 0;
        }
        if (v >= 255f) {
            return 255;
        }
        return Math.round(v);
    }
}
