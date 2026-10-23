package com.example.testdemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.testdemo.R;
import com.kyant.liquidglass2.GlassBackdrop;
import com.kyant.liquidglass2.GlassController;
import com.kyant.liquidglass2.GlassHighlight;
import com.kyant.liquidglass2.GlassStyle;
import com.kyant.liquidglass2.GlassSurface;

/**
 * 图案页底栏：480 蒙版坐标。
 *
 * <p>编组 (64,396) 352×165。左右各 175×165，中间空 2px。
 * 大圆角矩形 350×271 r=100 与 175×168（相对编组 Y=-3）求交，再与屏幕圆求交。
 * 分缝内侧顶角加小圆角。
 *
 * <p>描边共用内部 0.8、白 31%→0%。左填 #0DDB58 0%→白 45%；
 * 右填 #0DDB58 0%→100%。两钮都是 0% 在可见内沿、100% 在表圈（上深下亮）。
 * 左玻璃 −10°/44% 折射 50 深度 60 色散 50；右 −45°/31% 深度 50。
 */
public class KusaiPatternFooterView extends View {

    private static final float DESIGN = 480f;
    private static final float GROUP_X = 64f;
    private static final float GROUP_Y = 396f;
    /** 编组总宽 352 = 左钮 175 + 缝 2 + 右钮 175。单按钮模式下横跨此宽度。 */
    private static final float GROUP_W = 352f;
    private static final float GROUP_CENTER_X = GROUP_X + GROUP_W / 2f;
    private static final float ROUND_W = 350f;
    private static final float ROUND_H = 271f;
    private static final float ROUND_R = 100f;
    private static final float SHARP_W = 175f;
    private static final float HALF_H = 165f;
    private static final float GAP = 2f;
    private static final float INNER_TOP_R = 8f;
    private static final float RETRY_TEXT_X = 152f;
    private static final float CONTINUE_TEXT_X = 267f;
    private static final float TEXT_Y = 420f;
    private static final float TEXT_H = 26f;
    private static final float TEXT_SIZE = 20f;
    private static final float STROKE_WIDTH = 0.8f;
    private static final int TEXT_COLOR = 0xE6FFFFFF;
    /** 稿 0% 在可见上沿（内弧，上深），100% 在可见下沿（表圈，下亮）。 */
    private static final int RETRY_FILL_0 = 0x000DDB58;
    private static final int RETRY_FILL_100 = 0x73FFFFFF;
    private static final int CONTINUE_FILL_0 = 0x000DDB58;
    private static final int CONTINUE_FILL_100 = 0xFF0DDB58;
    private static final int STROKE_TOP = 0x4FFFFFFF;
    private static final int STROKE_BOTTOM = 0x00FFFFFF;

    private final Path mRetryPath = new Path();
    private final Path mContinuePath = new Path();
    private final Path mRetryLocal = new Path();
    private final Path mContinueLocal = new Path();
    private final Path mDrawPath = new Path();
    private final Region mRetryRegion = new Region();
    private final Region mContinueRegion = new Region();
    private final Matrix mScaleMatrix = new Matrix();
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mGradBounds = new RectF();
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private String mRetryLabel;
    private String mContinueLabel;
    private final GlassController mRetryGlass = new GlassController();
    private final GlassController mContinueGlass = new GlassController();
    private final GlassController mSingleGlass = new GlassController();

    private float mScale = 1f;
    private float mOffsetX;
    private float mOffsetY;
    private boolean mRetryEnabled;
    private boolean mContinueEnabled;
    private boolean mSingleButton;
    private String mSingleLabel;
    private OnClickListener mRetryClickListener;
    private OnClickListener mContinueClickListener;
    private OnClickListener mSingleClickListener;

    private final GlassSurface mRetrySurface = (canvas, shapePath, width, height) -> {
        drawFillAndStroke(canvas, shapePath, width, height, RETRY_FILL_0, RETRY_FILL_100);
    };

    private final GlassSurface mContinueSurface = (canvas, shapePath, width, height) -> {
        drawFillAndStroke(canvas, shapePath, width, height, CONTINUE_FILL_0, CONTINUE_FILL_100);
    };

