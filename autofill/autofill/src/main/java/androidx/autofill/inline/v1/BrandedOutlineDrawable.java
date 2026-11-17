/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.autofill.inline.v1;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;

import androidx.annotation.RequiresApi;
import androidx.autofill.R;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Draws the animated brand outline of an inline suggestion. */
@RequiresApi(api = Build.VERSION_CODES.R)
final class BrandedOutlineDrawable extends Drawable implements Animatable {
    private static final float ROTATION_START_DEG = 35f;
    private static final float ROTATION_DEGREES = 720f;
    private static final long ROTATION_DURATION_MS = 5000L;
    private static final long ROTATION_START_DELAY_MS = 1000L;
    private static final long FADE_DURATION_MS = 500L;
    private static final long FADE_START_DELAY_MS = 5000L;
    // The stroke is centered on the chip's edge and clipped to the chip, so half of it shows.
    private static final float STROKE_WIDTH_DP = 2f;
    private static final float EMPHASIZED_STROKE_WIDTH_DP = 4f;
    private static final float GLOW_RADIUS_LIGHT_DP = 8f;
    private static final float GLOW_RADIUS_DARK_DP = 7f;
    private static final int FALLBACK_OUTLINE_LIGHT_COLOR = 0xFFB0B1BC;
    private static final int FALLBACK_OUTLINE_DARK_COLOR = 0xFF454850;
    private static final float[] GRADIENT_POSITIONS = new float[] { 0f, 0.5f, 1f };

    private final int @NonNull [] mBrandColors;
    private final int @NonNull [] mGradientColors;
    private final boolean mIsNightMode;
    private final float mCornerRadius;
    private final float mOutlineStrokeWidth;
    private final float mEmphasizedOutlineStrokeWidth;
    private final float mInsetLeft;
    private final float mInsetTop;
    private final float mInsetRight;
    private final float mInsetBottom;
    private final Path mButtonShape = new Path();
    private final RectF mBoundsF = new RectF();
    private final Matrix mGradientMatrix = new Matrix();
    private final Paint mOutlineGradientPaint;
    private final Paint mOutlineStaticPaint;
    private final @Nullable RenderNode mInnerGlow;
    private final ValueAnimator mGradientAnimator;
    private final ValueAnimator mFadeAnimator;

    private float mRotationAngle = ROTATION_START_DEG;
    private int mInnerGlowAlpha = 255;
    private int mOutlineAlpha = 255;

    BrandedOutlineDrawable(@NonNull Context context, @Nullable Drawable existingBackground,
            int @NonNull [] brandColors) {
        mBrandColors = new int[] { brandColors[0], brandColors[1] };
        mGradientColors = new int[] { brandColors[0], brandColors[1], brandColors[0] };
        final Resources res = context.getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();
        mIsNightMode = (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        mCornerRadius = resolveCornerRadius(existingBackground,
                res.getDimension(R.dimen.autofill_inline_suggestion_chip_corner_radius));
        mOutlineStrokeWidth = dpToPx(metrics, STROKE_WIDTH_DP);
        mEmphasizedOutlineStrokeWidth = dpToPx(metrics, EMPHASIZED_STROKE_WIDTH_DP);

        final float defaultInset =
                res.getDimension(R.dimen.autofill_inline_suggestion_chip_inset);
        if (existingBackground instanceof LayerDrawable
                && ((LayerDrawable) existingBackground).getNumberOfLayers() > 0) {
            final LayerDrawable layerDrawable = (LayerDrawable) existingBackground;
            final int insetLeft = layerDrawable.getLayerInsetLeft(0);
            final int insetStart = layerDrawable.getLayerInsetStart(0);
            final int insetRight = layerDrawable.getLayerInsetRight(0);
            final int insetEnd = layerDrawable.getLayerInsetEnd(0);
            mInsetLeft = insetLeft != 0 ? insetLeft
                    : (insetStart != LayerDrawable.INSET_UNDEFINED ? insetStart : defaultInset);
            mInsetRight = insetRight != 0 ? insetRight
                    : (insetEnd != LayerDrawable.INSET_UNDEFINED ? insetEnd : defaultInset);
            mInsetTop = layerDrawable.getLayerInsetTop(0);
            mInsetBottom = layerDrawable.getLayerInsetBottom(0);
        } else {
            mInsetLeft = defaultInset;
            mInsetTop = defaultInset;
            mInsetRight = defaultInset;
            mInsetBottom = defaultInset;
        }

        mOutlineGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlineGradientPaint.setStyle(Paint.Style.STROKE);
        mOutlineGradientPaint.setStrokeWidth(mOutlineStrokeWidth);

        mOutlineStaticPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlineStaticPaint.setStyle(Paint.Style.STROKE);
        mOutlineStaticPaint.setStrokeWidth(mOutlineStrokeWidth);
        mOutlineStaticPaint.setColor(resolveRestingOutlineColor(context, mIsNightMode));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final float blurRadius = dpToPx(metrics,
                    mIsNightMode ? GLOW_RADIUS_DARK_DP : GLOW_RADIUS_LIGHT_DP);
            mInnerGlow = Api31Impl.createBlurredRenderNode(blurRadius);
        } else {
            mInnerGlow = null;
        }

        mGradientAnimator = ValueAnimator.ofFloat(0f, 1f);
        mGradientAnimator.setDuration(ROTATION_DURATION_MS);
        mGradientAnimator.setStartDelay(ROTATION_START_DELAY_MS);
        mGradientAnimator.setInterpolator(new LinearInterpolator());
        mGradientAnimator.addUpdateListener(animator -> {
            final float animatedValue = (float) animator.getAnimatedValue();
            mRotationAngle = ROTATION_START_DEG + animatedValue * ROTATION_DEGREES;
            invalidateSelf();
        });

        mFadeAnimator = ValueAnimator.ofFloat(0f, 1f);
        mFadeAnimator.setDuration(FADE_DURATION_MS);
        mFadeAnimator.setStartDelay(FADE_START_DELAY_MS);
        mFadeAnimator.addUpdateListener(animator -> {
            final float progress = (float) animator.getAnimatedValue();
            final int alpha = (int) ((1f - progress) * 255f);
            mInnerGlowAlpha = alpha;
            mOutlineAlpha = alpha;
            invalidateSelf();
        });
    }

