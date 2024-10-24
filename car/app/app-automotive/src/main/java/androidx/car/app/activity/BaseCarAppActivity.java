/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

import static androidx.car.app.CarAppService.SERVICE_INTERFACE;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.SessionInfo;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IInsetsListener;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.activity.renderer.surface.ISurfaceListener;
import androidx.car.app.activity.renderer.surface.OnBackPressedListener;
import androidx.car.app.activity.renderer.surface.SurfaceHolderListener;
import androidx.car.app.activity.renderer.surface.SurfaceWrapperProvider;
import androidx.car.app.activity.renderer.surface.TemplateSurfaceView;
import androidx.car.app.activity.ui.ErrorMessageView;
import androidx.car.app.activity.ui.LoadingView;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.automotive.R;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.ThreadUtils;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Core logic for CarAppLibrary Activity interaction with a host.
 *
 * <p> This base class handles binding and providing the UI surface of the real activity to the
 * host, and responds to instruction by the host to display content. 3P must not change the layout;
 * otherwise the host does not guarantee the right execution.
 */
@SuppressLint({"ForbiddenSuperClass"})
public abstract class BaseCarAppActivity extends FragmentActivity {

    TemplateSurfaceView mSurfaceView;
    ErrorMessageView mErrorMessageView;
    LoadingView mLoadingView;
    View mActivityContainerView;
    View mLocalContentContainerView;

    boolean mDecorFitsSystemWindows = false;

    /**
     * Displays the snapshot of the surface view to avoid a visual glitch when app comes
     * to foreground. This view sits behind the surface view and will be visible only when surface
     * is hidden (or not created yet).
     */
    ImageView mSurfaceSnapshotView;

    // The handler used to take surface view snapshot.
    private final Handler mSnapshotHandler = new Handler(Looper.myLooper());

    @Nullable SurfaceHolderListener mSurfaceHolderListener;
    @Nullable ActivityLifecycleDelegate mActivityLifecycleDelegate;
    @Nullable CarAppViewModel mViewModel;
    @Nullable OnBackPressedListener mOnBackPressedListener;
    @Nullable HostUpdateReceiver mHostUpdateReceiver;

    /**
     * A listener to conditionally send insets to the host, or handle them locally if the host
     * is not capable.
     */
    private final View.OnApplyWindowInsetsListener mWindowInsetsListener =
            new View.OnApplyWindowInsetsListener() {
                @Override
                public @Nullable WindowInsets onApplyWindowInsets(@NonNull View view,
                        @NonNull WindowInsets windowInsets) {
                    // Do not report inset changes if the activity is not in resumed state.
                    // Reporting the inset changes when the app is going away results in visible
                    // rescaling of certain UI elements such as maps right before app goes to the
                    // background. These inset changes then need to be corrected again once the
                    // app comes to the foreground resulting with another rescaling of the
                    // screen which is not desired.
                    if (getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
                        return WindowInsetsCompat.CONSUMED.toWindowInsets();
                    }

                    // IMPORTANT: The insets calculated here must match the windowing settings in
                    // SystemUiVisibility set in CarAppActivity#onCreate(). Failing to do so would
                    // cause a mismatch between the insets applied to the content on the hosts side
                    // vs. the actual visible window available on the client side.
                    Insets insets;
                    if (Build.VERSION.SDK_INT >= 30) {
                        insets = Api30Impl.getInsets(windowInsets);
                    } else {
                        insets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
                                .getInsets(WindowInsetsCompat.Type.systemBars()
                                        | WindowInsetsCompat.Type.ime())
                                .toPlatformInsets();
                    }
                    DisplayCutoutCompat displayCutout =
                            WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
                                    .getDisplayCutout();
                    requireNonNull(mViewModel).updateWindowInsets(insets, displayCutout);

                    // Insets are handled by the host. Only local content need padding.
                    mActivityContainerView.setPadding(0, 0, 0, 0);
                    mLocalContentContainerView.setPadding(insets.left, insets.top,
                            insets.right, insets.bottom);

                    return WindowInsetsCompat.CONSUMED.toWindowInsets();
                }
            };

