/*
 * Copyright 2019 The Android Open Source Project
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

import android.graphics.ImageFormat;
import android.media.MediaActionSound;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.camera.core.impl.DynamicRanges;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.internal.compat.MediaActionSoundCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

/**
 * An interface for retrieving camera information.
 *
 * <p>Applications can retrieve an instance via {@link Camera#getCameraInfo()}.
 */
public interface CameraInfo {

    /**
     * An unknown intrinsic zoom ratio. Usually to indicate the camera is unable to provide
     * necessary information to resolve its intrinsic zoom ratio.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    float INTRINSIC_ZOOM_RATIO_UNKNOWN = 1.0F;

    /**
     * An unknown camera implementation type.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    String IMPLEMENTATION_TYPE_UNKNOWN = "<unknown>";

    /**
     * A Camera2 API implementation type where the camera support level is
     * {@link android.hardware.camera2.CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
     * LIMITED},
     * {@link android.hardware.camera2.CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_FULL FULL},
     * {@link android.hardware.camera2.CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_3 LEVEL_3} or
     * {@link android.hardware.camera2.CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
     * EXTRERNAL}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    String IMPLEMENTATION_TYPE_CAMERA2 = "androidx.camera.camera2";

    /**
     * A Camera2 API implementation type where the camera support level is
     * {@link android.hardware.camera2.CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY LEGACY}.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    String IMPLEMENTATION_TYPE_CAMERA2_LEGACY = IMPLEMENTATION_TYPE_CAMERA2 + ".legacy";

    /**
     * A fake camera implementation type.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    String IMPLEMENTATION_TYPE_FAKE = "androidx.camera.fake";

    /**
     * Returns whether the shutter sound must be played in accordance to regional restrictions.
     *
     * <p>This method provides the general rule of playing shutter sounds. The exact
     * requirements of playing shutter sounds may vary among regions.
     *
     * <p>For image capture, the shutter sound is recommended to be played when receiving
     * {@link ImageCapture.OnImageCapturedCallback#onCaptureStarted()} or
     * {@link ImageCapture.OnImageSavedCallback#onCaptureStarted()}. For video capture, it's
     * recommended to play the start recording sound when receiving
     * {@code VideoRecordEvent.Start} and the stop recording sound when receiving
     * {@code VideoRecordEvent.Finalize}.
     *
     * <p>To play the system default sounds, it's recommended to use
     * {@link MediaActionSound#play(int)}. For image capture, play
     * {@link MediaActionSound#SHUTTER_CLICK}. For video capture, play
     * {@link MediaActionSound#START_VIDEO_RECORDING} and
     * {@link MediaActionSound#STOP_VIDEO_RECORDING}.
     *
     * <p>This method and {@link MediaActionSound#mustPlayShutterSound()} serve the same purpose,
     * while this method is compatible on API level lower than
     * {@link android.os.Build.VERSION_CODES#TIRAMISU}.
     *
     * @return {@code true} if shutter sound must be played, otherwise {@code false}.
     */
    static boolean mustPlayShutterSound() {
        return MediaActionSoundCompat.mustPlayShutterSound();
    }

    /**
     * Returns the sensor rotation in degrees, relative to the device's "natural" (default)
     * orientation.
     *
     * <p>See <a href="https://developer.android.com/guide/topics/sensors/sensors_overview#sensors-coords">Sensor Coordinate System</a>
     * for more information.
     *
     * @return the sensor rotation in degrees, relative to device's "natural" (default) orientation.
     */
    int getSensorRotationDegrees();

    /**
     * Returns the sensor rotation, in degrees, relative to the given rotation value.
     *
     * <p>Valid values for the relative rotation are {@link Surface#ROTATION_0} (natural), {@link
     * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     *
     * @param relativeRotation the rotation relative to which the output will be calculated.
     * @return the sensor rotation in degrees.
     */
    int getSensorRotationDegrees(@ImageOutputConfig.RotationValue int relativeRotation);

    /** Returns if flash unit is available or not. */
    boolean hasFlashUnit();

