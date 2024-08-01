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

import static androidx.camera.viewfinder.internal.utils.TransformUtils.createTransformInfo;
import static androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest.MIRROR_MODE_HORIZONTAL;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.viewfinder.internal.quirk.DeviceQuirks;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewNotCroppedByParentQuirk;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewStretchedQuirk;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.camera.viewfinder.internal.utils.Threads;
import androidx.camera.viewfinder.surface.ViewfinderSurfaceProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Base viewfinder widget that can display the camera feed for Camera2.
 *
 * <p> It internally uses either a {@link TextureView} or {@link SurfaceView} to display the
 * camera feed, and applies required transformations on them to correctly display the viewfinder,
 * this involves correcting their aspect ratio, scale and rotation.
 */
public final class CameraViewfinder extends FrameLayout {

    private static final String TAG = "CameraViewFinder";

    @ColorRes private static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final androidx.camera.viewfinder.surface.ImplementationMode DEFAULT_IMPL_MODE =
            androidx.camera.viewfinder.surface.ImplementationMode.EXTERNAL;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final ViewfinderTransformation mViewfinderTransformation = new ViewfinderTransformation();

    @SuppressWarnings("WeakerAccess")
    @NonNull
    private final DisplayRotationListener mDisplayRotationListener = new DisplayRotationListener();

    @NonNull
    private final Looper mRequiredLooper = Looper.myLooper();

    @NonNull androidx.camera.viewfinder.surface.ImplementationMode mImplementationMode;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    ViewfinderImplementation mImplementation;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest mCurrentSurfaceRequest;

