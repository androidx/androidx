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

import android.annotation.SuppressLint;

import androidx.annotation.DoNotInline;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.car.app.annotations.CarProtocol;

import java.time.Duration;

/**
 * A span that replaces the text it is attached to with a localized duration string.
 *
 * <p>For example, the following code creates a string that shows the duration as the first text in
 * the string before the interpunct:
 *
 * <pre>{@code
 * String interpunct = "\\u00b7";
 * SpannableString string = new SpannableString("  " + interpunct + " Point-of-Interest 1");
 * string.setSpan(DurationSpan.create(300), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
 * }</pre>
 *
 * <p>The span flags (e.g. SPAN_EXCLUSIVE_EXCLUSIVE) will be ignored.
 *
 * <p>This span will be ignored if it overlaps with any span that replaces text, such as another
 * {@link DistanceSpan}, {@link DurationSpan}, or {@link CarIconSpan}. However, it is possible to *
 * apply styling to the text, such as changing colors:
 *
 * <pre>{@code
 * String interpunct = "\\u00b7";
 * SpannableString string = new SpannableString("  " + interpunct + " Point-of-Interest 1");
 * string.setSpan(DurationSpan.create(300), 0, 1, SPAN_INCLUSIVE_INCLUSIVE);
 * string.setSpan(ForegroundCarColorSpan.create(CarColor.BLUE), 0, 1, SPAN_EXCLUSIVE_EXCLUSIVE);
 * }</pre>
 */
@CarProtocol
public final class DurationSpan extends CarSpan {
    @Keep
    private final long mDurationSeconds;

    /** Creates a {@link DurationSpan} with the given duration. */
    @NonNull
    public static DurationSpan create(long durationSeconds) {
        return new DurationSpan(durationSeconds);
    }

    /** Creates a {@link DurationSpan} with the given duration. */
    @NonNull
    @RequiresApi(26)
    public static DurationSpan create(@NonNull Duration duration) {
        return Api26Impl.create(duration);
    }

    /** Returns the time duration associated with this span, in seconds. */
    @SuppressLint("MethodNameUnits")
    public long getDurationSeconds() {
        return mDurationSeconds;
    }

    @Override
    @NonNull
    public String toString() {
        return "[seconds: " + mDurationSeconds + "]";
    }

    @Override
    public int hashCode() {
        // Equivalent implementation as Long.hashcode() but avoids the boxing.
        return (int) (mDurationSeconds ^ (mDurationSeconds >>> 32));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DurationSpan)) {
            return false;
        }
        DurationSpan otherSpan = (DurationSpan) other;

        return mDurationSeconds == otherSpan.mDurationSeconds;
    }

    DurationSpan(long durationSeconds) {
        mDurationSeconds = durationSeconds;
    }

    private DurationSpan() {
        mDurationSeconds = 0;
    }

    /**
     * Version-specific static inner class to avoid verification errors that negatively affect
     * run-time performance.
     */
    @RequiresApi(26)
    private static final class Api26Impl {
        private Api26Impl() {
        }

        @DoNotInline
        @NonNull
        public static DurationSpan create(@NonNull Duration duration) {
            return new DurationSpan(requireNonNull(duration).getSeconds());
        }
    }
}
