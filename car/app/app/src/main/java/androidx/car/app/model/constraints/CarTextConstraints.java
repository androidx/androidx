/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model.constraints;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.CarIconSpan;
import androidx.car.app.model.CarSpan;
import androidx.car.app.model.CarText;
import androidx.car.app.model.ClickableSpan;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ForegroundCarColorSpan;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Encapsulates the constraints to apply when rendering a {@link CarText} on a template.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CarTextConstraints {
    /** No {@link CarSpan}s allowed. */
    public static final @NonNull CarTextConstraints CONSERVATIVE =
            new CarTextConstraints(Collections.emptyList());

    /**
     * Allow all {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     *     <li>{@link ForegroundCarColorSpan}
     *     <li>{@link CarIconSpan}
     *     <li>{@link ClickableSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints UNCONSTRAINED =
            new CarTextConstraints(Arrays.asList(
                    CarIconSpan.class,
                    ClickableSpan.class,
                    DistanceSpan.class,
                    DurationSpan.class,
                    ForegroundCarColorSpan.class));

    /**
     * Allow clickable text-only {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     *     <li>{@link ClickableSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints CLICKABLE_TEXT_ONLY =
            new CarTextConstraints(Arrays.asList(
                    ClickableSpan.class,
                    DistanceSpan.class,
                    DurationSpan.class));

    /**
     * Allow color-only {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link ForegroundCarColorSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints COLOR_ONLY =
            new CarTextConstraints(Arrays.asList(ForegroundCarColorSpan.class));

    /**
     * Allow text-only {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints TEXT_ONLY =
            new CarTextConstraints(Arrays.asList(
                    DistanceSpan.class,
                    DurationSpan.class));

    /**
     * Allow text and icon {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     *     <li>{@link CarIconSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints TEXT_AND_ICON =
            new CarTextConstraints(Arrays.asList(
                    DistanceSpan.class,
                    DurationSpan.class,
                    CarIconSpan.class));

    /**
     * Allow text and color {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     *     <li>{@link ForegroundCarColorSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints TEXT_WITH_COLORS =
            new CarTextConstraints(Arrays.asList(
                    DistanceSpan.class,
                    DurationSpan.class,
                    ForegroundCarColorSpan.class));

    /**
     * Allow text with color and icon {@link CarSpan}s:
     *
     * <ul>
     *     <li>{@link DistanceSpan}
     *     <li>{@link DurationSpan}
     *     <li>{@link ForegroundCarColorSpan}
     *     <li>{@link CarIconSpan}
     * </ul>
     */
    public static final @NonNull CarTextConstraints TEXT_WITH_COLORS_AND_ICON =
            new CarTextConstraints(Arrays.asList(
                    DistanceSpan.class,
                    DurationSpan.class,
                    ForegroundCarColorSpan.class,
                    CarIconSpan.class));

    private final HashSet<Class<? extends CarSpan>> mAllowedTypes;

    /**
     * Returns {@code true} if the {@link CarText} meets the constraints' requirement.
     *
     * @throws IllegalArgumentException if any span types are not allowed
     */
    public void validateOrThrow(@NonNull CarText carText) {
        checkSupportedSpans(carText.getSpans());
        for (List<CarText.SpanWrapper> variantSpans : carText.getSpansForVariants()) {
            checkSupportedSpans(variantSpans);
        }
    }

    private void checkSupportedSpans(List<CarText.SpanWrapper> spans) {
        for (CarText.SpanWrapper span : spans) {
            Class<? extends CarSpan> klass = span.getCarSpan().getClass();
            if (!mAllowedTypes.contains(klass)) {
                throw new IllegalArgumentException(
                        "CarSpan type is not allowed: " + klass.getSimpleName());
            }
        }
    }

    private CarTextConstraints(List<Class<? extends CarSpan>> allowedSpans) {
        mAllowedTypes = new HashSet<>(allowedSpans);
    }
}
