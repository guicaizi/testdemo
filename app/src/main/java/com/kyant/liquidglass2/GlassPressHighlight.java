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

import android.view.MotionEvent;
import android.view.View;

/**
 * 跟手按压高光：按下时玻璃上亮起一片光斑，并跟着手指走，松手回弹。
 *
 * <p>对应 AndroidLiquidGlass 仓库 {@code app/.../catalog/utils/InteractiveHighlight.kt}。
 * 上游用 Compose 的 {@code Animatable(spring(0.5f, 300f))}，这里用同样的弹簧参数
 * 自己积分，不依赖任何动画框架。
 *
 * <h3>行为</h3>
 * <ul>
 *   <li>按下：{@code progress} 从 0 弹到 1（阻尼比 0.5，会略微过冲，手感更「弹」）；</li>
 *   <li>拖动：光斑位置实时跟随；</li>
 *   <li>抬起 / 取消：回弹到 0。</li>
 * </ul>
 *
 * <h3>注意</h3>
 * {@link #onTouch} 一律返回 {@code false}，也就是<b>不消费</b>事件——
 * 这样宿主自己的点击逻辑、子 View 的点击都不受影响。
 * 代价是宿主必须是 clickable 的（有 OnClickListener 或用 {@code setClickable(true)}），
 * 否则它不会收到后续的 MOVE / UP。
 */
public final class GlassPressHighlight implements View.OnTouchListener {

    /** 与上游 spring(dampingRatio = 0.5f, stiffness = 300f) 一致。 */
    private static final float STIFFNESS = 300f;
    private static final float DAMPING_RATIO = 0.5f;
    private static final float DAMPING = 2f * DAMPING_RATIO * (float) Math.sqrt(STIFFNESS);

    private static final float MAX_STEP_SECONDS = 1f / 60f;
    private static final float REST_DISTANCE = 0.002f;
    private static final float REST_VELOCITY = 0.01f;

    private final View mHost;
    private final GlassController mController;

    private final Runnable mAnimator = new Runnable() {
        @Override
        public void run() {
            mScheduled = false;
            step();
        }
    };

    private float mProgress;
    private float mVelocity;
    private float mTarget;
    private float mX;
    private float mY;
    private long mLastFrameNanos;
    private boolean mScheduled;
    private boolean mAttached;

    public GlassPressHighlight(View host, GlassController controller) {
        mHost = host;
        mController = controller;
    }

    /** 挂到宿主 View 上。宿主已有 OnTouchListener 时会被覆盖，请自行串联。 */
    public void attach() {
        if (mAttached) {
            return;
        }
        mAttached = true;
        mX = mHost.getWidth() * 0.5f;
        mY = mHost.getHeight() * 0.5f;
        mHost.setOnTouchListener(this);
        mHost.setClickable(true);
    }

    public void detach() {
        if (!mAttached) {
            return;
        }
        mAttached = false;
        mHost.removeCallbacks(mAnimator);
        mScheduled = false;
        mLastFrameNanos = 0L;
        mProgress = 0f;
        mVelocity = 0f;
        mTarget = 0f;
        mController.setPress(false, 0f, mX, mY);
        mHost.setOnTouchListener(null);
    }

    /** 当前按压强度，0..1（弹簧过冲时可能略大于 1）。 */
    public float getProgress() {
        return mProgress;
    }

    /** 光斑圆心（宿主局部坐标）。 */
    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    /** 直接推一个进度，用于外部动画（例如没有触摸事件的场景）。 */
    public void setProgressImmediate(float progress) {
        mProgress = progress;
        mTarget = progress;
        mVelocity = 0f;
        push();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();
                mTarget = 1f;
                mVelocity = 0f;
                push();
                start();
                break;
            case MotionEvent.ACTION_MOVE:
                mX = event.getX();
                mY = event.getY();
                push();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTarget = 0f;
                start();
                break;
            default:
                break;
        }
        // 不消费事件，宿主和子 View 的点击照常工作
        return false;
    }

    // ------------------------------------------------------------------

    private void start() {
        if (!mScheduled) {
            mLastFrameNanos = 0L;
            mScheduled = true;
            mHost.postOnAnimation(mAnimator);
        }
    }

    private void step() {
        final long now = System.nanoTime();
        float dt = mLastFrameNanos == 0L ? MAX_STEP_SECONDS : (now - mLastFrameNanos) / 1_000_000_000f;
        mLastFrameNanos = now;
        if (dt <= 0f) {
            dt = 0.0001f;
        }
        if (dt > 0.064f) {
            dt = 0.064f;
        }

        // 定步长积分，保证掉帧时弹簧不发散
        final int steps = Math.max(1, (int) Math.ceil(dt / MAX_STEP_SECONDS));
        final float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            final float acceleration = (mTarget - mProgress) * STIFFNESS - mVelocity * DAMPING;
            mVelocity += acceleration * h;
            mProgress += mVelocity * h;
        }
        if (mProgress < -0.5f) {
            mProgress = -0.5f;
            mVelocity = 0f;
        } else if (mProgress > 1.5f) {
            mProgress = 1.5f;
            mVelocity = 0f;
        }

        push();

        final boolean settled = Math.abs(mTarget - mProgress) < REST_DISTANCE
                && Math.abs(mVelocity) < REST_VELOCITY;
        if (settled) {
            mProgress = mTarget;
            mVelocity = 0f;
            push();
        } else {
            start();
        }
    }

    private void push() {
        mController.setPress(mProgress > 0.001f, mProgress, mX, mY);
        mHost.invalidate();
    }
}
