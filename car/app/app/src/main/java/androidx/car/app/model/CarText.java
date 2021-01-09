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

import android.text.Spanned;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A model used to send text with attached spans to the host.
 */
public class CarText {
    @Keep
    private final String mText;
    @Keep
    private final List<SpanWrapper> mSpans;

    /**
     * Returns {@code true} if the {@code carText} is {@code null} or an empty string, {@code
     * false} otherwise.
     */
    public static boolean isNullOrEmpty(@Nullable CarText carText) {
        return carText == null || carText.isEmpty();
    }

    /**
     * Returns a {@link CarText} instance for the given {@link CharSequence}.
     *
     * <p>Only {@link CarSpan} type spans are allowed in a {@link CarText}, other spans will be
     * removed from the provided {@link CharSequence}.
     */
    @NonNull
    public static CarText create(@NonNull CharSequence text) {
        return new CarText(text);
    }

    @NonNull
    public String getText() {
        return mText;
    }

    public boolean isEmpty() {
        return mText.isEmpty();
    }

    /** Returns the optional list of spans attached to the text. */
    @NonNull
    public List<SpanWrapper> getSpans() {
        return mSpans;
    }

    @NonNull
    @Override
    public String toString() {
        return getText();
    }

    /**
     * Returns a shortened string from the input {@code text}.
     */
    @Nullable
    public static String toShortString(@Nullable CarText text) {
        return text == null ? null : StringUtils.shortenString(text.toString());
    }

    private CarText() {
        mText = "";
        mSpans = Collections.emptyList();
    }

    private CarText(CharSequence text) {
        this.mText = text.toString().trim();

        mSpans = new ArrayList<>();

        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;

            for (Object span : spanned.getSpans(0, text.length(), Object.class)) {
                if (span instanceof CarSpan) {
                    mSpans.add(new SpanWrapper(spanned, (CarSpan) span));
                }
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarText)) {
            return false;
        }
        CarText otherText = (CarText) other;
        return Objects.equals(mText, otherText.mText) && Objects.equals(mSpans, otherText.mSpans);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mText, mSpans);
    }

    /**
     * Wraps a span to send it to the host.
     */
    public static class SpanWrapper {
        /**
         * @deprecated Removing in a follow up commit.
         */
        @Keep @Deprecated
        public final int start;
        /**
         * @deprecated Removing in a follow up commit.
         */
        @Keep @Deprecated
        public final int end;
        /**
         * @deprecated Removing in a follow up commit.
         */
        @Keep @Deprecated
        public final int flags;
        /**
         * @deprecated Removing in a follow up commit.
         */
        @Keep @Deprecated
        public final Object span;
        @Keep
        private final int mStart;
        @Keep
        private final int mEnd;
        @Keep
        private final int mFlags;
        @Keep @NonNull
        private final CarSpan mCarSpan;

        SpanWrapper(@NonNull Spanned spanned, @NonNull CarSpan carSpan) {
            mStart = spanned.getSpanStart(carSpan);
            mEnd = spanned.getSpanEnd(carSpan);
            mFlags = spanned.getSpanFlags(carSpan);
            mCarSpan = carSpan;
            this.start = mStart;
            this.end = mEnd;
            this.flags = mFlags;
            this.span = mCarSpan;
        }

        SpanWrapper() {
            mStart = 0;
            mEnd = 0;
            mFlags = 0;
            mCarSpan = new CarSpan();
            this.start = mStart;
            this.end = mEnd;
            this.flags = mFlags;
            this.span = mCarSpan;
        }

        public int getStart() {
            return mStart;
        }

        public int getEnd() {
            return mEnd;
        }

        public int getFlags() {
            return mFlags;
        }

        @NonNull
        public CarSpan getCarSpan() {
            return mCarSpan;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SpanWrapper)) {
                return false;
            }
            SpanWrapper wrapper = (SpanWrapper) other;
            return start == wrapper.start
                    && end == wrapper.end
                    && flags == wrapper.flags
                    && Objects.equals(span, wrapper.span);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, flags, span);
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + span + ": " + start + ", " + end + ", flags: " + flags + "]";
        }
    }
}
