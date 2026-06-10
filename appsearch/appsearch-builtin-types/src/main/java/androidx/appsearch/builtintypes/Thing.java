/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppSearch document representing a <a href="http://schema.org/Thing">Thing</a>, the most generic
 * type of an item.
 */
@Document(name = "builtin:Thing")
public class Thing {
    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.Score
    private final int mDocumentScore;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mName;

    @Document.StringProperty
    private final List<String> mAlternateNames;

    @Document.StringProperty
    private final String mDescription;

    @Document.StringProperty
    private final String mImage;

    @Document.StringProperty
    private final String mUrl;

    @Document.DocumentProperty
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private final List<PotentialAction> mPotentialActions;

    /**
     * Constructor for {@link Thing}.
     *
     * @param builder The builder to construct the {@link Thing} from.
     */
    @ExperimentalAppSearchApi
    public Thing(@NonNull BuilderBase<?> builder) {
        mNamespace = Preconditions.checkNotNull(builder.mNamespace);
        mId = Preconditions.checkNotNull(builder.mId);
        mDocumentScore = builder.mDocumentScore;
        mCreationTimestampMillis = builder.mCreationTimestampMillis;
        mDocumentTtlMillis = builder.mDocumentTtlMillis;
        mName = builder.mName;
        // If an old schema does not define the alternateNames field, AppSearch may attempt to
        // pass in null when converting its GenericDocument to the java class.
        if (builder.mAlternateNames == null) {
            mAlternateNames = Collections.emptyList();
        } else {
            mAlternateNames = Collections.unmodifiableList(builder.mAlternateNames);
        }
        mDescription = builder.mDescription;
        mImage = builder.mImage;
        mUrl = builder.mUrl;
        // AppSearch may pass null if old schema lacks the potentialActions field during
        // GenericDocument to Java class conversion.
        if (builder.mPotentialActions == null) {
            mPotentialActions = Collections.emptyList();
        } else {
            mPotentialActions = Collections.unmodifiableList(builder.mPotentialActions);
        }
    }

    /** Returns the namespace (or logical grouping) for this item. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier for this item. */
    public @NonNull String getId() {
        return mId;
    }

    /** Returns the intrinsic score (or importance) of this item. */
    public int getDocumentScore() {
        return mDocumentScore;
    }

