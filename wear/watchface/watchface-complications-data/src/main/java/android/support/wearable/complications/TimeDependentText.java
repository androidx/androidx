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

package android.support.wearable.complications;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface for any object that returns different text depending on the given timestamp.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TimeDependentText extends  Parcelable {
    /**
     * Returns the text that should be displayed for the given timestamp.
     *
     * @param resources {@link Resources} from the current {@link Context}
     * @param dateTimeMillis milliseconds since epoch, e.g. from {@link System#currentTimeMillis}
     */
    @NonNull
    CharSequence getTextAt(@NonNull Resources resources, long dateTimeMillis);

    /**
     * Returns true if the result of {@link #getTextAt} will be the same for both {@code
     * firstDateTimeMillis} and {@code secondDateTimeMillis}.
     */
    boolean returnsSameText(long firstDateTimeMillis, long secondDateTimeMillis);

    /** Returns the next time after {@code fromTime} at which the text may change. */
    long getNextChangeTime(long fromTime);
}
