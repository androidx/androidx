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
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityType;

import java.util.ArrayList;
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
    private static final String EXTRA_EXTRAS = "extras";
    private static final IconCompat NO_ICON =
            IconCompat.createWithData(new byte[0], 0, 0);

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static final TextClassification EMPTY = new TextClassification.Builder().build();

    @Nullable private final String mText;
    @NonNull private final List<RemoteActionCompat> mActions;
    @NonNull private final EntityConfidence mEntityConfidence;
    @Nullable private final String mId;
    @NonNull private final Bundle mExtras;

    TextClassification(
            @Nullable String text,
            @NonNull List<RemoteActionCompat> actions,
            @NonNull EntityConfidence entityConfidence,
            @Nullable String id,
            @NonNull Bundle extras) {
        mText = text;
        mActions = actions;
        mEntityConfidence = entityConfidence;
        mId = id;
        mExtras = extras;
    }

    /**
     * Gets the classified text.
     */
    @Nullable
    public CharSequence getText() {
        return mText;
    }

    /**
     * Returns the number of entity types found in the classified text.
     */
    @IntRange(from = 0)
    public int getEntityTypeCount() {
        return mEntityConfidence.getEntities().size();
    }

    /**
     * Returns the entity type at the specified index. Entities are ordered from high confidence
     * to low confidence.
     *
     * @throws IndexOutOfBoundsException if the specified index is out of range.
     * @see #getEntityTypeCount() for the number of entities available.
     */
    @NonNull
    public @EntityType String getEntityType(int index) {
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
        bundle.putBundle(EXTRA_EXTRAS, mExtras);
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
                .setId(bundle.getString(EXTRA_ID))
                .setExtras(bundle.getBundle(EXTRA_EXTRAS));
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
    @RequiresApi(26)
    @SuppressWarnings("deprecation") // To support O
    @NonNull
    @SuppressLint("RestrictedApi")
    static TextClassification fromPlatform(
            @NonNull Context context,
            @NonNull android.view.textclassifier.TextClassification textClassification) {
        Preconditions.checkNotNull(textClassification);

        Builder builder = new TextClassification.Builder()
                .setText(textClassification.getText());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setId(textClassification.getId());
        }

        final int entityCount = textClassification.getEntityCount();
        for (int i = 0; i < entityCount; i++) {
            String entity = textClassification.getEntity(i);
            builder.setEntityType(entity, textClassification.getConfidenceScore(entity));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            List<RemoteAction> actions = textClassification.getActions();
            for (RemoteAction action : actions) {
                builder.addAction(RemoteActionCompat.createFromRemoteAction(action));
            }
        } else {
            if (textClassification.getIntent() != null
                    && !TextUtils.isEmpty(textClassification.getLabel())) {
                builder.addAction(createRemoteActionCompat(context, textClassification));
            }
        }
        return builder.build();
    }

    /**
     * Converts a given {@link TextClassification} object to a {@link RemoteActionCompat} object.
     * It is assumed that the intent and the label in the textclassification object are not null.
     */
    @TargetApi(26)
    @SuppressWarnings("deprecation") //To support O
    @NonNull
    private static RemoteActionCompat createRemoteActionCompat(
            @NonNull Context context,
            @NonNull android.view.textclassifier.TextClassification textClassification) {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        textClassification.getText().hashCode(),
                        textClassification.getIntent(),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Drawable drawable = textClassification.getIcon();
        CharSequence label = textClassification.getLabel();
        IconCompat icon;
        if (drawable == null) {
            // Placeholder, should never be shown.
            icon = NO_ICON;
        } else {
            icon = ConvertUtils.createIconFromDrawable(textClassification.getIcon());
        }
        RemoteActionCompat remoteAction = new RemoteActionCompat(icon, label, label, pendingIntent);
        remoteAction.setShouldShowIcon(drawable != null);
        return remoteAction;
    }

    /**
     * @hide
     */
    // Lint does not know @EntityType in platform and here are same.
    @SuppressLint({"WrongConstant", "RestrictedApi"})
    @SuppressWarnings("deprecation") // To support O
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresApi(26)
    @NonNull
    Object toPlatform(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        android.view.textclassifier.TextClassification.Builder builder =
                new android.view.textclassifier.TextClassification.Builder()
                        .setText(getText() == null ? null : getText().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setId(getId());
        }

        final int entityCount = getEntityTypeCount();
        for (int i = 0; i < entityCount; i++) {
            String entity = getEntityType(i);
            builder.setEntityType(entity, getConfidenceScore(entity));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            List<RemoteActionCompat> actions = getActions();
            for (RemoteActionCompat action : actions) {
                builder.addAction(action.toRemoteAction());
            }
        }

        if (!getActions().isEmpty()) {
            final RemoteActionCompat firstAction = getActions().get(0);
            builder.setLabel(firstAction.getTitle().toString())
                    .setIcon(firstAction.getIcon().loadDrawable(context))
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                firstAction.getActionIntent().send();
                            } catch (PendingIntent.CanceledException e) {
                                Log.e(TextClassifier.DEFAULT_LOG_TAG, "Failed to start action ", e);
                            }
                        }
                    });
        }

        return builder.build();
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
        @Nullable private String mId;
        @Nullable private Bundle mExtras;

        /**
         * Sets the classified text.
         */
        public Builder setText(@Nullable CharSequence text) {
            mText = text == null ? null : text.toString();
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
        @SuppressLint("RestrictedApi")
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
         * Sets the extended, vendor specific data.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds and returns a {@link TextClassification} object.
         */
        @NonNull
        public TextClassification build() {
            return new TextClassification(
                    mText, mActions, new EntityConfidence(mEntityConfidence), mId,
                    mExtras == null ? Bundle.EMPTY : BundleUtils.deepCopy(mExtras));
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
        @Nullable private final Long mReferenceTime;
        @NonNull private final Bundle mExtras;

        Request(
                CharSequence text,
                int startIndex,
                int endIndex,
                LocaleListCompat defaultLocales,
                Long referenceTime,
                Bundle extras) {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mReferenceTime = referenceTime;
            mExtras = extras;
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
         *      interpreted. This should be milliseconds from the epoch of
         *      1970-01-01T00:00:00Z(UTC timezone).
         */
        @Nullable
        public Long getReferenceTime() {
            return mReferenceTime;
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
        static TextClassification.Request fromPlatform(
                @NonNull android.view.textclassifier.TextClassification.Request request) {
            return new TextClassification.Request.Builder(
                    request.getText(), request.getStartIndex(), request.getEndIndex())
                    .setReferenceTime(ConvertUtils.zonedDateTimeToUtcMs(request.getReferenceTime()))
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
            return new android.view.textclassifier.TextClassification.Request.Builder(
                    mText, mStartIndex, mEndIndex)
                    .setDefaultLocales(ConvertUtils.unwrapLocalListCompat(getDefaultLocales()))
                    .setReferenceTime(ConvertUtils.createZonedDateTimeFromUtc(mReferenceTime))
                    .build();
        }

        /**
         * A builder for building TextClassification requests.
         */
        public static final class Builder {

            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;
            private Bundle mExtras;

            @Nullable private LocaleListCompat mDefaultLocales;
            @Nullable private Long mReferenceTime = null;

            /**
             * @param text text providing context for the text to classify (which is specified
             *      by the sub sequence starting at startIndex and ending at endIndex)
             * @param startIndex start index of the text to classify
             * @param endIndex end index of the text to classify
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
             * @return this builder
             */
            @NonNull
            public Builder setDefaultLocales(@Nullable LocaleListCompat defaultLocales) {
                mDefaultLocales = defaultLocales;
                return this;
            }

            /**
             * @param referenceTime reference time based on which relative dates (e.g. "tomorrow")
             *      should be interpreted. This should usually be the time when the text was
             *      originally composed and should be milliseconds from the epoch of
             *      1970-01-01T00:00:00Z(UTC timezone). For example, if there is a message saying
             *      "see you 10 days later", and the message was composed yesterday, text classifier
             *      will then realize it is indeed means 9 days later from now and classify the text
             *      accordingly. If no reference time is set, now is used.
             *
             * @return this builder
             */
            @NonNull
            public Builder setReferenceTime(@Nullable Long referenceTime) {
                mReferenceTime = referenceTime;
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
                return new Request(mText, mStartIndex, mEndIndex, mDefaultLocales, mReferenceTime,
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
            BundleUtils.putLong(bundle, EXTRA_REFERENCE_TIME, mReferenceTime);
            bundle.putBundle(EXTRA_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Extracts a Request from a bundle that was added using {@link #toBundle()}.
         */
        public static Request createFromBundle(@NonNull Bundle bundle) {
            final Builder builder = new Builder(
                    bundle.getCharSequence(EXTRA_TEXT),
                    bundle.getInt(EXTRA_START_INDEX),
                    bundle.getInt(EXTRA_END_INDEX))
                    .setDefaultLocales(BundleUtils.getLocaleList(bundle, EXTRA_DEFAULT_LOCALES))
                    .setReferenceTime(BundleUtils.getLong(bundle, EXTRA_REFERENCE_TIME))
                    .setExtras(bundle.getBundle(EXTRA_EXTRAS));
            return builder.build();
        }
    }
}
