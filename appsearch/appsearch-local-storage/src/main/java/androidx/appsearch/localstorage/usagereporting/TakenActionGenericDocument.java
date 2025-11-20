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

package androidx.appsearch.localstorage.usagereporting;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.usagereporting.ActionConstants;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstract wrapper class for {@link GenericDocument} of all types of taken actions, which contains
 * common getters and constants.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class TakenActionGenericDocument extends GenericDocument {
    protected static final String PROPERTY_PATH_ACTION_TYPE = "actionType";

    /**
     * Static factory method to create a concrete object of {@link TakenActionGenericDocument} child
     * type, according to the given {@link GenericDocument}'s action type.
     *
     * @param document a generic document object.
     *
     * @throws IllegalArgumentException if the integer value of property {@code actionType} is
     *                                  invalid.
     */
    public static @NonNull TakenActionGenericDocument create(@NonNull GenericDocument document) {
        Preconditions.checkNotNull(document);
        int actionType = (int) document.getPropertyLong(PROPERTY_PATH_ACTION_TYPE);
        switch (actionType) {
            case ActionConstants.ACTION_TYPE_SEARCH:
                return new SearchActionGenericDocument.Builder(document).build();
            case ActionConstants.ACTION_TYPE_CLICK:
                return new ClickActionGenericDocument.Builder(document).build();
            default:
                throw new IllegalArgumentException(
                        "Cannot create taken action generic document with unknown action type");
        }
    }

    protected TakenActionGenericDocument(@NonNull GenericDocument document) {
        super(Preconditions.checkNotNull(document));
    }

    /** Returns the (enum) integer value of property {@code actionType}. */
    public int getActionType() {
        return (int) getPropertyLong(PROPERTY_PATH_ACTION_TYPE);
    }

    /** Abstract builder for {@link TakenActionGenericDocument}. */
    abstract static class Builder<T extends Builder<T>> extends GenericDocument.Builder<T> {
        @Nullable private Integer mActionType = null;

        /**
         * Creates a new {@link TakenActionGenericDocument.Builder}.
         *
         * <p>Document IDs are unique within a namespace.
         *
         * <p>The number of namespaces per app should be kept small for efficiency reasons.
         *
         * @param namespace  the namespace to set for the {@link GenericDocument}.
         * @param id         the unique identifier for the {@link GenericDocument} in its namespace.
         * @param schemaType the {@link AppSearchSchema} type of the {@link GenericDocument}. The
         *                   provided {@code schemaType} must be defined using
         *                   {@link AppSearchSession#setSchemaAsync} prior
         *                   to inserting a document of this {@code schemaType} into the
         *                   AppSearch index using
         *                   {@link AppSearchSession#putAsync}.
         *                   Otherwise, the document will be rejected by
         *                   {@link AppSearchSession#putAsync} with result code
         *                   {@link AppSearchResult#RESULT_NOT_FOUND}.
         * @param actionType the action type of the taken action. See definitions in
         *                   {@link ActionConstants}.
         */
        Builder(@NonNull String namespace, @NonNull String id, @NonNull String schemaType,
                int actionType) {
            super(Preconditions.checkNotNull(namespace), Preconditions.checkNotNull(id),
                    Preconditions.checkNotNull(schemaType));

            mActionType = actionType;
        }

        /**
         * Creates a new {@link TakenActionGenericDocument.Builder} from an existing
         * {@link GenericDocument}.
         */
        Builder(@NonNull GenericDocument document) {
            super(Preconditions.checkNotNull(document));
        }

        /** Builds the {@link GenericDocument} and ensures that the action type is set. */
        @Override
        public GenericDocument build() {
            if (mActionType != null) {
                setPropertyLong(PROPERTY_PATH_ACTION_TYPE, mActionType);
            }
          return super.build();
        }

    }
}
