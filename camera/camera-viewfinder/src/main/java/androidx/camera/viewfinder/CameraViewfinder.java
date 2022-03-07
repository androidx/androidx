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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.camera.viewfinder.internal.quirk.DeviceQuirks;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewNotCroppedByParentQuirk;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewStretchedQuirk;
import androidx.camera.viewfinder.internal.surface.ViewfinderSurfaceProvider;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.camera.viewfinder.internal.utils.Threads;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Base view finder that displays the camera feed for Camera2 and CameraX.
 *
 * <p> This class manages the viewfinder {@link Surface}'s lifecycle. It internally uses either a
 * {@link TextureView} or {@link SurfaceView} to display the camera feed, and applies required
 * transformations on them to correctly display the viewfinder, this involves correcting their
 * aspect ratio, scale and rotation.
 *
 * @hide
 */
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraViewfinder extends FrameLayout {

    private static final String TAG = "CameraViewFinder";

    @ColorRes private static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.PERFORMANCE;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final ViewfinderTransformation mViewfinderTransformation = new ViewfinderTransformation();

    @NonNull
    private final Looper mRequiredLooper = Looper.myLooper();

    @NonNull ImplementationMode mImplementationMode = DEFAULT_IMPL_MODE;

    @Nullable
    ViewfinderImplementation mImplementation;
    @Nullable
    ViewfinderSurfaceRequest mCurrentSurfaceRequest;

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
        public void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest) {
            if (!Threads.isMainThread()) {
                // In short term, throwing exception to guarantee onSurfaceRequest is
                //  called on main thread. In long term, user should be able to specify an
                //  executor to run this function.
                throw new IllegalStateException("onSurfaceRequested must be called on the main  "
                        + "thread");
            }
            Logger.d(TAG, "Surface requested by Viewfinder.");

            mImplementation = shouldUseTextureView(
                    surfaceRequest.isLegacyDevice(), mImplementationMode)
                    ? new TextureViewImplementation(
                            CameraViewfinder.this, mViewfinderTransformation)
                    : new SurfaceViewImplementation(
                            CameraViewfinder.this, mViewfinderTransformation);

            mImplementation.onSurfaceRequested(surfaceRequest);

            mViewfinderTransformation.setTransformationInfo(
                    surfaceRequest.getTransformationInfo(),
                    surfaceRequest.getResolution(),
                    surfaceRequest.isFrontCamera());
            redrawViewfinder();
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
            setImplementationMode(ImplementationMode.fromId(implementationModeId));
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
     * Sets the {@link ImplementationMode} for the {@link CameraViewfinder}.
     *
     * <p> {@link CameraViewfinder} displays the viewfinder with a {@link TextureView} when the
     * mode is {@link ImplementationMode#COMPATIBLE}, and tries to use a {@link SurfaceView} if
     * it is {@link ImplementationMode#PERFORMANCE} when possible, which depends on the device's
     * attributes (e.g. API level). If not set, the default mode is
     * {@link ImplementationMode#PERFORMANCE}.
     */
    @UiThread
    public void setImplementationMode(@NonNull final ImplementationMode implementationMode) {
        checkUiThread();
        mImplementationMode = implementationMode;
    }

    /**
     * Returns the {@link ImplementationMode}.
     *
     * <p> If nothing is set via {@link #setImplementationMode}, the default
     * value is {@link ImplementationMode#PERFORMANCE}.
     *
     * @return The {@link ImplementationMode} for {@link CameraViewfinder}.
     */
    @UiThread
    @NonNull
    public ImplementationMode getImplementationMode() {
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
     * @param scaleType A {@link ScaleType} to apply to the viewfinder.
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
     * Request surface asynchronously.
     *
     * @param surfaceRequest surface request.
     * @return surface.
     */
    @UiThread
    @NonNull
    public ListenableFuture<Surface> requestSurfaceAsync(
            @NonNull ViewfinderSurfaceRequest surfaceRequest) {
        checkUiThread();

        if (mCurrentSurfaceRequest != null
                && surfaceRequest.equals(mCurrentSurfaceRequest)) {
            return mCurrentSurfaceRequest.getViewfinderSurface().getSurface();
        }

        if (mCurrentSurfaceRequest != null) {
            mCurrentSurfaceRequest.markSurfaceSafeToRelease();
        }

        ListenableFuture<Surface> surfaceListenableFuture =
                surfaceRequest.getViewfinderSurface().getSurface();
        mCurrentSurfaceRequest = surfaceRequest;

        provideSurfaceIfReady();

        return surfaceListenableFuture;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
        if (mImplementation != null) {
            mImplementation.onAttachedToWindow();
        }
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
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    static boolean shouldUseTextureView(
            boolean isLegacyDevice,
            @NonNull final ImplementationMode implementationMode) {
        boolean hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class) != null
                ||  DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk.class) != null;
        if (Build.VERSION.SDK_INT <= 24 || isLegacyDevice || hasSurfaceViewQuirk) {
            // Force to use TextureView when the device is running android 7.0 and below, legacy
            // level or SurfaceView has quirks.
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

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void redrawViewfinder() {
        if (mImplementation != null) {
            mImplementation.redrawViewfinder();
        }
    }

    private boolean provideSurfaceIfReady() {
        final ViewfinderSurfaceRequest surfaceRequest = mCurrentSurfaceRequest;
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
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public enum ImplementationMode {

        /**
         * Use a {@link SurfaceView} for the viewfinder when possible. If the device
         * doesn't support {@link SurfaceView}, {@link CameraViewfinder} will fall back to use a
         * {@link TextureView} instead.
         *
         * <p>{@link CameraViewfinder} falls back to {@link TextureView} when the API level is 24.
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
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
}
