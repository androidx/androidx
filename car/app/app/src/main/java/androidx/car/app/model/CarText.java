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
    /** An empty CarText for convenience. */
    @NonNull
    public static final CarText EMPTY = CarText.create("");

    @Keep
    @Nullable
    private String mText;
    @Keep
    private final List<SpanWrapper> mSpans;

    /**
     * Returns {@code true} if the {@code carText} is {@code null} or an empty string, {@code
     * false} otherwise.
     */
    public static boolean isNullOrEmpty(@Nullable CarText carText) {
        if (carText == null) {
            return true;
        }

        String text = carText.mText;
        return text == null || text.isEmpty();
    }

    /**
     * Returns a {@link CarText} instance for the given {@link CharSequence}, by sanitizing the car
     * sequence (dropping unsupported {@link Spanned} objects, and wrapping the remaining supported
     * {@link Spanned} objects into data that can be sent across to the host in a bundle.
     */
    @NonNull
    public static CarText create(@NonNull CharSequence text) {
        return new CarText(text);
    }

    @Nullable
    public String getText() {
        return mText;
    }

    public boolean isEmpty() {
        return mText == null || mText.isEmpty();
    }

    /** Returns the optional list of spans attached to the text. */
    @NonNull
    public List<SpanWrapper> getSpans() {
        return mSpans;
    }

    @NonNull
    @Override
    public String toString() {
        String text = getText();
        return text == null ? "" : text;
    }

    /**
     * Returns a shortened string from the input {@code text}.
     */
    @Nullable
    public static String toShortString(@Nullable CarText text) {
        return text == null ? null : StringUtils.shortenString(text.toString());
    }

    public CarText() {
        mText = null;
        mSpans = Collections.emptyList();
    }

    private CarText(CharSequence text) {
        this.mText = text.toString();

        mSpans = new ArrayList<>();

        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;

            for (Object span : spanned.getSpans(0, text.length(), Object.class)) {
                if (span instanceof ForegroundCarColorSpan
                        || span instanceof CarIconSpan
                        || span instanceof DurationSpan
                        || span instanceof DistanceSpan) {
                    mSpans.add(SpanWrapper.wrap(spanned, span));
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
        @Keep
        public final int start;
        @Keep
        public final int end;
        @Keep
        public final int flags;
        @Keep
        @Nullable
        public final Object span;

        static SpanWrapper wrap(Spanned spanned, Object span) {
            return new SpanWrapper(spanned, span);
        }

        SpanWrapper(Spanned spanned, Object span) {
            this.start = spanned.getSpanStart(span);
            this.end = spanned.getSpanEnd(span);
            this.flags = spanned.getSpanFlags(span);
            this.span = span;
        }

        SpanWrapper() {
            start = 0;
            end = 0;
            flags = 0;
            span = null;
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

        @Override
        public String toString() {
            return "[" + span + ": " + start + ", " + end + ", flags: " + flags + "]";
        }
    }
}
