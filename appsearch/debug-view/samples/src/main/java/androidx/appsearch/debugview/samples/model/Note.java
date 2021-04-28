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
package androidx.appsearch.debugview.samples.model;

import androidx.annotation.NonNull;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

/**
 * Encapsulates a Note document.
 */
@Document
public class Note {

    Note(@NonNull String namespace, @NonNull String id, @NonNull String text) {
        mId = Preconditions.checkNotNull(id);
        mNamespace = Preconditions.checkNotNull(namespace);
        mText = Preconditions.checkNotNull(text);
    }

    @Document.Id
    private final String mId;

    @Document.Namespace
    private final String mNamespace;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mText;

    /** Returns the ID of the {@link Note} object. */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the namespace of the {@link Note} object. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the text of the {@link Note} object. */
    @NonNull
    public String getText() {
        return mText;
    }

    @Override
    @NonNull
    public String toString() {
        return mText;
    }

    /**
     * Builder for {@link Note} objects.
     *
     * <p>Once {@link #build} is called, the instance can no longer be used.
     */
    public static final class Builder {
        private String mNamespace = "";
        private String mId = "";
        private String mText = "";
        private boolean mBuilt = false;

        /**
         * Sets the namespace of the {@link Note} object.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Note.Builder setNamespace(@NonNull String namespace) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mNamespace = Preconditions.checkNotNull(namespace);
            return this;
        }

        /**
         * Sets the ID of the {@link Note} object.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Note.Builder setId(@NonNull String id) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mId = Preconditions.checkNotNull(id);
            return this;
        }

        /**
         * Sets the text of the {@link Note} object.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Note.Builder setText(@NonNull String text) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mText = Preconditions.checkNotNull(text);
            return this;
        }

        /**
         * Creates a new {@link Note} object.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Note build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;
            return new Note(mNamespace, mId, mText);
        }
    }
}
