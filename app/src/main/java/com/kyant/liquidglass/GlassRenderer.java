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
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;

/**
 * 纯渲染器：给一块矩形区域画液态玻璃。零 View 依赖，可以单独复用或单测。
 *
 * <p>对应上游的 {@code effects/*}（blur / lens / vibrancy）+ {@code highlight/*}
 * + {@code onDrawSurface} 三部分，把 Compose 的 {@code RenderEffect} 链换成
 * 一次性 CALayer 风格的四层绘制：
 *
 * <ol>
 *   <li><b>折射层</b> —— AGSL RuntimeShader，输入是抓下来的背景位图；</li>
 *   <li><b>表面层</b> —— 半透明填充（设计稿 15% 白）；</li>
 *   <li><b>按压层</b> —— 可选的跟手径向光斑（加色混合）；</li>
 *   <li><b>高光层</b> —— SDF 法线描边（加色混合）。</li>
 * </ol>
 *
 * <h3>坐标约定</h3>
 * 着色器在画布局部坐标里求值，即 {@code coord ∈ [0,width] × [0,height]}。
 * 传入的背景位图可能包含一圈扩边（{@link GlassStyle#paddingPx(float)}），
 * 用 {@link #setBackdrop(Bitmap, float, float)} 的偏移参数把位图左上角对齐到画布坐标。
 *
 * <h3>降级</h3>
 * API &lt; 33 没有 {@link RuntimeShader}，此时折射 / 色散 / SDF 高光全部跳过，
 * 只画表面填充和纯色描边高光——不会崩，也不会变成全透明。
 */
public final class GlassRenderer {

    /** 折射与色散需要 AGSL，也就是 API 33 / Android 13。 */
    public static boolean isShaderSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    private final Paint mRefractionPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mSurfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPressBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mBounds = new RectF();
    private final RectF mInsetBounds = new RectF();
    private final float[] mRadii = new float[4];
    private final float[] mRadii8 = new float[8];
    private final float[] mInsetRadii8 = new float[8];
    private final Path mShapePath = new Path();
    private final Path mInsetPath = new Path();
    private final Matrix mBackdropMatrix = new Matrix();

    private GlassStyle mStyle;

    private RuntimeShader mRefractionShader;
    private boolean mRefractionShaderHasDispersion;
    private RuntimeShader mHighlightShader;
    private int mHighlightShaderKind = -1;
    private RuntimeShader mPressShader;
    private boolean mShaderBroken;
    private boolean mSurfacePainterBroken;

    private Bitmap mBackdrop;
    private float mBackdropOffsetX;
    private float mBackdropOffsetY;

    private boolean mPressActive;
    private float mPressProgress;
    private float mPressX;
    private float mPressY;

    public GlassRenderer() {
        this(GlassStyle.spec());
    }

    public GlassRenderer(GlassStyle style) {
        mStyle = style == null ? GlassStyle.spec() : style;
        mSurfacePaint.setStyle(Paint.Style.FILL);
        mHighlightPaint.setStyle(Paint.Style.STROKE);
    }

    public GlassStyle getStyle() {
        return mStyle;
    }

    public void setStyle(GlassStyle style) {
        if (style != null) {
            mStyle = style;
        }
    }

    /**
     * 传入背景位图与它的对齐偏移。
     *
     * @param region    抓取到的背景（通常含扩边）
     * @param offsetX   位图左上角在<b>画布局部坐标</b>里的 x。有扩边 pad 时就是 {@code -pad}
     * @param offsetY   同上，y
     */
    public void setBackdrop(Bitmap region, float offsetX, float offsetY) {
        mBackdrop = region;
        mBackdropOffsetX = offsetX;
        mBackdropOffsetY = offsetY;
    }

    public void clearBackdrop() {
        mBackdrop = null;
    }

    /**
     * 设置跟手按压高光状态。推进 {@code progress} 从 0 到 1 并配上手指位置，
     * 就会在玻璃上叠一圈加色光斑。
     */
    public void setPress(boolean active, float progress, float x, float y) {
        mPressActive = active;
        mPressProgress = progress;
        mPressX = x;
        mPressY = y;
    }

