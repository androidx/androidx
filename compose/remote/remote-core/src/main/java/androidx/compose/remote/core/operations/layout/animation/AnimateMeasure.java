/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.core.operations.layout.animation;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.FloatFunctionDefine;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.DecoratorComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Basic interpolation manager between two ComponentMeasures
 *
 * <p>Handles position, size and visibility
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimateMeasure {
    protected long mStartTime;
    protected final @NonNull Component mComponent;
    protected final @NonNull ComponentMeasure mOriginal;
    protected final @NonNull ComponentMeasure mTarget;
    protected float mDuration;
    protected float mDurationVisibilityChange = mDuration;
    protected AnimationSpec.@NonNull ANIMATION mEnterAnimation = AnimationSpec.ANIMATION.FADE_IN;
    protected AnimationSpec.@NonNull ANIMATION mExitAnimation = AnimationSpec.ANIMATION.FADE_OUT;
    protected AnimationSpec.@NonNull SEQUENCE mEnterSequence = AnimationSpec.SEQUENCE.CONCURRENT;
    protected AnimationSpec.@NonNull SEQUENCE mExitSequence = AnimationSpec.SEQUENCE.CONCURRENT;
    protected int mEnterFunctionId = -1;
    protected int mExitFunctionId = -1;
    protected int mMotionEasingType = GeneralEasing.CUBIC_STANDARD;
    protected int mVisibilityEasingType = GeneralEasing.CUBIC_ACCELERATE;

    protected float mP = 0f;
    protected float mVp = 0f;
    protected long mLastElapsed = 0L;

    @NonNull
    protected FloatAnimation mMotionEasing =
            new FloatAnimation(mMotionEasingType, mDuration / 1000f, null, 0f, Float.NaN);

    @NonNull
    protected FloatAnimation mVisibilityEasing =
            new FloatAnimation(
                    mVisibilityEasingType, mDurationVisibilityChange / 1000f, null, 0f, Float.NaN);

    @Nullable protected ParticleAnimation mParticleAnimation;

    public AnimateMeasure(
            long startTime,
            @NonNull Component component,
            @NonNull ComponentMeasure original,
            @NonNull ComponentMeasure target,
            float duration,
            float durationVisibilityChange,
            AnimationSpec.@NonNull ANIMATION enterAnimation,
            AnimationSpec.@NonNull ANIMATION exitAnimation,
            int motionEasingType,
            int visibilityEasingType,
            int enterFunctionId,
            int exitFunctionId,
            AnimationSpec.@NonNull SEQUENCE enterSequence,
            AnimationSpec.@NonNull SEQUENCE exitSequence) {
        this.mStartTime = startTime;
        this.mComponent = component;
        this.mOriginal = original;
        this.mTarget = target;
        this.mDuration = duration;
        this.mDurationVisibilityChange = durationVisibilityChange;
        this.mEnterAnimation = enterAnimation;
        this.mExitAnimation = exitAnimation;
        this.mMotionEasingType = motionEasingType;
        this.mVisibilityEasingType = visibilityEasingType;
        this.mEnterFunctionId = enterFunctionId;
        this.mExitFunctionId = exitFunctionId;
        this.mEnterSequence = enterSequence;
        this.mExitSequence = exitSequence;

        float motionDuration = mDuration / 1000f;
        float visibilityDuration = mDurationVisibilityChange / 1000f;

        mMotionEasing = new FloatAnimation(mMotionEasingType, motionDuration, null, 0f, Float.NaN);
        mVisibilityEasing =
                new FloatAnimation(mVisibilityEasingType, visibilityDuration, null, 0f, Float.NaN);

        mMotionEasing.setTargetValue(1f);
        mVisibilityEasing.setTargetValue(1f);

        if (!isBeforeLayout()) {
            component.mVisibility = target.getVisibility();
        }
    }

    public AnimateMeasure(
            long startTime,
            @NonNull Component component,
            @NonNull ComponentMeasure original,
            @NonNull ComponentMeasure target,
            float duration,
            float durationVisibilityChange,
            AnimationSpec.@NonNull ANIMATION enterAnimation,
            AnimationSpec.@NonNull ANIMATION exitAnimation,
            int motionEasingType,
            int visibilityEasingType,
            int enterFunctionId,
            int exitFunctionId) {
        this(
                startTime,
                component,
                original,
                target,
                duration,
                durationVisibilityChange,
                enterAnimation,
                exitAnimation,
                motionEasingType,
                visibilityEasingType,
                enterFunctionId,
                exitFunctionId,
                AnimationSpec.SEQUENCE.CONCURRENT,
                AnimationSpec.SEQUENCE.CONCURRENT);
    }

    public AnimateMeasure(
            long startTime,
            @NonNull Component component,
            @NonNull ComponentMeasure original,
            @NonNull ComponentMeasure target,
            float duration,
            float durationVisibilityChange,
            AnimationSpec.@NonNull ANIMATION enterAnimation,
            AnimationSpec.@NonNull ANIMATION exitAnimation,
            int motionEasingType,
            int visibilityEasingType) {
        this(
                startTime,
                component,
                original,
                target,
                duration,
                durationVisibilityChange,
                enterAnimation,
                exitAnimation,
                motionEasingType,
                visibilityEasingType,
                -1,
                -1,
                AnimationSpec.SEQUENCE.CONCURRENT,
                AnimationSpec.SEQUENCE.CONCURRENT);
    }

    /** Returns true if this animation transitions the component to GONE or INVISIBLE. */
    public boolean isExitTransition() {
        return mOriginal.getVisibility() != mTarget.getVisibility()
                && (mTarget.isGone() || mTarget.isInvisible());
    }

    /** Returns true if this animation transitions the component
     * from GONE or INVISIBLE to VISIBLE. */
    public boolean isEnterTransition() {
        return mOriginal.getVisibility() != mTarget.getVisibility()
                && (mOriginal.isGone() || mOriginal.isInvisible())
                && mTarget.isVisible();
    }

    /**
     * Returns true if the visibility transition is configured to run before the layout change
     * ({@link AnimationSpec.SEQUENCE#BEFORE}).
     */
    public boolean isBeforeLayout() {
        if (isExitTransition()) {
            return mExitSequence == AnimationSpec.SEQUENCE.BEFORE;
        } else if (isEnterTransition()) {
            return mEnterSequence == AnimationSpec.SEQUENCE.BEFORE;
        }
        return false;
    }

    /**
     * Returns true if the visibility transition is configured to run after the layout change
     * ({@link AnimationSpec.SEQUENCE#AFTER}).
     */
    public boolean isAfterLayout() {
        if (isExitTransition()) {
            return mExitSequence == AnimationSpec.SEQUENCE.AFTER;
        } else if (isEnterTransition()) {
            return mEnterSequence == AnimationSpec.SEQUENCE.AFTER;
        }
        return false;
    }

    private void executeCustomAnimation(
            @NonNull PaintContext context, int functionId, float progress) {
        if (functionId == -1) {
            mComponent.paintingComponent(context);
            return;
        }
        RemoteContext remoteContext = context.getContext();
        Object obj = remoteContext.getObject(functionId);
        if (obj instanceof FloatFunctionDefine) {
            FloatFunctionDefine fn = (FloatFunctionDefine) obj;
            int[] args = fn.getArgs();
            float[] prevArgs = null;
            int prevIdInt = 0;
            if (fn.getExecutionDepth() > 0) {
                prevArgs = new float[args.length];
                for (int i = 0; i < args.length; i++) {
                    prevArgs[i] = remoteContext.getFloat(args[i]);
                }
                prevIdInt = args.length >= 6 ? remoteContext.getInteger(args[5]) : 0;
            }

            float x = (float) Math.floor(mComponent.getX());
            float y = (float) Math.floor(mComponent.getY());
            float w = (float) Math.ceil((mComponent.getX() - x) + mComponent.getWidth());
            float h = (float) Math.ceil((mComponent.getY() - y) + mComponent.getHeight());
            if (args.length >= 1) remoteContext.loadFloat(args[0], progress);
            if (args.length >= 2) remoteContext.loadFloat(args[1], w);
            if (args.length >= 3) remoteContext.loadFloat(args[2], h);
            if (args.length >= 4) remoteContext.loadFloat(args[3], x);
            if (args.length >= 5) remoteContext.loadFloat(args[4], y);
            if (args.length >= 6) {
                remoteContext.loadInteger(args[5], mComponent.getComponentId());
                remoteContext.loadFloat(args[5], mComponent.getComponentId());
            }

            Component prev = remoteContext.mLastComponent;
            remoteContext.mLastComponent = mComponent;
            androidx.compose.remote.core.operations.BitmapData.pushOffscreenScope(remoteContext);
            context.save();
            context.savePaint();
            paint.reset();
            paint.setColor(0f, 0f, 0f, 1f);
            paint.setStyle(PaintBundle.STYLE_FILL);
            context.applyPaint(paint);
            try {
                fn.execute(remoteContext);
            } finally {
                context.restorePaint();
                context.restore();
                androidx.compose.remote.core.operations.BitmapData.popOffscreenScope(remoteContext);
                remoteContext.mLastComponent = prev;
                if (prevArgs != null) {
                    for (int i = 0; i < args.length; i++) {
                        remoteContext.loadFloat(args[i], prevArgs[i]);
                    }
                    if (args.length >= 6) {
                        remoteContext.loadInteger(args[5], prevIdInt);
                    }
                }
            }
        } else {
            mComponent.paintingComponent(context);
        }
    }

    /**
     * Update the current bounds/visibility/etc given the current time
     *
     * @param currentTime the time we use to evaluate the animation
     */
    public void update(long currentTime) {
        mLastElapsed = currentTime - mStartTime;
        float motionTimeInSeconds = mLastElapsed / 1000f;
        mP = mMotionEasing.get(motionTimeInSeconds);
        if (isAfterLayout()) {
            if (mLastElapsed < mDuration) {
                mVp = 0f;
            } else {
                float visibilityTimeInSeconds = (mLastElapsed - mDuration) / 1000f;
                mVp = mVisibilityEasing.get(visibilityTimeInSeconds);
            }
        } else {
            float visibilityTimeInSeconds = mLastElapsed / 1000f;
            mVp = mVisibilityEasing.get(visibilityTimeInSeconds);
        }
    }

    @NonNull public PaintBundle paint = new PaintBundle();

    /** Apply the layout portion of the animation if any */
    public void apply(@NonNull RemoteContext context) {
        update(context.currentTime);
        mComponent.setX(getX());
        mComponent.setY(getY());
        mComponent.setWidth(getWidth());
        mComponent.setHeight(getHeight());
        if (isDone()) {
            mComponent.mVisibility = mTarget.getVisibility();
        }
        mComponent.updateVariables(context);

        float w = mComponent.getWidth();
        float h = mComponent.getHeight();
        for (Operation op : mComponent.mList) {
            if (op instanceof PaddingModifierOperation) {
                PaddingModifierOperation pop = (PaddingModifierOperation) op;
                w -= pop.getLeft() + pop.getRight();
                h -= pop.getTop() + pop.getBottom();
            }
            if (op instanceof DecoratorComponent) {
                ((DecoratorComponent) op).layout(context, mComponent, w, h);
            }
        }
    }

    /** Paint the transition animation for the component owned */
    public void paint(@NonNull PaintContext context) {
        apply(context.getContext());
        if (mOriginal.getVisibility() != mTarget.getVisibility()) {
            if (isEnterTransition() && isAfterLayout() && mLastElapsed < mDuration) {
                return;
            }
            if (isExitTransition() && isAfterLayout() && mLastElapsed < mDuration) {
                mComponent.paintingComponent(context);
                return;
            }
            if (mTarget.isGone() || mTarget.isInvisible()) {
                switch (mExitAnimation) {
                    case CUSTOM:
                        executeCustomAnimation(context, mExitFunctionId, mVp);
                        break;
                    case PARTICLE:
                        // particleAnimation(context, component, original, target, vp)
                        if (mParticleAnimation == null) {
                            mParticleAnimation = new ParticleAnimation();
                        }
                        mParticleAnimation.animate(context, mComponent, mOriginal, mTarget, mVp);
                        break;
                    case FADE_OUT:
                        context.save();
                        context.savePaint();
                        paint.reset();
                        paint.setColor(0f, 0f, 0f, 1f - mVp);
                        context.applyPaint(paint);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restorePaint();
                        context.restore();
                        break;
                    case SLIDE_LEFT:
                        context.save();
                        context.translate(-mVp * mComponent.getParent().getWidth(), 0f);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    case SLIDE_RIGHT:
                        context.save();
                        context.savePaint();
                        paint.reset();
                        paint.setColor(0f, 0f, 0f, 1f);
                        context.applyPaint(paint);
                        context.translate(mVp * mComponent.getParent().getWidth(), 0f);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restorePaint();
                        context.restore();
                        break;
                    case SLIDE_TOP:
                        context.save();
                        context.translate(0f, -mVp * mComponent.getParent().getHeight());
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    case SLIDE_BOTTOM:
                        context.save();
                        context.translate(0f, mVp * mComponent.getParent().getHeight());
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    default:
                        //            particleAnimation(context, component, original, target, vp)
                        if (mParticleAnimation == null) {
                            mParticleAnimation = new ParticleAnimation();
                        }
                        mParticleAnimation.animate(context, mComponent, mOriginal, mTarget, mVp);
                        break;
                }
            } else if ((mOriginal.isGone() || mOriginal.isInvisible()) && mTarget.isVisible()) {
                switch (mEnterAnimation) {
                    case CUSTOM:
                        executeCustomAnimation(context, mEnterFunctionId, mVp);
                        break;
                    case ROTATE:
                        float px = mTarget.getX() + mTarget.getW() / 2f;
                        float py = mTarget.getY() + mTarget.getH() / 2f;

                        context.save();
                        context.savePaint();
                        context.matrixRotate(mVp * 360f, px, py);
                        context.matrixScale(1f * mVp, 1f * mVp, px, py);
                        paint.reset();
                        paint.setColor(0f, 0f, 0f, mVp);
                        context.applyPaint(paint);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restorePaint();
                        context.restore();
                        break;
                    case FADE_IN:
                        context.save();
                        context.savePaint();
                        paint.reset();
                        paint.setColor(0f, 0f, 0f, mVp);
                        context.applyPaint(paint);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restorePaint();
                        context.restore();
                        break;
                    case SLIDE_LEFT:
                        context.save();
                        context.translate((1f - mVp) * mComponent.getParent().getWidth(), 0f);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    case SLIDE_RIGHT:
                        context.save();
                        context.translate(-(1f - mVp) * mComponent.getParent().getWidth(), 0f);
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    case SLIDE_TOP:
                        context.save();
                        context.translate(0f, (1f - mVp) * mComponent.getParent().getHeight());
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    case SLIDE_BOTTOM:
                        context.save();
                        context.translate(0f, -(1f - mVp) * mComponent.getParent().getHeight());
                        context.saveLayer(
                                mComponent.getX(),
                                mComponent.getY(),
                                mComponent.getWidth(),
                                mComponent.getHeight());
                        mComponent.paintingComponent(context);
                        context.restore();
                        context.restore();
                        break;
                    default:
                        break;
                }
            } else {
                mComponent.paintingComponent(context);
            }
        } else if (mTarget.isVisible()) {
            mComponent.paintingComponent(context);
        }

        if (isDone()) {
            mComponent.mVisibility = mTarget.getVisibility();
            mComponent.setX(mTarget.getX());
            mComponent.setY(mTarget.getY());
            mComponent.setWidth(mTarget.getW());
            mComponent.setHeight(mTarget.getH());
        }
    }

    public boolean isDone() {
        if (isBeforeLayout()) {
            return mVp >= 1f;
        }
        if (isAfterLayout()) {
            return mP >= 1f && mVp >= 1f && mLastElapsed >= mDuration;
        }
        return mP >= 1f && mVp >= 1f;
    }

    public float getX() {
        if (mOriginal.isGone()) {
            return mTarget.getX();
        }
        if (mTarget.isGone()) {
            return mOriginal.getX();
        }
        return mOriginal.getX() * (1 - mP) + mTarget.getX() * mP;
    }

    public float getY() {
        if (mOriginal.isGone()) {
            return mTarget.getY();
        }
        if (mTarget.isGone()) {
            return mOriginal.getY();
        }
        return mOriginal.getY() * (1 - mP) + mTarget.getY() * mP;
    }

    public float getWidth() {
        if (mOriginal.isGone()) {
            return mTarget.getW();
        }
        if (mTarget.isGone()) {
            return mOriginal.getW();
        }
        return mOriginal.getW() * (1 - mP) + mTarget.getW() * mP;
    }

    public float getHeight() {
        if (mOriginal.isGone()) {
            return mTarget.getH();
        }
        if (mTarget.isGone()) {
            return mOriginal.getH();
        }
        return mOriginal.getH() * (1 - mP) + mTarget.getH() * mP;
    }

    /**
     * Returns the visibility for this measure
     *
     * @return the current visibility (possibly interpolated)
     */
    public float getVisibility() {
        if (mOriginal.getVisibility() == mTarget.getVisibility()) {
            return 1f;
        } else if (mTarget.isVisible()) {
            return mVp;
        } else {
            return 1 - mVp;
        }
    }

    /**
     * Set the target values from the given measure
     *
     * @param context the current context
     * @param measure the target measure
     * @param currentTime the current time
     */
    public void updateTarget(
            @NonNull RemoteContext context, @NonNull ComponentMeasure measure, long currentTime) {
        float currentX = getX();
        float currentY = getY();
        float currentW = getWidth();
        float currentH = getHeight();

        mOriginal.setX(currentX);
        mOriginal.setY(currentY);
        mOriginal.setW(currentW);
        mOriginal.setH(currentH);

        float targetX = measure.getX();
        float targetY = measure.getY();
        float targetW = measure.getW();
        float targetH = measure.getH();
        int targetVisibility = measure.getVisibility();
        if (mTarget.getX() != targetX
                || mTarget.getY() != targetY
                || mTarget.getW() != targetW
                || mTarget.getH() != targetH
                || mTarget.getVisibility() != targetVisibility) {
            if (mTarget.getVisibility() != targetVisibility) {
                mOriginal.setVisibility(mTarget.getVisibility());
                mStartTime = currentTime;
            }
            mTarget.setX(targetX);
            mTarget.setY(targetY);
            mTarget.setW(targetW);
            mTarget.setH(targetH);
            mTarget.setVisibility(targetVisibility);
            if (!isBeforeLayout()) {
                mComponent.mVisibility = targetVisibility;
            }
            // We shouldn't reset the leftover animation time here
            // 1/ if we are eg fading out a component, and an updateTarget comes on, we don't want
            //    to restart the full animation time
            // 2/ if no visibility change but quick updates come in (eg live resize) it seems
            //    better as well to not restart the animation time and only allows the original
            //    time to wrap up
            // mStartTime = currentTime;
        }
    }

    public @NonNull ComponentMeasure getOriginal() {
        return mOriginal;
    }

    public @NonNull ComponentMeasure getTarget() {
        return mTarget;
    }

    @Override
    public String toString() {
        return "AnimateMeasure{isDone=" + isDone() + " origX=" + mOriginal.getX()
                + " targetX=" + mTarget.getX() + " mP=" + mP + "}";
    }
}