    /**
     * Returns a {@link LiveData} of current {@link TorchState}.
     *
     * <p>The torch can be turned on and off via {@link CameraControl#enableTorch(boolean)} which
     * will trigger the change event to the returned {@link LiveData}. Apps can either get
     * immediate value via {@link LiveData#getValue()} or observe it via
     * {@link LiveData#observe(LifecycleOwner, Observer)} to update torch UI accordingly.
     *
     * <p>If the camera doesn't have a flash unit (see {@link #hasFlashUnit()}), then the torch
     * state will be {@link TorchState#OFF}.
     *
     * @return a {@link LiveData} containing current torch state.
     */
    @NonNull
    LiveData<Integer> getTorchState();

    /**
     * Returns a {@link LiveData} of {@link ZoomState}.
     *
     * <p>The LiveData will be updated whenever the set zoom state has been changed. This can
     * occur when the application updates the zoom via {@link CameraControl#setZoomRatio(float)}
     * or {@link CameraControl#setLinearZoom(float)}. The zoom state can also change anytime a
     * camera starts up, for example when a {@link UseCase} is bound to it.
     */
    @NonNull
    LiveData<ZoomState> getZoomState();

    /**
     * Returns a {@link ExposureState}.
     *
     * <p>The {@link ExposureState} contains the current exposure related information.
     */
    @NonNull
    ExposureState getExposureState();

    /**
     * Returns a {@link LiveData} of the camera's state.
     *
     * <p>The {@link LiveData} will be updated whenever the {@linkplain CameraState camera's
     * state} changes, and can be any of the following: {@link CameraState.Type#PENDING_OPEN},
     * {@link CameraState.Type#OPENING}, {@link CameraState.Type#OPEN},
     * {@link CameraState.Type#CLOSING} and {@link CameraState.Type#CLOSED}.
     *
     * <p>Due to the inner workings of {@link LiveData}, some reported camera states may be
     * ignored if a newer value is posted before the observers are updated. For instance, this can
     * occur when the camera is opening or closing, the {@link CameraState.Type#OPENING} and
     * {@link CameraState.Type#CLOSING} states may not be reported to observers if they are rapidly
     * followed by the {@link CameraState.Type#OPEN} and {@link CameraState.Type#CLOSED} states
     * respectively.
     *
     * @return a {@link LiveData} of the camera's state.
     */
    @NonNull
    LiveData<CameraState> getCameraState();

    /**
     * Returns the implementation type of the camera, this depends on the {@link CameraXConfig}
     * used in the initialization of CameraX.
     *
     * @return the implementation type of the camera, which can be one of the following:
     * {@link #IMPLEMENTATION_TYPE_UNKNOWN}, {@link #IMPLEMENTATION_TYPE_CAMERA2_LEGACY},
     * {@link #IMPLEMENTATION_TYPE_CAMERA2}, {@link #IMPLEMENTATION_TYPE_FAKE}.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ImplementationType
    String getImplementationType();

    /**
     * Returns a {@link CameraSelector} unique to this camera.
     *
     * @return {@link CameraSelector} unique to this camera.
     */
    @NonNull
    CameraSelector getCameraSelector();

    /**
     * Returns the lens facing of this camera.
     *
     * @return one of {@link CameraSelector#LENS_FACING_FRONT} and
     * {@link CameraSelector#LENS_FACING_BACK}, or {@link CameraSelector#LENS_FACING_EXTERNAL}.
     * If the lens facing of the camera can not be resolved, return
     * {@link CameraSelector#LENS_FACING_UNKNOWN}.
     *
     * @throws IllegalArgumentException If the device cannot return a valid lens facing value,
     *                                  it will throw this exception.
     */
    @CameraSelector.LensFacing
    default int getLensFacing() {
        return CameraSelector.LENS_FACING_UNKNOWN;
    }

    /**
     * Returns the intrinsic zoom ratio of this camera.
     *
     * <p>The intrinsic zoom ratio is defined as the ratio between the angle of view of
     * the default camera and this camera. The default camera is the camera selected by
     * {@link CameraSelector#DEFAULT_FRONT_CAMERA} or {@link CameraSelector#DEFAULT_BACK_CAMERA}
     * depending on the lens facing of this camera. For example, if the default camera has angle of
     * view 60 degrees and this camera has 30 degrees, this camera will have intrinsic zoom ratio
     * {@code 2.0}.
     *
     * <p>The intrinsic zoom ratio is calculated approximately based on the focal length and the
     * sensor size. It's considered an inexact attribute of the camera and might not be hundred
     * percent accurate when compared with the output image. Especially for the case that the
     * camera doesn't read the whole sensor area due to cropping being applied.
     *
     * <p>The default camera is guaranteed to have intrinsic zoom ratio {@code 1.0}. Other cameras
     * that have intrinsic zoom ratio greater than {@code 1.0} are considered telephoto cameras and
     * cameras that have intrinsic zoom ratio less than {@code 1.0} are considered ultra
     * wide-angle cameras.
     *
     * <p>If the camera is unable to provide necessary information to resolve its intrinsic zoom
     * ratio, it will be considered as a standard camera which has intrinsic zoom ratio {@code 1.0}.
     *
     * @return the intrinsic zoom ratio of this camera.
     */
    @FloatRange(from = 0, fromInclusive = false)
    default float getIntrinsicZoomRatio() {
        return INTRINSIC_ZOOM_RATIO_UNKNOWN;
    }

