/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.core.util.Preconditions.checkNotNull;

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.builtintypes.properties.Keyword;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents an image file.
 *
 * <p>See <a href="http://schema.org/ImageObject">http://schema.org/ImageObject</a> for more
 * context.
 */
@Document(name = "builtin:ImageObject")
public class ImageObject extends Thing {

    @Document.DocumentProperty(name = "keywords", indexNestedProperties = true)
    private final @NonNull List<Keyword> mKeywords;

    @Document.StringProperty
    private final @Nullable String mSha256;

    @Document.StringProperty
    private final @Nullable String mThumbnailSha256;

    @Document.BytesProperty
    private final byte @Nullable [] mBytes;

    /**
     * Constructor for {@link ImageObject}.
     *
     * @param builder The builder to construct the {@link ImageObject} from.
     */
    @ExperimentalAppSearchApi
    public ImageObject(@NonNull BuilderBase<?> builder) {
        super(builder);
        mKeywords = new ArrayList<>(builder.mKeywords);
        mSha256 = builder.mSha256;
        mThumbnailSha256 = builder.mThumbnailSha256;
        mBytes = builder.mBytes;
    }

    /**
     * Keywords or tags used to describe some item.
     *
     * <p>See <a href="http://schema.org/keywords">http://schema.org/keywords</a> for more context.
     */
    public @NonNull List<Keyword> getKeywords() {
        return mKeywords;
    }

    /**
     * The SHA-2 SHA256 hash of the content of the item.
     * For example, a zero-length input has value
     * 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'.
     *
     * <p>See <a href="http://schema.org/sha256">http://schema.org/sha256</a> for more context.
     */
    public @Nullable String getSha256() {
        return mSha256;
    }

    /**
     * Returns the {@code sha256} for the thumbnail of this image or video.
     */
    public @Nullable String getThumbnailSha256() {
        return mThumbnailSha256;
    }

    /**
     * Returns the byte representation of this image or video.
     * Can be a compressed bitmap (e.g. JPEG or PNG).
     */
    public byte @Nullable [] getBytes() {
        return mBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageObject)) return false;
        ImageObject that = (ImageObject) o;
        return mKeywords.equals(that.mKeywords) && Objects.equals(mSha256, that.mSha256)
                && Objects.equals(mThumbnailSha256, that.mThumbnailSha256)
                && Arrays.equals(mBytes, that.mBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKeywords, mSha256, mThumbnailSha256, Arrays.hashCode(mBytes));
    }

    /**
     * Builder for {@link ImageObject}.
     */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
        /**
         * Constructor for an empty {@link Builder}.
         *
         * @param namespace Namespace for the Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Copy constructor.
         */
        public Builder(@NonNull ImageObject copyFrom) {
            super(copyFrom);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private final @NonNull List<Keyword> mKeywords;

        private @Nullable String mSha256;

        private @Nullable String mThumbnailSha256;

        private byte @Nullable [] mBytes;

        /**
         * Constructor for {@link ImageObject.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the Document. See {@link Document.Id}.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
            mKeywords = new ArrayList<>();
            mSha256 = null;
            mThumbnailSha256 = null;
            mBytes = null;
        }

        /**
         * Constructor for {@link ImageObject.BuilderBase} with all the existing values.
         *
         * @param copyFrom The existing {@link ImageObject} to copy values from.
         */
        public BuilderBase(@NonNull ImageObject copyFrom) {
            super(checkNotNull(copyFrom));
            mKeywords = new ArrayList<>(copyFrom.getKeywords());
            mSha256 = copyFrom.getSha256();
            mThumbnailSha256 = copyFrom.getThumbnailSha256();
            mBytes = copyFrom.getBytes();
        }

        @Override
        public @NonNull ImageObject build() {
            return new ImageObject(this);
        }

        /**
         * Appends the {@link Keyword} as a Text i.e. {@link String}.
         */
        // Atypical overloads in the Builder to model union types.
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public @NonNull T addKeyword(@NonNull String text) {
            mKeywords.add(new Keyword(checkNotNull(text)));
            return (T) this;
        }

        /**
         * Appends the {@link Keyword}.
         */
        public @NonNull T addKeyword(@NonNull Keyword keyword) {
            mKeywords.add(checkNotNull(keyword));
            return (T) this;
        }

        /**
         * Appends all the {@code values}.
         */
        public @NonNull T addKeywords(@NonNull Iterable<Keyword> values) {
            for (Keyword value : checkNotNull(values)) {
                mKeywords.add(checkNotNull(value));
            }
            return (T) this;
        }

        /**
         * Sets the {@code sha256}.
         */
        public @NonNull T setSha256(@Nullable String text) {
            mSha256 = text;
            return (T) this;
        }

        /**
         * Sets the {@code sha256} of the thumbnail of this image of video.
         */
        public @NonNull T setThumbnailSha256(@Nullable String text) {
            mThumbnailSha256 = text;
            return (T) this;
        }

        /**
         * Sets the byte representation of this image or video.
         */
        public @NonNull T setBytes(byte @Nullable [] bytes) {
            mBytes = bytes;
            return (T) this;
        }
    }
}
