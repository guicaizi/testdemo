// prize add by yanliang for 从 n06SystemUi 控制面板胶囊提取的独立控件, 20261023-start
package com.example.testdemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.kyant.liquidglass2.GlassBackdrop;
import com.kyant.liquidglass2.GlassController;
import com.kyant.liquidglass2.GlassHighlight;
import com.kyant.liquidglass2.GlassStyle;
import com.kyant.liquidglass2.GlassSurfaces;

/**
 * 控制面板「工作模式」胶囊的独立控件。
 *
 * <p>参数与 n06SystemUi 的 ControlPanelPillGlassStyle 完全一致，未做任何改动：
 * capsule / surfaceAlpha 0.15 / 竖向渐变 / refraction 0 / depthEffect false /
 * 45° 白 31% 1.5px 高光 / pressTint 0，0。
 *
 * <p>选中态渐变取自 GlassColors.primaryTop / primaryBottom（#0DDB58 25% → 100%），
 * 未选中态取自 qs_control_panel_volume_track_base / frosted。
 *
 * <p>继承 View 而不是 TextView/Button：软件画布抓背景时基类会干扰硬件显示列表，
 * 导致玻璃本体不显示（testdemo 里 GlassButton 踩过这个坑）。
 */
public class ControlPanelPillView extends View {

    /** 选中态：GlassColors.primaryTop */
    private static final int ACTIVE_TOP = 0x400DDB58;
    /** 选中态：GlassColors.primaryBottom */
    private static final int ACTIVE_BOTTOM = 0xFF0DDB58;
    /** 未选中态：qs_control_panel_volume_track_base */
    private static final int NEUTRAL_TOP = 0x1A777777;
    /** 未选中态：qs_control_panel_volume_track_frosted */
    private static final int NEUTRAL_BOTTOM = 0x4D111111;

    private final GlassController mGlass = new GlassController();
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private CharSequence mLabel = "";
    private boolean mActive = true;

    public ControlPanelPillView(Context context) {
        this(context, null);
    }

    public ControlPanelPillView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlPanelPillView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setClickable(true);
        setBackground(null);
        mGlass.attach(this);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        applyGlassStyle();
    }

    /** 选中态：true 用 #0DDB58 竖向渐变，false 用中性灰玻璃。 */
    public void setActive(boolean active) {
        if (mActive != active) {
            mActive = active;
            applyGlassStyle();
            invalidate();
        }
    }

    public boolean isActive() {
        return mActive;
    }

    public void setLabel(CharSequence label) {
        mLabel = label == null ? "" : label;
        invalidate();
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public void setLabelColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    /** 位置变化（例如拖动）后重算采样区域并重抓背景。 */
    public void refreshGlassBackdrop() {
        mGlass.onLayout();
        mGlass.invalidateBackdrop();
        invalidate();
    }

    private void applyGlassStyle() {
        final int top = mActive ? ACTIVE_TOP : NEUTRAL_TOP;
        final int bottom = mActive ? ACTIVE_BOTTOM : NEUTRAL_BOTTOM;
        mGlass.setStyle(GlassStyle.builder()
                .capsule()
                .surfaceAlpha(0.15f)
                .onDrawSurface(GlassSurfaces.vertical(top, bottom))
                .refraction(0f)
                .depthEffect(false)
                .highlight(GlassHighlight.direction(
                        45f, GlassHighlight.alphaWhite(0.31f), 1.5f))
                .pressTint(0f, 0f)
                .build());
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
    protected void onDraw(Canvas canvas) {
        if (GlassBackdrop.isCapturing()) {
            return;
        }
        mGlass.draw(canvas, getWidth(), getHeight());
        if (mLabel.length() > 0) {
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            float textY = (getHeight() - fm.ascent - fm.descent) / 2f;
            canvas.drawText(mLabel.toString(), getWidth() / 2f, textY, mTextPaint);
        }
    }
}
// prize add by yanliang for 从 n06SystemUi 控制面板胶囊提取的独立控件, 20261023-end
