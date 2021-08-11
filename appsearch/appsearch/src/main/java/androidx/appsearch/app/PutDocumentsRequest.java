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

package androidx.appsearch.app;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a request to index documents into an {@link AppSearchSession} database.
 *
 * <!--@exportToFramework:ifJetpack()-->
 * <p>Documents added to the request can be instances of classes annotated with
 * {@link androidx.appsearch.annotation.Document} or instances of
 * {@link GenericDocument}.
 * <!--@exportToFramework:else()-->
 *
 * @see AppSearchSession#put
 */
public final class PutDocumentsRequest {
    private final List<GenericDocument> mDocuments;

    PutDocumentsRequest(List<GenericDocument> documents) {
        mDocuments = documents;
    }

    /** Returns a list of {@link GenericDocument} objects that are part of this request. */
    @NonNull
    public List<GenericDocument> getGenericDocuments() {
        return Collections.unmodifiableList(mDocuments);
    }

    /** Builder for {@link PutDocumentsRequest} objects. */
    public static final class Builder {
        private ArrayList<GenericDocument> mDocuments = new ArrayList<>();
        private boolean mBuilt = false;

        /** Adds one or more {@link GenericDocument} objects to the request. */
        @NonNull
        public Builder addGenericDocuments(@NonNull GenericDocument... documents) {
            Preconditions.checkNotNull(documents);
            resetIfBuilt();
            return addGenericDocuments(Arrays.asList(documents));
        }

        /** Adds a collection of {@link GenericDocument} objects to the request. */
        @NonNull
        public Builder addGenericDocuments(
                @NonNull Collection<? extends GenericDocument> documents) {
            Preconditions.checkNotNull(documents);
            resetIfBuilt();
            mDocuments.addAll(documents);
            return this;
        }

// @exportToFramework:startStrip()
        /**
         * Adds one or more annotated {@link androidx.appsearch.annotation.Document}
         * documents to the request.
         *
         * @param documents annotated
         *                    {@link androidx.appsearch.annotation.Document} documents.
         * @throws AppSearchException if an error occurs converting a document class into a
         *                            {@link GenericDocument}.
         */
        // Merged list available from getGenericDocuments()
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addDocuments(@NonNull Object... documents) throws AppSearchException {
            Preconditions.checkNotNull(documents);
            resetIfBuilt();
            return addDocuments(Arrays.asList(documents));
        }

        /**
         * Adds a collection of annotated
         * {@link androidx.appsearch.annotation.Document} documents to the request.
         *
         * @param documents annotated
         *                    {@link androidx.appsearch.annotation.Document} documents.
         * @throws AppSearchException if an error occurs converting a document into a
         *                            {@link GenericDocument}.
         */
        // Merged list available from getGenericDocuments()
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addDocuments(@NonNull Collection<?> documents) throws AppSearchException {
            Preconditions.checkNotNull(documents);
            resetIfBuilt();
            List<GenericDocument> genericDocuments = new ArrayList<>(documents.size());
            for (Object document : documents) {
                GenericDocument genericDocument = GenericDocument.fromDocumentClass(document);
                genericDocuments.add(genericDocument);
            }
            return addGenericDocuments(genericDocuments);
        }
// @exportToFramework:endStrip()

        /** Creates a new {@link PutDocumentsRequest} object. */
        @NonNull
        public PutDocumentsRequest build() {
            mBuilt = true;
            return new PutDocumentsRequest(mDocuments);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mDocuments = new ArrayList<>(mDocuments);
                mBuilt = false;
            }
        }
    }
}
