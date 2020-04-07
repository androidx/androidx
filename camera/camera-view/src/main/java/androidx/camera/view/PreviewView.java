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
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.view.preview.transform.PreviewTransform;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;

/**
 * Custom View that displays the camera feed for CameraX's Preview use case.
 *
 * <p> This class manages the Surface lifecycle, as well as the preview aspect ratio and
 * orientation. Internally, it uses either a {@link android.view.TextureView} or
 * {@link android.view.SurfaceView} to display the camera feed.
 */
public class PreviewView extends FrameLayout {

    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.SURFACE_VIEW;

    @NonNull
    private ImplementationMode mPreferredImplementationMode = DEFAULT_IMPL_MODE;

    @VisibleForTesting
    @Nullable
    PreviewViewImplementation mImplementation;

    @NonNull
    private PreviewTransform mPreviewTransform = new PreviewTransform();

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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
    }

    /**
     * Specifies the preferred {@link ImplementationMode} to use for preview.
     * <p>
     * When the preferred {@link ImplementationMode} is {@link ImplementationMode#SURFACE_VIEW}
     * but the device doesn't support this mode (e.g. devices with a supported camera hardware level
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
     * @param cameraInfo The {@link CameraInfo} of the camera that will use the
     *                   {@link android.view.Surface} provided by the returned
     *                   {@link Preview.SurfaceProvider}.
     * @return A {@link Preview.SurfaceProvider} used to start the camera preview.
     */
    @NonNull
    @UiThread
    public Preview.SurfaceProvider createSurfaceProvider(@Nullable CameraInfo cameraInfo) {
        Threads.checkMainThread();
        removeAllViews();

        final ImplementationMode actualImplementationMode = computeImplementationMode(cameraInfo,
                mPreferredImplementationMode);
        mImplementation = computeImplementation(actualImplementationMode);
        mImplementation.init(this, mPreviewTransform);
        return mImplementation.getSurfaceProvider();
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
        Preconditions.checkNotNull(mImplementation);
        return new PreviewViewMeteringPointFactory(getDisplay(), cameraSelector,
                mImplementation.getResolution(), mPreviewTransform.getScaleType(), getWidth(),
                getHeight());
    }

    @NonNull
    private ImplementationMode computeImplementationMode(@Nullable CameraInfo cameraInfo,
            @NonNull final ImplementationMode preferredMode) {
        return cameraInfo == null || cameraInfo.getImplementationType().equals(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY) ? ImplementationMode.TEXTURE_VIEW
                : preferredMode;
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

    /**
     * The implementation mode of a {@link PreviewView}
     *
     * <p>Specifies how the Preview surface will be implemented internally: Using a
     * {@link android.view.SurfaceView} or a {@link android.view.TextureView} (which is the default)
     * </p>
     */
    public enum ImplementationMode {
        /** Use a {@link android.view.SurfaceView} for the preview */
        SURFACE_VIEW,

        /** Use a {@link android.view.TextureView} for the preview */
        TEXTURE_VIEW
    }

    /** Options for scaling the preview vis-à-vis its container {@link PreviewView}. */
    public enum ScaleType {
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and align it to the top left corner of the view.
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_START(0),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and center it inside the view.
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_CENTER(1),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link PreviewView}, and align it to the bottom right corner of the view.
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link PreviewView}.
         */
        FILL_END(2),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and align it to the top left corner of the view.
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_START(3),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and center it inside the view.
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link PreviewView}.
         */
        FIT_CENTER(4),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link PreviewView}, and align it to the bottom right corner of the view.
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
}
