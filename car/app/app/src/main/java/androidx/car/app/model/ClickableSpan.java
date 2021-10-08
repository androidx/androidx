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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

import java.util.Objects;

/**
 * A span that makes a section of text clickable.
 *
 * <p>The text of this span will be highlighted by the host, so users understand that it is
 * interactive. If this span overlaps the other spans (for example, {@link ForegroundCarColorSpan}),
 * the host might choose to ignore those spans if they conflict on how clickable text is
 * highlighted.
 *
 * <p>The host may ignore {@link ClickableSpan}s unless support for it is explicitly documented in
 * the API that takes the string.
 *
 * <p>For example, to make a portion of a text clickable:
 *
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with a clickable span");
 * string.setSpan(ClickableSpan.create(
 *     new OnClickListener
 *     ), 12, 22, Spanned.SPAN_INCLUSIVE_EXCLUSIVE));
 * }</pre>
 */
@RequiresCarApi(2)
@CarProtocol
public final class ClickableSpan extends CarSpan {
    @Keep
    @Nullable
    private final OnClickDelegate mOnClickDelegate;

    /**
     * Creates a {@link ClickableSpan} from a {@link OnClickListener}.
     *
     * <p>Note that the callback relates to UI events and will be executed on the main thread
     * using {@link Looper#getMainLooper()}.
     *
     * @throws NullPointerException if {@code onClickListener} is {@code null}
     */
    @NonNull
    @SuppressLint("ExecutorRegistration")
    public static ClickableSpan create(@NonNull OnClickListener onClickListener) {
        return new ClickableSpan(requireNonNull(onClickListener));
    }

    /** Returns the {@link OnClickDelegate} associated with this span. */
    @NonNull
    public OnClickDelegate getOnClickDelegate() {
        return requireNonNull(mOnClickDelegate);
    }

    @Override
    @NonNull
    public String toString() {
        return "[clickable]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mOnClickDelegate == null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClickableSpan)) {
            return false;
        }
        ClickableSpan otherSpan = (ClickableSpan) other;

        // Don't compare callback, only ensure if it is present in one, it is also present in
        // the other.
        return Objects.equals(mOnClickDelegate == null, otherSpan.mOnClickDelegate == null);
    }

    private ClickableSpan(OnClickListener onClickListener) {
        mOnClickDelegate = OnClickDelegateImpl.create(onClickListener);
    }

    /** Constructs an empty instance, used by serialization code. */
    private ClickableSpan() {
        mOnClickDelegate = null;
    }
}
