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

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.LocaleSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays one or more ComplicationText objects in a template.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public final class ComplicationTextTemplate implements Parcelable, TimeDependentText {

    private static final String KEY_TIME_DEPENDENT_TEXTS = "KEY_TIME_DEPENDENT_TEXTS";
    private static final String KEY_SURROUNDING_STRING = "KEY_SURROUNDING_STRING";

    public static final Creator<ComplicationTextTemplate> CREATOR =
            new Creator<ComplicationTextTemplate>() {
                @Override
                @NonNull
                @SuppressLint("SyntheticAccessor")
                public ComplicationTextTemplate createFromParcel(@NonNull Parcel in) {
                    return new ComplicationTextTemplate(in);
                }

                @Override
                @NonNull
                public ComplicationTextTemplate[] newArray(int size) {
                    return new ComplicationTextTemplate[size];
                }
            };

    /**
     * The plain-text part of the complication text. See {@link Builder#setSurroundingText} for
     * details.
     */
    private final CharSequence mSurroundingText;

    /** The time-dependent parts of the complication text. */
    private final ComplicationText[] mComplicationTexts;

    private ComplicationTextTemplate(
            @Nullable CharSequence surroundingText,
            @NonNull ComplicationText[] complicationTexts) {
        mSurroundingText = surroundingText;
        mComplicationTexts = complicationTexts;
        checkFields();
    }

    private ComplicationTextTemplate(@NonNull Parcel in) {
        this(in.readBundle(ComplicationTextTemplate.class.getClassLoader()));
    }

    private ComplicationTextTemplate(@NonNull Bundle rootBundle) {
        mSurroundingText = rootBundle.getCharSequence(KEY_SURROUNDING_STRING);

        Parcelable[] texts = rootBundle.getParcelableArray(KEY_TIME_DEPENDENT_TEXTS);
        assert texts != null;
        mComplicationTexts = new ComplicationText[texts.length];
        for (int i = 0; i < texts.length; i++) {
            mComplicationTexts[i] = (ComplicationText) texts[i];
        }

        checkFields();
    }

    private void checkFields() {
        if (mSurroundingText == null && mComplicationTexts.length == 0) {
            throw new IllegalStateException(
                    "One of mSurroundingText and mTimeDependentText must be non-null");
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(KEY_SURROUNDING_STRING, mSurroundingText);
        bundle.putParcelableArray(KEY_TIME_DEPENDENT_TEXTS, mComplicationTexts);
        out.writeBundle(bundle);
    }

    @NonNull
    @Override
    public CharSequence getTextAt(@NonNull Resources resources, long dateTimeMillis) {
        final int len = mComplicationTexts.length;
        if (len == 0) {
            return mSurroundingText;
        }
        CharSequence[] timeDependentParts = new CharSequence[len];
        for (int i = 0; i < len; i++) {
            timeDependentParts[i] = mComplicationTexts[i].getTextAt(resources, dateTimeMillis);
        }

        if (mSurroundingText == null) {
            return TextUtils.join(" ", timeDependentParts);
        }

        return TextUtils.expandTemplate(mSurroundingText, timeDependentParts);
    }

    @Override
    public boolean returnsSameText(long firstDateTimeMillis, long secondDateTimeMillis) {
        for (TimeDependentText text : mComplicationTexts) {
            if (!text.returnsSameText(firstDateTimeMillis, secondDateTimeMillis)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public long getNextChangeTime(long fromTime) {
        long result = Long.MAX_VALUE;
        for (TimeDependentText text : mComplicationTexts) {
            result = Math.min(result, text.getNextChangeTime(fromTime));
        }
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Builder for a ComplicationTextTemplate object that displays one or more {@link
     * ComplicationText} objects, within a format string if specified.
     */
    public static final class Builder {
        private CharSequence mSurroundingText;
        private final List<ComplicationText> mTexts = new ArrayList<>(2);

        /** Adds a ComplicationText to be applied to the {@link #setSurroundingText} template. */
        @NonNull
        public Builder addComplicationText(@NonNull ComplicationText text) {
            mTexts.add(text);
            return this;
        }

        /**
         * Sets the string within which the texts will be displayed. This is optional.
         *
         * <p>Within the text, {@code ^1} will be replaced with the first ComplicationText, {@code
         * ^2} will be replaced with the second, and so on. So for example to show a result like
         * {@code "8 of 10 minutes"} the surrounding text would be {@code "^1 of ^2 minutes"}.
         *
         * <p>To use the {@code ^} character within the text, escape it as {@code ^^}.
         *
         * <p>If not specified, the texts will be concatenated together, separated by spaces.
         *
         * <p>If the text contains spans, some of them may not be rendered by
         * {@link ComplicationDrawable}. Supported spans are {@link ForegroundColorSpan},
         * {@link LocaleSpan}, {@link SubscriptSpan}, {@link SuperscriptSpan}, {@link StyleSpan},
         * {@link StrikethroughSpan}, {@link TypefaceSpan} and {@link UnderlineSpan}.
         *
         * @param surroundingText string template
         * @return this builder for chaining
         */
        @NonNull
        public Builder setSurroundingText(@Nullable CharSequence surroundingText) {
            mSurroundingText = surroundingText;
            return this;
        }

        /**
         * Returns {@link ComplicationTextTemplate} including the ComplicationText objects formatted
         * as specified.
         */
        @NonNull
        @SuppressLint("SyntheticAccessor")
        public ComplicationTextTemplate build() {
            if (mTexts.isEmpty()) {
                throw new IllegalStateException("At least one text must be specified.");
            }

            return new ComplicationTextTemplate(
                    mSurroundingText, mTexts.toArray(new ComplicationText[mTexts.size()]));
        }
    }
}