    /** Returns the creation timestamp, in milliseconds since Unix epoch, of this item. */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live timestamp, in milliseconds since
     * {@link #getCreationTimestampMillis()}, for this item.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /** Returns the name of this item. */
    public @Nullable String getName() {
        return mName;
    }

    /** Returns an unmodifiable list of aliases, if any, for this item. */
    public @NonNull List<String> getAlternateNames() {
        return mAlternateNames;
    }

    /** Returns a description of this item. */
    public @Nullable String getDescription() {
        return mDescription;
    }

    /** Returns the URL for an image of this item. */
    // TODO(b/328672505): Discuss with other API reviewers or council whether this API should be
    //  changed to return ImageObject instead for improved flexibility.
    @ExperimentalAppSearchApi
    public @Nullable String getImage() {
        return mImage;
    }

    /**
     * Returns the deeplink URL of this item.
     *
     * <p>This item can be opened (or viewed) by creating an {@link Intent#ACTION_VIEW} intent
     * with this URL as the {@link Intent#setData(Uri)} uri.
     *
     * @see <a href="//reference/android/content/Intent#intent-structure">Intent Structure</a>
     */
    public @Nullable String getUrl() {
        return mUrl;
    }

    /** Returns the actions that can be taken on this object. */
    @ExperimentalAppSearchApi
    public @NonNull List<PotentialAction> getPotentialActions() {
        return mPotentialActions;
    }

    /** Builder for {@link Thing}. */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
        /** Constructs {@link Thing.Builder} with given {@code namespace} and {@code id} */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /** Constructs {@link Thing.Builder} from existing values in given {@link Thing}. */
        public Builder(@NonNull Thing thing) {
            super(thing);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> {
        private final String mNamespace;
        private final String mId;
        private int mDocumentScore;
        private long mCreationTimestampMillis;
        private long mDocumentTtlMillis;
        private String mName;
        private List<String> mAlternateNames = new ArrayList<>();
        private String mDescription;
        private String mImage;
        private String mUrl;
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        private List<PotentialAction> mPotentialActions = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Constructor for {@link Thing.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the Document. See {@link Document.Id}.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);

            // Default for unset creationTimestampMillis. AppSearch will internally convert this
            // to current time when creating the GenericDocument.
            mCreationTimestampMillis = -1;
        }

        /**
         * Constructor for {@link Thing.BuilderBase} from existing values in given {@link Thing}.
         *
         * @param thing The existing {@link Thing} to copy values from.
         */
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        public BuilderBase(@NonNull Thing thing) {
            this(thing.getNamespace(), thing.getId());
            mDocumentScore = thing.getDocumentScore();
            mCreationTimestampMillis = thing.getCreationTimestampMillis();
            mDocumentTtlMillis = thing.getDocumentTtlMillis();
            mName = thing.getName();
            mAlternateNames = new ArrayList<>(thing.getAlternateNames());
            mDescription = thing.getDescription();
            mImage = thing.getImage();
            mUrl = thing.getUrl();
            mPotentialActions = new ArrayList<>(thing.getPotentialActions());
        }

        /**
         * Sets the user-provided opaque document score of the current AppSearch document, which can
         * be used for ranking using
         * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.Score} for more information on
         * score.
         */
        @SuppressWarnings("unchecked")
        public @NonNull T setDocumentScore(int documentScore) {
            resetIfBuilt();
            mDocumentScore = documentScore;
            return (T) this;
        }

        /**
         * Sets the creation timestamp for the current AppSearch entity, in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>This timestamp refers to the creation time of the AppSearch entity, not when the
         * document is written into AppSearch.
         *
         * <p>If not set, then the current timestamp will be used.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
         * information on creation timestamp.
         */
        @SuppressWarnings("unchecked")
        public @NonNull T setCreationTimestampMillis(long creationTimestampMillis) {
            resetIfBuilt();
            mCreationTimestampMillis = creationTimestampMillis;
            return (T) this;
        }

        /**
         * Sets the time-to-live (TTL) for the current AppSearch document as a duration in
         * milliseconds.
         *
         * <p>The document will be automatically deleted when the TTL expires.
         *
         * <p>If not set, then the document will never expire.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on
         * TTL.
         */
        @SuppressWarnings("unchecked")
        public @NonNull T setDocumentTtlMillis(long documentTtlMillis) {
            resetIfBuilt();
            mDocumentTtlMillis = documentTtlMillis;
            return (T) this;
        }

        /** Sets the name of the item. */
        public @NonNull T setName(@Nullable String name) {
            resetIfBuilt();
            mName = name;
            return (T) this;
        }

        /** Adds an alias for the item. */
        public @NonNull T addAlternateName(@NonNull String alternateName) {
            resetIfBuilt();
            Preconditions.checkNotNull(alternateName);
            mAlternateNames.add(alternateName);
            return (T) this;
        }

        /** Sets a list of aliases for the item. */
        public @NonNull T setAlternateNames(@Nullable List<String> alternateNames) {
            resetIfBuilt();
            clearAlternateNames();
            if (alternateNames != null) {
                mAlternateNames.addAll(alternateNames);
            }
            return (T) this;
        }

        /** Clears the aliases, if any, for the item. */
        public @NonNull T clearAlternateNames() {
            resetIfBuilt();
            mAlternateNames.clear();
            return (T) this;
        }

        /** Sets the description for the item. */
        public @NonNull T setDescription(@Nullable String description) {
            resetIfBuilt();
            mDescription = description;
            return (T) this;
        }

        /** Sets the URL for an image of the item. */
        @ExperimentalAppSearchApi
        public @NonNull T setImage(@Nullable String image) {
            resetIfBuilt();
            mImage = image;
            return (T) this;
        }

        /**
         * Sets the deeplink URL of the item.
         *
         * <p>If this item can be displayed by any system UI surface, or can be read by another
         * Android package, through one of the
         * {@link androidx.appsearch.app.SetSchemaRequest.Builder} methods, this {@code url}
         * should act as a deeplink into the activity that can open it. Callers should be able to
         * construct an {@link Intent#ACTION_VIEW} intent with the {@code url} as the
         * {@link Intent#setData(Uri)} to view the item inside your application.
         *
         * <p>See <a href="//training/basics/intents/filters">Allowing Other Apps to Start Your
         * Activity</a> for more details on how to make activities in your app open for use by other
         * apps by defining intent filters.
         */
        public @NonNull T setUrl(@Nullable String url) {
            resetIfBuilt();
            mUrl = url;
            return (T) this;
        }
        /**
         * Add a new action to the list of potential actions for this document.
         */
        @ExperimentalAppSearchApi
        public @NonNull T addPotentialAction(@NonNull PotentialAction newPotentialAction) {
            resetIfBuilt();
            Preconditions.checkNotNull(newPotentialAction);
            mPotentialActions.add(newPotentialAction);
            return (T) this;
        }

        /** Sets a list of potential actions for this document. */
        @ExperimentalAppSearchApi
        public @NonNull T setPotentialActions(@Nullable List<PotentialAction> newPotentialActions) {
            resetIfBuilt();
            clearPotentialActions();
            if (newPotentialActions != null) {
                mPotentialActions.addAll(newPotentialActions);
            }
            return (T) this;
        }

        /**
         * Clear all the potential actions for this document.
         */
        @ExperimentalAppSearchApi
        public @NonNull T clearPotentialActions() {
            resetIfBuilt();
            mPotentialActions.clear();
            return (T) this;
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        protected void resetIfBuilt() {
            if (mBuilt) {
                mAlternateNames = new ArrayList<>(mAlternateNames);
                mPotentialActions = new ArrayList<>(mPotentialActions);
                mBuilt = false;
            }
        }

        /** Builds a {@link Thing} object. */
        public @NonNull Thing build() {
            mBuilt = true;
            return new Thing(this);
        }
    }
}
