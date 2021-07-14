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
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarAppService;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.activity.renderer.surface.ISurfaceListener;
import androidx.car.app.activity.renderer.surface.OnBackPressedListener;
import androidx.car.app.activity.renderer.surface.SurfaceHolderListener;
import androidx.car.app.activity.renderer.surface.SurfaceWrapperProvider;
import androidx.car.app.activity.renderer.surface.TemplateSurfaceView;
import androidx.car.app.activity.ui.ErrorMessageView;
import androidx.car.app.activity.ui.LoadingView;
import androidx.car.app.automotive.R;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.ThreadUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/**
 * The class representing a car app activity.
 *
 * <p>This class is responsible for binding to the host and rendering the content given by its
 * {@link androidx.car.app.CarAppService}.
 *
 * <p>Usage of {@link CarAppActivity} is only required for applications targeting Automotive OS.
 *
 * <h4>Activity Declaration</h4>
 *
 * <p>The app must declare and export this {@link CarAppActivity} in their manifest. In order for
 * it to show up in the car's app launcher, it must include a {@link Intent#CATEGORY_LAUNCHER}
 * intent filter.
 *
 * For example:
 *
 * <pre>{@code
 * <activity
 *   android:name="androidx.car.app.activity.CarAppActivity"
 *   android:exported="true"
 *   android:label="@string/your_app_label">
 *
 *   <intent-filter>
 *     <action android:name="android.intent.action.MAIN" />
 *     <category android:name="android.intent.category.LAUNCHER" />
 *   </intent-filter>
 *   <meta-data android:name="distractionOptimized" android:value="true"/>
 * </activity>
 * }</pre>
 *
 * <p>See {@link androidx.car.app.CarAppService} for how to declare your app's
 * {@link CarAppService} in the manifest.
 *
 *
 * <h4>Distraction-optimized Activities</h4>
 *
 * <p>The activity must be the {@code distractionOptimized} meta-data set to {@code true}, in order
 * for it to be displayed while driving. This is the only activity that can have this meta-data
 * set to {@code true}, any other activities marked this way may cause the app to be rejected
 * during app submission.
 */
@SuppressLint({"ForbiddenSuperClass"})
public final class CarAppActivity extends FragmentActivity {

    @SuppressLint({"ActionValue"})
    @VisibleForTesting
    static final String ACTION_RENDER = "android.car.template.host.RendererService";

