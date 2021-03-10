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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A span that replaces the text it is attached to with a {@link CarIcon} that is aligned with the
 * surrounding text.
 *
 * <p>The image may be scaled with the text differently depending on the template that the text
 * belongs to. Refer to the documentation of each template for that information.
 *
 * <p>For example, the following code creates a string for a navigation maneuver that has an image
 * with the number of a highway rendered as an icon in between "on" and "East":
 *
 * <pre>{@code
 * SpannableString string = new SpannableString("Turn right on 520 East");
 * string.setSpan(
 *     CarIconSpan.create(new CarIcon.Builder(
 *         IconCompat.createWithResource(getCarContext(), R.drawable.ic_520_highway))),
 *         14, 17, SPAN_INCLUSIVE_EXCLUSIVE);
 * }</pre>
 *
 * <p>{@link CarIconSpan}s in strings passed to the library templates may be ignored by the host
 * when displaying the text unless support for them is explicitly documented in the API that takes
 * the string.
 *
 * <p>This span will be ignored if it overlaps with any span that replaces text, such as another
 * {@link DistanceSpan}, {@link DurationSpan}, or {@link CarIconSpan}.
 *
 * @see CarIcon
 */
@CarProtocol
public final class CarIconSpan extends CarSpan {
    /**
     * Indicates how to align a car icon span with its surrounding text.
     *
     * @hide
     */
    @IntDef(
            value = {
                    ALIGN_CENTER,
                    ALIGN_BOTTOM,
                    ALIGN_BASELINE,
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Alignment {
    }

    /**
     * A constant indicating that the bottom of this span should be aligned with the bottom of the
     * surrounding text, at the same level as the lowest descender in the text.
     */
    @Alignment
    public static final int ALIGN_BOTTOM = 0;

    /**
     * A constant indicating that the bottom of this span should be aligned with the baseline of the
     * surrounding text.
     */
    @Alignment
    public static final int ALIGN_BASELINE = 1;

    /**
     * A constant indicating that this span should be vertically centered between the top and the
     * lowest descender.
     */
    @Alignment
    public static final int ALIGN_CENTER = 2;

    @Nullable
    @Keep
    private final CarIcon mIcon;
    @Alignment
    @Keep
    private final int mAlignment;

    /**
     * Creates a {@link CarIconSpan} from a {@link CarIcon} with a default alignment of {@link
     * #ALIGN_BASELINE}.
     *
     * @throws NullPointerException if {@code icon} is {@code null}
     * @see #create(CarIcon, int)
     */
    @NonNull
    public static CarIconSpan create(@NonNull CarIcon icon) {
        return create(icon, ALIGN_BASELINE);
    }

    /**
     * Creates a {@link CarIconSpan} from a {@link CarIcon}, specifying the alignment of the icon
     * with respect to its surrounding text.
     *
     * @param icon      the {@link CarIcon} to replace the text with
     * @param alignment the alignment of the {@link CarIcon} relative to the text. This should be
     *                  one of {@link #ALIGN_BASELINE}, {@link #ALIGN_BOTTOM} or
     *                  {@link #ALIGN_CENTER}
     * @throws NullPointerException     if {@code icon} is {@code null}
     * @throws IllegalArgumentException if {@code alignment} is not a valid value
     * @see #ALIGN_BASELINE
     * @see #ALIGN_BOTTOM
     * @see #ALIGN_CENTER
     */
    @NonNull
    public static CarIconSpan create(@NonNull CarIcon icon, @Alignment int alignment) {
        CarIconConstraints.DEFAULT.validateOrThrow(icon);
        if (alignment != ALIGN_BASELINE && alignment != ALIGN_BOTTOM && alignment != ALIGN_CENTER) {
            throw new IllegalStateException("Invalid alignment value: " + alignment);
        }

        return new CarIconSpan(requireNonNull(icon), alignment);
    }

    private CarIconSpan(@Nullable CarIcon icon, @Alignment int alignment) {
        mIcon = icon;
        mAlignment = alignment;
    }

    private CarIconSpan() {
        mIcon = null;
        mAlignment = ALIGN_BASELINE;
    }

    /**
     * Returns the {@link CarIcon} instance associated with this span.
     */
    @NonNull
    public CarIcon getIcon() {
        return requireNonNull(mIcon);
    }

    /**
     * Returns the alignment that should be used with this span.
     */
    @Alignment
    public int getAlignment() {
        return mAlignment;
    }

    @Override
    @NonNull
    public String toString() {
        return "[icon: " + mIcon + ", alignment: " + alignmentToString(mAlignment) + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mIcon);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarIconSpan)) {
            return false;
        }
        CarIconSpan otherIconSpan = (CarIconSpan) other;

        return Objects.equals(mIcon, otherIconSpan.mIcon);
    }

    private static String alignmentToString(@Alignment int alignment) {
        switch (alignment) {
            case ALIGN_BASELINE:
                return "baseline";
            case ALIGN_BOTTOM:
                return "bottom";
            case ALIGN_CENTER:
                return "center";
            default:
                return "unknown";
        }
    }
}
