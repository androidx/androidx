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

package androidx.camera.core;

import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.SizeCoordinate;

/**
 * A set of requirements and priorities used to select a resolution for the use case.
 *
 * <p>The resolution selection mechanism is determined by the following three steps:
 * <ol>
 *     <li> Collect supported output sizes to the candidate resolution list
 *     <li> Determine the selecting priority of the candidate resolution list by the preference
 *     settings
 *     <li> Consider all the resolution selector settings of bound use cases to find the best
 *     resolution for each use case
 * </ol>
 *
 * <p>For the first step, all supported resolution output sizes are put into the candidate
 * resolution list as the base in the beginning.
 *
 * <p>ResolutionSelector provides the following two functions for applications to adjust the
 * conditions of the candidate resolutions.
 * <ul>
 *     <li> {@link Builder#setMaxResolution(Size)}
 *     <li> {@link Builder#setHighResolutionEnabled(boolean)}
 * </ul>
 *
 * <p>For the second step, ResolutionSelector provides the following three functions for
 * applications to determine which resolution has higher priority to be selected.
 * <ul>
 *     <li> {@link Builder#setPreferredResolution(Size)}
 *     <li> {@link Builder#setPreferredResolutionByViewSize(Size)}
 *     <li> {@link Builder#setPreferredAspectRatio(int)}
 * </ul>
 *
 * <p>The resolution that exactly matches the preferred resolution is selected in first priority.
 * If the resolution can't be found, CameraX falls back to use the sizes of the preferred aspect
 * ratio. In this case, the preferred resolution is treated as the minimal bounding size to find
 * the best resolution.
 *
 * <p>Different types of use cases might have their own additional conditions. Please see the use
 * case config builders’ {@code setResolutionSelector()} function to know the condition details
 * for each type of use case.
 *
 * <p>For the third step, CameraX selects the final resolution for the use case based on the
 * camera device's hardware level, capabilities and the bound use case combination. Applications
 * can check which resolution is finally selected by using the use case's {@code
 * getResolutionInfo()} function.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ResolutionSelector {
    @Nullable
    private final Size mPreferredResolution;

    private final SizeCoordinate mSizeCoordinate;

    private final int mPreferredAspectRatio;

    @Nullable
    private final Size mMaxResolution;

    private final boolean mIsHighResolutionEnabled;

    ResolutionSelector(int preferredAspectRatio,
            @Nullable Size preferredResolution,
            @NonNull SizeCoordinate sizeCoordinate,
            @Nullable Size maxResolution,
            boolean isHighResolutionEnabled) {
        mPreferredAspectRatio = preferredAspectRatio;
        mPreferredResolution = preferredResolution;
        mSizeCoordinate = sizeCoordinate;
        mMaxResolution = maxResolution;
        mIsHighResolutionEnabled = isHighResolutionEnabled;
    }

    /**
     * Retrieves the preferred aspect ratio in the ResolutionSelector.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @AspectRatio.Ratio
    public int getPreferredAspectRatio() {
        return mPreferredAspectRatio;
    }

    /**
     * Retrieves the preferred resolution in the ResolutionSelector.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public Size getPreferredResolution() {
        return mPreferredResolution;
    }

    /**
     * Retrieves the size coordinate of the preferred resolution in the ResolutionSelector.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public SizeCoordinate getSizeCoordinate() {
        return mSizeCoordinate;
    }

    /**
     * Returns the max resolution in the ResolutionSelector.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public Size getMaxResolution() {
        return mMaxResolution;
    }

    /**
     * Returns {@code true} if high resolutions are allowed to be selected, otherwise {@code false}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isHighResolutionEnabled() {
        return mIsHighResolutionEnabled;
    }

    /**
     * Builder for a {@link ResolutionSelector}.
     */
    public static final class Builder {
        @AspectRatio.Ratio
        private int mPreferredAspectRatio = AspectRatio.RATIO_4_3;
        @Nullable
        private Size mPreferredResolution = null;
        @NonNull
        private SizeCoordinate mSizeCoordinate = SizeCoordinate.CAMERA_SENSOR;
        @Nullable
        private Size mMaxResolution = null;
        private boolean mIsHighResolutionEnabled = false;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        private Builder(@NonNull ResolutionSelector selector) {
            mPreferredAspectRatio = selector.getPreferredAspectRatio();
            mPreferredResolution = selector.getPreferredResolution();
            mSizeCoordinate = selector.getSizeCoordinate();
            mMaxResolution = selector.getMaxResolution();
            mIsHighResolutionEnabled = selector.isHighResolutionEnabled();
        }

        /**
         * Generates a Builder from another {@link ResolutionSelector} object.
         *
         * @param selector an existing {@link ResolutionSelector}.
         * @return the new Builder.
         */
        @NonNull
        public static Builder fromSelector(@NonNull ResolutionSelector selector) {
            return new Builder(selector);
        }

        /**
         * Sets the preferred aspect ratio that the output images are expected to have.
         *
         * <p>The aspect ratio is the ratio of width to height in the camera sensor's natural
         * orientation. If set, CameraX finds the sizes that match the aspect ratio with priority
         * . Among the sizes that match the aspect ratio, the larger the size, the higher the
         * priority.
         *
         * <p>If CameraX can't find any available sizes that match the preferred aspect ratio,
         * CameraX falls back to select the sizes with the nearest aspect ratio that can contain
         * the full field of view of the sizes with preferred aspect ratio.
         *
         * <p>If preferred aspect ratio is not set, the default aspect ratio is
         * {@link AspectRatio#RATIO_4_3}, which usually has largest field of view because most
         * camera sensor are {@code 4:3}.
         *
         * <p>This API is useful for apps that want to capture images matching the {@code 16:9}
         * display aspect ratio. Apps can set preferred aspect ratio as
         * {@link AspectRatio#RATIO_16_9} to achieve this.
         *
         * <p>The actual aspect ratio of the output may differ from the specified preferred
         * aspect ratio value. Application code should check the resulting output's resolution.
         *
         * @param preferredAspectRatio the aspect ratio you prefer to use.
         * @return the current Builder.
         */
        @NonNull
        public Builder setPreferredAspectRatio(@AspectRatio.Ratio int preferredAspectRatio) {
            mPreferredAspectRatio = preferredAspectRatio;
            return this;
        }

        /**
         * Sets the preferred resolution you expect to select. The resolution is expressed in the
         * camera sensor's natural orientation (landscape), which means you can set the size
         * retrieved from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes} directly.
         *
         * <p>Once the preferred resolution is set, CameraX finds exactly matched size first
         * regardless of the preferred aspect ratio. This API is useful for apps that want to
         * select an exact size retrieved from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes}.
         *
         * <p>If CameraX can't find the size that matches the preferred resolution, it attempts
         * to establish a minimal bound for the given resolution. The actual resolution is the
         * closest available resolution that is not smaller than the preferred resolution.
         * However, if no resolution exists that is equal to or larger than the preferred
         * resolution, the nearest available resolution smaller than the preferred resolution is
         * chosen.
         *
         * <p>When the preferred resolution is used as a minimal bound, CameraX also considers
         * the preferred aspect ratio to find the sizes that either match it or are close to it.
         * Using preferred resolution as the minimal bound is useful for apps that want to shrink
         * the size for the surface. For example, for apps that just show the camera preview in a
         * small view, apps can specify a size smaller than display size. CameraX can effectively
         * select a smaller size for better efficiency.
         *
         * <p>If both {@link Builder#setPreferredResolution(Size)} and
         * {@link Builder#setPreferredResolutionByViewSize(Size)} are invoked, which one set
         * later overrides the one set before.
         *
         * @param preferredResolution the preferred resolution expressed in the orientation of
         *                            the device camera sensor coordinate to choose the preferred
         *                            resolution from supported output sizes list.
         * @return the current Builder.
         */
        @NonNull
        public Builder setPreferredResolution(@NonNull Size preferredResolution) {
            mPreferredResolution = preferredResolution;
            mSizeCoordinate = SizeCoordinate.CAMERA_SENSOR;
            return this;
        }

        /**
         * Sets the preferred resolution you expect to select. The resolution is expressed in the
         * Android {@link View} coordinate system.
         *
         * <p>For phone devices, the sensor coordinate orientation usually has 90 degrees
         * difference from the phone device display’s natural orientation. Depending on the
         * display rotation value when the use case is bound, CameraX transforms the input
         * resolution into the camera sensor's natural orientation to find the best suitable
         * resolution.
         *
         * <p>Once the preferred resolution is set, CameraX finds the size that exactly matches
         * the preferred resolution first regardless of the preferred aspect ratio.
         *
         * <p>If CameraX can't find the size that matches the preferred resolution, it attempts
         * to establish a minimal bound for the given resolution. The actual resolution is the
         * closest available resolution that is not smaller than the preferred resolution.
         * However, if no resolution exists that is equal to or larger than the preferred
         * resolution, the nearest available resolution smaller than the preferred resolution is
         * chosen.
         *
         * <p>When the preferred resolution is used as a minimal bound, CameraX also considers
         * the preferred aspect ratio to find the sizes that either match it or are close to it.
         * Using Android {@link View} size as preferred resolution is useful for apps that want
         * to shrink the size for the surface. For example, for apps that just show the camera
         * preview in a small view, apps can specify the small size of Android {@link View}.
         * CameraX can effectively select a smaller size for better efficiency.
         *
         * <p>If both {@link Builder#setPreferredResolution(Size)} and
         * {@link Builder#setPreferredResolutionByViewSize(Size)} are invoked, the later setting
         * overrides the former one.
         *
         * @param preferredResolutionByViewSize the preferred resolution expressed in the
         *                                      orientation of the app layout's Android
         *                                      {@link View} to choose the preferred resolution
         *                                      from supported output sizes list.
         * @return the current Builder.
         */
        @NonNull
        public Builder setPreferredResolutionByViewSize(
                @NonNull Size preferredResolutionByViewSize) {
            mPreferredResolution = preferredResolutionByViewSize;
            mSizeCoordinate = SizeCoordinate.ANDROID_VIEW;
            return this;
        }

        /**
         * Sets the max resolution condition for the use case.
         *
         * <p>The max resolution prevents the use case to select the sizes which either width or
         * height exceeds the specified resolution.
         *
         * <p>The resolution should be expressed in the camera sensor's natural orientation
         * (landscape).
         *
         * <p>For example, if applications want to select a resolution smaller than a specific
         * resolution to have better performance, a {@link ResolutionSelector} which sets this
         * specific resolution as the max resolution can be used. Or, if applications want to
         * select a larger resolution for a {@link Preview} which has the default max resolution
         * of the small one of device's screen size and 1080p (1920x1080), use a
         * {@link ResolutionSelector} with max resolution.
         *
         * @param resolution the max resolution limitation to choose from supported output sizes
         *                   list.
         * @return the current Builder.
         */
        @NonNull
        public Builder setMaxResolution(@NonNull Size resolution) {
            mMaxResolution = resolution;
            return this;
        }

        /**
         * Sets whether high resolutions are allowed to be selected for the use cases.
         *
         * <p>Calling this function allows the use case to select the high resolution output
         * sizes if it is supported for the camera device.
         *
         * <p>When high resolution is enabled, if an {@link ImageCapture} with
         * {@link ImageCapture#CAPTURE_MODE_ZERO_SHUTTER_LAG} mode is bound, the
         * {@link ImageCapture#CAPTURE_MODE_ZERO_SHUTTER_LAG} mode is forced disabled.
         *
         * <p>When using the {@code camera-extensions} to enable an extension mode, even if high
         * resolution is enabled, the supported high resolution output sizes are still excluded
         * from the candidate resolution list.
         *
         * <p>When using the {@code camera-camera2} CameraX implementation, the supported
         * high resolutions are retrieved from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getHighResolutionOutputSizes(int)}.
         * Be noticed that the high resolution sizes might cause the entire capture session to
         * not meet the 20 fps frame rate. Even if only an ImageCapture use case selects a high
         * resolution, it might still impact the FPS of the Preview, ImageAnalysis or
         * VideoCapture use cases which are bound together. This function only takes effect on
         * devices with
         * {@link android.hardware.camera2.CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE}
         * capability. For devices without
         * {@link android.hardware.camera2.CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE}
         * capability, all resolutions can be retrieved from
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes(int)},
         * but it is not guaranteed to meet >= 20 fps for any resolution in the list.
         *
         * @param enabled {@code true} to allow to select high resolution for the use case.
         * @return the current Builder.
         */
        @NonNull
        public Builder setHighResolutionEnabled(boolean enabled) {
            mIsHighResolutionEnabled = enabled;
            return this;
        }

        /**
         * Builds the {@link ResolutionSelector}.
         *
         * @return the {@link ResolutionSelector} built with the specified resolution settings.
         */
        @NonNull
        public ResolutionSelector build() {
            return new ResolutionSelector(mPreferredAspectRatio, mPreferredResolution,
                    mSizeCoordinate, mMaxResolution, mIsHighResolutionEnabled);
        }
    }
}