    /**
     * {@link ICarAppActivity} implementation that allows the {@link IRendererService} to
     * communicate with this {@link CarAppActivity}.
     */
    private final ICarAppActivity.Stub mCarActivity =
            new ICarAppActivity.Stub() {
                @Override
                public void setSurfacePackage(@NonNull Bundleable bundleable) {
                    requireNonNull(bundleable);
                    try {
                        Object surfacePackage = bundleable.get();
                        ThreadUtils.runOnMain(() -> mSurfaceView.setSurfacePackage(surfacePackage));
                    } catch (BundlerException e) {
                        Log.e(LogTags.TAG, "Unable to set surface package", e);
                        requireNonNull(mViewModel).onError(ErrorHandler.ErrorType.HOST_ERROR);
                    }
                }

                @Override
                public void registerRendererCallback(@NonNull IRendererCallback callback) {
                    requireNonNull(callback);
                    ThreadUtils.runOnMain(
                            () -> {
                                mSurfaceView.setOnCreateInputConnectionListener(editorInfo ->
                                        getServiceDispatcher().fetch("OnCreateInputConnection",
                                                null,
                                                () -> callback.onCreateInputConnection(editorInfo))
                                );

                                mOnBackPressedListener = () ->
                                        getServiceDispatcher().dispatch("onBackPressed",
                                                callback::onBackPressed);

                                requireNonNull(mActivityLifecycleDelegate)
                                        .registerRendererCallback(callback);
                                requireNonNull(mViewModel).setRendererCallback(callback);
                            });
                }

                @Override
                public void setInsetsListener(@NonNull IInsetsListener listener) {
                    requireNonNull(listener);
                    ThreadUtils.runOnMain(
                            () -> {
                                requireNonNull(mViewModel).setInsetsListener(listener);
                                // We need to adjust local insets now that we know the host will
                                // take care of them.
                                mActivityContainerView.requestApplyInsets();
                            });
                }

                @Override
                public void setSurfaceListener(@NonNull ISurfaceListener listener) {
                    requireNonNull(listener);
                    ThreadUtils.runOnMain(
                            () -> requireNonNull(mSurfaceHolderListener)
                                    .setSurfaceListener(listener));
                }

                @Override
                public void onStartInput() {
                    ThreadUtils.runOnMain(() -> mSurfaceView.onStartInput());
                }

                @Override
                public void onStopInput() {
                    ThreadUtils.runOnMain(() -> mSurfaceView.onStopInput());
                }

                @Override
                public void startCarApp(@NonNull Intent intent) {
                    startActivity(intent);
                }

                @Override
                public void finishCarApp() {
                    finish();
                }

                @Override
                public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
                        int newSelEnd) {
                    ThreadUtils.runOnMain(() -> mSurfaceView.onUpdateSelection(oldSelStart,
                            oldSelEnd, newSelStart, newSelEnd));
                }

