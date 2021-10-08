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

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;

import java.util.Objects;

/**
 * A span that replaces the text it is attached to with the string representation of a {@link
 * Distance} instance.
 *
 * <p>The {@link Distance} instance will be displayed by the host in a localized format, so that it
 * will be consistent with the rest of the user interface where distance information are displayed.
 *
 * <p>For example, the following code creates a string that shows the distance as the first text in
 * the string before the interpunct:
 *
 * <pre>{@code
 * String interpunct = "\\u00b7";
 * SpannableString string = new SpannableString("  " + interpunct + " Point-of-Interest 1");
 * string.setSpan(
 *   DistanceSpan.create(
 *     Distance.create(1000, "1.0", UNIT_KILOMETERS)), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
 * }</pre>
 *
 * <p>The span flags (e.g. SPAN_EXCLUSIVE_EXCLUSIVE) will be ignored.
 *
 * <p>This span will be ignored if it overlaps with any span that replaces text, such as another
 * {@link DistanceSpan}, {@link DurationSpan}, or {@link CarIconSpan}. However, it is possible to
 * apply styling to the text, such as changing colors:
 *
 * <pre>{@code
 * String interpunct = "\\u00b7";
 * SpannableString string = new SpannableString("  " + interpunct + " Point-of-Interest 1");
 * string.setSpan(
 *   DistanceSpan.create(
 *     Distance.create(1000, "1.0", UNIT_KILOMETERS)), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
 * string.setSpan(ForegroundCarColorSpan.create(CarColor.BLUE), 0, 1, SPAN_EXCLUSIVE_EXCLUSIVE);
 * }</pre>
 */
@CarProtocol
public final class DistanceSpan extends CarSpan {
    @Nullable
    @Keep
    private final Distance mDistance;

    /** Creates a {@link DistanceSpan} from a {@link CarIcon}. */
    @NonNull
    public static DistanceSpan create(@NonNull Distance distance) {
        return new DistanceSpan(requireNonNull(distance));
    }

    /**
     * Returns the {@link Distance} instance associated with this span.
     */
    @NonNull
    public Distance getDistance() {
        return requireNonNull(mDistance);
    }

    @NonNull
    @Override
    public String toString() {
        return "[distance: " + mDistance + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mDistance);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DistanceSpan)) {
            return false;
        }
        DistanceSpan otherSpan = (DistanceSpan) other;

        return Objects.equals(mDistance, otherSpan.mDistance);
    }

    private DistanceSpan(Distance distance) {
        mDistance = distance;
    }

    private DistanceSpan() {
        mDistance = null;
    }
}
