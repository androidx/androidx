/*
 * Copyright 2024 The Android Open Source Project
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
// @exportToFramework:skipFile()

package androidx.appsearch.usagereporting;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.Document;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link TakenAction} is an abstract class which holds common fields of other AppSearch built-in
 * action types (e.g. {@link SearchAction}, {@link ClickAction}).
 *
 * <p>Clients can report the user's actions by creating concrete actions with
 * {@link androidx.appsearch.app.PutDocumentsRequest.Builder#addTakenActions} API.
 */
@Document(name = "builtin:TakenAction")
public abstract class TakenAction {
    /** Default TTL for all related {@link TakenAction} documents: 60 days. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final long DEFAULT_DOCUMENT_TTL_MILLIS = 60L * 24 * 60 * 60 * 1000;

    /** AppSearch taken action type. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            ActionConstants.ACTION_TYPE_UNKNOWN,
            ActionConstants.ACTION_TYPE_SEARCH,
            ActionConstants.ACTION_TYPE_CLICK,
            ActionConstants.ACTION_TYPE_IMPRESSION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
    }

    @NonNull
    @Document.Namespace
    private final String mNamespace;

    @NonNull
    @Document.Id
    private final String mId;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.CreationTimestampMillis
    private final long mActionTimestampMillis;

    @Document.LongProperty
    @ActionType
    private final int mActionType;

    TakenAction(@NonNull String namespace, @NonNull String id, long documentTtlMillis,
            long actionTimestampMillis, @ActionType int actionType) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mDocumentTtlMillis = documentTtlMillis;
        mActionTimestampMillis = actionTimestampMillis;
        mActionType = actionType;
    }

    /** Returns the namespace of the {@link TakenAction}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of the {@link TakenAction}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the time-to-live (TTL) of the {@link TakenAction} document as a duration in
     * milliseconds.
     *
     * <p>The document will be automatically deleted when the TTL expires (since
     * {@link #getActionTimestampMillis()}).
     *
     * <p>The default TTL for {@link TakenAction} document is 60 days.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on TTL.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /**
     * Returns the timestamp when the user took the action, in milliseconds since Unix epoch.
     *
     * <p>The action timestamp will be used together with {@link #getDocumentTtlMillis()} as the
     * document retention.
     */
    public long getActionTimestampMillis() {
        return mActionTimestampMillis;
    }

    /**
     * Returns the action type of the {@link TakenAction}.
     *
     * @see TakenAction.ActionType
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ActionType
    public int getActionType() {
        return mActionType;
    }

    // TODO(b/330777270): improve AnnotationProcessor for abstract document class, and remove this
    //                    builder.
    /** Builder for {@link TakenAction}. */
    @Document.BuilderProducer
    static final class Builder extends BuilderImpl<Builder> {
        /**
         * Constructor for {@link TakenAction.Builder}.
         *
         * @param namespace             Namespace for the Document. See {@link Document.Namespace}.
         * @param id                    Unique identifier for the Document. See {@link Document.Id}.
         * @param actionTimestampMillis The timestamp when the user took the action, in milliseconds
         *                              since Unix epoch.
         * @param actionType            Action type enum for the Document. See
         *                              {@link TakenAction.ActionType}.
         */
        Builder(@NonNull String namespace, @NonNull String id, long actionTimestampMillis,
                @TakenAction.ActionType int actionType) {
            super(namespace, id, actionTimestampMillis, actionType);
        }

        /** Constructor for {@link TakenAction.Builder} with all the existing values. */
        Builder(@NonNull TakenAction takenAction) {
            super(takenAction);
        }
    }

    // Use templated BuilderImpl to resolve base class setter return type issue for child class
    // builder instances.
    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> {
        protected final String mNamespace;
        protected final String mId;
        protected long mDocumentTtlMillis;
        protected long mActionTimestampMillis;
        @ActionType
        protected int mActionType;

        /**
         * Constructs {@link TakenAction.BuilderImpl} with given {@code namespace}, {@code id},
         * {@code actionTimestampMillis} and {@code actionType}.
         *
         * @param namespace             The namespace of the {@link TakenAction} document.
         * @param id                    The id of the {@link TakenAction} document.
         * @param actionTimestampMillis The timestamp when the user took the action, in milliseconds
         *                              since Unix epoch.
         * @param actionType            The action type enum of the Document.
         */
        BuilderImpl(@NonNull String namespace, @NonNull String id, long actionTimestampMillis,
                @TakenAction.ActionType int actionType) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);
            mActionTimestampMillis = actionTimestampMillis;
            mActionType = actionType;

            // Default for documentTtlMillis.
            mDocumentTtlMillis = TakenAction.DEFAULT_DOCUMENT_TTL_MILLIS;
        }

        /**
         * Constructs {@link TakenAction.BuilderImpl} by copying existing values from the given
         * {@link TakenAction}.
         *
         * @param takenAction an existing {@link TakenAction} object.
         */
        BuilderImpl(@NonNull TakenAction takenAction) {
            this(takenAction.getNamespace(), takenAction.getId(),
                    takenAction.getActionTimestampMillis(), takenAction.getActionType());
            mDocumentTtlMillis = takenAction.getDocumentTtlMillis();
        }

        /**
         * Sets the time-to-live (TTL) of the {@link TakenAction} document as a duration in
         * milliseconds.
         *
         * <p>The document will be automatically deleted when the TTL expires (since
         * {@link TakenAction#getActionTimestampMillis()}).
         *
         * <p>The default TTL for {@link TakenAction} document is 60 days.
         *
         * <p>See {@link androidx.appsearch.annotation.Document.TtlMillis} for more information on
         * TTL.
         */
        @CanIgnoreReturnValue
        @NonNull
        public T setDocumentTtlMillis(long documentTtlMillis) {
            mDocumentTtlMillis = documentTtlMillis;
            return (T) this;
        }

        // TODO(b/330777270): improve AnnotationProcessor for abstract document class builder, and
        //                    make it an abstract method.
        /**
         * For AppSearch annotation processor requirement only. The client should never call it
         * since it is impossible to instantiate an abstract class.
         *
         * @throws UnsupportedOperationException
         */
        @NonNull
        public TakenAction build() {
            throw new UnsupportedOperationException();
        }
    }
}
