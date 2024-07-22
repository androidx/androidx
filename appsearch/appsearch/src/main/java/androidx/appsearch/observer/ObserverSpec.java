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

package androidx.appsearch.observer;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassFactoryRegistry;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.ObserverSpecCreator;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Configures the types, namespaces and other properties that {@link ObserverCallback} instances
 * match against.
 */
@SafeParcelable.Class(creator = "ObserverSpecCreator")
@SuppressWarnings("HiddenSuperclass")
public final class ObserverSpec extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull
    public static final Parcelable.Creator<ObserverSpec> CREATOR =
            new ObserverSpecCreator();

    @Field(id = 1)
    final List<String> mFilterSchemas;

    /** Populated on first use */
    @Nullable private volatile Set<String> mFilterSchemasCached;

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Constructor
    public ObserverSpec(
            @Param(id = 1) @NonNull List<String> filterSchemas) {
        mFilterSchemas = Preconditions.checkNotNull(filterSchemas);
    }

    /**
     * Returns the list of schema types which observers using this spec will trigger on.
     *
     * <p>If empty, the observers will trigger on all schema types.
     */
    @NonNull
    public Set<String> getFilterSchemas() {
        if (mFilterSchemasCached == null) {
            if (mFilterSchemas == null) {
                mFilterSchemasCached = Collections.emptySet();
            } else {
                mFilterSchemasCached = Collections.unmodifiableSet(new ArraySet<>(mFilterSchemas));
            }
        }
        return mFilterSchemasCached;
    }

    /** Builder for {@link ObserverSpec} instances. */
    public static final class Builder {
        private ArrayList<String> mFilterSchemas = new ArrayList<>();
        private boolean mBuilt = false;

        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided schema types.
         *
         * <p>If unset, the observer will match documents of all types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull String... schemas) {
            Preconditions.checkNotNull(schemas);
            resetIfBuilt();
            return addFilterSchemas(Arrays.asList(schemas));
        }

        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided schema types.
         *
         * <p>If unset, the observer will match documents of all types.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterSchemas(@NonNull Collection<String> schemas) {
            Preconditions.checkNotNull(schemas);
            resetIfBuilt();
            mFilterSchemas.addAll(schemas);
            return this;
        }

// @exportToFramework:startStrip()
        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided document classes.
         *
         * <p>If unset, the observer will match documents of all types.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentClasses(@NonNull java.lang.Class<?>... documentClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            return addFilterDocumentClasses(Arrays.asList(documentClasses));
        }

        /**
         * Restricts an observer using this spec to triggering only for documents of one of the
         * provided document classes.
         *
         * <p>If unset, the observer will match documents of all types.
         *
         * <p>Merged list available from {@link #getFilterSchemas()}.
         *
         * @param documentClasses classes annotated with {@link Document}.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        @CanIgnoreReturnValue
        @NonNull
        public Builder addFilterDocumentClasses(
                @NonNull Collection<? extends java.lang.Class<?>> documentClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(documentClasses);
            resetIfBuilt();
            List<String> schemas = new ArrayList<>(documentClasses.size());
            DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
            for (java.lang.Class<?> documentClass : documentClasses) {
                DocumentClassFactory<?> factory = registry.getOrCreateFactory(documentClass);
                schemas.add(factory.getSchemaName());
            }
            addFilterSchemas(schemas);
            return this;
        }
// @exportToFramework:endStrip()

        /** Constructs a new {@link ObserverSpec} from the contents of this builder. */
        @NonNull
        public ObserverSpec build() {
            mBuilt = true;
            return new ObserverSpec(mFilterSchemas);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mFilterSchemas = new ArrayList<>(mFilterSchemas);
                mBuilt = false;
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ObserverSpecCreator.writeToParcel(this, dest, flags);
    }
}
