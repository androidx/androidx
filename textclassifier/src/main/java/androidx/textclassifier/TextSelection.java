/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.textclassifier.TextClassifier.EntityType;

/**
 * Information about where text selection should be.
 */
public final class TextSelection implements Parcelable {

    private final int mStartIndex;
    private final int mEndIndex;
    @NonNull private final EntityConfidence mEntityConfidence;
    @NonNull private final String mSignature;

    private TextSelection(
            int startIndex, int endIndex, @NonNull Map<String, Float> entityConfidence,
            @NonNull String signature) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mEntityConfidence = new EntityConfidence(entityConfidence);
        mSignature = signature;
    }

    /**
     * Returns the start index of the text selection.
     */
    public int getSelectionStartIndex() {
        return mStartIndex;
    }

    /**
     * Returns the end index of the text selection.
     */
    public int getSelectionEndIndex() {
        return mEndIndex;
    }

    /**
     * Returns the number of entities found in the classified text.
     */
    @IntRange(from = 0)
    public int getEntityCount() {
        return mEntityConfidence.getEntities().size();
    }

    /**
     * Returns the entity at the specified index. Entities are ordered from high confidence
     * to low confidence.
     *
     * @throws IndexOutOfBoundsException if the specified index is out of range.
     * @see #getEntityCount() for the number of entities available.
     */
    @NonNull
    public @EntityType String getEntity(int index) {
        return mEntityConfidence.getEntities().get(index);
    }

    /**
     * Returns the confidence score for the specified entity. The value ranges from
     * 0 (low confidence) to 1 (high confidence). 0 indicates that the entity was not found for the
     * classified text.
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getConfidenceScore(@EntityType String entity) {
        return mEntityConfidence.getConfidenceScore(entity);
    }

    /**
     * Returns the signature for this object.
     * The TextClassifier that generates this object may use it as a way to internally identify
     * this object.
     */
    @NonNull
    public String getSignature() {
        return mSignature;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "TextSelection {startIndex=%d, endIndex=%d, entities=%s, signature=%s}",
                mStartIndex, mEndIndex, mEntityConfidence, mSignature);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStartIndex);
        dest.writeInt(mEndIndex);
        mEntityConfidence.writeToParcel(dest, flags);
        dest.writeString(mSignature);
    }

    public static final Parcelable.Creator<TextSelection> CREATOR =
            new Parcelable.Creator<TextSelection>() {
                @Override
                public TextSelection createFromParcel(Parcel in) {
                    return new TextSelection(in);
                }

                @Override
                public TextSelection[] newArray(int size) {
                    return new TextSelection[size];
                }
            };

    private TextSelection(Parcel in) {
        mStartIndex = in.readInt();
        mEndIndex = in.readInt();
        mEntityConfidence = EntityConfidence.CREATOR.createFromParcel(in);
        mSignature = in.readString();
    }

    /**
     * Builder used to build {@link TextSelection} objects.
     */
    public static final class Builder {

        private final int mStartIndex;
        private final int mEndIndex;
        @NonNull private final Map<String, Float> mEntityConfidence = new ArrayMap<>();
        @NonNull private String mSignature = "";

        /**
         * Creates a builder used to build {@link TextSelection} objects.
         *
         * @param startIndex the start index of the text selection.
         * @param endIndex the end index of the text selection. Must be greater than startIndex
         */
        public Builder(@IntRange(from = 0) int startIndex, @IntRange(from = 0) int endIndex) {
            Preconditions.checkArgument(startIndex >= 0);
            Preconditions.checkArgument(endIndex > startIndex);
            mStartIndex = startIndex;
            mEndIndex = endIndex;
        }

        /**
         * Sets an entity type for the classified text and assigns a confidence score.
         *
         * @param confidenceScore a value from 0 (low confidence) to 1 (high confidence).
         *      0 implies the entity does not exist for the classified text.
         *      Values greater than 1 are clamped to 1.
         */
        public Builder setEntityType(
                @NonNull @EntityType String type,
                @FloatRange(from = 0.0, to = 1.0) float confidenceScore) {
            mEntityConfidence.put(type, confidenceScore);
            return this;
        }

        /**
         * Sets a signature for the TextSelection object.
         *
         * The TextClassifier that generates the TextSelection object may use it as a way to
         * internally identify the TextSelection object.
         */
        public Builder setSignature(@NonNull String signature) {
            mSignature = Preconditions.checkNotNull(signature);
            return this;
        }

        /**
         * Builds and returns {@link TextSelection} object.
         */
        public TextSelection build() {
            return new TextSelection(
                    mStartIndex, mEndIndex, mEntityConfidence, mSignature);
        }
    }

    /**
     * Optional input parameters for generating TextSelection.
     */
    public static final class Options implements Parcelable {

        private @Nullable ArrayList<Locale> mDefaultLocales;

        public Options() {}

        /**
         * @param defaultLocales ordered list of locale preferences that may be used to disambiguate
         *      the provided text. If no locale preferences exist, set this to null or an empty
         *      locale list.
         */
        public Options setDefaultLocales(@Nullable Collection<Locale> defaultLocales) {
            mDefaultLocales = defaultLocales == null ? null : new ArrayList<>(defaultLocales);
            return this;
        }

        /**
         * @return ordered list of locale preferences that can be used to disambiguate
         *      the provided text.
         */
        @Nullable
        public List<Locale> getDefaultLocales() {
            return mDefaultLocales;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mDefaultLocales != null ? mDefaultLocales.size() : 0);
            if (mDefaultLocales != null) {
                for (Locale locale : mDefaultLocales) {
                    dest.writeSerializable(locale);
                }
            }
        }

        public static final Parcelable.Creator<Options> CREATOR =
                new Parcelable.Creator<Options>() {
                    @Override
                    public Options createFromParcel(Parcel in) {
                        return new Options(in);
                    }

                    @Override
                    public Options[] newArray(int size) {
                        return new Options[size];
                    }
                };

        private Options(Parcel in) {
            final int numLocales = in.readInt();
            if (numLocales > 0) {
                mDefaultLocales = new ArrayList<>();
                mDefaultLocales.ensureCapacity(numLocales);
                for (int i = 0; i < numLocales; ++i) {
                    mDefaultLocales.add((Locale) in.readSerializable());
                }
            }
        }
    }
}
