/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.animation.ValueAnimator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationParameters;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.CubicBezierEasing;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Easing;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.RepeatMode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Helper class for Animations in ProtoLayout. It contains helper methods used in rendered and
 * constants for default values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimationsHelper {

    private static final Duration DEFAULT_ANIM_DURATION = Duration.ofMillis(300);
    private static final Interpolator DEFAULT_ANIM_INTERPOLATOR = new LinearInterpolator();
    private static final Duration DEFAULT_ANIM_DELAY = Duration.ZERO;
    private static final int DEFAULT_REPEAT_COUNT = 0;
    private static final int DEFAULT_REPEAT_MODE = ValueAnimator.RESTART;
    private static final Map<RepeatMode, Integer> sRepeatModeForAnimator =
            new EnumMap<>(RepeatMode.class);

    static {
        sRepeatModeForAnimator.put(RepeatMode.REPEAT_MODE_UNKNOWN, DEFAULT_REPEAT_MODE);
        sRepeatModeForAnimator.put(RepeatMode.REPEAT_MODE_RESTART, ValueAnimator.RESTART);
        sRepeatModeForAnimator.put(RepeatMode.REPEAT_MODE_REVERSE, ValueAnimator.REVERSE);
    }

    private AnimationsHelper() {
    }

    /** Returns the main duration from the given {@link AnimationSpec} or default value if not
     * set. */
    @SuppressWarnings("deprecation") // Make sure the deprecated method is valid for compatibility
    @NonNull
    public static Duration getMainDurationOrDefault(@NonNull AnimationSpec spec) {
        return spec.hasAnimationParameters()
                && spec.getAnimationParameters().getDurationMillis() > 0
                ? Duration.ofMillis(spec.getAnimationParameters().getDurationMillis())
                : spec.getDurationMillis() > 0
                        ? Duration.ofMillis(spec.getDurationMillis())
                        : DEFAULT_ANIM_DURATION;
    }

    /** Returns the main delay from the given {@link AnimationSpec} or default value if not set. */
    @SuppressWarnings("deprecation") // Make sure the deprecated method is valid for compatibility
    @NonNull
    public static Duration getMainDelayOrDefault(@NonNull AnimationSpec spec) {
        return spec.getAnimationParameters().hasDelayMillis()
                ? Duration.ofMillis(spec.getAnimationParameters().getDelayMillis())
                : spec.getStartDelayMillis() > 0
                        ? Duration.ofMillis(spec.getStartDelayMillis())
                        : DEFAULT_ANIM_DELAY;
    }

    /**
     * Returns the main easing converted to the Interpolator from the given {@link AnimationSpec} or
     * default value if not set.
     */
    @SuppressWarnings("deprecation") // Make sure the deprecated method is valid for compatibility
    @NonNull
    public static Interpolator getMainInterpolatorOrDefault(@NonNull AnimationSpec spec) {
        Interpolator interpolator = DEFAULT_ANIM_INTERPOLATOR;

        Easing easing = null;
        if (spec.getAnimationParameters().hasEasing()) {
            easing = spec.getAnimationParameters().getEasing();
        } else if (spec.hasEasing()) {
            easing = spec.getEasing();
        }
        if (easing != null) {
            switch (easing.getInnerCase()) {
                case CUBIC_BEZIER:
                    if (easing.hasCubicBezier()) {
                        CubicBezierEasing cbe = easing.getCubicBezier();
                        interpolator = new PathInterpolator(cbe.getX1(), cbe.getY1(), cbe.getX2(),
                                cbe.getY2());
                    }
                    break;
                case INNER_NOT_SET:
                    break;
            }
        }

        return interpolator;
    }

    /**
     * Returns the repeat count from the given {@link AnimationSpec} or default value if not set
     */
    public static int getRepeatCountOrDefault(@NonNull AnimationSpec spec) {
        int repeatCount = DEFAULT_REPEAT_COUNT;

        if (spec.hasRepeatable()) {
            Repeatable repeatable = spec.getRepeatable();
            if (repeatable.getIterations() <= 0) {
                repeatCount = ValueAnimator.INFINITE;
            } else {
                // -1 because ValueAnimator uses count as number of how many times will animation be
                // repeated in addition to the first play.
                repeatCount = repeatable.getIterations() - 1;
            }
        }

        return repeatCount;
    }

    /** Returns the repeat mode from the given {@link AnimationSpec} or default value if not set. */
    public static int getRepeatModeOrDefault(@NonNull AnimationSpec spec) {
        int repeatMode = DEFAULT_REPEAT_MODE;

        if (spec.hasRepeatable()) {
            Repeatable repeatable = spec.getRepeatable();
            Integer repeatModeFromMap = sRepeatModeForAnimator.get(repeatable.getRepeatMode());
            if (repeatModeFromMap != null) {
                repeatMode = repeatModeFromMap;
            }
        }

        return repeatMode;
    }

    // public static Duration getOverrideForwardDurationOrDefault(@NonNull AnimationSpec spec) {...}

    @NonNull
    public static Duration getOverrideReverseDurationOrDefault(@NonNull AnimationSpec spec) {
        if (spec.hasRepeatable()) {
            Repeatable repeatable = spec.getRepeatable();
            if (repeatable.hasReverseRepeatOverride()) {
                AnimationParameters reverseParameters = repeatable.getReverseRepeatOverride();
                return reverseParameters.getDurationMillis() > 0
                        ? Duration.ofMillis(reverseParameters.getDurationMillis())
                        : getMainDurationOrDefault(spec);
            }
        }

        return getMainDurationOrDefault(spec);
    }

    /**
     * Returns true when a reverse duration which is different from forward duration is provided
     * for a reverse repeated animation.
     */
    static boolean hasCustomReverseDuration(@NonNull AnimationSpec spec) {
        return spec.hasRepeatable()
                && getRepeatCountOrDefault(spec) != 0
                && getRepeatModeOrDefault(spec) == ValueAnimator.REVERSE
                && getOverrideReverseDurationOrDefault(spec).toMillis()
                != getMainDurationOrDefault(spec).toMillis();
    }

    static class RepeatDelays {
        long mForwardRepeatDelay;
        long mReverseRepeatDelay;

        RepeatDelays(long forwardRepeatDelay, long reverseRepeatDelay) {
            mForwardRepeatDelay = forwardRepeatDelay;
            mReverseRepeatDelay = reverseRepeatDelay;
        }
    }

    /** Return the pair of forward repeat delay and reverse repeat delay */
    static RepeatDelays getRepeatDelays(AnimationSpec spec) {
        long mainDelay = getMainDelayOrDefault(spec).toMillis();
        long forwardRepeatDelay = mainDelay;
        long reverseRepeatDelay = mainDelay;
        int repeatCount = getRepeatCountOrDefault(spec);
        if (repeatCount > 0 || repeatCount == ValueAnimator.INFINITE) {
            if (spec.getRepeatable().getForwardRepeatOverride().hasDelayMillis()) {
                forwardRepeatDelay =
                        spec.getRepeatable().getForwardRepeatOverride().getDelayMillis();
            }

            if (getRepeatModeOrDefault(spec) == ValueAnimator.REVERSE
                    && spec.getRepeatable().getReverseRepeatOverride().hasDelayMillis()) {
                reverseRepeatDelay =
                        spec.getRepeatable().getReverseRepeatOverride().getDelayMillis();
            }
        }

        return new RepeatDelays(forwardRepeatDelay, reverseRepeatDelay);
    }

    /**
     * When a reverse duration which is different from forward duration is provided for a reverse
     * repeated animation, the spec would be split into main and aux specs. Main spec is for the
     * animator which require/release quota and plays the forward part of animation; and aux spec is
     * for the animator which plays the reverse part of animation, syncs with the main animator and
     * consumes no quota. These two specs are passed into {@link QuotaAwareAnimatorWithAux} to
     * create
     * main and aux animators which are played alternately. For other cases, null is returned as no
     * split would happen.
     */
    @Nullable
    static Pair<AnimationSpec, AnimationSpec> maybeSplitToMainAndAuxAnimationSpec(
            @NonNull AnimationSpec spec) {
        if (!hasCustomReverseDuration(spec)) {
            return null;
        }

        long forwardDuration = getMainDurationOrDefault(spec).toMillis();
        long reverseDuration = getOverrideReverseDurationOrDefault(spec).toMillis();
        Repeatable repeatable = spec.getRepeatable();
        RepeatDelays repeatDelays = getRepeatDelays(spec);

        Easing easing = null;
        if (spec.getAnimationParameters().hasEasing()) {
            easing = spec.getAnimationParameters().getEasing();
        }

        AnimationParameters.Builder mainParametersBuilder =
                AnimationParameters.newBuilder()
                        .setDurationMillis(forwardDuration)
                        .setDelayMillis(getMainDelayOrDefault(spec).toMillis());
        if (easing != null) {
            mainParametersBuilder.setEasing(easing);
        }
        AnimationSpec mainAnimatorSpec =
                AnimationSpec.newBuilder()
                        .setAnimationParameters(mainParametersBuilder.build())
                        .setRepeatable(
                                Repeatable.newBuilder()
                                        .setIterations((repeatable.getIterations() + 1) / 2)
                                        .setRepeatMode(RepeatMode.REPEAT_MODE_RESTART)
                                        .setForwardRepeatOverride(
                                                AnimationParameters.newBuilder()
                                                        .setDelayMillis(
                                                                repeatDelays.mReverseRepeatDelay)
                                                        .build())
                                        .build())
                        .build();

        AnimationParameters.Builder auxParametersBuilder =
                AnimationParameters.newBuilder()
                        .setDurationMillis(reverseDuration)
                        // The aux animator plays the reverse part of animation, so wait until
                        // the first pass of forward animation has run, plus repeat delay if any.
                        .setDelayMillis(forwardDuration + repeatDelays.mReverseRepeatDelay);
        if (spec.getRepeatable().getReverseRepeatOverride().hasEasing()) {
            easing = spec.getRepeatable().getReverseRepeatOverride().getEasing();
        }
        if (easing != null) {
            auxParametersBuilder.setEasing(easing);
        }
        AnimationSpec auxAnimatorSpec =
                AnimationSpec.newBuilder()
                        .setAnimationParameters(auxParametersBuilder.build())
                        .setRepeatable(
                                Repeatable.newBuilder()
                                        .setIterations(repeatable.getIterations() / 2)
                                        .setRepeatMode(RepeatMode.REPEAT_MODE_RESTART)
                                        .setForwardRepeatOverride(
                                                AnimationParameters.newBuilder()
                                                        .setDelayMillis(
                                                                repeatDelays.mForwardRepeatDelay)
                                                        .build())
                                        .build())
                        .build();
        return Pair.create(mainAnimatorSpec, auxAnimatorSpec);
    }

    /**
     * Sets animation parameters (duration, delay, easing, repeat mode and count) to the given
     * animator. These will be values from the given AnimationSpec if they are set and default
     * values otherwise.
     */
    public static void applyAnimationSpecToAnimator(
            @NonNull ValueAnimator animator, @NonNull AnimationSpec spec) {
        animator.setDuration(getMainDurationOrDefault(spec).toMillis());
        animator.setStartDelay(getMainDelayOrDefault(spec).toMillis());
        animator.setInterpolator(getMainInterpolatorOrDefault(spec));
        animator.setRepeatCount(getRepeatCountOrDefault(spec));
        animator.setRepeatMode(getRepeatModeOrDefault(spec));
    }

    /**
     * Sets animation parameters (duration, delay, easing, repeat mode and count) to the given
     * animation. These will be values from the given AnimationSpec if they are set and default
     * values otherwise.
     */
    public static void applyAnimationSpecToAnimation(
            @NonNull Animation animation, @NonNull AnimationSpec spec) {
        animation.setDuration(getMainDurationOrDefault(spec).toMillis());
        animation.setStartOffset(getMainDelayOrDefault(spec).toMillis());
        animation.setInterpolator(getMainInterpolatorOrDefault(spec));
        animation.setRepeatCount(getRepeatCountOrDefault(spec));
        animation.setRepeatMode(getRepeatModeOrDefault(spec));
    }
}
