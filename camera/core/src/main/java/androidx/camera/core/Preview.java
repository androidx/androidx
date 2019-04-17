/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageOutputConfig.RotationValue;

import com.google.auto.value.AutoValue;

import java.util.Map;
import java.util.Objects;

/**
 * A use case that provides a camera preview stream for a view finder.
 *
 * <p>The preview stream is connected to an underlying {@link SurfaceTexture}. The caller is still
 * responsible for deciding how this texture is shown.
 */
public class Preview extends UseCase {
    /**
     * Provides a static configuration with implementation-agnostic options.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final Defaults DEFAULT_CONFIG = new Defaults();
    private static final String TAG = "Preview";
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final CheckedSurfaceTexture.OnTextureChangedListener mSurfaceTextureListener =
            new CheckedSurfaceTexture.OnTextureChangedListener() {
                @Override
                public void onTextureChanged(SurfaceTexture newSurfaceTexture, Size newResolution) {
                    Preview.this.updateOutput(newSurfaceTexture, newResolution);
                }
            };
    final CheckedSurfaceTexture mCheckedSurfaceTexture =
            new CheckedSurfaceTexture(mSurfaceTextureListener);
    private final PreviewConfig.Builder mUseCaseConfigBuilder;
    @Nullable
    private OnPreviewOutputUpdateListener mSubscribedPreviewOutputListener;
    @Nullable
    private PreviewOutput mLatestPreviewOutput;
    private boolean mSurfaceDispatched = false;

    /**
     * Creates a new view finder use case from the given configuration.
     *
     * @param config for this use case instance
     */
    @MainThread
    public Preview(PreviewConfig config) {
        super(config);
        mUseCaseConfigBuilder = PreviewConfig.Builder.fromConfig(config);
    }

    private static SessionConfig.Builder createFrom(
            PreviewConfig config, DeferrableSurface surface) {
        SessionConfig.Builder sessionConfigBuilder = SessionConfig.Builder.createFrom(config);
        sessionConfigBuilder.addSurface(surface);
        return sessionConfigBuilder;
    }

    private static String getCameraIdUnchecked(LensFacing lensFacing) {
        try {
            return CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get camera id for camera lens facing " + lensFacing, e);
        }
    }

    /**
     * Removes previously PreviewOutput listener.
     *
     * <p>This is equivalent to calling {@code setOnPreviewOutputUpdateListener(null)}.
     */
    @UiThread
    public void removePreviewOutputListener() {
        setOnPreviewOutputUpdateListener(null);
    }

    /**
     * Gets {@link OnPreviewOutputUpdateListener}
     *
     * @return the last set listener or {@code null} if no listener is set
     */
    @UiThread
    @Nullable
    public OnPreviewOutputUpdateListener getOnPreviewOutputUpdateListener() {
        return mSubscribedPreviewOutputListener;
    }

    /**
     * Sets a listener to get the {@link PreviewOutput} updates.
     *
     * <p>Setting this listener will signal to the camera that the use case is ready to receive
     * data. Setting the listener to {@code null} will signal to the camera that the camera should
     * no longer stream data to the last {@link PreviewOutput}.
     *
     * <p>Once {@link OnPreviewOutputUpdateListener#onUpdated(PreviewOutput)} is called,
     * ownership of the {@link PreviewOutput} and its contents is transferred to the user. It is
     * the user's responsibility to release the last {@link SurfaceTexture} returned by {@link
     * PreviewOutput#getSurfaceTexture()} when a new SurfaceTexture is provided via an update or
     * when the user is finished with the use case.
     *
     * @param newListener The listener which will receive {@link PreviewOutput} updates.
     */
    @UiThread
    public void setOnPreviewOutputUpdateListener(
            @Nullable OnPreviewOutputUpdateListener newListener) {
        OnPreviewOutputUpdateListener oldListener = mSubscribedPreviewOutputListener;
        mSubscribedPreviewOutputListener = newListener;
        if (oldListener == null && newListener != null) {
            notifyActive();
            if (mLatestPreviewOutput != null) {
                mSurfaceDispatched = true;
                newListener.onUpdated(mLatestPreviewOutput);
            }
        } else if (oldListener != null && newListener == null) {
            notifyInactive();
        } else if (oldListener != null && oldListener != newListener) {
            if (mLatestPreviewOutput != null) {
                mCheckedSurfaceTexture.resetSurfaceTexture();
            }
        }
    }

    // TODO: Timeout may be exposed as a PreviewConfig(moved to CameraControl)