                @Override
                public void showAssist(Bundle args) {
                    BaseCarAppActivity.this.showAssist(args);
                }
            };

    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        private Api30Impl() {
        }

        static Insets getInsets(WindowInsets windowInsets) {
            return windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
        }

        static WindowInsets getDecorViewInsets(WindowInsets insets) {
            return new WindowInsets.Builder(insets).setInsets(
                    WindowInsets.Type.displayCutout(), Insets.NONE).build();
        }

        static void setDecorFitsSystemWindows(BaseCarAppActivity activity, Window window,
                boolean decorFitsSystemWindows) {
            // Set mDecorFitsSystemWindows so we can retrieve its value for testing.
            activity.mDecorFitsSystemWindows = decorFitsSystemWindows;
            window.setDecorFitsSystemWindows(decorFitsSystemWindows);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSoftInputHandling();
        setContentView(R.layout.activity_template);
        mActivityContainerView = requireViewById(R.id.activity_container);
        mLocalContentContainerView = requireViewById(R.id.local_content_container);
        mSurfaceView = requireViewById(R.id.template_view_surface);
        mErrorMessageView = requireViewById(R.id.error_message_view);
        mLoadingView = requireViewById(R.id.loading_view);
        mSurfaceSnapshotView = requireViewById(R.id.template_view_snapshot);

    }

    /**
     * Binds the {@link BaseCarAppActivity} and it's view against the view model.
     */
    public void bindToViewModel(@NonNull SessionInfo sessionInfo) {
        ComponentName serviceComponentName = getServiceComponentName();
        if (serviceComponentName == null) {
            Log.e(LogTags.TAG, "Unspecified service class name");
            finish();
            return;
        }
        initializeViewModel(serviceComponentName, sessionInfo);

        mHostUpdateReceiver = new HostUpdateReceiver(requireNonNull(mViewModel));
        mHostUpdateReceiver.register(this);
        mActivityLifecycleDelegate = new ActivityLifecycleDelegate(getServiceDispatcher());
        mSurfaceHolderListener = new SurfaceHolderListener(getServiceDispatcher(),
                new SurfaceWrapperProvider(mSurfaceView));

        registerActivityLifecycleCallbacks(requireNonNull(mActivityLifecycleDelegate));

        configureSurfaceView();
        //View Model State Observations requires a non null mSurfaceHolderListener
        viewModelObserveAndBind();
        // Inset Changes require view model to be initialized
        observeInsetChanges();
    }

    private void initializeViewModel(ComponentName serviceComponentName, SessionInfo sessionInfo) {
        CarAppViewModelFactory factory = CarAppViewModelFactory.getInstance(getApplication(),
                serviceComponentName, sessionInfo);
        mViewModel = new ViewModelProvider(this, factory).get(CarAppViewModel.class);
        mViewModel.setActivity(this);
        mViewModel.resetState();
    }

    private void configureSurfaceView() {
        // Set the z-order to receive the UI events on the surface.
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceView.setServiceDispatcher(getServiceDispatcher());
        mSurfaceView.setViewModel(requireNonNull(mViewModel));
        mSurfaceView.getHolder().addCallback(mSurfaceHolderListener);
    }

    private void viewModelObserveAndBind() {
        requireNonNull(mViewModel).getError().observe(this, this::onErrorChanged);
        requireNonNull(mViewModel).getState().observe(this, this::onStateChanged);
        requireNonNull(mViewModel).bind(getIntent(), mCarActivity, getDisplayId());
    }

    private void observeInsetChanges() {
        mActivityContainerView.setOnApplyWindowInsetsListener(mWindowInsetsListener);

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

        // Remove display cut-out insets on DecorView
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                insets = Api30Impl.getDecorViewInsets(insets);
            }
            return view.onApplyWindowInsets(insets);
        });

        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.setDecorFitsSystemWindows(this, getWindow(), false);
        } else {
            getWindow().getDecorView().setFitsSystemWindows(false);
        }
        mActivityContainerView.requestApplyInsets();
    }

    /**
     * TODO(b/283985939): Workaround for testing {@code setDecorFitsSystemWindows} for older
     * versions of Roboelectric that don't support {@code getDecorFitsSystemWindows}. Remove this
     * once Roboelectric version is upgraded to v4.10.3.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean getDecorFitsSystemWindows() {
        return mDecorFitsSystemWindows;
    }

    /** Takes a snapshot of the surface view and puts it in the surfaceSnapshotView if succeeded. */
    private void takeSurfaceSnapshot() {
        // Nothing to do if the surface is not ready yet.
        if (mSurfaceView.getHolder().getSurface() == null
                || mSurfaceView.getWidth() == 0 || mSurfaceView.getHeight() == 0) {
            return;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            PixelCopy.request(mSurfaceView, bitmap, status -> {
                if (status == PixelCopy.SUCCESS) {
                    mSurfaceSnapshotView.setImageBitmap(bitmap);
                } else {
                    Log.w(LogTags.TAG, "Failed to take snapshot of the surface view");
                    mSurfaceSnapshotView.setImageBitmap(null);
                }
            }, mSnapshotHandler);
        } catch (Exception e) {
            Log.e(LogTags.TAG, "Failed to take snapshot of the surface view", e);
            mSurfaceSnapshotView.setImageBitmap(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        takeSurfaceSnapshot();
    }

    // TODO(b/189862860): Address SOFT_INPUT_ADJUST_RESIZE deprecation
    @SuppressWarnings("deprecation")
    private void setSoftInputHandling() {
        getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onBackPressed() {
        if (mOnBackPressedListener != null) {
            mOnBackPressedListener.onBackPressed();
        }
    }

    private void onErrorChanged(ErrorHandler.@Nullable ErrorType errorType) {
        ThreadUtils.runOnMain(() -> mErrorMessageView.setError(errorType));
    }

    private void onStateChanged(CarAppViewModel.@NonNull State state) {
        ThreadUtils.runOnMain(() -> {
            requireNonNull(mSurfaceView);
            requireNonNull(mSurfaceSnapshotView);
            requireNonNull(mSurfaceHolderListener);

            switch (state) {
                case IDLE:
                    mSurfaceView.setVisibility(View.GONE);
                    mSurfaceSnapshotView.setVisibility(View.VISIBLE);
                    mSurfaceHolderListener.setSurfaceListener(null);
                    mErrorMessageView.setVisibility(View.GONE);
                    mLoadingView.setVisibility(View.GONE);
                    break;
                case ERROR:
                    mSurfaceView.setVisibility(View.GONE);
                    mSurfaceSnapshotView.setVisibility(View.GONE);
                    mSurfaceHolderListener.setSurfaceListener(null);
                    mErrorMessageView.setVisibility(View.VISIBLE);
                    mLoadingView.setVisibility(View.GONE);
                    break;
                case CONNECTING:
                    mSurfaceView.setVisibility(View.GONE);
                    mSurfaceSnapshotView.setVisibility(View.VISIBLE);
                    mErrorMessageView.setVisibility(View.GONE);
                    mLoadingView.setVisibility(View.VISIBLE);
                    break;
                case CONNECTED:
                    mSurfaceView.setVisibility(View.VISIBLE);
                    mSurfaceSnapshotView.setVisibility(View.VISIBLE);
                    mErrorMessageView.setVisibility(View.GONE);
                    mLoadingView.setVisibility(View.GONE);
                    break;
            }
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        requireNonNull(mSurfaceHolderListener).setSurfaceListener(null);
        requireNonNull(mActivityLifecycleDelegate).registerRendererCallback(null);

        requireNonNull(mViewModel).bind(intent, mCarActivity, getDisplayId());
    }

    // TODO(b/189864400): Address WindowManager#getDefaultDisplay() deprecation
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    int getDisplayId() {
        return getWindowManager().getDefaultDisplay().getDisplayId();
    }

    @VisibleForTesting
    ServiceDispatcher getServiceDispatcher() {
        return requireNonNull(mViewModel).getServiceDispatcher();
    }

    @Override
    protected void onDestroy() {
        if (mHostUpdateReceiver != null) {
            mHostUpdateReceiver.unregister(this);
        }
        if (mSurfaceHolderListener != null) {
            mSurfaceHolderListener.setSurfaceListener(null);
        }
        if (mViewModel != null) {
            mViewModel.setActivity(null);
        }
        super.onDestroy();
    }

    /**
     * @see #getServiceComponentName()
     */
    @ExperimentalCarApi
    public @Nullable ComponentName retrieveServiceComponentName() {
        return getServiceComponentName();
    }

    /**
     * Retrieves the {@link  ComponentName} to which the view model will talk
     * in order to render.
     */
    @SuppressWarnings("deprecation")
    public @Nullable ComponentName getServiceComponentName() {
        Intent intent = new Intent(SERVICE_INTERFACE);
        intent.setPackage(getPackageName());
        List<ResolveInfo> infos = getPackageManager().queryIntentServices(intent, 0);
        if (infos == null || infos.isEmpty()) {
            Log.e(LogTags.TAG, "Unable to find required " + SERVICE_INTERFACE
                    + " implementation. App manifest must include exactly one car app service.");
            return null;
        } else if (infos.size() != 1) {
            Log.e(LogTags.TAG, "Found more than one " + SERVICE_INTERFACE
                    + " implementation. App manifest must include exactly one car app service.");
            return null;
        }
        String serviceName = infos.get(0).serviceInfo.name;
        return new ComponentName(this, serviceName);
    }
}