    /**
     * Returns if the given {@link FocusMeteringAction} is supported on the devices.
     *
     * <p>It returns true if at least one valid AF/AE/AWB region generated by the given
     * {@link FocusMeteringAction} is supported on the current camera. For example, on a camera
     * supporting only AF regions, passing in a {@link FocusMeteringAction} specifying AF/AE regions
     * to this API will still return true. But it will return false if the
     * {@link FocusMeteringAction} specifies only the AE region since none of the specified
     * regions are supported.
     *
     * <p>If it returns false, invoking
     * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} with the given
     * {@link FocusMeteringAction} will always fail.
     */
    default boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        return false;
    }

    /**
     * Returns if {@link ImageCapture#CAPTURE_MODE_ZERO_SHUTTER_LAG} is supported on the current
     * device.
     *
     * <p>ZERO_SHUTTER_LAG will be supported when all of the following conditions are met
     * <ul>
     *     <li> API Level >= 23
     *     <li> {@link ImageFormat#PRIVATE} reprocessing is supported
     * </ul>
     *
     * @return true if supported, otherwise false.
     */
    @ExperimentalZeroShutterLag
    default boolean isZslSupported() {
        return false;
    }

    /**
     * Returns an unordered set of the frame rate ranges, in frames per second, supported by this
     * device's AE algorithm.
     *
     * <p>These are the frame rate ranges that the AE algorithm on the device can support. When
     * CameraX is configured to run with the camera2 implementation, this list will be derived
     * from {@link android.hardware.camera2.CameraCharacteristics
     * #CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES}, though ranges may be added or removed for
     * compatibility reasons.
     *
     * <p>There is no guarantee that these ranges can be used for every size surface or
     * combination of use cases. If attempting to run the device using an unsupported range, there
     * may be stability issues or the device may quietly choose another frame rate operating range.
     *
     * <p>The returned set does not have any ordering guarantees and frame rate ranges may overlap.
     *
     * @return The set of FPS ranges supported by the device's AE algorithm
     * @see androidx.camera.video.VideoCapture.Builder#setTargetFrameRate(Range)
     */
    @NonNull
    default Set<Range<Integer>> getSupportedFrameRateRanges() {
        return Collections.emptySet();
    }

    /**
     * Returns if logical multi camera is supported on the device.
     *
     * <p>A logical camera is a grouping of two or more of those physical cameras.
     * See <a href="https://developer.android.com/media/camera/camera2/multi-camera">Multi-camera API</a>
     *
     * @return true if supported, otherwise false.
     * @see android.hardware.camera2.CameraMetadata
     * #REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
     */
    default boolean isLogicalMultiCameraSupported() {
        return false;
    }

    /**
     * Returns if {@link ImageFormat#PRIVATE} reprocessing is supported on the device.
     *
     * @return true if supported, otherwise false.
     * @see android.hardware.camera2.CameraMetadata
     * #REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    default boolean isPrivateReprocessingSupported() {
        return false;
    }

    /**
     * Returns the supported dynamic ranges of this camera from a set of candidate dynamic ranges.
     *
     * <p>Dynamic range specifies how the range of colors, highlights and shadows captured by
     * the frame producer are represented on a display. Some dynamic ranges allow the preview
     * surface to make full use of the extended range of brightness of the display.
     *
     * <p>The returned dynamic ranges are those which the camera can produce. However, because
     * care usually needs to be taken to ensure the frames produced can be displayed correctly,
     * the returned dynamic ranges will be limited to those passed in to {@code
     * candidateDynamicRanges}. For example, if the device display supports HLG, HDR10 and
     * HDR10+, and you're attempting to use a UI component to receive frames from those dynamic
     * ranges that you know will be display correctly, you would use a {@code
     * candidateDynamicRanges} set consisting of {@code {DynamicRange.HLG_10_BIT,
     * DynamicRange.HDR10_10_BIT, DynamicRange.HDR10_PLUS_10_BIT}}. If the only 10-bit/HDR {@code
     * DynamicRange} the camera can produce is {@code HLG_10_BIT}, then that will be the only
     * dynamic range returned by this method given the above candidate list.
     *
     * <p>Consult the documentation of each use case to determine whether using the dynamic ranges
     * published here are appropriate. Some use cases may have complex requirements that prohibit
     * them from publishing a candidate list for use with this method, such as
     * {@link androidx.camera.video.Recorder Recorder}. For those cases, alternative APIs may be
     * present for querying the supported dynamic ranges that can be set on the use case.
     *
     * <p>The dynamic ranges published as return values by this method are fully-defined. That is,
     * the resulting set will not contain dynamic ranges such as {@link DynamicRange#UNSPECIFIED} or
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}. However, non-fully-defined dynamic ranges can
     * be used in {@code candidateDynamicRanges}, and will resolve to fully-defined dynamic ranges
     * in the resulting set. To query all dynamic ranges the camera can produce, {@code
     * Collections.singleton(DynamicRange.UNSPECIFIED}} can be used as the candidate set.
     *
     * <p>Because SDR is always supported, including {@link DynamicRange#SDR} in {@code
     * candidateDynamicRanges} will always result in {@code SDR} being present in the result set.
     * If an empty candidate set is provided, an {@link IllegalArgumentException} will be thrown.
     *
     * @param candidateDynamicRanges a set of dynamic ranges representing the dynamic ranges the
     *                               consumer of frames can support. Note that each use case may
     *                               have its own requirements on which dynamic ranges it can
     *                               consume based on how it is configured, and those dynamic
     *                               ranges may not be published as a set of candidate dynamic
     *                               ranges. In that case, this API may not be appropriate. An
     *                               example of this is
     *                               {@link androidx.camera.video.VideoCapture VideoCapture}'s
     *                               {@link androidx.camera.video.Recorder Recorder} class, which
     *                               must also take into account the dynamic ranges supported by
     *                               the media codecs on the device, and the quality of the video
     *                               being recorded. For that class, it is recommended to use
     *            {@link androidx.camera.video.RecorderVideoCapabilities#getSupportedDynamicRanges()
     *                               RecorderVideoCapabilities.getSupportedDynamicRanges()}
     *                               instead.
     * @return a set of dynamic ranges supported by the camera based on the candidate dynamic ranges
     * @throws IllegalArgumentException if an empty candidate dynamic range set is provided.
     *
     * @see Preview.Builder#setDynamicRange(DynamicRange)
     * @see androidx.camera.video.RecorderVideoCapabilities#getSupportedDynamicRanges()
     */
    @NonNull
    default Set<DynamicRange> querySupportedDynamicRanges(
            @NonNull Set<DynamicRange> candidateDynamicRanges) {
        // For the default implementation, only assume SDR is supported.
        return DynamicRanges.findAllPossibleMatches(candidateDynamicRanges,
                Collections.singleton(DynamicRange.SDR));
    }

    /**
     * Returns a set of physical camera {@link CameraInfo}s.
     *
     * <p>A logical camera is a grouping of two or more of those physical cameras.
     * See <a href="https://developer.android.com/media/camera/camera2/multi-camera">Multi-camera API</a>
     *
     * <p> Check {@link #isLogicalMultiCameraSupported()} to see if the device is supporting
     * physical camera or not. If the device doesn't support physical camera, empty set will
     * be returned.
     *
     * @return Set of physical camera {@link CameraInfo}s.
     * @see #isLogicalMultiCameraSupported()
     */
    @NonNull
    default Set<CameraInfo> getPhysicalCameraInfos() {
        return Collections.emptySet();
    }

    @StringDef(open = true, value = {IMPLEMENTATION_TYPE_UNKNOWN,
            IMPLEMENTATION_TYPE_CAMERA2_LEGACY, IMPLEMENTATION_TYPE_CAMERA2,
            IMPLEMENTATION_TYPE_FAKE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    @interface ImplementationType {
    }
}
