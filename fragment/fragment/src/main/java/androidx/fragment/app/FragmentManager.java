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

import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.core.app.MultiWindowModeChangedInfo;
import androidx.core.app.OnMultiWindowModeChangedProvider;
import androidx.core.app.OnPictureInPictureModeChangedProvider;
import androidx.core.app.PictureInPictureModeChangedInfo;
import androidx.core.content.OnConfigurationChangedProvider;
import androidx.core.content.OnTrimMemoryProvider;
import androidx.core.util.Consumer;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.R;
import androidx.fragment.app.strictmode.FragmentStrictMode;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static library support version of the framework's {@link android.app.FragmentManager}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework {@link FragmentManager}
 * documentation for a class overview.
 *
 * <p>Your activity must derive from {@link FragmentActivity} to use this. From such an activity,
 * you can acquire the {@link FragmentManager} by calling
 * {@link FragmentActivity#getSupportFragmentManager}.
 */
public abstract class FragmentManager implements FragmentResultOwner {
    private static final String SAVED_STATE_KEY = "android:support:fragments";
    private static final String FRAGMENT_MANAGER_STATE_KEY = "state";
    private static final String RESULT_KEY_PREFIX = "result_";
    private static final String FRAGMENT_KEY_PREFIX = "fragment_";

    private static boolean DEBUG = false;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String TAG = "FragmentManager";

    /**
     * Control whether the framework's internal fragment manager debugging
     * logs are turned on.  If enabled, you will see output in logcat as
     * the framework performs fragment operations.
     * @deprecated FragmentManager now respects {@link Log#isLoggable(String, int)} for debug
     * logging, allowing you to use <code>adb shell setprop log.tag.FragmentManager VERBOSE</code>.
     * @see Log#isLoggable(String, int)
     */
    @Deprecated
    public static void enableDebugLogging(boolean enabled) {
        FragmentManager.DEBUG = enabled;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean isLoggingEnabled(int level) {
        return DEBUG || Log.isLoggable(TAG, level);
    }

    /**
     * Flag for {@link #popBackStack(String, int)}
     * and {@link #popBackStack(int, int)}: If set, and the name or ID of
     * a back stack entry has been supplied, then all matching entries will
     * be consumed until one that doesn't match is found or the bottom of
     * the stack is reached.  Otherwise, all entries up to but not including that entry
     * will be removed.
     */
    public static final int POP_BACK_STACK_INCLUSIVE = 1;

    /**
     * Representation of an entry on the fragment back stack, as created
     * with {@link FragmentTransaction#addToBackStack(String)
     * FragmentTransaction.addToBackStack()}.  Entries can later be
     * retrieved with {@link FragmentManager#getBackStackEntryAt(int)
     * FragmentManager.getBackStackEntryAt()}.
     *
     * <p>Note that you should never hold on to a BackStackEntry object;
     * the identifier as returned by {@link #getId} is the only thing that
     * will be persisted across activity instances.
     */
    public interface BackStackEntry {
        /**
         * Return the unique identifier for the entry.  This is the only
         * representation of the entry that will persist across activity
         * instances.
         */
        int getId();

        /**
         * Get the name that was supplied to
         * {@link FragmentTransaction#addToBackStack(String)
         * FragmentTransaction.addToBackStack(String)} when creating this entry.
         */
        @Nullable
        String getName();

        /**
         * Return the full bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         * @deprecated Store breadcrumb titles separately from back stack entries. For example,
         * by using an <code>android:label</code> on a fragment in a navigation graph.
         */
        @Deprecated
        @StringRes
        int getBreadCrumbTitleRes();

        /**
         * Return the short bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         * @deprecated Store breadcrumb short titles separately from back stack entries. For
         * example, by using an <code>android:label</code> on a fragment in a navigation graph.
         */
        @Deprecated
        @StringRes
        int getBreadCrumbShortTitleRes();

        /**
         * Return the full bread crumb title for the entry, or null if it
         * does not have one.
         * @deprecated Store breadcrumb titles separately from back stack entries. For example,
         *          * by using an <code>android:label</code> on a fragment in a navigation graph.
         */
        @Deprecated
        @Nullable
        CharSequence getBreadCrumbTitle();

        /**
         * Return the short bread crumb title for the entry, or null if it
         * does not have one.
         * @deprecated Store breadcrumb short titles separately from back stack entries. For
         * example, by using an <code>android:label</code> on a fragment in a navigation graph.
         */
        @Deprecated
        @Nullable
        CharSequence getBreadCrumbShortTitle();
    }

    /**
     * Interface to watch for changes to the back stack.
     */
    public interface OnBackStackChangedListener {
        /**
         * Called whenever the contents of the back stack change.
         */
        @MainThread
        void onBackStackChanged();

        /**
         * Called whenever the contents of the back stack are starting to be changed, before
         * fragments being to move to their target states.
         *
         * @param fragment that is affected by the starting back stack change
         * @param pop whether this back stack change is a pop
         */
        @MainThread
        default void onBackStackChangeStarted(@NonNull Fragment fragment, boolean pop) { }

        /**
         * Called whenever the contents of a back stack change is committed.
         *
         * @param fragment that is affected by the committed back stack change
         * @param pop whether this back stack change is a pop
         */
        @MainThread
        default void onBackStackChangeCommitted(@NonNull Fragment fragment, boolean pop) { }
    }

    /**
     * A {@link FragmentResultListener} that is lifecycle aware so that
     * the listener can be fired when the lifecycle is {@link Lifecycle.State#STARTED}.
     */
    private static class LifecycleAwareResultListener implements FragmentResultListener {
        private final Lifecycle mLifecycle;
        private final FragmentResultListener mListener;
        private final LifecycleEventObserver mObserver;

        LifecycleAwareResultListener(@NonNull Lifecycle lifecycle,
                @NonNull FragmentResultListener listener,
                @NonNull LifecycleEventObserver observer) {
            mLifecycle = lifecycle;
            mListener = listener;
            mObserver = observer;
        }

        public boolean isAtLeast(Lifecycle.State state) {
            return mLifecycle.getCurrentState().isAtLeast(state);
        }

        @Override
        public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
            mListener.onFragmentResult(requestKey, result);
        }

        public void removeObserver() {
            mLifecycle.removeObserver(mObserver);
        }
    }

    /**
     * Callback interface for listening to fragment state changes that happen
     * within a given FragmentManager.
     */
    @SuppressWarnings("unused")
    public abstract static class FragmentLifecycleCallbacks {
        /**
         * Called right before the fragment's {@link Fragment#onAttach(Context)} method is called.
         * This is a good time to inject any required dependencies or perform other configuration
         * for the fragment before any of the fragment's lifecycle methods are invoked.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param context Context that the Fragment is being attached to
         */
        public void onFragmentPreAttached(@NonNull FragmentManager fm, @NonNull Fragment f,
                @NonNull Context context) {}

        /**
         * Called after the fragment has been attached to its host. Its host will have had
         * <code>onAttachFragment</code> called before this call happens.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param context Context that the Fragment was attached to
         */
        public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f,
                @NonNull Context context) {}

        /**
         * Called right before the fragment's {@link Fragment#onCreate(Bundle)} method is called.
         * This is a good time to inject any required dependencies or perform other configuration
         * for the fragment.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentPreCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                @Nullable Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onCreate(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                @Nullable Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onActivityCreated(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         *
         * @deprecated To get a callback specifically when a Fragment activity's
         * {@link android.app.Activity#onCreate(Bundle)} is called, register a
         * {@link androidx.lifecycle.LifecycleObserver} on the Activity's {@link Lifecycle} in
         * {@link #onFragmentAttached(FragmentManager, Fragment, Context)}, removing it when it
         * receives the {@link Lifecycle.State#CREATED} callback.
         */
        @Deprecated
        public void onFragmentActivityCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                @Nullable Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned a non-null view from the FragmentManager's
         * request to {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment that created and owns the view
         * @param v View returned by the fragment
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                @NonNull View v, @Nullable Bundle savedInstanceState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onStart()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onResume()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onPause()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onStop()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onSaveInstanceState(Bundle)}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         * @param outState Saved state bundle for the fragment
         */
        public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f,
                @NonNull Bundle outState) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDestroyView()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDestroy()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {}

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link Fragment#onDetach()}.
         *
         * @param fm Host FragmentManager
         * @param f Fragment changing state
         */
        public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {}
    }

    private final ArrayList<OpGenerator> mPendingActions = new ArrayList<>();
    private boolean mExecutingActions;

    private final FragmentStore mFragmentStore = new FragmentStore();
    ArrayList<BackStackRecord> mBackStack;
    private ArrayList<Fragment> mCreatedMenus;
    private final FragmentLayoutInflaterFactory mLayoutInflaterFactory =
            new FragmentLayoutInflaterFactory(this);
    private OnBackPressedDispatcher mOnBackPressedDispatcher;
    private final OnBackPressedCallback mOnBackPressedCallback =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    FragmentManager.this.handleOnBackPressed();
                }
            };

    private final AtomicInteger mBackStackIndex = new AtomicInteger();

    private final Map<String, BackStackState> mBackStackStates =
            Collections.synchronizedMap(new HashMap<String, BackStackState>());

    private final Map<String, Bundle> mResults =
            Collections.synchronizedMap(new HashMap<String, Bundle>());
    private final Map<String, LifecycleAwareResultListener> mResultListeners =
            Collections.synchronizedMap(new HashMap<String, LifecycleAwareResultListener>());

    private ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private final FragmentLifecycleCallbacksDispatcher mLifecycleCallbacksDispatcher =
            new FragmentLifecycleCallbacksDispatcher(this);
    private final CopyOnWriteArrayList<FragmentOnAttachListener> mOnAttachListeners =
            new CopyOnWriteArrayList<>();

    private final Consumer<Configuration> mOnConfigurationChangedListener = newConfig -> {
        if (isParentAdded()) {
            dispatchConfigurationChanged(newConfig, false);
        }
    };
    private final Consumer<Integer> mOnTrimMemoryListener = level -> {
        if (isParentAdded() && level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            dispatchLowMemory(false);
        }
    };
    private final Consumer<MultiWindowModeChangedInfo> mOnMultiWindowModeChangedListener =
            info -> {
                if (isParentAdded()) {
                    dispatchMultiWindowModeChanged(info.isInMultiWindowMode(), false);
                }
            };
    private final Consumer<PictureInPictureModeChangedInfo>
            mOnPictureInPictureModeChangedListener = info -> {
                if (isParentAdded()) {
                    dispatchPictureInPictureModeChanged(info.isInPictureInPictureMode(), false);
                }
            };

    private final MenuProvider mMenuProvider = new MenuProvider() {
        @Override
        public void onPrepareMenu(@NonNull Menu menu) {
            dispatchPrepareOptionsMenu(menu);
        }

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            dispatchCreateOptionsMenu(menu, menuInflater);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            return dispatchOptionsItemSelected(menuItem);
        }

        @Override
        public void onMenuClosed(@NonNull Menu menu) {
            dispatchOptionsMenuClosed(menu);
        }
    };

    int mCurState = Fragment.INITIALIZING;
    private FragmentHostCallback<?> mHost;
    private FragmentContainer mContainer;
    private Fragment mParent;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    Fragment mPrimaryNav;
    private FragmentFactory mFragmentFactory = null;
    private FragmentFactory mHostFragmentFactory = new FragmentFactory() {
        @SuppressWarnings("deprecation")
        @NonNull
        @Override
        public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
            return getHost().instantiate(getHost().getContext(), className, null);
        }
    };
    private SpecialEffectsControllerFactory mSpecialEffectsControllerFactory = null;
    private SpecialEffectsControllerFactory mDefaultSpecialEffectsControllerFactory =
            new SpecialEffectsControllerFactory() {
                @NonNull
                @Override
                public SpecialEffectsController createController(@NonNull ViewGroup container) {
                    return new DefaultSpecialEffectsController(container);
                }
            };

    private ActivityResultLauncher<Intent> mStartActivityForResult;
    private ActivityResultLauncher<IntentSenderRequest> mStartIntentSenderForResult;
    private ActivityResultLauncher<String[]> mRequestPermissions;

    ArrayDeque<LaunchedFragmentInfo> mLaunchedFragments = new ArrayDeque<>();

    private static final String EXTRA_CREATED_FILLIN_INTENT = "androidx.fragment"
            + ".extra.ACTIVITY_OPTIONS_BUNDLE";

    private boolean mNeedMenuInvalidate;
    private boolean mStateSaved;
    private boolean mStopped;
    private boolean mDestroyed;
    private boolean mHavePendingDeferredStart;

    // Temporary vars for removing redundant operations in BackStackRecords:
    private ArrayList<BackStackRecord> mTmpRecords;
    private ArrayList<Boolean> mTmpIsPop;
    private ArrayList<Fragment> mTmpAddedFragments;

    private FragmentManagerViewModel mNonConfig;

    private FragmentStrictMode.Policy mStrictModePolicy;

    private Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions(true);
        }
    };

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        LogWriter logw = new LogWriter(TAG);
        PrintWriter pw = new PrintWriter(logw);
        if (mHost != null) {
            try {
                mHost.onDump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        }
        throw ex;
    }

    /**
     * @deprecated Use {@link #beginTransaction()}.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    @NonNull
    public FragmentTransaction openTransaction() {
        return beginTransaction();
    }

    /**
     * Start a series of edit operations on the Fragments associated with
     * this FragmentManager.
     *
     * <p>Note: A fragment transaction can only be created/committed prior
     * to an activity saving its state.  If you try to commit a transaction
     * after {@link FragmentActivity#onSaveInstanceState FragmentActivity.onSaveInstanceState()}
     * (and prior to a following {@link FragmentActivity#onStart FragmentActivity.onStart}
     * or {@link FragmentActivity#onResume FragmentActivity.onResume()}, you will get an error.
     * This is because the framework takes care of saving your current fragments
     * in the state, and if changes are made after the state is saved then they
     * will be lost.</p>
     */
    @NonNull
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    /**
     * After a {@link FragmentTransaction} is committed with
     * {@link FragmentTransaction#commit FragmentTransaction.commit()}, it
     * is scheduled to be executed asynchronously on the process's main thread.
     * If you want to immediately executing any such pending operations, you
     * can call this function (only from the main thread) to do so.  Note that
     * all callbacks and other related behavior will be done from within this
     * call, so be careful about where this is called from.
     *
     * <p>If you are committing a single transaction that does not modify the
     * fragment back stack, strongly consider using
     * {@link FragmentTransaction#commitNow()} instead. This can help avoid
     * unwanted side effects when other code in your app has pending committed
     * transactions that expect different timing.</p>
     * <p>
     * This also forces the start of any postponed Transactions where
     * {@link Fragment#postponeEnterTransition()} has been called.
     *
     * @return Returns true if there were any pending transactions to be
     * executed.
     */
    @MainThread
    public boolean executePendingTransactions() {
        boolean updates = execPendingActions(true);
        forcePostponedTransactions();
        return updates;
    }

    private void updateOnBackPressedCallbackEnabled() {
        // Always enable the callback if we have pending actions
        // as we don't know if they'll change the back stack entry count.
        // See handleOnBackPressed() for more explanation
        synchronized (mPendingActions) {
            if (!mPendingActions.isEmpty()) {
                mOnBackPressedCallback.setEnabled(true);
                return;
            }
        }
        // This FragmentManager needs to have a back stack for this to be enabled
        // And the parent fragment, if it exists, needs to be the primary navigation
        // fragment.
        mOnBackPressedCallback.setEnabled(getBackStackEntryCount() > 0
                && isPrimaryNavigation(mParent));
    }

    /**
     * Recursively check up the FragmentManager hierarchy of primary
     * navigation Fragments to ensure that all of the parent Fragments are the
     * primary navigation Fragment for their associated FragmentManager
     */
    boolean isPrimaryNavigation(@Nullable Fragment parent) {
        // If the parent is null, then we're at the root host
        // and we're always the primary navigation
        if (parent == null) {
            return true;
        }
        FragmentManager parentFragmentManager = parent.mFragmentManager;
        Fragment primaryNavigationFragment = parentFragmentManager
                .getPrimaryNavigationFragment();
        // The parent Fragment needs to be the primary navigation Fragment
        // and, if it has a parent itself, that parent also needs to be
        // the primary navigation fragment, recursively up the stack
        return parent.equals(primaryNavigationFragment)
                && isPrimaryNavigation(parentFragmentManager.mParent);
    }

    /**
     * Recursively check up the FragmentManager hierarchy of Fragments to see
     * if the menus are all visible.
     */
    boolean isParentMenuVisible(@Nullable Fragment parent) {
        if (parent == null) {
            return true;
        }

        return parent.isMenuVisible();
    }

    /**
     * Recursively check up the FragmentManager hierarchy of Fragments to see
     * if the fragment is hidden.
     */
    boolean isParentHidden(@Nullable Fragment parent) {
        if (parent == null) {
            return false;
        }

        return parent.isHidden();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleOnBackPressed() {
        // First, execute any pending actions to make sure we're in an
        // up to date view of the world just in case anyone is queuing
        // up transactions that change the back stack then immediately
        // calling onBackPressed()
        execPendingActions(true);
        if (mOnBackPressedCallback.isEnabled()) {
            // We still have a back stack, so we can pop
            popBackStackImmediate();
        } else {
            // Sigh. Due to FragmentManager's asynchronicity, we can
            // get into cases where we *think* we can handle the back
            // button but because of frame perfect dispatch, we fell
            // on our face. Since our callback is disabled, we can
            // re-trigger the onBackPressed() to dispatch to the next
            // enabled callback
            mOnBackPressedDispatcher.onBackPressed();
        }
    }

    /**
     * Restores the back stack previously saved via {@link #saveBackStack(String)}. This
     * will result in all of the transactions that made up that back stack to be re-executed,
     * thus re-adding any fragments that were added through those transactions. All state of
     * those fragments will be restored as part of this process. If no state was previously
     * saved with the given name, this operation does nothing.
     * <p>
     * This function is asynchronous -- it enqueues the
     * request to restore, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name The name of the back stack previously saved by {@link #saveBackStack(String)}.
     */
    public void restoreBackStack(@NonNull String name) {
        enqueueAction(new RestoreBackStackState(name), false);
    }

    /**
     * Save the back stack. While this functions similarly to
     * {@link #popBackStack(String, int)}, it <strong>does not</strong> throw away the
     * state of any fragments that were added through those transactions. Instead, the
     * back stack that is saved by this method can later be restored with its state
     * in tact.
     * <p>
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name The name set by {@link FragmentTransaction#addToBackStack(String)}.
     */
    public void saveBackStack(@NonNull String name) {
        enqueueAction(new SaveBackStackState(name), false);
    }

    /**
     * Clears the back stack previously saved via {@link #saveBackStack(String)}. This
     * will result in all of the transactions that made up that back stack to be thrown away,
     * thus destroying any fragments that were added through those transactions. All state of
     * those fragments will be cleared as part of this process. If no state was previously
     * saved with the given name, this operation does nothing.
     * <p>
     * This function is asynchronous -- it enqueues the
     * request to clear, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name The name of the back stack previously saved by {@link #saveBackStack(String)}.
     */
    public void clearBackStack(@NonNull String name) {
        enqueueAction(new ClearBackStackState(name), false);
    }

    /**
     * Pop the top state off the back stack. This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     */
    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    /**
     * Like {@link #popBackStack()}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    @MainThread
    public boolean popBackStackImmediate() {
        return popBackStackImmediate(null, -1, 0);
    }

    /**
     * Pop the last fragment transition from the manager's fragment
     * back stack.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name If non-null, this is the name of a previous back state
     * to look for; if found, all states up to that state will be popped.  The
     * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     * the named state itself is popped. If null, only the top state is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public void popBackStack(@Nullable final String name, final int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    /**
     * Like {@link #popBackStack(String, int)}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    @MainThread
    public boolean popBackStackImmediate(@Nullable String name, int flags) {
        return popBackStackImmediate(name, -1, flags);
    }

    /**
     * Pop all back stack states up to the one with the given identifier.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param id Identifier of the stated to be popped. If no identifier exists,
     * false is returned.
     * The identifier is the number returned by
     * {@link FragmentTransaction#commit() FragmentTransaction.commit()}.  The
     * {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     * the named state itself is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public void popBackStack(final int id, final int flags) {
        popBackStack(id, flags, false);
    }

    void popBackStack(final int id, final int flags, boolean allowStateLoss) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopBackStackState(null, id, flags), allowStateLoss);
    }

    /**
     * Like {@link #popBackStack(int, int)}, but performs the operation immediately
     * inside of the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     * @return Returns true if there was something popped, else false.
     */
    public boolean popBackStackImmediate(int id, int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackImmediate(null, id, flags);
    }

    /**
     * Used by all public popBackStackImmediate methods, this executes pending transactions and
     * returns true if the pop action did anything, regardless of what other pending
     * transactions did.
     *
     * @return true if the pop operation did anything or false otherwise.
     */
    private boolean popBackStackImmediate(@Nullable String name, int id, int flags) {
        execPendingActions(false);
        ensureExecReady(true);

        if (mPrimaryNav != null // We have a primary nav fragment
                && id < 0 // No valid id (since they're local)
                && name == null) { // no name to pop to (since they're local)
            final FragmentManager childManager = mPrimaryNav.getChildFragmentManager();
            if (childManager.popBackStackImmediate()) {
                // We did something, just not to this specific FragmentManager. Return true.
                return true;
            }
        }

        boolean executePop = popBackStackState(mTmpRecords, mTmpIsPop, name, id, flags);
        if (executePop) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();
        return executePop;
    }

    /**
     * Return the number of entries currently in the back stack.
     */
    public int getBackStackEntryCount() {
        return mBackStack != null ? mBackStack.size() : 0;
    }

    /**
     * Return the BackStackEntry at index <var>index</var> in the back stack;
     * entries start index 0 being the bottom of the stack.
     */
    @NonNull
    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    /**
     * Add a new listener for changes to the fragment back stack.
     */
    public void addOnBackStackChangedListener(@NonNull OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<>();
        }
        mBackStackChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added with
     * {@link #addOnBackStackChangedListener(OnBackStackChangedListener)}.
     */
    public void removeOnBackStackChangedListener(@NonNull OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    @Override
    public final void setFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        // Check if there is a listener waiting for a result with this key
        LifecycleAwareResultListener resultListener = mResultListeners.get(requestKey);
        // if there is and it is started, fire the callback
        if (resultListener != null && resultListener.isAtLeast(Lifecycle.State.STARTED)) {
            resultListener.onFragmentResult(requestKey, result);
        } else {
            // else, save the result for later
            mResults.put(requestKey, result);
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Setting fragment result with key " + requestKey + " and "
                    + "result " + result);
        }
    }

    @Override
    public final void clearFragmentResult(@NonNull String requestKey) {
        mResults.remove(requestKey);
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Clearing fragment result with key " + requestKey);
        }
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public final void setFragmentResultListener(@NonNull final String requestKey,
            @NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final FragmentResultListener listener) {
        final Lifecycle lifecycle = lifecycleOwner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        LifecycleEventObserver observer = new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    // once we are started, check for any stored results
                    Bundle storedResult = mResults.get(requestKey);
                    if (storedResult != null) {
                        // if there is a result, fire the callback
                        listener.onFragmentResult(requestKey, storedResult);
                        // and clear the result
                        clearFragmentResult(requestKey);
                    }
                }

                if (event == Lifecycle.Event.ON_DESTROY) {
                    lifecycle.removeObserver(this);
                    mResultListeners.remove(requestKey);
                }
            }
        };
        LifecycleAwareResultListener storedListener = mResultListeners.put(requestKey,
                new LifecycleAwareResultListener(lifecycle, listener, observer));
        if (storedListener != null) {
            storedListener.removeObserver();
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Setting FragmentResultListener with key " + requestKey
                    + " lifecycleOwner " + lifecycle + " and listener " + listener);
        }
        // Only add the observer after we've added the listener to the map
        // to ensure that re-entrant removals actually have a registered listener to remove
        lifecycle.addObserver(observer);
    }

    @Override
    public final void clearFragmentResultListener(@NonNull String requestKey) {
        LifecycleAwareResultListener listener = mResultListeners.remove(requestKey);
        if (listener != null) {
            listener.removeObserver();
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Clearing FragmentResultListener for key " + requestKey);
        }
    }

    /**
     * Put a reference to a fragment in a Bundle.  This Bundle can be
     * persisted as saved state, and when later restoring
     * {@link #getFragment(Bundle, String)} will return the current
     * instance of the same fragment.
     *
     * @param bundle The bundle in which to put the fragment reference.
     * @param key The name of the entry in the bundle.
     * @param fragment The Fragment whose reference is to be stored.
     */
    public void putFragment(@NonNull Bundle bundle, @NonNull String key,
            @NonNull Fragment fragment) {
        if (fragment.mFragmentManager != this) {
            throwException(new IllegalStateException("Fragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        bundle.putString(key, fragment.mWho);
    }

    /**
     * Retrieve the current Fragment instance for a reference previously
     * placed with {@link #putFragment(Bundle, String, Fragment)}.
     *
     * @param bundle The bundle from which to retrieve the fragment reference.
     * @param key The name of the entry in the bundle.
     * @return Returns the current Fragment instance that is associated with
     * the given reference.
     */
    @Nullable
    public Fragment getFragment(@NonNull Bundle bundle, @NonNull String key) {
        String who = bundle.getString(key);
        if (who == null) {
            return null;
        }
        Fragment f = findActiveFragment(who);
        if (f == null) {
            throwException(new IllegalStateException("Fragment no longer exists for key "
                    + key + ": unique id " + who));
        }
        return f;
    }

    /**
     * Find a {@link Fragment} associated with the given {@link View}.
     *
     * This method will locate the {@link Fragment} associated with this view. This is automatically
     * populated for the View returned by {@link Fragment#onCreateView} and its children.
     *
     * @param view the view to search from
     * @return the locally scoped {@link Fragment} to the given view
     * @throws IllegalStateException if the given view does not correspond with a
     * {@link Fragment}.
     */
    @NonNull
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"}) // We should throw a ClassCast
    // exception if the type is wrong
    public static <F extends Fragment> F findFragment(@NonNull View view) {
        Fragment fragment = findViewFragment(view);
        if (fragment == null) {
            throw new IllegalStateException("View " + view + " does not have a Fragment set");
        }
        return (F) fragment;
    }

    /**
     * Recurse up the view hierarchy, looking for the Fragment
     * @param view the view to search from
     * @return the locally scoped {@link Fragment} to the given view, if found
     */
    @Nullable
    static Fragment findViewFragment(@NonNull View view) {
        while (view != null) {
            Fragment fragment = getViewFragment(view);
            if (fragment != null) {
                return fragment;
            }
            ViewParent parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    /**
     * Check if this view has an associated Fragment
     * @param view the view to search from
     * @return the locally scoped {@link Fragment} to the given view, if found
     */
    @Nullable
    static Fragment getViewFragment(@NonNull View view) {
        Object tag = view.getTag(R.id.fragment_container_view_tag);
        if (tag instanceof Fragment) {
            return (Fragment) tag;
        }
        return null;
    }

    void onContainerAvailable(@NonNull FragmentContainerView container) {
        for (FragmentStateManager fragmentStateManager:
                mFragmentStore.getActiveFragmentStateManagers()) {
            Fragment fragment = fragmentStateManager.getFragment();
            if (fragment.mContainerId == container.getId() && fragment.mView != null
                    && fragment.mView.getParent() == null
            ) {
                fragment.mContainer = container;
                fragmentStateManager.addViewToContainer();
            }
        }
    }

    /**
     * Recurse up the view hierarchy, looking for a FragmentManager
     *
     * @param view the view to search from
     * @return The containing {@link FragmentManager} of the given view.
     * @throws IllegalStateException if there no Fragment associated with the view and the
     * view's context is not a {@link FragmentActivity}.
     */
    @NonNull
    static FragmentManager findFragmentManager(@NonNull View view) {
        // Search the view ancestors for a Fragment
        Fragment fragment = findViewFragment(view);
        FragmentManager fm;
        // If there is a Fragment in the hierarchy, get its childFragmentManager, otherwise
        // use the fragmentManager of the Activity.
        if (fragment != null) {
            if (!fragment.isAdded()) {
                throw new IllegalStateException("The Fragment " + fragment + " that owns View "
                        + view + " has already been destroyed. Nested fragments should always "
                        + "use the child FragmentManager.");
            }
            fm = fragment.getChildFragmentManager();
        } else {
            Context context = view.getContext();
            FragmentActivity fragmentActivity = null;
            while (context instanceof ContextWrapper) {
                if (context instanceof FragmentActivity) {
                    fragmentActivity = (FragmentActivity) context;
                    break;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
            if (fragmentActivity != null) {
                fm = fragmentActivity.getSupportFragmentManager();
            } else {
                throw new IllegalStateException("View " + view + " is not within a subclass of "
                        + "FragmentActivity.");
            }

        }
        return fm;
    }

    /**
     * Get a list of all fragments that are currently added to the FragmentManager.
     * This may include those that are hidden as well as those that are shown.
     * This will not include any fragments only in the back stack, or fragments that
     * are detached or removed.
     * <p>
     * The order of the fragments in the list is the order in which they were
     * added or attached.
     *
     * @return A list of all fragments that are added to the FragmentManager.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public List<Fragment> getFragments() {
        return mFragmentStore.getFragments();
    }

    @NonNull
    ViewModelStore getViewModelStore(@NonNull Fragment f) {
        return mNonConfig.getViewModelStore(f);
    }

    @NonNull
    private FragmentManagerViewModel getChildNonConfig(@NonNull Fragment f) {
        return mNonConfig.getChildNonConfig(f);
    }

    void addRetainedFragment(@NonNull Fragment f) {
        mNonConfig.addRetainedFragment(f);
    }

    void removeRetainedFragment(@NonNull Fragment f) {
        mNonConfig.removeRetainedFragment(f);
    }

    /**
     * This is used by FragmentController to get the Active fragments.
     *
     * @return A list of active fragments in the fragment manager, including those that are in the
     * back stack.
     */
    @NonNull
    List<Fragment> getActiveFragments() {
        return mFragmentStore.getActiveFragments();
    }

    /**
     * Used by FragmentController to get the number of Active Fragments.
     *
     * @return The number of active fragments.
     */
    int getActiveFragmentCount() {
        return mFragmentStore.getActiveFragmentCount();
    }

    /**
     * Save the current instance state of the given Fragment.  This can be
     * used later when creating a new instance of the Fragment and adding
     * it to the fragment manager, to have it create itself to match the
     * current state returned here.  Note that there are limits on how
     * this can be used:
     *
     * <ul>
     * <li>The Fragment must currently be attached to the FragmentManager.
     * <li>A new Fragment created using this saved state must be the same class
     * type as the Fragment it was created from.
     * <li>The saved state can not contain dependencies on other fragments --
     * that is it can't use {@link #putFragment(Bundle, String, Fragment)} to
     * store a fragment reference because that reference may not be valid when
     * this saved state is later used.  Likewise the Fragment's target and
     * result code are not included in this state.
     * </ul>
     *
     * @param fragment The Fragment whose state is to be saved.
     * @return The generated state.  This will be null if there was no
     * interesting state created by the fragment.
     */
    @Nullable
    public Fragment.SavedState saveFragmentInstanceState(@NonNull Fragment fragment) {
        FragmentStateManager fragmentStateManager = mFragmentStore.getFragmentStateManager(
                fragment.mWho);
        if (fragmentStateManager == null || !fragmentStateManager.getFragment().equals(fragment)) {
            throwException(new IllegalStateException("Fragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        return fragmentStateManager.saveInstanceState();
    }

    private void clearBackStackStateViewModels() {
        boolean shouldClear;
        if (mHost instanceof ViewModelStoreOwner) {
            shouldClear = mFragmentStore.getNonConfig().isCleared();
        } else if (mHost.getContext() instanceof Activity) {
            Activity activity = (Activity) mHost.getContext();
            shouldClear = !activity.isChangingConfigurations();
        } else {
            shouldClear = true;
        }
        if (shouldClear) {
            for (BackStackState backStackState : mBackStackStates.values()) {
                for (String who : backStackState.mFragments) {
                    mFragmentStore.getNonConfig().clearNonConfigState(who);
                }
            }
        }
    }

    /**
     * Returns true if the final {@link android.app.Activity#onDestroy() Activity.onDestroy()}
     * call has been made on the FragmentManager's Activity, so this instance is now dead.
     */
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (mParent != null) {
            Class<?> cls = mParent.getClass();
            sb.append(cls.getSimpleName());
            sb.append("{");
            sb.append(Integer.toHexString(System.identityHashCode(mParent)));
            sb.append("}");
        } else if (mHost != null) {
            Class<?> cls = mHost.getClass();
            sb.append(cls.getSimpleName());
            sb.append("{");
            sb.append(Integer.toHexString(System.identityHashCode(mHost)));
            sb.append("}");
        } else {
            sb.append("null");
        }
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Print the FragmentManager's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer A PrintWriter to which the dump is to be set.
     * @param args Additional arguments to the dump request.
     */
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, @Nullable String[] args) {
        String innerPrefix = prefix + "    ";

        mFragmentStore.dump(prefix, fd, writer, args);

        int count;
        if (mCreatedMenus != null) {
            count = mCreatedMenus.size();
            if (count > 0) {
                writer.print(prefix); writer.println("Fragments Created Menus:");
                for (int i = 0; i < count; i++) {
                    Fragment f = mCreatedMenus.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }

        if (mBackStack != null) {
            count = mBackStack.size();
            if (count > 0) {
                writer.print(prefix); writer.println("Back Stack:");
                for (int i = 0; i < count; i++) {
                    BackStackRecord bs = mBackStack.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, writer);
                }
            }
        }

        writer.print(prefix);
        writer.println("Back Stack Index: " + mBackStackIndex.get());

        synchronized (mPendingActions) {
            count = mPendingActions.size();
            if (count > 0) {
                writer.print(prefix); writer.println("Pending Actions:");
                for (int i = 0; i < count; i++) {
                    OpGenerator r = mPendingActions.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(r);
                }
            }
        }

        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(mContainer);
        if (mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(mCurState);
        writer.print(" mStateSaved=");
        writer.print(mStateSaved);
        writer.print(" mStopped=");
        writer.print(mStopped);
        writer.print(" mDestroyed=");
        writer.println(mDestroyed);
        if (mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(mNeedMenuInvalidate);
        }
    }

    void performPendingDeferredStart(@NonNull FragmentStateManager fragmentStateManager) {
        Fragment f = fragmentStateManager.getFragment();
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            fragmentStateManager.moveToExpectedState();
        }
    }

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    /**
     * Allows for changing the draw order on a container, if the container is a
     * FragmentContainerView.
     */
    void setExitAnimationOrder(@NonNull Fragment f, boolean isPop) {
        ViewGroup container = getFragmentContainer(f);
        if (container != null) {
            if (container instanceof FragmentContainerView) {
                ((FragmentContainerView) container).setDrawDisappearingViewsLast(!isPop);
            }
        }
    }

    /**
     * Changes the state of the fragment manager to {@code newState}. If the fragment manager
     * changes state or {@code always} is {@code true}, any fragments within it have their
     * states updated as well.
     *
     * @param newState The new state for the fragment manager
     * @param always If {@code true}, all fragments update their state, even
     *               if {@code newState} matches the current fragment manager's state.
     */
    void moveToState(int newState, boolean always) {
        if (mHost == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }

        if (!always && newState == mCurState) {
            return;
        }

        mCurState = newState;
        mFragmentStore.moveToExpectedState();
        startPendingDeferredFragments();

        if (mNeedMenuInvalidate && mHost != null && mCurState == Fragment.RESUMED) {
            mHost.onSupportInvalidateOptionsMenu();
            mNeedMenuInvalidate = false;
        }
    }

    private void startPendingDeferredFragments() {
        for (FragmentStateManager fragmentStateManager :
                mFragmentStore.getActiveFragmentStateManagers()) {
            performPendingDeferredStart(fragmentStateManager);
        }
    }

    /**
     * For a given Fragment, get any existing FragmentStateManager found in the
     * {@link FragmentStore} or create a brand new FragmentStateManager if one does
     * not exist.
     *
     * @param f The Fragment to create a FragmentStateManager for
     * @return A valid FragmentStateManager
     */
    @NonNull
    FragmentStateManager createOrGetFragmentStateManager(@NonNull Fragment f) {
        FragmentStateManager existing = mFragmentStore.getFragmentStateManager(f.mWho);
        if (existing != null) {
            return existing;
        }
        FragmentStateManager fragmentStateManager = new FragmentStateManager(
                mLifecycleCallbacksDispatcher, mFragmentStore, f);
        // Restore state any state set via setInitialSavedState()
        fragmentStateManager.restoreState(mHost.getContext().getClassLoader());
        // Catch the FragmentStateManager up to our current state
        fragmentStateManager.setFragmentManagerState(mCurState);
        return fragmentStateManager;
    }

    FragmentStateManager addFragment(@NonNull Fragment fragment) {
        if (fragment.mPreviousWho != null) {
            FragmentStrictMode.onFragmentReuse(fragment, fragment.mPreviousWho);
        }
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "add: " + fragment);
        FragmentStateManager fragmentStateManager = createOrGetFragmentStateManager(fragment);
        fragment.mFragmentManager = this;
        mFragmentStore.makeActive(fragmentStateManager);
        if (!fragment.mDetached) {
            mFragmentStore.addFragment(fragment);
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (isMenuAvailable(fragment)) {
                mNeedMenuInvalidate = true;
            }
        }
        return fragmentStateManager;
    }

    void removeFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        }
        final boolean inactive = !fragment.isInBackStack();
        if (!fragment.mDetached || inactive) {
            mFragmentStore.removeFragment(fragment);
            if (isMenuAvailable(fragment)) {
                mNeedMenuInvalidate = true;
            }
            fragment.mRemoving = true;
            setVisibleRemovingFragment(fragment);
        }
    }

    /**
     * Marks a fragment as hidden to be later animated.
     *
     * @param fragment The fragment to be shown.
     */
    void hideFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "hide: " + fragment);
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
            setVisibleRemovingFragment(fragment);
        }
    }

    /**
     * Marks a fragment as shown to be later animated.
     *
     * @param fragment The fragment to be shown.
     */
    void showFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "show: " + fragment);
        if (fragment.mHidden) {
            fragment.mHidden = false;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    void detachFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "detach: " + fragment);
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                // We are not already in back stack, so need to remove the fragment.
                if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "remove from detach: " + fragment);
                mFragmentStore.removeFragment(fragment);
                if (isMenuAvailable(fragment)) {
                    mNeedMenuInvalidate = true;
                }
                setVisibleRemovingFragment(fragment);
            }
        }
    }

    void attachFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "attach: " + fragment);
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                mFragmentStore.addFragment(fragment);
                if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "add from attach: " + fragment);
                if (isMenuAvailable(fragment)) {
                    mNeedMenuInvalidate = true;
                }
            }
        }
    }

    /**
     * Finds a fragment that was identified by the given id either when inflated
     * from XML or as the container ID when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack associated with this ID are searched.
     * @return The fragment if found or null otherwise.
     */
    @Nullable
    public Fragment findFragmentById(@IdRes int id) {
        return mFragmentStore.findFragmentById(id);
    }

    /**
     * Finds a fragment that was identified by the given tag either when inflated
     * from XML or as supplied when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack are searched.
     * <p>
     * If provided a {@code null} tag, this method returns null.
     *
     * @param tag the tag used to search for the fragment
     * @return The fragment if found or null otherwise.
     */
    @Nullable
    public Fragment findFragmentByTag(@Nullable String tag) {
        return mFragmentStore.findFragmentByTag(tag);
    }

    Fragment findFragmentByWho(@NonNull String who) {
        return mFragmentStore.findFragmentByWho(who);
    }

    @Nullable
    Fragment findActiveFragment(@NonNull String who) {
        return mFragmentStore.findActiveFragment(who);
    }

    private void checkStateLoss() {
        if (isStateSaved()) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
    }

    /**
     * Returns {@code true} if the FragmentManager's state has already been saved
     * by its host. Any operations that would change saved state should not be performed
     * if this method returns true. For example, any popBackStack() method, such as
     * {@link #popBackStackImmediate()} or any FragmentTransaction using
     * {@link FragmentTransaction#commit()} instead of
     * {@link FragmentTransaction#commitAllowingStateLoss()} will change
     * the state and will result in an error.
     *
     * @return true if this FragmentManager's state has already been saved by its host
     */
    public boolean isStateSaved() {
        // See saveAllState() for the explanation of this.  We do this for
        // all platform versions, to keep our behavior more consistent between
        // them.
        return mStateSaved || mStopped;
    }

    /**
     * Adds an action to the queue of pending actions.
     *
     * @param action the action to add
     * @param allowStateLoss whether to allow loss of state information
     * @throws IllegalStateException if the activity has been destroyed
     */
    void enqueueAction(@NonNull OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            if (mHost == null) {
                if (mDestroyed) {
                    throw new IllegalStateException("FragmentManager has been destroyed");
                } else {
                    throw new IllegalStateException("FragmentManager has not been attached to a "
                            + "host.");
                }
            }
            checkStateLoss();
        }
        synchronized (mPendingActions) {
            if (mHost == null) {
                if (allowStateLoss) {
                    // This FragmentManager isn't attached, so drop the entire transaction.
                    return;
                }
                throw new IllegalStateException("Activity has been destroyed");
            }
            mPendingActions.add(action);
            scheduleCommit();
        }
    }

    /**
     * Schedules the execution when one hasn't been scheduled already. This should happen
     * the first time {@link #enqueueAction(OpGenerator, boolean)} is called or when
     * a postponed transaction has been started with
     * {@link Fragment#startPostponedEnterTransition()}
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void scheduleCommit() {
        synchronized (mPendingActions) {
            boolean pendingReady = mPendingActions.size() == 1;
            if (pendingReady) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
                updateOnBackPressedCallbackEnabled();
            }
        }
    }

    int allocBackStackIndex() {
        return mBackStackIndex.getAndIncrement();
    }

    /**
     * Broken out from exec*, this prepares for gathering and executing operations.
     *
     * @param allowStateLoss true if state loss should be ignored or false if it should be
     *                       checked.
     */
    private void ensureExecReady(boolean allowStateLoss) {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }

        if (mHost == null) {
            if (mDestroyed) {
                throw new IllegalStateException("FragmentManager has been destroyed");
            } else {
                throw new IllegalStateException("FragmentManager has not been attached to a host.");
            }
        }

        if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }

        if (!allowStateLoss) {
            checkStateLoss();
        }

        if (mTmpRecords == null) {
            mTmpRecords = new ArrayList<>();
            mTmpIsPop = new ArrayList<>();
        }
    }

    void execSingleAction(@NonNull OpGenerator action, boolean allowStateLoss) {
        if (allowStateLoss && (mHost == null || mDestroyed)) {
            // This FragmentManager isn't attached, so drop the entire transaction.
            return;
        }
        ensureExecReady(allowStateLoss);
        if (action.generateOps(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();
    }

    /**
     * Broken out of exec*, this cleans up the mExecutingActions and the temporary structures
     * used in executing operations.
     */
    private void cleanupExec() {
        mExecutingActions = false;
        mTmpIsPop.clear();
        mTmpRecords.clear();
    }

    /**
     * Only call from main thread!
     */
    boolean execPendingActions(boolean allowStateLoss) {
        ensureExecReady(allowStateLoss);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();

        return didSomething;
    }

    /**
     * Remove redundant BackStackRecord operations and executes them. This method merges operations
     * of proximate records that allow reordering. See
     * {@link FragmentTransaction#setReorderingAllowed(boolean)}.
     * <p>
     * For example, a transaction that adds to the back stack and then another that pops that
     * back stack record will be optimized to remove the unnecessary operation.
     * <p>
     * Likewise, two transactions committed that are executed at the same time will be optimized
     * to remove the redundant operations as well as two pop operations executed together.
     *
     * @param records The records pending execution
     * @param isRecordPop The direction that these records are being run.
     */
    private void removeRedundantOperationsAndExecute(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop) {
        if (records.isEmpty()) {
            return;
        }

        if (records.size() != isRecordPop.size()) {
            throw new IllegalStateException("Internal error with the back stack records");
        }

        final int numRecords = records.size();
        int startIndex = 0;
        for (int recordNum = 0; recordNum < numRecords; recordNum++) {
            final boolean canReorder = records.get(recordNum).mReorderingAllowed;
            if (!canReorder) {
                // execute all previous transactions
                if (startIndex != recordNum) {
                    executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                }
                // execute all pop operations that don't allow reordering together or
                // one add operation
                int reorderingEnd = recordNum + 1;
                if (isRecordPop.get(recordNum)) {
                    while (reorderingEnd < numRecords
                            && isRecordPop.get(reorderingEnd)
                            && !records.get(reorderingEnd).mReorderingAllowed) {
                        reorderingEnd++;
                    }
                }
                executeOpsTogether(records, isRecordPop, recordNum, reorderingEnd);
                startIndex = reorderingEnd;
                recordNum = reorderingEnd - 1;
            }
        }
        if (startIndex != numRecords) {
            executeOpsTogether(records, isRecordPop, startIndex, numRecords);
        }
    }

    /**
     * Executes a subset of a list of BackStackRecords, all of which either allow reordering or
     * do not allow ordering.
     * @param records A list of BackStackRecords that are to be executed
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first record in <code>records</code> to be executed
     * @param endIndex One more than the final record index in <code>records</code> to executed.
     */
    private void executeOpsTogether(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        final boolean allowReordering = records.get(startIndex).mReorderingAllowed;
        boolean addToBackStack = false;
        if (mTmpAddedFragments == null) {
            mTmpAddedFragments = new ArrayList<>();
        } else {
            mTmpAddedFragments.clear();
        }
        mTmpAddedFragments.addAll(mFragmentStore.getFragments());
        Fragment oldPrimaryNav = getPrimaryNavigationFragment();
        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
            if (!isPop) {
                oldPrimaryNav = record.expandOps(mTmpAddedFragments, oldPrimaryNav);
            } else {
                oldPrimaryNav = record.trackAddedFragmentsInPop(mTmpAddedFragments, oldPrimaryNav);
            }
            addToBackStack = addToBackStack || record.mAddToBackStack;
        }
        mTmpAddedFragments.clear();

        if (!allowReordering && mCurState >= Fragment.CREATED) {
            // When reordering isn't allowed, we may be operating on Fragments that haven't
            // been made active
            for (int index = startIndex; index < endIndex; index++) {
                BackStackRecord record = records.get(index);
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null && fragment.mFragmentManager != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        mFragmentStore.makeActive(fragmentStateManager);
                    }
                }
            }
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        // The last operation determines the overall direction, this ensures that operations
        // such as push, push, pop, push are correctly considered a push
        boolean isPop = isRecordPop.get(endIndex - 1);

        if (addToBackStack && mBackStackChangeListeners != null
                && !mBackStackChangeListeners.isEmpty()) {
            Set<Fragment> fragments = new LinkedHashSet<>();
            // Build a list of fragments based on the records
            for (BackStackRecord record : records) {
                fragments.addAll(fragmentsFromRecord(record));
            }
            // Dispatch to all of the fragments in the list
            for (OnBackStackChangedListener listener : mBackStackChangeListeners) {
                // We give all fragment the back stack changed started signal first
                for (Fragment fragment: fragments) {
                    listener.onBackStackChangeStarted(fragment, isPop);
                }
            }
            for (OnBackStackChangedListener listener : mBackStackChangeListeners) {
                // Then we give them all the committed signal
                for (Fragment fragment: fragments) {
                    listener.onBackStackChangeCommitted(fragment, isPop);
                }
            }
        }
        // Ensure that Fragments directly affected by operations
        // are moved to their expected state in operation order
        for (int index = startIndex; index < endIndex; index++) {
            BackStackRecord record = records.get(index);
            if (isPop) {
                // Pop operations get applied in reverse order
                for (int opIndex = record.mOps.size() - 1; opIndex >= 0; opIndex--) {
                    FragmentTransaction.Op op = record.mOps.get(opIndex);
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            } else {
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            }

        }
        // And only then do we move all other fragments to the current state
        moveToState(mCurState, true);
        Set<SpecialEffectsController> changedControllers = collectChangedControllers(
                records, startIndex, endIndex);
        for (SpecialEffectsController controller : changedControllers) {
            controller.updateOperationDirection(isPop);
            controller.markPostponedState();
            controller.executePendingOperations();
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            isPop = isRecordPop.get(recordNum);
            if (isPop && record.mIndex >= 0) {
                record.mIndex = -1;
            }
            record.runOnCommitRunnables();
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    private Set<SpecialEffectsController> collectChangedControllers(
            @NonNull ArrayList<BackStackRecord> records, int startIndex, int endIndex) {
        Set<SpecialEffectsController> controllers = new HashSet<>();
        for (int index = startIndex; index < endIndex; index++) {
            BackStackRecord record = records.get(index);
            for (FragmentTransaction.Op op : record.mOps) {
                Fragment fragment = op.mFragment;
                if (fragment != null) {
                    ViewGroup container = fragment.mContainer;
                    if (container != null) {
                        controllers.add(SpecialEffectsController.getOrCreateController(
                                container, this));
                    }
                }
            }
        }
        return controllers;
    }

    /**
     * Run the operations in the BackStackRecords, either to push or pop.
     *
     * @param records The list of records whose operations should be run.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first entry in records to run.
     * @param endIndex One past the index of the final entry in records to run.
     */
    private static void executeOps(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            if (isPop) {
                record.bumpBackStackNesting(-1);
                record.executePopOps();
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    /**
     * Set a Fragment that is visibly being removed from the screen to a tag on its container.
     * If a Fragment with the same container is already set, the previously added
     * Fragment has its exit animation updated to the correct exit animation (either exit or
     * pop_exit).
     */
    private void setVisibleRemovingFragment(@NonNull Fragment f) {
        ViewGroup container = getFragmentContainer(f);
        if (container != null
                && f.getEnterAnim() + f.getExitAnim() + f.getPopEnterAnim() + f.getPopExitAnim() > 0
        ) {
            if (container.getTag(R.id.visible_removing_fragment_view_tag) == null) {
                container.setTag(R.id.visible_removing_fragment_view_tag, f);
            }
            ((Fragment) container.getTag(R.id.visible_removing_fragment_view_tag))
                    .setPopDirection(f.getPopDirection());
        }
    }

    private ViewGroup getFragmentContainer(@NonNull Fragment f) {
        // If there's already a container, just return it
        if (f.mContainer != null) {
            return f.mContainer;
        }
        // If the fragment has no containerId we should return null immediately.
        if (f.mContainerId <= 0) {
            return null;
        }
        // This will be false if a child fragment is added to its parent's childFragmentManager
        // before a view is created for Parent. In all other cases (adding a fragment to an
        // FragmentActivity's fragmentManager, adding a child fragment to a parent that has a view),
        // it should be true.
        if (mContainer.onHasView()) {
            View view = mContainer.onFindViewById(f.mContainerId);
            // We should handle the case where the container may not be a ViewGroup
            if (view instanceof ViewGroup) {
                return (ViewGroup) view;
            }
        }
        return null;
    }

    /**
     * Starts all postponed transactions regardless of whether they are ready or not.
     */
    private void forcePostponedTransactions() {
        Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
        for (SpecialEffectsController controller : controllers) {
            controller.forcePostponedExecutePendingOperations();
        }
    }

    /**
     * Ends the animations of fragments so that they immediately reach the end state.
     * This is used prior to saving the state so that the correct state is saved.
     */
    private void endAnimatingAwayFragments() {
        Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
        for (SpecialEffectsController controller : controllers) {
            controller.forceCompleteAllOperations();
        }
    }

    private Set<SpecialEffectsController> collectAllSpecialEffectsController() {
        Set<SpecialEffectsController> controllers = new HashSet<>();
        for (FragmentStateManager fragmentStateManager :
                mFragmentStore.getActiveFragmentStateManagers()) {
            ViewGroup container = fragmentStateManager.getFragment().mContainer;
            if (container != null) {
                controllers.add(SpecialEffectsController.getOrCreateController(container,
                        getSpecialEffectsControllerFactory()));
            }
        }
        return controllers;
    }

    /**
     * Adds all records in the pending actions to records and whether they are add or pop
     * operations to isPop. After executing, the pending actions will be empty.
     *
     * @param records All pending actions will generate BackStackRecords added to this.
     *                This contains the transactions, in order, to execute.
     * @param isPop All pending actions will generate booleans to add to this. This contains
     *              an entry for each entry in records to indicate whether or not it is a
     *              pop action.
     */
    private boolean generateOpsForPendingActions(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isPop) {
        boolean didSomething = false;
        synchronized (mPendingActions) {
            if (mPendingActions.isEmpty()) {
                return false;
            }

            try {
                final int numActions = mPendingActions.size();
                for (int i = 0; i < numActions; i++) {
                    didSomething |= mPendingActions.get(i).generateOps(records, isPop);
                }
            } finally {
                // Whether generateOps succeeds or not, we clear the pending actions
                // to avoid re-processing the same set of actions a second time
                mPendingActions.clear();
                mHost.getHandler().removeCallbacks(mExecCommit);
            }
        }
        return didSomething;
    }

    private void doPendingDeferredStart() {
        if (mHavePendingDeferredStart) {
            mHavePendingDeferredStart = false;
            startPendingDeferredFragments();
        }
    }

    private void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            for (int i = 0; i < mBackStackChangeListeners.size(); i++) {
                mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    private Set<Fragment> fragmentsFromRecord(@NonNull BackStackRecord record) {
        Set<Fragment> fragments = new HashSet<>();
        for (int i = 0; i < record.mOps.size(); i++) {
            Fragment f = record.mOps.get(i).mFragment;
            if (f != null && record.mAddToBackStack) {
                fragments.add(f);
            }
        }
        return fragments;
    }

    void addBackStackState(BackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<>();
        }
        mBackStack.add(state);
    }

    boolean restoreBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @NonNull String name) {
        BackStackState backStackState = mBackStackStates.remove(name);
        if (backStackState == null) {
            return false;
        }

        HashMap<String, Fragment> pendingSavedFragments = new HashMap<>();
        for (BackStackRecord record : records) {
            if (record.mBeingSaved) {
                for (FragmentTransaction.Op op : record.mOps) {
                    if (op.mFragment != null) {
                        pendingSavedFragments.put(op.mFragment.mWho, op.mFragment);
                    }
                }
            }
        }
        List<BackStackRecord> backStackRecords = backStackState.instantiate(this,
                pendingSavedFragments);
        boolean added = false;
        for (BackStackRecord record : backStackRecords) {
            added = record.generateOps(records, isRecordPop) || added;
        }
        return added;
    }

    boolean saveBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @NonNull String name) {
        final int index = findBackStackIndex(name, -1, true);
        if (index < 0) {
            return false;
        }

        // Assert that all of the transactions use setReorderingAllowed(true)
        // to ensure that when they are restored, they are restored as a single
        // atomic operation and intermediate fragments aren't moved all the way
        // up to the RESUMED state
        for (int i = index; i < mBackStack.size(); i++) {
            BackStackRecord record = mBackStack.get(i);
            if (!record.mReorderingAllowed) {
                throwException(new IllegalArgumentException("saveBackStack(\"" + name + "\") "
                        + "included FragmentTransactions must use setReorderingAllowed(true) "
                        + "to ensure that the back stack can be restored as an atomic operation. "
                        + "Found " + record + " that did not use setReorderingAllowed(true)."));
            }
        }

        // Assert that the set of affected fragments are entirely self contained within
        // the set of transactions being saved by ensuring that the first transaction including
        // that fragment includes an OP_ADD
        HashSet<Fragment> allFragments = new HashSet<>();
        for (int i = index; i < mBackStack.size(); i++) {
            BackStackRecord record = mBackStack.get(i);
            HashSet<Fragment> affectedFragments = new HashSet<>();
            HashSet<Fragment> addedFragments = new HashSet<>();
            for (FragmentTransaction.Op op : record.mOps) {
                Fragment f = op.mFragment;
                if (f == null) {
                    continue;
                }
                if (!op.mFromExpandedOp || op.mCmd == FragmentTransaction.OP_ADD
                        || op.mCmd == FragmentTransaction.OP_REPLACE
                        || op.mCmd == FragmentTransaction.OP_SET_PRIMARY_NAV) {
                    allFragments.add(f);
                    affectedFragments.add(f);
                }
                if (op.mCmd == FragmentTransaction.OP_ADD
                        || op.mCmd == FragmentTransaction.OP_REPLACE) {
                    addedFragments.add(f);
                }
            }
            affectedFragments.removeAll(addedFragments);
            if (!affectedFragments.isEmpty()) {
                throwException(new IllegalArgumentException("saveBackStack(\"" + name + "\") "
                        + "must be self contained and not reference fragments from "
                        + "non-saved FragmentTransactions. Found reference to fragment"
                        + (affectedFragments.size() == 1
                        ? " " + affectedFragments.iterator().next()
                        : "s " + affectedFragments)
                        + " in " + record + " that were previously "
                        + "added to the FragmentManager through a separate FragmentTransaction."));
            }
        }

        // Ensure that there are no retained fragments in the affected fragments or
        // their transitive set of child fragments
        ArrayDeque<Fragment> fragmentsToSearch = new ArrayDeque<>(allFragments);
        while (!fragmentsToSearch.isEmpty()) {
            Fragment currentFragment = fragmentsToSearch.removeFirst();
            if (currentFragment.mRetainInstance) {
                throwException(new IllegalArgumentException("saveBackStack(\"" + name + "\") "
                        + "must not contain retained fragments. Found "
                        + (allFragments.contains(currentFragment)
                        ? "direct reference to retained "
                        : "retained child ")
                        + "fragment " + currentFragment));
            }
            // Then recursively check the child fragments for retained fragments
            for (Fragment f : currentFragment.mChildFragmentManager.getActiveFragments()) {
                if (f != null) {
                    fragmentsToSearch.addLast(f);
                }
            }
        }

        // Now actually record each save
        final ArrayList<String> fragments = new ArrayList<>();
        for (Fragment f : allFragments) {
            fragments.add(f.mWho);
        }
        final ArrayList<BackStackRecordState> backStackRecordStates =
                new ArrayList<>(mBackStack.size() - index);
        // Add placeholders for each BackStackRecordState
        for (int i = index; i < mBackStack.size(); i++) {
            backStackRecordStates.add(null);
        }
        final BackStackState backStackState = new BackStackState(
                fragments, backStackRecordStates);
        for (int i = mBackStack.size() - 1; i >= index; i--) {
            BackStackRecord record = mBackStack.remove(i);

            // Create a copy of the record to save
            BackStackRecord copy = new BackStackRecord(record);
            copy.collapseOps();
            BackStackRecordState state = new BackStackRecordState(copy);
            backStackRecordStates.set(i - index, state);

            // And now mark the record as being saved to ensure that each
            // fragment saves its state properly
            record.mBeingSaved = true;
            records.add(record);
            isRecordPop.add(true);
        }
        mBackStackStates.put(name, backStackState);
        return true;
    }

    /**
     * We have to handle a number of cases here:
     * 1. We have no back stack state at all
     * 2. We have previously saved the back stack state and we now only have the state
     * 3. We are in the process of handling a saveBackStack() operation (it is in
     * the set of records to be processed prior to this)
     * 3a. We are in the process of handling a saveBackStack() and there are other
     * FragmentTransactions queued up between that save and this clear (maybe even
     * including a restoreBackStack operation).
     *
     * This comes together to mean that we can't actually 'clear' anything at the time
     * when this particular method is called - instead, we need to enqueue exactly what
     * records, etc. we need to do to get the back stack and state into the right state
     * after they're all executed. This means 'clear' really means 'restore'+'pop' - as
     * we 'pop' instead of 'save', any saved state (and ViewModels, etc.) will be cleared
     * no matter what pending operations are enqueued up before or after this.
     */
    boolean clearBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @NonNull String name) {
        boolean restoredBackStackState = restoreBackStackState(records, isRecordPop, name);
        if (!restoredBackStackState) {
            return false;
        }
        return popBackStackState(records, isRecordPop, name, -1, POP_BACK_STACK_INCLUSIVE);
    }

    @SuppressWarnings({"unused", "WeakerAccess"}) /* synthetic access */
    boolean popBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @Nullable String name, int id, int flags) {
        int index = findBackStackIndex(name, id, (flags & POP_BACK_STACK_INCLUSIVE) != 0);
        if (index < 0) {
            return false;
        }
        for (int i = mBackStack.size() - 1; i >= index; i--) {
            records.add(mBackStack.remove(i));
            isRecordPop.add(true);
        }
        return true;
    }

    /**
     * Find the index in the back stack associated with the given name / id.
     * <p>
     * When <code>inclusive</code> is <code>true</code>, the index of the matching record
     * will be returned. When it is <code>false</code>, the index of the record directly
     * after it will be returned. In cases where you are doing an inclusive search and
     * multiple records have the same name / id, the index returned includes all
     * consecutive matches following the first match.
     *
     * @param name The name set via {@link FragmentTransaction#addToBackStack(String)}. Use
     *             <code>null</code> if you do not want to search by name.
     * @param id The id returned by {@link FragmentTransaction#commit()}. Use
     *           <code>-1</code> if you do not want to search by id.
     * @param inclusive Whether to include the record specified by name or id.
     * @return
     */
    private int findBackStackIndex(@Nullable String name, int id, boolean inclusive) {
        if (mBackStack == null || mBackStack.isEmpty()) {
            return -1;
        }
        if (name == null && id < 0) {
            if (inclusive) {
                return 0;
            } else {
                return mBackStack.size() - 1;
            }
        } else {
            // If a name or ID is specified, look for that place in
            // the stack.
            int index = mBackStack.size() - 1;
            while (index >= 0) {
                BackStackRecord bss = mBackStack.get(index);
                if (name != null && name.equals(bss.getName())) {
                    break;
                }
                if (id >= 0 && id == bss.mIndex) {
                    break;
                }
                index--;
            }
            if (index < 0) {
                return index;
            }
            if (inclusive) {
                // Consume all following entries that match.
                while (index > 0) {
                    BackStackRecord bss = mBackStack.get(index - 1);
                    if ((name != null && name.equals(bss.getName()))
                            || (id >= 0 && id == bss.mIndex)) {
                        index--;
                        continue;
                    }
                    break;
                }
            } else if (index == mBackStack.size() - 1) {
                // For a non-inclusive search, finding the last record
                // is the same as finding nothing at all since the
                // matching record itself is not included
                return -1;
            } else {
                // Non-inclusive, so skip the actual matching record
                index++;
            }
            return index;
        }
    }

    /**
     * @deprecated Ideally, all {@link androidx.fragment.app.FragmentHostCallback} instances
     * implement ViewModelStoreOwner and we can remove this method entirely.
     */
    @Deprecated
    FragmentManagerNonConfig retainNonConfig() {
        if (mHost instanceof ViewModelStoreOwner) {
            throwException(new IllegalStateException("You cannot use retainNonConfig when your "
                    + "FragmentHostCallback implements ViewModelStoreOwner."));
        }
        return mNonConfig.getSnapshot();
    }

    Parcelable saveAllState() {
        if (mHost instanceof SavedStateRegistryOwner) {
            throwException(new IllegalStateException("You cannot use saveAllState when your "
                    + "FragmentHostCallback implements SavedStateRegistryOwner."));
        }
        Bundle savedState = saveAllStateInternal();
        return savedState.isEmpty() ? null : savedState;
    }

    @NonNull
    Bundle saveAllStateInternal() {
        Bundle bundle = new Bundle();
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions(true);

        mStateSaved = true;
        mNonConfig.setIsStateSaved(true);

        // First save all active fragments.
        ArrayList<String> active = mFragmentStore.saveActiveFragments();

        // And grab all fragments' saved state bundles
        HashMap<String, Bundle> savedState = mFragmentStore.getAllSavedState();
        if (savedState.isEmpty()) {
            if (isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "saveAllState: no fragments!");
            }
        } else {
            // Build list of currently added fragments.
            ArrayList<String> added = mFragmentStore.saveAddedFragments();

            // Now save back stack.
            BackStackRecordState[] backStack = null;
            if (mBackStack != null) {
                int size = mBackStack.size();
                if (size > 0) {
                    backStack = new BackStackRecordState[size];
                    for (int i = 0; i < size; i++) {
                        backStack[i] = new BackStackRecordState(mBackStack.get(i));
                        if (isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(TAG, "saveAllState: adding back stack #" + i
                                    + ": " + mBackStack.get(i));
                        }
                    }
                }
            }

            FragmentManagerState fms = new FragmentManagerState();
            fms.mActive = active;
            fms.mAdded = added;
            fms.mBackStack = backStack;
            fms.mBackStackIndex = mBackStackIndex.get();
            if (mPrimaryNav != null) {
                fms.mPrimaryNavActiveWho = mPrimaryNav.mWho;
            }
            fms.mBackStackStateKeys.addAll(mBackStackStates.keySet());
            fms.mBackStackStates.addAll(mBackStackStates.values());
            fms.mLaunchedFragments = new ArrayList<>(mLaunchedFragments);
            bundle.putParcelable(FRAGMENT_MANAGER_STATE_KEY, fms);

            for (String resultName : mResults.keySet()) {
                bundle.putBundle(RESULT_KEY_PREFIX + resultName, mResults.get(resultName));
            }

            for (String fWho : savedState.keySet()) {
                bundle.putBundle(FRAGMENT_KEY_PREFIX + fWho, savedState.get(fWho));
            }
        }

        return bundle;
    }

    @SuppressWarnings("deprecation")
    void restoreAllState(@Nullable Parcelable state, @Nullable FragmentManagerNonConfig nonConfig) {
        if (mHost instanceof ViewModelStoreOwner) {
            throwException(new IllegalStateException("You must use restoreSaveState when your "
                    + "FragmentHostCallback implements ViewModelStoreOwner"));
        }
        mNonConfig.restoreFromSnapshot(nonConfig);
        restoreSaveStateInternal(state);
    }

    void restoreSaveState(@Nullable Parcelable state) {
        if (mHost instanceof SavedStateRegistryOwner) {
            throwException(new IllegalStateException("You cannot use restoreSaveState when your "
                    + "FragmentHostCallback implements SavedStateRegistryOwner."));
        }
        restoreSaveStateInternal(state);
    }

    @SuppressWarnings("deprecation")
    void restoreSaveStateInternal(@Nullable Parcelable state) {
        // If there is no saved state at all, then there's nothing else to do
        if (state == null) return;
        Bundle bundle = (Bundle) state;

        // Restore the fragment results
        for (String bundleKey : bundle.keySet()) {
            if (bundleKey.startsWith(RESULT_KEY_PREFIX)) {
                Bundle savedResult = bundle.getBundle(bundleKey);
                if (savedResult != null) {
                    savedResult.setClassLoader(mHost.getContext().getClassLoader());
                    String resultKey = bundleKey.substring(RESULT_KEY_PREFIX.length());
                    mResults.put(resultKey, savedResult);
                }
            }
        }

        // Restore the saved bundle for all fragments
        HashMap<String, Bundle> allStateBundles = new HashMap<>();
        for (String bundleKey : bundle.keySet()) {
            if (bundleKey.startsWith(FRAGMENT_KEY_PREFIX)) {
                Bundle savedFragmentBundle = bundle.getBundle(bundleKey);
                if (savedFragmentBundle != null) {
                    savedFragmentBundle.setClassLoader(mHost.getContext().getClassLoader());
                    String fragmentKey = bundleKey.substring(FRAGMENT_KEY_PREFIX.length());
                    allStateBundles.put(fragmentKey, savedFragmentBundle);
                }
            }
        }
        mFragmentStore.restoreSaveState(allStateBundles);

        FragmentManagerState fms = bundle.getParcelable(FRAGMENT_MANAGER_STATE_KEY);
        if (fms == null) return;

        // Build the full list of active fragments, instantiating them from
        // their saved state.
        mFragmentStore.resetActiveFragments();
        for (String who : fms.mActive) {
            // Retrieve any saved state, clearing it out for future calls
            Bundle stateBundle = mFragmentStore.setSavedState(who, null);
            if (stateBundle != null) {
                FragmentStateManager fragmentStateManager;
                FragmentState fs = stateBundle.getParcelable(
                        FragmentStateManager.FRAGMENT_STATE_KEY);
                Fragment retainedFragment = mNonConfig.findRetainedFragmentByWho(fs.mWho);
                if (retainedFragment != null) {
                    if (isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(TAG, "restoreSaveState: re-attaching retained "
                                + retainedFragment);
                    }
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, retainedFragment, stateBundle);
                } else {
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, mHost.getContext().getClassLoader(),
                            getFragmentFactory(), stateBundle);
                }
                Fragment f = fragmentStateManager.getFragment();
                f.mSavedFragmentState = stateBundle;
                f.mFragmentManager = this;
                if (isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "restoreSaveState: active (" + f.mWho + "): " + f);
                }
                fragmentStateManager.restoreState(mHost.getContext().getClassLoader());
                mFragmentStore.makeActive(fragmentStateManager);
                // Catch the FragmentStateManager up to our current state
                // In almost all cases, this is Fragment.INITIALIZING, but just in
                // case a FragmentController does something...unique, let's do this anyways.
                fragmentStateManager.setFragmentManagerState(mCurState);
            }
        }

        // Check to make sure there aren't any retained fragments that aren't in mActive
        // This can happen if a retained fragment is added after the state is saved
        for (Fragment f : mNonConfig.getRetainedFragments()) {
            if (!mFragmentStore.containsActiveFragment(f.mWho)) {
                if (isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "Discarding retained Fragment " + f
                            + " that was not found in the set of active Fragments " + fms.mActive);
                }
                mNonConfig.removeRetainedFragment(f);
                // We need to ensure that onDestroy and any other clean up is done
                // so move the Fragment up to CREATED, then mark it as being removed, then
                // destroy it without actually adding the Fragment to the FragmentStore
                f.mFragmentManager = this;
                FragmentStateManager fragmentStateManager = new FragmentStateManager(
                        mLifecycleCallbacksDispatcher, mFragmentStore, f);
                fragmentStateManager.setFragmentManagerState(Fragment.CREATED);
                fragmentStateManager.moveToExpectedState();
                f.mRemoving = true;
                fragmentStateManager.moveToExpectedState();
            }
        }

        // Build the list of currently added fragments.
        mFragmentStore.restoreAddedFragments(fms.mAdded);

        // Build the back stack.
        if (fms.mBackStack != null) {
            mBackStack = new ArrayList<>(fms.mBackStack.length);
            for (int i = 0; i < fms.mBackStack.length; i++) {
                BackStackRecord bse = fms.mBackStack[i].instantiate(this);
                if (isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "restoreAllState: back stack #" + i
                            + " (index " + bse.mIndex + "): " + bse);
                    LogWriter logw = new LogWriter(TAG);
                    PrintWriter pw = new PrintWriter(logw);
                    bse.dump("  ", pw, false);
                    pw.close();
                }
                mBackStack.add(bse);
            }
        } else {
            mBackStack = null;
        }
        mBackStackIndex.set(fms.mBackStackIndex);

        if (fms.mPrimaryNavActiveWho != null) {
            mPrimaryNav = findActiveFragment(fms.mPrimaryNavActiveWho);
            dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
        }

        ArrayList<String> savedBackStackStateKeys = fms.mBackStackStateKeys;
        if (savedBackStackStateKeys != null) {
            for (int i = 0; i < savedBackStackStateKeys.size(); i++) {
                mBackStackStates.put(savedBackStackStateKeys.get(i), fms.mBackStackStates.get(i));
            }
        }

        mLaunchedFragments = new ArrayDeque<>(fms.mLaunchedFragments);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public FragmentHostCallback<?> getHost() {
        return mHost;
    }

    @Nullable
    Fragment getParent() {
        return mParent;
    }

    @NonNull
    FragmentContainer getContainer() {
        return mContainer;
    }

    @NonNull
    FragmentStore getFragmentStore() {
        return mFragmentStore;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SyntheticAccessor")
    void attachController(@NonNull FragmentHostCallback<?> host,
            @NonNull FragmentContainer container, @Nullable final Fragment parent) {
        if (mHost != null) throw new IllegalStateException("Already attached");
        mHost = host;
        mContainer = container;
        mParent = parent;

        // Add a FragmentOnAttachListener to the parent fragment / host to support
        // backward compatibility with the deprecated onAttachFragment() APIs
        if (mParent != null) {
            addFragmentOnAttachListener(new FragmentOnAttachListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onAttachFragment(@NonNull FragmentManager fragmentManager,
                        @NonNull Fragment fragment) {
                    parent.onAttachFragment(fragment);
                }
            });
        } else if (host instanceof FragmentOnAttachListener) {
            addFragmentOnAttachListener((FragmentOnAttachListener) host);
        }

        if (mParent != null) {
            // Since the callback depends on us being the primary navigation fragment,
            // update our callback now that we have a parent so that we have the correct
            // state by default
            updateOnBackPressedCallbackEnabled();
        }
        // Set up the OnBackPressedCallback
        if (host instanceof OnBackPressedDispatcherOwner) {
            OnBackPressedDispatcherOwner dispatcherOwner = ((OnBackPressedDispatcherOwner) host);
            mOnBackPressedDispatcher = dispatcherOwner.getOnBackPressedDispatcher();
            LifecycleOwner owner = parent != null ? parent : dispatcherOwner;
            mOnBackPressedDispatcher.addCallback(owner, mOnBackPressedCallback);
        }

        // Get the FragmentManagerViewModel
        if (parent != null) {
            mNonConfig = parent.mFragmentManager.getChildNonConfig(parent);
        } else if (host instanceof ViewModelStoreOwner) {
            ViewModelStore viewModelStore = ((ViewModelStoreOwner) host).getViewModelStore();
            mNonConfig = FragmentManagerViewModel.getInstance(viewModelStore);
        } else {
            mNonConfig = new FragmentManagerViewModel(false);
        }
        // Ensure that the state is in sync with FragmentManager
        mNonConfig.setIsStateSaved(isStateSaved());
        mFragmentStore.setNonConfig(mNonConfig);

        if (mHost instanceof SavedStateRegistryOwner && parent == null) {
            SavedStateRegistry registry =
                    ((SavedStateRegistryOwner) mHost).getSavedStateRegistry();
            registry.registerSavedStateProvider(SAVED_STATE_KEY, () -> {
                        return saveAllStateInternal();
                    }
            );

            Bundle savedInstanceState = registry
                    .consumeRestoredStateForKey(SAVED_STATE_KEY);
            if (savedInstanceState != null) {
                restoreSaveStateInternal(savedInstanceState);
            }
        }

        if (mHost instanceof ActivityResultRegistryOwner) {
            ActivityResultRegistry registry =
                    ((ActivityResultRegistryOwner) mHost).getActivityResultRegistry();

            String parentId = parent != null ? parent.mWho + ":" : "";
            String keyPrefix = "FragmentManager:" + parentId;

            mStartActivityForResult = registry.register(keyPrefix + "StartActivityForResult",
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            LaunchedFragmentInfo requestInfo = mLaunchedFragments.pollLast();
                            if (requestInfo == null) {
                                Log.w(TAG, "No Activities were started for result for " + this);
                                return;
                            }
                            String fragmentWho = requestInfo.mWho;
                            int requestCode = requestInfo.mRequestCode;
                            Fragment fragment =  mFragmentStore.findFragmentByWho(fragmentWho);
                            // Although unlikely, it is possible this fragment could be null if a
                            // fragment transactions was committed immediately after the for
                            // result call
                            if (fragment == null) {
                                Log.w(TAG,
                                        "Activity result delivered for unknown Fragment "
                                                + fragmentWho);
                                return;
                            }
                            fragment.onActivityResult(requestCode, result.getResultCode(),
                                    result.getData());
                        }
                    });

            mStartIntentSenderForResult = registry.register(keyPrefix
                            + "StartIntentSenderForResult",
                    new FragmentManager.FragmentIntentSenderContract(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            LaunchedFragmentInfo requestInfo = mLaunchedFragments.pollFirst();
                            if (requestInfo == null) {
                                Log.w(TAG, "No IntentSenders were started for " + this);
                                return;
                            }
                            String fragmentWho = requestInfo.mWho;
                            int requestCode = requestInfo.mRequestCode;
                            Fragment fragment =  mFragmentStore.findFragmentByWho(fragmentWho);
                            // Although unlikely, it is possible this fragment could be null if a
                            // fragment transactions was committed immediately after the for
                            // result call
                            if (fragment == null) {
                                Log.w(TAG, "Intent Sender result delivered for unknown Fragment "
                                                + fragmentWho);
                                return;
                            }
                            fragment.onActivityResult(requestCode, result.getResultCode(),
                                    result.getData());
                        }
                    });

            mRequestPermissions = registry.register(keyPrefix + "RequestPermissions",
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    new ActivityResultCallback<Map<String, Boolean>>() {
                        @SuppressLint("SyntheticAccessor")
                        @Override
                        public void onActivityResult(Map<String, Boolean> result) {
                            String[] permissions = result.keySet().toArray(new String[0]);
                            ArrayList<Boolean> resultValues = new ArrayList<>(result.values());
                            int[] grantResults = new int[resultValues.size()];
                            for (int i = 0; i < resultValues.size(); i++) {
                                grantResults[i] = resultValues.get(i)
                                        ? PackageManager.PERMISSION_GRANTED
                                        : PackageManager.PERMISSION_DENIED;
                            }
                            LaunchedFragmentInfo requestInfo = mLaunchedFragments.pollFirst();
                            if (requestInfo == null) {
                                Log.w(TAG, "No permissions were requested for " + this);
                                return;
                            }
                            String fragmentWho = requestInfo.mWho;
                            int requestCode = requestInfo.mRequestCode;
                            Fragment fragment =  mFragmentStore.findFragmentByWho(fragmentWho);
                            // Although unlikely, it is possible this fragment could be null if a
                            // fragment transactions was committed immediately after the request
                            // permissions call
                            if (fragment == null) {
                                Log.w(TAG, "Permission request result delivered for unknown "
                                        + "Fragment " + fragmentWho);
                                return;
                            }
                            fragment.onRequestPermissionsResult(requestCode, permissions,
                                    grantResults);
                        }
                    });
        }

        if (mHost instanceof OnConfigurationChangedProvider) {
            OnConfigurationChangedProvider onConfigurationChangedProvider =
                    (OnConfigurationChangedProvider) mHost;
            onConfigurationChangedProvider.addOnConfigurationChangedListener(
                    mOnConfigurationChangedListener);
        }

        if (mHost instanceof OnTrimMemoryProvider) {
            OnTrimMemoryProvider onTrimMemoryProvider = (OnTrimMemoryProvider) mHost;
            onTrimMemoryProvider.addOnTrimMemoryListener(mOnTrimMemoryListener);
        }

        if (mHost instanceof OnMultiWindowModeChangedProvider) {
            OnMultiWindowModeChangedProvider onMultiWindowModeChangedProvider =
                    (OnMultiWindowModeChangedProvider) mHost;
            onMultiWindowModeChangedProvider.addOnMultiWindowModeChangedListener(
                    mOnMultiWindowModeChangedListener);
        }

        if (mHost instanceof OnPictureInPictureModeChangedProvider) {
            OnPictureInPictureModeChangedProvider onPictureInPictureModeChangedProvider =
                    (OnPictureInPictureModeChangedProvider) mHost;
            onPictureInPictureModeChangedProvider.addOnPictureInPictureModeChangedListener(
                    mOnPictureInPictureModeChangedListener);
        }

        if (mHost instanceof MenuHost && parent == null) {
            ((MenuHost) mHost).addMenuProvider(mMenuProvider);
        }
    }

    void noteStateNotSaved() {
        // A fragment added via the <fragment> tag can have noteStateNotSaved() called
        // by its parent fragment before attachController() has been called. In this case,
        // we should early return as the state not being saved is the default.
        if (mHost == null) {
            return;
        }
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        for (Fragment fragment : mFragmentStore.getFragments()) {
            if (fragment != null) {
                fragment.noteStateNotSaved();
            }
        }
    }

    void launchStartActivityForResult(@NonNull Fragment f,
            @NonNull Intent intent,
            int requestCode, @Nullable Bundle options) {
        if (mStartActivityForResult != null) {
            LaunchedFragmentInfo info = new LaunchedFragmentInfo(f.mWho, requestCode);
            mLaunchedFragments.addLast(info);
            if (options != null) {
                intent.putExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE, options);
            }
            mStartActivityForResult.launch(intent);
        } else {
            mHost.onStartActivityFromFragment(f, intent, requestCode, options);
        }
    }

    @SuppressWarnings("deprecation")
    void launchStartIntentSenderForResult(@NonNull Fragment f,
            @NonNull IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
        if (mStartIntentSenderForResult != null) {
            if (options != null) {
                if (fillInIntent == null) {
                    fillInIntent = new Intent();
                    fillInIntent.putExtra(EXTRA_CREATED_FILLIN_INTENT, true);
                }
                if (isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "ActivityOptions " + options + " were added to fillInIntent "
                            + fillInIntent + " for fragment " + f);
                }
                fillInIntent.putExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE, options);
            }
            IntentSenderRequest request =
                    new IntentSenderRequest.Builder(intent).setFillInIntent(fillInIntent)
                            .setFlags(flagsValues, flagsMask).build();
            LaunchedFragmentInfo info = new LaunchedFragmentInfo(f.mWho, requestCode);
            mLaunchedFragments.addLast(info);
            if (isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "Fragment " + f + "is launching an IntentSender for result ");
            }
            mStartIntentSenderForResult.launch(request);
        } else {
            mHost.onStartIntentSenderFromFragment(f, intent, requestCode, fillInIntent,
                    flagsMask, flagsValues, extraFlags, options);
        }
    }

    @SuppressWarnings("deprecation")
    void launchRequestPermissions(@NonNull Fragment f, @NonNull String[] permissions,
            int requestCode) {
        if (mRequestPermissions != null) {
            LaunchedFragmentInfo info = new LaunchedFragmentInfo(f.mWho, requestCode);
            mLaunchedFragments.addLast(info);
            mRequestPermissions.launch(permissions);
        } else {
            mHost.onRequestPermissionsFromFragment(f, permissions, requestCode);
        }
    }

    void dispatchAttach() {
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        dispatchStateChange(Fragment.ATTACHED);
    }

    void dispatchCreate() {
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        dispatchStateChange(Fragment.CREATED);
    }

    void dispatchViewCreated() {
        dispatchStateChange(Fragment.VIEW_CREATED);
    }

    void dispatchActivityCreated() {
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        dispatchStateChange(Fragment.ACTIVITY_CREATED);
    }

    void dispatchStart() {
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        dispatchStateChange(Fragment.STARTED);
    }

    void dispatchResume() {
        mStateSaved = false;
        mStopped = false;
        mNonConfig.setIsStateSaved(false);
        dispatchStateChange(Fragment.RESUMED);
    }

    void dispatchPause() {
        dispatchStateChange(Fragment.STARTED);
    }

    void dispatchStop() {
        mStopped = true;
        mNonConfig.setIsStateSaved(true);
        dispatchStateChange(Fragment.ACTIVITY_CREATED);
    }

    void dispatchDestroyView() {
        dispatchStateChange(Fragment.CREATED);
    }

    void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions(true);
        endAnimatingAwayFragments();
        clearBackStackStateViewModels();
        dispatchStateChange(Fragment.INITIALIZING);
        if (mHost instanceof OnTrimMemoryProvider) {
            OnTrimMemoryProvider onTrimMemoryProvider = (OnTrimMemoryProvider) mHost;
            onTrimMemoryProvider.removeOnTrimMemoryListener(mOnTrimMemoryListener);
        }
        if (mHost instanceof OnConfigurationChangedProvider) {
            OnConfigurationChangedProvider onConfigurationChangedProvider =
                    (OnConfigurationChangedProvider) mHost;
            onConfigurationChangedProvider.removeOnConfigurationChangedListener(
                    mOnConfigurationChangedListener);
        }
        if (mHost instanceof OnMultiWindowModeChangedProvider) {
            OnMultiWindowModeChangedProvider onMultiWindowModeChangedProvider =
                    (OnMultiWindowModeChangedProvider) mHost;
            onMultiWindowModeChangedProvider.removeOnMultiWindowModeChangedListener(
                    mOnMultiWindowModeChangedListener);
        }
        if (mHost instanceof OnPictureInPictureModeChangedProvider) {
            OnPictureInPictureModeChangedProvider onPictureInPictureModeChangedProvider =
                    (OnPictureInPictureModeChangedProvider) mHost;
            onPictureInPictureModeChangedProvider.removeOnPictureInPictureModeChangedListener(
                    mOnPictureInPictureModeChangedListener);
        }
        if (mHost instanceof MenuHost && mParent == null) {
            ((MenuHost) mHost).removeMenuProvider(mMenuProvider);
        }
        mHost = null;
        mContainer = null;
        mParent = null;
        if (mOnBackPressedDispatcher != null) {
            // mOnBackPressedDispatcher can hold a reference to the host
            // so we need to null it out to prevent memory leaks
            mOnBackPressedCallback.remove();
            mOnBackPressedDispatcher = null;
        }
        if (mStartActivityForResult != null) {
            mStartActivityForResult.unregister();
            mStartIntentSenderForResult.unregister();
            mRequestPermissions.unregister();
        }
    }

    private void dispatchStateChange(int nextState) {
        try {
            mExecutingActions = true;
            mFragmentStore.dispatchStateChange(nextState);
            moveToState(nextState, false);
            Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
            for (SpecialEffectsController controller : controllers) {
                controller.forceCompleteAllOperations();
            }
        } finally {
            mExecutingActions = false;
        }
        execPendingActions(true);
    }

    void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode, boolean recursive) {
        if (recursive && mHost instanceof OnMultiWindowModeChangedProvider) {
            throwException(new IllegalStateException("Do not call dispatchMultiWindowModeChanged() "
                    + "on host. Host implements OnMultiWindowModeChangedProvider and automatically "
                    + "dispatches multi-window mode changes to fragments."));
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
                if (recursive) {
                    f.mChildFragmentManager.dispatchMultiWindowModeChanged(
                            isInMultiWindowMode, true
                    );
                }
            }
        }
    }

    void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode, boolean recursive) {
        if (recursive && mHost instanceof OnPictureInPictureModeChangedProvider) {
            throwException(new IllegalStateException("Do not call "
                    + "dispatchPictureInPictureModeChanged() on host. Host implements "
                    + "OnPictureInPictureModeChangedProvider and automatically dispatches "
                    + "picture-in-picture mode changes to fragments."));
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
                if (recursive) {
                    f.mChildFragmentManager.dispatchPictureInPictureModeChanged(
                            isInPictureInPictureMode, true
                    );
                }
            }
        }
    }

    void dispatchConfigurationChanged(@NonNull Configuration newConfig, boolean recursive) {
        if (recursive && mHost instanceof OnConfigurationChangedProvider) {
            throwException(new IllegalStateException("Do not call dispatchConfigurationChanged() "
                    + "on host. Host implements OnConfigurationChangedProvider and automatically "
                    + "dispatches configuration changes to fragments."));
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performConfigurationChanged(newConfig);
                if (recursive) {
                    f.mChildFragmentManager.dispatchConfigurationChanged(newConfig, true);
                }
            }
        }
    }

    void dispatchLowMemory(boolean recursive) {
        if (recursive && mHost instanceof OnTrimMemoryProvider) {
            throwException(new IllegalStateException("Do not call dispatchLowMemory() on host. "
                    + "Host implements OnTrimMemoryProvider and automatically dispatches "
                    + "low memory callbacks to fragments."));
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performLowMemory();
                if (recursive) {
                    f.mChildFragmentManager.dispatchLowMemory(true);
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    boolean dispatchCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (mCurState < Fragment.CREATED) {
            return false;
        }
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                if (isParentMenuVisible(f) && f.performCreateOptionsMenu(menu, inflater)) {
                    show = true;
                    if (newMenus == null) {
                        newMenus = new ArrayList<>();
                    }
                    newMenus.add(f);
                }
            }
        }

        if (mCreatedMenus != null) {
            for (int i = 0; i < mCreatedMenus.size(); i++) {
                Fragment f = mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(f)) {
                    f.onDestroyOptionsMenu();
                }
            }
        }

        mCreatedMenus = newMenus;

        return show;
    }

    boolean dispatchPrepareOptionsMenu(@NonNull Menu menu) {
        if (mCurState < Fragment.CREATED) {
            return false;
        }
        boolean show = false;
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                if (isParentMenuVisible(f) && f.performPrepareOptionsMenu(menu)) {
                    show = true;
                }
            }
        }
        return show;
    }

    boolean dispatchOptionsItemSelected(@NonNull MenuItem item) {
        if (mCurState < Fragment.CREATED) {
            return false;
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                if (f.performOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean dispatchContextItemSelected(@NonNull MenuItem item) {
        if (mCurState < Fragment.CREATED) {
            return false;
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                if (f.performContextItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    void dispatchOptionsMenuClosed(@NonNull Menu menu) {
        if (mCurState < Fragment.CREATED) {
            return;
        }
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performOptionsMenuClosed(menu);
            }
        }
    }

    void setPrimaryNavigationFragment(@Nullable Fragment f) {
        if (f != null && (!f.equals(findActiveFragment(f.mWho))
                || (f.mHost != null && f.mFragmentManager != this))) {
            throw new IllegalArgumentException("Fragment " + f
                    + " is not an active fragment of FragmentManager " + this);
        }
        Fragment previousPrimaryNav = mPrimaryNav;
        mPrimaryNav = f;
        dispatchParentPrimaryNavigationFragmentChanged(previousPrimaryNav);
        dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
    }

    private void dispatchParentPrimaryNavigationFragmentChanged(@Nullable Fragment f) {
        if (f != null && f.equals(findActiveFragment(f.mWho))) {
            f.performPrimaryNavigationFragmentChanged();
        }
    }

    void dispatchPrimaryNavigationFragmentChanged() {
        updateOnBackPressedCallbackEnabled();
        // Dispatch the change event to this FragmentManager's primary navigation fragment
        dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
    }

    /**
     * Return the currently active primary navigation fragment for this FragmentManager.
     * The primary navigation fragment is set by fragment transactions using
     * {@link FragmentTransaction#setPrimaryNavigationFragment(Fragment)}.
     *
     * <p>The primary navigation fragment's
     * {@link Fragment#getChildFragmentManager() child FragmentManager} will be called first
     * to process delegated navigation actions such as {@link #popBackStack()} if no ID
     * or transaction name is provided to pop to.</p>
     *
     * @return the fragment designated as the primary navigation fragment
     */
    @Nullable
    public Fragment getPrimaryNavigationFragment() {
        return mPrimaryNav;
    }

    void setMaxLifecycle(@NonNull Fragment f, @NonNull Lifecycle.State state) {
        if (!f.equals(findActiveFragment(f.mWho))
                || (f.mHost != null && f.mFragmentManager != this)) {
            throw new IllegalArgumentException("Fragment " + f
                    + " is not an active fragment of FragmentManager " + this);
        }
        f.mMaxState = state;
    }

    /**
     * Set a {@link FragmentFactory} for this FragmentManager that will be used
     * to create new Fragment instances from this point onward.
     * <p>
     * The {@link Fragment#getChildFragmentManager() child FragmentManager} of all Fragments
     * in this FragmentManager will also use this factory if one is not explicitly set.
     *
     * @param fragmentFactory the factory to use to create new Fragment instances
     * @see #getFragmentFactory()
     */
    public void setFragmentFactory(@NonNull FragmentFactory fragmentFactory) {
        mFragmentFactory = fragmentFactory;
    }

    /**
     * Gets the current {@link FragmentFactory} used to instantiate new Fragment instances.
     * <p>
     * If no factory has been explicitly set on this FragmentManager via
     * {@link #setFragmentFactory(FragmentFactory)}, the FragmentFactory of the
     * {@link Fragment#getParentFragmentManager() parent FragmentManager} will be returned.
     *
     * @return the current FragmentFactory
     */
    @NonNull
    public FragmentFactory getFragmentFactory() {
        if (mFragmentFactory != null) {
            return mFragmentFactory;
        }
        if (mParent != null) {
            // This can't call setFragmentFactory since we need to
            // compute this each time getFragmentFactory() is called
            // so that if the parent's FragmentFactory changes, we
            // pick the change up here.
            return mParent.mFragmentManager.getFragmentFactory();
        }
        return mHostFragmentFactory;
    }

    /**
     * Set a {@link SpecialEffectsControllerFactory} for this FragmentManager that will be used
     * to create new SpecialEffectsController instances from this point onward.
     *
     * @param specialEffectsControllerFactory the factory to use to create new
     *                                        SpecialEffectsController instances.
     */
    void setSpecialEffectsControllerFactory(
            @NonNull SpecialEffectsControllerFactory specialEffectsControllerFactory) {
        mSpecialEffectsControllerFactory = specialEffectsControllerFactory;
    }

    /**
     * Gets the current {@link SpecialEffectsControllerFactory} used to instantiate new
     * SpecialEffectsController instances.
     *
     * @return the current SpecialEffectsControllerFactory
     */
    @NonNull
    SpecialEffectsControllerFactory getSpecialEffectsControllerFactory() {
        if (mSpecialEffectsControllerFactory != null) {
            return mSpecialEffectsControllerFactory;
        }
        if (mParent != null) {
            // This can't call setSpecialEffectsControllerFactory since we need to
            // compute this each time getSpecialEffectsControllerFactory() is called
            // so that if the parent's SpecialEffectsControllerFactory changes, we
            // pick the change up here.
            return mParent.mFragmentManager.getSpecialEffectsControllerFactory();
        }
        return mDefaultSpecialEffectsControllerFactory;
    }

    @NonNull
    FragmentLifecycleCallbacksDispatcher getLifecycleCallbacksDispatcher() {
        return mLifecycleCallbacksDispatcher;
    }

    /**
     * Registers a {@link FragmentLifecycleCallbacks} to listen to fragment lifecycle events
     * happening in this FragmentManager. All registered callbacks will be automatically
     * unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    public void registerFragmentLifecycleCallbacks(@NonNull FragmentLifecycleCallbacks cb,
            boolean recursive) {
        mLifecycleCallbacksDispatcher.registerFragmentLifecycleCallbacks(cb, recursive);
    }

    /**
     * Unregisters a previously registered {@link FragmentLifecycleCallbacks}. If the callback
     * was not previously registered this call has no effect. All registered callbacks will be
     * automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    public void unregisterFragmentLifecycleCallbacks(@NonNull FragmentLifecycleCallbacks cb) {
        mLifecycleCallbacksDispatcher.unregisterFragmentLifecycleCallbacks(cb);
    }

    /**
     * Add a {@link FragmentOnAttachListener} that should receive a call to
     * {@link FragmentOnAttachListener#onAttachFragment(FragmentManager, Fragment)} when a
     * new Fragment is attached to this FragmentManager.
     *
     * @param listener Listener to add
     */
    public void addFragmentOnAttachListener(@NonNull FragmentOnAttachListener listener) {
        mOnAttachListeners.add(listener);
    }

    /**
     * Dispatch {@link FragmentOnAttachListener#onAttachFragment(FragmentManager, Fragment)} to
     * each listener registered via {@link #addFragmentOnAttachListener(FragmentOnAttachListener)}.
     *
     * @param fragment The Fragment that was attached
     */
    void dispatchOnAttachFragment(@NonNull Fragment fragment) {
        for (FragmentOnAttachListener listener : mOnAttachListeners) {
            listener.onAttachFragment(this, fragment);
        }
    }

    /**
     * Remove a {@link FragmentOnAttachListener} that was previously added via
     * {@link #addFragmentOnAttachListener(FragmentOnAttachListener)}. It will no longer
     * get called when a new Fragment is attached.
     *
     * @param listener Listener to remove
     */
    public void removeFragmentOnAttachListener(@NonNull FragmentOnAttachListener listener) {
        mOnAttachListeners.remove(listener);
    }

    void dispatchOnHiddenChanged() {
        for (Fragment fragment : mFragmentStore.getActiveFragments()) {
            if (fragment != null) {
                fragment.onHiddenChanged(fragment.isHidden());
                fragment.mChildFragmentManager.dispatchOnHiddenChanged();
            }
        }
    }

    // Checks if fragments that belong to this fragment manager (or their children) have menus,
    // and if they are visible.
    boolean checkForMenus() {
        boolean hasMenu = false;
        for (Fragment fragment : mFragmentStore.getActiveFragments()) {
            if (fragment != null) {
                hasMenu = isMenuAvailable(fragment);
            }
            if (hasMenu) {
                return true;
            }
        }
        return false;
    }

    private boolean isMenuAvailable(@NonNull Fragment f) {
        return (f.mHasMenu && f.mMenuVisible) || f.mChildFragmentManager.checkForMenus();
    }

    void invalidateMenuForFragment(@NonNull Fragment f) {
        if (f.mAdded && isMenuAvailable(f)) {
            mNeedMenuInvalidate = true;
        }
    }

    private boolean isParentAdded() {
        // The root fragment manager is always considered added
        if (mParent == null) {
            return true;
        }
        return mParent.isAdded() && mParent.getParentFragmentManager().isParentAdded();
    }

    static int reverseTransit(int transit) {
        int rev = 0;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_FADE;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN;
                break;
        }
        return rev;

    }

    @NonNull
    LayoutInflater.Factory2 getLayoutInflaterFactory() {
        return mLayoutInflaterFactory;
    }

    /** Returns the current policy for this FragmentManager. If no policy is set, returns null. */
    @Nullable
    public FragmentStrictMode.Policy getStrictModePolicy() {
        return mStrictModePolicy;
    }

    /**
     * Sets the policy for what actions should be detected, as well as the penalty if such actions
     * occur. The {@link Fragment#getChildFragmentManager() child FragmentManager} of all Fragments
     * in this FragmentManager will also use this policy if one is not explicitly set. Pass null to
     * clear the policy.
     *
     * @param policy the policy to put into place
     */
    public void setStrictModePolicy(@Nullable FragmentStrictMode.Policy policy) {
        mStrictModePolicy = policy;
    }

    /**
     * An add or pop transaction to be scheduled for the UI thread.
     */
    interface OpGenerator {
        /**
         * Generate transactions to add to {@code records} and whether or not the transaction is
         * an add or pop to {@code isRecordPop}.
         *
         * records and isRecordPop must be added equally so that each transaction in records
         * matches the boolean for whether or not it is a pop in isRecordPop.
         *
         * @param records A list to add transactions to.
         * @param isRecordPop A list to add whether or not the transactions added to records is
         *                    a pop transaction.
         * @return true if something was added or false otherwise.
         */
        boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                @NonNull ArrayList<Boolean> isRecordPop);
    }

    /**
     * A pop operation OpGenerator. This will be run on the UI thread and will generate the
     * transactions that will be popped if anything can be popped.
     */
    private class PopBackStackState implements OpGenerator {
        final String mName;
        final int mId;
        final int mFlags;

        PopBackStackState(@Nullable String name, int id, int flags) {
            mName = name;
            mId = id;
            mFlags = flags;
        }

        @Override
        public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                @NonNull ArrayList<Boolean> isRecordPop) {
            if (mPrimaryNav != null // We have a primary nav fragment
                    && mId < 0 // No valid id (since they're local)
                    && mName == null) { // no name to pop to (since they're local)
                final FragmentManager childManager = mPrimaryNav.getChildFragmentManager();
                if (childManager.popBackStackImmediate()) {
                    // We didn't add any operations for this FragmentManager even though
                    // a child did do work.
                    return false;
                }
            }
            return popBackStackState(records, isRecordPop, mName, mId, mFlags);
        }
    }

    private class RestoreBackStackState implements OpGenerator {

        private final String mName;

        RestoreBackStackState(@NonNull String name) {
            mName = name;
        }

        @Override
        public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                @NonNull ArrayList<Boolean> isRecordPop) {
            return restoreBackStackState(records, isRecordPop, mName);
        }
    }

    private class SaveBackStackState implements OpGenerator {

        private final String mName;

        SaveBackStackState(@NonNull String name) {
            mName = name;
        }

        @Override
        public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                @NonNull ArrayList<Boolean> isRecordPop) {
            return saveBackStackState(records, isRecordPop, mName);
        }
    }

    private class ClearBackStackState implements OpGenerator {

        private final String mName;

        ClearBackStackState(@NonNull String name) {
            mName = name;
        }

        @Override
        public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                @NonNull ArrayList<Boolean> isRecordPop) {
            return clearBackStackState(records, isRecordPop, mName);
        }
    }

    @SuppressLint("BanParcelableUsage")
    static class LaunchedFragmentInfo implements Parcelable {
        String mWho;
        int mRequestCode;

        LaunchedFragmentInfo(@NonNull String who, int requestCode) {
            mWho = who;
            mRequestCode = requestCode;
        }

        LaunchedFragmentInfo(@NonNull Parcel in) {
            mWho = in.readString();
            mRequestCode = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mWho);
            dest.writeInt(mRequestCode);
        }

        public static final Parcelable.Creator<LaunchedFragmentInfo> CREATOR =
                new Creator<LaunchedFragmentInfo>() {
                    @Override
                    public LaunchedFragmentInfo createFromParcel(Parcel in) {
                        return new LaunchedFragmentInfo(in);
                    }

                    @Override
                    public LaunchedFragmentInfo[] newArray(int size) {
                        return new LaunchedFragmentInfo[size];
                    }
                };
    }

    static class FragmentIntentSenderContract extends ActivityResultContract<IntentSenderRequest,
            ActivityResult> {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, IntentSenderRequest input) {
            Intent result = new Intent(ACTION_INTENT_SENDER_REQUEST);
            Intent fillInIntent = input.getFillInIntent();
            if (fillInIntent != null) {
                Bundle activityOptions = fillInIntent.getBundleExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE);
                if (activityOptions != null) {
                    result.putExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE, activityOptions);
                    fillInIntent.removeExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE);
                    if (fillInIntent.getBooleanExtra(EXTRA_CREATED_FILLIN_INTENT, false)) {
                        input = new IntentSenderRequest.Builder(input.getIntentSender())
                                .setFillInIntent(null)
                                .setFlags(input.getFlagsValues(), input.getFlagsMask())
                                .build();
                    }
                }
            }
            result.putExtra(EXTRA_INTENT_SENDER_REQUEST, input);
            if (isLoggingEnabled(Log.VERBOSE)) {
                Log.v(TAG, "CreateIntent created the following intent: " + result);
            }
            return result;
        }

        @NonNull
        @Override
        public ActivityResult parseResult(int resultCode, @Nullable Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }
}
