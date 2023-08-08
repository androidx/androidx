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

import static androidx.annotation.Dimension.DP;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo;
import androidx.wear.protolayout.proto.DeviceParametersProto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Builders for request messages used to fetch layouts and resources. */
public final class DeviceParametersBuilders {
    private DeviceParametersBuilders() {}

    /**
     * The platform of the device requesting a layout.
     *
     * @since 1.0
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({DEVICE_PLATFORM_UNDEFINED, DEVICE_PLATFORM_WEAR_OS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DevicePlatform {}

    /**
     * Device platform is undefined.
     *
     * @since 1.0
     */
    public static final int DEVICE_PLATFORM_UNDEFINED = 0;

    /**
     * Device is a Wear OS device.
     *
     * @since 1.0
     */
    public static final int DEVICE_PLATFORM_WEAR_OS = 1;

    /**
     * The shape of a screen.
     *
     * @since 1.0
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({SCREEN_SHAPE_UNDEFINED, SCREEN_SHAPE_ROUND, SCREEN_SHAPE_RECT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenShape {}

    /**
     * Screen shape is undefined.
     *
     * @since 1.0
     */
    public static final int SCREEN_SHAPE_UNDEFINED = 0;

    /**
     * A round screen (typically found on most Wear devices).
     *
     * @since 1.0
     */
    public static final int SCREEN_SHAPE_ROUND = 1;

    /**
     * Rectangular screens.
     *
     * @since 1.0
     */
    public static final int SCREEN_SHAPE_RECT = 2;

    /**
     * Parameters describing the device requesting a layout update. This contains physical and
     * logical characteristics about the device (e.g. screen size and density, etc).
     *
     * @since 1.0
     */
    public static final class DeviceParameters {
        private final DeviceParametersProto.DeviceParameters mImpl;

        DeviceParameters(DeviceParametersProto.DeviceParameters impl) {
            this.mImpl = impl;
        }

        /**
         * Gets width of the device's screen in DP.
         *
         * @since 1.0
         */
        @Dimension(unit = DP)
        public int getScreenWidthDp() {
            return mImpl.getScreenWidthDp();
        }

        /**
         * Gets height of the device's screen in DP.
         *
         * @since 1.0
         */
        @Dimension(unit = DP)
        public int getScreenHeightDp() {
            return mImpl.getScreenHeightDp();
        }

        /**
         * Gets density of the display. This value is the scaling factor to get from DP to Pixels
         * (px = dp * density).
         *
         * @since 1.0
         */
        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public float getScreenDensity() {
            return mImpl.getScreenDensity();
        }

        /**
         * Gets current user preference for the scaling factor for fonts displayed on the display.
         * This value is used to get from SP to DP (dp = sp * font_scale).
         *
         * @since 1.2
         */
        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public float getFontScale() {
            return mImpl.getFontScale();
        }

        /**
         * Gets the platform of the device.
         *
         * @since 1.0
         */
        @DevicePlatform
        public int getDevicePlatform() {
            return mImpl.getDevicePlatform().getNumber();
        }

        /**
         * Gets the shape of the device's screen.
         *
         * @since 1.0
         */
        @ScreenShape
        public int getScreenShape() {
            return mImpl.getScreenShape().getNumber();
        }

        /**
         * Gets the maximum schema version supported by the current renderer. When building a layout
         * that uses features not available on schema version 1.0 , this can be used to
         * conditionally choose which feature to use.
         *
         * @since 1.2
         */
        @NonNull
        public VersionInfo getRendererSchemaVersion() {
            if (mImpl.hasRendererSchemaVersion()) {
                return VersionInfo.fromProto(mImpl.getRendererSchemaVersion());
            } else {
                return new VersionInfo.Builder().setMajor(1).setMinor(0).build();
            }
        }

