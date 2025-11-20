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

package androidx.camera.viewfinder.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.UiThread;
import androidx.camera.viewfinder.core.ImplementationMode;
import androidx.camera.viewfinder.core.ScaleType;
import androidx.camera.viewfinder.core.TransformationInfo;
import androidx.camera.viewfinder.core.ViewfinderDefaults;
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.camera.viewfinder.core.impl.ViewfinderSurfaceSessionImpl;
import androidx.camera.viewfinder.view.internal.futures.Futures;
import androidx.camera.viewfinder.view.internal.utils.Logger;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Viewfinder widget that displays a {@link Surface} from a source such as a Camera2 camera feed.
 *
 * <p> It internally uses either a {@link TextureView} or {@link SurfaceView} to display the
 * {@link Surface}, and applies required transformations on them to correctly display the
 * viewfinder. This involves correcting their aspect ratio, scale, rotation, and mirroring.
 */
public final class ViewfinderView extends FrameLayout {

    private static final String TAG = "ViewfinderView";

    @ColorRes private static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE =
            ViewfinderDefaults.getImplementationMode();

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final @NonNull ViewfinderTransformation mViewfinderTransformation =
            new ViewfinderTransformation();

    @SuppressWarnings("WeakerAccess")
    private final @NonNull DisplayRotationListener mDisplayRotationListener =
            new DisplayRotationListener();

    private final @NonNull Looper mRequiredLooper = Objects.requireNonNull(Looper.myLooper());

    /**
     * Holds the implementation mode set through the {@code app:implementationMode} attr or the
     * {@link #DEFAULT_IMPL_MODE} if that is not set.
     *
     * <p>The applied implementation mode, which is stored in {@code mCurrentImplementationMode},
     * will use the implementation mode set through the {@link ViewfinderSurfaceRequest}, or will
     * default to {@code mDefaultImplementationMode} if the surface request does not specify an
     * implementation mode.
     */
    @NonNull ImplementationMode mDefaultImplementationMode;
    @NonNull ImplementationMode mCurrentImplementationMode;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable ViewfinderImplementation mImplementation;