    TemplateSurfaceView mSurfaceView;
    ErrorMessageView mErrorMessageView;
    LoadingView mLoadingView;
    @Nullable SurfaceHolderListener mSurfaceHolderListener;
    @Nullable ActivityLifecycleDelegate mActivityLifecycleDelegate;
    @Nullable CarAppViewModel mViewModel;
    @Nullable OnBackPressedListener mOnBackPressedListener;
    @Nullable HostUpdateReceiver mHostUpdateReceiver;

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
                        Log.d(LogTags.TAG, "setSurfacePackage");
                        ThreadUtils.runOnMain(() -> mSurfaceView.setSurfacePackage(surfacePackage));
                    } catch (BundlerException e) {
                        Log.e(LogTags.TAG, "Unable to set surface package", e);
                        requireNonNull(mViewModel).onError(ErrorHandler.ErrorType.HOST_ERROR);
                    }
                }

                @Override
                public void registerRendererCallback(@NonNull IRendererCallback callback) {
                    requireNonNull(callback);
                    Log.d(LogTags.TAG, "registerRendererCallback");
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
                public void setSurfaceListener(@NonNull ISurfaceListener listener) {
                    requireNonNull(listener);
                    Log.d(LogTags.TAG, "setSurfaceListener");
                    ThreadUtils.runOnMain(
                            () -> requireNonNull(mSurfaceHolderListener)
                                    .setSurfaceListener(listener));
                }

                @Override
                public void onStartInput() {
                    Log.d(LogTags.TAG, "onStartInput");
                    ThreadUtils.runOnMain(() -> mSurfaceView.onStartInput());
                }

                @Override
                public void onStopInput() {
                    Log.d(LogTags.TAG, "onStopInput");
                    ThreadUtils.runOnMain(() -> mSurfaceView.onStopInput());
                }

                @Override
                public void startCarApp(@NonNull Intent intent) {
                    Log.d(LogTags.TAG, "startCarApp");
                    startActivity(intent);
                }

                @Override
                public void finishCarApp() {
                    Log.d(LogTags.TAG, "finishCarApp");
                    finish();
                }

                @Override
                public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
                        int newSelEnd) {
                    Log.d(LogTags.TAG, "onUpdateSelection");
                    ThreadUtils.runOnMain(() -> mSurfaceView.onUpdateSelection(oldSelStart,
                            oldSelEnd, newSelStart, newSelEnd));
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSoftInputHandling();
        setContentView(R.layout.activity_template);
        mSurfaceView = requireViewById(R.id.template_view_surface);
        mErrorMessageView = requireViewById(R.id.error_message_view);
        mLoadingView = requireViewById(R.id.loading_view);

        ComponentName serviceComponentName = retrieveServiceComponentName();
        if (serviceComponentName == null) {
            Log.e(LogTags.TAG, "Unspecified service class name");
            finish();
            return;
        }

        CarAppViewModelFactory factory = CarAppViewModelFactory.getInstance(getApplication(),
                serviceComponentName);
        mViewModel = new ViewModelProvider(this, factory).get(CarAppViewModel.class);
        mViewModel.setActivity(this);
        mViewModel.resetState();
        mViewModel.getError().observe(this, this::onErrorChanged);
        mViewModel.getState().observe(this, this::onStateChanged);

        mHostUpdateReceiver = new HostUpdateReceiver(mViewModel);
        mHostUpdateReceiver.register(this);
        mActivityLifecycleDelegate = new ActivityLifecycleDelegate(getServiceDispatcher());
        mSurfaceHolderListener = new SurfaceHolderListener(getServiceDispatcher(),
                new SurfaceWrapperProvider(mSurfaceView));

        registerActivityLifecycleCallbacks(requireNonNull(mActivityLifecycleDelegate));

        // Set the z-order to receive the UI events on the surface.
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceView.setServiceDispatcher(getServiceDispatcher());
        mSurfaceView.setViewModel(mViewModel);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderListener);

        mViewModel.bind(getIntent(), mCarActivity, getDisplayId());
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

    private void onErrorChanged(@Nullable ErrorHandler.ErrorType errorType) {
        ThreadUtils.runOnMain(() -> {
            mErrorMessageView.setError(errorType);
        });
    }

    private void onStateChanged(@NonNull CarAppViewModel.State state) {
        ThreadUtils.runOnMain(() -> {
            requireNonNull(mSurfaceView);
            requireNonNull(mSurfaceHolderListener);

            switch (state) {
                case IDLE:
                    mSurfaceView.setVisibility(View.GONE);
                    mSurfaceHolderListener.setSurfaceListener(null);
                    mErrorMessageView.setVisibility(View.GONE);
                    mLoadingView.setVisibility(View.GONE);
                    break;
                case ERROR:
                    mSurfaceView.setVisibility(View.GONE);
                    mSurfaceHolderListener.setSurfaceListener(null);
                    mErrorMessageView.setVisibility(View.VISIBLE);
                    mLoadingView.setVisibility(View.GONE);
                    break;
                case CONNECTING:
                    mSurfaceView.setVisibility(View.GONE);
                    mErrorMessageView.setVisibility(View.GONE);
                    mLoadingView.setVisibility(View.VISIBLE);
                    break;
                case CONNECTED:
                    mSurfaceView.setVisibility(View.VISIBLE);
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
        requireNonNull(mHostUpdateReceiver).unregister(this);
        requireNonNull(mViewModel).setActivity(null);
        super.onDestroy();
    }

    @Nullable
    private ComponentName retrieveServiceComponentName() {
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
