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

package androidx.wear.tiles;

import static androidx.annotation.Dimension.DP;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.DeviceParametersProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Builders for request messages used to fetch tiles and resources. */
public final class DeviceParametersBuilders {
    private DeviceParametersBuilders() {}

    /**
     * The platform of the device requesting a tile.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({DEVICE_PLATFORM_UNDEFINED, DEVICE_PLATFORM_WEAR_OS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DevicePlatform {}

    /** Device platform is undefined. */
    public static final int DEVICE_PLATFORM_UNDEFINED = 0;

    /** Device is a Wear OS device. */
    public static final int DEVICE_PLATFORM_WEAR_OS = 1;

    /**
     * The shape of a screen.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({SCREEN_SHAPE_UNDEFINED, SCREEN_SHAPE_ROUND, SCREEN_SHAPE_RECT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenShape {}

    /** Screen shape is undefined. */
    public static final int SCREEN_SHAPE_UNDEFINED = 0;

    /** A round screen (typically found on most Wear devices). */
    public static final int SCREEN_SHAPE_ROUND = 1;

    /** Rectangular screens. */
    public static final int SCREEN_SHAPE_RECT = 2;

    /**
     * Parameters describing the device requesting a tile update. This contains physical and logical
     * characteristics about the device (e.g. screen size and density, etc).
     */
    public static final class DeviceParameters {
        private final DeviceParametersProto.DeviceParameters mImpl;

        private DeviceParameters(DeviceParametersProto.DeviceParameters impl) {
            this.mImpl = impl;
        }

        /** Gets width of the device's screen in DP. */
        @Dimension(unit = DP)
        public int getScreenWidthDp() {
            return mImpl.getScreenWidthDp();
        }

        /** Gets height of the device's screen in DP. */
        @Dimension(unit = DP)
        public int getScreenHeightDp() {
            return mImpl.getScreenHeightDp();
        }

        /**
         * Gets density of the display. This value is the scaling factor to get from DP to Pixels
         * (px = dp * density).
         */
        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public float getScreenDensity() {
            return mImpl.getScreenDensity();
        }

        /** Gets the platform of the device. */
        @DevicePlatform
        public int getDevicePlatform() {
            return mImpl.getDevicePlatform().getNumber();
        }

        /** Gets the shape of the device's screen. */
        @ScreenShape
        public int getScreenShape() {
            return mImpl.getScreenShape().getNumber();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static DeviceParameters fromProto(
                @NonNull DeviceParametersProto.DeviceParameters proto) {
            return new DeviceParameters(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DeviceParametersProto.DeviceParameters toProto() {
            return mImpl;
        }

        /** Builder for {@link DeviceParameters} */
        public static final class Builder {
            private final DeviceParametersProto.DeviceParameters.Builder mImpl =
                    DeviceParametersProto.DeviceParameters.newBuilder();

            public Builder() {}

            /** Sets width of the device's screen in DP. */
            @NonNull
            public Builder setScreenWidthDp(@Dimension(unit = DP) int screenWidthDp) {
                mImpl.setScreenWidthDp(screenWidthDp);
                return this;
            }

            /** Sets height of the device's screen in DP. */
            @NonNull
            public Builder setScreenHeightDp(@Dimension(unit = DP) int screenHeightDp) {
                mImpl.setScreenHeightDp(screenHeightDp);
                return this;
            }

            /**
             * Sets density of the display. This value is the scaling factor to get from DP to
             * Pixels (px = dp * density).
             */
            @NonNull
            public Builder setScreenDensity(
                    @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
                            float screenDensity) {
                mImpl.setScreenDensity(screenDensity);
                return this;
            }

            /** Sets the platform of the device. */
            @NonNull
            public Builder setDevicePlatform(@DevicePlatform int devicePlatform) {
                mImpl.setDevicePlatform(
                        DeviceParametersProto.DevicePlatform.forNumber(devicePlatform));
                return this;
            }

            /** Sets the shape of the device's screen. */
            @NonNull
            public Builder setScreenShape(@ScreenShape int screenShape) {
                mImpl.setScreenShape(DeviceParametersProto.ScreenShape.forNumber(screenShape));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public DeviceParameters build() {
                return DeviceParameters.fromProto(mImpl.build());
            }
        }
    }
}
