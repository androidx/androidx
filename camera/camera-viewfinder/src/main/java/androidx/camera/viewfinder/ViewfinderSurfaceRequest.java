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

package androidx.camera.viewfinder;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import static androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.MIRROR_MODE_HORIZONTAL;
import static androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.MIRROR_MODE_NONE;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * The request to get a {@link Surface} to display camera feed.
 *
 * <p> This request contains requirements for the surface resolution and camera
 * device information from {@link CameraCharacteristics}.
 *
 * <p> Calling {@link CameraViewfinder#requestSurfaceAsync(ViewfinderSurfaceRequest)} with this
 * request will send the request to the surface provider, which is either a {@link TextureView} or
 * {@link SurfaceView} and get a {@link ListenableFuture} of {@link Surface}.
 *
 * <p> Calling {@link ViewfinderSurfaceRequest#markSurfaceSafeToRelease()} will notify the
 * surface provider that the surface is not needed and related resources can be released.
 *
 * @deprecated Use {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest} instead.
 */
@Deprecated
public class ViewfinderSurfaceRequest {

    private static final String TAG = "ViewfinderSurfaceRequest";

    @NonNull private androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
            mViewfinderSurfaceRequest;

    /**
     * Creates a new surface request with surface resolution, camera device, lens facing and
     * sensor orientation information.
     *
     * surfaceRequest The {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest}.
     */
    ViewfinderSurfaceRequest(
            @NonNull androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest surfaceRequest) {
        mViewfinderSurfaceRequest = surfaceRequest;
    }

    @Override
    @SuppressWarnings("GenericException") // super.finalize() throws Throwable
    protected void finalize() throws Throwable {
        // TODO(b/323226220): Differentiate between surface being released by consumer vs producer
        mViewfinderSurfaceRequest.markSurfaceSafeToRelease();
        super.finalize();
    }

    /**
     * Returns the resolution of the requested {@link Surface}.
     *
     * <p>The value is set by {@link Builder#Builder(Size)}.
     *
     * The surface which fulfills this request must have the resolution specified here in
     * order to fulfill the resource requirements of the camera.
     *
     * @return The guaranteed supported resolution.
     * @see SurfaceTexture#setDefaultBufferSize(int, int)
     */
    @NonNull
    public Size getResolution() {
        return mViewfinderSurfaceRequest.getResolution();
    }

    /**
     * Returns the sensor orientation.
     *
     * <p>The value is set by {@link Builder#setSensorOrientation(int)}, which can be retrieved from
     * {@link CameraCharacteristics} by key {@link CameraCharacteristics#SENSOR_ORIENTATION}.
     *
     * @return The sensor orientation.
     */
    @SensorOrientationDegreesValue
    public int getSensorOrientation() {
        return mViewfinderSurfaceRequest.getSourceOrientation();
    }

    /**
     * Returns the camera lens facing.
     *
     * <p>The value is set by {@link Builder#setLensFacing(int)}, which can be retrieved from
     * {@link CameraCharacteristics} by key {@link CameraCharacteristics#LENS_FACING}.
     *
     * @return The lens facing.
     */
    @LensFacingValue
    public int getLensFacing() {
        return mViewfinderSurfaceRequest.getOutputMirrorMode() == MIRROR_MODE_HORIZONTAL
                ? LENS_FACING_FRONT : LENS_FACING_BACK;
    }

    /**
     * Returns the {@link androidx.camera.viewfinder.CameraViewfinder.ImplementationMode}.
     *
     * <p>The value is set by {@link
     * Builder#setImplementationMode(androidx.camera.viewfinder.CameraViewfinder.ImplementationMode)
     * }.
     *
     * @return {@link androidx.camera.viewfinder.CameraViewfinder.ImplementationMode}.
     *         The value will be null if it's not set via
     * {@link Builder#setImplementationMode(
     * androidx.camera.viewfinder.CameraViewfinder.ImplementationMode)}.
     */
    @Nullable
    public androidx.camera.viewfinder.CameraViewfinder.ImplementationMode getImplementationMode() {
        return androidx.camera.viewfinder.CameraViewfinder.ImplementationMode.fromId(
                mViewfinderSurfaceRequest.getImplementationMode().getId());
    }

    @NonNull
    androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest getViewfinderSurfaceRequest() {
        return mViewfinderSurfaceRequest;
    }

    /**
     * Closes the viewfinder surface to mark it as safe to release.
     *
     * <p> This method should be called by the user when the requested surface is not needed and
     * related resources can be released.
     */
    public void markSurfaceSafeToRelease() {
        mViewfinderSurfaceRequest.markSurfaceSafeToRelease();
    }

    @SuppressLint("PairedRegistration")
    void addRequestCancellationListener(@NonNull Executor executor,
            @NonNull Runnable listener) {
        mViewfinderSurfaceRequest.addRequestCancellationListener(executor, listener);
    }

    /**
     * Builder for {@link ViewfinderSurfaceRequest}.
     *
     * @deprecated Use {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.Builder}
     * instead.
     */
    @Deprecated
    public static final class Builder {
        @NonNull
        private androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.Builder mBuilder;

        /**
         * Constructor for {@link Builder}.
         *
         * <p>Creates a builder with viewfinder resolution.
         *
         * @param resolution viewfinder resolution.
         */
        public Builder(@NonNull Size resolution) {
            mBuilder = new androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.Builder(
                    resolution);
        }

        /**
         * Constructor for {@link Builder}.
         *
         * <p>Creates a builder with other builder instance. The returned builder will be
         * pre-populated with the state of the provided builder.
         *
         * @param builder {@link Builder} instance.
         */
        public Builder(@NonNull Builder builder) {
            mBuilder = builder.mBuilder;
        }

        /**
         * Constructor for {@link Builder}.
         *
         * <p>Creates a builder with other {@link ViewfinderSurfaceRequest} instance. The
         * returned builder will be pre-populated with the state of the provided
         * {@link ViewfinderSurfaceRequest} instance.
         *
         * @param surfaceRequest {@link ViewfinderSurfaceRequest} instance.
         */
        public Builder(@NonNull ViewfinderSurfaceRequest surfaceRequest) {
            mBuilder = new androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.Builder(
                    surfaceRequest.getResolution());
            mBuilder.setSourceOrientation(surfaceRequest.getSensorOrientation());
            mBuilder.setOutputMirrorMode(surfaceRequest.getLensFacing() == LENS_FACING_FRONT
                    ? MIRROR_MODE_HORIZONTAL : MIRROR_MODE_NONE);
            mBuilder.setImplementationMode(
                    androidx.camera.viewfinder.surface.ImplementationMode.fromId(
                            surfaceRequest.getImplementationMode().getId()));
        }

        /**
         * Sets the {@link androidx.camera.viewfinder.CameraViewfinder.ImplementationMode}.
         *
         * <p><b>Possible values:</b></p>
         * <ul>
         *   <li>{@link
         *   androidx.camera.viewfinder.CameraViewfinder.ImplementationMode#PERFORMANCE PERFORMANCE}
         *   </li>
         *   <li>{@link
         *   androidx.camera.viewfinder.CameraViewfinder.ImplementationMode#COMPATIBLE COMPATIBLE}
         *   </li>
         * </ul>
         *
         * <p>If not set or setting to null, the
         * {@link androidx.camera.viewfinder.CameraViewfinder.ImplementationMode} set via {@code app
         * :implementationMode} in layout xml will be used for {@link CameraViewfinder}. If not
         * set in the layout xml, the default value
         * {@link androidx.camera.viewfinder.CameraViewfinder.ImplementationMode#PERFORMANCE} will
         * be used in {@link CameraViewfinder}.
         *
         * @param implementationMode The {@link
         * androidx.camera.viewfinder.CameraViewfinder.ImplementationMode}.
         * @return This builder.
         */
        @NonNull
        public Builder setImplementationMode(
                @Nullable
                androidx.camera.viewfinder.CameraViewfinder.ImplementationMode implementationMode) {
            mBuilder.setImplementationMode(
                    androidx.camera.viewfinder.surface.ImplementationMode.fromId(
                            implementationMode.getId()));
            return this;
        }

        /**
         * Sets the lens facing.
         *
         * <p><b>Possible values:</b></p>
         * <ul>
         *   <li>{@link CameraMetadata#LENS_FACING_FRONT FRONT}</li>
         *   <li>{@link CameraMetadata#LENS_FACING_BACK BACK}</li>
         *   <li>{@link CameraMetadata#LENS_FACING_EXTERNAL EXTERNAL}</li>
         * </ul>
         *
         * <p>The value can be retrieved from {@link CameraCharacteristics} by key
         * {@link CameraCharacteristics#LENS_FACING}. If not set,
         * {@link CameraMetadata#LENS_FACING_BACK} will be used by default.
         *
         * @param lensFacing The lens facing.
         * @return This builder.
         */
        @NonNull
        public Builder setLensFacing(@LensFacingValue int lensFacing) {
            mBuilder.setOutputMirrorMode(lensFacing == LENS_FACING_FRONT ? MIRROR_MODE_HORIZONTAL :
                    MIRROR_MODE_NONE);
            return this;
        }

        /**
         * Sets the sensor orientation.
         *
         * <p><b>Range of valid values:</b><br>
         * 0, 90, 180, 270</p>
         *
         * <p>The value can be retrieved from {@link CameraCharacteristics} by key
         * {@link CameraCharacteristics#SENSOR_ORIENTATION}. If it is not
         * set, 0 will be used by default.
         *
         * @param sensorOrientation The camera sensor orientation.
         * @return this builder.
         */
        @NonNull
        public Builder setSensorOrientation(@SensorOrientationDegreesValue int sensorOrientation) {
            mBuilder.setSourceOrientation(sensorOrientation);
            return this;
        }

        /**
         * Builds the {@link ViewfinderSurfaceRequest}.
         * @return the instance of {@link ViewfinderSurfaceRequest}.
         */
        @NonNull
        public ViewfinderSurfaceRequest build() {
            return new ViewfinderSurfaceRequest(mBuilder.build());
        }
    }

    /**
     * Valid integer sensor orientation degrees values.
     */
    @IntDef({0, 90, 180, 270})
    @Retention(RetentionPolicy.SOURCE)
    @interface SensorOrientationDegreesValue {
    }

    /**
     * Valid integer sensor orientation degrees values.
     */
    @IntDef({LENS_FACING_FRONT, LENS_FACING_BACK, LENS_FACING_EXTERNAL})
    @Retention(RetentionPolicy.SOURCE)
    @interface LensFacingValue {
    }
}
