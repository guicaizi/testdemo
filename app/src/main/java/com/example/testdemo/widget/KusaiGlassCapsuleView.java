// prize add by yanliang for 胶囊液态玻璃按钮测试控件, 20261023-start
package com.example.testdemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.kyant.liquidglass2.GlassBackdrop;
import com.kyant.liquidglass2.GlassController;
import com.kyant.liquidglass2.GlassHighlight;
import com.kyant.liquidglass2.GlassStyle;
import com.kyant.liquidglass2.GlassSurface;

/**
 * 胶囊液态玻璃按钮（测试用）：参考 KusaiPatternFooterView 单按钮模式的绘制配方，
 * 尺寸自适应宿主 View（不再用 480 蒙版坐标）。
 *
 * <p>表面 kusai 绿渐变 #0DDB58 0%→100%，白描边 31%→0%，
 * 折射 0 / 深度 50 / 色散 50，方向高光 0° 白 31%。
 */
public class KusaiGlassCapsuleView extends View {

    private static final int FILL_0 = 0x000DDB58;
    private static final int FILL_100 = 0xFF0DDB58;
    private static final int STROKE_TOP = 0x4FFFFFFF;
    private static final int STROKE_BOTTOM = 0x00FFFFFF;
    private static final int TEXT_COLOR = 0xE6FFFFFF;

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mGradBounds = new RectF();
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final GlassController mGlass = new GlassController();

    private CharSequence mLabel = "";

    private final GlassSurface mSurface =
            (canvas, shapePath, width, height) -> drawFillAndStroke(canvas, shapePath, width, height);

    public KusaiGlassCapsuleView(Context context) {
        super(context);
        init();
    }

    public KusaiGlassCapsuleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KusaiGlassCapsuleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(true);
        setBackground(null);
        mGlass.attach(this);
        mGlass.setStyle(buildStyle());
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(20f);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mFillPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(0.8f);
    }

    public void setLabel(CharSequence label) {
        mLabel = label == null ? "" : label;
        invalidate();
    }

    private GlassStyle buildStyle() {
        return GlassStyle.builder()
                .noSurface()
                .onDrawSurface(mSurface)
                .refraction(0f)
                .depth(0.5f)
                .dispersion(0.5f)
                .depthEffect(false)
                .pressTint(0f, 0f)
                .corners(26f, 26f, 26f, 26f)
                .highlight(GlassHighlight.direction(
                        0f, GlassHighlight.alphaWhite(0.31f), 1.5f))
                .build();
    }

    private void drawFillAndStroke(Canvas canvas, Path shapePath, float width, float height) {
        shapePath.computeBounds(mGradBounds, true);
        float top = mGradBounds.top;
        float bottom = mGradBounds.bottom;
        if (bottom <= top) {
            top = 0f;
            bottom = height;
        }
        // 0% 在内沿（上深），100% 在表圈（下亮）。
        mFillPaint.setShader(new LinearGradient(
                0f, top, 0f, bottom, FILL_0, FILL_100, Shader.TileMode.CLAMP));
        canvas.drawPath(shapePath, mFillPaint);
        mStrokePaint.setShader(new LinearGradient(
                0f, top, 0f, bottom, STROKE_TOP, STROKE_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawPath(shapePath, mStrokePaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mGlass.onAttached();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mGlass.onLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGlass.onDetached();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (GlassBackdrop.isCapturing()) {
            return;
        }
        // 玻璃本体（折射 + 表面渐变 + 描边 + 高光）
        mGlass.draw(canvas, getWidth(), getHeight());
        // 居中文本
        CharSequence text = mLabel;
        if (text == null || text.length() == 0) {
            return;
        }
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textY = (getHeight() - fm.ascent - fm.descent) / 2f;
        Paint.Align oldAlign = mTextPaint.getTextAlign();
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text.toString(), getWidth() / 2f, textY, mTextPaint);
        mTextPaint.setTextAlign(oldAlign);
    }
}
// prize add by yanliang for 胶囊液态玻璃按钮测试控件, 20261023-end