        /**
         * Gets renderer supported {@link Capabilities}.
         *
         * @since 1.2
         */
        @ProtoLayoutExperimental
        @Nullable
        public Capabilities getCapabilities() {
            if (mImpl.hasCapabilities()) {
                return Capabilities.fromProto(mImpl.getCapabilities());
            } else {
                return null;
            }
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static DeviceParameters fromProto(
                @NonNull DeviceParametersProto.DeviceParameters proto) {
            return new DeviceParameters(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DeviceParametersProto.DeviceParameters toProto() {
            return mImpl;
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        @NonNull
        public String toString() {
            return "DeviceParameters{"
                    + "screenWidthDp="
                    + getScreenWidthDp()
                    + ", screenHeightDp="
                    + getScreenHeightDp()
                    + ", screenDensity="
                    + getScreenDensity()
                    + ", fontScale="
                    + getFontScale()
                    + ", devicePlatform="
                    + getDevicePlatform()
                    + ", screenShape="
                    + getScreenShape()
                    + ", rendererSchemaVersion="
                    + getRendererSchemaVersion()
                    + ", capabilities="
                    + getCapabilities()
                    + "}";
        }

        /** Builder for {@link DeviceParameters} */
        public static final class Builder {
            private final DeviceParametersProto.DeviceParameters.Builder mImpl =
                    DeviceParametersProto.DeviceParameters.newBuilder();

            public Builder() {}

            /**
             * Sets width of the device's screen in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setScreenWidthDp(@Dimension(unit = DP) int screenWidthDp) {
                mImpl.setScreenWidthDp(screenWidthDp);
                return this;
            }

            /**
             * Sets height of the device's screen in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setScreenHeightDp(@Dimension(unit = DP) int screenHeightDp) {
                mImpl.setScreenHeightDp(screenHeightDp);
                return this;
            }

            /**
             * Sets density of the display. This value is the scaling factor to get from DP to
             * Pixels (px = dp * density).
             *
             * @since 1.0
             */
            @NonNull
            public Builder setScreenDensity(
                    @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
                            float screenDensity) {
                mImpl.setScreenDensity(screenDensity);
                return this;
            }

            /**
             * Sets current user preference for the scaling factor for fonts displayed on the
             * display. This value is used to get from SP to DP (dp = sp * font_scale).
             *
             * @since 1.2
             */
            @NonNull
            public Builder setFontScale(
                    @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
                            float fontScale) {
                mImpl.setFontScale(fontScale);
                return this;
            }

            /**
             * Sets the platform of the device.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setDevicePlatform(@DevicePlatform int devicePlatform) {
                mImpl.setDevicePlatform(
                        DeviceParametersProto.DevicePlatform.forNumber(devicePlatform));
                return this;
            }

            /**
             * Sets the shape of the device's screen.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setScreenShape(@ScreenShape int screenShape) {
                mImpl.setScreenShape(DeviceParametersProto.ScreenShape.forNumber(screenShape));
                return this;
            }

            /**
             * Sets the maximum schema version supported by the current renderer. When building a
             * layout that uses features not available on schema version 1.0 , this can be used to
             * conditionally choose which feature to use.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setRendererSchemaVersion(@NonNull VersionInfo rendererSchemaVersion) {
                mImpl.setRendererSchemaVersion(rendererSchemaVersion.toProto());
                return this;
            }

            /**
             * Sets renderer supported {@link Capabilities}.
             *
             * @since 1.2
             */
            @ProtoLayoutExperimental
            @NonNull
            public Builder setCapabilities(@NonNull Capabilities capabilities) {
                mImpl.setCapabilities(capabilities.toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public DeviceParameters build() {
                return DeviceParameters.fromProto(mImpl.build());
            }
        }
    }

    /**
     * {@link Capabilities} describing the features that the renderer supports. These features are
     * not necessarily tied to a specific schema version. {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Layout} providers can use these information
     * to conditionally generate different layouts based on the presence/value of a feature.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class Capabilities {
        private final DeviceParametersProto.Capabilities mImpl;

        Capabilities(DeviceParametersProto.Capabilities impl) {
            this.mImpl = impl;
        }

        /**
         * Gets current minimum freshness limit in milliseconds for a layout. This can change based
         * on various factors. Any freshness request lower than the current limit will be replaced
         * by that limit. A value of 0 here signifies that the minimum freshness limit in unknown.
         *
         * @since 1.2
         */
        @ProtoLayoutExperimental
        public long getMinimumFreshnessLimitMillis() {
            return mImpl.getMinimumFreshnessLimitMillis();
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Capabilities fromProto(@NonNull DeviceParametersProto.Capabilities proto) {
            return new Capabilities(proto);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DeviceParametersProto.Capabilities toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "Capabilities{"
                    + "minimumFreshnessLimitMillis="
                    + getMinimumFreshnessLimitMillis()
                    + "}";
        }

        /** Builder for {@link Capabilities} */
        public static final class Builder {
            private final DeviceParametersProto.Capabilities.Builder mImpl =
                    DeviceParametersProto.Capabilities.newBuilder();

            public Builder() {}

            /**
             * Sets current minimum freshness limit in milliseconds for a layout. This can change
             * based on various factors. Any freshness request lower than the current limit will be
             * replaced by that limit. A value of 0 here signifies that the minimum freshness limit
             * in unknown.
             *
             * @since 1.2
             */
            @NonNull
            @ProtoLayoutExperimental
            public Builder setMinimumFreshnessLimitMillis(long minimumFreshnessLimitMillis) {
                mImpl.setMinimumFreshnessLimitMillis(minimumFreshnessLimitMillis);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Capabilities build() {
                return Capabilities.fromProto(mImpl.build());
            }
        }
    }
}
