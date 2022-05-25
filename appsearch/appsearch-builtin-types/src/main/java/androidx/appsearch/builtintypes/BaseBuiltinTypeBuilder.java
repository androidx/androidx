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

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

/**
 * Default Builder for an AppSearch built in type. This builder includes all the default
 * AppSearch properties.
 */
abstract class BaseBuiltinTypeBuilder<T extends BaseBuiltinTypeBuilder<T>> {
    protected final String mNamespace;
    protected final String mId;
    protected int mDocumentScore;
    protected long mCreationTimestampMillis;
    protected long mDocumentTtlMillis;

    protected BaseBuiltinTypeBuilder(@NonNull String namespace, @NonNull String id) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);

        // Default for unset creationTimestampMillis. AppSearch will internally convert this
        // to current time when creating the GenericDocument.
        mCreationTimestampMillis = -1;
    }

    /**
     * Sets the user-provided opaque document score of the current AppSearch document, which can
     * be used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.Score} for more information on score.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public T setDocumentScore(int documentScore) {
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
    @NonNull
    @SuppressWarnings("unchecked")
    public T setCreationTimestampMillis(long creationTimestampMillis) {
        mCreationTimestampMillis = creationTimestampMillis;
        return (T) this;
    }

    /**
     * Sets the time-to-live (TTL) for the current AppSearch document as a duration in milliseconds.
     *
     * <p>The document will be automatically deleted when the TTL expires.
     *
     * <p>If not set, then the document will never expire.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on
     * TTL.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public T setDocumentTtlMillis(long documentTtlMillis) {
        mDocumentTtlMillis = documentTtlMillis;
        return (T) this;
    }
}