    private final OnLayoutChangeListener mOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                boolean isSizeChanged =
                        right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;
                if (isSizeChanged) {
                    redrawViewfinder();
                }
            };

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final ViewfinderSurfaceProvider mSurfaceProvider = new ViewfinderSurfaceProvider() {

        @Override
        @AnyThread
        public void onSurfaceRequested(
                @NonNull androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest surfaceRequest
        ) {
            if (!Threads.isMainThread()) {
                // In short term, throwing exception to guarantee onSurfaceRequest is
                //  called on main thread. In long term, user should be able to specify an
                //  executor to run this function.
                throw new IllegalStateException("onSurfaceRequested must be called on the main  "
                        + "thread");
            }
            Logger.d(TAG, "Surface requested by Viewfinder.");

            if (surfaceRequest.getImplementationMode() != null) {
                mImplementationMode = surfaceRequest.getImplementationMode();
            }

            mImplementation = shouldUseTextureView(mImplementationMode)
                    ? new TextureViewImplementation(
                            CameraViewfinder.this, mViewfinderTransformation)
                    : new SurfaceViewImplementation(
                            CameraViewfinder.this, mViewfinderTransformation);

            mImplementation.onSurfaceRequested(surfaceRequest);

            Display display = getDisplay();
            if (display != null) {
                mViewfinderTransformation.setTransformationInfo(
                        createTransformInfo(surfaceRequest.getResolution(),
                                display,
                                surfaceRequest.getOutputMirrorMode()
                                        == MIRROR_MODE_HORIZONTAL,
                                surfaceRequest.getSourceOrientation()),
                        surfaceRequest.getResolution(),
                        surfaceRequest.getOutputMirrorMode()
                                == MIRROR_MODE_HORIZONTAL);
                redrawViewfinder();
            }
        }
    };

    @UiThread
    public CameraViewfinder(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.Viewfinder, defStyleAttr, defStyleRes);
        ViewCompat.saveAttributeDataForStyleable(this, context, R.styleable.Viewfinder, attrs,
                attributes, defStyleAttr, defStyleRes);

        try {
            final int scaleTypeId = attributes.getInteger(
                    R.styleable.Viewfinder_scaleType,
                    mViewfinderTransformation.getScaleType().getId());
            setScaleType(ScaleType.fromId(scaleTypeId));

            int implementationModeId =
                    attributes.getInteger(R.styleable.Viewfinder_implementationMode,
                            DEFAULT_IMPL_MODE.getId());
            mImplementationMode = androidx.camera.viewfinder.surface.ImplementationMode.fromId(
                    implementationModeId);
        } finally {
            attributes.recycle();
        }

        // Set background only if it wasn't already set. A default background prevents the content
        // behind the viewfinder from being visible before the viewfinder starts streaming.
        if (getBackground() == null) {
            setBackgroundColor(ContextCompat.getColor(getContext(), DEFAULT_BACKGROUND_COLOR));
        }
    }

    /**
     * Returns the {@link ImplementationMode}.
     *
     * <p>For each {@link ViewfinderSurfaceRequest} sent to {@link CameraViewfinder}, the
     * {@link ImplementationMode} set in the {@link ViewfinderSurfaceRequest} will be used first.
     * If it's not set, the {@code app:implementationMode} in the layout xml will be used. If
     * it's not set in the layout xml, the default value {@link ImplementationMode#PERFORMANCE}
     * will be used. Each {@link ViewfinderSurfaceRequest} sent to {@link CameraViewfinder} can
     * override the {@link ImplementationMode} once it has set the {@link ImplementationMode}.
     *
     * @return The {@link ImplementationMode} for the viewfinder.
     *
     * @deprecated Use {@link #getSurfaceImplementationMode()} instead. The
     * {@link ImplementationMode} in viewfinder-view will be made obsolete with the
     * introduction of viewfinder-core.
     */
    @Deprecated
    @UiThread
    @NonNull
    public ImplementationMode getImplementationMode() {
        checkUiThread();
        return ImplementationMode.fromId(mImplementationMode.getId());
    }

    /**
     * Returns the {@link androidx.camera.viewfinder.surface.ImplementationMode}.
     *
     * <p> For each {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest} sent to
     * {@link CameraViewfinder}, the
     * {@link androidx.camera.viewfinder.surface.ImplementationMode} set in the
     * {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest} will be used first.
     * If it's not set, the {@code app:implementationMode} in the layout xml will be used. If
     * it's not set in the layout xml, the default value
     * {@link androidx.camera.viewfinder.surface.ImplementationMode#EXTERNAL}
     * will be used. Each {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest} sent
     * to {@link CameraViewfinder} can override the
     * {@link androidx.camera.viewfinder.surface.ImplementationMode} once it has set the
     * {@link androidx.camera.viewfinder.surface.ImplementationMode}.
     *
     * @return The {@link androidx.camera.viewfinder.surface.ImplementationMode} for
     * {@link CameraViewfinder}.
     */
    @UiThread
    @NonNull
    public androidx.camera.viewfinder.surface.ImplementationMode getSurfaceImplementationMode() {
        checkUiThread();
        return mImplementationMode;
    }

    /**
     * Applies a {@link ScaleType} to the viewfinder.
     *
     * <p> This value can also be set in the layout XML file via the {@code app:scaleType}
     * attribute.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * <p> This method should be called after {@link CameraViewfinder} is inflated and can be
     * called before or after
     * {@link CameraViewfinder#requestSurfaceAsync(ViewfinderSurfaceRequest)}. The
     * {@link ScaleType} to set will be effective immediately after the method is called.
     *
     * @param scaleType The {@link ScaleType} to apply to the viewfinder.
     * @attr name app:scaleType
     */
    @UiThread
    public void setScaleType(@NonNull final ScaleType scaleType) {
        checkUiThread();
        mViewfinderTransformation.setScaleType(scaleType);
        redrawViewfinder();
    }

    /**
     * Returns the {@link ScaleType} currently applied to the viewfinder.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @return The {@link ScaleType} currently applied to the viewfinder.
     */
    @UiThread
    @NonNull
    public ScaleType getScaleType() {
        checkUiThread();
        return mViewfinderTransformation.getScaleType();
    }

    /**
     * Requests surface by sending a {@link ViewfinderSurfaceRequest}.
     *
     * <p> Only one request can be handled at the same time. If requesting a surface with
     * the same {@link ViewfinderSurfaceRequest}, the previous requested surface will be returned.
     * If requesting a surface with a new {@link ViewfinderSurfaceRequest}, the previous
     * requested surface will be released and a new surface will be requested.
     *
     * <p> The result is a {@link ListenableFuture} of {@link Surface}, which provides the
     * functionality to attach listeners and propagate exceptions.
     *
     * <pre>{@code
     * ViewfinderSurfaceRequest request = new ViewfinderSurfaceRequest(
     *     new Size(width, height), cameraManager.getCameraCharacteristics(cameraId));
     *
     * ListenableFuture<Surface> surfaceListenableFuture =
     *     mCameraViewFinder.requestSurfaceAsync(request);
     *
     * Futures.addCallback(surfaceListenableFuture, new FutureCallback<Surface>() {
     *     {@literal @}Override
     *     public void onSuccess({@literal @}Nullable Surface surface) {
     *         if (surface != null) {
     *             createCaptureSession(surface);
     *         }
     *     }
     *
     *     {@literal @}Override
     *     public void onFailure(Throwable t) {}
     * }, ContextCompat.getMainExecutor(getContext()));
     * }</pre>
     *
     * @param surfaceRequest The {@link ViewfinderSurfaceRequest} to get a surface.
     * @return The requested surface.
     *
     * @see ViewfinderSurfaceRequest
     * @deprecated Use
     * {@link #requestSurfaceAsync(androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest)}
     * instead. The {@link ViewfinderSurfaceRequest} in viewfinder-view will be made obsolete
     * with the introduction of viewfinder-core.
     */
    @Deprecated
    @UiThread
    @NonNull
    public ListenableFuture<Surface> requestSurfaceAsync(
            @NonNull ViewfinderSurfaceRequest surfaceRequest) {
        return requestSurfaceAsync(surfaceRequest.getViewfinderSurfaceRequest());
    }

    /**
     * Requests surface by sending a
     * {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest}.
     *
     * <p> Only one request can be handled at the same time. If requesting a surface with
     * the same {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest}, the previous
     * requested surface will be returned. If requesting a surface with a new
     * {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest}, the previous
     * requested surface will be released and a new surface will be requested.
     *
     * <p> The result is a {@link ListenableFuture} of {@link Surface}, which provides the
     * functionality to attach listeners and propagate exceptions.
     *
     * <pre>{@code
     * ViewfinderSurfaceRequest request = new ViewfinderSurfaceRequest(
     *     new Size(width, height), cameraManager.getCameraCharacteristics(cameraId));
     *
     * ListenableFuture<Surface> surfaceListenableFuture =
     *     mCameraViewFinder.requestSurfaceAsync(request);
     *
     * Futures.addCallback(surfaceListenableFuture, new FutureCallback<Surface>() {
     *     {@literal @}Override
     *     public void onSuccess({@literal @}Nullable Surface surface) {
     *         if (surface != null) {
     *             createCaptureSession(surface);
     *         }
     *     }
     *
     *     {@literal @}Override
     *     public void onFailure(Throwable t) {}
     * }, ContextCompat.getMainExecutor(getContext()));
     * }</pre>
     *
     * @param surfaceRequest The {@link androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest}
     *                       to get a surface.
     * @return The requested surface.
     *
     * @see androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
     */
    @UiThread
    @NonNull
    public ListenableFuture<Surface> requestSurfaceAsync(
            @NonNull androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest surfaceRequest) {
        checkUiThread();

        if (mCurrentSurfaceRequest != null
                && surfaceRequest.equals(mCurrentSurfaceRequest)) {
            return mCurrentSurfaceRequest.getSurfaceAsync();
        }

        if (mCurrentSurfaceRequest != null) {
            mCurrentSurfaceRequest.markSurfaceSafeToRelease();
        }

        ListenableFuture<Surface> surfaceListenableFuture =
                surfaceRequest.getSurfaceAsync();
        mCurrentSurfaceRequest = surfaceRequest;

        provideSurfaceIfReady();

        return surfaceListenableFuture;
    }

    /**
     * Returns a {@link Bitmap} representation of the content displayed on the
     * {@link CameraViewfinder}, or {@code null} if the camera viewfinder hasn't started yet.
     * <p>
     * The returned {@link Bitmap} uses the {@link Bitmap.Config#ARGB_8888} pixel format and its
     * dimensions are the same as this view's.
     * <p>
     * <strong>Do not</strong> invoke this method from a drawing method
     * ({@link View#onDraw(Canvas)} for instance).
     * <p>
     * If an error occurs during the copy, an empty {@link Bitmap} will be returned.
     *
     * @return A {@link Bitmap.Config#ARGB_8888} {@link Bitmap} representing the content
     * displayed on the {@link CameraViewfinder}, or null if the camera viewfinder hasn't started
     * yet.
     */
    @UiThread
    @Nullable
    public Bitmap getBitmap() {
        checkUiThread();
        return mImplementation == null ? null : mImplementation.getBitmap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onAttachedToWindow();
        }
        startListeningToDisplayChange();

        // TODO: need to handle incomplete surface request if request is received before view
        //  attached to window.
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onDetachedFromWindow();
        }
        if (mCurrentSurfaceRequest != null) {
            mCurrentSurfaceRequest.markSurfaceSafeToRelease();
            mCurrentSurfaceRequest = null;
        }
        stopListeningToDisplayChange();
    }

    @VisibleForTesting
    static boolean shouldUseTextureView(
            @NonNull final androidx.camera.viewfinder.surface.ImplementationMode implementationMode
    ) {
        boolean hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class) != null
                ||  DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk.class) != null;
        if (Build.VERSION.SDK_INT <= 24 || hasSurfaceViewQuirk) {
            // Force to use TextureView when the device is running android 7.0 and below, legacy
            // level or SurfaceView has quirks.
            Logger.d(TAG, "Implementation mode to set is not supported, forcing to use "
                    + "TextureView, because transform APIs are not supported on these devices.");
            return true;
        }
        switch (implementationMode) {
            case EMBEDDED:
                return true;
            case EXTERNAL:
                return false;
            default:
                throw new IllegalArgumentException(
                        "Invalid implementation mode: " + implementationMode);
        }
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void redrawViewfinder() {
        if (mImplementation != null) {
            mImplementation.redrawViewfinder();
        }
    }

    private boolean provideSurfaceIfReady() {
        final androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest surfaceRequest =
                mCurrentSurfaceRequest;
        final ViewfinderSurfaceProvider surfaceProvider = mSurfaceProvider;
        if (surfaceProvider != null && surfaceRequest != null) {
            surfaceProvider.onSurfaceRequested(surfaceRequest);
            return true;
        }
        return false;
    }

    /**
     * Checks if the current thread is the same UI thread on which the class was constructed.
     *
     * @see <a href = go/android-api-guidelines/concurrency#uithread></a>
     */
    private void checkUiThread() {
        // Ignore mRequiredLooper == null because this can be called from the super
        // class constructor before the class's own constructor has run.
        if (mRequiredLooper != null && Looper.myLooper() != mRequiredLooper) {
            Throwable throwable = new Throwable(
                    "A method was called on thread '" + Thread.currentThread().getName()
                            + "'. All methods must be called on the same thread. (Expected Looper "
                            + mRequiredLooper + ", but called on " + Looper.myLooper() + ".");
            throw new RuntimeException(throwable);
        }
    }

    /**
     * The implementation mode of a {@link CameraViewfinder}.
     *
     * <p> User preference on how the {@link CameraViewfinder} should render the viewfinder.
     * {@link CameraViewfinder} displays the viewfinder with either a {@link SurfaceView} or a
     * {@link TextureView}. A {@link SurfaceView} is generally better than a {@link TextureView}
     * when it comes to certain key metrics, including power and latency. On the other hand,
     * {@link TextureView} is better supported by a wider range of devices. The option is used by
     * {@link CameraViewfinder} to decide what is the best internal implementation given the device
     * capabilities and user configurations.
     *
     * @deprecated Use {@link androidx.camera.viewfinder.surface.ImplementationMode} instead.
     */
    @Deprecated
    public enum ImplementationMode {

        /**
         * Use a {@link SurfaceView} for the viewfinder when possible. A SurfaceView has somewhat
         * lower latency and less performance and power overhead than a TextureView. Use this
         *
         * If the device doesn't support {@link SurfaceView}, {@link CameraViewfinder} will fall
         * back to use a {@link TextureView} instead.
         *
         * <p>{@link CameraViewfinder} falls back to {@link TextureView} when the API level is 24 or
         * lower, the camera hardware support level is
         * {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}.
         *
         */
        PERFORMANCE(0),

        /**
         * Use a {@link TextureView} for the viewfinder.
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

    /** Options for scaling the viewfinder vis-à-vis its container {@link CameraViewfinder}. */
    public enum ScaleType {
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraViewfinder}, and align it to the start of the view, which is the top left
         * corner in a left-to-right (LTR) layout, or the top right corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the viewfinder to be cropped if the camera viewfinder aspect ratio does
         * not match that of its container {@link CameraViewfinder}.
         */
        FILL_START(0),
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraViewfinder}, and center it in the view.
         * <p>
         * This may cause the viewfinder to be cropped if the camera viewfinder aspect ratio does
         * not match that of its container {@link CameraViewfinder}.
         */
        FILL_CENTER(1),
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraViewfinder}, and align it to the end of the view, which is the bottom right
         * corner in a left-to-right (LTR) layout, or the bottom left corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the viewfinder to be cropped if the camera viewfinder aspect ratio does
         * not match that of its container {@link CameraViewfinder}.
         */
        FILL_END(2),
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraViewfinder}, and align it to the start of the view, which is the
         * top left corner in a left-to-right (LTR) layout, or the top right corner in a
         * right-to-left (RTL) layout. The background area not covered by the viewfinder stream
         * will be black or the background of the {@link CameraViewfinder}
         * <p>
         * Both dimensions of the viewfinder will be equal or less than the corresponding dimensions
         * of its container {@link CameraViewfinder}.
         */
        FIT_START(3),
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraViewfinder}, and center it inside the view. The background
         * area not covered by the viewfinder stream will be black or the background of the
         * {@link CameraViewfinder}.
         * <p>
         * Both dimensions of the viewfinder will be equal or less than the corresponding dimensions
         * of its container {@link CameraViewfinder}.
         */
        FIT_CENTER(4),
        /**
         * Scale the viewfinder, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraViewfinder}, and align it to the end of the view, which is the
         * bottom right corner in a left-to-right (LTR) layout, or the bottom left corner in a
         * right-to-left (RTL) layout. The background area not covered by the viewfinder stream
         * will be black or the background of the {@link CameraViewfinder}.
         * <p>
         * Both dimensions of the viewfinder will be equal or less than the corresponding dimensions
         * of its container {@link CameraViewfinder}.
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

    private void startListeningToDisplayChange() {
        DisplayManager displayManager = getDisplayManager();
        if (displayManager == null) {
            return;
        }
        displayManager.registerDisplayListener(mDisplayRotationListener,
                new Handler(Looper.getMainLooper()));
    }

    private void stopListeningToDisplayChange() {
        DisplayManager displayManager = getDisplayManager();
        if (displayManager == null) {
            return;
        }
        displayManager.unregisterDisplayListener(mDisplayRotationListener);
    }

    @Nullable
    private DisplayManager getDisplayManager() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return (DisplayManager) context.getApplicationContext()
                .getSystemService(Context.DISPLAY_SERVICE);
    }
    /**
     * Listener for display rotation changes.
     *
     * <p> When the device is rotated 180° from side to side, the activity is not
     * destroyed and recreated. This class is necessary to make sure preview's target rotation
     * gets updated when that happens.
     */
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    class DisplayRotationListener implements DisplayManager.DisplayListener {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            Display display = getDisplay();
            if (display != null && display.getDisplayId() == displayId) {
                androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest surfaceRequest =
                        mCurrentSurfaceRequest;
                if (surfaceRequest != null) {
                    mViewfinderTransformation.updateTransformInfo(
                            createTransformInfo(surfaceRequest.getResolution(),
                                    display,
                                    surfaceRequest.getOutputMirrorMode()
                                            == MIRROR_MODE_HORIZONTAL,
                                    surfaceRequest.getSourceOrientation()));
                    redrawViewfinder();
                }
            }
        }
    }
}
