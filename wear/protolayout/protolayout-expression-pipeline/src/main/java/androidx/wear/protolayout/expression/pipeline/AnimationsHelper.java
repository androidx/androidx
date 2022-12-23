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
import androidx.annotation.RestrictTo;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.CubicBezierEasing;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.RepeatMode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.Repeatable;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Helper class for Animations in ProtoLayout. It contains helper methods used in rendered and
 * constants for default values.
 *
 * @hide
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

    private AnimationsHelper() {}

    /** Returns the duration from the given {@link AnimationSpec} or default value if not set. */
    @NonNull
    public static Duration getDurationOrDefault(@NonNull AnimationSpec spec) {
        return spec.getDurationMillis() > 0
                ? Duration.ofMillis(spec.getDurationMillis())
                : DEFAULT_ANIM_DURATION;
    }

    /** Returns the delay from the given {@link AnimationSpec} or default value if not set. */
    @NonNull
    public static Duration getDelayOrDefault(@NonNull AnimationSpec spec) {
        return spec.getDelayMillis() > 0
                ? Duration.ofMillis(spec.getDelayMillis())
                : DEFAULT_ANIM_DELAY;
    }

    /**
     * Returns the easing converted to the Interpolator from the given {@link AnimationSpec} or
     * default value if not set.
     */
    @NonNull
    public static Interpolator getInterpolatorOrDefault(@NonNull AnimationSpec spec) {
        Interpolator interpolator = DEFAULT_ANIM_INTERPOLATOR;

        if (spec.hasEasing()) {
            switch (spec.getEasing().getInnerCase()) {
                case CUBIC_BEZIER:
                    if (spec.getEasing().hasCubicBezier()) {
                        CubicBezierEasing cbe = spec.getEasing().getCubicBezier();
                        interpolator =
                                new PathInterpolator(
                                        cbe.getX1(), cbe.getY1(), cbe.getX2(), cbe.getY2());
                    }
                    break;
                case INNER_NOT_SET:
                    break;
            }
        }

        return interpolator;
    }

    /**
     * Returns the repeat count from the given {@link AnimationSpec} or default value if not set.
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

    /**
     * Sets animation parameters (duration, delay, easing, repeat mode and count) to the given
     * animator. These will be values from the given AnimationSpec if they are set and default
     * values otherwise.
     */
    public static void applyAnimationSpecToAnimator(
            @NonNull ValueAnimator animator, @NonNull AnimationSpec spec) {
        animator.setDuration(getDurationOrDefault(spec).toMillis());
        animator.setStartDelay(getDelayOrDefault(spec).toMillis());
        animator.setInterpolator(getInterpolatorOrDefault(spec));
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
        animation.setDuration(getDurationOrDefault(spec).toMillis());
        animation.setStartOffset(getDelayOrDefault(spec).toMillis());
        animation.setInterpolator(getInterpolatorOrDefault(spec));
        animation.setRepeatCount(getRepeatCountOrDefault(spec));
        animation.setRepeatMode(getRepeatModeOrDefault(spec));
    }
}
