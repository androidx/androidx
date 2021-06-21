/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Activity which runs the camera preview with opengl processing */
public class OpenGLActivity extends AppCompatActivity {
    private static final String TAG = "OpenGLActivity";

    /**
     * Intent Extra string for choosing which Camera implementation to use.
     */
    public static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation";

    /**
     * Intent Extra string for choosing which type of render surface to use to display Preview.
     */
    public static final String INTENT_EXTRA_RENDER_SURFACE_TYPE = "render_surface_type";
    /**
     * TextureView render surface for {@link OpenGLActivity#INTENT_EXTRA_RENDER_SURFACE_TYPE}.
     * This is the default render surface.
     */
    public static final String RENDER_SURFACE_TYPE_TEXTUREVIEW = "textureview";
    /**
     * SurfaceView render surface for {@link OpenGLActivity#INTENT_EXTRA_RENDER_SURFACE_TYPE}.
     * This type will block the main thread while detaching it's {@link Surface} from the OpenGL
     * renderer to avoid compatibility issues on some devices.
     */
    public static final String RENDER_SURFACE_TYPE_SURFACEVIEW = "surfaceview";
    /**
     * SurfaceView render surface (in non-blocking mode) for
     * {@link OpenGLActivity#INTENT_EXTRA_RENDER_SURFACE_TYPE}. This type will NOT
     * block the main thread while detaching it's {@link Surface} from the OpenGL
     * renderer, but some devices may crash due to their OpenGL/EGL implementation not being
     * thread-safe.
     */
    public static final String RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING =
            "surfaceview_nonblocking";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
            };

    private static final int FPS_NUM_SAMPLES = 10;
    private OpenGLRenderer mRenderer;
    private DisplayManager.DisplayListener mDisplayListener;
    private ProcessCameraProvider mCameraProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.opengl_activity);

        OpenGLRenderer renderer = mRenderer = new OpenGLRenderer();
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        View viewFinder = OpenGLActivity.chooseViewFinder(getIntent().getExtras(), viewFinderStub,
                renderer);

        // Add a frame update listener to display FPS
        FpsRecorder fpsRecorder = new FpsRecorder(FPS_NUM_SAMPLES);
        TextView fpsCounterView = findViewById(R.id.fps_counter);
        renderer.setFrameUpdateListener(ContextCompat.getMainExecutor(this), timestamp -> {
            double fps = fpsRecorder.recordTimestamp(timestamp);
            fpsCounterView.setText(getString(R.string.fps_counter_template,
                    (Double.isNaN(fps) || Double.isInfinite(fps)) ? "---" : String.format(Locale.US,
                            "%.0f", fps)));
        });

        // A display listener is needed when the phone rotates 180 degrees without stopping at a
        // 90 degree increment. In these cases, onCreate() isn't triggered, so we need to ensure
        // the output surface uses the correct orientation.
        mDisplayListener =
                new DisplayListener() {
                    @Override
                    public void onDisplayAdded(int displayId) {
                    }

                    @Override
                    public void onDisplayRemoved(int displayId) {
                    }

                    @Override
                    public void onDisplayChanged(int displayId) {
                        Display viewFinderDisplay = viewFinder.getDisplay();
                        if (viewFinderDisplay != null
                                && viewFinderDisplay.getDisplayId() == displayId) {
                            renderer.invalidateSurface(Surfaces.toSurfaceRotationDegrees(
                                    viewFinderDisplay.getRotation()));
                        }
                    }
                };

        DisplayManager dpyMgr =
                Objects.requireNonNull((DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
        dpyMgr.registerDisplayListener(mDisplayListener, new Handler(Looper.getMainLooper()));

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            if (cameraImplementation != null) {
                CameraXViewModel.configureCameraProvider(cameraImplementation);
            }
        }

        CameraXViewModel viewModel = new ViewModelProvider(this).get(CameraXViewModel.class);
        viewModel
                .getCameraProvider()
                .observe(
                        this,
                        cameraProviderResult -> {
                            if (cameraProviderResult.hasProvider()) {
                                mCameraProvider = cameraProviderResult.getProvider();
                                if (allPermissionsGranted()) {
                                    startCamera();
                                }
                            } else {
                                Log.e(TAG, "Failed to retrieve ProcessCameraProvider",
                                        cameraProviderResult.getError());
                                Toast.makeText(getApplicationContext(),
                                        "Unable to initialize CameraX. See logs "
                                                + "for details.", Toast.LENGTH_LONG).show();
                            }
                        });

        if (!allPermissionsGranted()) {
            mRequestPermissions.launch(REQUIRED_PERMISSIONS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DisplayManager dpyMgr = Objects.requireNonNull(
                (DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
        dpyMgr.unregisterDisplayListener(mDisplayListener);
        mRenderer.shutdown();
    }

    /**
     * Chooses the type of view to use for the viewfinder based on intent extras.
     *
     * @param intentExtras   Optional extras which can contain an extra with key
     *                       {@link #INTENT_EXTRA_RENDER_SURFACE_TYPE}. Possible values are one of
     *                       {@link #RENDER_SURFACE_TYPE_TEXTUREVIEW},
     *                       {@link #RENDER_SURFACE_TYPE_SURFACEVIEW}, or
     *                       {@link #RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING}. If {@code null},
     *                       or the bundle does not contain a surface type, then
     *                       {@link #RENDER_SURFACE_TYPE_TEXTUREVIEW} will be used.
     * @param viewFinderStub The stub to inflate the chosen viewfinder into.
     * @param renderer       The {@link OpenGLRenderer} which will render frames into the
     *                       viewfinder.
     * @return The inflated viewfinder View.
     */
    public static View chooseViewFinder(@Nullable Bundle intentExtras,
            @NonNull ViewStub viewFinderStub,
            @NonNull OpenGLRenderer renderer) {
        // By default we choose TextureView to maximize compatibility.
        String renderSurfaceType = OpenGLActivity.RENDER_SURFACE_TYPE_TEXTUREVIEW;
        if (intentExtras != null) {
            renderSurfaceType = intentExtras.getString(INTENT_EXTRA_RENDER_SURFACE_TYPE,
                    RENDER_SURFACE_TYPE_TEXTUREVIEW);
        }

        switch (renderSurfaceType) {
            case RENDER_SURFACE_TYPE_TEXTUREVIEW:
                Log.d(TAG, "Using TextureView render surface.");
                return TextureViewRenderSurface.inflateWith(viewFinderStub, renderer);
            case RENDER_SURFACE_TYPE_SURFACEVIEW:
                Log.d(TAG, "Using SurfaceView render surface.");
                return SurfaceViewRenderSurface.inflateWith(viewFinderStub, renderer);
            case RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING:
                Log.d(TAG, "Using SurfaceView (non-blocking) render surface.");
                return SurfaceViewRenderSurface.inflateNonBlockingWith(viewFinderStub, renderer);
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Unknown render "
                                + "surface type: %s. Supported surface types include: [%s, %s, %s]",
                        renderSurfaceType, RENDER_SURFACE_TYPE_TEXTUREVIEW,
                        RENDER_SURFACE_TYPE_SURFACEVIEW,
                        RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING));
        }
    }

    private void startCamera() {
        // Keep screen on for this app. This is just for convenience, and is not required.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set the aspect ratio of Preview to match the aspect ratio of the view finder (defined
        // with ConstraintLayout).
        Preview preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();

        mRenderer.attachInputPreview(preview).addListener(() -> {
            Log.d(TAG, "OpenGLRenderer get the new surface for the Preview");
        }, ContextCompat.getMainExecutor(this));
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        mCameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    // **************************** Permission handling code start *******************************//
    private final ActivityResultLauncher<String[]> mRequestPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    new ActivityResultCallback<Map<String, Boolean>>() {
                        @Override
                        public void onActivityResult(Map<String, Boolean> result) {
                            for (String permission : REQUIRED_PERMISSIONS) {
                                if (!Objects.requireNonNull(result.get(permission))) {
                                    Toast.makeText(OpenGLActivity.this, "Permissions not granted",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }

                            // All permissions granted.
                            if (mCameraProvider != null) {
                                startCamera();
                            }
                        }
                    });

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    // **************************** Permission handling code end *********************************//
}
