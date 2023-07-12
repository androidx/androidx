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

package androidx.fragment.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.lifecycle.SavedStateHandleSupport.enableSavedStateHandles;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;
import androidx.annotation.AnimRes;
import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.arch.core.util.Function;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.LayoutInflaterCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.strictmode.FragmentStrictMode;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.loader.app.LoaderManager;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static library support version of the framework's {@link android.app.Fragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation. See the framework {@link android.app.Fragment}
 * documentation for a class overview.
 *
 * <p>The main differences when using this support version instead of the framework version are:
 * <ul>
 *  <li>Your activity must extend {@link FragmentActivity}
 *  <li>You must call {@link FragmentActivity#getSupportFragmentManager} to get the
 *  {@link FragmentManager}
 * </ul>
 *
 */
public class Fragment implements ComponentCallbacks, OnCreateContextMenuListener, LifecycleOwner,
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory, SavedStateRegistryOwner,
        ActivityResultCaller {

    static final Object USE_DEFAULT_TRANSITION = new Object();

    static final int INITIALIZING = -1;          // Not yet attached.
    static final int ATTACHED = 0;               // Attached to the host.
    static final int CREATED = 1;                // Created.
    static final int VIEW_CREATED = 2;           // View Created.
    static final int AWAITING_EXIT_EFFECTS = 3;  // Downward state, awaiting exit effects
    static final int ACTIVITY_CREATED = 4;       // Fully created, not started.
    static final int STARTED = 5;                // Created and started, not resumed.
    static final int AWAITING_ENTER_EFFECTS = 6; // Upward state, awaiting enter effects
    static final int RESUMED = 7;                // Created started and resumed.

    int mState = INITIALIZING;

    // When instantiated from saved state, this is the saved state.
    Bundle mSavedFragmentState;
    SparseArray<Parcelable> mSavedViewState;
    Bundle mSavedViewRegistryState;
    // If the userVisibleHint is changed before the state is set,
    // it is stored here
    @Nullable Boolean mSavedUserVisibleHint;

    // Internal unique name for this fragment;
    @NonNull
    String mWho = UUID.randomUUID().toString();

    // Construction arguments;
    Bundle mArguments;

    // Target fragment.
    Fragment mTarget;

    // For use when retaining a fragment: this is the who of the last mTarget.
    String mTargetWho = null;

    // Target request code.
    int mTargetRequestCode;

    // Boolean indicating whether this Fragment is the primary navigation fragment
    private Boolean mIsPrimaryNavigationFragment = null;

    // True if the fragment is in the list of added fragments.
    boolean mAdded;

    // If set this fragment is being removed from its activity.
    boolean mRemoving;

    boolean mBeingSaved;

    // Set to true if this fragment was instantiated from a layout file.
    boolean mFromLayout;

    // Set to true when the view has actually been inflated in its layout.
    boolean mInLayout;

    // True if this fragment has been restored from previously saved state.
    boolean mRestored;

    // True if performCreateView has been called and a matching call to performDestroyView
    // has not yet happened.
    boolean mPerformedCreateView;

    // Number of active back stack entries this fragment is in.
    int mBackStackNesting;

    // The fragment manager we are associated with.  Set as soon as the
    // fragment is used in a transaction; cleared after it has been removed
    // from all transactions.
    FragmentManager mFragmentManager;

    // Host this fragment is attached to.
    FragmentHostCallback<?> mHost;

    // Private fragment manager for child fragments inside of this one.
    @NonNull
    FragmentManager mChildFragmentManager = new FragmentManagerImpl();

    // If this Fragment is contained in another Fragment, this is that container.
    Fragment mParentFragment;

    // The optional identifier for this fragment -- either the container ID if it
    // was dynamically added to the view hierarchy, or the ID supplied in
    // layout.
    int mFragmentId;

    // When a fragment is being dynamically added to the view hierarchy, this
    // is the identifier of the parent container it is being added to.
    int mContainerId;

    // The optional named tag for this fragment -- usually used to find
    // fragments that are not part of the layout.
    String mTag;

    // Set to true when the app has requested that this fragment be hidden
    // from the user.
    boolean mHidden;

    // Set to true when the app has requested that this fragment be deactivated.
    boolean mDetached;

    // If set this fragment would like its instance retained across
    // configuration changes.
    boolean mRetainInstance;

    // If set this fragment changed its mRetainInstance while it was detached
    boolean mRetainInstanceChangedWhileDetached;

    // If set this fragment has menu items to contribute.
    boolean mHasMenu;

    // Set to true to allow the fragment's menu to be shown.
    boolean mMenuVisible = true;

    // Used to verify that subclasses call through to super class.
    private boolean mCalled;

    // The parent container of the fragment after dynamically added to UI.
    ViewGroup mContainer;

    // The View generated for this fragment.
    View mView;

    // Whether this fragment should defer starting until after other fragments
    // have been started and their loaders are finished.
    boolean mDeferStart;

    // Hint provided by the app that this fragment is currently visible to the user.
    boolean mUserVisibleHint = true;

    // The animation and transition information for the fragment. This will be null
    // unless the elements are explicitly accessed and should remain null for Fragments
    // without Views.
    AnimationInfo mAnimationInfo;

    // Handler used when the Fragment is postponed but not yet attached to the FragmentManager
    Handler mPostponedHandler;

    // Runnable that is used to indicate if the Fragment has a postponed transition that is on a
    // timeout.
    Runnable mPostponedDurationRunnable = new Runnable() {
        @Override
        public void run() {
            startPostponedEnterTransition();
        }
    };

    // True if mHidden has been changed and the animation should be scheduled.
    boolean mHiddenChanged;

    // The cached value from onGetLayoutInflater(Bundle) that will be returned from
    // getLayoutInflater()
    LayoutInflater mLayoutInflater;

    // Keep track of whether or not this Fragment has run performCreate(). Retained instance
    // fragments can have mRetaining set to true without going through creation, so we must
    // track it separately.
    boolean mIsCreated;

    // Holds the unique ID for the previous instance of the fragment if it had already been
    // added to a FragmentManager and has since been removed.
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public String mPreviousWho;

    // Max Lifecycle state this Fragment can achieve.
    Lifecycle.State mMaxState = Lifecycle.State.RESUMED;

    LifecycleRegistry mLifecycleRegistry;

    // This is initialized in performCreateView and unavailable outside of the
    // onCreateView/onDestroyView lifecycle
    @Nullable FragmentViewLifecycleOwner mViewLifecycleOwner;
    MutableLiveData<LifecycleOwner> mViewLifecycleOwnerLiveData = new MutableLiveData<>();

    ViewModelProvider.Factory mDefaultFactory;

    SavedStateRegistryController mSavedStateRegistryController;

    @LayoutRes
    private int mContentLayoutId;

    private final AtomicInteger mNextLocalRequestCode = new AtomicInteger();

    private final ArrayList<OnPreAttachedListener> mOnPreAttachedListeners = new ArrayList<>();

    private abstract static class OnPreAttachedListener {
        abstract void onPreAttached();
    }

    private final OnPreAttachedListener mSavedStateAttachListener = new OnPreAttachedListener() {
        @Override
        void onPreAttached() {
            mSavedStateRegistryController.performAttach();
            enableSavedStateHandles(Fragment.this);
            // Restore the state immediately so that every lifecycle callback including
            // onAttach() can safely access the state in the SavedStateRegistry
            Bundle savedStateRegistryState = mSavedFragmentState != null
                    ? mSavedFragmentState.getBundle(FragmentStateManager.REGISTRY_STATE_KEY)
                    : null;
            mSavedStateRegistryController.performRestore(savedStateRegistryState);
        }
    };

    /**
     * {@inheritDoc}
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     */
    @Override
    @NonNull
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Get a {@link LifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle. In most cases, this mirrors the lifecycle of the Fragment itself, but in cases
     * of {@link FragmentTransaction#detach(Fragment) detached} Fragments, the lifecycle of the
     * Fragment can be considerably longer than the lifecycle of the View itself.
     * <p>
     * Namely, the lifecycle of the Fragment's View is:
     * <ol>
     * <li>{@link Lifecycle.Event#ON_CREATE created} after {@link #onViewStateRestored(Bundle)}</li>
     * <li>{@link Lifecycle.Event#ON_START started} after {@link #onStart()}</li>
     * <li>{@link Lifecycle.Event#ON_RESUME resumed} after {@link #onResume()}</li>
     * <li>{@link Lifecycle.Event#ON_PAUSE paused} before {@link #onPause()}</li>
     * <li>{@link Lifecycle.Event#ON_STOP stopped} before {@link #onStop()}</li>
     * <li>{@link Lifecycle.Event#ON_DESTROY destroyed} before {@link #onDestroyView()}</li>
     * </ol>
     *
     * The first method where it is safe to access the view lifecycle is
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} under the condition that you must
     * return a non-null view (an IllegalStateException will be thrown if you access the view
     * lifecycle but don't return a non-null view).
     * <p>The view lifecycle remains valid through the call to {@link #onDestroyView()}, after which
     * {@link #getView()} will return null, the view lifecycle will be destroyed, and this method
     * will throw an IllegalStateException. Consider using
     * {@link #getViewLifecycleOwnerLiveData()} or {@link FragmentTransaction#runOnCommit(Runnable)}
     * to receive a callback for when the Fragment's view lifecycle is available.
     * <p>
     * This should only be called on the main thread.
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return A {@link LifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle.
     * @throws IllegalStateException if the {@link #getView() Fragment's View is null}.
     */
    @MainThread
    @NonNull
    public LifecycleOwner getViewLifecycleOwner() {
        if (mViewLifecycleOwner == null) {
            throw new IllegalStateException("Can't access the Fragment View's LifecycleOwner "
                    + "for " + this + " when getView() is null i.e., before onCreateView() or "
                    + "after onDestroyView()");
        }
        return mViewLifecycleOwner;
    }

    /**
     * Retrieve a {@link LiveData} which allows you to observe the
     * {@link #getViewLifecycleOwner() lifecycle of the Fragment's View}.
     * <p>
     * This will be set to the new {@link LifecycleOwner} after {@link #onCreateView} returns a
     * non-null View and will set to null after {@link #onDestroyView()}.
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return A LiveData that changes in sync with {@link #getViewLifecycleOwner()}.
     */
    @NonNull
    public LiveData<LifecycleOwner> getViewLifecycleOwnerLiveData() {
        return mViewLifecycleOwnerLiveData;
    }

    /**
     * Returns the {@link ViewModelStore} associated with this Fragment
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return a {@code ViewModelStore}
     * @throws IllegalStateException if called before the Fragment is attached i.e., before
     * onAttach().
     */
    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (mFragmentManager == null) {
            throw new IllegalStateException("Can't access ViewModels from detached fragment");
        }
        if (getMinimumMaxLifecycleState() == Lifecycle.State.INITIALIZED.ordinal()) {
            throw new IllegalStateException("Calling getViewModelStore() before a Fragment "
                    + "reaches onCreate() when using setMaxLifecycle(INITIALIZED) is not "
                    + "supported");
        }
        return mFragmentManager.getViewModelStore(this);
    }


    private int getMinimumMaxLifecycleState() {
        if (mMaxState == Lifecycle.State.INITIALIZED || mParentFragment == null) {
            return mMaxState.ordinal();
        }
        return Math.min(mMaxState.ordinal(), mParentFragment.getMinimumMaxLifecycleState());
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mFragmentManager == null) {
            throw new IllegalStateException("Can't access ViewModels from detached fragment");
        }
        if (mDefaultFactory == null) {
            Application application = null;
            Context appContext = requireContext().getApplicationContext();
            while (appContext instanceof ContextWrapper) {
                if (appContext instanceof Application) {
                    application = (Application) appContext;
                    break;
                }
                appContext = ((ContextWrapper) appContext).getBaseContext();
            }
            if (application == null && FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(FragmentManager.TAG, "Could not find Application instance from "
                        + "Context " + requireContext().getApplicationContext() + ", you will "
                        + "need CreationExtras to use AndroidViewModel with the default "
                        + "ViewModelProvider.Factory");
            }
            mDefaultFactory = new SavedStateViewModelFactory(
                    application,
                    this,
                    getArguments());
        }
        return mDefaultFactory;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@link #getArguments() Fragment's arguments} when this is first called will be used
     * as the defaults to any {@link androidx.lifecycle.SavedStateHandle} passed to a view model
     * created using this extra.</p>
     */
    @NonNull
    @Override
    @CallSuper
    public CreationExtras getDefaultViewModelCreationExtras() {
        Application application = null;
        Context appContext = requireContext().getApplicationContext();
        while (appContext instanceof ContextWrapper) {
            if (appContext instanceof Application) {
                application = (Application) appContext;
                break;
            }
            appContext = ((ContextWrapper) appContext).getBaseContext();
        }
        if (application == null && FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(FragmentManager.TAG, "Could not find Application instance from "
                    + "Context " + requireContext().getApplicationContext() + ", you will "
                    + "not be able to use AndroidViewModel with the default "
                    + "ViewModelProvider.Factory");
        }
        MutableCreationExtras extras = new MutableCreationExtras();
        if (application != null) {
            extras.set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application);
        }
        extras.set(SavedStateHandleSupport.SAVED_STATE_REGISTRY_OWNER_KEY, this);
        extras.set(SavedStateHandleSupport.VIEW_MODEL_STORE_OWNER_KEY, this);
        if (getArguments() != null) {
            extras.set(SavedStateHandleSupport.DEFAULT_ARGS_KEY, getArguments());
        }
        return extras;
    }

    @NonNull
    @Override
    public final SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    /**
     * State information that has been retrieved from a fragment instance
     * through {@link FragmentManager#saveFragmentInstanceState(Fragment)
     * FragmentManager.saveFragmentInstanceState}.
     */
    @SuppressLint("BanParcelableUsage, ParcelClassLoader")
    public static class SavedState implements Parcelable {
        final Bundle mState;

        SavedState(Bundle state) {
            mState = state;
        }

        SavedState(@NonNull Parcel in, @Nullable ClassLoader loader) {
            mState = in.readBundle();
            if (loader != null && mState != null) {
                mState.setClassLoader(loader);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeBundle(mState);
        }

        @NonNull
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Thrown by {@link FragmentFactory#instantiate(ClassLoader, String)} when
     * there is an instantiation failure.
     */
    @SuppressWarnings("JavaLangClash")
    public static class InstantiationException extends RuntimeException {
        public InstantiationException(@NonNull String msg, @Nullable Exception cause) {
            super(msg, cause);
        }
    }

    /**
     * Constructor used by the default {@link FragmentFactory}. You must
     * {@link FragmentManager#setFragmentFactory(FragmentFactory) set a custom FragmentFactory}
     * if you want to use a non-default constructor to ensure that your constructor
     * is called when the fragment is re-instantiated.
     *
     * <p>It is strongly recommended to supply arguments with {@link #setArguments}
     * and later retrieved by the Fragment with {@link #getArguments}. These arguments
     * are automatically saved and restored alongside the Fragment.
     *
     * <p>Applications should generally not implement a constructor. Prefer
     * {@link #onAttach(Context)} instead. It is the first place application code can run where
     * the fragment is ready to be used - the point where the fragment is actually associated with
     * its context. Some applications may also want to implement {@link #onInflate} to retrieve
     * attributes from a layout resource, although note this happens when the fragment is attached.
     */
    public Fragment() {
        initLifecycle();
    }

    /**
     * Alternate constructor that can be called from your default, no argument constructor to
     * provide a default layout that will be inflated by
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * <pre class="prettyprint">
     * class MyFragment extends Fragment {
     *   public MyFragment() {
     *     super(R.layout.fragment_main);
     *   }
     * }
     * </pre>
     *
     * You must
     * {@link FragmentManager#setFragmentFactory(FragmentFactory) set a custom FragmentFactory}
     * if you want to use a non-default constructor to ensure that your constructor is called
     * when the fragment is re-instantiated.
     *
     * @see #Fragment()
     * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    @ContentView
    public Fragment(@LayoutRes int contentLayoutId) {
        this();
        mContentLayoutId = contentLayoutId;
    }

    private void initLifecycle() {
        mLifecycleRegistry = new LifecycleRegistry(this);
        mSavedStateRegistryController = SavedStateRegistryController.create(this);
        // The default factory depends on the SavedStateRegistry so it
        // needs to be reset when the SavedStateRegistry is reset
        mDefaultFactory = null;
        if (!mOnPreAttachedListeners.contains(mSavedStateAttachListener)) {
            registerOnPreAttachListener(mSavedStateAttachListener);
        }
    }

    /**
     * Like {@link #instantiate(Context, String, Bundle)} but with a null
     * argument Bundle.
     * @deprecated Use {@link FragmentManager#getFragmentFactory()} and
     * {@link FragmentFactory#instantiate(ClassLoader, String)}
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    @NonNull
    public static Fragment instantiate(@NonNull Context context, @NonNull String fname) {
        return instantiate(context, fname, null);
    }

    /**
     * Create a new instance of a Fragment with the given class name.  This is
     * the same as calling its empty constructor, setting the {@link ClassLoader} on the
     * supplied arguments, then calling {@link #setArguments(Bundle)}.
     *
     * @param context The calling context being used to instantiate the fragment.
     * This is currently just used to get its ClassLoader.
     * @param fname The class name of the fragment to instantiate.
     * @param args Bundle of arguments to supply to the fragment, which it
     * can retrieve with {@link #getArguments()}.  May be null.
     * @return Returns a new fragment instance.
     * @throws InstantiationException If there is a failure in instantiating
     * the given fragment class.  This is a runtime exception; it is not
     * normally expected to happen.
     * @deprecated Use {@link FragmentManager#getFragmentFactory()} and
     * {@link FragmentFactory#instantiate(ClassLoader, String)}, manually calling
     * {@link #setArguments(Bundle)} on the returned Fragment.
     */
    @Deprecated
    @NonNull
    public static Fragment instantiate(@NonNull Context context, @NonNull String fname,
            @Nullable Bundle args) {
        try {
            Class<? extends Fragment> clazz = FragmentFactory.loadFragmentClass(
                    context.getClassLoader(), fname);
            Fragment f = clazz.getConstructor().newInstance();
            if (args != null) {
                args.setClassLoader(f.getClass().getClassLoader());
                f.setArguments(args);
            }
            return f;
        } catch (java.lang.InstantiationException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (NoSuchMethodException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": could not find Fragment constructor", e);
        } catch (InvocationTargetException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": calling Fragment constructor caused an exception", e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    final void restoreViewState(Bundle savedInstanceState) {
        if (mSavedViewState != null) {
            mView.restoreHierarchyState(mSavedViewState);
            mSavedViewState = null;
        }
        mCalled = false;
        onViewStateRestored(savedInstanceState);
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onViewStateRestored()");
        }
        if (mView != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        }
    }

    final boolean isInBackStack() {
        return mBackStackNesting > 0;
    }

    /**
     * Subclasses can not override equals().
     */
    @Override public final boolean equals(@Nullable Object o) {
        return super.equals(o);
    }

    /**
     * Subclasses can not override hashCode().
     */
    @Override public final int hashCode() {
        return super.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        Class<?> cls = getClass();
        sb.append(cls.getSimpleName());
        sb.append("{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("}");
        sb.append(" (");
        sb.append(mWho);
        if (mFragmentId != 0) {
            sb.append(" id=0x");
            sb.append(Integer.toHexString(mFragmentId));
        }
        if (mTag != null) {
            sb.append(" tag=");
            sb.append(mTag);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Return the identifier this fragment is known by.  This is either
     * the android:id value supplied in a layout or the container view ID
     * supplied when adding the fragment.
     */
    final public int getId() {
        return mFragmentId;
    }

    /**
     * Get the tag name of the fragment, if specified.
     */
    @Nullable
    final public String getTag() {
        return mTag;
    }

    /**
     * Supply the construction arguments for this fragment.
     * The arguments supplied here will be retained across fragment destroy and
     * creation.
     * <p>This method cannot be called if the fragment is added to a FragmentManager and
     * if {@link #isStateSaved()} would return true.</p>
     */
    public void setArguments(@Nullable Bundle args) {
        if (mFragmentManager != null && isStateSaved()) {
            throw new IllegalStateException("Fragment already added and state has been saved");
        }
        mArguments = args;
    }

    /**
     * Return the arguments supplied when the fragment was instantiated,
     * if any.
     */
    @Nullable
    final public Bundle getArguments() {
        return mArguments;
    }

    /**
     * Return the arguments supplied when the fragment was instantiated.
     *
     * @throws IllegalStateException if no arguments were supplied to the Fragment.
     * @see #getArguments()
     */
    @NonNull
    public final Bundle requireArguments() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("Fragment " + this + " does not have any arguments.");
        }
        return arguments;
    }

    /**
     * Returns true if this fragment is added and its state has already been saved
     * by its host. Any operations that would change saved state should not be performed
     * if this method returns true, and some operations such as {@link #setArguments(Bundle)}
     * will fail.
     *
     * @return true if this fragment's state has already been saved by its host
     */
    public final boolean isStateSaved() {
        if (mFragmentManager == null) {
            return false;
        }
        return mFragmentManager.isStateSaved();
    }

    /**
     * Set the initial saved state that this Fragment should restore itself
     * from when first being constructed, as returned by
     * {@link FragmentManager#saveFragmentInstanceState(Fragment)
     * FragmentManager.saveFragmentInstanceState}.
     *
     * @param state The state the fragment should be restored from.
     */
    public void setInitialSavedState(@Nullable SavedState state) {
        if (mFragmentManager != null) {
            throw new IllegalStateException("Fragment already added");
        }
        mSavedFragmentState = state != null && state.mState != null
                ? state.mState : null;
    }

    /**
     * Optional target for this fragment.  This may be used, for example,
     * if this fragment is being started by another, and when done wants to
     * give a result back to the first.  The target set here is retained
     * across instances via {@link FragmentManager#putFragment
     * FragmentManager.putFragment()}.
     *
     * @param fragment The fragment that is the target of this one.
     * @param requestCode Optional request code, for convenience if you
     * are going to call back with {@link #onActivityResult(int, int, Intent)}.
     *
     * @deprecated Instead of using a target fragment to pass results, the fragment requesting a
     * result should use
     * {@link FragmentManager#setFragmentResultListener(String, LifecycleOwner,
     * FragmentResultListener)} to register a {@link FragmentResultListener} with a {@code
     * requestKey} using its {@link #getParentFragmentManager() parent fragment manager}. The
     * fragment delivering a result should then call
     * {@link FragmentManager#setFragmentResult(String, Bundle)} using the same {@code requestKey}.
     * Consider using {@link #setArguments} to pass the {@code requestKey} if you need to support
     * dynamic request keys.
     */
    @SuppressWarnings("ReferenceEquality, deprecation")
    @Deprecated
    public void setTargetFragment(@Nullable Fragment fragment, int requestCode) {
        if (fragment != null) {
            FragmentStrictMode.onSetTargetFragmentUsage(this, fragment, requestCode);
        }
        // Don't allow a caller to set a target fragment in another FragmentManager,
        // but there's a snag: people do set target fragments before fragments get added.
        // We'll have the FragmentManager check that for validity when we move
        // the fragments to a valid state.
        final FragmentManager mine = mFragmentManager;
        final FragmentManager theirs = fragment != null ? fragment.mFragmentManager :
                null;
        if (mine != null && theirs != null && mine != theirs) {
            throw new IllegalArgumentException("Fragment " + fragment
                    + " must share the same FragmentManager to be set as a target fragment");
        }

        // Don't let someone create a cycle.
        for (Fragment check = fragment; check != null; check = check.getTargetFragment(false)) {
            if (check.equals(this)) {
                throw new IllegalArgumentException("Setting " + fragment + " as the target of "
                        + this + " would create a target cycle");
            }
        }
        if (fragment == null) {
            mTargetWho = null;
            mTarget = null;
        } else if (mFragmentManager != null && fragment.mFragmentManager != null) {
            // Just save the reference to the Fragment
            mTargetWho = fragment.mWho;
            mTarget = null;
        } else {
            // Save the Fragment itself, waiting until we're attached
            mTargetWho = null;
            mTarget = fragment;
        }
        mTargetRequestCode = requestCode;
    }

    /**
     * Return the target fragment set by {@link #setTargetFragment}.
     *
     * @deprecated Instead of using a target fragment to pass results, use
     * {@link FragmentManager#setFragmentResult(String, Bundle)} to deliver results to
     * {@link FragmentResultListener} instances registered by other fragments via
     * {@link FragmentManager#setFragmentResultListener(String, LifecycleOwner,
     * FragmentResultListener)}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Nullable
    @Deprecated
    final public Fragment getTargetFragment() {
        return getTargetFragment(true);
    }

    /**
     * Use with {@param logViolations} set to {@code false} for all internal calls instead of the
     * public {@link #getTargetFragment}.
     */
    @Nullable
    private Fragment getTargetFragment(boolean logViolations) {
        if (logViolations) {
            FragmentStrictMode.onGetTargetFragmentUsage(this);
        }

        if (mTarget != null) {
            // Ensure that any Fragment set with setTargetFragment is immediately
            // available here
            return mTarget;
        } else if (mFragmentManager != null && mTargetWho != null) {
            // Look up the target Fragment from the FragmentManager
            return mFragmentManager.findActiveFragment(mTargetWho);
        }
        return null;
    }

    /**
     * Return the target request code set by {@link #setTargetFragment}.
     *
     * @deprecated When using the target fragment replacement of
     * {@link FragmentManager#setFragmentResultListener(String, LifecycleOwner,
     * FragmentResultListener)} and {@link FragmentManager#setFragmentResult(String, Bundle)},
     * consider using {@link #setArguments} to pass a {@code requestKey} if you need to support
     * dynamic request keys.
     */
    @Deprecated
    final public int getTargetRequestCode() {
        FragmentStrictMode.onGetTargetFragmentRequestCodeUsage(this);
        return mTargetRequestCode;
    }

    /**
     * Return the {@link Context} this fragment is currently associated with.
     *
     * @see #requireContext()
     */
    @Nullable
    public Context getContext() {
        return mHost == null ? null : mHost.getContext();
    }

    /**
     * Return the {@link Context} this fragment is currently associated with.
     *
     * @throws IllegalStateException if not currently associated with a context.
     * @see #getContext()
     */
    @NonNull
    public final Context requireContext() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to a context.");
        }
        return context;
    }

    /**
     * Return the {@link FragmentActivity} this fragment is currently associated with.
     * May return {@code null} if the fragment is associated with a {@link Context}
     * instead.
     *
     * @see #requireActivity()
     */
    @Nullable
    final public FragmentActivity getActivity() {
        return mHost == null ? null : (FragmentActivity) mHost.getActivity();
    }

    /**
     * Return the {@link FragmentActivity} this fragment is currently associated with.
     *
     * @throws IllegalStateException if not currently associated with an activity or if associated
     * only with a context.
     * @see #getActivity()
     */
    @NonNull
    public final FragmentActivity requireActivity() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
        }
        return activity;
    }

    /**
     * Return the host object of this fragment. May return {@code null} if the fragment
     * isn't currently being hosted.
     *
     * @see #requireHost()
     */
    @Nullable
    final public Object getHost() {
        return mHost == null ? null : mHost.onGetHost();
    }

    /**
     * Return the host object of this fragment.
     *
     * @throws IllegalStateException if not currently associated with a host.
     * @see #getHost()
     */
    @NonNull
    public final Object requireHost() {
        Object host = getHost();
        if (host == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to a host.");
        }
        return host;
    }

    /**
     * Return <code>requireActivity().getResources()</code>.
     */
    @NonNull
    final public Resources getResources() {
        return requireContext().getResources();
    }

    /**
     * Return a localized, styled CharSequence from the application's package's
     * default string table.
     *
     * @param resId Resource id for the CharSequence text
     */
    @NonNull
    public final CharSequence getText(@StringRes int resId) {
        return getResources().getText(resId);
    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    @NonNull
    public final String getString(@StringRes int resId) {
        return getResources().getString(resId);
    }

    /**
     * Return a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}.
     *
     * @param resId Resource id for the format string
     * @param formatArgs The format arguments that will be used for substitution.
     */
    @NonNull
    public final String getString(@StringRes int resId, @Nullable Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this fragment's activity.  Note that this will be non-null slightly
     * before {@link #getActivity()}, during the time from when the fragment is
     * placed in a {@link FragmentTransaction} until it is committed and
     * attached to its activity.
     *
     * <p>If this Fragment is a child of another Fragment, the FragmentManager
     * returned here will be the parent's {@link #getChildFragmentManager()}.
     *
     * @see #getParentFragmentManager()
     * @deprecated This has been removed in favor of <code>getParentFragmentManager()</code> which
     * throws an {@link IllegalStateException} if the FragmentManager is null. Check if
     * {@link #isAdded()} returns <code>false</code> to determine if the FragmentManager is
     * <code>null</code>.
     */
    @Nullable
    @Deprecated
    final public FragmentManager getFragmentManager() {
        return mFragmentManager;
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this fragment's activity.  Note that this will be available slightly
     * before {@link #getActivity()}, during the time from when the fragment is
     * placed in a {@link FragmentTransaction} until it is committed and
     * attached to its activity.
     *
     * <p>If this Fragment is a child of another Fragment, the FragmentManager
     * returned here will be the parent's {@link #getChildFragmentManager()}.
     *
     * @throws IllegalStateException if not associated with a transaction or host.
     */
    @NonNull
    public final FragmentManager getParentFragmentManager() {
        FragmentManager fragmentManager = mFragmentManager;
        if (fragmentManager == null) {
            throw new IllegalStateException(
                    "Fragment " + this + " not associated with a fragment manager.");
        }
        return fragmentManager;
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this fragment's activity.  Note that this will be available slightly
     * before {@link #getActivity()}, during the time from when the fragment is
     * placed in a {@link FragmentTransaction} until it is committed and
     * attached to its activity.
     *
     * <p>If this Fragment is a child of another Fragment, the FragmentManager
     * returned here will be the parent's {@link #getChildFragmentManager()}.
     *
     * @throws IllegalStateException if not associated with a transaction or host.
     * @see #getParentFragmentManager()
     * @deprecated This has been renamed to <code>getParentFragmentManager()</code> to make it
     * clear that you are accessing the FragmentManager that contains this Fragment and not the
     * FragmentManager associated with child Fragments.
     */
    @NonNull
    @Deprecated
    public final FragmentManager requireFragmentManager() {
        return getParentFragmentManager();
    }

    /**
     * Return a private FragmentManager for placing and managing Fragments
     * inside of this Fragment.
     */
    @NonNull
    final public FragmentManager getChildFragmentManager() {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " has not been attached yet.");
        }
        return mChildFragmentManager;
    }

    /**
     * Returns the parent Fragment containing this Fragment.  If this Fragment
     * is attached directly to an Activity, returns null.
     */
    @Nullable
    final public Fragment getParentFragment() {
        return mParentFragment;
    }

    /**
     * Returns the parent Fragment containing this Fragment.
     *
     * @throws IllegalStateException if this Fragment is attached directly to an Activity or
     * other Fragment host.
     * @see #getParentFragment()
     */
    @NonNull
    public final Fragment requireParentFragment() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            Context context = getContext();
            if (context == null) {
                throw new IllegalStateException("Fragment " + this + " is not attached to"
                        + " any Fragment or host");
            } else {
                throw new IllegalStateException("Fragment " + this + " is not a child Fragment, it"
                        + " is directly attached to " + getContext());
            }
        }
        return parentFragment;
    }

    /**
     * Return true if the fragment is currently added to its activity.
     */
    final public boolean isAdded() {
        return mHost != null && mAdded;
    }

    /**
     * Return true if the fragment has been explicitly detached from the UI.
     * That is, {@link FragmentTransaction#detach(Fragment)
     * FragmentTransaction.detach(Fragment)} has been used on it.
     */
    final public boolean isDetached() {
        return mDetached;
    }

    /**
     * Return true if this fragment is currently being removed from its
     * activity.  This is  <em>not</em> whether its activity is finishing, but
     * rather whether it is in the process of being removed from its activity.
     */
    final public boolean isRemoving() {
        return mRemoving;
    }

    /**
     * Return true if the layout is included as part of an activity view
     * hierarchy via the &lt;fragment&gt; tag.  This will always be true when
     * fragments are created through the &lt;fragment&gt; tag, <em>except</em>
     * in the case where an old fragment is restored from a previous state and
     * it does not appear in the layout of the current state.
     */
    final public boolean isInLayout() {
        return mInLayout;
    }

    /**
     * Return true if the fragment is in the resumed state.  This is true
     * for the duration of {@link #onResume()} and {@link #onPause()} as well.
     */
    final public boolean isResumed() {
        return mState >= RESUMED;
    }

    /**
     * Return true if the fragment is currently visible to the user.  This means
     * it: (1) has been added, (2) has its view attached to the window, and
     * (3) is not hidden.
     */
    final public boolean isVisible() {
        return isAdded() && !isHidden() && mView != null
                && mView.getWindowToken() != null && mView.getVisibility() == View.VISIBLE;
    }

    /**
     * Return true if the fragment has been hidden. This includes the case if the fragment is
     * hidden because its parent is hidden. By default fragments
     * are shown.  You can find out about changes to this state with
     * {@link #onHiddenChanged}.  Note that the hidden state is orthogonal
     * to other states -- that is, to be visible to the user, a fragment
     * must be both started and not hidden.
     */
    final public boolean isHidden() {
        return mHidden || (mFragmentManager != null
                && mFragmentManager.isParentHidden(mParentFragment));
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @SuppressLint("KotlinPropertyAccess")
    final public boolean hasOptionsMenu() {
        return mHasMenu;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final public boolean isMenuVisible() {
        return mMenuVisible && (mFragmentManager == null
                || mFragmentManager.isParentMenuVisible(mParentFragment));
    }

    /**
     * Called when the hidden state (as returned by {@link #isHidden()} of
     * the fragment or another fragment in its hierarchy has changed.  Fragments start out not
     * hidden; this will be called whenever the fragment changes state from that.
     * @param hidden True if the fragment is now hidden, false otherwise.
     */
    @MainThread
    public void onHiddenChanged(boolean hidden) {
    }

    /**
     * Control whether a fragment instance is retained across Activity
     * re-creation (such as from a configuration change). If set, the fragment
     * lifecycle will be slightly different when an activity is recreated:
     * <ul>
     * <li> {@link #onDestroy()} will not be called (but {@link #onDetach()} still
     * will be, because the fragment is being detached from its current activity).
     * <li> {@link #onCreate(Bundle)} will not be called since the fragment
     * is not being re-created.
     * <li> {@link #onAttach(Activity)} and {@link #onActivityCreated(Bundle)} <b>will</b>
     * still be called.
     * </ul>
     *
     * @param retain <code>true</code> to retain this fragment instance across configuration
     *               changes, <code>false</code> otherwise.
     *
     * @see #getRetainInstance()
     * @deprecated Instead of retaining the Fragment itself, use a non-retained Fragment and keep
     * retained state in a ViewModel attached to that Fragment. The ViewModel's constructor and
     * its onCleared() callback provide the signal for initial creation and final destruction of
     * the retained state.
     */
    @Deprecated
    public void setRetainInstance(boolean retain) {
        FragmentStrictMode.onSetRetainInstanceUsage(this);
        mRetainInstance = retain;
        if (mFragmentManager != null) {
            if (retain) {
                mFragmentManager.addRetainedFragment(this);
            } else {
                mFragmentManager.removeRetainedFragment(this);
            }
        } else {
            mRetainInstanceChangedWhileDetached = true;
        }
    }

    /**
     * Returns <code>true</code> if this fragment instance's state will be retained across
     * configuration changes, and <code>false</code> if it will not.
     *
     * @return whether or not this fragment instance will be retained.
     * @see #setRetainInstance(boolean)
     *
     * @deprecated Instead of retaining the Fragment itself, use a non-retained Fragment and keep
     * retained state in a ViewModel attached to that Fragment. The ViewModel's constructor and
     * its onCleared() callback provide the signal for initial creation and final destruction of
     * the retained state.
     */
    @Deprecated
    final public boolean getRetainInstance() {
        FragmentStrictMode.onGetRetainInstanceUsage(this);
        return mRetainInstance;
    }

    /**
     * Report that this fragment would like to participate in populating
     * the options menu by receiving a call to {@link #onCreateOptionsMenu}
     * and related methods.
     *
     * @param hasMenu If true, the fragment has menu items to contribute.
     *
     * @deprecated This method is no longer needed when using a {@link MenuProvider} to provide
     * a {@link Menu} to your activity, which replaces {@link #onCreateOptionsMenu} as the
     * recommended way to provide a consistent, optionally {@link Lifecycle}-aware, and modular
     * way to handle menu creation and item selection.
     */
    @Deprecated
    public void setHasOptionsMenu(boolean hasMenu) {
        if (mHasMenu != hasMenu) {
            mHasMenu = hasMenu;
            if (isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    /**
     * Set a hint for whether this fragment's menu should be visible.  This
     * is useful if you know that a fragment has been placed in your view
     * hierarchy so that the user can not currently seen it, so any menu items
     * it has should also not be shown.
     *
     * @param menuVisible The default is true, meaning the fragment's menu will
     * be shown as usual.  If false, the user will not see the menu.
     */
    public void setMenuVisibility(boolean menuVisible) {
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (mHasMenu && isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    /**
     * Set a hint to the system about whether this fragment's UI is currently visible
     * to the user. This hint defaults to true and is persistent across fragment instance
     * state save and restore.
     *
     * <p>An app may set this to false to indicate that the fragment's UI is
     * scrolled out of visibility or is otherwise not directly visible to the user.
     * This may be used by the system to prioritize operations such as fragment lifecycle updates
     * or loader ordering behavior.</p>
     *
     * <p><strong>Note:</strong> This method may be called outside of the fragment lifecycle.
     * and thus has no ordering guarantees with regard to fragment lifecycle method calls.</p>
     *
     * @param isVisibleToUser true if this fragment's UI is currently visible to the user (default),
     *                        false if it is not.
     *
     * @deprecated If you are manually calling this method, use
     * {@link FragmentTransaction#setMaxLifecycle(Fragment, Lifecycle.State)} instead. If
     * overriding this method, behavior implemented when passing in <code>true</code> should be
     * moved to {@link Fragment#onResume()}, and behavior implemented when passing in
     * <code>false</code> should be moved to {@link Fragment#onPause()}.
     */
    @Deprecated
    public void setUserVisibleHint(boolean isVisibleToUser) {
        FragmentStrictMode.onSetUserVisibleHint(this, isVisibleToUser);
        if (!mUserVisibleHint && isVisibleToUser && mState < STARTED
                && mFragmentManager != null && isAdded() && mIsCreated) {
            mFragmentManager.performPendingDeferredStart(
                    mFragmentManager.createOrGetFragmentStateManager(this));
        }
        mUserVisibleHint = isVisibleToUser;
        mDeferStart = mState < STARTED && !isVisibleToUser;
        if (mSavedFragmentState != null) {
            // Ensure that if the user visible hint is set before the Fragment has
            // restored its state that we don't lose the new value
            mSavedUserVisibleHint = isVisibleToUser;
        }
    }

    /**
     * @return The current value of the user-visible hint on this fragment.
     * @see #setUserVisibleHint(boolean)
     *
     * @deprecated Use {@link FragmentTransaction#setMaxLifecycle(Fragment, Lifecycle.State)}
     * instead.
     */
    @Deprecated
    public boolean getUserVisibleHint() {
        return mUserVisibleHint;
    }

    /**
     * Return the LoaderManager for this fragment.
     *
     * @deprecated Use
     * {@link LoaderManager#getInstance(LifecycleOwner) LoaderManager.getInstance(this)}.
     */
    @Deprecated
    @NonNull
    public LoaderManager getLoaderManager() {
        return LoaderManager.getInstance(this);
    }

    /**
     * Call {@link Activity#startActivity(Intent)} from the fragment's
     * containing Activity.
     */
    public void startActivity(@NonNull Intent intent) {
        startActivity(intent, null);
    }

    /**
     * Call {@link Activity#startActivity(Intent, Bundle)} from the fragment's
     * containing Activity.
     */
    public void startActivity(@NonNull Intent intent,
            @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mHost.onStartActivityFromFragment(this /*fragment*/, intent, -1, options);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int)} from the fragment's
     * containing Activity.
     *
     * @param intent The intent to start.
     * @param requestCode The request code to be returned in
     * {@link Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
     *                    between 0 and 65535 to be considered valid. If given requestCode is
     *                    greater than 65535, an IllegalArgumentException would be thrown.
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public void startActivityForResult(@NonNull Intent intent,
            int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int, Bundle)} from the fragment's
     * containing Activity.
     *
     * @param intent The intent to start.
     * @param requestCode The request code to be returned in
     * {@link Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
     *                    between 0 and 65535 to be considered valid. If given requestCode is
     *                    greater than 65535, an IllegalArgumentException would be thrown.
     * @param options Additional options for how the Activity should be started. See
     * {@link Context#startActivity(Intent, Bundle)} for more details. This value may be null.
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void startActivityForResult(@NonNull Intent intent,
            int requestCode, @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        getParentFragmentManager().launchStartActivityForResult(this /*fragment*/, intent,
                requestCode, options);
    }

    /**
     * Call {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int,
     * Bundle)} from the fragment's containing Activity.
     *
     * @param intent The IntentSender to launch.
     * @param requestCode The request code to be returned in
     * {@link Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
     *                    between 0 and 65535 to be considered valid. If given requestCode is
     *                    greater than 65535, an IllegalArgumentException would be thrown.
     * @param fillInIntent If non-null, this will be provided as the intent parameter to
     * {@link IntentSender#sendIntent(Context, int, Intent, IntentSender.OnFinished, Handler)}.
     *                     This value may be null.
     * @param flagsMask Intent flags in the original IntentSender that you would like to change.
     * @param flagsValues Desired values for any bits set in <code>flagsMask</code>.
     * @param extraFlags Always set to 0.
     * @param options Additional options for how the Activity should be started. See
     * {@link Context#startActivity(Intent, Bundle)} for more details. This value may be null.
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * passing in a {@link StartIntentSenderForResult} object for the
     * {@link ActivityResultContract}.
     */
    @Deprecated
    public void startIntentSenderForResult(@NonNull IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Fragment " + this + " received the following in "
                    + "startIntentSenderForResult() requestCode: " + requestCode + " IntentSender: "
                    + intent + " fillInIntent: " + fillInIntent + " options: " + options);
        }
        getParentFragmentManager().launchStartIntentSenderForResult(this, intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with the appropriate {@link ActivityResultContract} and handling the result in the
     * {@link ActivityResultCallback#onActivityResult(Object) callback}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Fragment " + this + " received the following in "
                    + "onActivityResult(): requestCode: " + requestCode + " resultCode: "
                    + resultCode + " data: " + data);
        }
    }

    /**
     * Requests permissions to be granted to this application. These permissions
     * must be requested in your manifest, they should not be granted to your app,
     * and they should have protection level {@link android.content.pm.PermissionInfo
     * #PROTECTION_DANGEROUS dangerous}, regardless whether they are declared by
     * the platform or a third-party app.
     * <p>
     * Normal permissions {@link android.content.pm.PermissionInfo#PROTECTION_NORMAL}
     * are granted at install time if requested in the manifest. Signature permissions
     * {@link android.content.pm.PermissionInfo#PROTECTION_SIGNATURE} are granted at
     * install time if requested in the manifest and the signature of your app matches
     * the signature of the app declaring the permissions.
     * </p>
     * <p>
     * Call {@link #shouldShowRequestPermissionRationale(String)} before calling this API to
     * check if the system recommends to show a rationale dialog before asking for a permission.
     * </p>
     * <p>
     * If your app does not have the requested permissions the user will be presented
     * with UI for accepting them. After the user has accepted or rejected the
     * requested permissions you will receive a callback on {@link
     * #onRequestPermissionsResult(int, String[], int[])} reporting whether the
     * permissions were granted or not.
     * </p>
     * <p>
     * Note that requesting a permission does not guarantee it will be granted and
     * your app should be able to run without having this permission.
     * </p>
     * <p>
     * This method may start an activity allowing the user to choose which permissions
     * to grant and which to reject. Hence, you should be prepared that your activity
     * may be paused and resumed. Further, granting some permissions may require
     * a restart of you application. In such a case, the system will recreate the
     * activity stack before delivering the result to {@link
     * #onRequestPermissionsResult(int, String[], int[])}.
     * </p>
     * <p>
     * When checking whether you have a permission you should use {@link
     * android.content.Context#checkSelfPermission(String)}.
     * </p>
     * <p>
     * Calling this API for permissions already granted to your app would show UI
     * to the user to decided whether the app can still hold these permissions. This
     * can be useful if the way your app uses the data guarded by the permissions
     * changes significantly.
     * </p>
     *
     * @param permissions The requested permissions.
     * @param requestCode Application specific request code to match with a result reported to
     * {@link #onRequestPermissionsResult(int, String[], int[])}. Must be between 0 and 65535 to
     *                    be considered valid. If given requestCode is greater than 65535, an
     *                    IllegalArgumentException would be thrown.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     * @see android.content.Context#checkSelfPermission(String)
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing
     * in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and
     * handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.
     */
    @Deprecated
    public final void requestPermissions(@NonNull String[] permissions, int requestCode) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        getParentFragmentManager().launchRequestPermissions(this, permissions, requestCode);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     * @see #requestPermissions(String[], int)
     *
     * @deprecated This method has been deprecated in favor of using the Activity Result API
     * which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
     * contracts for common intents available in
     * {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
     * testing, and allow receiving results in separate, testable classes independent from your
     * fragment. Use
     * {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing
     * in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and
     * handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.
     */
    @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
    @Deprecated
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        /* callback - do nothing */
    }

    /**
     * Gets whether you should show UI with rationale before requesting a permission.
     *
     * @param permission A permission your app wants to request.
     * @return Whether you should show permission rationale UI.
     *
     * @see Context#checkSelfPermission(String)
     * @see #requestPermissions(String[], int)
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        if (mHost != null) {
            return mHost.onShouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    /**
     * Returns the LayoutInflater used to inflate Views of this Fragment. The default
     * implementation will throw an exception if the Fragment is not attached.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     * @return The LayoutInflater used to inflate Views of this Fragment.
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public LayoutInflater onGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        // TODO: move the implementation in getLayoutInflater to here
        return getLayoutInflater(savedInstanceState);
    }

    /**
     * Returns the cached LayoutInflater used to inflate Views of this Fragment. If
     * {@link #onGetLayoutInflater(Bundle)} has not been called {@link #onGetLayoutInflater(Bundle)}
     * will be called with a {@code null} argument and that value will be cached.
     * <p>
     * The cached LayoutInflater will be replaced immediately prior to
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} and cleared immediately after
     * {@link #onDetach()}.
     *
     * @return The LayoutInflater used to inflate Views of this Fragment.
     */
    @NonNull
    public final LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            return performGetLayoutInflater(null);
        }
        return mLayoutInflater;
    }

    /**
     * Calls {@link #onGetLayoutInflater(Bundle)} and caches the result for use by
     * {@link #getLayoutInflater()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     * @return The LayoutInflater used to inflate Views of this Fragment.
     */
    @NonNull
    LayoutInflater performGetLayoutInflater(@Nullable Bundle savedInstanceState) {
        mLayoutInflater = onGetLayoutInflater(savedInstanceState);
        return mLayoutInflater;
    }

    /**
     * Override {@link #onGetLayoutInflater(Bundle)} when you need to change the
     * LayoutInflater or call {@link #getLayoutInflater()} when you want to
     * retrieve the current LayoutInflater.
     *
     * @deprecated Override {@link #onGetLayoutInflater(Bundle)} or call
     * {@link #getLayoutInflater()} instead of this method.
     */
    @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
    @Deprecated
    @NonNull
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public LayoutInflater getLayoutInflater(@Nullable Bundle savedFragmentState) {
        if (mHost == null) {
            throw new IllegalStateException("onGetLayoutInflater() cannot be executed until the "
                    + "Fragment is attached to the FragmentManager.");
        }
        LayoutInflater result = mHost.onGetLayoutInflater();
        LayoutInflaterCompat.setFactory2(result, mChildFragmentManager.getLayoutInflaterFactory());
        return result;
    }

    /**
     * Called when a fragment is being created as part of a view layout
     * inflation, typically from setting the content view of an activity.  This
     * may be called immediately after the fragment is created from a
     * {@link FragmentContainerView} in a layout file.  Note this is <em>before</em>
     * the fragment's {@link #onAttach(Context)} has been called; all you should
     * do here is parse the attributes and save them away.
     *
     * <p>This is called <em>the first time</em> the fragment is inflated. If it is
     * being inflated into a new instance with saved state, this method will not be
     * called a second time for the restored state fragment.</p>
     *
     * <p>Here is a typical implementation of a fragment that can take parameters
     * both through attributes supplied here as well from {@link #getArguments()}:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentArgumentsSupport.java
     *      fragment}
     *
     * <p>Note that parsing the XML attributes uses a "styleable" resource.  The
     * declaration for the styleable used here is:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/main/res/values/attrs.xml fragment_arguments}
     *
     * <p>The fragment can then be declared within its activity's content layout
     * through a tag like this:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/main/res/layout/fragment_arguments_support.xml from_attributes}
     *
     * <p>This fragment can also be created dynamically from arguments given
     * at runtime in the arguments Bundle; here is an example of doing so at
     * creation of the containing activity:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/main/java/com/example/android/supportv4/app/FragmentArgumentsSupport.java
     *      create}
     *
     * @param context The Activity that is inflating this fragment.
     * @param attrs The attributes at the tag where the fragment is
     * being created.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @SuppressWarnings("deprecation")
    @UiThread
    @CallSuper
    public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs,
            @Nullable Bundle savedInstanceState) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onInflate(hostActivity, attrs, savedInstanceState);
        }
    }

    /**
     * Called when a fragment is being created as part of a view layout
     * inflation, typically from setting the content view of an activity.
     *
     * @deprecated See {@link #onInflate(Context, AttributeSet, Bundle)}.
     */
    @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
    @Deprecated
    @UiThread
    @CallSuper
    public void onInflate(@NonNull Activity activity, @NonNull AttributeSet attrs,
            @Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when a fragment is attached as a child of this fragment.
     *
     * <p>This is called after the attached fragment's <code>onAttach</code> and before
     * the attached fragment's <code>onCreate</code> if the fragment has not yet had a previous
     * call to <code>onCreate</code>.</p>
     *
     * @param childFragment child fragment being attached
     *
     * @deprecated The responsibility for listening for fragments being attached has been moved
     * to FragmentManager. You can add a listener to
     * {@link #getChildFragmentManager()} the child FragmentManager} by calling
     * {@link FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}
     *  in {@link #onAttach(Context)} to get callbacks when a child fragment is attached.
     */
    @SuppressWarnings({"unused", "DeprecatedIsStillUsed"})
    @Deprecated
    @MainThread
    public void onAttachFragment(@NonNull Fragment childFragment) {
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @SuppressWarnings("deprecation")
    @MainThread
    @CallSuper
    public void onAttach(@NonNull Context context) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onAttach(hostActivity);
        }
    }

    /**
     * Called when a fragment is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @deprecated See {@link #onAttach(Context)}.
     */
    @SuppressWarnings({"unused", "DeprecatedIsStillUsed"})
    @Deprecated
    @MainThread
    @CallSuper
    public void onAttach(@NonNull Activity activity) {
        mCalled = true;
    }

    /**
     * Called when a fragment loads an animation. Note that if
     * {@link FragmentTransaction#setCustomAnimations(int, int)} was called with
     * {@link Animator} resources instead of {@link Animation} resources, {@code nextAnim}
     * will be an animator resource.
     *
     * @param transit The value set in {@link FragmentTransaction#setTransition(int)} or 0 if not
     *                set.
     * @param enter {@code true} when the fragment is added/attached/shown or {@code false} when
     *              the fragment is removed/detached/hidden.
     * @param nextAnim The resource set in
     *                 {@link FragmentTransaction#setCustomAnimations(int, int)},
     *                 {@link FragmentTransaction#setCustomAnimations(int, int, int, int)}, or
     *                 0 if neither was called. The value will depend on the current operation.
     */
    @MainThread
    @Nullable
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return null;
    }

    /**
     * Called when a fragment loads an animator. This will be called when
     * {@link #onCreateAnimation(int, boolean, int)} returns null. Note that if
     * {@link FragmentTransaction#setCustomAnimations(int, int)} was called with
     * {@link Animation} resources instead of {@link Animator} resources, {@code nextAnim}
     * will be an animation resource.
     *
     * @param transit The value set in {@link FragmentTransaction#setTransition(int)} or 0 if not
     *                set.
     * @param enter {@code true} when the fragment is added/attached/shown or {@code false} when
     *              the fragment is removed/detached/hidden.
     * @param nextAnim The resource set in
     *                 {@link FragmentTransaction#setCustomAnimations(int, int)},
     *                 {@link FragmentTransaction#setCustomAnimations(int, int, int, int)}, or
     *                 0 if neither was called. The value will depend on the current operation.
     */
    @MainThread
    @Nullable
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        return null;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, add a {@link androidx.lifecycle.LifecycleObserver} on the
     * activity's Lifecycle, removing it when it receives the
     * {@link Lifecycle.State#CREATED} callback.
     *
     * <p>Any restored child fragments will be created before the base
     * <code>Fragment.onCreate</code> method returns.</p>
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @MainThread
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mCalled = true;
        restoreChildFragmentState();
        if (!mChildFragmentManager.isStateAtLeast(Fragment.CREATED)) {
            mChildFragmentManager.dispatchCreate();
        }
    }

    /**
     * Restore the state of the child FragmentManager. Called by either
     * {@link #onCreate(Bundle)} for non-retained instance fragments or by
     * {@link FragmentManager#moveToState(Fragment, int, int, int, boolean)}
     * for retained instance fragments.
     *
     * <p><strong>Postcondition:</strong> if there were child fragments to restore,
     * the child FragmentManager will be instantiated and brought to the {@link #CREATED} state.
     * </p>
     */
    void restoreChildFragmentState() {
        if (mSavedFragmentState != null) {
            Bundle childFragmentManagerState = mSavedFragmentState.getBundle(
                    FragmentStateManager.CHILD_FRAGMENT_MANAGER_KEY);
            if (childFragmentManagerState != null) {
                mChildFragmentManager.restoreSaveStateInternal(childFragmentManagerState);
                mChildFragmentManager.dispatchCreate();
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onViewCreated(View, Bundle)}.
     * <p>A default View can be returned by calling {@link #Fragment(int)} in your
     * constructor. Otherwise, this method returns null.
     *
     * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
     * logic that operates on the returned View to {@link #onViewCreated(View, Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @MainThread
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (mContentLayoutId != 0) {
            return inflater.inflate(mContentLayoutId, container, false);
        }
        return null;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    }

    /**
     * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}),
     * if provided.
     *
     * @return The fragment's root view, or null if it has no layout.
     */
    @Nullable
    public View getView() {
        return mView;
    }

    /**
     * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}).
     *
     * @throws IllegalStateException if no view was returned by {@link #onCreateView}.
     * @see #getView()
     */
    @NonNull
    public final View requireView() {
        View view = getView();
        if (view == null) {
            throw new IllegalStateException("Fragment " + this + " did not return a View from"
                    + " onCreateView() or this was called before onCreateView().");
        }
        return view;
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     *
     * @deprecated use {@link #onViewCreated(View, Bundle)} for code touching
     * the view created by {@link #onCreateView} and {@link #onCreate(Bundle)} for other
     * initialization.
     * To get a callback specifically when a Fragment activity's
     * {@link Activity#onCreate(Bundle)} is called, register a
     * {@link androidx.lifecycle.LifecycleObserver} on the Activity's
     * {@link Lifecycle} in {@link #onAttach(Context)}, removing it when it receives the
     * {@link Lifecycle.State#CREATED} callback.
     */
    @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
    @MainThread
    @CallSuper
    @Deprecated
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onViewCreated(View, Bundle)} and before {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @MainThread
    @CallSuper
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    @MainThread
    @CallSuper
    public void onStart() {
        mCalled = true;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @MainThread
    @CallSuper
    public void onResume() {
        mCalled = true;
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance if its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onViewCreated(View, Bundle)}.
     *
     * <p>This corresponds to {@link Activity#onSaveInstanceState(Bundle)
     * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
     * applies here as well.  Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @MainThread
    public void onSaveInstanceState(@NonNull Bundle outState) {
    }

    /**
     * Called when the Fragment's activity changes from fullscreen mode to multi-window mode and
     * visa-versa. This is generally tied to {@link Activity#onMultiWindowModeChanged} of the
     * containing Activity.
     *
     * @param isInMultiWindowMode True if the activity is in multi-window mode.
     */
    @SuppressWarnings("unused")
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
    }

    /**
     * Called by the system when the activity changes to and from picture-in-picture mode. This is
     * generally tied to {@link Activity#onPictureInPictureModeChanged} of the containing Activity.
     *
     * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
     */
    @SuppressWarnings("unused")
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    }

    @Override
    @CallSuper
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        mCalled = true;
    }

    /**
     * Callback for when the primary navigation state of this Fragment has changed. This can be
     * the result of the {@link #getParentFragmentManager()}  containing FragmentManager} having its
     * primary navigation fragment changed via
     * {@link androidx.fragment.app.FragmentTransaction#setPrimaryNavigationFragment} or due to
     * the primary navigation fragment changing in a parent FragmentManager.
     *
     * @param isPrimaryNavigationFragment True if and only if this Fragment and any
     * {@link #getParentFragment() parent fragment} is set as the primary navigation fragment
     * via {@link androidx.fragment.app.FragmentTransaction#setPrimaryNavigationFragment}.
     */
    @MainThread
    public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @MainThread
    @CallSuper
    public void onPause() {
        mCalled = true;
    }

    /**
     * Called when the Fragment is no longer started.  This is generally
     * tied to {@link Activity#onStop() Activity.onStop} of the containing
     * Activity's lifecycle.
     */
    @MainThread
    @CallSuper
    public void onStop() {
        mCalled = true;
    }

    @MainThread
    @Override
    @CallSuper
    public void onLowMemory() {
        mCalled = true;
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @MainThread
    @CallSuper
    public void onDestroyView() {
        mCalled = true;
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @MainThread
    @CallSuper
    public void onDestroy() {
        mCalled = true;
    }

    /**
     * Called by the fragment manager once this fragment has been removed,
     * so that we don't have any left-over state if the application decides
     * to re-use the instance.  This only clears state that the framework
     * internally manages, not things the application sets.
     */
    void initState() {
        initLifecycle();
        mPreviousWho = mWho;
        mWho = UUID.randomUUID().toString();
        mAdded = false;
        mRemoving = false;
        mFromLayout = false;
        mInLayout = false;
        mRestored = false;
        mBackStackNesting = 0;
        mFragmentManager = null;
        mChildFragmentManager = new FragmentManagerImpl();
        mHost = null;
        mFragmentId = 0;
        mContainerId = 0;
        mTag = null;
        mHidden = false;
        mDetached = false;
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @MainThread
    @CallSuper
    public void onDetach() {
        mCalled = true;
    }

    /**
     * Initialize the contents of the Fragment host's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link Activity#onCreateOptionsMenu(Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu The options menu in which you place your items.
     *
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     *
     * @deprecated {@link androidx.activity.ComponentActivity} now implements {@link MenuHost},
     * an interface that allows any component, including your activity itself, to add menu items
     * by calling {@link #addMenuProvider(MenuProvider)} without forcing all components through
     * this single method override. As this provides a consistent, optionally {@link Lifecycle}
     * -aware, and modular way to handle menu creation and item selection, replace usages of this
     * method with one or more calls to {@link #addMenuProvider(MenuProvider)} in your Activity's
     * {@link #onCreate(Bundle)} method, having each provider override
     * {@link MenuProvider#onCreateMenu(Menu, MenuInflater)} to create their menu items.
     */
    @MainThread
    @Deprecated
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    }

    /**
     * Prepare the Fragment host's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.  See
     * {@link Activity#onPrepareOptionsMenu(Menu) Activity.onPrepareOptionsMenu}
     * for more information.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     *
     * @see #setHasOptionsMenu
     * @see #onCreateOptionsMenu
     *
     * @deprecated {@link androidx.activity.ComponentActivity} now implements {@link MenuHost},
     * an interface that allows any component, including your activity itself, to add menu items
     * by calling {@link #addMenuProvider(MenuProvider)} without forcing all components through
     * this single method override. The {@link MenuProvider} interface uses a single
     * {@link MenuProvider#onCreateMenu(Menu, MenuInflater)} method for managing both the creation
     * and preparation of menu items. Replace usages of this method with one or more calls to
     * {@link #addMenuProvider(MenuProvider)} in your Activity's {@link #onCreate(Bundle)} method,
     * moving any preparation of menu items to {@link MenuProvider#onPrepareMenu(Menu)}.
     */
    @MainThread
    @Deprecated
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
    }

    /**
     * Called when this fragment's option menu items are no longer being
     * included in the overall options menu.  Receiving this call means that
     * the menu needed to be rebuilt, but this fragment's items were not
     * included in the newly built menu (its {@link #onCreateOptionsMenu(Menu, MenuInflater)}
     * was not called).
     *
     * @deprecated {@link androidx.activity.ComponentActivity} now implements {@link MenuHost},
     * an interface that allows any component, including your activity itself, to add menu items
     * by calling {@link #addMenuProvider(MenuProvider)} without forcing all components through
     * this single method override. Each {@link MenuProvider} then provides a consistent, optionally
     * {@link Lifecycle}-aware, and modular way to handle menu item selection for the menu items
     * created by that provider. Replace usages of this method with one or more calls to
     * {@link #removeMenuProvider(MenuProvider)} in your Activity's {@link #onCreate(Bundle)}
     * method, whenever it is necessary to remove the individual {@link MenuProvider}. If a
     * {@link MenuProvider} was added with Lifecycle-awareness, this removal will happen
     * automatically.
     */
    @MainThread
    @Deprecated
    public void onDestroyOptionsMenu() {
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onCreateOptionsMenu
     *
     * @deprecated {@link androidx.activity.ComponentActivity} now implements {@link MenuHost},
     * an interface that allows any component, including your activity itself, to add menu items
     * by calling {@link #addMenuProvider(MenuProvider)} without forcing all components through
     * this single method override. Each {@link MenuProvider} then provides a consistent, optionally
     * {@link Lifecycle}-aware, and modular way to handle menu item selection for the menu items
     * created by that provider. Replace usages of this method with one or more calls to
     * {@link #addMenuProvider(MenuProvider)} in your Activity's {@link #onCreate(Bundle)} method,
     * delegating menu item selection to the individual {@link MenuProvider} that created the menu
     * items you wish to handle.
     */
    @SuppressWarnings("unused")
    @MainThread
    @Deprecated
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return false;
    }

    /**
     * This hook is called whenever the options menu is being closed (either by the user canceling
     * the menu with the back/menu button, or when an item is selected).
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     *
     * @deprecated {@link androidx.activity.ComponentActivity} now implements {@link MenuHost},
     * an interface that allows any component, including your activity itself, to add menu items
     * by calling {@link #addMenuProvider(MenuProvider)} without forcing all components through
     * this single method override. The {@link MenuProvider} interface uses a single
     * {@link MenuProvider#onCreateMenu(Menu, MenuInflater)} method for managing both the creation
     * and preparation of menu items. Replace usages of this method with one or more calls to
     * {@link #addMenuProvider(MenuProvider)} in your Activity's {@link #onCreate(Bundle)} method,
     * overriding {@link MenuProvider#onMenuClosed(Menu)} to delegate menu closing to the
     * individual {@link MenuProvider} that created the menu.
     */
    @SuppressWarnings("unused")
    @MainThread
    @Deprecated
    public void onOptionsMenuClosed(@NonNull Menu menu) {
    }

    /**
     * Called when a context menu for the {@code view} is about to be shown.
     * Unlike {@link #onCreateOptionsMenu}, this will be called every
     * time the context menu is about to be shown and should be populated for
     * the view (or item inside the view for {@link AdapterView} subclasses,
     * this can be found in the {@code menuInfo})).
     * <p>
     * Use {@link #onContextItemSelected(android.view.MenuItem)} to know when an
     * item has been selected.
     * <p>
     * The default implementation calls up to
     * {@link Activity#onCreateContextMenu Activity.onCreateContextMenu}, though
     * you can not call this implementation if you don't want that behavior.
     * <p>
     * It is not safe to hold onto the context menu after this method returns.
     * {@inheritDoc}
     */
    @MainThread
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
            @Nullable ContextMenuInfo menuInfo) {
        requireActivity().onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * Registers a context menu to be shown for the given view (multiple views
     * can show the context menu). This method will set the
     * {@link OnCreateContextMenuListener} on the view to this fragment, so
     * {@link #onCreateContextMenu(ContextMenu, View, ContextMenuInfo)} will be
     * called when it is time to show the context menu.
     *
     * @see #unregisterForContextMenu(View)
     * @param view The view that should show a context menu.
     */
    public void registerForContextMenu(@NonNull View view) {
        view.setOnCreateContextMenuListener(this);
    }

    /**
     * Prevents a context menu to be shown for the given view. This method will
     * remove the {@link OnCreateContextMenuListener} on the view.
     *
     * @see #registerForContextMenu(View)
     * @param view The view that should stop showing a context menu.
     */
    public void unregisterForContextMenu(@NonNull View view) {
        view.setOnCreateContextMenuListener(null);
    }

    /**
     * This hook is called whenever an item in a context menu is selected. The
     * default implementation simply returns false to have the normal processing
     * happen (calling the item's Runnable or sending a message to its Handler
     * as appropriate). You can use this method for any items for which you
     * would like to do processing without those other facilities.
     * <p>
     * Use {@link MenuItem#getMenuInfo()} to get extra information set by the
     * View that added this menu item.
     * <p>
     * Derived classes should call through to the base class for it to perform
     * the default menu handling.
     *
     * @param item The context menu item that was selected.
     * @return boolean Return false to allow normal context menu processing to
     *         proceed, true to consume it here.
     */
    @SuppressWarnings("unused")
    @MainThread
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return false;
    }

    /**
     * When custom transitions are used with Fragments, the enter transition callback
     * is called when this Fragment is attached or detached when not popping the back stack.
     *
     * @param callback Used to manipulate the shared element transitions on this Fragment
     *                 when added not as a pop from the back stack.
     */
    public void setEnterSharedElementCallback(@Nullable SharedElementCallback callback) {
        ensureAnimationInfo().mEnterTransitionCallback = callback;
    }

    /**
     * When custom transitions are used with Fragments, the exit transition callback
     * is called when this Fragment is attached or detached when popping the back stack.
     *
     * @param callback Used to manipulate the shared element transitions on this Fragment
     *                 when added as a pop from the back stack.
     */
    public void setExitSharedElementCallback(@Nullable SharedElementCallback callback) {
        ensureAnimationInfo().mExitTransitionCallback = callback;
    }

    /**
     * Sets the Transition that will be used to move Views into the initial scene. The entering
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#INVISIBLE} to {@link View#VISIBLE}. If <code>transition</code> is null,
     * entering Views will remain unaffected.
     *
     * @param transition The Transition to use to move Views into the initial Scene.
     *         <code>transition</code> must be an
     *         {@link android.transition.Transition} or
     *         {@link androidx.transition.Transition}.
     */
    public void setEnterTransition(@Nullable Object transition) {
        ensureAnimationInfo().mEnterTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views into the initial scene. The entering
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#INVISIBLE} to {@link View#VISIBLE}.
     *
     * @return the Transition to use to move Views into the initial Scene.
     */
    @Nullable
    public Object getEnterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mEnterTransition;
    }

    /**
     * Sets the Transition that will be used to move Views out of the scene when the Fragment is
     * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#VISIBLE} to {@link View#INVISIBLE}. If <code>transition</code> is null,
     * entering Views will remain unaffected. If nothing is set, the default will be to
     * use the same value as set in {@link #setEnterTransition(Object)}.
     *
     * @param transition The Transition to use to move Views out of the Scene when the Fragment
     *         is preparing to close due to popping the back stack. <code>transition</code> must be
     *         an {@link android.transition.Transition} or
     *         {@link androidx.transition.Transition}.
     */
    public void setReturnTransition(@Nullable Object transition) {
        ensureAnimationInfo().mReturnTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views out of the scene when the Fragment is
     * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#VISIBLE} to {@link View#INVISIBLE}. If nothing is set, the default will be to use
     * the same transition as {@link #getEnterTransition()}.
     *
     * @return the Transition to use to move Views out of the Scene when the Fragment
     *         is preparing to close due to popping the back stack.
     */
    @Nullable
    public Object getReturnTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mReturnTransition == USE_DEFAULT_TRANSITION ? getEnterTransition()
                : mAnimationInfo.mReturnTransition;
    }

    /**
     * Sets the Transition that will be used to move Views out of the scene when the
     * fragment is removed, hidden, or detached when not popping the back stack.
     * The exiting Views will be those that are regular Views or ViewGroups that
     * have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as exiting is governed by changing visibility
     * from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null, the views will
     * remain unaffected.
     *
     * @param transition The Transition to use to move Views out of the Scene when the Fragment
     *          is being closed not due to popping the back stack. <code>transition</code>
     *          must be an
     *          {@link android.transition.Transition} or
     *          {@link androidx.transition.Transition}.
     */
    public void setExitTransition(@Nullable Object transition) {
        ensureAnimationInfo().mExitTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views out of the scene when the
     * fragment is removed, hidden, or detached when not popping the back stack.
     * The exiting Views will be those that are regular Views or ViewGroups that
     * have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as exiting is governed by changing visibility
     * from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null, the views will
     * remain unaffected.
     *
     * @return the Transition to use to move Views out of the Scene when the Fragment
     *         is being closed not due to popping the back stack.
     */
    @Nullable
    public Object getExitTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mExitTransition;
    }

    /**
     * Sets the Transition that will be used to move Views in to the scene when returning due
     * to popping a back stack. The entering Views will be those that are regular Views
     * or ViewGroups that have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions
     * will extend {@link android.transition.Visibility} as exiting is governed by changing
     * visibility from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null,
     * the views will remain unaffected. If nothing is set, the default will be to use the same
     * transition as {@link #getExitTransition()}.
     *
     * @param transition The Transition to use to move Views into the scene when reentering from a
     *          previously-started Activity due to popping the back stack. <code>transition</code>
     *          must be an
     *          {@link android.transition.Transition} or
     *          {@link androidx.transition.Transition}.
     */
    public void setReenterTransition(@Nullable Object transition) {
        ensureAnimationInfo().mReenterTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views in to the scene when returning due
     * to popping a back stack. The entering Views will be those that are regular Views
     * or ViewGroups that have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions
     * will extend {@link android.transition.Visibility} as exiting is governed by changing
     * visibility from {@link View#VISIBLE} to {@link View#INVISIBLE}. If nothing is set, the
     * default will be to use the same transition as {@link #getExitTransition()}.
     *
     * @return the Transition to use to move Views into the scene when reentering from a
     *                   previously-started Activity due to popping the back stack.
     */
    @Nullable
    public Object getReenterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mReenterTransition == USE_DEFAULT_TRANSITION ? getExitTransition()
                : mAnimationInfo.mReenterTransition;
    }

    /**
     * Sets the Transition that will be used for shared elements transferred into the content
     * Scene. Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     *
     * @param transition The Transition to use for shared elements transferred into the content
     *          Scene.  <code>transition</code> must be an
     *          {@link android.transition.Transition android.transition.Transition} or
     *          {@link androidx.transition.Transition androidx.transition.Transition}.
     */
    public void setSharedElementEnterTransition(@Nullable Object transition) {
        ensureAnimationInfo().mSharedElementEnterTransition = transition;
    }

    /**
     * Returns the Transition that will be used for shared elements transferred into the content
     * Scene. Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     *
     * @return The Transition to use for shared elements transferred into the content
     *                   Scene.
     */
    @Nullable
    public Object getSharedElementEnterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mSharedElementEnterTransition;
    }

    /**
     * Sets the Transition that will be used for shared elements transferred back during a
     * pop of the back stack. This Transition acts in the leaving Fragment.
     * Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     * If no value is set, the default will be to use the same value as
     * {@link #setSharedElementEnterTransition(Object)}.
     *
     * @param transition The Transition to use for shared elements transferred out of the content
     *          Scene. <code>transition</code> must be an
     *          {@link android.transition.Transition android.transition.Transition} or
     *          {@link androidx.transition.Transition androidx.transition.Transition}.
     */
    public void setSharedElementReturnTransition(@Nullable Object transition) {
        ensureAnimationInfo().mSharedElementReturnTransition = transition;
    }

    /**
     * Return the Transition that will be used for shared elements transferred back during a
     * pop of the back stack. This Transition acts in the leaving Fragment.
     * Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     * If no value is set, the default will be to use the same value as
     * {@link #setSharedElementEnterTransition(Object)}.
     *
     * @return The Transition to use for shared elements transferred out of the content
     *                   Scene.
     */
    @Nullable
    public Object getSharedElementReturnTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mSharedElementReturnTransition == USE_DEFAULT_TRANSITION
                ? getSharedElementEnterTransition()
                : mAnimationInfo.mSharedElementReturnTransition;
    }

    /**
     * Sets whether the the exit transition and enter transition overlap or not.
     * When true, the enter transition will start as soon as possible. When false, the
     * enter transition will wait until the exit transition completes before starting.
     *
     * @param allow true to start the enter transition when possible or false to
     *              wait until the exiting transition completes.
     */
    public void setAllowEnterTransitionOverlap(boolean allow) {
        ensureAnimationInfo().mAllowEnterTransitionOverlap = allow;
    }

    /**
     * Returns whether the the exit transition and enter transition overlap or not.
     * When true, the enter transition will start as soon as possible. When false, the
     * enter transition will wait until the exit transition completes before starting.
     *
     * @return true when the enter transition should start as soon as possible or false to
     * when it should wait until the exiting transition completes.
     */
    public boolean getAllowEnterTransitionOverlap() {
        return (mAnimationInfo == null || mAnimationInfo.mAllowEnterTransitionOverlap == null)
                ? true : mAnimationInfo.mAllowEnterTransitionOverlap;
    }

    /**
     * Sets whether the the return transition and reenter transition overlap or not.
     * When true, the reenter transition will start as soon as possible. When false, the
     * reenter transition will wait until the return transition completes before starting.
     *
     * @param allow true to start the reenter transition when possible or false to wait until the
     *              return transition completes.
     */
    public void setAllowReturnTransitionOverlap(boolean allow) {
        ensureAnimationInfo().mAllowReturnTransitionOverlap = allow;
    }

    /**
     * Returns whether the the return transition and reenter transition overlap or not.
     * When true, the reenter transition will start as soon as possible. When false, the
     * reenter transition will wait until the return transition completes before starting.
     *
     * @return true to start the reenter transition when possible or false to wait until the
     *         return transition completes.
     */
    public boolean getAllowReturnTransitionOverlap() {
        return (mAnimationInfo == null || mAnimationInfo.mAllowReturnTransitionOverlap == null)
                ? true : mAnimationInfo.mAllowReturnTransitionOverlap;
    }

    /**
     * Postpone the entering Fragment transition until {@link #startPostponedEnterTransition()}
     * or {@link FragmentManager#executePendingTransactions()} has been called.
     * <p>
     * This method gives the Fragment the ability to delay Fragment animations
     * until all data is loaded. Until then, the added, shown, and
     * attached Fragments will be INVISIBLE and removed, hidden, and detached Fragments won't
     * be have their Views removed. The transaction runs when all postponed added Fragments in the
     * transaction have called {@link #startPostponedEnterTransition()}.
     * <p>
     * This method should be called before being added to the FragmentTransaction or
     * in {@link #onCreate(Bundle)}, {@link #onAttach(Context)}, or
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}}.
     * {@link #startPostponedEnterTransition()} must be called to allow the Fragment to
     * start the transitions.
     * <p>
     * When a FragmentTransaction is started that may affect a postponed FragmentTransaction,
     * based on which containers are in their operations, the postponed FragmentTransaction
     * will have its start triggered. The early triggering may result in faulty or nonexistent
     * animations in the postponed transaction. FragmentTransactions that operate only on
     * independent containers will not interfere with each other's postponement.
     * <p>
     * Calling postponeEnterTransition on Fragments with a null View will not postpone the
     * transition.
     *
     * @see Activity#postponeEnterTransition()
     * @see FragmentTransaction#setReorderingAllowed(boolean)
     */
    public void postponeEnterTransition() {
        ensureAnimationInfo().mEnterTransitionPostponed = true;
    }

    /**
     * Postpone the entering Fragment transition for a given amount of time and then call
     * {@link #startPostponedEnterTransition()}.
     * <p>
     * This method gives the Fragment the ability to delay Fragment animations for a given amount
     * of time. Until then, the added, shown, and attached Fragments will be INVISIBLE and removed,
     * hidden, and detached Fragments won't be have their Views removed. The transaction runs when
     * all postponed added Fragments in the transaction have called
     * {@link #startPostponedEnterTransition()}.
     * <p>
     * This method should be called before being added to the FragmentTransaction or
     * in {@link #onCreate(Bundle)}, {@link #onAttach(Context)}, or
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}}.
     * <p>
     * When a FragmentTransaction is started that may affect a postponed FragmentTransaction,
     * based on which containers are in their operations, the postponed FragmentTransaction
     * will have its start triggered. The early triggering may result in faulty or nonexistent
     * animations in the postponed transaction. FragmentTransactions that operate only on
     * independent containers will not interfere with each other's postponement.
     * <p>
     * Calling postponeEnterTransition on Fragments with a null View will not postpone the
     * transition.
     *
     * @param duration The length of the delay in {@code timeUnit} units
     * @param timeUnit The units of time for {@code duration}
     * @see Activity#postponeEnterTransition()
     * @see FragmentTransaction#setReorderingAllowed(boolean)
     */
    public final void postponeEnterTransition(long duration, @NonNull TimeUnit timeUnit) {
        ensureAnimationInfo().mEnterTransitionPostponed = true;
        if (mPostponedHandler != null) {
            mPostponedHandler.removeCallbacks(mPostponedDurationRunnable);
        }
        if (mFragmentManager != null) {
            mPostponedHandler = mFragmentManager.getHost().getHandler();
        } else {
            mPostponedHandler = new Handler(Looper.getMainLooper());
        }
        mPostponedHandler.removeCallbacks(mPostponedDurationRunnable);
        mPostponedHandler.postDelayed(mPostponedDurationRunnable, timeUnit.toMillis(duration));
    }

    /**
     * Begin postponed transitions after {@link #postponeEnterTransition()} was called.
     * If postponeEnterTransition() was called, you must call startPostponedEnterTransition()
     * or {@link FragmentManager#executePendingTransactions()} to complete the FragmentTransaction.
     * If postponement was interrupted with {@link FragmentManager#executePendingTransactions()},
     * before {@code startPostponedEnterTransition()}, animations may not run or may execute
     * improperly.
     *
     * @see Activity#startPostponedEnterTransition()
     */
    public void startPostponedEnterTransition() {
        if (mAnimationInfo == null || !ensureAnimationInfo().mEnterTransitionPostponed) {
            // If you never called postponeEnterTransition(), there's nothing for us to do
            return;
        }
        if (mHost == null) {
            ensureAnimationInfo().mEnterTransitionPostponed = false;
        } else if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            mHost.getHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    callStartTransitionListener(false);
                }
            });
        } else {
            callStartTransitionListener(true);
        }
    }

    /**
     * Calls the start transition listener. This must be called on the UI thread.
     *
     * @param calledDirectly Whether this was called directly or if it was already posted
     *                       to the UI thread
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void callStartTransitionListener(boolean calledDirectly) {
        if (mAnimationInfo != null) {
            mAnimationInfo.mEnterTransitionPostponed = false;
        }
        if (mView != null && mContainer != null && mFragmentManager != null) {
            // Mark the updated postponed state with the SpecialEffectsController immediately
            final SpecialEffectsController controller = SpecialEffectsController
                    .getOrCreateController(mContainer, mFragmentManager);
            controller.markPostponedState();
            if (calledDirectly) {
                // But if this call was called directly, we need to post the
                // executePendingOperations() to avoid re-entrant calls
                // and avoid calling execute during layout / draw calls
                mHost.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        controller.executePendingOperations();
                    }
                });
            } else {
                // We've already posted our call, so we can execute directly
                controller.executePendingOperations();
            }
            if (mPostponedHandler != null) {
                mPostponedHandler.removeCallbacks(mPostponedDurationRunnable);
                mPostponedHandler = null;
            }
        }
    }

    /**
     * Print the Fragments's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    @SuppressWarnings("deprecation")
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, @Nullable String[] args) {
        writer.print(prefix); writer.print("mFragmentId=#");
                writer.print(Integer.toHexString(mFragmentId));
                writer.print(" mContainerId=#");
                writer.print(Integer.toHexString(mContainerId));
                writer.print(" mTag="); writer.println(mTag);
        writer.print(prefix); writer.print("mState="); writer.print(mState);
                writer.print(" mWho="); writer.print(mWho);
                writer.print(" mBackStackNesting="); writer.println(mBackStackNesting);
        writer.print(prefix); writer.print("mAdded="); writer.print(mAdded);
                writer.print(" mRemoving="); writer.print(mRemoving);
                writer.print(" mFromLayout="); writer.print(mFromLayout);
                writer.print(" mInLayout="); writer.println(mInLayout);
        writer.print(prefix); writer.print("mHidden="); writer.print(mHidden);
                writer.print(" mDetached="); writer.print(mDetached);
                writer.print(" mMenuVisible="); writer.print(mMenuVisible);
                writer.print(" mHasMenu="); writer.println(mHasMenu);
        writer.print(prefix); writer.print("mRetainInstance="); writer.print(mRetainInstance);
                writer.print(" mUserVisibleHint="); writer.println(mUserVisibleHint);
        if (mFragmentManager != null) {
            writer.print(prefix); writer.print("mFragmentManager=");
                    writer.println(mFragmentManager);
        }
        if (mHost != null) {
            writer.print(prefix); writer.print("mHost=");
                    writer.println(mHost);
        }
        if (mParentFragment != null) {
            writer.print(prefix); writer.print("mParentFragment=");
                    writer.println(mParentFragment);
        }
        if (mArguments != null) {
            writer.print(prefix); writer.print("mArguments="); writer.println(mArguments);
        }
        if (mSavedFragmentState != null) {
            writer.print(prefix); writer.print("mSavedFragmentState=");
                    writer.println(mSavedFragmentState);
        }
        if (mSavedViewState != null) {
            writer.print(prefix); writer.print("mSavedViewState=");
                    writer.println(mSavedViewState);
        }
        if (mSavedViewRegistryState != null) {
            writer.print(prefix); writer.print("mSavedViewRegistryState=");
                    writer.println(mSavedViewRegistryState);
        }
        Fragment target = getTargetFragment(false);
        if (target != null) {
            writer.print(prefix); writer.print("mTarget="); writer.print(target);
                    writer.print(" mTargetRequestCode=");
                    writer.println(mTargetRequestCode);
        }
        writer.print(prefix); writer.print("mPopDirection="); writer.println(getPopDirection());
        if (getEnterAnim() != 0) {
            writer.print(prefix); writer.print("getEnterAnim="); writer.println(getEnterAnim());
        }
        if (getExitAnim() != 0) {
            writer.print(prefix); writer.print("getExitAnim="); writer.println(getExitAnim());
        }
        if (getPopEnterAnim() != 0) {
            writer.print(prefix); writer.print("getPopEnterAnim=");
            writer.println(getPopEnterAnim());
        }
        if (getPopExitAnim() != 0) {
            writer.print(prefix); writer.print("getPopExitAnim="); writer.println(getPopExitAnim());
        }
        if (mContainer != null) {
            writer.print(prefix); writer.print("mContainer="); writer.println(mContainer);
        }
        if (mView != null) {
            writer.print(prefix); writer.print("mView="); writer.println(mView);
        }
        if (getAnimatingAway() != null) {
            writer.print(prefix);
            writer.print("mAnimatingAway=");
            writer.println(getAnimatingAway());
        }
        if (getContext() != null) {
            LoaderManager.getInstance(this).dump(prefix, fd, writer, args);
        }
        writer.print(prefix);
        writer.println("Child " + mChildFragmentManager + ":");
        mChildFragmentManager.dump(prefix + "  ", fd, writer, args);
    }

    @Nullable
    Fragment findFragmentByWho(@NonNull String who) {
        if (who.equals(mWho)) {
            return this;
        }
        return mChildFragmentManager.findFragmentByWho(who);
    }

    @NonNull
    FragmentContainer createFragmentContainer() {
        return new FragmentContainer() {
            @Override
            @Nullable
            public View onFindViewById(int id) {
                if (mView == null) {
                    throw new IllegalStateException("Fragment " + Fragment.this
                            + " does not have a view");
                }
                return mView.findViewById(id);
            }

            @Override
            public boolean onHasView() {
                return (mView != null);
            }
        };
    }

    void performAttach() {
        for (OnPreAttachedListener listener: mOnPreAttachedListeners) {
            listener.onPreAttached();
        }
        mOnPreAttachedListeners.clear();
        mChildFragmentManager.attachController(mHost, createFragmentContainer(), this);
        mState = ATTACHED;
        mCalled = false;
        onAttach(mHost.getContext());
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onAttach()");
        }
        mFragmentManager.dispatchOnAttachFragment(this);
        mChildFragmentManager.dispatchAttach();
    }

    void performCreate(Bundle savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mState = CREATED;
        mCalled = false;
        if (Build.VERSION.SDK_INT >= 19) {
            mLifecycleRegistry.addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source,
                        @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (mView != null) {
                            Api19Impl.cancelPendingInputEvents(mView);
                        }
                    }
                }
            });
        }
        onCreate(savedInstanceState);
        mIsCreated = true;
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onCreate()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    void performCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mPerformedCreateView = true;
        mViewLifecycleOwner = new FragmentViewLifecycleOwner(this, getViewModelStore(),
                () -> {
                    // Perform the restore as soon as the FragmentViewLifecycleOwner
                    // becomes initialized, to ensure it is always available
                    mViewLifecycleOwner.performRestore(mSavedViewRegistryState);
                    mSavedViewRegistryState = null;
                });
        mView = onCreateView(inflater, container, savedInstanceState);
        if (mView != null) {
            // Initialize the view lifecycle
            mViewLifecycleOwner.initialize();
            // Tell the fragment's new view about it before we tell anyone listening
            // to mViewLifecycleOwnerLiveData and before onViewCreated, so that calls to
            // ViewTree get() methods return something meaningful
            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(FragmentManager.TAG, "Setting ViewLifecycleOwner on View " + mView
                        + " for Fragment " + this);
            }
            ViewTreeLifecycleOwner.set(mView, mViewLifecycleOwner);
            ViewTreeViewModelStoreOwner.set(mView, mViewLifecycleOwner);
            ViewTreeSavedStateRegistryOwner.set(mView, mViewLifecycleOwner);
            // Then inform any Observers of the new LifecycleOwner
            mViewLifecycleOwnerLiveData.setValue(mViewLifecycleOwner);
        } else {
            if (mViewLifecycleOwner.isInitialized()) {
                throw new IllegalStateException("Called getViewLifecycleOwner() but "
                        + "onCreateView() returned null");
            }
            mViewLifecycleOwner = null;
        }
    }

    void performViewCreated() {
        // since calling super.onViewCreated() is not required, we do not need to set and check the
        // `mCalled` flag
        Bundle savedInstanceState = null;
        if (mSavedFragmentState != null) {
            savedInstanceState = mSavedFragmentState.getBundle(
                    FragmentStateManager.SAVED_INSTANCE_STATE_KEY);
        }
        onViewCreated(mView, savedInstanceState);
        mChildFragmentManager.dispatchViewCreated();
    }

    @SuppressWarnings("deprecation")
    void performActivityCreated(Bundle savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mState = AWAITING_EXIT_EFFECTS;
        mCalled = false;
        onActivityCreated(savedInstanceState);
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onActivityCreated()");
        }
        restoreViewState();
        mChildFragmentManager.dispatchActivityCreated();
    }

    @SuppressWarnings("deprecation")
    private void restoreViewState() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(FragmentManager.TAG, "moveto RESTORE_VIEW_STATE: " + this);
        }
        if (mView != null) {
            Bundle savedInstanceState = null;
            if (mSavedFragmentState != null) {
                savedInstanceState = mSavedFragmentState.getBundle(
                        FragmentStateManager.SAVED_INSTANCE_STATE_KEY);
            }
            restoreViewState(savedInstanceState);
        }
        mSavedFragmentState = null;
    }

    @SuppressWarnings("ConstantConditions")
    void performStart() {
        mChildFragmentManager.noteStateNotSaved();
        mChildFragmentManager.execPendingActions(true);
        mState = STARTED;
        mCalled = false;
        onStart();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onStart()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        if (mView != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }
        mChildFragmentManager.dispatchStart();
    }

    @SuppressWarnings("ConstantConditions")
    void performResume() {
        mChildFragmentManager.noteStateNotSaved();
        mChildFragmentManager.execPendingActions(true);
        mState = RESUMED;
        mCalled = false;
        onResume();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onResume()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        if (mView != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }
        mChildFragmentManager.dispatchResume();
    }

    void noteStateNotSaved() {
        mChildFragmentManager.noteStateNotSaved();
    }

    void performPrimaryNavigationFragmentChanged() {
        boolean isPrimaryNavigationFragment = mFragmentManager.isPrimaryNavigation(this);
        // Only send out the callback / dispatch if the state has changed
        if (mIsPrimaryNavigationFragment == null
                || mIsPrimaryNavigationFragment != isPrimaryNavigationFragment) {
            mIsPrimaryNavigationFragment = isPrimaryNavigationFragment;
            onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment);
            mChildFragmentManager.dispatchPrimaryNavigationFragmentChanged();
        }
    }

    void performMultiWindowModeChanged(boolean isInMultiWindowMode) {
        onMultiWindowModeChanged(isInMultiWindowMode);
    }

    void performPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    void performConfigurationChanged(@NonNull Configuration newConfig) {
        onConfigurationChanged(newConfig);
    }

    void performLowMemory() {
        onLowMemory();
    }

    /*
    void performTrimMemory(int level) {
        onTrimMemory(level);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchTrimMemory(level);
        }
    }
    */

    boolean performCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onCreateOptionsMenu(menu, inflater);
            }
            show |= mChildFragmentManager.dispatchCreateOptionsMenu(menu, inflater);
        }
        return show;
    }

    boolean performPrepareOptionsMenu(@NonNull Menu menu) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onPrepareOptionsMenu(menu);
            }
            show |= mChildFragmentManager.dispatchPrepareOptionsMenu(menu);
        }
        return show;
    }

    boolean performOptionsItemSelected(@NonNull MenuItem item) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                if (onOptionsItemSelected(item)) {
                    return true;
                }
            }
            return mChildFragmentManager.dispatchOptionsItemSelected(item);
        }
        return false;
    }

    boolean performContextItemSelected(@NonNull MenuItem item) {
        if (!mHidden) {
            if (onContextItemSelected(item)) {
                return true;
            }
            return mChildFragmentManager.dispatchContextItemSelected(item);
        }
        return false;
    }

    void performOptionsMenuClosed(@NonNull Menu menu) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                onOptionsMenuClosed(menu);
            }
            mChildFragmentManager.dispatchOptionsMenuClosed(menu);
        }
    }

    void performSaveInstanceState(Bundle outState) {
        onSaveInstanceState(outState);
    }

    @SuppressWarnings("ConstantConditions")
    void performPause() {
        mChildFragmentManager.dispatchPause();
        if (mView != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mState = AWAITING_ENTER_EFFECTS;
        mCalled = false;
        onPause();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onPause()");
        }
    }

    @SuppressWarnings("ConstantConditions")
    void performStop() {
        mChildFragmentManager.dispatchStop();
        if (mView != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onStop();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onStop()");
        }
    }

    @SuppressWarnings("ConstantConditions")
    void performDestroyView() {
        mChildFragmentManager.dispatchDestroyView();
        if (mView != null && mViewLifecycleOwner.getLifecycle().getCurrentState()
                        .isAtLeast(Lifecycle.State.CREATED)) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }
        mState = CREATED;
        mCalled = false;
        onDestroyView();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onDestroyView()");
        }
        // Handles the detach/reattach case where the view hierarchy
        // is destroyed and recreated and an additional call to
        // onLoadFinished may be needed to ensure the new view
        // hierarchy is populated from data from the Loaders
        LoaderManager.getInstance(this).markForRedelivery();
        mPerformedCreateView = false;
    }

    void performDestroy() {
        mChildFragmentManager.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        mState = ATTACHED;
        mCalled = false;
        mIsCreated = false;
        onDestroy();
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onDestroy()");
        }
    }

    void performDetach() {
        mState = INITIALIZING;
        mCalled = false;
        onDetach();
        mLayoutInflater = null;
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this
                    + " did not call through to super.onDetach()");
        }

        // Destroy the child FragmentManager if we still have it here.
        // This is normally done in performDestroy(), but is done here
        // specifically if the Fragment is retained.
        if (!mChildFragmentManager.isDestroyed()) {
            mChildFragmentManager.dispatchDestroy();
            mChildFragmentManager = new FragmentManagerImpl();
        }
    }

    private AnimationInfo ensureAnimationInfo() {
        if (mAnimationInfo == null) {
            mAnimationInfo = new AnimationInfo();
        }
        return mAnimationInfo;
    }

    void setAnimations(
            @AnimRes int enter,
            @AnimRes int exit,
            @AnimRes int popEnter,
            @AnimRes int popExit) {
        if (mAnimationInfo == null && enter == 0 && exit == 0 && popEnter == 0 && popExit == 0) {
            return; // no change!
        }
        ensureAnimationInfo().mEnterAnim = enter;
        ensureAnimationInfo().mExitAnim = exit;
        ensureAnimationInfo().mPopEnterAnim = popEnter;
        ensureAnimationInfo().mPopExitAnim = popExit;
    }

    @AnimRes
    int getEnterAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mEnterAnim;
    }

    @AnimRes
    int getExitAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mExitAnim;
    }

    @AnimRes
    int getPopEnterAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mPopEnterAnim;
    }

    @AnimRes
    int getPopExitAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mPopExitAnim;
    }

    boolean getPopDirection() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mIsPop;
    }

    void setPopDirection(boolean isPop) {
        if (mAnimationInfo == null) {
            return; // no change!
        }
        ensureAnimationInfo().mIsPop = isPop;
    }

    int getNextTransition() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mNextTransition;
    }

    void setNextTransition(int nextTransition) {
        if (mAnimationInfo == null && nextTransition == 0) {
            return; // no change!
        }
        ensureAnimationInfo();
        mAnimationInfo.mNextTransition = nextTransition;
    }

    @NonNull
    ArrayList<String> getSharedElementSourceNames() {
        if (mAnimationInfo == null || mAnimationInfo.mSharedElementSourceNames == null) {
            return new ArrayList<>();
        }
        return mAnimationInfo.mSharedElementSourceNames;
    }

    @NonNull
    ArrayList<String> getSharedElementTargetNames() {
        if (mAnimationInfo == null || mAnimationInfo.mSharedElementTargetNames == null) {
            return new ArrayList<>();
        }
        return mAnimationInfo.mSharedElementTargetNames;
    }

    void setSharedElementNames(@Nullable ArrayList<String> sharedElementSourceNames,
            @Nullable ArrayList<String> sharedElementTargetNames) {
        ensureAnimationInfo();
        mAnimationInfo.mSharedElementSourceNames = sharedElementSourceNames;
        mAnimationInfo.mSharedElementTargetNames = sharedElementTargetNames;
    }

    SharedElementCallback getEnterTransitionCallback() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mEnterTransitionCallback;
    }

    SharedElementCallback getExitTransitionCallback() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mExitTransitionCallback;
    }

    View getAnimatingAway() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mAnimatingAway;
    }

    void setPostOnViewCreatedAlpha(float alpha) {
        ensureAnimationInfo().mPostOnViewCreatedAlpha = alpha;
    }

    float getPostOnViewCreatedAlpha() {
        if (mAnimationInfo == null) {
            return 1f;
        }
        return mAnimationInfo.mPostOnViewCreatedAlpha;
    }

    void setFocusedView(View view) {
        ensureAnimationInfo().mFocusedView = view;
    }

    View getFocusedView() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mFocusedView;
    }

    boolean isPostponed() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mEnterTransitionPostponed;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the host of this fragment is an {@link ActivityResultRegistryOwner} the
     * {@link ActivityResultRegistry} of the host will be used. Otherwise, this will use the
     * registry of the Fragment's Activity.
     */
    @MainThread
    @NonNull
    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback) {
        return prepareCallInternal(contract, new Function<Void, ActivityResultRegistry>() {
            @Override
            public ActivityResultRegistry apply(Void input) {
                if (mHost instanceof ActivityResultRegistryOwner) {
                    return ((ActivityResultRegistryOwner) mHost).getActivityResultRegistry();
                }
                return requireActivity().getActivityResultRegistry();
            }
        }, callback);
    }

    @MainThread
    @NonNull
    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultRegistry registry,
            @NonNull final ActivityResultCallback<O> callback) {
        return prepareCallInternal(contract, new Function<Void, ActivityResultRegistry>() {
            @Override
            public ActivityResultRegistry apply(Void input) {
                return registry;
            }
        }, callback);
    }

    @NonNull
    private <I, O> ActivityResultLauncher<I> prepareCallInternal(
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final Function<Void, ActivityResultRegistry> registryProvider,
            @NonNull final ActivityResultCallback<O> callback) {
        // Throw if attempting to register after the Fragment is CREATED.
        if (mState > CREATED) {
            throw new IllegalStateException("Fragment " + this + " is attempting to "
                    + "registerForActivityResult after being created. Fragments must call "
                    + "registerForActivityResult() before they are created (i.e. initialization, "
                    + "onAttach(), or onCreate()).");
        }
        final AtomicReference<ActivityResultLauncher<I>> ref = new AtomicReference<>();
        // We can't call generateActivityResultKey during initialization of the Fragment
        // since we need to wait for the mWho to be restored from saved instance state
        // so we'll wait until we have all the information needed to register  to actually
        // generate the key and register.

        registerOnPreAttachListener(new OnPreAttachedListener() {
            @Override
            void onPreAttached() {
                final String key = generateActivityResultKey();
                ActivityResultRegistry registry = registryProvider.apply(null);
                ref.set(registry.register(key, Fragment.this, contract, callback));
            }
        });

        return new ActivityResultLauncher<I>() {
            @Override
            public void launch(I input, @Nullable ActivityOptionsCompat options) {
                ActivityResultLauncher<I> delegate = ref.get();
                if (delegate == null) {
                    throw new IllegalStateException("Operation cannot be started before fragment "
                            + "is in created state");
                }
                delegate.launch(input, options);
            }

            @Override
            public void unregister() {
                ActivityResultLauncher<I> delegate = ref.getAndSet(null);
                if (delegate != null) {
                    delegate.unregister();
                }
            }

            @NonNull
            @Override
            public ActivityResultContract<I, ?> getContract() {
                return contract;
            }
        };
    }

    private void registerOnPreAttachListener(@NonNull final OnPreAttachedListener callback) {
        //If we are already attached, we can register immediately
        if (mState >= ATTACHED) {
            callback.onPreAttached();
        } else {
            // else we need to wait until we are attached
            mOnPreAttachedListeners.add(callback);
        }
    }

    @NonNull
    String generateActivityResultKey() {
        return "fragment_" + mWho + "_rq#" + mNextLocalRequestCode.getAndIncrement();
    }

    /**
     * Contains all the animation and transition information for a fragment. This will only
     * be instantiated for Fragments that have Views.
     */
    static class AnimationInfo {
        // Non-null if the fragment's view hierarchy is currently animating away,
        // meaning we need to wait a bit on completely destroying it.  This is the
        // view that is animating.
        View mAnimatingAway;

        // If app requests the animation direction, this is what to use
        boolean mIsPop;

        // All possible animations
        @AnimRes int mEnterAnim;
        @AnimRes int mExitAnim;
        @AnimRes int mPopEnterAnim;
        @AnimRes int mPopExitAnim;

        // If app has requested a specific transition, this is the one to use.
        int mNextTransition;

        // If app has requested a specific set of shared element objects, this is the one to use.
        ArrayList<String> mSharedElementSourceNames;
        ArrayList<String> mSharedElementTargetNames;

        Object mEnterTransition = null;
        Object mReturnTransition = USE_DEFAULT_TRANSITION;
        Object mExitTransition = null;
        Object mReenterTransition = USE_DEFAULT_TRANSITION;
        Object mSharedElementEnterTransition = null;
        Object mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
        Boolean mAllowReturnTransitionOverlap;
        Boolean mAllowEnterTransitionOverlap;

        SharedElementCallback mEnterTransitionCallback = null;
        SharedElementCallback mExitTransitionCallback = null;

        float mPostOnViewCreatedAlpha = 1f;
        View mFocusedView = null;

        // True when postponeEnterTransition has been called and startPostponeEnterTransition
        // hasn't been called yet.
        boolean mEnterTransitionPostponed;
    }

    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() { }

        static void cancelPendingInputEvents(@NonNull View view) {
            view.cancelPendingInputEvents();
        }
    }
}
