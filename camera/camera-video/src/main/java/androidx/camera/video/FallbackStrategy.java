/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class represents the strategy that will be adopted when the device does not support all the
 * desired {@link Quality} in {@link QualitySelector} in order to select the quality as possible.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FallbackStrategy {

    // Restrict access to sealed class
    private FallbackStrategy() {
    }

    /**
     * Returns a fallback strategy that will choose the quality that is closest to and higher
     * than the input quality.
     *
     * <p>If that can not result in a supported quality, choose the quality that is closest to
     * and lower than the input quality.
     */
    @NonNull
    public static FallbackStrategy higherQualityOrLowerThan(@NonNull Quality quality) {
        return new AutoValue_FallbackStrategy_RuleStrategy(quality,
                FALLBACK_RULE_HIGHER_OR_LOWER);
    }

    /**
     * Returns a fallback strategy that will choose the quality that is closest to and higher
     * than the input quality.
     */
    @NonNull
    public static FallbackStrategy higherQualityThan(@NonNull Quality quality) {
        return new AutoValue_FallbackStrategy_RuleStrategy(quality,
                FALLBACK_RULE_HIGHER);
    }

    /**
     * Returns a fallback strategy that will choose the quality that is closest to and lower than
     * the input quality.
     *
     * <p>If that can not result in a supported quality, choose the quality that is closest to
     * and higher than the input quality.
     */
    @NonNull
    public static FallbackStrategy lowerQualityOrHigherThan(@NonNull Quality quality) {
        return new AutoValue_FallbackStrategy_RuleStrategy(quality,
                FALLBACK_RULE_LOWER_OR_HIGHER);
    }

    /**
     * Returns a fallback strategy that will choose the quality that is closest to and lower
     * than the input quality.
     */
    @NonNull
    public static FallbackStrategy lowerQualityThan(@NonNull Quality quality) {
        return new AutoValue_FallbackStrategy_RuleStrategy(quality,
                FALLBACK_RULE_LOWER);
    }

    static final int FALLBACK_RULE_NONE = 0;

    /**
     * Choose the quality that is closest to and higher than the desired quality. If that can not
     * result in a supported quality, choose the quality that is closest to and lower than the
     * desired quality.
     */
    static final int FALLBACK_RULE_HIGHER_OR_LOWER = 1;

    /**
     * Choose the quality that is closest to and higher than the desired quality.
     */
    static final int FALLBACK_RULE_HIGHER = 2;

    /**
     * Choose the quality that is closest to and lower than the desired quality. If that can not
     * result in a supported quality, choose the quality that is closest to and higher than the
     * desired quality.
     */
    static final int FALLBACK_RULE_LOWER_OR_HIGHER = 3;

    /**
     * Choose the quality that is closest to and lower than the desired quality.
     */
    static final int FALLBACK_RULE_LOWER = 4;

    /**
     * The fallback strategies when desired quality is not supported.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FALLBACK_RULE_NONE,
            FALLBACK_RULE_HIGHER_OR_LOWER,
            FALLBACK_RULE_HIGHER,
            FALLBACK_RULE_LOWER_OR_HIGHER,
            FALLBACK_RULE_LOWER
    })
    @interface FallbackRule {
    }

    /**
     * The strategy that no fallback strategy will be applied.
     */
    static final FallbackStrategy NONE =
            new AutoValue_FallbackStrategy_RuleStrategy(Quality.NONE, FALLBACK_RULE_NONE);

    @AutoValue
    abstract static class RuleStrategy extends FallbackStrategy {
        @NonNull
        abstract Quality getFallbackQuality();

        @FallbackRule
        abstract int getFallbackRule();
    }
}