    public KusaiPatternFooterView(Context context) {
        this(context, null);
    }

    public KusaiPatternFooterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KusaiPatternFooterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setClickable(true);
        setBackground(null);
        mRetryLabel = context.getString(R.string.kusai_pattern_redraw);
        mContinueLabel = context.getString(R.string.kusai_pattern_continue);
        mSingleLabel = mContinueLabel;
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(TEXT_SIZE);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mFillPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(STROKE_WIDTH);
        mRetryGlass.attach(this);
        mContinueGlass.attach(this);
        mSingleGlass.attach(this);
        mRetryGlass.setStyle(buildStyle(true));
        mContinueGlass.setStyle(buildStyle(false));
        mSingleGlass.setStyle(buildSingleStyle());
    }

    public void setRetryEnabled(boolean enabled) {
        if (mRetryEnabled == enabled) {
            return;
        }
        mRetryEnabled = enabled;
        invalidate();
    }

    public void setContinueEnabled(boolean enabled) {
        if (mContinueEnabled == enabled) {
            return;
        }
        mContinueEnabled = enabled;
        invalidate();
    }

    /** Defaults stay Retry / Continue. Backup Unlock passes Cancel / Switch. */
    public void setRetryLabel(CharSequence label) {
        String next = label == null ? "" : label.toString();
        if (next.equals(mRetryLabel)) {
            return;
        }
        mRetryLabel = next;
        invalidate();
    }

    /** Defaults stay Retry / Continue. Backup Unlock passes Cancel / Switch. */
    public void setContinueLabel(CharSequence label) {
        String next = label == null ? "" : label.toString();
        if (next.equals(mContinueLabel)) {
            return;
        }
        mContinueLabel = next;
        invalidate();
    }

    public void setOnRetryClickListener(OnClickListener listener) {
        mRetryClickListener = listener;
    }

    public void setOnContinueClickListener(OnClickListener listener) {
        mContinueClickListener = listener;
    }

    /**
     * 切换单按钮模式。开启后只显示一个横跨整个编组宽度的绿色按钮（Continue 同款玻璃样式），
     * 居中显示 {@link #setSingleButtonLabel} 设置的文案。
     */
    public void setSingleButton(boolean single) {
        if (mSingleButton == single) {
            return;
        }
        mSingleButton = single;
        rebuildPaths();
        invalidate();
    }

    public boolean isSingleButton() {
        return mSingleButton;
    }

    public void setSingleButtonLabel(CharSequence label) {
        String next = label == null ? "" : label.toString();
        if (next.equals(mSingleLabel)) {
            return;
        }
        mSingleLabel = next;
        invalidate();
    }

    public void setOnSingleClickListener(OnClickListener listener) {
        mSingleClickListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mRetryGlass.onAttached();
        mContinueGlass.onAttached();
        mSingleGlass.onAttached();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mRetryGlass.onLayout();
        mContinueGlass.onLayout();
        mSingleGlass.onLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRetryGlass.onDetached();
        mContinueGlass.onDetached();
        mSingleGlass.onDetached();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScale = Math.min(w, h) / DESIGN;
        mOffsetX = (w - DESIGN * mScale) / 2f;
        mOffsetY = (h - DESIGN * mScale) / 2f;
        rebuildPaths();
    }

    private void rebuildPaths() {
        final float leftX = GROUP_X;
        final float rightX = GROUP_X + SHARP_W + GAP;
        final float roundTop = GROUP_Y;
        final float halfTop = GROUP_Y;
        final float halfBottom = GROUP_Y + HALF_H;

        Path screen = new Path();
        screen.addCircle(DESIGN / 2f, DESIGN / 2f, DESIGN / 2f, Path.Direction.CW);

        if (mSingleButton) {
            // 单按钮：横跨整个编组宽度，两侧大圆角，与屏幕圆求交。
            Path fullRounded = new Path();
            fullRounded.addRoundRect(
                    new RectF(leftX, roundTop, leftX + GROUP_W, roundTop + ROUND_H),
                    ROUND_R, ROUND_R, Path.Direction.CW);
            mContinuePath.reset();
            mContinuePath.addRoundRect(
                    new RectF(leftX, halfTop, leftX + GROUP_W, halfBottom),
                    new float[] { ROUND_R, ROUND_R, ROUND_R, ROUND_R, 0f, 0f, 0f, 0f },
                    Path.Direction.CW);
            mContinuePath.op(fullRounded, Path.Op.INTERSECT);
            mContinuePath.op(screen, Path.Op.INTERSECT);

            mContinueLocal.set(mContinuePath);
            mContinueLocal.offset(-leftX, -halfTop);

            Region clip = new Region(0, 0, (int) DESIGN, (int) DESIGN);
            mContinueRegion.setPath(mContinuePath, clip);
            mRetryPath.reset();
            mRetryRegion.setEmpty();
            return;
        }

        Path leftRounded = new Path();
        leftRounded.addRoundRect(
                new RectF(leftX, roundTop, leftX + ROUND_W, roundTop + ROUND_H),
                ROUND_R, ROUND_R, Path.Direction.CW);
        Path rightRounded = new Path();
        rightRounded.addRoundRect(
                new RectF(leftX + GAP, roundTop, leftX + GAP + ROUND_W, roundTop + ROUND_H),
                ROUND_R, ROUND_R, Path.Direction.CW);

        mRetryPath.reset();
        mRetryPath.addRoundRect(
                new RectF(leftX, halfTop, leftX + SHARP_W, halfBottom),
                new float[] { 0f, 0f, INNER_TOP_R, INNER_TOP_R, 0f, 0f, 0f, 0f },
                Path.Direction.CW);
        mRetryPath.op(leftRounded, Path.Op.INTERSECT);
        mRetryPath.op(screen, Path.Op.INTERSECT);

        mContinuePath.reset();
        mContinuePath.addRoundRect(
                new RectF(rightX, halfTop, rightX + SHARP_W, halfBottom),
                new float[] { INNER_TOP_R, INNER_TOP_R, 0f, 0f, 0f, 0f, 0f, 0f },
                Path.Direction.CW);
        mContinuePath.op(rightRounded, Path.Op.INTERSECT);
        mContinuePath.op(screen, Path.Op.INTERSECT);

        mRetryLocal.set(mRetryPath);
        mRetryLocal.offset(-leftX, -halfTop);
        mContinueLocal.set(mContinuePath);
        mContinueLocal.offset(-rightX, -halfTop);

        Region clip = new Region(0, 0, (int) DESIGN, (int) DESIGN);
        mRetryRegion.setPath(mRetryPath, clip);
        mContinueRegion.setPath(mContinuePath, clip);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (GlassBackdrop.isCapturing()) {
            return;
        }
        if (mRetryPath.isEmpty() && mContinuePath.isEmpty()) {
            return;
        }
        if (mSingleButton) {
            drawGlass(canvas, mSingleGlass, mContinueLocal, GROUP_X, GROUP_Y, GROUP_W);

            int save = canvas.save();
            canvas.translate(mOffsetX, mOffsetY);
            canvas.scale(mScale, mScale);
            mTextPaint.setTextSize(TEXT_SIZE);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            float textBaseline = TEXT_Y + (TEXT_H - fm.ascent - fm.descent) / 2f;
            canvas.drawText(mSingleLabel, GROUP_CENTER_X, textBaseline, mTextPaint);
            canvas.restoreToCount(save);
            return;
        }

        drawGlass(canvas, mRetryGlass, mRetryLocal, GROUP_X, GROUP_Y, SHARP_W);
        drawGlass(canvas, mContinueGlass, mContinueLocal, GROUP_X + SHARP_W + GAP, GROUP_Y, SHARP_W);

        int save = canvas.save();
        canvas.translate(mOffsetX, mOffsetY);
        canvas.scale(mScale, mScale);
        mTextPaint.setTextSize(TEXT_SIZE);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textBaseline = TEXT_Y + (TEXT_H - fm.ascent - fm.descent) / 2f;
        canvas.drawText(mRetryLabel, RETRY_TEXT_X, textBaseline, mTextPaint);
        canvas.drawText(mContinueLabel, CONTINUE_TEXT_X, textBaseline, mTextPaint);
        canvas.restoreToCount(save);
    }

    private void drawGlass(Canvas canvas, GlassController glass, Path localDesign,
                           float designLeft, float designTop, float designWidth) {
        final float s = mScale;
        final float vx = mOffsetX + designLeft * s;
        final float vy = mOffsetY + designTop * s;
        final float vw = designWidth * s;
        final float vh = HALF_H * s;
        mDrawPath.set(localDesign);
        if (s != 1f) {
            mScaleMatrix.setScale(s, s);
            mDrawPath.transform(mScaleMatrix);
        }
        int save = canvas.save();
        canvas.translate(vx, vy);
        glass.renderer().draw(canvas, vw, vh, mDrawPath, vx, vy);
        canvas.restoreToCount(save);
    }

    private void drawFillAndStroke(Canvas canvas, Path shapePath, float width, float height,
                                   int color0, int color100) {
        shapePath.computeBounds(mGradBounds, true);
        float top = mGradBounds.top;
        float bottom = mGradBounds.bottom;
        if (bottom <= top) {
            top = 0f;
            bottom = height;
        }
        // 0% 在内弧（上深），100% 在表圈（下亮），渐变铺在可见高度上。
        mFillPaint.setShader(new LinearGradient(
                0f, top, 0f, bottom, color0, color100, Shader.TileMode.CLAMP));
        canvas.drawPath(shapePath, mFillPaint);
        mStrokePaint.setShader(new LinearGradient(
                0f, top, 0f, bottom, STROKE_TOP, STROKE_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawPath(shapePath, mStrokePaint);
    }

    private GlassStyle buildStyle(boolean retry) {
        final GlassStyle.Builder builder = GlassStyle.builder()
                .noSurface()
                .onDrawSurface(retry ? mRetrySurface : mContinueSurface)
                .refraction(0f)
                .depth(retry ? 0.60f : 0.50f)
                .dispersion(0.50f)
                .depthEffect(false)
                .pressTint(0f, 0f);
        if (retry) {
            builder.corners(ROUND_R, INNER_TOP_R, 0f, 0f)
                    .highlight(GlassHighlight.direction(
                            10f, GlassHighlight.alphaWhite(0.44f), 1.5f));
        } else {
            builder.corners(INNER_TOP_R, ROUND_R, 0f, 0f)
                    .highlight(GlassHighlight.direction(
                            45f, GlassHighlight.alphaWhite(0.31f), 1.5f));
        }
        return builder.build();
    }

    /** 单按钮样式：两侧大圆角，绿色渐变填充，高光居中偏上。 */
    private GlassStyle buildSingleStyle() {
        return GlassStyle.builder()
                .noSurface()
                .onDrawSurface(mContinueSurface)
                .refraction(0f)
                .depth(0.50f)
                .dispersion(0.50f)
                .depthEffect(false)
                .pressTint(0f, 0f)
                .corners(ROUND_R, ROUND_R, 0f, 0f)
                .highlight(GlassHighlight.direction(
                        0f, GlassHighlight.alphaWhite(0.31f), 1.5f))
                .build();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - mOffsetX) / mScale;
        float y = (event.getY() - mOffsetY) / mScale;
        boolean onRetry = mRetryRegion.contains((int) x, (int) y);
        boolean onContinue = mContinueRegion.contains((int) x, (int) y);
        if (!onRetry && !onContinue) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mSingleButton) {
                if (onContinue && mSingleClickListener != null) {
                    mSingleClickListener.onClick(this);
                }
            } else if (onRetry && mRetryEnabled && mRetryClickListener != null) {
                mRetryClickListener.onClick(this);
            } else if (onContinue && mContinueEnabled && mContinueClickListener != null) {
                mContinueClickListener.onClick(this);
            }
        }
        return true;
    }
}