    private CameraControl getCurrentCameraControl() {
        PreviewConfig config = (PreviewConfig) getUseCaseConfig();
        String cameraId = getCameraIdUnchecked(config.getLensFacing());
        return getCameraControl(cameraId);
    }

    /**
     * Adjusts the view finder according to the properties in some local regions.
     *
     * <p>The auto-focus (AF) and auto-exposure (AE) properties will be recalculated from the local
     * regions.
     *
     * @param focus    rectangle with dimensions in sensor coordinate frame for focus
     * @param metering rectangle with dimensions in sensor coordinate frame for metering
     */
    public void focus(Rect focus, Rect metering) {
        focus(focus, metering, null);
    }

    /**
     * Adjusts the view finder according to the properties in some local regions with a callback
     * called once focus scan has completed.
     *
     * <p>The auto-focus (AF) and auto-exposure (AE) properties will be recalculated from the local
     * regions.
     *
     * @param focus    rectangle with dimensions in sensor coordinate frame for focus
     * @param metering rectangle with dimensions in sensor coordinate frame for metering
     * @param listener listener for when focus has completed
     */
    public void focus(Rect focus, Rect metering, @Nullable OnFocusListener listener) {
        getCurrentCameraControl().focus(focus, metering, listener, mMainHandler);
    }

    /**
     * Adjusts the view finder to zoom to a local region.
     *
     * @param crop rectangle with dimensions in sensor coordinate frame for zooming
     */
    public void zoom(Rect crop) {
        getCurrentCameraControl().setCropRegion(crop);
    }

    /**
     * Sets torch on/off.
     *
     * @param torch True if turn on torch, otherwise false
     */
    public void enableTorch(boolean torch) {
        getCurrentCameraControl().enableTorch(torch);
    }

    /** True if the torch is on */
    public boolean isTorchOn() {
        return getCurrentCameraControl().isTorchOn();
    }

    /**
     * Sets the rotation of the surface texture consumer.
     *
     * <p>In most cases this should be set to the current rotation returned by {@link
     * Display#getRotation()}. This will update the rotation value in {@link PreviewOutput} to
     * reflect the angle the PreviewOutput should be rotated to match the supplied rotation.
     *
     * @param rotation Rotation of the surface texture consumer.
     */
    public void setTargetRotation(@RotationValue int rotation) {
        ImageOutputConfig oldConfig = (ImageOutputConfig) getUseCaseConfig();
        int oldRotation = oldConfig.getTargetRotation(ImageOutputConfig.INVALID_ROTATION);
        if (oldRotation == ImageOutputConfig.INVALID_ROTATION || oldRotation != rotation) {
            mUseCaseConfigBuilder.setTargetRotation(rotation);
            updateUseCaseConfig(mUseCaseConfigBuilder.build());

            // TODO(b/122846516): Update session configuration and possibly reconfigure session.
            // For now we'll just attempt to update the rotation metadata.
            invalidateMetadata();
        }
    }

