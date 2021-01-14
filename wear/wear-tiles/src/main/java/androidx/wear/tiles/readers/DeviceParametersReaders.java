/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.tiles.readers;

import static androidx.annotation.Dimension.DP;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.wear.tiles.proto.DeviceParametersProto;
import androidx.wear.tiles.readers.RequestReaders.TileRequest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Readers for androidx.wear.tiles' device parameters structures. */
public class DeviceParametersReaders {
    private DeviceParametersReaders() {}

    /**
     * The platform of the device requesting a tile.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({DEVICE_PLATFORM_UNDEFINED, DEVICE_PLATFORM_WEAR_OS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DevicePlatform {}

    /** Device platform is undefined. */
    public static final int DEVICE_PLATFORM_UNDEFINED = 0;

    /** Device is a Wear OS by Google device. */
    public static final int DEVICE_PLATFORM_WEAR_OS = 1;

    /**
     * The shape of a screen.
     *
     * @hide
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

    /** Reader for the Device Parameters returned from {@link TileRequest#getDeviceParameters()}. */
    public static class DeviceParameters {
        private final DeviceParametersProto.DeviceParameters mProto;

        DeviceParameters(DeviceParametersProto.DeviceParameters proto) {
            this.mProto = proto;
        }

        /** Get the width of the screen, in DP. */
        @Dimension(unit = DP)
        public int getScreenWidthDp() {
            return mProto.getScreenWidthDp();
        }

        /** Get the height of the screen, in DP. */
        @Dimension(unit = DP)
        public int getScreenHeightDp() {
            return mProto.getScreenHeightDp();
        }

        /**
         * Get the density of the screen. This value is the scaling factor to get from DP to Pixels,
         * where PX = DP * density.
         */
        @FloatRange(from = 0.0, fromInclusive = false)
        public float getScreenDensity() {
            return mProto.getScreenDensity();
        }

        /** Get the platform of the device. */
        @DevicePlatform
        public int getDevicePlatform() {
            return mProto.getDevicePlatformValue();
        }

        /** Get the shape of the screen of the device. */
        @ScreenShape
        public int getScreenShape() {
            return mProto.getScreenShapeValue();
        }
    }
}
