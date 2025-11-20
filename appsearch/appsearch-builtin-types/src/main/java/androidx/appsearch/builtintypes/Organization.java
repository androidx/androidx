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

import java.util.List;

/**
 * AppSearch document representing an {@link Organization} entity.
 */
@Document(name = "builtin:Organization")
public class Organization extends Thing {
    @Document.DocumentProperty
    private @Nullable ImageObject mLogo;

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    protected Organization(
        @NonNull String namespace, @NonNull String id, int documentScore,
        long creationTimestampMillis, long documentTtlMillis,
        @Nullable String name, @Nullable List<String> alternateNames,
        @Nullable String description,
        @Nullable String image, @Nullable String url,
        @NonNull List<PotentialAction> potentialActions,
        @Nullable ImageObject logo) {
        super(namespace, id, documentScore, creationTimestampMillis,
            documentTtlMillis, name, alternateNames, description, image, url,
            potentialActions);
        this.mLogo = logo;
    }

    /**
     * Returns the logo of the organization, if set.
     */
    public @Nullable ImageObject getLogo() {
        return mLogo;
    }

    @Document.BuilderProducer
    public static final class Builder extends BuilderImpl<Builder> {

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
    static class BuilderImpl<T extends BuilderImpl<T>> extends Thing.BuilderImpl<T> {
        protected @Nullable ImageObject mLogo;

        BuilderImpl(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        BuilderImpl(@NonNull Organization organization) {
            super(new Thing.Builder(organization).build());
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
            return new Organization(
                mNamespace,
                mId,
                mDocumentScore,
                mCreationTimestampMillis,
                mDocumentTtlMillis,
                mName,
                mAlternateNames,
                mDescription,
                mImage,
                mUrl,
                mPotentialActions,
                mLogo);
        }
    }
}