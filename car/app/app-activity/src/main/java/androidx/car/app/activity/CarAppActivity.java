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

import static android.content.pm.PackageManager.NameNotFoundException;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.activity.renderer.surface.ISurfaceListener;
import androidx.car.app.activity.renderer.surface.OnBackPressedListener;
import androidx.car.app.activity.renderer.surface.SurfaceHolderListener;
import androidx.car.app.activity.renderer.surface.SurfaceWrapperProvider;
import androidx.car.app.activity.renderer.surface.TemplateSurfaceView;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.ThreadUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * The class representing a car app activity.
 *
 * <p>This class is responsible for binding to the host and rendering the content given by a {@link
 * androidx.car.app.CarAppService}.
 *
 * <p>Usage of {@link CarAppActivity} is only required for applications targeting Automotive OS.
 *
 * <h4>Activity Declaration</h4>
 *
 * <p>The app must declare an {@code activity-alias} for a {@link CarAppActivity} providing its
 * associated {@link androidx.car.app.CarAppService} as meta-data. For example:
 *
 * <pre>{@code
 * <activity-alias
 *   android:enabled="true"
 *   android:exported="true"
 *   android:label="@string/your_app_label"
 *   android:name=".YourActivityAliasName"
 *   android:targetActivity="androidx.car.app.activity.CarAppActivity" >
 *   <intent-filter>
 *     <action android:name="android.intent.action.MAIN" />
 *     <category android:name="android.intent.category.LAUNCHER" />
 *   </intent-filter>
 *   <meta-data
 *     android:name="androidx.car.app.CAR_APP_SERVICE"
 *     android:value=".YourCarAppService" />
 *   <meta-data android:name="distractionOptimized" android:value="true"/>
 * </activity-alias>
 * }</pre>
 *
 * <p>See {@link androidx.car.app.CarAppService} for how to declare your app's car app service in
 * the manifest.
 *
 * <p>Note the name of the alias should be unique and resemble a fully qualified class name, but
 * unlike the name of the target activity, the alias name is arbitrary; it does not refer to an
 * actual class.
 */
// TODO(b/179225768): Remove distractionOptimized from the javadoc above if we can make that
// implicit for car apps.
@SuppressLint({"ForbiddenSuperClass"})
public final class CarAppActivity extends FragmentActivity {
    @VisibleForTesting
    static final String SERVICE_METADATA_KEY = "androidx.car.app.CAR_APP_SERVICE";

    @SuppressLint({"ActionValue"})
    @VisibleForTesting
    static final String ACTION_RENDER = "android.car.template.host.RendererService";

    TemplateSurfaceView mSurfaceView;
    @Nullable SurfaceHolderListener mSurfaceHolderListener;
    @Nullable ActivityLifecycleDelegate mActivityLifecycleDelegate;
    @Nullable OnBackPressedListener mOnBackPressedListener;
    @Nullable CarAppViewModel mViewModel;
    private int mDisplayId;

    /**
     * Handles the service connection errors by presenting a message the user and potentially
     * finishing the activity.
     */
    final ErrorHandler mErrorHandler = (errorType, exception) -> {
        requireNonNull(errorType);

        Log.e(LogTags.TAG, "Service error: " + errorType, exception);

        requireNonNull(mViewModel).unbind();

        ThreadUtils.runOnMain(() -> {
            Log.d(LogTags.TAG, "Showing error fragment");

            if (mSurfaceView != null) {
                mSurfaceView.setVisibility(View.GONE);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(
                            R.id.error_message_container,
                            ErrorMessageFragment.newInstance(errorType))
                    .commit();
        });
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
                        mErrorHandler.onError(ErrorHandler.ErrorType.HOST_ERROR, e);
                    }
                }

                @Override
                public void registerRendererCallback(@NonNull IRendererCallback callback) {
                    requireNonNull(callback);
                    ThreadUtils.runOnMain(
                            () -> {
                                mSurfaceView.setOnCreateInputConnectionListener(editorInfo ->
                                        getServiceDispatcher().fetch(null, () ->
                                                callback.onCreateInputConnection(
                                                        editorInfo)));

                                mOnBackPressedListener = () ->
                                        getServiceDispatcher().dispatch(callback::onBackPressed);

                                requireNonNull(mActivityLifecycleDelegate)
                                        .registerRendererCallback(callback);
                                requireNonNull(mViewModel).setRendererCallback(callback);
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
            };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);
        mSurfaceView = requireViewById(R.id.template_view_surface);

        ComponentName serviceComponentName = retrieveServiceComponentName();
        if (serviceComponentName == null) {
            Log.e(LogTags.TAG, "Unspecified service class name");
            finish();
            return;
        }
        mDisplayId = getWindowManager().getDefaultDisplay().getDisplayId();

        CarAppViewModelFactory factory = CarAppViewModelFactory.getInstance(getApplication(),
                serviceComponentName);
        mViewModel = new ViewModelProvider(this, factory).get(CarAppViewModel.class);
        mViewModel.getErrorEvent().observe(this,
                errorEvent -> mErrorHandler.onError(errorEvent.getErrorType(),
                        errorEvent.getException()));

        mActivityLifecycleDelegate = new ActivityLifecycleDelegate(getServiceDispatcher());
        mSurfaceHolderListener = new SurfaceHolderListener(getServiceDispatcher(),
                new SurfaceWrapperProvider(mSurfaceView));

        registerActivityLifecycleCallbacks(requireNonNull(mActivityLifecycleDelegate));

        // Set the z-order to receive the UI events on the surface.
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceView.setServiceDispatcher(getServiceDispatcher());
        mSurfaceView.setErrorHandler(mErrorHandler);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderListener);

        mViewModel.bind(getIntent(), mCarActivity, mDisplayId);
    }

    @Override
    public void onBackPressed() {
        if (mOnBackPressedListener != null) {
            mOnBackPressedListener.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        requireNonNull(mViewModel).bind(intent, mCarActivity, mDisplayId);
    }

    @VisibleForTesting
    int getDisplayId() {
        return mDisplayId;
    }

    @VisibleForTesting
    ServiceDispatcher getServiceDispatcher() {
        return requireNonNull(mViewModel).getServiceDispatcher();
    }

    @Nullable
    private ComponentName retrieveServiceComponentName() {
        ActivityInfo activityInfo = null;
        try {
            activityInfo =
                    getPackageManager()
                            .getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            Log.e(LogTags.TAG, "Unable to find component: " + getComponentName(), e);
        }

        if (activityInfo == null) {
            return null;
        }

        String serviceName = activityInfo.metaData.getString(SERVICE_METADATA_KEY);
        if (serviceName == null) {
            Log.e(
                    LogTags.TAG,
                    "Unable to find required metadata tag with name "
                            + SERVICE_METADATA_KEY
                            + ". App manifest must include metadata tag with name "
                            + SERVICE_METADATA_KEY
                            + " and the name of the car app service as the value");
            return null;
        }

        return new ComponentName(this, serviceName);
    }
}
