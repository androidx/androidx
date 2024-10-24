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

package androidx.car.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A message to show to the user for a short period of time. */
public final class CarToast {
    @IntDef(value = {LENGTH_SHORT, LENGTH_LONG})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface Duration {
    }

    /**
     * Show the Toast view for a short period of time. This is the default duration.
     *
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 0;

    /**
     * Show the Toast view for a long period of time.
     *
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 1;

    private final CarContext mCarContext;
    private @Nullable CharSequence mText;
    private int mDuration;

    /**
     * Constructs an empty toast.
     *
     * <p>You <strong>MUST</strong> call {@link #setText} before you can call {@link #show}.
     *
     * @throws NullPointerException if {@code carContext} is {@code null}
     */
    @VisibleForTesting
    CarToast(@NonNull CarContext carContext) {
        mCarContext = requireNonNull(carContext);
    }

    /**
     * Creates and sets the text and duration for the toast view.
     *
     * @param textResId the resource id for the text to show. If the {@code textResId} is 0, the
     *                  text will be set to empty
     * @param duration  how long to display the message. Either {@link #LENGTH_SHORT} or {@link
     *                  #LENGTH_LONG}
     * @throws NullPointerException if {@code carContext} is {@code null}
     */
    public static @NonNull CarToast makeText(
            @NonNull CarContext carContext, @StringRes int textResId, @Duration int duration) {
        return makeText(
                requireNonNull(carContext),
                textResId == 0 ? "" : carContext.getString(textResId),
                duration);
    }

    /**
     * Creates and sets the text and duration for the toast view.
     *
     * @param text     the text to show
     * @param duration how long to display the message. Either {@link #LENGTH_SHORT} or {@link
     *                 #LENGTH_LONG}
     * @throws NullPointerException if either the {@code carContext} or the {@code text} are {@code
     *                              null}
     */
    public static @NonNull CarToast makeText(
            @NonNull CarContext carContext, @NonNull CharSequence text, @Duration int duration) {
        CarToast carToast = new CarToast(requireNonNull(carContext));
        carToast.mText = requireNonNull(text);
        carToast.mDuration = duration;
        return carToast;
    }

    /**
     * Sets the text for the toast.
     *
     * @param textResId the resource id for the text. If the {@code textResId} is 0, the text
     *                  will be set to empty
     */
    public void setText(@StringRes int textResId) {
        mText = textResId == 0 ? "" : mCarContext.getString(textResId);
    }

    /**
     * Sets the text for the toast.
     *
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(@NonNull CharSequence text) {
        mText = requireNonNull(text);
    }

    /**
     * Sets how long to show the toast for.
     *
     * @param duration how long to display the message. Either {@link #LENGTH_SHORT} or {@link
     *                 #LENGTH_LONG}
     */
    public void setDuration(@Duration int duration) {
        mDuration = duration;
    }

    /**
     * Shows the toast with the specified text for the specified duration.
     *
     * @throws HostException if the remote call fails
     */
    public void show() {
        CharSequence text = mText;
        if (text == null) {
            throw new IllegalStateException("setText must have been called");
        }

        mCarContext.getCarService(AppManager.class).showToast(text, mDuration);
    }
}
