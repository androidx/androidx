/*
 * Copyright 2021-2022 The Android Open Source Project
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

package androidx.wear.tiles;

import static androidx.annotation.Dimension.PX;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Builders for the resources for a layout. */
public final class ResourceBuilders {
    private ResourceBuilders() {}

    /**
     * Format describing the contents of an image data byte array.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({IMAGE_FORMAT_UNDEFINED, IMAGE_FORMAT_RGB_565})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageFormat {}

    /** An undefined image format. */
    public static final int IMAGE_FORMAT_UNDEFINED = 0;

    /**
     * An image format where each pixel is stored on 2 bytes, with red using 5 bits, green using 6
     * bits and blue using 5 bits of precision.
     */
    public static final int IMAGE_FORMAT_RGB_565 = 1;

    /** An image resource which maps to an Android drawable by resource ID. */
    public static final class AndroidImageResourceByResId {
        private final ResourceProto.AndroidImageResourceByResId mImpl;

        private AndroidImageResourceByResId(ResourceProto.AndroidImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the Android resource ID of this image. This must refer to a drawable under
         * R.drawable. Intended for testing purposes only.
         */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AndroidImageResourceByResId fromProto(
                @NonNull ResourceProto.AndroidImageResourceByResId proto) {
            return new AndroidImageResourceByResId(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.AndroidImageResourceByResId toProto() {
            return mImpl;
        }

        /** Builder for {@link AndroidImageResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidImageResourceByResId.Builder mImpl =
                    ResourceProto.AndroidImageResourceByResId.newBuilder();

            public Builder() {}

            /**
             * Sets the Android resource ID of this image. This must refer to a drawable under
             * R.drawable.
             */
            @NonNull
            public Builder setResourceId(@DrawableRes int resourceId) {
                mImpl.setResourceId(resourceId);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AndroidImageResourceByResId build() {
                return AndroidImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image resource whose data is fully inlined, with no dependency on a system or app
     * resource.
     */
    public static final class InlineImageResource {
        private final ResourceProto.InlineImageResource mImpl;

        private InlineImageResource(ResourceProto.InlineImageResource impl) {
            this.mImpl = impl;
        }

        /** Gets the byte array representing the image. Intended for testing purposes only. */
        @NonNull
        public byte[] getData() {
            return mImpl.getData().toByteArray();
        }

        /**
         * Gets the native width of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size. Intended for testing
         * purposes only.
         */
        @Dimension(unit = PX)
        public int getWidthPx() {
            return mImpl.getWidthPx();
        }

        /**
         * Gets the native height of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size. Intended for testing
         * purposes only.
         */
        @Dimension(unit = PX)
        public int getHeightPx() {
            return mImpl.getHeightPx();
        }

        /**
         * Gets the format of the byte array data representing the image. May be left unspecified or
         * set to IMAGE_FORMAT_UNDEFINED in which case the platform will attempt to extract this
         * from the raw image data. If the platform does not support the format, the image will not
         * be decoded or displayed. Intended for testing purposes only.
         */
        @ImageFormat
        public int getFormat() {
            return mImpl.getFormat().getNumber();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static InlineImageResource fromProto(
                @NonNull ResourceProto.InlineImageResource proto) {
            return new InlineImageResource(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.InlineImageResource toProto() {
            return mImpl;
        }

        /** Builder for {@link InlineImageResource} */
        public static final class Builder {
            private final ResourceProto.InlineImageResource.Builder mImpl =
                    ResourceProto.InlineImageResource.newBuilder();

            public Builder() {}

            /** Sets the byte array representing the image. */
            @NonNull
            public Builder setData(@NonNull byte[] data) {
                mImpl.setData(ByteString.copyFrom(data));
                return this;
            }

            /**
             * Sets the native width of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             */
            @NonNull
            public Builder setWidthPx(@Dimension(unit = PX) int widthPx) {
                mImpl.setWidthPx(widthPx);
                return this;
            }

            /**
             * Sets the native height of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             */
            @NonNull
            public Builder setHeightPx(@Dimension(unit = PX) int heightPx) {
                mImpl.setHeightPx(heightPx);
                return this;
            }

            /**
             * Sets the format of the byte array data representing the image. May be left
             * unspecified or set to IMAGE_FORMAT_UNDEFINED in which case the platform will attempt
             * to extract this from the raw image data. If the platform does not support the format,
             * the image will not be decoded or displayed.
             */
            @NonNull
            public Builder setFormat(@ImageFormat int format) {
                mImpl.setFormat(ResourceProto.ImageFormat.forNumber(format));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public InlineImageResource build() {
                return InlineImageResource.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image resource, which can be used by layouts. This holds multiple underlying resource
     * types, which the underlying runtime will pick according to what it thinks is appropriate.
     */
    public static final class ImageResource {
        private final ResourceProto.ImageResource mImpl;

        private ImageResource(ResourceProto.ImageResource impl) {
            this.mImpl = impl;
        }

        /**
         * Gets an image resource that maps to an Android drawable by resource ID. Intended for
         * testing purposes only.
         */
        @Nullable
        public AndroidImageResourceByResId getAndroidResourceByResId() {
            if (mImpl.hasAndroidResourceByResId()) {
                return AndroidImageResourceByResId.fromProto(mImpl.getAndroidResourceByResId());
            } else {
                return null;
            }
        }

        /**
         * Gets an image resource that contains the image data inline. Intended for testing purposes
         * only.
         */
        @Nullable
        public InlineImageResource getInlineResource() {
            if (mImpl.hasInlineResource()) {
                return InlineImageResource.fromProto(mImpl.getInlineResource());
            } else {
                return null;
            }
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ImageResource fromProto(@NonNull ResourceProto.ImageResource proto) {
            return new ImageResource(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.ImageResource toProto() {
            return mImpl;
        }

        /** Builder for {@link ImageResource} */
        public static final class Builder {
            private final ResourceProto.ImageResource.Builder mImpl =
                    ResourceProto.ImageResource.newBuilder();

            public Builder() {}

            /** Sets an image resource that maps to an Android drawable by resource ID. */
            @NonNull
            public Builder setAndroidResourceByResId(
                    @NonNull AndroidImageResourceByResId androidResourceByResId) {
                mImpl.setAndroidResourceByResId(androidResourceByResId.toProto());
                return this;
            }

            /** Sets an image resource that contains the image data inline. */
            @NonNull
            public Builder setInlineResource(@NonNull InlineImageResource inlineResource) {
                mImpl.setInlineResource(inlineResource.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ImageResource build() {
                return ImageResource.fromProto(mImpl.build());
            }
        }
    }

    /** The resources for a layout. */
    public static final class Resources {
        private final ResourceProto.Resources mImpl;

        private Resources(ResourceProto.Resources impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the version of this {@link Resources} instance.
         *
         * <p>Each tile specifies the version of resources it requires. After fetching a tile, the
         * renderer will use the resources version specified by the tile to separately fetch the
         * resources.
         *
         * <p>This value must match the version of the resources required by the tile for the tile
         * to render successfully, and must match the resource version specified in {@link
         * androidx.wear.tiles.RequestBuilders.ResourcesRequest} which triggered this request.
         * Intended for testing purposes only.
         */
        @NonNull
        public String getVersion() {
            return mImpl.getVersion();
        }

        /**
         * Gets a map of resource_ids to images, which can be used by layouts. Intended for testing
         * purposes only.
         */
        @NonNull
        public Map<String, ImageResource> getIdToImageMapping() {
            Map<String, ImageResource> map = new HashMap<>();
            for (Entry<String, ResourceProto.ImageResource> entry :
                    mImpl.getIdToImageMap().entrySet()) {
                map.put(entry.getKey(), ImageResource.fromProto(entry.getValue()));
            }
            return Collections.unmodifiableMap(map);
        }

        /** Converts to byte array representation. */
        @NonNull
        @TilesExperimental
        public byte[] toByteArray() {
            return mImpl.toByteArray();
        }

        /** Converts from byte array representation. */
        @SuppressWarnings("ProtoParseWithRegistry")
        @Nullable
        @TilesExperimental
        public static Resources fromByteArray(@NonNull byte[] byteArray) {
            try {
                return fromProto(ResourceProto.Resources.parseFrom(byteArray));
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Resources fromProto(@NonNull ResourceProto.Resources proto) {
            return new Resources(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.Resources toProto() {
            return mImpl;
        }

        /** Builder for {@link Resources} */
        public static final class Builder {
            private final ResourceProto.Resources.Builder mImpl =
                    ResourceProto.Resources.newBuilder();

            public Builder() {}

            /**
             * Sets the version of this {@link Resources} instance.
             *
             * <p>Each tile specifies the version of resources it requires. After fetching a tile,
             * the renderer will use the resources version specified by the tile to separately fetch
             * the resources.
             *
             * <p>This value must match the version of the resources required by the tile for the
             * tile to render successfully, and must match the resource version specified in {@link
             * androidx.wear.tiles.RequestBuilders.ResourcesRequest} which triggered this request.
             */
            @NonNull
            public Builder setVersion(@NonNull String version) {
                mImpl.setVersion(version);
                return this;
            }

            /** Adds an entry into a map of resource_ids to images, which can be used by layouts. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder addIdToImageMapping(@NonNull String id, @NonNull ImageResource image) {
                mImpl.putIdToImage(id, image.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Resources build() {
                return Resources.fromProto(mImpl.build());
            }
        }
    }
}
