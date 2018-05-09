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

import static androidx.textclassifier.ConvertUtils.buildZonedDateTimeFromCalendar;
import static androidx.textclassifier.ConvertUtils.unwrapLocalListCompat;

import android.app.RemoteAction;
import android.os.Bundle;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Information for generating a widget to handle classified text.
 *
 * <p>A TextClassification object contains icons, labels, and intents that may be used to build a
 * widget that can be used to act on classified text.
 *
 * <p>e.g. building a menu that allows the user how to act on a piece of text:
 *
 * <pre>{@code
 *   // Called preferably outside the UiThread.
 *   TextClassification classification = textClassifier.classifyText(allText, 10, 25);
 *
 *   // Called on the UiThread.
 *   for (RemoteActionCompat action : classification.getActions()) {
 *       MenuItem item = menu.add(action.getTitle());
 *       item.setContentDescription(action.getContentDescription());
 *       item.setOnMenuItemClickListener(v -> action.getActionIntent().send());
 *       if (action.shouldShowIcon()) {
 *           item.setIcon(action.getIcon().loadDrawable(context));
 *       }
 *   }
 * }</pre>
 */
public final class TextClassification {

    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_ACTIONS = "actions";
    private static final String EXTRA_ENTITY_CONFIDENCE = "entity_conf";
    private static final String EXTRA_ID = "id";

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static final TextClassification EMPTY = new TextClassification.Builder().build();

    @Nullable private final String mText;
    @NonNull private final List<RemoteActionCompat> mActions;
    @NonNull private final EntityConfidence mEntityConfidence;
    @Nullable private final String mId;

    private TextClassification(
            @Nullable String text,
            @NonNull List<RemoteActionCompat> actions,
            @NonNull EntityConfidence entityConfidence,
            @Nullable String id) {
        mText = text;
        mActions = actions;
        mEntityConfidence = entityConfidence;
        mId = id;
    }

    /**
     * Gets the classified text.
     */
    @Nullable
    public String getText() {
        return mText;
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
     * Returns a list of actions that may be performed on the text. The list is ordered based on
     * the likelihood that a user will use the action, with the most likely action appearing first.
     */
    @NonNull
    public List<RemoteActionCompat> getActions() {
        return mActions;
    }

    /**
     * Returns the id for this object.
     */
    @Nullable
    public String getId() {
        return mId;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "TextClassification {text=%s, entities=%s, actions=%s, id=%s}",
                mText, mEntityConfidence, mActions, mId);
    }

    /**
     * Adds this classification to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TEXT, mText);
        BundleUtils.putRemoteActionList(bundle, EXTRA_ACTIONS, mActions);
        BundleUtils.putMap(bundle, EXTRA_ENTITY_CONFIDENCE, mEntityConfidence.getConfidenceMap());
        bundle.putString(EXTRA_ID, mId);
        return bundle;
    }

    /**
     * Extracts a classification from a bundle that was added using {@link #toBundle()}.
     * @throws IllegalArgumentException
     */
    @NonNull
    public static TextClassification createFromBundle(@NonNull Bundle bundle) {
        final Builder builder = new Builder()
                .setText(bundle.getString(EXTRA_TEXT))
                .setId(bundle.getString(EXTRA_ID));
        for (Map.Entry<String, Float> entityConfidence : BundleUtils.getFloatStringMapOrThrow(
                bundle, EXTRA_ENTITY_CONFIDENCE).entrySet()) {
            builder.setEntityType(entityConfidence.getKey(), entityConfidence.getValue());
        }
        for (RemoteActionCompat action : BundleUtils.getRemoteActionListOrThrow(
                bundle, EXTRA_ACTIONS)) {
            builder.addAction(action);
        }
        return builder.build();
    }


    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(28)
    static final class Convert {

        private Convert() {
        }

        @NonNull
        static TextClassification fromPlatform(
                @NonNull android.view.textclassifier.TextClassification textClassification) {
            Preconditions.checkNotNull(textClassification);

            Builder builder = new TextClassification.Builder()
                    .setText(textClassification.getText())
                    .setId(textClassification.getId());

            final int entityCount = textClassification.getEntityCount();
            for (int i = 0; i < entityCount; i++) {
                String entity = textClassification.getEntity(i);
                builder.setEntityType(entity, textClassification.getConfidenceScore(entity));
            }

            List<RemoteAction> actions = textClassification.getActions();
            for (RemoteAction action: actions) {
                builder.addAction(RemoteActionCompat.createFromRemoteAction(action));
            }

            return builder.build();
        }
    }

    /**
     * Builder for building {@link TextClassification} objects.
     *
     * <p>e.g.
     *
     * <pre>{@code
     *   TextClassification classification = new TextClassification.Builder()
     *          .setText(classifiedText)
     *          .setEntityType(TextClassifier.TYPE_EMAIL, 0.9)
     *          .setEntityType(TextClassifier.TYPE_OTHER, 0.1)
     *          .addAction(remoteAction1)
     *          .addAction(remoteAction2)
     *          .build();
     * }</pre>
     */
    public static final class Builder {

        @Nullable private String mText;
        @NonNull private List<RemoteActionCompat> mActions = new ArrayList<>();
        @NonNull private final Map<String, Float> mEntityConfidence = new ArrayMap<>();
        @Nullable private String mId = "";

