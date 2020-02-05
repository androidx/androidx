/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.activity;

import static android.os.Build.VERSION.SDK_INT;

import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.ACTION_REQUEST_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.EXTRA_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestPermissions.EXTRA_PERMISSION_GRANT_RESULTS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ReportFragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for activities that enables composition of higher level components.
 * <p>
 * Rather than all functionality being built directly into this class, only the minimal set of
 * lower level building blocks are included. Higher level components can then be used as needed
 * without enforcing a deep Activity class hierarchy or strong coupling between components.
 */
public class ComponentActivity extends androidx.core.app.ComponentActivity implements
        LifecycleOwner,
        ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner,
        ActivityResultCaller {

    static final class NonConfigurationInstances {
        Object custom;
        ViewModelStore viewModelStore;
    }

    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private final SavedStateRegistryController mSavedStateRegistryController =
            SavedStateRegistryController.create(this);

    // Lazily recreated from NonConfigurationInstances by getViewModelStore()
    private ViewModelStore mViewModelStore;
    private ViewModelProvider.Factory mDefaultFactory;

    private final OnBackPressedDispatcher mOnBackPressedDispatcher =
            new OnBackPressedDispatcher(new Runnable() {
                @Override
                public void run() {
                    // Calling onBackPressed() on an Activity with its state saved can cause an
                    // error on devices on API levels before 26. We catch that specific error and
                    // throw all others.
                    try {
                        ComponentActivity.super.onBackPressed();
                    } catch (IllegalStateException e) {
                        if (!TextUtils.equals(e.getMessage(),
                                "Can not perform this action after onSaveInstanceState")) {
                            throw e;
                        }
                    }
                }
            });

    @LayoutRes
    private int mContentLayoutId;

    private final AtomicInteger mNextLocalRequestCode = new AtomicInteger();

    private ActivityResultRegistry mActivityResultRegistry = new ActivityResultRegistry() {

        @Override
        public <I, O> void invoke(
                final int requestCode,
                @NonNull ActivityResultContract<I, O> contract,
                I input) {
            Intent intent = contract.createIntent(input);
            if (ACTION_REQUEST_PERMISSIONS.equals(intent.getAction())) {
                String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);

                if (SDK_INT < Build.VERSION_CODES.M || permissions == null) {
                    return;
                }

                List<String> nonGrantedPermissions = new ArrayList<>();
                for (String permission : permissions) {
                    if (checkPermission(permission,
                            android.os.Process.myPid(), android.os.Process.myUid())
                            != PackageManager.PERMISSION_GRANTED) {
                        nonGrantedPermissions.add(permission);
                    }
                }

                if (!nonGrantedPermissions.isEmpty()) {
                    requestPermissions(nonGrantedPermissions.toArray(
                            new String[nonGrantedPermissions.size()]), requestCode);
                }
            } else {
                ComponentActivity.this.startActivityForResult(intent, requestCode);
            }
        }
    };

    /**
     * Default constructor for ComponentActivity. All Activities must have a default constructor
     * for API 27 and lower devices or when using the default
     * {@link android.app.AppComponentFactory}.
     */
    public ComponentActivity() {
        Lifecycle lifecycle = getLifecycle();
        //noinspection ConstantConditions
        if (lifecycle == null) {
            throw new IllegalStateException("getLifecycle() returned null in ComponentActivity's "
                    + "constructor. Please make sure you are lazily constructing your Lifecycle "
                    + "in the first call to getLifecycle() rather than relying on field "
                    + "initialization.");
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source,
                        @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        Window window = getWindow();
                        final View decor = window != null ? window.peekDecorView() : null;
                        if (decor != null) {
                            decor.cancelPendingInputEvents();
                        }
                    }
                }
            });
        }
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!isChangingConfigurations()) {
                        getViewModelStore().clear();
                    }
                }
            }
        });

        if (19 <= SDK_INT && SDK_INT <= 23) {
            getLifecycle().addObserver(new ImmLeaksCleaner(this));
        }
    }

    /**
     * Alternate constructor that can be used to provide a default layout
     * that will be inflated as part of <code>super.onCreate(savedInstanceState)</code>.
     *
     * <p>This should generally be called from your constructor that takes no parameters,
     * as is required for API 27 and lower or when using the default
     * {@link android.app.AppComponentFactory}.
     *
     * @see #ComponentActivity()
     */
    @ContentView
    public ComponentActivity(@LayoutRes int contentLayoutId) {
        this();
        mContentLayoutId = contentLayoutId;
    }

    /**
     * {@inheritDoc}
     *
     * If your ComponentActivity is annotated with {@link ContentView}, this will
     * call {@link #setContentView(int)} for you.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedStateRegistryController.performRestore(savedInstanceState);
        mActivityResultRegistry.onRestoreInstanceState(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
        if (mContentLayoutId != 0) {
            setContentView(mContentLayoutId);
        }
    }

    @CallSuper
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Lifecycle lifecycle = getLifecycle();
        if (lifecycle instanceof LifecycleRegistry) {
            ((LifecycleRegistry) lifecycle).setCurrentState(Lifecycle.State.CREATED);
        }
        super.onSaveInstanceState(outState);
        mSavedStateRegistryController.performSave(outState);
        mActivityResultRegistry.onSaveInstanceState(outState);
    }

    /**
     * Retain all appropriate non-config state.  You can NOT
     * override this yourself!  Use a {@link androidx.lifecycle.ViewModel} if you want to
     * retain your own non config state.
     */
    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public final Object onRetainNonConfigurationInstance() {
        // Maintain backward compatibility.
        Object custom = onRetainCustomNonConfigurationInstance();

        ViewModelStore viewModelStore = mViewModelStore;
        if (viewModelStore == null) {
            // No one called getViewModelStore(), so see if there was an existing
            // ViewModelStore from our last NonConfigurationInstance
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                viewModelStore = nc.viewModelStore;
            }
        }

        if (viewModelStore == null && custom == null) {
            return null;
        }

        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.custom = custom;
        nci.viewModelStore = viewModelStore;
        return nci;
    }

    /**
     * Use this instead of {@link #onRetainNonConfigurationInstance()}.
     * Retrieve later with {@link #getLastCustomNonConfigurationInstance()}.
     *
     * @deprecated Use a {@link androidx.lifecycle.ViewModel} to store non config state.
     */
    @Deprecated
    @Nullable
    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    /**
     * Return the value previously returned from
     * {@link #onRetainCustomNonConfigurationInstance()}.
     *
     * @deprecated Use a {@link androidx.lifecycle.ViewModel} to store non config state.
     */
    @Deprecated
    @Nullable
    public Object getLastCustomNonConfigurationInstance() {
        NonConfigurationInstances nc = (NonConfigurationInstances)
                getLastNonConfigurationInstance();
        return nc != null ? nc.custom : null;
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // Set the VTLO before setting the content view so that the inflation process
        // and attach listeners will see it already present
        ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view) {
        // Set the VTLO before setting the content view so that attach listeners
        // will see it already present
        ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        super.setContentView(view);
    }

    @Override
    public void setContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view,
            @SuppressLint({"UnknownNullness", "MissingNullability"})
                    ViewGroup.LayoutParams params) {
        // Set the VTLO before setting the content view so that attach listeners
        // will see it already present
        ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        super.setContentView(view, params);
    }

    @Override
    public void addContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view,
            @SuppressLint({"UnknownNullness", "MissingNullability"})
                    ViewGroup.LayoutParams params) {
        // Set the VTLO before setting the content view so that attach listeners
        // will see it already present.
        ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        super.addContentView(view, params);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of ComponentActivity. If you do override
     * this method, you <code>must</code>:
     * <ol>
     *     <li>Return an instance of {@link LifecycleRegistry}</li>
     *     <li>Lazily initialize your LifecycleRegistry object when this is first called.
     *     Note that this method will be called in the super classes' constructor, before any
     *     field initialization or object state creation is complete.</li>
     * </ol>
     */
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Returns the {@link ViewModelStore} associated with this activity
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of ComponentActivity.
     *
     * @return a {@code ViewModelStore}
     * @throws IllegalStateException if called before the Activity is attached to the Application
     * instance i.e., before onCreate()
     */
    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mViewModelStore == null) {
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                // Restore the ViewModelStore from NonConfigurationInstances
                mViewModelStore = nc.viewModelStore;
            }
            if (mViewModelStore == null) {
                mViewModelStore = new ViewModelStore();
            }
        }
        return mViewModelStore;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The extras of {@link #getIntent()} when this is first called will be used as
     * the defaults to any {@link androidx.lifecycle.SavedStateHandle} passed to a view model
     * created using this factory.</p>
     */
    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mDefaultFactory == null) {
            mDefaultFactory = new SavedStateViewModelFactory(
                    getApplication(),
                    this,
                    getIntent() != null ? getIntent().getExtras() : null);
        }
        return mDefaultFactory;
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. The {@link #getOnBackPressedDispatcher() OnBackPressedDispatcher} will be given a
     * chance to handle the back button before the default behavior of
     * {@link android.app.Activity#onBackPressed()} is invoked.
     *
     * @see #getOnBackPressedDispatcher()
     */
    @Override
    @MainThread
    public void onBackPressed() {
        mOnBackPressedDispatcher.onBackPressed();
    }

    /**
     * Retrieve the {@link OnBackPressedDispatcher} that will be triggered when
     * {@link #onBackPressed()} is called.
     * @return The {@link OnBackPressedDispatcher} associated with this ComponentActivity.
     */
    @NonNull
    @Override
    public final OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return mOnBackPressedDispatcher;
    }

    @NonNull
    @Override
    public final SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    @CallSuper
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!mActivityResultRegistry.dispatchResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (!mActivityResultRegistry.dispatchResult(requestCode, Activity.RESULT_OK, new Intent()
                .putExtra(EXTRA_PERMISSIONS, permissions)
                .putExtra(EXTRA_PERMISSION_GRANT_RESULTS, grantResults))) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @NonNull
    @Override
    public <I, O> ActivityResultLauncher<I> prepareCall(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultRegistry registry,
            @NonNull final ActivityResultCallback<O> callback) {
        return registry.registerActivityResultCallback(
                "activity_rq#" + mNextLocalRequestCode.getAndIncrement(), this, contract, callback);
    }

    @NonNull
    @Override
    public <I, O> ActivityResultLauncher<I> prepareCall(
            @NonNull ActivityResultContract<I, O> contract,
            @NonNull ActivityResultCallback<O> callback) {
        return prepareCall(contract, mActivityResultRegistry, callback);
    }

    /**
     * Get the {@link ActivityResultRegistry} associated with this activity.
     *
     * @return the {@link ActivityResultRegistry}
     */
    @NonNull
    public ActivityResultRegistry getActivityResultRegistry() {
        return mActivityResultRegistry;
    }
}
