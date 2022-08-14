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

import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS;
import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS;
import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION;
import static androidx.lifecycle.SavedStateHandleSupport.enableSavedStateHandles;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.contextaware.ContextAware;
import androidx.activity.contextaware.ContextAwareHelper;
import androidx.activity.contextaware.OnContextAvailableListener;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.MultiWindowModeChangedInfo;
import androidx.core.app.OnMultiWindowModeChangedProvider;
import androidx.core.app.OnNewIntentProvider;
import androidx.core.app.OnPictureInPictureModeChangedProvider;
import androidx.core.app.PictureInPictureModeChangedInfo;
import androidx.core.content.ContextCompat;
import androidx.core.content.OnConfigurationChangedProvider;
import androidx.core.content.OnTrimMemoryProvider;
import androidx.core.os.BuildCompat;
import androidx.core.util.Consumer;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuHostHelper;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ReportFragment;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;
import androidx.tracing.Trace;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for activities that enables composition of higher level components.
 * <p>
 * Rather than all functionality being built directly into this class, only the minimal set of
 * lower level building blocks are included. Higher level components can then be used as needed
 * without enforcing a deep Activity class hierarchy or strong coupling between components.
 */
public class ComponentActivity extends androidx.core.app.ComponentActivity implements
        ContextAware,
        LifecycleOwner,
        ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner,
        ActivityResultRegistryOwner,
        ActivityResultCaller,
        OnConfigurationChangedProvider,
        OnTrimMemoryProvider,
        OnNewIntentProvider,
        OnMultiWindowModeChangedProvider,
        OnPictureInPictureModeChangedProvider,
        MenuHost {

    static final class NonConfigurationInstances {
        Object custom;
        ViewModelStore viewModelStore;
    }

    private static final String ACTIVITY_RESULT_TAG = "android:support:activity-result";

    final ContextAwareHelper mContextAwareHelper = new ContextAwareHelper();
    private final MenuHostHelper mMenuHostHelper = new MenuHostHelper(this::invalidateMenu);
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SavedStateRegistryController mSavedStateRegistryController =
            SavedStateRegistryController.create(this);

    // Lazily recreated from NonConfigurationInstances by getViewModelStore()
    private ViewModelStore mViewModelStore;
    private ViewModelProvider.Factory mDefaultFactory;

    private final OnBackPressedDispatcher mOnBackPressedDispatcher =
            new OnBackPressedDispatcher(new Runnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    // Calling onBackPressed() on an Activity with its state saved can cause an
                    // error on devices on API levels before 26. We catch that specific error
                    // and throw all others.
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

    private final ActivityResultRegistry mActivityResultRegistry = new ActivityResultRegistry() {

        @SuppressWarnings("deprecation")
        @Override
        public <I, O> void onLaunch(
                final int requestCode,
                @NonNull ActivityResultContract<I, O> contract,
                I input,
                @Nullable ActivityOptionsCompat options) {
            ComponentActivity activity = ComponentActivity.this;

            // Immediate result path
            final ActivityResultContract.SynchronousResult<O> synchronousResult =
                    contract.getSynchronousResult(activity, input);
            if (synchronousResult != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dispatchResult(requestCode, synchronousResult.getValue());
                    }
                });
                return;
            }

            // Start activity path
            Intent intent = contract.createIntent(activity, input);
            Bundle optionsBundle = null;
            // If there are any extras, we should defensively set the classLoader
            if (intent.getExtras() != null && intent.getExtras().getClassLoader() == null) {
                intent.setExtrasClassLoader(activity.getClassLoader());
            }
            if (intent.hasExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE)) {
                optionsBundle = intent.getBundleExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE);
                intent.removeExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE);
            } else if (options != null) {
                optionsBundle = options.toBundle();
            }
            if (ACTION_REQUEST_PERMISSIONS.equals(intent.getAction())) {

                // requestPermissions path
                String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);

                if (permissions == null) {
                    permissions = new String[0];
                }

                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            } else if (ACTION_INTENT_SENDER_REQUEST.equals(intent.getAction())) {
                IntentSenderRequest request =
                        intent.getParcelableExtra(EXTRA_INTENT_SENDER_REQUEST);
                try {
                    // startIntentSenderForResult path
                    ActivityCompat.startIntentSenderForResult(activity, request.getIntentSender(),
                            requestCode, request.getFillInIntent(), request.getFlagsMask(),
                            request.getFlagsValues(), 0, optionsBundle);
                } catch (final IntentSender.SendIntentException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            dispatchResult(requestCode, RESULT_CANCELED,
                                    new Intent().setAction(ACTION_INTENT_SENDER_REQUEST)
                                            .putExtra(EXTRA_SEND_INTENT_EXCEPTION, e));
                        }
                    });
                }
            } else {
                // startActivityForResult path
                ActivityCompat.startActivityForResult(activity, intent, requestCode, optionsBundle);
            }
        }
    };

    private final CopyOnWriteArrayList<Consumer<Configuration>> mOnConfigurationChangedListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Integer>> mOnTrimMemoryListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Intent>> mOnNewIntentListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MultiWindowModeChangedInfo>>
            mOnMultiWindowModeChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<PictureInPictureModeChangedInfo>>
            mOnPictureInPictureModeChangedListeners = new CopyOnWriteArrayList<>();

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
                            Api19Impl.cancelPendingInputEvents(decor);
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
                    // Clear out the available context
                    mContextAwareHelper.clearAvailableContext();
                    // And clear the ViewModelStore
                    if (!isChangingConfigurations()) {
                        getViewModelStore().clear();
                    }
                }
            }
        });
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                ensureViewModelStore();
                getLifecycle().removeObserver(this);
            }
        });
        mSavedStateRegistryController.performAttach();
        enableSavedStateHandles(this);

        if (19 <= SDK_INT && SDK_INT <= 23) {
            getLifecycle().addObserver(new ImmLeaksCleaner(this));
        }
        getSavedStateRegistry().registerSavedStateProvider(ACTIVITY_RESULT_TAG,
                () -> {
                    Bundle outState = new Bundle();
                    mActivityResultRegistry.onSaveInstanceState(outState);
                    return outState;
                });
        addOnContextAvailableListener(context -> {
            Bundle savedInstanceState = getSavedStateRegistry()
                    .consumeRestoredStateForKey(ACTIVITY_RESULT_TAG);
            if (savedInstanceState != null) {
                mActivityResultRegistry.onRestoreInstanceState(savedInstanceState);
            }
        });
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
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Restore the Saved State first so that it is available to
        // OnContextAvailableListener instances
        mSavedStateRegistryController.performRestore(savedInstanceState);
        mContextAwareHelper.dispatchOnContextAvailable(this);
        super.onCreate(savedInstanceState);
        ReportFragment.injectIfNeededIn(this);
        if (BuildCompat.isAtLeastT()) {
            mOnBackPressedDispatcher.setOnBackInvokedDispatcher(getOnBackInvokedDispatcher());
        }
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
        initViewTreeOwners();
        super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view) {
        initViewTreeOwners();
        super.setContentView(view);
    }

    @Override
    public void setContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view,
            @SuppressLint({"UnknownNullness", "MissingNullability"})
                    ViewGroup.LayoutParams params) {
        initViewTreeOwners();
        super.setContentView(view, params);
    }

    @Override
    public void addContentView(@SuppressLint({"UnknownNullness", "MissingNullability"}) View view,
            @SuppressLint({"UnknownNullness", "MissingNullability"})
                    ViewGroup.LayoutParams params) {
        initViewTreeOwners();
        super.addContentView(view, params);
    }

    private void initViewTreeOwners() {
        // Set the view tree owners before setting the content view so that the inflation process
        // and attach listeners will see them already present
        ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        ViewTreeViewModelStoreOwner.set(getWindow().getDecorView(), this);
        ViewTreeSavedStateRegistryOwner.set(getWindow().getDecorView(), this);
        ViewTreeOnBackPressedDispatcherOwner.set(getWindow().getDecorView(), this);
    }

    @Nullable
    @Override
    public Context peekAvailableContext() {
        return mContextAwareHelper.peekAvailableContext();
    }

    /**
     * {@inheritDoc}
     *
     * Any listener added here will receive a callback as part of
     * <code>super.onCreate()</code>, but importantly <strong>before</strong> any other
     * logic is done (including calling through to the framework
     * {@link Activity#onCreate(Bundle)} with the exception of restoring the state
     * of the {@link #getSavedStateRegistry() SavedStateRegistry} for use in your listener.
     */
    @Override
    public final void addOnContextAvailableListener(
            @NonNull OnContextAvailableListener listener) {
        mContextAwareHelper.addOnContextAvailableListener(listener);
    }

    @Override
    public final void removeOnContextAvailableListener(
            @NonNull OnContextAvailableListener listener) {
        mContextAwareHelper.removeOnContextAvailableListener(listener);
    }

    @Override
    public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            super.onPreparePanel(featureId, view, menu);
            mMenuHostHelper.onPrepareMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            super.onCreatePanelMenu(featureId, menu);
            mMenuHostHelper.onCreateMenu(menu, getMenuInflater());
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            return mMenuHostHelper.onMenuItemSelected(item);
        }
        return false;
    }

    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        mMenuHostHelper.onMenuClosed(menu);
        super.onPanelClosed(featureId, menu);
    }

    @Override
    public void addMenuProvider(@NonNull MenuProvider provider) {
        mMenuHostHelper.addMenuProvider(provider);
    }

    @Override
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner) {
        mMenuHostHelper.addMenuProvider(provider, owner);
    }

    @Override
    @SuppressLint("LambdaLast")
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state) {
        mMenuHostHelper.addMenuProvider(provider, owner, state);
    }

    @Override
    public void removeMenuProvider(@NonNull MenuProvider provider) {
        mMenuHostHelper.removeMenuProvider(provider);
    }

    @Override
    public void invalidateMenu() {
        invalidateOptionsMenu();
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
        ensureViewModelStore();
        return mViewModelStore;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void ensureViewModelStore() {
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
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mDefaultFactory == null) {
            mDefaultFactory = new SavedStateViewModelFactory(
                    getApplication(),
                    this,
                    getIntent() != null ? getIntent().getExtras() : null);
        }
        return mDefaultFactory;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The extras of {@link #getIntent()} when this is first called will be used as
     * the defaults to any {@link androidx.lifecycle.SavedStateHandle} passed to a view model
     * created using this extra.</p>
     */
    @NonNull
    @Override
    @CallSuper
    public CreationExtras getDefaultViewModelCreationExtras() {
        MutableCreationExtras extras = new MutableCreationExtras();
        if (getApplication() != null) {
            extras.set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, getApplication());
        }
        extras.set(SavedStateHandleSupport.SAVED_STATE_REGISTRY_OWNER_KEY, this);
        extras.set(SavedStateHandleSupport.VIEW_MODEL_STORE_OWNER_KEY, this);
        if (getIntent() != null && getIntent().getExtras() != null) {
            extras.set(SavedStateHandleSupport.DEFAULT_ARGS_KEY, getIntent().getExtras());
        }
        return extras;
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. The {@link #getOnBackPressedDispatcher() OnBackPressedDispatcher} will be given a
     * chance to handle the back button before the default behavior of
     * {@link android.app.Activity#onBackPressed()} is invoked.
     *
     * @see #getOnBackPressedDispatcher()
     */
    @SuppressWarnings("deprecation")
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

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}.
     */
    @Override
    @Deprecated
    public void startActivityForResult(@NonNull Intent intent,
            int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}.
     */
    @Override
    @Deprecated
    public void startActivityForResult(@NonNull Intent intent,
            int requestCode, @Nullable Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartIntentSenderForResult} object for the
     * {@link ActivityResultContract}.
     */
    @Override
    @Deprecated
    public void startIntentSenderForResult(@NonNull IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags)
            throws IntentSender.SendIntentException {
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues,
                extraFlags);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartIntentSenderForResult} object for the
     * {@link ActivityResultContract}.
     */
    @Override
    @Deprecated
    public void startIntentSenderForResult(@NonNull IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues,
                extraFlags, options);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with the appropriate {@link ActivityResultContract} and handling the result in the
     * {@link ActivityResultCallback#onActivityResult(Object) callback}.
     */
    @CallSuper
    @Override
    @Deprecated
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!mActivityResultRegistry.dispatchResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * activity. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing
     * in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and
     * handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.
     */
    @CallSuper
    @Override
    @Deprecated
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (!mActivityResultRegistry.dispatchResult(requestCode, Activity.RESULT_OK, new Intent()
                .putExtra(EXTRA_PERMISSIONS, permissions)
                .putExtra(EXTRA_PERMISSION_GRANT_RESULTS, grantResults))) {
            if (Build.VERSION.SDK_INT >= 23) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @NonNull
    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultRegistry registry,
            @NonNull final ActivityResultCallback<O> callback) {
        return registry.register(
                "activity_rq#" + mNextLocalRequestCode.getAndIncrement(), this, contract, callback);
    }

    @NonNull
    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull ActivityResultContract<I, O> contract,
            @NonNull ActivityResultCallback<O> callback) {
        return registerForActivityResult(contract, mActivityResultRegistry, callback);
    }

    /**
     * Get the {@link ActivityResultRegistry} associated with this activity.
     *
     * @return the {@link ActivityResultRegistry}
     */
    @NonNull
    @Override
    public final ActivityResultRegistry getActivityResultRegistry() {
        return mActivityResultRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnConfigurationChangedListener(Consumer)}.
     */
    @CallSuper
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for (Consumer<Configuration> listener : mOnConfigurationChangedListeners) {
            listener.accept(newConfig);
        }
    }

    @Override
    public final void addOnConfigurationChangedListener(
            @NonNull Consumer<Configuration> listener
    ) {
        mOnConfigurationChangedListeners.add(listener);
    }

    @Override
    public final void removeOnConfigurationChangedListener(
            @NonNull Consumer<Configuration> listener
    ) {
        mOnConfigurationChangedListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via {@link #addOnTrimMemoryListener(Consumer)}.
     */
    @CallSuper
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        for (Consumer<Integer> listener : mOnTrimMemoryListeners) {
            listener.accept(level);
        }
    }

    @Override
    public final void addOnTrimMemoryListener(@NonNull Consumer<Integer> listener) {
        mOnTrimMemoryListeners.add(listener);
    }

    @Override
    public final void removeOnTrimMemoryListener(@NonNull Consumer<Integer> listener) {
        mOnTrimMemoryListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnNewIntentListener(Consumer)}.
     */
    @CallSuper
    @Override
    protected void onNewIntent(
            @SuppressLint({"UnknownNullness", "MissingNullability"}) Intent intent
    ) {
        super.onNewIntent(intent);
        for (Consumer<Intent> listener : mOnNewIntentListeners) {
            listener.accept(intent);
        }
    }

    @Override
    public final void addOnNewIntentListener(
            @NonNull Consumer<Intent> listener
    ) {
        mOnNewIntentListeners.add(listener);
    }

    @Override
    public final void removeOnNewIntentListener(
            @NonNull Consumer<Intent> listener
    ) {
        mOnNewIntentListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnMultiWindowModeChangedListener(Consumer)}.
     */
    @CallSuper
    @Override
    @SuppressWarnings("deprecation")
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        // We specifically do not call super.onMultiWindowModeChanged() to avoid
        // crashing when this method is manually called prior to API 24 (which is
        // when this method was added to the framework)
        for (Consumer<MultiWindowModeChangedInfo> listener : mOnMultiWindowModeChangedListeners) {
            listener.accept(new MultiWindowModeChangedInfo(isInMultiWindowMode));
        }
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnMultiWindowModeChangedListener(Consumer)}.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @CallSuper
    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode,
            @NonNull Configuration newConfig) {
        // We specifically do not call super.onMultiWindowModeChanged() to avoid
        // triggering the call to onMultiWindowModeChanged(boolean) which would
        // send a second callback to listeners without the newConfig
        for (Consumer<MultiWindowModeChangedInfo> listener : mOnMultiWindowModeChangedListeners) {
            listener.accept(new MultiWindowModeChangedInfo(isInMultiWindowMode, newConfig));
        }
    }

    @Override
    public final void addOnMultiWindowModeChangedListener(
            @NonNull Consumer<MultiWindowModeChangedInfo> listener
    ) {
        mOnMultiWindowModeChangedListeners.add(listener);
    }

    @Override
    public final void removeOnMultiWindowModeChangedListener(
            @NonNull Consumer<MultiWindowModeChangedInfo> listener
    ) {
        mOnMultiWindowModeChangedListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnPictureInPictureModeChangedListener(Consumer)}.
     */
    @CallSuper
    @Override
    @SuppressWarnings("deprecation")
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        // We specifically do not call super.onPictureInPictureModeChanged() to avoid
        // crashing when this method is manually called prior to API 24 (which is
        // when this method was added to the framework)
        for (Consumer<PictureInPictureModeChangedInfo> listener :
                mOnPictureInPictureModeChangedListeners) {
            listener.accept(new PictureInPictureModeChangedInfo(isInPictureInPictureMode));
        }
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via
     * {@link #addOnPictureInPictureModeChangedListener(Consumer)}.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @CallSuper
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            @NonNull Configuration newConfig) {
        // We specifically do not call super.onPictureInPictureModeChanged() to avoid
        // triggering the call to onPictureInPictureModeChanged(boolean) which would
        // send a second callback to listeners without the newConfig
        for (Consumer<PictureInPictureModeChangedInfo> listener :
                mOnPictureInPictureModeChangedListeners) {
            listener.accept(new PictureInPictureModeChangedInfo(
                    isInPictureInPictureMode, newConfig));
        }
    }

    @Override
    public final void addOnPictureInPictureModeChangedListener(
            @NonNull Consumer<PictureInPictureModeChangedInfo> listener
    ) {
        mOnPictureInPictureModeChangedListeners.add(listener);
    }

    @Override
    public final void removeOnPictureInPictureModeChangedListener(
            @NonNull Consumer<PictureInPictureModeChangedInfo> listener
    ) {
        mOnPictureInPictureModeChangedListeners.remove(listener);
    }

    @Override
    public void reportFullyDrawn() {
        try {
            if (Trace.isEnabled()) {
                // TODO: Ideally we'd include getComponentName() (as later versions of platform
                //  do), but b/175345114 needs to be addressed.
                Trace.beginSection("reportFullyDrawn() for ComponentActivity");
            }

            if (Build.VERSION.SDK_INT > 19) {
                super.reportFullyDrawn();
            } else if (Build.VERSION.SDK_INT == 19 && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.UPDATE_DEVICE_STATS) == PackageManager.PERMISSION_GRANTED) {
                // On API 19, the Activity.reportFullyDrawn() method requires the
                // UPDATE_DEVICE_STATS permission, otherwise it throws an exception. Instead of
                // throwing, we fall back to a no-op call.
                super.reportFullyDrawn();
            }
            // The Activity.reportFullyDrawn() got added in API 19, fall back to a no-op call if
            // this method gets called on devices with an earlier version.
        } finally {
            Trace.endSection();
        }
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() { }

        static void cancelPendingInputEvents(View view) {
            view.cancelPendingInputEvents();
        }

    }
}