        /**
         * Sets the classified text.
         */
        public Builder setText(@Nullable String text) {
            mText = text;
            return this;
        }

        /**
         * Sets an entity type for the classification result and assigns a confidence score.
         * If a confidence score had already been set for the specified entity type, this will
         * override that score.
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
         * Adds an action that may be performed on the classified text. Actions should be added in
         * order of likelihood that the user will use them, with the most likely action being added
         * first.
         */
        @NonNull
        public Builder addAction(@NonNull RemoteActionCompat action) {
            Preconditions.checkArgument(action != null);
            mActions.add(action);
            return this;
        }

        /**
         * Sets an id for the TextClassification object.
         */
        @NonNull
        public Builder setId(@Nullable String id) {
            mId = id;
            return this;
        }

        /**
         * Builds and returns a {@link TextClassification} object.
         */
        @NonNull
        public TextClassification build() {
            return new TextClassification(
                    mText, mActions, new EntityConfidence(mEntityConfidence), mId);
        }
    }

    /**
     * A request object for generating TextClassification.
     */
    public static final class Request {

        private static final String EXTRA_TEXT = "text";
        private static final String EXTRA_START_INDEX = "start";
        private static final String EXTRA_END_INDEX = "end";
        private static final String EXTRA_DEFAULT_LOCALES = "locales";
        private static final String EXTRA_REFERENCE_TIME = "reftime";
        private static final String EXTRA_CALLING_PACKAGE_NAME = "calling_package";

        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        @Nullable private final LocaleListCompat mDefaultLocales;
        @Nullable private final Calendar mReferenceTime;

        private Request(
                CharSequence text,
                int startIndex,
                int endIndex,
                LocaleListCompat defaultLocales,
                Calendar referenceTime) {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mReferenceTime = referenceTime;
        }

        /**
         * Returns the text providing context for the text to classify (which is specified
         *      by the sub sequence starting at startIndex and ending at endIndex)
         */
        @NonNull
        public CharSequence getText() {
            return mText;
        }

        /**
         * Returns start index of the text to classify.
         */
        @IntRange(from = 0)
        public int getStartIndex() {
            return mStartIndex;
        }

        /**
         * Returns end index of the text to classify.
         */
        @IntRange(from = 0)
        public int getEndIndex() {
            return mEndIndex;
        }

        /**
         * @return ordered list of locale preferences that can be used to disambiguate
         *      the provided text.
         */
        @Nullable
        public LocaleListCompat getDefaultLocales() {
            return mDefaultLocales;
        }

        /**
         * @return reference time based on which relative dates (e.g. "tomorrow") should be
         *      interpreted.
         */
        @Nullable
        public Calendar getReferenceTime() {
            return mReferenceTime;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        static final class Convert {

            private Convert() {
            }

            @NonNull
            static android.view.textclassifier.TextClassification.Request toPlatform(
                    @NonNull Request request) {
                Preconditions.checkNotNull(request);

                return new android.view.textclassifier.TextClassification.Request.Builder(
                        request.mText, request.mStartIndex, request.mEndIndex)
                        .setDefaultLocales(unwrapLocalListCompat(request.getDefaultLocales()))
                        .setReferenceTime(buildZonedDateTimeFromCalendar(request.mReferenceTime))
                        .build();
            }
        }

        /**
         * A builder for building TextClassification requests.
         */
        public static final class Builder {

            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;

            @Nullable private LocaleListCompat mDefaultLocales;
            @Nullable private Calendar mReferenceTime;

            /**
             * @param text text providing context for the text to classify (which is specified
             *      by the sub sequence starting at startIndex and ending at endIndex)
             * @param startIndex start index of the text to classify
             * @param endIndex end index of the text to classify
             */
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
             * @return this builder
             */
            @NonNull
            public Builder setDefaultLocales(@Nullable LocaleListCompat defaultLocales) {
                mDefaultLocales = defaultLocales;
                return this;
            }

            /**
             * @param referenceTime reference time based on which relative dates (e.g. "tomorrow"
             *      should be interpreted. This should usually be the time when the text was
             *      originally composed. If no reference time is set, now is used.
             *
             * @return this builder
             */
            @NonNull
            public Builder setReferenceTime(@Nullable Calendar referenceTime) {
                mReferenceTime = referenceTime;
                return this;
            }

            /**
             * Builds and returns the request object.
             */
            @NonNull
            public Request build() {
                return new Request(mText, mStartIndex, mEndIndex, mDefaultLocales, mReferenceTime);
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
            BundleUtils.putCalendar(bundle, EXTRA_REFERENCE_TIME, mReferenceTime);
            return bundle;
        }

        /**
         * Extracts a Request from a bundle that was added using {@link #toBundle()}.
         */
        public static Request createFromBundle(@NonNull Bundle bundle) {
            final Builder builder = new Builder(
                    bundle.getString(EXTRA_TEXT),
                    bundle.getInt(EXTRA_START_INDEX),
                    bundle.getInt(EXTRA_END_INDEX))
                    .setDefaultLocales(BundleUtils.getLocaleList(bundle, EXTRA_DEFAULT_LOCALES))
                    .setReferenceTime(BundleUtils.getCalendar(bundle, EXTRA_REFERENCE_TIME));
            final Request request = builder.build();
            return request;
        }
    }
}