    /**
     * 把玻璃画到 {@code (0,0)-(width,height)} 这块区域上。调用方负责在合适的时机调用。
     */
    public void draw(Canvas canvas, float width, float height) {
        draw(canvas, width, height, null, 0f, 0f);
    }

    /**
     * @param shape 自定义轮廓；null 则按样式画圆角矩形。路径须在当前画布局部坐标里。
     * @param backdropShiftX 画布原点相对宿主 View 左上角的 x，用来对齐抓到的背景。
     * @param backdropShiftY 同上，y
     */
    public void draw(Canvas canvas, float width, float height, Path shape,
                     float backdropShiftX, float backdropShiftY) {
        if (canvas == null || width <= 0f || height <= 0f) {
            return;
        }
        final GlassStyle style = mStyle;
        final float minDimension = Math.min(width, height);

        mBounds.set(0f, 0f, width, height);
        copyRadii(style.radiiFor(minDimension));
        buildRadii8();
        mShapePath.reset();
        final boolean custom = shape != null && !shape.isEmpty();
        if (custom) {
            mShapePath.set(shape);
        } else {
            mShapePath.addRoundRect(mBounds, mRadii8, Path.Direction.CW);
        }

        final float savedOx = mBackdropOffsetX;
        final float savedOy = mBackdropOffsetY;
        mBackdropOffsetX -= backdropShiftX;
        mBackdropOffsetY -= backdropShiftY;
        try {
            drawRefraction(canvas, style, width, height, minDimension);
            drawSurface(canvas, style, width, height);
            drawPress(canvas, style, width, height, minDimension);
            drawHighlight(canvas, style, width, height, minDimension, custom);
        } finally {
            mBackdropOffsetX = savedOx;
            mBackdropOffsetY = savedOy;
        }
    }

    /** 释放 shader 缓存。视图 detach 时调用。 */
    public void release() {
        mRefractionShader = null;
        mHighlightShader = null;
        mHighlightShaderKind = -1;
        mPressShader = null;
        mBackdrop = null;
    }

    // ------------------------------------------------------------------ 折射

