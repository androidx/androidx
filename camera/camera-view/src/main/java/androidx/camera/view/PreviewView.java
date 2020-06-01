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
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.view.preview.transform.PreviewTransform;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom View that displays the camera feed for CameraX's Preview use case.
 *
 * <p> This class manages the Surface lifecycle, as well as the preview aspect ratio and
 * orientation. Internally, it uses either a {@link android.view.TextureView} or
 * {@link android.view.SurfaceView} to display the camera feed.
 */
public class PreviewView extends FrameLayout {

    private static final String TAG = "PreviewView";

    @ColorRes
    static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.SURFACE_VIEW;

    @NonNull
    private ImplementationMode mPreferredImplementationMode = DEFAULT_IMPL_MODE;

    @VisibleForTesting
    @Nullable
    PreviewViewImplementation mImplementation;

    @NonNull
    private PreviewTransform mPreviewTransform = new PreviewTransform();

    @NonNull
    private MutableLiveData<StreamState> mPreviewStreamStateLiveData =
            new MutableLiveData<>(StreamState.IDLE);

    @Nullable
    private AtomicReference<PreviewStreamStateObserver> mActiveStreamStateObserver =
            new AtomicReference<>();

    private final OnLayoutChangeListener mOnLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            if (mImplementation != null) {
                mImplementation.redrawPreview();
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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onDetachedFromWindow();
        }
    }

    /**
     * Specifies the preferred {@link ImplementationMode} to use for preview.
     * <p>
     * When the preferred {@link ImplementationMode} is {@link ImplementationMode#SURFACE_VIEW}
     * but the device doesn't support this mode (e.g. devices with API level not newer than
     * Android 7.0 or a supported camera hardware level
     * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}),
     * the actual implementation mode will be {@link ImplementationMode#TEXTURE_VIEW}.
     *
     * @param preferredMode <code>SURFACE_VIEW</code> if a {@link android.view.SurfaceView}
     *                      should be used to display the camera feed -when possible-, or
     *                      <code>TEXTURE_VIEW</code> to use a {@link android.view.TextureView}.
     */
    public void setPreferredImplementationMode(@NonNull final ImplementationMode preferredMode) {
        mPreferredImplementationMode = preferredMode;
    }

    /**
     * Returns the preferred {@link ImplementationMode} for preview.
     * <p>
     * If the preferred {@link ImplementationMode} hasn't been set using
     * {@link #setPreferredImplementationMode(ImplementationMode)}, it defaults to
     * {@link ImplementationMode#SURFACE_VIEW}.
     *
     * @return The preferred {@link ImplementationMode} for preview.
     */
    @NonNull
    public ImplementationMode getPreferredImplementationMode() {
        return mPreferredImplementationMode;
    }

    /**
     * Gets the {@link Preview.SurfaceProvider} to be used with
     * {@link Preview#setSurfaceProvider(Executor, Preview.SurfaceProvider)}.
     * <p>
     * The returned {@link Preview.SurfaceProvider} will provide a preview
     * {@link android.view.Surface} to the camera that's either managed by a
     * {@link android.view.TextureView} or {@link android.view.SurfaceView}. This option is
     * determined by the {@linkplain #setPreferredImplementationMode(ImplementationMode)
     * preferred implementation mode} and the device's capabilities.
     *
     * @return A {@link Preview.SurfaceProvider} used to start the camera preview.
     */
    @NonNull
    @UiThread
    public Preview.SurfaceProvider createSurfaceProvider() {
        Threads.checkMainThread();
        removeAllViews();

        return surfaceRequest -> {
            Log.d(TAG, "Surface requested by Preview.");
            CameraInternal camera = (CameraInternal) surfaceRequest.getCamera();
            final ImplementationMode actualImplementationMode =
                    computeImplementationMode(camera.getCameraInfo(), mPreferredImplementationMode);
            mPreviewTransform.setSensorDimensionFlipNeeded(
                    isSensorDimensionFlipNeeded(camera.getCameraInfo()));
            mImplementation = computeImplementation(actualImplementationMode);
            mImplementation.init(this, mPreviewTransform);

            PreviewStreamStateObserver streamStateObserver =
                    new PreviewStreamStateObserver((CameraInfoInternal) camera.getCameraInfo(),
                            mPreviewStreamStateLiveData, mImplementation);
            mActiveStreamStateObserver.set(streamStateObserver);

            camera.getCameraState().addObserver(
                    ContextCompat.getMainExecutor(getContext()), streamStateObserver);

            mImplementation.onSurfaceRequested(surfaceRequest, ()-> {
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
     * Creates a {@link MeteringPointFactory} by a given {@link CameraSelector}
     * <p>
     * This {@link MeteringPointFactory} is capable of creating a {@link MeteringPoint} by a
     * (x, y) in the {@link PreviewView}. It converts the points by current scaleType.
     *
     * @param cameraSelector the CameraSelector which the {@link Preview} is bound to.
     * @return a {@link MeteringPointFactory}
     */
    @NonNull
    public MeteringPointFactory createMeteringPointFactory(@NonNull CameraSelector cameraSelector) {
        Preconditions.checkNotNull(mImplementation,
                "Must set the Preview's surfaceProvider and bind it to a lifecycle first");
        return new PreviewViewMeteringPointFactory(getDisplay(), cameraSelector,
                mImplementation.getResolution(), mPreviewTransform.getScaleType(), getWidth(),
                getHeight());
    }

    /**
     * Gets the {@link LiveData} of current preview {@link StreamState}.
     *
     * <p>There are two states, {@link StreamState#IDLE} and {@link StreamState#STREAMING}.
     * {@link StreamState#IDLE} represents the preview is currently not visible and streaming is
     * stopped. {@link StreamState#STREAMING} means the preview is streaming.
     *
     * <p>When it's in STREAMING state, it guarantees preview is visible only when
     * implementationMode is TEXTURE_VIEW. When in SURFACE_VIEW implementationMode, it is
     * possible that preview becomes visible slightly after state changes to STREAMING. For apps
     * relying on the preview visible signal to be working correctly, please set TEXTURE_VIEW
     * mode by {@link #setPreferredImplementationMode}.
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

    @NonNull
    private ImplementationMode computeImplementationMode(@NonNull CameraInfo cameraInfo,
            @NonNull final ImplementationMode preferredMode) {
        // Force to use TEXTURE_VIEW when the device is running android 7.0 and below, legacy
        // level or it is running in remote display mode.
        return Build.VERSION.SDK_INT <= 24 || cameraInfo.getImplementationType().equals(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY) || isRemoteDisplayMode()
                ? ImplementationMode.TEXTURE_VIEW : preferredMode;
    }

    @NonNull
    private PreviewViewImplementation computeImplementation(
            @NonNull final ImplementationMode mode) {
        switch (mode) {
            case SURFACE_VIEW:
                return new SurfaceViewImplementation();
            case TEXTURE_VIEW:
                return new TextureViewImplementation();
            default:
                throw new IllegalStateException(
                        "Unsupported implementation mode " + mode);
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
     * <p>
     * {@link PreviewView} manages the preview {@link Surface} by either using a
     * {@link android.view.SurfaceView} or a {@link android.view.TextureView}. A
     * {@link android.view.SurfaceView} is generally better than a
     * {@link android.view.TextureView} when it comes to certain key metrics, including power and
     * latency, which is why {@link PreviewView} tries to use a {@link android.view.SurfaceView} by
     * default, but will fall back to use a {@link android.view.TextureView} when it's explicitly
     * set by calling {@link #setPreferredImplementationMode(ImplementationMode)} with
     * {@link ImplementationMode#TEXTURE_VIEW}, or when the device does not support using a
     * {@link android.view.SurfaceView} well (for example on LEGACY devices and devices running
     * on API 24 or less).
     */
    public enum ImplementationMode {
        /**
         * Use a {@link android.view.SurfaceView} for the preview. If the device doesn't support
         * it well, {@link PreviewView} will fall back to use a {@link android.view.TextureView}
         * instead.
         */
        SURFACE_VIEW,

        /** Use a {@link android.view.TextureView} for the preview */
        TEXTURE_VIEW
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
         * It guarantees preview is visible only when implementationMode is TEXTURE_VIEW. When in
         * SURFACE_VIEW implementationMode, it is possible that preview becomes visible slightly
         * after state changes to STREAMING. For apps relying on the preview visible signal to
         * be working correctly, please set TEXTURE_VIEW mode by
         * {@link #setPreferredImplementationMode}.
         */
        STREAMING
    }
}
