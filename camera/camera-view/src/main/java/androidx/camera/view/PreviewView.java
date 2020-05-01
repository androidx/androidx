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

package androidx.camera.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.Logger;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.view.preview.transform.PreviewTransform;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom View that displays the camera feed for CameraX's Preview use case.
 *
 * <p> This class manages the Surface lifecycle, as well as the preview aspect ratio and
 * orientation. Internally, it uses either a {@link TextureView} or {@link SurfaceView} to
 * display the camera feed.
 *
 * <p> If {@link PreviewView} uses a {@link SurfaceView} to display the preview
 * stream, be careful when overlapping a {@link View} that's initially not visible (either
 * {@link View#INVISIBLE} or {@link View#GONE}) on top of it. When the
 * {@link SurfaceView} is attached to the display window, it calls
 * {@link android.view.ViewParent#requestTransparentRegion(View)} which requests a computation of
 * the transparent regions on the display. At this point, the {@link View} isn't visible, causing
 * the overlapped region between the {@link SurfaceView} and the {@link View} to be
 * considered transparent. Later if the {@link View} becomes {@linkplain View#VISIBLE visible}, it
 * will not be displayed on top of {@link SurfaceView}. A way around this is to call
 * {@link android.view.ViewParent#requestTransparentRegion(View)} right after making the
 * {@link View} visible, or initially hiding the {@link View} by setting its
 * {@linkplain View#setAlpha(float) opacity} to 0, then setting it to 1.0F to show it.
 */
public class PreviewView extends FrameLayout {

    private static final String TAG = "PreviewView";

    @ColorRes
    static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.PERFORMANCE;

    @NonNull
    private ImplementationMode mImplementationMode = DEFAULT_IMPL_MODE;

    @VisibleForTesting
    @Nullable
    PreviewViewImplementation mImplementation;

    @NonNull
    PreviewTransform mPreviewTransform = new PreviewTransform();

    @NonNull
    private MutableLiveData<StreamState> mPreviewStreamStateLiveData =
            new MutableLiveData<>(StreamState.IDLE);

    @Nullable
    private AtomicReference<PreviewStreamStateObserver> mActiveStreamStateObserver =
            new AtomicReference<>();
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    CameraController mCameraController;

    @NonNull
    PreviewViewMeteringPointFactory mPreviewViewMeteringPointFactory =
            new PreviewViewMeteringPointFactory();

    private final OnLayoutChangeListener mOnLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            if (mImplementation != null) {
                mImplementation.redrawPreview();
            }

            mPreviewViewMeteringPointFactory.setViewSize(right - left, top - bottom);

            boolean isSizeChanged =
                    right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;
            if (mCameraController != null && isSizeChanged) {
                mCameraController.attachPreviewSurface(createSurfaceProvider(), getWidth(),
                        getHeight());
            }
        }
    };

    public PreviewView(@NonNull Context context) {
        this(context, null);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.PreviewView, defStyleAttr, defStyleRes);
        if (Build.VERSION.SDK_INT >= 29) {
            saveAttributeDataForStyleable(context, R.styleable.PreviewView, attrs, attributes,
                    defStyleAttr, defStyleRes);
        }

        try {
            final int scaleTypeId = attributes.getInteger(
                    R.styleable.PreviewView_scaleType,
                    mPreviewTransform.getScaleType().getId());
            setScaleType(ScaleType.fromId(scaleTypeId));
        } finally {
            attributes.recycle();
        }

        // Set background only if it wasn't already set. A default background prevents the content
        // behind the PreviewView from being visible before the preview starts streaming.
        if (getBackground() == null) {
            setBackgroundColor(ContextCompat.getColor(getContext(), DEFAULT_BACKGROUND_COLOR));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onAttachedToWindow();
        }
        mPreviewViewMeteringPointFactory.setDisplay(getDisplay());
        if (mCameraController != null) {
            mCameraController.attachPreviewSurface(createSurfaceProvider(), getWidth(),
                    getHeight());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onDetachedFromWindow();
        }
        mPreviewViewMeteringPointFactory.setDisplay(getDisplay());
        if (mCameraController != null) {
            mCameraController.clearPreviewSurface();
        }
    }

    /**
     * Sets the {@link ImplementationMode} for the {@link PreviewView}.
     *
     * <p> {@link PreviewView} displays the preview with either a {@link SurfaceView} or a
     * {@link TextureView} depending on the mode. If not set, the default value is
     * {@link ImplementationMode#PERFORMANCE}.
     *
     * @see ImplementationMode
     */
    public void setImplementationMode(@NonNull final ImplementationMode implementationMode) {
        mImplementationMode = implementationMode;
    }

    /**
     * Returns the {@link ImplementationMode}.
     *
     * <p> If nothing is set via {@link #setImplementationMode}, the default
     * value is {@link ImplementationMode#PERFORMANCE}.
     *
     * @return The {@link ImplementationMode} for {@link PreviewView}.
     */
    @NonNull
    public ImplementationMode getImplementationMode() {
        return mImplementationMode;
    }

    /**
     * Gets the {@link Preview.SurfaceProvider} to be used with
     * {@link Preview#setSurfaceProvider(Executor, Preview.SurfaceProvider)}.
     *
     * <p> The returned {@link Preview.SurfaceProvider} will provide a preview {@link Surface} to
     * the camera that's either managed by a {@link TextureView} or {@link SurfaceView} depending
     * on the {@link ImplementationMode}.
     *
     * @see ImplementationMode
     * @return A {@link Preview.SurfaceProvider} used to start the camera preview.
     */
    @NonNull
    @UiThread
    public Preview.SurfaceProvider createSurfaceProvider() {
        Threads.checkMainThread();
        return surfaceRequest -> {
            Logger.d(TAG, "Surface requested by Preview.");
            CameraInternal camera = (CameraInternal) surfaceRequest.getCamera();
            mPreviewTransform.setSensorDimensionFlipNeeded(
                    isSensorDimensionFlipNeeded(camera.getCameraInfo()));
            mImplementation = shouldUseTextureView(camera.getCameraInfo(), mImplementationMode)
                    ? new TextureViewImplementation() : new SurfaceViewImplementation();
            mImplementation.init(this, mPreviewTransform);

            PreviewStreamStateObserver streamStateObserver =
                    new PreviewStreamStateObserver((CameraInfoInternal) camera.getCameraInfo(),
                            mPreviewStreamStateLiveData, mImplementation);
            mActiveStreamStateObserver.set(streamStateObserver);

            camera.getCameraState().addObserver(
                    ContextCompat.getMainExecutor(getContext()), streamStateObserver);

            mPreviewViewMeteringPointFactory.setViewImplementationResolution(
                    mImplementation.getResolution());
            mPreviewViewMeteringPointFactory.setCameraInfo(camera.getCameraInfo());

            mImplementation.onSurfaceRequested(surfaceRequest, () -> {
                // We've no longer needed this observer, if there is no new StreamStateObserver
                // (another SurfaceRequest), reset the streamState to IDLE.
                // This is needed for the case when unbinding preview while other use cases are
                // still bound.
                if (mActiveStreamStateObserver.compareAndSet(streamStateObserver, null)) {
                    streamStateObserver.updatePreviewStreamState(StreamState.IDLE);
                }
                streamStateObserver.clear();
                camera.getCameraState().removeObserver(streamStateObserver);
            });
        };
    }

    /**
     * Applies a {@link ScaleType} to the preview.
     * <p>
     * Note that the {@link ScaleType#FILL_CENTER} is applied to the preview by default.
     *
     * @param scaleType A {@link ScaleType} to apply to the preview.
     */
    public void setScaleType(@NonNull final ScaleType scaleType) {
        mPreviewTransform.setScaleType(scaleType);
        mPreviewViewMeteringPointFactory.setScaleType(scaleType);
        if (mImplementation != null) {
            mImplementation.redrawPreview();
        }
    }

    /**
     * Returns the device rotation value currently applied to the preview.
     *
     * @return The device rotation value currently applied to the preview.
     */
    public int getDeviceRotationForRemoteDisplayMode() {
        return mPreviewTransform.getDeviceRotation();
    }

    /**
     * Provides the device rotation value to the preview in remote display mode.
     *
     * <p>The device rotation value will only take effect when detecting current view is
     * on a remote display. If current view is on the device builtin display, {@link PreviewView}
     * will directly use view's rotation value to do the transformation related calculations.
     *
     * <p>The preview transform calculations have strong dependence on the device rotation value.
     * When a application is running in remote display, the rotation value obtained from current
     * view will cause incorrect transform calculation results. To make the preview output result
     * correct in remote display mode, the developers need to provide the device rotation value
     * obtained from {@link android.view.OrientationEventListener}.
     *
     * <p>The mapping between the device rotation value and the orientation value obtained from
     * {@link android.view.OrientationEventListener} are listed as the following.
     * <p>{@link android.view.OrientationEventListener#ORIENTATION_UNKNOWN}: orientation == -1
     * <p>{@link Surface#ROTATION_0}: orientation >= 315 || orientation < 45
     * <p>{@link Surface#ROTATION_90}: orientation >= 225 && orientation < 315
     * <p>{@link Surface#ROTATION_180}: orientation >= 135 && orientation < 225
     * <p>{@link Surface#ROTATION_270}: orientation >= 45 && orientation < 135
     *
     * @param deviceRotation The device rotation value, expressed as one of
     *                       {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                       {@link Surface#ROTATION_180}, or
     *                       {@link Surface#ROTATION_270}.
     */
    public void setDeviceRotationForRemoteDisplayMode(final int deviceRotation) {
        // This only take effect when it is remote display mode.
        if (deviceRotation == mPreviewTransform.getDeviceRotation()
                || !isRemoteDisplayMode()) {
            return;
        }

        mPreviewTransform.setDeviceRotation(deviceRotation);
        if (mImplementation != null) {
            mImplementation.redrawPreview();
        }
    }

    /**
     * Returns the {@link ScaleType} currently applied to the preview.
     * <p>
     * By default, {@link ScaleType#FILL_CENTER} is applied to the preview.
     *
     * @return The {@link ScaleType} currently applied to the preview.
     */
    @NonNull
    public ScaleType getScaleType() {
        return mPreviewTransform.getScaleType();
    }

    /**
     * Gets the {@link MeteringPointFactory} for the Camera currently connected to the PreviewView.
     *
     * <p>The returned {@link MeteringPointFactory} is capable of creating {@link MeteringPoint}s
     * from (x, y) coordinates in the {@link PreviewView}. This conversion takes into account its
     * {@link ScaleType}.
     *
     * <p>When the PreviewView has a width and/or height equal to zero, or when a preview
     * {@link Surface} is not yet requested, the returned factory will always create invalid
     * {@link MeteringPoint}s which could lead to the failure of
     * {@link androidx.camera.core.CameraControl#startFocusAndMetering(FocusMeteringAction)} but it
     * won't cause any crash.
     *
     * @return a {@link MeteringPointFactory}
     */
    @NonNull
    public MeteringPointFactory getMeteringPointFactory() {
        return mPreviewViewMeteringPointFactory;
    }

    /**
     * Gets the {@link LiveData} of current preview {@link StreamState}.
     *
     * <p>There are two states, {@link StreamState#IDLE} and {@link StreamState#STREAMING}.
     * {@link StreamState#IDLE} represents the preview is currently not visible and streaming is
     * stopped. {@link StreamState#STREAMING} means the preview is streaming.
     *
     * <p>When {@link PreviewView} is in a {@link StreamState#STREAMING} state, it guarantees
     * preview is visible only when implementationMode is {@link ImplementationMode#COMPATIBLE}.
     * When in {@link ImplementationMode#PERFORMANCE} mode, it is possible that preview becomes
     * visible slightly after state changes to {@link StreamState#STREAMING}. For apps
     * relying on the preview visible signal to be working correctly, please set
     * {@link ImplementationMode#COMPATIBLE} mode in
     * {@link #setImplementationMode}.
     *
     * @return A {@link LiveData} containing the {@link StreamState}. Apps can either get current
     * value by {@link LiveData#getValue()} or register a observer by {@link LiveData#observe} .
     */
    @NonNull
    public LiveData<StreamState> getPreviewStreamState() {
        return mPreviewStreamStateLiveData;
    }

    /**
     * Returns a {@link Bitmap} representation of the content displayed on the preview
     * {@link Surface}, or {@code null} if the camera preview hasn't started yet.
     * <p>
     * The returned {@link Bitmap} uses the {@link Bitmap.Config#ARGB_8888} pixel format, and its
     * dimensions depend on the {@link PreviewView}'s {@link ScaleType}. When the
     * {@link ScaleType} is {@link ScaleType#FILL_START}, {@link ScaleType#FILL_CENTER} or
     * {@link ScaleType#FILL_END}, the returned {@link Bitmap} has the same size as the
     * {@link PreviewView}. However, when the {@link ScaleType} is {@link ScaleType#FIT_START},
     * {@link ScaleType#FIT_CENTER} or {@link ScaleType#FIT_END}, the returned {@link Bitmap}
     * might be smaller than the {@link PreviewView}, since it doesn't also include its background.
     * <p>
     * <strong>Do not</strong> invoke this method from a drawing method
     * ({@link View#onDraw(Canvas)} for instance).
     * <p>
     * If an error occurs during the copy, an empty {@link Bitmap} will be returned.
     *
     * @return A {@link Bitmap.Config#ARGB_8888} {@link Bitmap} representing the content
     * displayed on the preview {@link Surface}, or null if the camera preview hasn't started yet.
     */
    @Nullable
    public Bitmap getBitmap() {
        return mImplementation == null ? null : mImplementation.getBitmap();
    }

    private boolean shouldUseTextureView(@NonNull CameraInfo cameraInfo,
            @NonNull final ImplementationMode implementationMode) {
        if (Build.VERSION.SDK_INT <= 24 || cameraInfo.getImplementationType().equals(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY) || isRemoteDisplayMode()) {
            // Force to use TextureView when the device is running android 7.0 and below, legacy
            // level or it is running in remote display mode.
            return true;
        }
        switch (implementationMode) {
            case COMPATIBLE:
                return true;
            case PERFORMANCE:
                return false;
            default:
                throw new IllegalArgumentException(
                        "Invalid implementation mode: " + implementationMode);
        }
    }

    private boolean isSensorDimensionFlipNeeded(@NonNull CameraInfo cameraInfo) {
        int sensorDegrees;

        // Retrieve sensor rotation degrees when there is camera info.
        sensorDegrees = cameraInfo.getSensorRotationDegrees();

        // When the sensor degrees value is 90 or 270, the width/height of the surface resolution
        // need to be swapped to do the scale related calculations.
        return sensorDegrees % 180 == 90;
    }

    @SuppressWarnings("deprecation")
    private boolean isRemoteDisplayMode() {
        DisplayManager displayManager =
                (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);

        Display display = ((WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        if (displayManager.getDisplays().length <= 1) {
            // When there is not more than one display on the device, it won't be remote display
            // mode.
            return false;
        } else if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            // When there is more than one display on the device and the display that the
            // application is running on is not the default built-in display id (0), it is remote
            // display mode.
            return true;
        }

        return false;
    }

    /**
     * The implementation mode of a {@link PreviewView}.
     *
     * <p> User preference on how the {@link PreviewView} should render the preview.
     * {@link PreviewView} displays the preview with either a {@link SurfaceView} or a
     * {@link TextureView}. A {@link SurfaceView} is generally better than a {@link TextureView}
     * when it comes to certain key metrics, including power and latency. On the other hand,
     * {@link TextureView} is better supported by a wider range of devices. The option is used by
     * {@link PreviewView} to decide what is the best internal implementation given the device
     * capabilities and user configurations.
     */
    public enum ImplementationMode {

        /**
         * Use a {@link SurfaceView} for the preview when possible. If the device
         * doesn't support {@link SurfaceView}, {@link PreviewView} will fall back to use a
         * {@link TextureView} instead.
         *
         * {@link PreviewView} falls back to {@link TextureView} when the API level is 24 or lower,
         * the camera hardware is
         * {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}, or
         * {@link Preview#getTargetRotation()} is different from {@link PreviewView}'s display
         * rotation.
         *
         * Use of this mode is discouraged if {@link Preview.Builder#setTargetRotation(int)} is set
         * to a value different than the display's rotation at the time the {@link Preview} was
         * created. If preview's target rotation is changed after the preview starts, it will
         * cause extra latency to switch from {@link SurfaceView} to {@link TextureView} because
         * {@link SurfaceView} does not support arbitrary transformation. This mode is also
         * encouraged if the {@link PreviewView} needs to be animated. {@link SurfaceView}
         * animation is not supported on API level 24 or lower. Also for streaming state
         * provided in {@link #getPreviewStreamState}, the {@link StreamState#STREAMING} state
         * might happen prematurely if this mode is used.
         *
         * @see Preview.Builder#setTargetRotation(int)
         * @see Preview.Builder#getTargetRotation()
         * @see Display#getRotation()
         * @see CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
         * @see StreamState#STREAMING
         */
        PERFORMANCE,

        /**
         * Use a {@link TextureView} for the preview.
         */
        COMPATIBLE
    }

    /** Options for scaling the preview vis-à-vis its container {@link PreviewView}. */
    public enum ScaleType {
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and align it to the start of the view, which is the top left
         * corner in a left-to-right (LTR) layout, or the top right corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_START(0),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and center it in the view.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_CENTER(1),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and align it to the end of the view, which is the bottom right
         * corner in a left-to-right (LTR) layout, or the bottom left corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_END(2),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and align it to the start of the view, which is the
         * top left corner in a left-to-right (LTR) layout, or the top right corner in a
         * right-to-left (RTL) layout.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_START(3),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and center it inside the view.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_CENTER(4),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and align it to the end of the view, which is the
         * bottom right corner in a left-to-right (LTR) layout, or the bottom left corner in a
         * right-to-left (RTL) layout.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_END(5);

        private final int mId;

        ScaleType(int id) {
            mId = id;
        }

        int getId() {
            return mId;
        }

        static ScaleType fromId(int id) {
            for (ScaleType scaleType : values()) {
                if (scaleType.mId == id) {
                    return scaleType;
                }
            }
            throw new IllegalArgumentException("Unknown scale type id " + id);
        }
    }

    /**
     * Definitions for current preview stream state.
     */
    public enum StreamState {
        /** Preview is not visible yet. */
        IDLE,
        /**
         * Preview is streaming.
         *
         * It only guarantees preview streaming when implementation mode is
         * {@link ImplementationMode#COMPATIBLE}. When in {@link ImplementationMode#PERFORMANCE},
         * it is possible that preview becomes visible slightly after state is changed. For apps
         * relying on the preview visible signal to work correctly, please set
         * {@link ImplementationMode#PERFORMANCE} mode via {@link #setImplementationMode}.
         */
        STREAMING
    }

    /**
     * Sets the {@link CameraController}.
     *
     * <p> The controller creates and manages the {@link Preview} that backs the
     * {@link PreviewView}. It also configures the {@link ViewPort} based on the {@link ScaleType}
     * and the dimension of the {@link PreviewView}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @MainThread
    public void setController(@Nullable CameraController cameraController) {
        Threads.checkMainThread();
        if (mCameraController != null && mCameraController != cameraController) {
            // If already bound to a different controller, ask the old controller to stop
            // using this PreviewView.
            mCameraController.clearPreviewSurface();
        }
        mCameraController = cameraController;
        if (mCameraController != null) {
            mCameraController.attachPreviewSurface(createSurfaceProvider(), getWidth(),
                    getHeight());
        }
    }

    /**
     * Get the {@link CameraController}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    @MainThread
    public CameraController getController() {
        Threads.checkMainThread();
        return mCameraController;
    }
}