    private void drawRefraction(Canvas canvas, GlassStyle style, float width, float height,
                                float minDimension) {
        if (!isShaderSupported() || mShaderBroken) {
            return;
        }
        final Bitmap backdrop = mBackdrop;
        if (backdrop == null || backdrop.isRecycled()) {
            return;
        }
        final float amount = style.refractionAmountPx(minDimension);
        final float band = style.refractionHeightPx(minDimension);
        if (amount <= 0f || band <= 0f) {
            // 折射关闭时背景原样透出，不需要额外绘制
            return;
        }

        final boolean dispersion = style.hasDispersion();
        final RuntimeShader shader = obtainRefractionShader(dispersion);
        if (shader == null) {
            return;
        }

        final BitmapShader content =
                new BitmapShader(backdrop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mBackdropMatrix.reset();
        mBackdropMatrix.setTranslate(-mBackdropOffsetX, -mBackdropOffsetY);
        content.setLocalMatrix(mBackdropMatrix);

        try {
            shader.setInputShader("content", content);
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("offset", 0f, 0f);
            shader.setFloatUniform("cornerRadii", mRadii[0], mRadii[1], mRadii[2], mRadii[3]);
            shader.setFloatUniform("refractionHeight", band);
            // 负值 = 向内采样，进入视野的是更靠里的背景，产生放大 / 吸入感。
            // 想反过来（边缘把外面内容吸进来），把符号改回来即可。
            shader.setFloatUniform("refractionAmount", -amount);
            shader.setFloatUniform("depthEffect", style.isDepthEffect() ? 1f : 0f);
            if (dispersion) {
                shader.setFloatUniform("chromaticAberration", style.dispersion());
            }
            mRefractionPaint.setShader(shader);
            canvas.drawPath(mShapePath, mRefractionPaint);
        } catch (RuntimeException e) {
            // shader 编译或绑定失败，整体退回「只有表面 + 高光」的形态
            mShaderBroken = true;
        }
    }

    // ------------------------------------------------------------------ 表面

    private void drawSurface(Canvas canvas, GlassStyle style, float width, float height) {
        final int color = style.surfaceColor();
        if (Color.alpha(color) != 0) {
            mSurfacePaint.setShader(null);
            mSurfacePaint.setColor(color);
            canvas.drawPath(mShapePath, mSurfacePaint);
        }
        final GlassSurface painter = style.surfacePainter();
        if (painter != null) {
            // 业务侧自己画渐变 / 多层填充。异常不能拖垮整帧，抓一次就永久停用。
            if (!mSurfacePainterBroken) {
                try {
                    painter.drawSurface(canvas, mShapePath, width, height);
                } catch (RuntimeException e) {
                    mSurfacePainterBroken = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------ 按压

    private void drawPress(Canvas canvas, GlassStyle style, float width, float height,
                           float minDimension) {
        if (!mPressActive || mPressProgress <= 0.001f) {
            return;
        }
        final int save = canvas.save();
        canvas.clipPath(mShapePath);

        // 底光：按下时整块稍微提亮
        mPressBasePaint.setColor(GlassHighlight.alphaWhite(style.pressBaseAlpha() * mPressProgress));
        mPressBasePaint.setStyle(Paint.Style.FILL);
        setPlusBlend(mPressBasePaint);
        canvas.drawPath(mShapePath, mPressBasePaint);

        // 跟手光斑
        if (isShaderSupported() && !mShaderBroken && style.pressGlowAlpha() > 0f) {
            final RuntimeShader shader = obtainPressShader();
            if (shader != null) {
                try {
                    shader.setFloatUniform("size", width, height);
                    shader.setColorUniform("color",
                            GlassHighlight.alphaWhite(style.pressGlowAlpha() * mPressProgress));
                    shader.setFloatUniform("radius", minDimension * 1.5f);
                    shader.setFloatUniform("position",
                            clamp(mPressX, 0f, width), clamp(mPressY, 0f, height));
                    mPressPaint.setShader(shader);
                    setPlusBlend(mPressPaint);
                    canvas.drawRect(0f, 0f, width, height, mPressPaint);
                } catch (RuntimeException e) {
                    mShaderBroken = true;
                }
            }
        }

        canvas.restoreToCount(save);
    }

    // ------------------------------------------------------------------ 高光

    private void drawHighlight(Canvas canvas, GlassStyle style, float width, float height,
                               float minDimension, boolean customShape) {
        final GlassHighlight highlight = style.highlight();
        if (highlight == null || !highlight.isEnabled()) {
            return;
        }
        final float visible = Math.min(highlight.widthPx(), minDimension * 0.5f);
        if (visible <= 0f) {
            return;
        }
        if (customShape) {
            mInsetPath.set(mShapePath);
        } else {
            // 上游是「描边宽度翻倍 + 裁掉外半边」。这里改成把路径内缩半个宽度，
            // 效果完全等价，但不需要 clipPath，边缘抗锯齿更干净。
            final float inset = visible * 0.5f;
            mInsetBounds.set(inset, inset, width - inset, height - inset);
            for (int i = 0; i < 4; i++) {
                final float radius = Math.max(0f, mRadii[i] - inset);
                mInsetRadii8[i * 2] = radius;
                mInsetRadii8[i * 2 + 1] = radius;
            }
            mInsetPath.reset();
            mInsetPath.addRoundRect(mInsetBounds, mInsetRadii8, Path.Direction.CW);
        }

        mHighlightPaint.setStrokeWidth(visible);
        setPlusBlend(mHighlightPaint);
        applyHighlightMaskFilter(mHighlightPaint, highlight);

        RuntimeShader shader = null;
        if (highlight.kind() != GlassHighlight.KIND_PLAIN && isShaderSupported() && !mShaderBroken) {
            shader = obtainHighlightShader(highlight.kind());
            if (shader != null) {
                try {
                    shader.setFloatUniform("size", width, height);
                    shader.setFloatUniform("cornerRadii", mRadii[0], mRadii[1], mRadii[2], mRadii[3]);
                    if (highlight.kind() == GlassHighlight.KIND_DEFAULT) {
                        // shader 里的颜色取不透明，实际透明度交给 Paint alpha ——
                        // 与上游「颜色 alpha=1 + layer alpha」的做法等价。
                        shader.setColorUniform("color", highlight.color() | 0xFF000000);
                    }
                    shader.setFloatUniform("angle", (float) Math.toRadians(highlight.angleDeg()));
                    shader.setFloatUniform("falloff", highlight.falloff());
                } catch (RuntimeException e) {
                    mShaderBroken = true;
                    shader = null;
                }
            }
        }

        if (shader != null) {
            mHighlightPaint.setShader(shader);
            mHighlightPaint.setAlpha(Color.alpha(highlight.color()));
        } else {
            mHighlightPaint.setShader(null);
            mHighlightPaint.setColor(highlight.color());
        }
        canvas.drawPath(mInsetPath, mHighlightPaint);
    }

    // ------------------------------------------------------------------ 工具

    private RuntimeShader obtainRefractionShader(boolean dispersion) {
        if (mRefractionShader != null && mRefractionShaderHasDispersion == dispersion) {
            return mRefractionShader;
        }
        try {
            mRefractionShader = new RuntimeShader(dispersion
                    ? GlassShaders.REFRACTION_WITH_DISPERSION
                    : GlassShaders.REFRACTION);
            mRefractionShaderHasDispersion = dispersion;
            return mRefractionShader;
        } catch (RuntimeException e) {
            mShaderBroken = true;
            mRefractionShader = null;
            return null;
        }
    }

    private RuntimeShader obtainHighlightShader(int kind) {
        if (mHighlightShader != null && mHighlightShaderKind == kind) {
            return mHighlightShader;
        }
        try {
            mHighlightShader = new RuntimeShader(kind == GlassHighlight.KIND_AMBIENT
                    ? GlassShaders.HIGHLIGHT_AMBIENT
                    : GlassShaders.HIGHLIGHT_DEFAULT);
            mHighlightShaderKind = kind;
            return mHighlightShader;
        } catch (RuntimeException e) {
            mShaderBroken = true;
            mHighlightShader = null;
            return null;
        }
    }

    private RuntimeShader obtainPressShader() {
        if (mPressShader != null) {
            return mPressShader;
        }
        try {
            mPressShader = new RuntimeShader(GlassShaders.PRESS_RADIAL);
            return mPressShader;
        } catch (RuntimeException e) {
            mShaderBroken = true;
            return null;
        }
    }

    /**
     * 加色混合。上游高光用 {@code BlendMode.Plus}——
     * 这是「光沿着边缘亮起来」的关键，用默认的 SRC_OVER 会偏闷。
     */
    private static void setPlusBlend(Paint paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.setBlendMode(BlendMode.PLUS);
        } else {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        }
    }

    private static void applyHighlightMaskFilter(Paint paint, GlassHighlight highlight) {
        final float blur = highlight.blurPx();
        if (blur <= 0f) {
            return;
        }
        try {
            paint.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        } catch (RuntimeException ignored) {
            // 硬件加速画布上遮罩滤镜可能不被支持，忽略即可
        }
    }

    private void copyRadii(float[] source) {
        System.arraycopy(source, 0, mRadii, 0, 4);
    }

    /** 把 4 个角半径摊成 Path.addRoundRect 需要的 8 个分量（x,y 成对）。 */
    private void buildRadii8() {
        for (int i = 0; i < 4; i++) {
            mRadii8[i * 2] = mRadii[i];
            mRadii8[i * 2 + 1] = mRadii[i];
        }
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
