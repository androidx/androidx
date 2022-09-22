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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A set of requirements and priorities used to select a resolution for the use case.
 *
 * <p>The resolution size or the aspect ratio parameters being set in this
 * {@link ResolutionSelector} is used to find the surface size in the camera sensor's natural
 * orientation (landscape) from the supported resolution list. In camera2 implementation, this
 * supported resolution list can be retrieved from
 * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes(int)}. The
 * {@link android.hardware.camera2.params.StreamConfigurationMap} can be retrieved from interop
 * class
 * {@link androidx.camera.camera2.interop.Camera2CameraInfo#getCameraCharacteristic(CameraCharacteristics.Key)}.
 *
 * <p>When the target aspect ratio is set, the resolutions of the specified aspect ratio will be
 * selected in priority. If no resolution matches the aspect ratio, the resolution of the aspect
 * ratio which is closest to the specified target aspect ratio will be selected in priority.
 *
 * <p>When the target resolution is set, the target resolution attempts to establish a minimum
 * bound for the image resolution. If no resolution exists that is equal to or larger than the
 * target resolution, the nearest available resolution smaller than the target resolution will be
 * chosen. Resolutions with the same aspect ratio of the provided size will be considered in
 * higher priority before resolutions of different aspect ratios.
 *
 * <p>When the max resolution is set, the resolutions that either width or height exceed the
 * specified max resolution will be filtered out to be prevented from selecting.
 *
 * <p>When the high resolution support is enabled, the resolutions retrieved from
 * {@link android.hardware.camera2.params.StreamConfigurationMap#getHighResolutionOutputSizes(int)}
 * can be selected. This is typically used to take high resolution still images. Please note that
 * enabling high resolutions might cause the entire capture session to not meet the 20 fps frame
 * rate.
 *
 * <p>According to the camera device's hardware level and the bound use cases combination,
 * CameraX will select the best resolution for the use case by the all conditions. Applications
 * can know which resolution is finally selected to use by the use case's
 * <code>getResolutionInfo()</code> function. For more details see the guaranteed supported
 * configurations tables in {@link android.hardware.camera2.CameraDevice}'s
 * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a> section.
 *
 * <p>This {@link ResolutionSelector} determines the size of the {@link Surface} to get the camera
 * output for the use case. However, the actual output that the user sees will be adjusted by the
 * camera sensor orientation and the display orientation so that the user gets the upright image
 * properly. For example, when setting the target resolution size to <code>(1920, 1080)</code> in
 * a camera sensor that has sensor rotation 90 degrees and the device is in natural portrait
 * orientation ({@link Surface#ROTATION_0}), the output images will be transformed properly to be
 * displayed correctly in preview which has the aspect ratio of 9:16.
 *
 * <p>The existing setTargetResolution and setTargetAspectRatio APIs in
 * Preview/ImageCapture/ImageAnalysis's Builder are deprecated and are not compatible with
 * {@link ResolutionSelector}. Calling any of these APIs together with {@link ResolutionSelector}
 * will throw an {@link IllegalArgumentException}.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ResolutionSelector {

    private static final int ASPECT_RATIO_UNKNOWN = -1;
    private final int mTargetAspectRatio;
    @Nullable
    private final Size mTargetResolution;
    @Nullable
    private final Size mMaxResolution;
    private final boolean mIsHighResolutionEnabled;

    ResolutionSelector(int targetAspectRatio, @Nullable Size targetResolution,
            @Nullable Size maxResolution, boolean isHighResolutionEnabled) {
        mTargetAspectRatio = targetAspectRatio;
        mTargetResolution = targetResolution;
        mMaxResolution = maxResolution;
        mIsHighResolutionEnabled = isHighResolutionEnabled;
    }

    /**
     * Returns whether a target aspect ratio is set in the {@link ResolutionSelector}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean hasTargetAspectRatio() {
        return mTargetAspectRatio != ASPECT_RATIO_UNKNOWN;
    }

    /**
     * Retrieves the target aspect ratio setting in the {@link ResolutionSelector}.
     *
     * @throws IllegalArgumentException when no target aspect ratio is set.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @AspectRatio.Ratio
    public int getTargetAspectRatio() {
        if (mTargetAspectRatio == ASPECT_RATIO_UNKNOWN) {
            throw new IllegalArgumentException("No target aspect ratio is set!!");
        }

        return mTargetAspectRatio;
    }

    /**
     * Retrieves the target resolution setting in the {@link ResolutionSelector}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public Size getTargetResolution() {
        return mTargetResolution;
    }

    /**
     * Returns the max resolution setting in the {@link ResolutionSelector}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public Size getMaxResolution() {
        return mMaxResolution;
    }

    /**
     * Returns whether high resolutions are allowed to be selected.
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
        private int mTargetAspectRatio = ASPECT_RATIO_UNKNOWN;
        @Nullable
        private Size mTargetResolution = null;
        @Nullable
        private Size mMaxResolution = null;
        private boolean mIsHighResolutionEnabled = false;

        /**
         * Creates a new {@link Builder} object.
         */
        public Builder() {
        }

        private Builder(@NonNull ResolutionSelector selector) {
            if (selector.hasTargetAspectRatio()) {
                mTargetAspectRatio = selector.getTargetAspectRatio();
            }
            mTargetResolution = selector.getTargetResolution();
            mMaxResolution = selector.getMaxResolution();
            mIsHighResolutionEnabled = selector.isHighResolutionEnabled();
        }

        /**
         * Generates a {@link Builder} from another {@link ResolutionSelector} object.
         *
         * @param selector An existing {@link ResolutionSelector}.
         * @return The new {@link Builder}.
         */
        @NonNull
        public static Builder fromSelector(@NonNull ResolutionSelector selector) {
            return new Builder(selector);
        }

        /**
         * Sets the target aspect ratio that the output images are expected to have.
         *
         * <p>The input aspect ratio parameter being set in this {@link ResolutionSelector} is
         * used to find the surface size in the camera sensor's natural orientation (landscape)
         * from the supported resolution list.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case. Attempting so will throw an IllegalArgumentException when calling the
         * {@link #build()} function.
         *
         * <p>About the usage example, if the device has a <code>16:9</code> display and wants to
         * capture images matching the display aspect ratio, a {@link ResolutionSelector} created
         * with {@link AspectRatio.Ratio#RATIO_16_9} target aspect ratio setting can be used. If
         * no target aspect ratio and resolution is set for the use case,
         * {@link AspectRatio.Ratio#RATIO_4_3} target aspect ratio is set by default. Usually,
         * the camera sensor is in size of <code>4:3</code> aspect ratio and output images of
         * <code>4:3</code> aspect ratio will have the full FOV of the camera device.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints. Application
         * code should check the resulting output's resolution and the resulting aspect ratio may
         * not be exactly as requested.
         *
         * @param targetAspectRatio A {@link AspectRatio} representing the ratio of the target's
         *                          width and height.
         */
        @NonNull
        public Builder setTargetAspectRatio(@AspectRatio.Ratio int targetAspectRatio) {
            mTargetAspectRatio = targetAspectRatio;
            return this;
        }

        /**
         * Sets the target resolution that the output images are expected to have.
         *
         * <p>The input resolution parameter being set in this {@link ResolutionSelector} is used
         * to find the surface size in the camera sensor's natural orientation (landscape) from
         * the supported resolution list.
         *
         * <p>It is not allowed to set both target resolution and target aspect ratio on the same
         * use case. Attempting so will throw an IllegalArgumentException when calling the
         * {@link #build()} function.
         *
         * <p>About the usage example, if applications have a 1080p (1920x1080) display but only
         * need a <code>640x480</code> preview for some specific performance or design concern, a
         * {@link ResolutionSelector} created with <code>640x480</code> target resolution setting
         * can be used. If no target resolution and aspect ratio is set, each type of use case
         * has its own default value. {@link ImageCapture} will try to capture the largest image
         * as it can. {@link ImageAnalysis} will capture <code>640x480</code> size of images for
         * analyzing by default. {@link Preview} will select a resolution under the device's
         * screen resolution or 1080p (1920x1080), whichever is smaller.
         *
         * <p>The target resolution attempts to establish a minimum bound for the image
         * resolution. The actual image resolution will be the closest available resolution in
         * size that is not smaller than the target resolution, as determined by the Camera
         * implementation. However, if no resolution exists that is equal to or larger than the
         * target resolution, the nearest available resolution smaller than the target resolution
         * will be chosen. Resolutions with the same aspect ratio of the provided {@link Size}
         * will be considered in higher priority before resolutions of different aspect ratios.
         *
         * @param targetResolution The target resolution to choose from supported output sizes list.
         */
        @NonNull
        public Builder setTargetResolution(@NonNull Size targetResolution) {
            mTargetResolution = targetResolution;
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
         * <p>About the usage example, if applications want to select a resolution smaller than a
         * specific resolution to have better performance, a {@link ResolutionSelector} which
         * sets this specific resolution as the max resolution can be used. Or, if applications
         * want to select a larger resolution for a {@link Preview} which has the default max
         * resolution setting of the small one of device's screen size and 1080p (1920x1080), a
         * {@link ResolutionSelector} created with max resolution setting can also be used.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, which resolution
         * will be finally selected will depend on the camera device's hardware level and the
         * bound use cases combination. The device hardware level information can be retrieved by
         * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL}
         * from the interop class
         * {@link androidx.camera.camera2.interop.Camera2CameraInfo#getCameraCharacteristic(CameraCharacteristics.Key)}.
         * For more details see the
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a>
         * section in {@link android.hardware.camera2.CameraDevice}.
         *
         * @param resolution The max resolution limitation to choose from supported output sizes
         *                   list.
         * @return The current {@link Builder}.
         */
        @NonNull
        public Builder setMaxResolution(@NonNull Size resolution) {
            mMaxResolution = resolution;
            return this;
        }

        /**
         * Sets whether high resolutions are allowed to be selected for the use cases.
         *
         * <p> Calling this function will allow the use case to select the high resolution output
         * sizes if it is supported for the camera device.
         *
         * <p>When using the <code>camera-camera2</code> CameraX implementation, the supported
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
         * {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes(int)}, but
         * it is not guaranteed to meet >= 20 fps for any resolution in the list.
         *
         * <p>Which resolution will be finally selected will depend on the camera device's
         * hardware level and the bound use cases combination. The device hardware level
         * information can be retrieved by
         * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL}
         * from the interop class
         * {@link androidx.camera.camera2.interop.Camera2CameraInfo#getCameraCharacteristic(CameraCharacteristics.Key)}.
         * For more details see the
         * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture">Regular capture</a>
         * section in {@link android.hardware.camera2.CameraDevice}.
         *
         * @param enabled True to allow to select high resolution for the use case.
         * @return The current {@link Builder}.
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
         * @throws IllegalArgumentException when both target aspect and resolution are set at the
         * same time.
         */
        @NonNull
        public ResolutionSelector build() {
            if (mTargetAspectRatio != ASPECT_RATIO_UNKNOWN && mTargetResolution != null) {
                throw new IllegalArgumentException("Cannot use both setTargetResolution and "
                        + "setTargetAspectRatio at the same time.");
            }

            return new ResolutionSelector(mTargetAspectRatio, mTargetResolution, mMaxResolution,
                    mIsHighResolutionEnabled);
        }
    }
}
