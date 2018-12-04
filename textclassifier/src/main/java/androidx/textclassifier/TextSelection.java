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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityType;

import java.util.Locale;
import java.util.Map;

/**
 * Information about where text selection should be.
 */
public final class TextSelection {

    private static final String EXTRA_START_INDEX = "start";
    private static final String EXTRA_END_INDEX = "end";
    private static final String EXTRA_ENTITY_CONFIDENCE = "entity_conf";
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_EXTRAS = "extras";

    private final int mStartIndex;
    private final int mEndIndex;
    @NonNull private final EntityConfidence mEntityConfidence;
    @Nullable private final String mId;
    @NonNull private final Bundle mExtras;

    TextSelection(
            int startIndex,
            int endIndex,
            @NonNull EntityConfidence entityConfidence,
            @Nullable String id,
            @NonNull Bundle extras) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mEntityConfidence = entityConfidence;
        mId = id;
        mExtras = extras;
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
     * Returns the id, if one exists, for this object.
     */
    @Nullable
    public String getId() {
        return mId;
    }

    /**
     * Returns the extended, vendor specific data.
     *
     * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
     * prefer to hold a reference to the returned bundle rather than frequently calling this
     * method. Avoid updating the content of this bundle. On pre-O devices, the values in the
     * Bundle are not deep copied.
     */
    @NonNull
    public Bundle getExtras() {
        return BundleUtils.deepCopy(mExtras);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "TextSelection {id=%s, startIndex=%d, endIndex=%d, entities=%s}",
                mId, mStartIndex, mEndIndex, mEntityConfidence);
    }

    /**
     * Adds this selection to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_START_INDEX, mStartIndex);
        bundle.putInt(EXTRA_END_INDEX, mEndIndex);
        BundleUtils.putMap(bundle, EXTRA_ENTITY_CONFIDENCE, mEntityConfidence.getConfidenceMap());
        bundle.putString(EXTRA_ID, mId);
        bundle.putBundle(EXTRA_EXTRAS, mExtras);
        return bundle;
    }

    /**
     * Extracts a selection from a bundle that was added using {@link #toBundle()}.
     */
    @NonNull
    public static TextSelection createFromBundle(@NonNull Bundle bundle) {
        final Builder builder = new Builder(
                bundle.getInt(EXTRA_START_INDEX),
                bundle.getInt(EXTRA_END_INDEX))
                .setId(bundle.getString(EXTRA_ID))
                .setExtras(bundle.getBundle(EXTRA_EXTRAS));
        for (Map.Entry<String, Float> entityConfidence : BundleUtils.getFloatStringMapOrThrow(
                bundle, EXTRA_ENTITY_CONFIDENCE).entrySet()) {
            builder.setEntityType(entityConfidence.getKey(), entityConfidence.getValue());
        }
        return builder.build();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(26)
    @NonNull
    @SuppressLint("RestrictedApi")
    static TextSelection fromPlatform(
            @NonNull android.view.textclassifier.TextSelection textSelection) {
        Preconditions.checkNotNull(textSelection);

        Builder builder = new Builder(
                textSelection.getSelectionStartIndex(), textSelection.getSelectionEndIndex());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setId(textSelection.getId());
        }

        final int entityCount = textSelection.getEntityCount();
        for (int i = 0; i < entityCount; i++) {
            String entity = textSelection.getEntity(i);
            builder.setEntityType(entity, textSelection.getConfidenceScore(entity));
        }

        return builder.build();
    }

    /**
     * @hide
     */
    @SuppressLint("WrongConstant") // Lint does not know @EntityType in platform and here are same.
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(26)
    @NonNull
    Object toPlatform() {
        android.view.textclassifier.TextSelection.Builder builder =
                new android.view.textclassifier.TextSelection.Builder(
                        getSelectionStartIndex(),
                        getSelectionEndIndex());
        if (getId() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setId(getId());
        }

        final int entityCount = getEntityCount();
        for (int i = 0; i < entityCount; i++) {
            String entity = getEntity(i);
            builder.setEntityType(entity, getConfidenceScore(entity));
        }
        return builder.build();
    }

    /**
     * Builder used to build {@link TextSelection} objects.
     */
    public static final class Builder {

        private final int mStartIndex;
        private final int mEndIndex;
        @NonNull private final Map<String, Float> mEntityConfidence = new ArrayMap<>();
        @Nullable private String mId;
        @Nullable private Bundle mExtras;

        /**
         * Creates a builder used to build {@link TextSelection} objects.
         *
         * @param startIndex the start index of the text selection.
         * @param endIndex the end index of the text selection. Must be greater than startIndex
         */
        @SuppressLint("RestrictedApi")
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
        @NonNull
        public Builder setEntityType(
                @NonNull @EntityType String type,
                @FloatRange(from = 0.0, to = 1.0) float confidenceScore) {
            mEntityConfidence.put(type, confidenceScore);
            return this;
        }

        /**
         * Sets an id for the TextSelection object.
         */
        @NonNull
        public Builder setId(@Nullable String id) {
            mId = id;
            return this;
        }

        /**
         * Sets the extended, vendor specific data.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds and returns {@link TextSelection} object.
         */
        @NonNull
        public TextSelection build() {
            return new TextSelection(
                    mStartIndex, mEndIndex, new EntityConfidence(mEntityConfidence), mId,
                    mExtras == null ? Bundle.EMPTY : BundleUtils.deepCopy(mExtras));
        }
    }

    /**
     * A request object for generating TextSelection.
     */
    public static final class Request {

        private static final String EXTRA_TEXT = "text";
        private static final String EXTRA_START_INDEX = "start";
        private static final String EXTRA_END_INDEX = "end";
        private static final String EXTRA_DEFAULT_LOCALES = "locales";
        private static final String EXTRA_CALLING_PACKAGE_NAME = "calling_package";

        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        @Nullable private final LocaleListCompat mDefaultLocales;
        @NonNull private final Bundle mExtras;

        Request(
                CharSequence text,
                int startIndex,
                int endIndex,
                LocaleListCompat defaultLocales,
                Bundle extras) {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mExtras = extras;
        }

        /**
         * Returns the text providing context for the selected text (which is specified by the
         * sub sequence starting at startIndex and ending at endIndex).
         */
        @NonNull
        public CharSequence getText() {
            return mText;
        }

        /**
         * Returns start index of the selected part of text.
         */
        @IntRange(from = 0)
        public int getStartIndex() {
            return mStartIndex;
        }

        /**
         * Returns end index of the selected part of text.
         */
        @IntRange(from = 0)
        public int getEndIndex() {
            return mEndIndex;
        }

        /**
         * @return ordered list of locale preferences that can be used to disambiguate the
         * provided text.
         */
        @Nullable
        public LocaleListCompat getDefaultLocales() {
            return mDefaultLocales;
        }

        /**
         * Returns the extended, vendor specific data.
         *
         * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
         * prefer to hold a reference to the returned bundle rather than frequently calling this
         * method. Avoid updating the content of this bundle. On pre-O devices, the values in the
         * Bundle are not deep copied.
         */
        @NonNull
        public Bundle getExtras() {
            return BundleUtils.deepCopy(mExtras);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @NonNull
        static TextSelection.Request fromPlatfrom(
                @NonNull android.view.textclassifier.TextSelection.Request request) {
            return new TextSelection.Request.Builder(
                    request.getText(), request.getStartIndex(), request.getEndIndex())
                    .setDefaultLocales(ConvertUtils.wrapLocalList(request.getDefaultLocales()))
                    .build();
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @NonNull
        Object toPlatform() {
            return new android.view.textclassifier.TextSelection.Request.Builder(
                    mText, mStartIndex, mEndIndex)
                    .setDefaultLocales(ConvertUtils.unwrapLocalListCompat(mDefaultLocales))
                    .build();
        }

        /**
         * A builder for building TextSelection requests.
         */
        public static final class Builder {

            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;
            private Bundle mExtras;

            @Nullable private LocaleListCompat mDefaultLocales;

            /**
             * @param text text providing context for the selected text (which is specified by the
             *      sub sequence starting at selectionStartIndex and ending at selectionEndIndex)
             * @param startIndex start index of the selected part of text
             * @param endIndex end index of the selected part of text
             */
            @SuppressLint("RestrictedApi")
            public Builder(
                    @NonNull CharSequence text,
                    @IntRange(from = 0) int startIndex,
                    @IntRange(from = 0) int endIndex) {
                Preconditions.checkArgument(text != null);
                Preconditions.checkArgument(startIndex >= 0);
                Preconditions.checkArgument(endIndex <= text.length());
                Preconditions.checkArgument(endIndex > startIndex);
                mText = text;
                mStartIndex = startIndex;
                mEndIndex = endIndex;
            }

            /**
             * @param defaultLocales ordered list of locale preferences that may be used to
             *      disambiguate the provided text. If no locale preferences exist, set this to null
             *      or an empty locale list.
             *
             * @return this builder.
             */
            @NonNull
            public Builder setDefaultLocales(@Nullable LocaleListCompat defaultLocales) {
                mDefaultLocales = defaultLocales;
                return this;
            }

            /**
             * Sets the extended, vendor specific data.
             *
             * @return this builder
             */
            @NonNull
            public Builder setExtras(@Nullable Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Builds and returns the request object.
             */
            @NonNull
            public Request build() {
                return new Request(mText, mStartIndex, mEndIndex, mDefaultLocales,
                        mExtras == null ? Bundle.EMPTY : BundleUtils.deepCopy(mExtras));
            }
        }

        /**
         * Adds this Request to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            bundle.putCharSequence(EXTRA_TEXT, mText);
            bundle.putInt(EXTRA_START_INDEX, mStartIndex);
            bundle.putInt(EXTRA_END_INDEX, mEndIndex);
            BundleUtils.putLocaleList(bundle, EXTRA_DEFAULT_LOCALES, mDefaultLocales);
            bundle.putBundle(EXTRA_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Extracts a Request from a bundle that was added using {@link #toBundle()}.
         */
        @NonNull
        public static Request createFromBundle(@NonNull Bundle bundle) {
            final Builder builder = new Builder(
                    bundle.getString(EXTRA_TEXT),
                    bundle.getInt(EXTRA_START_INDEX),
                    bundle.getInt(EXTRA_END_INDEX))
                    .setDefaultLocales(BundleUtils.getLocaleList(bundle, EXTRA_DEFAULT_LOCALES))
                    .setExtras(bundle.getBundle(EXTRA_EXTRAS));
            final Request request = builder.build();
            return request;
        }
    }
}
