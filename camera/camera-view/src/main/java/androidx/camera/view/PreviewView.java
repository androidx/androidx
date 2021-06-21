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

import static androidx.camera.view.TransformUtils.getNormalizedToBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalUseCaseGroup;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.Logger;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.view.internal.compat.quirk.DeviceQuirks;
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.OutputTransform;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom View that displays the camera feed for CameraX's {@link Preview} use case.
 *
 * <p> This class manages the preview {@link Surface}'s lifecycle. It internally uses either a
 * {@link TextureView} or {@link SurfaceView} to display the camera feed, and applies required
 * transformations on them to correctly display the preview, this involves correcting their
 * aspect ratio, scale and rotation.
 *
 * <p> If {@link PreviewView} uses a {@link SurfaceView} to display the preview
 * stream, be careful when overlapping a {@link View} that's initially not visible (either
 * {@link View#INVISIBLE} or {@link View#GONE}) on top of it. When the {@link SurfaceView} is
 * attached to the display window, it calls
 * {@link android.view.ViewParent#requestTransparentRegion(View)} which requests a computation of
 * the transparent regions on the display. At this point, the {@link View} isn't visible, causing
 * the overlapped region between the {@link SurfaceView} and the {@link View} to be
 * considered transparent. Later if the {@link View} becomes {@linkplain View#VISIBLE visible}, it
 * will not be displayed on top of {@link SurfaceView}. A way around this is to call
 * {@link android.view.ViewParent#requestTransparentRegion(View)} right after making the
 * {@link View} visible, or initially hiding the {@link View} by setting its
 * {@linkplain View#setAlpha(float) opacity} to 0, then setting it to 1.0F to show it.
 */
public final class PreviewView extends FrameLayout {

    private static final String TAG = "PreviewView";

    @ColorRes
    static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.PERFORMANCE;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    ImplementationMode mImplementationMode = DEFAULT_IMPL_MODE;

    @VisibleForTesting
    @Nullable
    PreviewViewImplementation mImplementation;

    @NonNull
    final PreviewTransformation mPreviewTransform = new PreviewTransformation();

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final MutableLiveData<StreamState> mPreviewStreamStateLiveData =
            new MutableLiveData<>(StreamState.IDLE);

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    final AtomicReference<PreviewStreamStateObserver> mActiveStreamStateObserver =
            new AtomicReference<>();
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    CameraController mCameraController;

    @NonNull
    PreviewViewMeteringPointFactory mPreviewViewMeteringPointFactory =
            new PreviewViewMeteringPointFactory(mPreviewTransform);

    // Detector for zoom-to-scale.
    @NonNull
    private final ScaleGestureDetector mScaleGestureDetector;

    @Nullable
    private MotionEvent mTouchUpEvent;

    private final OnLayoutChangeListener mOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                boolean isSizeChanged =
                        right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;
                if (isSizeChanged) {
                    redrawPreview();
                    attachToControllerIfReady(true);
                }
            };

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final Preview.SurfaceProvider mSurfaceProvider = new Preview.SurfaceProvider() {

        @OptIn(markerClass = ExperimentalUseCaseGroup.class)
        @Override
        @AnyThread
        public void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest) {
            if (!Threads.isMainThread()) {
                // Post on main thread to ensure thread safety.
                ContextCompat.getMainExecutor(getContext()).execute(
                        () -> mSurfaceProvider.onSurfaceRequested(surfaceRequest));
                return;
            }
            Logger.d(TAG, "Surface requested by Preview.");
            CameraInternal camera = surfaceRequest.getCamera();
            surfaceRequest.setTransformationInfoListener(
                    ContextCompat.getMainExecutor(getContext()),
                    transformationInfo -> {
                        Logger.d(TAG,
                                "Preview transformation info updated. " + transformationInfo);
                        // TODO(b/159127402): maybe switch to COMPATIBLE mode if target
                        //  rotation is not display rotation.
                        boolean isFrontCamera =
                                camera.getCameraInfoInternal().getLensFacing()
                                        == CameraSelector.LENS_FACING_FRONT;
                        mPreviewTransform.setTransformationInfo(transformationInfo,
                                surfaceRequest.getResolution(), isFrontCamera);
                        redrawPreview();
                    });

            mImplementation = shouldUseTextureView(surfaceRequest, mImplementationMode)
                    ? new TextureViewImplementation(PreviewView.this, mPreviewTransform)
                    : new SurfaceViewImplementation(PreviewView.this, mPreviewTransform);

            PreviewStreamStateObserver streamStateObserver =
                    new PreviewStreamStateObserver(camera.getCameraInfoInternal(),
                            mPreviewStreamStateLiveData, mImplementation);
            mActiveStreamStateObserver.set(streamStateObserver);

            camera.getCameraState().addObserver(
                    ContextCompat.getMainExecutor(getContext()), streamStateObserver);
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
        }
    };

    @UiThread
    public PreviewView(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Threads.checkMainThread();
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.PreviewView, defStyleAttr, defStyleRes);
        ViewCompat.saveAttributeDataForStyleable(this, context, R.styleable.PreviewView, attrs,
                attributes, defStyleAttr, defStyleRes);

        try {
            final int scaleTypeId = attributes.getInteger(
                    R.styleable.PreviewView_scaleType,
                    mPreviewTransform.getScaleType().getId());
            setScaleType(ScaleType.fromId(scaleTypeId));

            int implementationModeId =
                    attributes.getInteger(R.styleable.PreviewView_implementationMode,
                            DEFAULT_IMPL_MODE.getId());
            setImplementationMode(ImplementationMode.fromId(implementationModeId));
        } finally {
            attributes.recycle();
        }

        mScaleGestureDetector = new ScaleGestureDetector(
                context, new PinchToZoomOnScaleGestureListener());

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
        attachToControllerIfReady(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onDetachedFromWindow();
        }
        if (mCameraController != null) {
            mCameraController.clearPreviewSurface();
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mCameraController == null) {
            // Do not consume events if controller is not set.
            return super.onTouchEvent(event);
        }
        boolean isSingleTouch = event.getPointerCount() == 1;
        boolean isUpEvent = event.getAction() == MotionEvent.ACTION_UP;
        boolean notALongPress = event.getEventTime() - event.getDownTime()
                < ViewConfiguration.getLongPressTimeout();
        if (isSingleTouch && isUpEvent && notALongPress) {
            // If the event is a click, invoke tap-to-focus and forward it to user's
            // OnClickListener#onClick.
            mTouchUpEvent = event;
            performClick();
            // A click has been detected and forwarded. Consume the event so onClick won't be
            // invoked twice.
            return true;
        }
        return mScaleGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        if (mCameraController != null) {
            // mTouchUpEvent == null means it's an accessibility click. Focus at the center instead.
            float x = mTouchUpEvent != null ? mTouchUpEvent.getX() : getWidth() / 2f;
            float y = mTouchUpEvent != null ? mTouchUpEvent.getY() : getHeight() / 2f;
            mCameraController.onTapToFocus(mPreviewViewMeteringPointFactory, x, y);
        }
        mTouchUpEvent = null;
        return super.performClick();
    }

    /**
     * Sets the {@link ImplementationMode} for the {@link PreviewView}.
     *
     * <p> {@link PreviewView} displays the preview with a {@link TextureView} when the
     * mode is {@link ImplementationMode#COMPATIBLE}, and tries to use a {@link SurfaceView} if
     * it is {@link ImplementationMode#PERFORMANCE} when possible, which depends on the device's
     * attributes (e.g. API level, camera hardware support level). If not set, the default mode
     * is {@link ImplementationMode#PERFORMANCE}.
     *
     * <p> This method needs to be called before the {@link Preview.SurfaceProvider} is set on
     * {@link Preview}. Once changed, {@link Preview.SurfaceProvider} needs to be set again. e.g.
     * {@code preview.setSurfaceProvider(previewView.getSurfaceProvider())}.
     */
    @UiThread
    public void setImplementationMode(@NonNull final ImplementationMode implementationMode) {
        Threads.checkMainThread();
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
    @UiThread
    @NonNull
    public ImplementationMode getImplementationMode() {
        Threads.checkMainThread();
        return mImplementationMode;
    }

    /**
     * Gets a {@link Preview.SurfaceProvider} to be used with
     * {@link Preview#setSurfaceProvider(Executor, Preview.SurfaceProvider)}. This allows the
     * camera feed to start when the {@link Preview} use case is bound to a lifecycle.
     *
     * <p> The returned {@link Preview.SurfaceProvider} will provide a preview {@link Surface} to
     * the camera that's either managed by a {@link TextureView} or {@link SurfaceView} depending
     * on the {@link ImplementationMode} and the device's attributes (e.g. API level, camera
     * hardware support level).
     *
     * @return A {@link Preview.SurfaceProvider} to attach to a {@link Preview} use case.
     * @see ImplementationMode
     */
    @UiThread
    @NonNull
    @OptIn(markerClass = ExperimentalUseCaseGroup.class)
    public Preview.SurfaceProvider getSurfaceProvider() {
        Threads.checkMainThread();
        return mSurfaceProvider;
    }

    /**
     * Applies a {@link ScaleType} to the preview.
     *
     * <p> If a {@link CameraController} is attached to {@link PreviewView}, the change will take
     * immediate effect. It also takes immediate effect if {@link #getViewPort()} is not set in
     * the bound {@link UseCaseGroup}. Otherwise, the {@link UseCase}s need to be bound again
     * with the latest value of {@link #getViewPort()}.
     *
     * <p> This value can also be set in the layout XML file via the {@code app:scaleType}
     * attribute.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @param scaleType A {@link ScaleType} to apply to the preview.
     * @attr name app:scaleType
     */
    @UiThread
    public void setScaleType(@NonNull final ScaleType scaleType) {
        Threads.checkMainThread();
        mPreviewTransform.setScaleType(scaleType);
        redrawPreview();
        // Notify controller to re-calculate the crop rect.
        attachToControllerIfReady(false);
    }

    /**
     * Returns the {@link ScaleType} currently applied to the preview.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @return The {@link ScaleType} currently applied to the preview.
     */
    @UiThread
    @NonNull
    public ScaleType getScaleType() {
        Threads.checkMainThread();
        return mPreviewTransform.getScaleType();
    }

    /**
     * Gets the {@link MeteringPointFactory} for the camera currently connected to the
     * {@link PreviewView}, if any.
     *
     * <p> The returned {@link MeteringPointFactory} is capable of creating {@link MeteringPoint}s
     * from (x, y) coordinates in the {@link PreviewView}. This conversion takes into account its
     * {@link ScaleType}. The {@link MeteringPointFactory} is automatically adjusted if the
     * {@link PreviewView} layout or the {@link ScaleType} changes.
     *
     * <p> The {@link MeteringPointFactory} returns invalid {@link MeteringPoint} if the
     * preview is not ready, or the {@link PreviewView} dimension is zero. The invalid
     * {@link MeteringPoint} will cause
     * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} to fail but it won't
     * crash the application. Wait for the {@link StreamState#STREAMING} state to make sure the
     * preview is ready.
     *
     * @return a {@link MeteringPointFactory}
     * @see #getPreviewStreamState()
     */
    @UiThread
    @NonNull
    public MeteringPointFactory getMeteringPointFactory() {
        Threads.checkMainThread();
        return mPreviewViewMeteringPointFactory;
    }

    /**
     * Gets a {@link LiveData} for the preview {@link StreamState}.
     *
     * <p>There are two preview stream states, {@link StreamState#IDLE} and
     * {@link StreamState#STREAMING}. {@link StreamState#IDLE} indicates the preview is currently
     * not visible and streaming is stopped. {@link StreamState#STREAMING} means the preview is
     * streaming or is about to start streaming. This state guarantees the preview is visible
     * only when the {@link ImplementationMode} is {@link ImplementationMode#COMPATIBLE}. When in
     * {@link ImplementationMode#PERFORMANCE} mode, it is possible the preview becomes
     * visible slightly after the state changes to {@link StreamState#STREAMING}.
     *
     * <p>Apps that require a precise signal for when the preview starts should
     * {@linkplain #setImplementationMode(ImplementationMode) set} the implementation mode to
     * {@link ImplementationMode#COMPATIBLE}.
     *
     * @return A {@link LiveData} of the preview's {@link StreamState}. Apps can get the current
     * state with {@link LiveData#getValue()}, or register an observer with
     * {@link LiveData#observe} .
     */
    @NonNull
    public LiveData<StreamState> getPreviewStreamState() {
        return mPreviewStreamStateLiveData;
    }

    /**
     * Returns a {@link Bitmap} representation of the content displayed on the
     * {@link PreviewView}, or {@code null} if the camera preview hasn't started yet.
     * <p>
     * The returned {@link Bitmap} uses the {@link Bitmap.Config#ARGB_8888} pixel format and its
     * dimensions are the same as this view's.
     * <p>
     * <strong>Do not</strong> invoke this method from a drawing method
     * ({@link View#onDraw(Canvas)} for instance).
     * <p>
     * If an error occurs during the copy, an empty {@link Bitmap} will be returned.
     * <p>
     * If the preview hasn't started yet, the method may return null or an empty {@link Bitmap}. Use
     * {@link #getPreviewStreamState()} to get the {@link StreamState} and wait for
     * {@link StreamState#STREAMING} to make sure the preview is started.
     *
     * @return A {@link Bitmap.Config#ARGB_8888} {@link Bitmap} representing the content
     * displayed on the {@link PreviewView}, or null if the camera preview hasn't started yet.
     */
    @UiThread
    @Nullable
    public Bitmap getBitmap() {
        Threads.checkMainThread();
        return mImplementation == null ? null : mImplementation.getBitmap();
    }

    /**
     * Gets a {@link ViewPort} based on the current status of {@link PreviewView}.
     *
     * <p> Returns a {@link ViewPort} instance based on the {@link PreviewView}'s current width,
     * height, layout direction, scale type and display rotation. By using the {@link ViewPort}, all
     * the {@link UseCase}s in the {@link UseCaseGroup} will have the same output image that also
     * matches the aspect ratio of the {@link PreviewView}.
     *
     * @return null if the view is not currently attached or the view's width/height is zero.
     * @see ViewPort
     * @see UseCaseGroup
     */
    @UiThread
    @Nullable
    @ExperimentalUseCaseGroup
    public ViewPort getViewPort() {
        Threads.checkMainThread();
        if (getDisplay() == null) {
            // Returns null if the layout is not ready.
            return null;
        }
        return getViewPort(getDisplay().getRotation());
    }

    /**
     * Gets a {@link ViewPort} with custom target rotation.
     *
     * <p>Returns a {@link ViewPort} instance based on the {@link PreviewView}'s current width,
     * height, layout direction, scale type and the given target rotation.
     *
     * <p>Use this method if {@link Preview}'s desired rotation is not the default display
     * rotation. For example, when remote display is in use and the desired rotation for the
     * remote display is based on the accelerometer reading. In that case, use
     * {@link android.view.OrientationEventListener} to obtain the target rotation and create
     * {@link ViewPort} as following:
     * <p>{@link android.view.OrientationEventListener#ORIENTATION_UNKNOWN}: orientation == -1
     * <p>{@link Surface#ROTATION_0}: orientation >= 315 || orientation < 45
     * <p>{@link Surface#ROTATION_90}: orientation >= 225 && orientation < 315
     * <p>{@link Surface#ROTATION_180}: orientation >= 135 && orientation < 225
     * <p>{@link Surface#ROTATION_270}: orientation >= 45 && orientation < 135
     *
     * <p> Once the target rotation is obtained, use it with {@link Preview#setTargetRotation} to
     * update the rotation. Example:
     *
     * <pre><code>
     * Preview preview = new Preview.Builder().setTargetRotation(targetRotation).build();
     * ViewPort viewPort = previewView.getViewPort(targetRotation);
     * UseCaseGroup useCaseGroup =
     *     new UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(preview).build();
     * cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup);
     * </code></pre>
     *
     * <p> Note that for non-display rotation to work, the mode must be set to
     * {@link ImplementationMode#COMPATIBLE}.
     *
     * @param targetRotation A rotation value, expressed as one of
     *                       {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                       {@link Surface#ROTATION_180}, or
     *                       {@link Surface#ROTATION_270}.
     * @return null if the view's width/height is zero.
     * @see ImplementationMode
     */
    @UiThread
    @SuppressLint("WrongConstant")
    @Nullable
    @ExperimentalUseCaseGroup
    public ViewPort getViewPort(@ImageOutputConfig.RotationValue int targetRotation) {
        Threads.checkMainThread();
        if (getWidth() == 0 || getHeight() == 0) {
            return null;
        }
        return new ViewPort.Builder(new Rational(getWidth(), getHeight()), targetRotation)
                .setScaleType(getViewPortScaleType())
                .setLayoutDirection(getLayoutDirection())
                .build();
    }

    /**
     * Converts {@link PreviewView.ScaleType} to {@link ViewPort.ScaleType}.
     */
    private int getViewPortScaleType() {
        switch (getScaleType()) {
            case FILL_END:
                return ViewPort.FILL_END;
            case FILL_CENTER:
                return ViewPort.FILL_CENTER;
            case FILL_START:
                return ViewPort.FILL_START;
            case FIT_END:
                // Fallthrough
            case FIT_CENTER:
                // Fallthrough
            case FIT_START:
                return ViewPort.FIT;
            default:
                throw new IllegalStateException("Unexpected scale type: " + getScaleType());
        }
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void redrawPreview() {
        if (mImplementation != null) {
            mImplementation.redrawPreview();
        }
        mPreviewViewMeteringPointFactory.recalculate(new Size(getWidth(), getHeight()),
                getLayoutDirection());
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    static boolean shouldUseTextureView(@NonNull SurfaceRequest surfaceRequest,
            @NonNull final ImplementationMode implementationMode) {
        // TODO(b/159127402): use TextureView if target rotation is not display rotation.
        boolean isLegacyDevice = surfaceRequest.getCamera().getCameraInfoInternal()
                .getImplementationType().equals(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
        boolean hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class) != null;
        if (surfaceRequest.isRGBA8888Required() || Build.VERSION.SDK_INT <= 24 || isLegacyDevice
                || hasSurfaceViewQuirk) {
            // Force to use TextureView when the device is running android 7.0 and below, legacy
            // level, RGBA8888 is required or SurfaceView has quirks.
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
         * <p>{@link PreviewView} falls back to {@link TextureView} when the API level is 24 or
         * lower, the camera hardware support level is
         * {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}, or
         * {@link Preview#getTargetRotation()} is different from {@link PreviewView}'s display
         * rotation.
         *
         * <p>Do not use this mode if {@link Preview.Builder#setTargetRotation(int)} is set
         * to a value different than the display's rotation, because {@link SurfaceView} does not
         * support arbitrary rotations. Do not use this mode if the {@link PreviewView}
         * needs to be animated. {@link SurfaceView} animation is not supported on API level 24
         * or lower. Also, for the preview's streaming state provided in
         * {@link #getPreviewStreamState}, the {@link StreamState#STREAMING} state might happen
         * prematurely if this mode is used.
         *
         * @see Preview.Builder#setTargetRotation(int)
         * @see Preview.Builder#getTargetRotation()
         * @see Display#getRotation()
         * @see CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
         * @see StreamState#STREAMING
         */
        PERFORMANCE(0),

        /**
         * Use a {@link TextureView} for the preview.
         */
        COMPATIBLE(1);

        private final int mId;

        ImplementationMode(int id) {
            mId = id;
        }

        int getId() {
            return mId;
        }

        static ImplementationMode fromId(int id) {
            for (ImplementationMode implementationMode : values()) {
                if (implementationMode.mId == id) {
                    return implementationMode;
                }
            }
            throw new IllegalArgumentException("Unknown implementation mode id " + id);
        }
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
         * right-to-left (RTL) layout. The background area not covered by the preview stream
         * will be black or the background of the {@link PreviewView}
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_START(3),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and center it inside the view. The background area not
         * covered by the preview stream will be black or the background of the {@link PreviewView}.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_CENTER(4),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and align it to the end of the view, which is the
         * bottom right corner in a left-to-right (LTR) layout, or the bottom left corner in a
         * right-to-left (RTL) layout. The background area not covered by the preview stream
         * will be black or the background of the {@link PreviewView}.
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
     * Definitions for the preview stream state.
     */
    public enum StreamState {
        /** Preview is not visible yet. */
        IDLE,
        /**
         * Preview is streaming.
         *
         * <p>This state only guarantees the preview is streaming when the implementation mode is
         * {@link ImplementationMode#COMPATIBLE}. When in {@link ImplementationMode#PERFORMANCE}
         * mode, it is possible that the preview becomes visible slightly after the state has
         * changed. For apps requiring a precise signal for when the preview starts, please set
         * {@link ImplementationMode#COMPATIBLE} mode via {@link #setImplementationMode}.
         */
        STREAMING
    }

    /**
     * GestureListener that speeds up scale factor and sends it to controller.
     */
    class PinchToZoomOnScaleGestureListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mCameraController != null) {
                mCameraController.onPinchToZoom(detector.getScaleFactor());
            }
            return true;
        }
    }

    /**
     * Sets the {@link CameraController}.
     *
     * <p> Once set, the controller will use {@link PreviewView} to display camera preview feed.
     * It also uses the {@link PreviewView}'s layout dimension to set the crop rect for all the use
     * cases so that the output from other use cases match what the end user sees in
     * {@link PreviewView}. It also enables features like tap-to-focus and pinch-to-zoom.
     *
     * <p> Setting it to {@code null} or to a different {@link CameraController} stops the previous
     * {@link CameraController} from working. The previous {@link CameraController} will remain
     * detached until it's set on the {@link PreviewView} again.
     *
     * @throws IllegalArgumentException If the {@link CameraController}'s camera selector
     *                                  is unable to resolve a camera to be used for the enabled
     *                                  use cases.
     * @see CameraController
     */
    @UiThread
    public void setController(@Nullable CameraController cameraController) {
        Threads.checkMainThread();
        if (mCameraController != null && mCameraController != cameraController) {
            // If already bound to a different controller, ask the old controller to stop
            // using this PreviewView.
            mCameraController.clearPreviewSurface();
        }
        mCameraController = cameraController;
        attachToControllerIfReady(/*shouldFailSilently=*/false);
    }

    /**
     * Get the {@link CameraController}.
     */
    @Nullable
    @UiThread
    public CameraController getController() {
        Threads.checkMainThread();
        return mCameraController;
    }

    /**
     * Gets the {@link OutputTransform} associated with the {@link PreviewView}.
     *
     * <p> Returns a {@link OutputTransform} object that represents the transform being applied to
     * the associated {@link Preview} use case. Returns null if the transform info is not ready.
     * For example, when the associated {@link Preview} has not been bound or the
     * {@link PreviewView}'s layout is not ready.
     *
     * <p> {@link PreviewView} needs to be in {@link ImplementationMode#COMPATIBLE} mode for the
     * transform to work correctly. For example, the returned {@link OutputTransform} may
     * not respect the value of {@link #getMatrix()} when {@link ImplementationMode#PERFORMANCE}
     * mode is used.
     *
     * @return the transform applied on the preview by this {@link PreviewView}.
     * @see CoordinateTransform
     */
    @TransformExperimental
    @Nullable
    public OutputTransform getOutputTransform() {
        Threads.checkMainThread();
        Matrix matrix = null;
        try {
            matrix = mPreviewTransform.getSurfaceToPreviewViewMatrix(
                    new Size(getWidth(), getHeight()), getLayoutDirection());
        } catch (IllegalStateException ex) {
            // Fall-through. It will be handled below.
        }

        Rect surfaceCropRect = mPreviewTransform.getSurfaceCropRect();
        if (matrix == null || surfaceCropRect == null) {
            Logger.d(TAG, "Transform info is not ready");
            return null;
        }
        // Map it to the normalized space (-1, -1) - (1, 1).
        matrix.preConcat(getNormalizedToBuffer(surfaceCropRect));

        // Add the custom transform applied by the app. e.g. View#setScaleX.
        if (mImplementation instanceof TextureViewImplementation) {
            matrix.postConcat(getMatrix());
        } else {
            Logger.w(TAG, "PreviewView needs to be in COMPATIBLE mode for the transform"
                    + " to work correctly.");
        }

        return new OutputTransform(matrix, new Size(surfaceCropRect.width(),
                surfaceCropRect.height()));
    }

    @OptIn(markerClass = ExperimentalUseCaseGroup.class)
    private void attachToControllerIfReady(boolean shouldFailSilently) {
        Display display = getDisplay();
        ViewPort viewPort = getViewPort();
        if (mCameraController != null && viewPort != null && isAttachedToWindow()
                && display != null) {
            try {
                mCameraController.attachPreviewSurface(getSurfaceProvider(), viewPort, display);
            } catch (IllegalStateException ex) {
                if (shouldFailSilently) {
                    // Swallow the exception and fail silently if the method is invoked by View
                    // events.
                    Logger.e(TAG, ex.getMessage(), ex);
                } else {
                    throw ex;
                }
            }
        }
    }
}