    int @NonNull [] getBrandColors() {
        return mBrandColors.clone();
    }

    @Override
    public void start() {
        mInnerGlowAlpha = 255;
        mOutlineAlpha = 255;
        mGradientAnimator.start();
        mFadeAnimator.start();
    }

    @Override
    public void stop() {
        mGradientAnimator.cancel();
        mFadeAnimator.cancel();
    }

    @Override
    public boolean isRunning() {
        return mGradientAnimator.isRunning() || mFadeAnimator.isRunning();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        mBoundsF.set(
                bounds.left + mInsetLeft,
                bounds.top + mInsetTop,
                bounds.right - mInsetRight,
                bounds.bottom - mInsetBottom);
        if (mBoundsF.width() <= 0f || mBoundsF.height() <= 0f) {
            return;
        }

        if (mInnerGlow != null) {
            mInnerGlow.setPosition(0, 0, bounds.width(), bounds.height());
            final Canvas glowCanvas = mInnerGlow.beginRecording(
                    bounds.width(), bounds.height());
            try {
                final float strokeWidth = mIsNightMode
                        ? mEmphasizedOutlineStrokeWidth : mOutlineStrokeWidth;
                drawAnimatedOutline(glowCanvas, mBoundsF, mInnerGlowAlpha, strokeWidth);
            } finally {
                mInnerGlow.endRecording();
            }
        }

        final int saveCount = canvas.save();
        try {
            mButtonShape.reset();
            mButtonShape.addRoundRect(
                    mBoundsF, mCornerRadius, mCornerRadius, Path.Direction.CW);
            canvas.clipPath(mButtonShape);
            final int restingOutlineAlpha = Math.max(0, Math.min(255, 255 - mOutlineAlpha));
            if (restingOutlineAlpha > 0) {
                mOutlineStaticPaint.setAlpha(restingOutlineAlpha);
                canvas.drawPath(mButtonShape, mOutlineStaticPaint);
            }
            if (mOutlineAlpha > 0) {
                drawAnimatedOutline(canvas, mBoundsF, mOutlineAlpha, mOutlineStrokeWidth);
            }
            if (mInnerGlow != null && canvas.isHardwareAccelerated()) {
                canvas.drawRenderNode(mInnerGlow);
            }
        } finally {
            canvas.restoreToCount(saveCount);
        }
    }

    private void drawAnimatedOutline(@NonNull Canvas canvas, @NonNull RectF boundsF,
            int alpha, float strokeWidth) {
        mButtonShape.reset();
        mButtonShape.addRoundRect(boundsF, mCornerRadius, mCornerRadius, Path.Direction.CW);

        final LinearGradient gradientShader = new LinearGradient(
                boundsF.left,
                boundsF.top,
                boundsF.right,
                boundsF.bottom,
                mGradientColors,
                GRADIENT_POSITIONS,
                Shader.TileMode.CLAMP);
        mGradientMatrix.setRotate(mRotationAngle, boundsF.centerX(), boundsF.centerY());
        gradientShader.setLocalMatrix(mGradientMatrix);

        mOutlineGradientPaint.setShader(gradientShader);
        mOutlineGradientPaint.setAlpha(alpha);
        mOutlineGradientPaint.setStrokeWidth(strokeWidth);
        canvas.drawPath(mButtonShape, mOutlineGradientPaint);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mOutlineGradientPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mOutlineGradientPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private static float dpToPx(@NonNull DisplayMetrics metrics, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    private static int resolveRestingOutlineColor(
            @NonNull Context context, boolean isNightMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return context.getColor(isNightMode
                    ? android.R.color.system_outline_variant_dark
                    : android.R.color.system_outline_variant_light);
        }
        return isNightMode ? FALLBACK_OUTLINE_DARK_COLOR : FALLBACK_OUTLINE_LIGHT_COLOR;
    }

    static float resolveCornerRadius(@Nullable Drawable background, float fallback) {
        Drawable shape = background;
        if (shape instanceof LayerDrawable
                && ((LayerDrawable) shape).getNumberOfLayers() > 0) {
            shape = ((LayerDrawable) shape).getDrawable(0);
        }
        if (!(shape instanceof GradientDrawable)) {
            return fallback;
        }
        return ((GradientDrawable) shape).getCornerRadius();
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Api31Impl {
        private Api31Impl() {
        }

        static @NonNull RenderNode createBlurredRenderNode(float blurRadius) {
            final RenderNode node = new RenderNode("innerGlow");
            node.setRenderEffect(RenderEffect.createBlurEffect(
                    blurRadius, blurRadius, Shader.TileMode.MIRROR));
            return node;
        }
    }
}
