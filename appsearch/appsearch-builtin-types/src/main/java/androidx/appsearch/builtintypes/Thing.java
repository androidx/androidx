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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppSearch document representing a <a href="http://schema.org/Thing">Thing</a>, the most generic
 * type of an item.
 */
@Document(name = "builtin:Thing")
public final class Thing {
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

    Thing(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            @NonNull List<String> alternateNames, @Nullable String description,
            @Nullable String image, @Nullable String url) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mDocumentScore = documentScore;
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mName = name;
        mAlternateNames = Collections.unmodifiableList(alternateNames);
        mDescription = description;
        mImage = image;
        mUrl = url;
    }

    /** Returns the namespace (or logical grouping) for this item. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier for this item. */
    @NonNull
    public String getId() {
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
    @Nullable
    public String getName() {
        return mName;
    }

    /** Returns an unmodifiable list of aliases, if any, for this item. */
    @NonNull
    public List<String> getAlternateNames() {
        return mAlternateNames;
    }

    /** Returns a description of this item. */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /** Returns the URL for an image of this item. */
    @Nullable
    public String getImage() {
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
    @Nullable
    public String getUrl() {
        return mUrl;
    }

    /** Builder for {@link Thing}. */
    public static final class Builder extends BaseBuiltinTypeBuilder<Builder> {
        private String mName;
        private List<String> mAlternateNames = new ArrayList<>();
        private String mDescription;
        private String mImage;
        private String mUrl;

        /** Constructs {@link Thing.Builder} with given {@code namespace} and {@code id} */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /** Constructs {@link Thing.Builder} from existing values in given {@link Thing}. */
        public Builder(@NonNull Thing thing) {
            this(thing.getNamespace(), thing.getId());
            mDocumentScore = thing.getDocumentScore();
            mCreationTimestampMillis = thing.getCreationTimestampMillis();
            mDocumentTtlMillis = thing.getDocumentTtlMillis();
            mName = thing.getName();
            mAlternateNames = new ArrayList<>(thing.getAlternateNames());
            mDescription = thing.getDescription();
            mImage = thing.getImage();
            mUrl = thing.getUrl();
        }

        /** Sets the name of the item. */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /** Adds an alias for the item. */
        @NonNull
        public Builder addAlternateName(@NonNull String alternateName) {
            Preconditions.checkNotNull(alternateName);
            mAlternateNames.add(alternateName);
            return this;
        }

        /** Clears the aliases, if any, for the item. */
        @NonNull
        public Builder clearAlternateNames() {
            mAlternateNames.clear();
            return this;
        }

        /** Sets the description for the item. */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            mDescription = description;
            return this;
        }

        /** Sets the URL for an image of the item. */
        @NonNull
        public Builder setImage(@Nullable String image) {
            mImage = image;
            return this;
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
        @NonNull
        public Builder setUrl(@Nullable String url) {
            mUrl = url;
            return this;
        }

        /** Builds a {@link Thing} object. */
        @NonNull
        public Thing build() {
            return new Thing(mNamespace, mId, mDocumentScore, mCreationTimestampMillis,
                    mDocumentTtlMillis, mName, mAlternateNames, mDescription, mImage, mUrl);
        }
    }
}
