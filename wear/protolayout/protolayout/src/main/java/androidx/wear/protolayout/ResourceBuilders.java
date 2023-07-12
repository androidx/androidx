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

package androidx.wear.protolayout;

import static androidx.annotation.Dimension.PX;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.TriggerBuilders.Trigger;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.protobuf.ByteString;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
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
     * @since 1.0
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({IMAGE_FORMAT_UNDEFINED, IMAGE_FORMAT_RGB_565})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageFormat {}

    /**
     * An undefined image format.
     *
     * @since 1.0
     */
    public static final int IMAGE_FORMAT_UNDEFINED = 0;

    /**
     * An image format where each pixel is stored on 2 bytes, with red using 5 bits, green using 6
     * bits and blue using 5 bits of precision.
     *
     * @since 1.0
     */
    public static final int IMAGE_FORMAT_RGB_565 = 1;

    /**
     * Format describing the contents of an animated image.
     *
     * @since 1.2
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ANIMATED_IMAGE_FORMAT_UNDEFINED, ANIMATED_IMAGE_FORMAT_AVD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimatedImageFormat {}

    /**
     * An undefined image format.
     *
     * @since 1.2
     */
    public static final int ANIMATED_IMAGE_FORMAT_UNDEFINED = 0;

    /**
     * Android AnimatedVectorDrawable.
     *
     * @since 1.2
     */
    public static final int ANIMATED_IMAGE_FORMAT_AVD = 1;

    /**
     * An image resource which maps to an Android drawable by resource ID.
     *
     * @since 1.0
     */
    public static final class AndroidImageResourceByResId {
        private final ResourceProto.AndroidImageResourceByResId mImpl;

        AndroidImageResourceByResId(ResourceProto.AndroidImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the Android resource ID of this image. This must refer to a drawable under
         * R.drawable.
         *
         * @since 1.0
         */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }
        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AndroidImageResourceByResId fromProto(
                @NonNull ResourceProto.AndroidImageResourceByResId proto) {
            return new AndroidImageResourceByResId(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.AndroidImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "AndroidImageResourceByResId{" + "resourceId=" + getResourceId() + "}";
        }

        /** Builder for {@link AndroidImageResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidImageResourceByResId.Builder mImpl =
                    ResourceProto.AndroidImageResourceByResId.newBuilder();

            public Builder() {}

            /**
             * Sets the Android resource ID of this image. This must refer to a drawable under
             * R.drawable.
             *
             * @since 1.0
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
     *
     * @since 1.0
     */
    public static final class InlineImageResource {
        private final ResourceProto.InlineImageResource mImpl;

        InlineImageResource(ResourceProto.InlineImageResource impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the byte array representing the image.
         *
         * @since 1.0
         */
        @NonNull
        public byte[] getData() {
            return mImpl.getData().toByteArray();
        }

        /**
         * Gets the native width of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size.
         *
         * @since 1.0
         */
        @Dimension(unit = PX)
        public int getWidthPx() {
            return mImpl.getWidthPx();
        }

        /**
         * Gets the native height of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size.
         *
         * @since 1.0
         */
        @Dimension(unit = PX)
        public int getHeightPx() {
            return mImpl.getHeightPx();
        }

        /**
         * Gets the format of the byte array data representing the image. May be left unspecified or
         * set to IMAGE_FORMAT_UNDEFINED in which case the platform will attempt to extract this
         * from the raw image data. If the platform does not support the format, the image will not
         * be decoded or displayed.
         *
         * @since 1.0
         */
        @ImageFormat
        public int getFormat() {
            return mImpl.getFormat().getNumber();
        }
        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static InlineImageResource fromProto(
                @NonNull ResourceProto.InlineImageResource proto) {
            return new InlineImageResource(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.InlineImageResource toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "InlineImageResource{"
                    + "data="
                    + Arrays.toString(getData())
                    + ", widthPx="
                    + getWidthPx()
                    + ", heightPx="
                    + getHeightPx()
                    + ", format="
                    + getFormat()
                    + "}";
        }

        /** Builder for {@link InlineImageResource} */
        public static final class Builder {
            private final ResourceProto.InlineImageResource.Builder mImpl =
                    ResourceProto.InlineImageResource.newBuilder();

            public Builder() {}

            /**
             * Sets the byte array representing the image.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setData(@NonNull byte[] data) {
                mImpl.setData(ByteString.copyFrom(data));
                return this;
            }

            /**
             * Sets the native width of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setWidthPx(@Dimension(unit = PX) int widthPx) {
                mImpl.setWidthPx(widthPx);
                return this;
            }

            /**
             * Sets the native height of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             *
             * @since 1.0
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
             *
             * @since 1.0
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
     * A non-seekable animated image resource that maps to an Android drawable by resource ID. The
     * animation is started with given trigger, fire and forget.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class AndroidAnimatedImageResourceByResId {
        private final ResourceProto.AndroidAnimatedImageResourceByResId mImpl;

        AndroidAnimatedImageResourceByResId(
                ResourceProto.AndroidAnimatedImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the format for the animated image.
         *
         * @since 1.2
         */
        @AnimatedImageFormat
        public int getAnimatedImageFormat() {
            return mImpl.getAnimatedImageFormat().getNumber();
        }

        /**
         * Gets the Android resource ID, e.g. R.drawable.foo.
         *
         * @since 1.2
         */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }

        /**
         * Gets the trigger to start the animation.
         *
         * @since 1.2
         */
        @Nullable
        public Trigger getStartTrigger() {
            if (mImpl.hasStartTrigger()) {
                return TriggerBuilders.triggerFromProto(mImpl.getStartTrigger());
            } else {
                return null;
            }
        }
        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AndroidAnimatedImageResourceByResId fromProto(
                @NonNull ResourceProto.AndroidAnimatedImageResourceByResId proto) {
            return new AndroidAnimatedImageResourceByResId(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.AndroidAnimatedImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "AndroidAnimatedImageResourceByResId{"
                    + "animatedImageFormat="
                    + getAnimatedImageFormat()
                    + ", resourceId="
                    + getResourceId()
                    + ", startTrigger="
                    + getStartTrigger()
                    + "}";
        }

        /** Builder for {@link AndroidAnimatedImageResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidAnimatedImageResourceByResId.Builder mImpl =
                    ResourceProto.AndroidAnimatedImageResourceByResId.newBuilder();

            public Builder() {}

            /**
             * Sets the format for the animated image.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimatedImageFormat(@AnimatedImageFormat int animatedImageFormat) {
                mImpl.setAnimatedImageFormat(
                        ResourceProto.AnimatedImageFormat.forNumber(animatedImageFormat));
                return this;
            }

            /**
             * Sets the Android resource ID, e.g. R.drawable.foo.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setResourceId(@DrawableRes int resourceId) {
                mImpl.setResourceId(resourceId);
                return this;
            }

            /**
             * Sets the trigger to start the animation.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setStartTrigger(@NonNull Trigger startTrigger) {
                mImpl.setStartTrigger(startTrigger.toTriggerProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AndroidAnimatedImageResourceByResId build() {
                return AndroidAnimatedImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A seekable animated image resource that maps to an Android drawable by resource ID. The
     * animation progress is bound to the provided dynamic float.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class AndroidSeekableAnimatedImageResourceByResId {
        private final ResourceProto.AndroidSeekableAnimatedImageResourceByResId mImpl;

        AndroidSeekableAnimatedImageResourceByResId(
                ResourceProto.AndroidSeekableAnimatedImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the format for the animated image.
         *
         * @since 1.2
         */
        @AnimatedImageFormat
        public int getAnimatedImageFormat() {
            return mImpl.getAnimatedImageFormat().getNumber();
        }

        /**
         * Gets the Android resource ID, e.g. R.drawable.foo.
         *
         * @since 1.2
         */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }

        /**
         * Gets a {@link androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat},
         * normally transformed from certain states with the data binding pipeline to control the
         * progress of the animation. Its value is required to fall in the range of [0.0, 1.0]. Any
         * values outside this range would be clamped. When the first value of the {@link
         * androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat} arrives, the animation
         * starts from progress 0 to that value. After that it plays from current progress to the
         * new value on subsequent updates. If not set, the animation will play on load (similar to
         * a non-seekable animated).
         *
         * @since 1.2
         */
        @Nullable
        public DynamicFloat getProgress() {
            if (mImpl.hasProgress()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getProgress());
            } else {
                return null;
            }
        }
        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AndroidSeekableAnimatedImageResourceByResId fromProto(
                @NonNull ResourceProto.AndroidSeekableAnimatedImageResourceByResId proto) {
            return new AndroidSeekableAnimatedImageResourceByResId(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.AndroidSeekableAnimatedImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "AndroidSeekableAnimatedImageResourceByResId{"
                    + "animatedImageFormat="
                    + getAnimatedImageFormat()
                    + ", resourceId="
                    + getResourceId()
                    + ", progress="
                    + getProgress()
                    + "}";
        }

        /** Builder for {@link AndroidSeekableAnimatedImageResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidSeekableAnimatedImageResourceByResId.Builder mImpl =
                    ResourceProto.AndroidSeekableAnimatedImageResourceByResId.newBuilder();

            public Builder() {}

            /**
             * Sets the format for the animated image.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimatedImageFormat(@AnimatedImageFormat int animatedImageFormat) {
                mImpl.setAnimatedImageFormat(
                        ResourceProto.AnimatedImageFormat.forNumber(animatedImageFormat));
                return this;
            }

            /**
             * Sets the Android resource ID, e.g. R.drawable.foo.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setResourceId(@DrawableRes int resourceId) {
                mImpl.setResourceId(resourceId);
                return this;
            }

            /**
             * Sets a {@link androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat},
             * normally transformed from certain states with the data binding pipeline to control
             * the progress of the animation. Its value is required to fall in the range of [0.0,
             * 1.0]. Any values outside this range would be clamped. When the first value of the
             * {@link androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat} arrives,
             * the animation starts from progress 0 to that value. After that it plays from current
             * progress to the new value on subsequent updates. If not set, the animation will play
             * on load (similar to a non-seekable animated).
             *
             * @since 1.2
             */
            @NonNull
            public Builder setProgress(@NonNull DynamicFloat progress) {
                mImpl.setProgress(progress.toDynamicFloatProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AndroidSeekableAnimatedImageResourceByResId build() {
                return AndroidSeekableAnimatedImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image resource, which can be used by layouts. This holds multiple underlying resource
     * types, which the underlying runtime will pick according to what it thinks is appropriate.
     *
     * @since 1.0
     */
    public static final class ImageResource {
        private final ResourceProto.ImageResource mImpl;

        ImageResource(ResourceProto.ImageResource impl) {
            this.mImpl = impl;
        }

        /**
         * Gets an image resource that maps to an Android drawable by resource ID.
         *
         * @since 1.0
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
         * Gets an image resource that contains the image data inline.
         *
         * @since 1.0
         */
        @Nullable
        public InlineImageResource getInlineResource() {
            if (mImpl.hasInlineResource()) {
                return InlineImageResource.fromProto(mImpl.getInlineResource());
            } else {
                return null;
            }
        }

        /**
         * Gets a non-seekable animated image resource that maps to an Android drawable by resource
         * ID. The animation is started with given trigger, fire and forget.
         *
         * @since 1.2
         */
        @Nullable
        @ProtoLayoutExperimental
        public AndroidAnimatedImageResourceByResId getAndroidAnimatedResourceByResId() {
            if (mImpl.hasAndroidAnimatedResourceByResId()) {
                return AndroidAnimatedImageResourceByResId.fromProto(
                        mImpl.getAndroidAnimatedResourceByResId());
            } else {
                return null;
            }
        }

        /**
         * Gets a seekable animated image resource that maps to an Android drawable by resource ID.
         * The animation progress is bound to the provided dynamic float.
         *
         * @since 1.2
         */
        @Nullable
        @ProtoLayoutExperimental
        public AndroidSeekableAnimatedImageResourceByResId
                getAndroidSeekableAnimatedResourceByResId() {
            if (mImpl.hasAndroidSeekableAnimatedResourceByResId()) {
                return AndroidSeekableAnimatedImageResourceByResId.fromProto(
                        mImpl.getAndroidSeekableAnimatedResourceByResId());
            } else {
                return null;
            }
        }
        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ImageResource fromProto(@NonNull ResourceProto.ImageResource proto) {
            return new ImageResource(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.ImageResource toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public String toString() {
            return "ImageResource{"
                    + "androidResourceByResId="
                    + getAndroidResourceByResId()
                    + ", inlineResource="
                    + getInlineResource()
                    + ", androidAnimatedResourceByResId="
                    + getAndroidAnimatedResourceByResId()
                    + ", androidSeekableAnimatedResourceByResId="
                    + getAndroidSeekableAnimatedResourceByResId()
                    + "}";
        }

        /** Builder for {@link ImageResource} */
        public static final class Builder {
            private final ResourceProto.ImageResource.Builder mImpl =
                    ResourceProto.ImageResource.newBuilder();

            public Builder() {}

            /**
             * Sets an image resource that maps to an Android drawable by resource ID.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setAndroidResourceByResId(
                    @NonNull AndroidImageResourceByResId androidResourceByResId) {
                mImpl.setAndroidResourceByResId(androidResourceByResId.toProto());
                return this;
            }

            /**
             * Sets an image resource that contains the image data inline.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setInlineResource(@NonNull InlineImageResource inlineResource) {
                mImpl.setInlineResource(inlineResource.toProto());
                return this;
            }

            /**
             * Sets a non-seekable animated image resource that maps to an Android drawable by
             * resource ID. The animation is started with given trigger, fire and forget.
             *
             * @since 1.2
             */
            @NonNull
            @ProtoLayoutExperimental
            public Builder setAndroidAnimatedResourceByResId(
                    @NonNull AndroidAnimatedImageResourceByResId androidAnimatedResourceByResId) {
                mImpl.setAndroidAnimatedResourceByResId(androidAnimatedResourceByResId.toProto());
                return this;
            }

            /**
             * Sets a seekable animated image resource that maps to an Android drawable by resource
             * ID. The animation progress is bound to the provided dynamic float.
             *
             * @since 1.2
             */
            @NonNull
            @ProtoLayoutExperimental
            public Builder setAndroidSeekableAnimatedResourceByResId(
                    @NonNull
                            AndroidSeekableAnimatedImageResourceByResId
                                    androidSeekableAnimatedResourceByResId) {
                mImpl.setAndroidSeekableAnimatedResourceByResId(
                        androidSeekableAnimatedResourceByResId.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ImageResource build() {
                return ImageResource.fromProto(mImpl.build());
            }
        }
    }

    /**
     * The resources for a layout.
     *
     * @since 1.0
     */
    public static final class Resources {
        private final ResourceProto.Resources mImpl;

        Resources(ResourceProto.Resources impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the version of this {@link Resources} instance.
         *
         * <p>Each layout specifies the version of resources it requires. After fetching a layout,
         * the renderer will use the resources version specified by the layout to separately fetch
         * the resources.
         *
         * <p>This value must match the version of the resources required by the layout for the
         * layout to render successfully, and must match the resource version specified in
         * ResourcesRequest which triggered this request.
         *
         * @since 1.0
         */
        @NonNull
        public String getVersion() {
            return mImpl.getVersion();
        }

        /**
         * Gets a map of resource_ids to images, which can be used by layouts.
         *
         * @since 1.0
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

        /**
         * Creates a new wrapper instance from the proto.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Resources fromProto(@NonNull ResourceProto.Resources proto) {
            return new Resources(proto);
        }

        /**
         * Returns the internal proto instance.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ResourceProto.Resources toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "Resources{"
                    + "version="
                    + getVersion()
                    + ", idToImageMapping="
                    + getIdToImageMapping()
                    + "}";
        }

        /** Builder for {@link Resources} */
        public static final class Builder {
            private final ResourceProto.Resources.Builder mImpl =
                    ResourceProto.Resources.newBuilder();

            public Builder() {}

            /**
             * Sets the version of this {@link Resources} instance.
             *
             * <p>Each layout specifies the version of resources it requires. After fetching a
             * layout, the renderer will use the resources version specified by the layout to
             * separately fetch the resources.
             *
             * <p>This value must match the version of the resources required by the layout for the
             * layout to render successfully, and must match the resource version specified in
             * ResourcesRequest which triggered this request.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setVersion(@NonNull String version) {
                mImpl.setVersion(version);
                return this;
            }

            /**
             * Adds an entry into a map of resource_ids to images, which can be used by layouts.
             *
             * @since 1.0
             */
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