    @Override
    public String toString() {
        return TAG + ":" + getName();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(LensFacing lensFacing) {
        PreviewConfig defaults = CameraX.getDefaultUseCaseConfig(PreviewConfig.class, lensFacing);
        if (defaults != null) {
            return PreviewConfig.Builder.fromConfig(defaults);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void clear() {
        mCheckedSurfaceTexture.release();

        removePreviewOutputListener();
        notifyInactive();

        SurfaceTexture oldTexture =
                (mLatestPreviewOutput == null)
                        ? null
                        : mLatestPreviewOutput.getSurfaceTexture();
        if (oldTexture != null && !mSurfaceDispatched) {
            oldTexture.release();
        }

        super.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap) {
        PreviewConfig config = (PreviewConfig) getUseCaseConfig();
        String cameraId = getCameraIdUnchecked(config.getLensFacing());
        Size resolution = suggestedResolutionMap.get(cameraId);
        if (resolution == null) {
            throw new IllegalArgumentException(
                    "Suggested resolution map missing resolution for camera " + cameraId);
        }

        mCheckedSurfaceTexture.setResolution(resolution);
        mCheckedSurfaceTexture.resetSurfaceTexture();

        SessionConfig.Builder sessionConfigBuilder = createFrom(config, mCheckedSurfaceTexture);
        attachToCamera(cameraId, sessionConfigBuilder.build());

        return suggestedResolutionMap;
    }

    @UiThread
    private void invalidateMetadata() {
        if (mLatestPreviewOutput != null) {
            // Only update the output if we have a SurfaceTexture. Otherwise we'll wait until a
            // SurfaceTexture is ready.
            updateOutput(
                    mLatestPreviewOutput.getSurfaceTexture(),
                    mLatestPreviewOutput.getTextureSize());
        }
    }

    @UiThread
    void updateOutput(SurfaceTexture surfaceTexture, Size resolution) {
        PreviewConfig useCaseConfig = (PreviewConfig) getUseCaseConfig();

        int relativeRotation =
                (mLatestPreviewOutput == null) ? 0
                        : mLatestPreviewOutput.getRotationDegrees();
        try {
            // Attempt to get the camera ID. If this fails, we probably don't have permission, so we
            // will rely on the updated UseCaseConfig to set the correct rotation in
            // onSuggestedResolutionUpdated()
            String cameraId = CameraX.getCameraWithLensFacing(useCaseConfig.getLensFacing());
            CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
            relativeRotation =
                    cameraInfo.getSensorRotationDegrees(
                            useCaseConfig.getTargetRotation(Surface.ROTATION_0));
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to update output metadata: " + e);
        }

        PreviewOutput newOutput =
                PreviewOutput.create(surfaceTexture, resolution, relativeRotation);

        // Only update the output if something has changed
        if (!Objects.equals(mLatestPreviewOutput, newOutput)) {
            SurfaceTexture oldTexture =
                    (mLatestPreviewOutput == null)
                            ? null
                            : mLatestPreviewOutput.getSurfaceTexture();
            OnPreviewOutputUpdateListener outputListener = getOnPreviewOutputUpdateListener();

            mLatestPreviewOutput = newOutput;

            boolean textureChanged = oldTexture != surfaceTexture;
            if (textureChanged) {
                // If the old surface was never dispatched, we can safely release the old
                // SurfaceTexture.
                if (oldTexture != null && !mSurfaceDispatched) {
                    oldTexture.release();
                }

                // Keep track of whether this SurfaceTexture is dispatched
                mSurfaceDispatched = false;
            }

            if (outputListener != null) {
                // If we have a listener, then we should be active and we require a reset if the
                // SurfaceTexture changed.
                if (textureChanged) {
                    notifyReset();
                }

                mSurfaceDispatched = true;
                outputListener.onUpdated(newOutput);
            }
        }
    }

    /** Describes the error that occurred during preview operation. */
    public enum UseCaseError {
        /** Unknown error occurred. See message or log for more details. */
        UNKNOWN_ERROR
    }

    /** A listener of {@link PreviewOutput}. */
    public interface OnPreviewOutputUpdateListener {
        /** Callback when PreviewOutput has been updated. */
        void onUpdated(PreviewOutput output);
    }

    /**
     * Provides a base static default configuration for the Preview
     *
     * <p>These values may be overridden by the implementation. They only provide a minimum set of
     * defaults that are implementation independent.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class Defaults implements ConfigProvider<PreviewConfig> {
        private static final Handler DEFAULT_HANDLER = new Handler(Looper.getMainLooper());
        private static final Size DEFAULT_MAX_RESOLUTION =
                CameraX.getSurfaceManager().getPreviewSize();
        private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 2;

        private static final PreviewConfig DEFAULT_CONFIG;

        static {
            PreviewConfig.Builder builder =
                    new PreviewConfig.Builder()
                            .setCallbackHandler(DEFAULT_HANDLER)
                            .setMaxResolution(DEFAULT_MAX_RESOLUTION)
                            .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY);
            DEFAULT_CONFIG = builder.build();
        }

        @Override
        public PreviewConfig getConfig(LensFacing lensFacing) {
            return DEFAULT_CONFIG;
        }
    }

    /**
     * A bundle containing a {@link SurfaceTexture} and properties needed to display a Preview.
     */
    @AutoValue
    public abstract static class PreviewOutput {

        PreviewOutput() {
        }

        static PreviewOutput create(
                SurfaceTexture surfaceTexture, Size textureSize, int rotationDegrees) {
            return new AutoValue_Preview_PreviewOutput(
                    surfaceTexture, textureSize, rotationDegrees);
        }

        /** Returns the PreviewOutput that receives image data. */
        public abstract SurfaceTexture getSurfaceTexture();

        /** Returns the dimensions of the PreviewOutput. */
        public abstract Size getTextureSize();

        /**
         * Returns the rotation required, in degrees, to transform the PreviewOutput to match the
         * orientation given by ImageOutputConfig#getTargetRotation(int).
         *
         * <p>This number is independent of any rotation value that can be derived from the
         * PreviewOutput's {@link SurfaceTexture#getTransformMatrix(float[])}.
         */
        public abstract int getRotationDegrees();
    }
}
