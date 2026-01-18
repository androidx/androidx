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
import static androidx.wear.protolayout.ResourceBuilders.AndroidLottieResourceByResId.Builder.LOTTIE_PROPERTIES_LIMIT;
import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RawRes;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.TriggerBuilders.Trigger;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.expression.pipeline.DynamicProtoHashEquals;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.protobuf.ByteString;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/** Builders for the resources for a layout. */
public final class ResourceBuilders {
    private ResourceBuilders() {}

    /** Format describing the contents of an image data byte array. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({IMAGE_FORMAT_UNDEFINED, IMAGE_FORMAT_RGB_565, IMAGE_FORMAT_ARGB_8888})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageFormat {}

    /** An undefined image format. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int IMAGE_FORMAT_UNDEFINED = 0;

    /**
     * An image format where each pixel is stored on 2 bytes, with red using 5 bits, green using 6
     * bits and blue using 5 bits of precision.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final int IMAGE_FORMAT_RGB_565 = 1;

    /**
     * An image format where each pixel is stored on 4 bytes. RGB and alpha (for translucency) is
     * stored with 8 bits of precision.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int IMAGE_FORMAT_ARGB_8888 = 2;

    /** Format describing the contents of an animated image. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ANIMATED_IMAGE_FORMAT_UNDEFINED, ANIMATED_IMAGE_FORMAT_AVD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimatedImageFormat {}

    /** An undefined image format. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANIMATED_IMAGE_FORMAT_UNDEFINED = 0;

    /** Android AnimatedVectorDrawable. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int ANIMATED_IMAGE_FORMAT_AVD = 1;

    /** An image resource which maps to an Android drawable by resource ID. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class AndroidImageResourceByResId {
        private final ResourceProto.AndroidImageResourceByResId mImpl;

        AndroidImageResourceByResId(ResourceProto.AndroidImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the Android resource ID of this image. This must refer to a drawable under
         * R.drawable.
         */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }

        @Override
        public int hashCode() {
            return getResourceId();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidImageResourceByResId)) {
                return false;
            }
            AndroidImageResourceByResId that = (AndroidImageResourceByResId) obj;
            return that.getResourceId() == getResourceId();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidImageResourceByResId fromProto(
                ResourceProto.@NonNull AndroidImageResourceByResId proto) {
            return new AndroidImageResourceByResId(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull AndroidImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "AndroidImageResourceByResId{" + "resourceId=" + getResourceId() + "}";
        }

        /** Builder for {@link AndroidImageResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidImageResourceByResId.Builder mImpl =
                    ResourceProto.AndroidImageResourceByResId.newBuilder();

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the Android resource ID of this image. This must refer to a drawable under
             * R.drawable.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setResourceId(@DrawableRes int resourceId) {
                mImpl.setResourceId(resourceId);
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AndroidImageResourceByResId build() {
                return AndroidImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image resource whose data is fully inlined, with no dependency on a system or app
     * resource.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class InlineImageResource {
        private final ResourceProto.InlineImageResource mImpl;

        InlineImageResource(ResourceProto.InlineImageResource impl) {
            this.mImpl = impl;
        }

        /** Gets the byte array representing the image. */
        public byte @NonNull [] getData() {
            return mImpl.getData().toByteArray();
        }

        /**
         * Gets the native width of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size.
         */
        @Dimension(unit = PX)
        public int getWidthPx() {
            return mImpl.getWidthPx();
        }

        /**
         * Gets the native height of the image, in pixels. Only required for formats (e.g.
         * IMAGE_FORMAT_RGB_565) where the image data does not include size.
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
         */
        @ImageFormat
        public int getFormat() {
            return mImpl.getFormat().getNumber();
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    getFormat(), getWidthPx(), getHeightPx(), Arrays.hashCode(getData()));
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof InlineImageResource)) {
                return false;
            }
            InlineImageResource that = (InlineImageResource) obj;
            return that.getFormat() == getFormat()
                    && that.getWidthPx() == getWidthPx()
                    && that.getHeightPx() == getHeightPx()
                    && Arrays.equals(that.getData(), getData());
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull InlineImageResource fromProto(
                ResourceProto.@NonNull InlineImageResource proto) {
            return new InlineImageResource(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull InlineImageResource toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the byte array representing the image. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setData(byte @NonNull [] data) {
                mImpl.setData(ByteString.copyFrom(data));
                return this;
            }

            /**
             * Sets the native width of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidthPx(@Dimension(unit = PX) int widthPx) {
                mImpl.setWidthPx(widthPx);
                return this;
            }

            /**
             * Sets the native height of the image, in pixels. Only required for formats (e.g.
             * IMAGE_FORMAT_RGB_565) where the image data does not include size.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setHeightPx(@Dimension(unit = PX) int heightPx) {
                mImpl.setHeightPx(heightPx);
                return this;
            }

            /**
             * Sets the format of the byte array data representing the image. May be left
             * unspecified or set to IMAGE_FORMAT_UNDEFINED in which case the platform will attempt
             * to extract this from the raw image data. If the platform does not support the format,
             * the image will not be decoded or displayed.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setFormat(@ImageFormat int format) {
                mImpl.setFormat(ResourceProto.ImageFormat.forNumber(format));
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull InlineImageResource build() {
                return InlineImageResource.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A non-seekable animated image resource that maps to an Android drawable by resource ID. The
     * animation is started with given trigger, fire and forget.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class AndroidAnimatedImageResourceByResId {
        private final ResourceProto.AndroidAnimatedImageResourceByResId mImpl;

        AndroidAnimatedImageResourceByResId(
                ResourceProto.AndroidAnimatedImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /** Gets the format for the animated image. */
        @AnimatedImageFormat
        public int getAnimatedImageFormat() {
            return mImpl.getAnimatedImageFormat().getNumber();
        }

        /** Gets the Android resource ID, e.g. R.drawable.foo. */
        @DrawableRes
        public int getResourceId() {
            return mImpl.getResourceId();
        }

        /** Gets the trigger to start the animation. */
        public @Nullable Trigger getStartTrigger() {
            if (mImpl.hasStartTrigger()) {
                return TriggerBuilders.triggerFromProto(mImpl.getStartTrigger());
            } else {
                return null;
            }
        }

        @Override
        @SuppressWarnings("ResourceType")
        public int hashCode() {
            return Objects.hash(
                    getResourceId(), getAnimatedImageFormat(), Trigger.hash(getStartTrigger()));
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidAnimatedImageResourceByResId)) {
                return false;
            }
            AndroidAnimatedImageResourceByResId that = (AndroidAnimatedImageResourceByResId) obj;
            return that.getResourceId() == getResourceId()
                    && that.getAnimatedImageFormat() == getAnimatedImageFormat()
                    && Trigger.equal(that.getStartTrigger(), getStartTrigger());
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidAnimatedImageResourceByResId fromProto(
                ResourceProto.@NonNull AndroidAnimatedImageResourceByResId proto) {
            return new AndroidAnimatedImageResourceByResId(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull AndroidAnimatedImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the format for the animated image. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimatedImageFormat(
                    @AnimatedImageFormat int animatedImageFormat) {
                mImpl.setAnimatedImageFormat(
                        ResourceProto.AnimatedImageFormat.forNumber(animatedImageFormat));
                return this;
            }

            /** Sets the Android resource ID, e.g. R.drawable.foo. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setResourceId(@DrawableRes int resourceId) {
                mImpl.setResourceId(resourceId);
                return this;
            }

            /** Sets the trigger to start the animation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStartTrigger(@NonNull Trigger startTrigger) {
                mImpl.setStartTrigger(startTrigger.toTriggerProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AndroidAnimatedImageResourceByResId build() {
                return AndroidAnimatedImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A seekable animated image resource that maps to an Android drawable by resource ID. The
     * animation progress is bound to the provided dynamic float.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class AndroidSeekableAnimatedImageResourceByResId {
        private final ResourceProto.AndroidSeekableAnimatedImageResourceByResId mImpl;

        AndroidSeekableAnimatedImageResourceByResId(
                ResourceProto.AndroidSeekableAnimatedImageResourceByResId impl) {
            this.mImpl = impl;
        }

        /** Gets the format for the animated image. */
        @AnimatedImageFormat
        public int getAnimatedImageFormat() {
            return mImpl.getAnimatedImageFormat().getNumber();
        }

        /** Gets the Android resource ID, e.g. R.drawable.foo. */
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
         */
        public @Nullable DynamicFloat getProgress() {
            if (mImpl.hasProgress()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getProgress());
            } else {
                return null;
            }
        }

        @Override
        @SuppressWarnings("ResourceType")
        public int hashCode() {
            DynamicFloat progress = getProgress();
            return Objects.hash(
                    getResourceId(),
                    getAnimatedImageFormat(),
                    progress != null
                            ? DynamicProtoHashEquals.hashCode(progress.toDynamicFloatProto())
                            : null);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidSeekableAnimatedImageResourceByResId)) {
                return false;
            }
            AndroidSeekableAnimatedImageResourceByResId that =
                    (AndroidSeekableAnimatedImageResourceByResId) obj;
            DynamicFloat progress = getProgress();
            DynamicFloat thatProgress = that.getProgress();
            return that.getResourceId() == getResourceId()
                    && that.getAnimatedImageFormat() == getAnimatedImageFormat()
                    && DynamicProtoHashEquals.equals(
                            progress != null ? progress.toDynamicFloatProto() : null,
                            thatProgress != null ? thatProgress.toDynamicFloatProto() : null);
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidSeekableAnimatedImageResourceByResId fromProto(
                ResourceProto.@NonNull AndroidSeekableAnimatedImageResourceByResId proto) {
            return new AndroidSeekableAnimatedImageResourceByResId(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull AndroidSeekableAnimatedImageResourceByResId toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the format for the animated image. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimatedImageFormat(
                    @AnimatedImageFormat int animatedImageFormat) {
                mImpl.setAnimatedImageFormat(
                        ResourceProto.AnimatedImageFormat.forNumber(animatedImageFormat));
                return this;
            }

            /** Sets the Android resource ID, e.g. R.drawable.foo. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setResourceId(@DrawableRes int resourceId) {
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
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setProgress(@NonNull DynamicFloat progress) {
                mImpl.setProgress(progress.toDynamicFloatProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AndroidSeekableAnimatedImageResourceByResId build() {
                return AndroidSeekableAnimatedImageResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /** A Lottie resource that is read from a raw Android resource ID. */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public static final class AndroidLottieResourceByResId {
        private final ResourceProto.AndroidLottieResourceByResId mImpl;

        AndroidLottieResourceByResId(ResourceProto.AndroidLottieResourceByResId impl) {
            this.mImpl = impl;
        }

        /** Gets the Android resource ID, e.g. R.raw.foo. */
        @RawRes
        public int getRawResourceId() {
            return mImpl.getRawResourceId();
        }

        /**
         * Gets a {@link androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat},
         * normally transformed from certain states with the data binding pipeline to control the
         * progress of the animation.
         */
        public @Nullable DynamicFloat getProgress() {
            if (mImpl.hasProgress()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getProgress());
            } else {
                return null;
            }
        }

        /** Gets the trigger to start the animation. */
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @Nullable Trigger getStartTrigger() {
            if (mImpl.hasStartTrigger()) {
                return TriggerBuilders.triggerFromProto(mImpl.getStartTrigger());
            } else {
                return null;
            }
        }

        /**
         * Gets the collection of properties to customize Lottie further.
         *
         * <p>There shouldn't be more than 10 of properties in this collection. Each property can be
         * applied to one or more elements in Lottie file.
         */
        public @NonNull List<LottieProperty> getProperties() {
            List<LottieProperty> list = new ArrayList<>();
            for (ResourceProto.LottieProperty item : mImpl.getPropertiesList()) {
                list.add(ResourceBuilders.lottiePropertyFromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        @Override
        public int hashCode() {
            DynamicFloat progress = getProgress();
            return Objects.hash(
                    getRawResourceId(),
                    Trigger.hash(getStartTrigger()),
                    progress != null
                            ? DynamicProtoHashEquals.hashCode(progress.toDynamicFloatProto())
                            : null);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AndroidLottieResourceByResId)) {
                return false;
            }
            AndroidLottieResourceByResId that = (AndroidLottieResourceByResId) obj;
            DynamicFloat progress = getProgress();
            DynamicFloat thatProgress = that.getProgress();
            return that.getRawResourceId() == getRawResourceId()
                    && Trigger.equal(that.getStartTrigger(), getStartTrigger())
                    && DynamicProtoHashEquals.equals(
                            progress != null ? progress.toDynamicFloatProto() : null,
                            thatProgress != null ? thatProgress.toDynamicFloatProto() : null);
        }

        /**
         * Returns how many properties can be added for customization of one Lottie animation via
         * {@link Builder#setProperties(LottieProperty...)} method.
         */
        public static int getMaxPropertiesCount() {
            return LOTTIE_PROPERTIES_LIMIT;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull AndroidLottieResourceByResId fromProto(
                ResourceProto.@NonNull AndroidLottieResourceByResId proto) {
            return new AndroidLottieResourceByResId(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull AndroidLottieResourceByResId toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "AndroidLottieResourceByResId{"
                    + "rawResourceId="
                    + getRawResourceId()
                    + ", progress="
                    + getProgress()
                    + ", startTrigger="
                    + getStartTrigger()
                    + ", properties="
                    + getProperties()
                    + "}";
        }

        /** Builder for {@link AndroidLottieResourceByResId} */
        public static final class Builder {
            private final ResourceProto.AndroidLottieResourceByResId.Builder mImpl =
                    ResourceProto.AndroidLottieResourceByResId.newBuilder();

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param resourceId the Android resource ID, e.g. R.raw.foo.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            @SuppressLint("CheckResult") // (b/247804720)
            public Builder(@RawRes int resourceId) {
                setRawResourceId(resourceId);
            }

            @RequiresSchemaVersion(major = 1, minor = 500)
            Builder() {}

            /** Sets the Android resource ID, e.g. R.raw.foo. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            @NonNull Builder setRawResourceId(@RawRes int rawResourceId) {
                mImpl.setRawResourceId(rawResourceId);
                return this;
            }

            /**
             * Sets a {@link androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat},
             * normally transformed from certain states with the data binding pipeline to control
             * the progress of the animation.
             *
             * <p>Its value is required to fall in the range of [0.0, 1.0]. Any values outside this
             * range would be clamped.
             *
             * <p>When the first value of the {@link
             * androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat} arrives, the
             * animation starts from progress 0 to that value. After that it plays from current
             * progress to the new value on subsequent updates.
             *
             * <p>If not set, the animation will play on load.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setProgress(@NonNull DynamicFloat progress) {
                mImpl.setProgress(progress.toDynamicFloatProto());
                return this;
            }

            /** Sets the trigger to start the animation. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setStartTrigger(@NonNull Trigger startTrigger) {
                mImpl.setStartTrigger(startTrigger.toTriggerProto());
                return this;
            }

            /**
             * Adds one item to the collection of properties to customize Lottie further.
             *
             * <p>There shouldn't be more than 10 of properties in this collection. Each property
             * can be applied to one or more elements in Lottie file.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            @NonNull Builder addProperty(@NonNull LottieProperty property) {
                mImpl.addProperties(property.toLottiePropertyProto());
                return this;
            }

            @VisibleForTesting static final int LOTTIE_PROPERTIES_LIMIT = 10;

            /**
             * Adds customizations for Lottie animation, for example to apply a color to a slot with
             * specified ID.
             *
             * <p>Only up to {@link #getMaxPropertiesCount()} properties are allowed to be added for
             * customization of one Lottie animation.
             *
             * <p>If more than one {@link LottieProperty} with the same filter and different value
             * (for example, with the same slot ID and different color in case of {@link
             * LottieProperty#colorForSlot}) is added, the last filter will be applied.
             *
             * @throws IllegalArgumentException if the number of properties added is larger than
             *     {@link #getMaxPropertiesCount()}.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public @NonNull Builder setProperties(LottieProperty @NonNull ... properties) {
                if (properties.length > LOTTIE_PROPERTIES_LIMIT) {
                    throw new IllegalArgumentException(
                            "Number of given LottieProperty can't be larger than "
                                    + LOTTIE_PROPERTIES_LIMIT
                                    + ".");
                }

                for (LottieProperty property : properties) {
                    addProperty(property);
                }
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AndroidLottieResourceByResId build() {
                return AndroidLottieResourceByResId.fromProto(mImpl.build());
            }
        }
    }

    /**
     * An image resource, which can be used by layouts. This holds multiple underlying resource
     * types, which the underlying runtime will pick according to what it thinks is appropriate.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ImageResource {
        private final ResourceProto.ImageResource mImpl;
        @Nullable private Integer mHashCode = null;

        ImageResource(ResourceProto.ImageResource impl) {
            this.mImpl = impl;
        }

        /** Gets an image resource that maps to an Android drawable by resource ID. */
        public @Nullable AndroidImageResourceByResId getAndroidResourceByResId() {
            if (mImpl.hasAndroidResourceByResId()) {
                return AndroidImageResourceByResId.fromProto(mImpl.getAndroidResourceByResId());
            } else {
                return null;
            }
        }

        /** Gets an image resource that contains the image data inline. */
        public @Nullable InlineImageResource getInlineResource() {
            if (mImpl.hasInlineResource()) {
                return InlineImageResource.fromProto(mImpl.getInlineResource());
            } else {
                return null;
            }
        }

        /**
         * Gets a non-seekable animated image resource that maps to an Android drawable by resource
         * ID. The animation is started with given trigger, fire and forget.
         */
        @ProtoLayoutExperimental
        public @Nullable AndroidAnimatedImageResourceByResId getAndroidAnimatedResourceByResId() {
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
         */
        @ProtoLayoutExperimental
        public @Nullable AndroidSeekableAnimatedImageResourceByResId
                getAndroidSeekableAnimatedResourceByResId() {
            if (mImpl.hasAndroidSeekableAnimatedResourceByResId()) {
                return AndroidSeekableAnimatedImageResourceByResId.fromProto(
                        mImpl.getAndroidSeekableAnimatedResourceByResId());
            } else {
                return null;
            }
        }

        /** Gets a Lottie resource that is read from a raw Android resource ID. */
        public @Nullable AndroidLottieResourceByResId getAndroidLottieResourceByResId() {
            if (mImpl.hasAndroidLottieResourceByResId()) {
                return AndroidLottieResourceByResId.fromProto(
                        mImpl.getAndroidLottieResourceByResId());
            } else {
                return null;
            }
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public int hashCode() {
            if (mHashCode != null) {
                return mHashCode;
            }

            mHashCode =
                    Objects.hash(
                            getAndroidResourceByResId(),
                            getAndroidAnimatedResourceByResId(),
                            getAndroidSeekableAnimatedResourceByResId(),
                            getAndroidLottieResourceByResId(),
                            getInlineResource());

            return mHashCode;
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ImageResource)) {
                return false;
            }
            ImageResource that = (ImageResource) obj;
            return Objects.equals(that.getAndroidResourceByResId(), getAndroidResourceByResId())
                    && Objects.equals(
                            that.getAndroidAnimatedResourceByResId(),
                            getAndroidAnimatedResourceByResId())
                    && Objects.equals(
                            that.getAndroidSeekableAnimatedResourceByResId(),
                            getAndroidSeekableAnimatedResourceByResId())
                    && Objects.equals(
                            that.getAndroidLottieResourceByResId(),
                            getAndroidLottieResourceByResId())
                    && Objects.equals(that.getInlineResource(), getInlineResource());
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ImageResource fromProto(ResourceProto.@NonNull ImageResource proto) {
            return new ImageResource(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull ImageResource toProto() {
            return mImpl;
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @NonNull String toString() {
            return "ImageResource{"
                    + "androidResourceByResId="
                    + getAndroidResourceByResId()
                    + ", inlineResource="
                    + getInlineResource()
                    + ", androidAnimatedResourceByResId="
                    + getAndroidAnimatedResourceByResId()
                    + ", androidSeekableAnimatedResourceByResId="
                    + getAndroidSeekableAnimatedResourceByResId()
                    + ", androidLottieResourceByResId="
                    + getAndroidLottieResourceByResId()
                    + "}";
        }

        /** Builder for {@link ImageResource} */
        public static final class Builder {
            private final ResourceProto.ImageResource.Builder mImpl =
                    ResourceProto.ImageResource.newBuilder();

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets an image resource that maps to an Android drawable by resource ID. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAndroidResourceByResId(
                    @NonNull AndroidImageResourceByResId androidResourceByResId) {
                mImpl.setAndroidResourceByResId(androidResourceByResId.toProto());
                return this;
            }

            /** Sets an image resource that contains the image data inline. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setInlineResource(@NonNull InlineImageResource inlineResource) {
                mImpl.setInlineResource(inlineResource.toProto());
                return this;
            }

            /**
             * Sets a non-seekable animated image resource that maps to an Android drawable by
             * resource ID. The animation is started with given trigger, fire and forget.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setAndroidAnimatedResourceByResId(
                    @NonNull AndroidAnimatedImageResourceByResId androidAnimatedResourceByResId) {
                mImpl.setAndroidAnimatedResourceByResId(androidAnimatedResourceByResId.toProto());
                return this;
            }

            /**
             * Sets a seekable animated image resource that maps to an Android drawable by resource
             * ID. The animation progress is bound to the provided dynamic float.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setAndroidSeekableAnimatedResourceByResId(
                    @NonNull AndroidSeekableAnimatedImageResourceByResId
                            androidSeekableAnimatedResourceByResId) {
                mImpl.setAndroidSeekableAnimatedResourceByResId(
                        androidSeekableAnimatedResourceByResId.toProto());
                return this;
            }

            /** sets a Lottie resource that is read from a raw Android resource ID. */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setAndroidLottieResourceByResId(
                    @NonNull AndroidLottieResourceByResId androidLottieResourceByResId) {
                mImpl.setAndroidLottieResourceByResId(androidLottieResourceByResId.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ImageResource build() {
                return ImageResource.fromProto(mImpl.build());
            }
        }
    }

    /** The resources for a layout. */
    @RequiresSchemaVersion(major = 1, minor = 0)
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
         */
        public @NonNull String getVersion() {
            return mImpl.getVersion();
        }

        /** Gets a map of resource_ids to images, which can be used by layouts. */
        public @NonNull Map<String, ImageResource> getIdToImageMapping() {
            Map<String, ImageResource> map = new HashMap<>();
            for (Entry<String, ResourceProto.ImageResource> entry :
                    mImpl.getIdToImageMap().entrySet()) {
                map.put(entry.getKey(), ImageResource.fromProto(entry.getValue()));
            }
            return Collections.unmodifiableMap(map);
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Resources fromProto(ResourceProto.@NonNull Resources proto) {
            return new Resources(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull Resources toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
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
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setVersion(@NonNull String version) {
                mImpl.setVersion(version);
                return this;
            }

            /** Adds an entry into a map of resource_ids to images, which can be used by layouts. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder addIdToImageMapping(
                    @NonNull String id, @NonNull ImageResource image) {
                mImpl.putIdToImage(id, image.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Resources build() {
                return Resources.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A single point of customization in a Lottie file.
     *
     * <p>Property can be applicable to one or more elements.
     */
    @RequiresSchemaVersion(major = 1, minor = 600)
    public abstract static class LottieProperty {
        private LottieProperty() {}

        /**
         * {@link LottieProperty} customization for applying the given color on all slots with the
         * specified slot ID.
         *
         * @param sid the slot ID to which color filter should be applied. The same ID can
         *     correspond to multiple slots, however, for each of those, all parents in the path
         *     should have names.
         * @param color the color to be applied on slots with this slot ID
         */
        @RequiresSchemaVersion(major = 1, minor = 600)
        public static @NonNull LottieProperty colorForSlot(
                @NonNull String sid, @NonNull ColorProp color) {
            return new LottieColorForSlot.Builder().setSid(sid).setColor(color).build();
        }

        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public abstract ResourceProto.@NonNull LottieProperty toLottiePropertyProto();

        /** Get the fingerprint for this object or null if unknown. */
        abstract @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link LottieProperty} objects. */
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull LottieProperty build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull LottieProperty lottiePropertyFromProto(
            ResourceProto.@NonNull LottieProperty proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasSlotColor()) {
            return LottieColorForSlot.fromProto(proto.getSlotColor(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of LottieProperty");
    }

    static @NonNull LottieProperty lottiePropertyFromProto(
            ResourceProto.@NonNull LottieProperty proto) {
        return lottiePropertyFromProto(proto, null);
    }

    /**
     * A single point of customization for Lottie file, that applies given color on all slots with
     * the specified slot ID.
     */
    @RequiresSchemaVersion(major = 1, minor = 600)
    static final class LottieColorForSlot extends LottieProperty {
        private final ResourceProto.LottieColorForSlot mImpl;
        private final @Nullable Fingerprint mFingerprint;

        LottieColorForSlot(
                ResourceProto.LottieColorForSlot impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the slot ID to which color filter should be applied. The same ID can correspond to
         * multiple slots, however, for each of those, all parents in the path should have names and
         * the last name needs to have the same value as this slot ID.
         */
        public @NonNull String getSid() {
            return mImpl.getSid();
        }

        /** Gets the color to be applied on slots with this slot ID. */
        public @Nullable ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull LottieColorForSlot fromProto(
                ResourceProto.@NonNull LottieColorForSlot proto,
                @Nullable Fingerprint fingerprint) {
            return new LottieColorForSlot(proto, fingerprint);
        }

        static @NonNull LottieColorForSlot fromProto(
                ResourceProto.@NonNull LottieColorForSlot proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ResourceProto.@NonNull LottieColorForSlot toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ResourceProto.@NonNull LottieProperty toLottiePropertyProto() {
            return ResourceProto.LottieProperty.newBuilder().setSlotColor(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "LottieColorForSlot{" + "sid=" + getSid() + ", color=" + getColor() + "}";
        }

        /** Builder for {@link LottieColorForSlot}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements LottieProperty.Builder {
            private final ResourceProto.LottieColorForSlot.Builder mImpl =
                    ResourceProto.LottieColorForSlot.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1002991796);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 600)
            Builder() {}

            /**
             * Sets the slot ID to which color filter should be applied. The same ID can correspond
             * to multiple slots, however, for each of those, all parents in the path should have
             * names and the last name needs to have the same value as this slot ID.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public @NonNull Builder setSid(@NonNull String sid) {
                mImpl.setSid(sid);
                mFingerprint.recordPropertyUpdate(1, sid.hashCode());
                return this;
            }

            /**
             * Sets the color to be applied on slots with this slot ID.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
                if (color.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "LottieColorForSlot.Builder.setColor doesn't support dynamic"
                                    + " values.");
                }
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull LottieColorForSlot build() {
                return new LottieColorForSlot(mImpl.build(), mFingerprint);
            }
        }
    }
}
