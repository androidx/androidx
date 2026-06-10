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

package androidx.appsearch.builtintypes;

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS;

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AppSearch document representing a {@link WebPage} entity.
 *
 * <p>See <a href="https://schema.org/WebPage">https://schema.org/WebPage</a> for more context.
 */
@Document(name = WebPage.SCHEMA_NAME)
public class WebPage extends Thing {

    // DO NOT CHANGE since it will alter schema definition
    public static final String SCHEMA_NAME = "builtin:WebPage";

    @Document.DocumentProperty
    private final @Nullable ImageObject mFavicon;

    @ExperimentalAppSearchApi
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS)
    private final @Nullable String mSource;

    /**
     * Constructor for {@link WebPage}.
     *
     * @param builder The builder to construct the {@link WebPage} from.
     */
    @ExperimentalAppSearchApi
    public WebPage(@NonNull BuilderBase<?> builder) {
        super(builder);
        mFavicon = builder.mFavicon;
        mSource = builder.mSource;
    }

    /**
     * Returns a favicon that represents the web page.
     */
    public @Nullable ImageObject getFavicon() {
        return mFavicon;
    }

    /**
     * Returns the source of how the web page was accessed in CamelCase. (e.g. Tab, CustomTab)
     */
    @ExperimentalAppSearchApi
    public @Nullable String getSource() {
        return mSource;
    }

    /** Builder for {@link WebPage}. */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {

        /** Constructs {@link WebPage.Builder} with given {@code namespace} and {@code id} */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /** Constructs {@link WebPage.Builder} from existing values in given {@link WebPage}. */
        public Builder(@NonNull WebPage webpage) {
            super(webpage);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private ImageObject mFavicon;
        @ExperimentalAppSearchApi
        private String mSource;

        /**
         * Constructor for {@link WebPage.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the Document. See {@link Document.Id}.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor for {@link WebPage.BuilderBase} with all the existing values.
         *
         * @param webPage The existing {@link WebPage} to copy values from.
         */
        public BuilderBase(@NonNull WebPage webPage) {
            super(Preconditions.checkNotNull(webPage));
            mFavicon = webPage.getFavicon();
            mSource = webPage.getSource();
        }

        /**
         * Sets the favicon that represents the web page.
         */
        public @NonNull T setFavicon(@Nullable ImageObject favicon) {
            mFavicon = favicon;
            return (T) this;
        }

        /**
         * Sets the source of how the web page was accessed in CamelCase. (e.g. Tab, CustomTab)
         */
        @ExperimentalAppSearchApi
        public @NonNull T setSource(@Nullable String type) {
            mSource = type;
            return (T) this;
        }

        /** Builds the {@link WebPage}. */
        @Override
        public @NonNull WebPage build() {
            return new WebPage(this);
        }
    }
}
