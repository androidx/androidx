/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AppSearch document representing an {@link Organization} entity.
 */
@Document(name = "builtin:Organization")
public class Organization extends Thing {
    @Document.DocumentProperty
    private @Nullable ImageObject mLogo;

    /**
     * Constructor for {@link Organization}.
     *
     * @param builder The builder to construct the {@link Organization} from.
     */
    @ExperimentalAppSearchApi
    public Organization(@NonNull BuilderBase<?> builder) {
        super(builder);
        mLogo = builder.mLogo;
    }

    /**
     * Returns the logo of the organization, if set.
     */
    public @Nullable ImageObject getLogo() {
        return mLogo;
    }

    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {

        /**
         * Constructor for {@link Organization.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         */
        public Builder(@NonNull String namespace,  @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull Organization organization) {
            super(organization);
        }
    }

    /**
     * Builder for {@link Organization}.
     */
    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private @Nullable ImageObject mLogo;

        /**
         * Constructor for {@link Organization.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id The unique identifier for the Document.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor for {@link Organization.BuilderBase} with all the existing values.
         *
         * @param organization The existing {@link Organization} to copy values from.
         */
        public BuilderBase(@NonNull Organization organization) {
            super(organization);
            mLogo = organization.getLogo();
        }

        /**
         * Sets the logo of the organization.
         */
        public @NonNull T setLogo(@Nullable ImageObject logo) {
            mLogo = logo;
            return (T) this;
        }

        @Override
        public @NonNull Organization build() {
            return new Organization(this);
        }
    }
}