    private final OnLayoutChangeListener mOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                boolean isSizeChanged =
                        right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;
                if (isSizeChanged) {
                    redrawViewfinder();
                }
            };

    @UiThread
    public ViewfinderView(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public ViewfinderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public ViewfinderView(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public ViewfinderView(@NonNull Context context,
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
            mDefaultImplementationMode = ImplementationMode.fromId(
                    implementationModeId);
            mCurrentImplementationMode = mDefaultImplementationMode;
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
     * <p> The {@link ImplementationMode} can be specified in multiple ways, with the following
     * order of precedence:
     * <ol>
     *     <li>Set directly in the {@link ViewfinderSurfaceRequest}.</li>
     *     <li>Set via the {@code app:implementationMode} attribute in the layout XML.</li>
     *     <li>If not specified, it uses the default value from
     *     {@link ViewfinderDefaults#getImplementationMode()}. This default is chosen for
     *     maximum compatibility, using {@link ImplementationMode#EMBEDDED} on API levels 24
     *     and below or on devices with known hardware quirks, and the higher-performance
     *     {@link ImplementationMode#EXTERNAL} on all other devices.</li>
     * </ol>
     *
     * @return The currently active {@link ImplementationMode} for {@link ViewfinderView}.
     */
    @UiThread
    public @NonNull ImplementationMode getSurfaceImplementationMode() {
        checkUiThread();
        return mCurrentImplementationMode;
    }

    /**
     * Applies a {@link ScaleType} to the viewfinder.
     *
     * <p> This value can also be set in the layout XML file via the {@code app:scaleType}
     * attribute.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * <p> This method should be called after {@link ViewfinderView} is inflated and can be
     * called before or after
     * {@link ViewfinderView#requestSurfaceSessionAsync(ViewfinderSurfaceRequest)}.
     * The {@link ScaleType} to set will be effective immediately after the method is called.
     *
     * @param scaleType The {@link ScaleType} to apply to the viewfinder.
     * @attr name app:scaleType
     */
    @UiThread
    public void setScaleType(final @NonNull ScaleType scaleType) {
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
    public @NonNull ScaleType getScaleType() {
        checkUiThread();
        return mViewfinderTransformation.getScaleType();
    }

    /**
     * Requests surface by sending a {@link ViewfinderSurfaceRequest}.
     *
     * <p> Only one request can be handled at the same time. If requesting a surface with
     * a new {@link ViewfinderSurfaceRequest}, or the previous request, the previous
     * returned {@link ListenableFuture} will be cancelled if it has not yet completed.
     *
     * <p> The result is a {@link ListenableFuture} of {@link ViewfinderSurfaceSession}, which
     * provides the functionality to attach listeners and propagate exceptions.
     *
     * <pre>{@code
     * ViewfinderSurfaceRequest request = new ViewfinderSurfaceRequest(width, height);
     *
     * ListenableFuture<ViewfinderSurfaceSession> sessionFuture =
     *     mViewfinder.requestSurfaceSessionAsync(request);
     *
     * Futures.addCallback(sessionFuture, new FutureCallback<ViewfinderSurfaceSession>() {
     *     {@literal @}Override
     *     public void onSuccess({@literal @}Nullable ViewfinderSurfaceSession session) {
     *         if (session != null) {
     *             createCaptureSession(session);
     *         }
     *     }
     *
     *     {@literal @}Override
     *     public void onFailure(Throwable t) {}
     * }, ContextCompat.getMainExecutor(getContext()));
     * }</pre>
     *
     * <p> If the source will produce frames that are rotated, mirrored, or require a crop, relative
     * to the display orientation, use {@link #setTransformationInfo(TransformationInfo)}.
     *
     * @param surfaceRequest The {@link ViewfinderSurfaceRequest} to get a surface session.
     * @return A {@link ListenableFuture} to retrieve the eventual surface session.
     * @see ViewfinderSurfaceRequest
     * @see ViewfinderSurfaceSession
     * @see TransformationInfo#DEFAULT
     */
    @UiThread
    public @NonNull ListenableFuture<ViewfinderSurfaceSession> requestSurfaceSessionAsync(
            @NonNull ViewfinderSurfaceRequest surfaceRequest) {
        checkUiThread();

        if (surfaceRequest.getImplementationMode() != null) {
            mCurrentImplementationMode = surfaceRequest.getImplementationMode();
        } else {
            mCurrentImplementationMode = mDefaultImplementationMode;
        }

        ViewfinderImplementation viewfinderImplementation =
                mCurrentImplementationMode == ImplementationMode.EMBEDDED
                        ? new TextureViewImplementation(
                        ViewfinderView.this, mViewfinderTransformation)
                        : new SurfaceViewImplementation(
                                ViewfinderView.this, mViewfinderTransformation);

        if (mImplementation != null && mImplementation != viewfinderImplementation) {
            mImplementation.onImplementationReplaced();
        }
        mImplementation = viewfinderImplementation;
        ListenableFuture<RefCounted<Surface>> surfaceFuture = CallbackToFutureAdapter.getFuture(
                (surfaceResponse) -> {
                    viewfinderImplementation.onSurfaceRequested(surfaceRequest,
                            surfaceResponse);
                    return "requestSurfaceSessionAsync(" + surfaceRequest + ")";
                }
        );

        Logger.d(TAG, "Surface requested by Viewfinder.");


        Display display = getDisplay();
        if (display != null) {
            Size resolution = new Size(surfaceRequest.getWidth(), surfaceRequest.getHeight());
            mViewfinderTransformation.setResolution(resolution);
            redrawViewfinder();
        }

        return Futures.transform(surfaceFuture, (refCountedSurface) -> {
            Surface surface = Objects.requireNonNull(refCountedSurface).acquire();
            if (surface != null) {
                return new ViewfinderSurfaceSessionImpl(
                        surface, surfaceRequest, () -> {
                    Objects.requireNonNull(refCountedSurface).release();
                    return Unit.INSTANCE;
                });
            } else {
                throw new CancellationException();
            }
        }, Runnable::run);
    }

    /**
     * Updates the {@link TransformationInfo} used by the current surface session.
     *
     * <p>This can be used to specify information related to how the source buffers are transformed
     * relative to the coordinate system of the source, such as rotation, mirroring, and crop
     * (region of interest).
     *
     * <p> If not set, the default value is {@link TransformationInfo#DEFAULT}.
     *
     * <p> This method should be called after {@link ViewfinderView} is inflated and can be called
     * before or after {@link ViewfinderView#requestSurfaceSessionAsync(ViewfinderSurfaceRequest)}.
     * The {@link TransformationInfo} will be effective immediately after the method is called.
     *
     * @param transformationInfo the updated transformation info.
     */
    @UiThread
    public void setTransformationInfo(@NonNull TransformationInfo transformationInfo) {
        mViewfinderTransformation.setTransformationInfo(transformationInfo);
        redrawViewfinder();
    }

    /**
     * Returns the {@link TransformationInfo} currently applied to the viewfinder.
     *
     * @return the previously set transformation info, or {@link TransformationInfo#DEFAULT} if none
     * has been set by {@link #setTransformationInfo(TransformationInfo)}.
     */
    @UiThread
    @NonNull
    public TransformationInfo getTransformationInfo() {
        return mViewfinderTransformation.getTransformationInfo();
    }

    /**
     * Returns a {@link Bitmap} representation of the content displayed on the
     * {@link ViewfinderView}, or {@code null} if the viewfinder hasn't started yet.
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
     * displayed on the {@link ViewfinderView}, or null if the viewfinder hasn't started yet.
     */
    @UiThread
    public @Nullable Bitmap getBitmap() {
        checkUiThread();
        return mImplementation == null ? null : mImplementation.getBitmap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
        startListeningToDisplayChange();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        stopListeningToDisplayChange();
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void redrawViewfinder() {
        if (mImplementation != null) {
            mImplementation.redrawViewfinder();
        }
    }

    /**
     * Checks if the current thread is the same UI thread on which the class was constructed.
     *
     * @see <a href="http://go/android-api-guidelines/concurrency#uithread">API Guidelines</a>
     */
    private void checkUiThread() {
        // Ignore mRequiredLooper == null because this can be called from the super
        // class constructor before the class's own constructor has run.
        if (Looper.myLooper() != mRequiredLooper) {
            Throwable throwable = new Throwable(
                    "A method was called on thread '" + Thread.currentThread().getName()
                            + "'. All methods must be called on the same thread. (Expected Looper "
                            + mRequiredLooper + ", but called on " + Looper.myLooper() + ".");
            throw new RuntimeException(throwable);
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

    private @Nullable DisplayManager getDisplayManager() {
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
                redrawViewfinder();
            }
        }
    }
}
