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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
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
import androidx.collection.ArraySet;
import androidx.core.os.CancellationSignal;
import androidx.fragment.R;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private static boolean DEBUG = false;
    static final String TAG = "FragmentManager";
    static boolean USE_STATE_MANAGER = false;

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

    static boolean isLoggingEnabled(int level) {
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
    }

    /**
     * A {@link FragmentResultListener} that is lifecycle aware so that
     * the listener can be fired when the lifecycle is {@link Lifecycle.State#STARTED}.
     */
    private static class LifecycleAwareResultListener implements FragmentResultListener {
        private final Lifecycle mLifecycle;
        private final FragmentResultListener mListener;

        LifecycleAwareResultListener(@NonNull Lifecycle lifecycle,
                @NonNull FragmentResultListener listener) {
            mLifecycle = lifecycle;
            mListener = listener;
        }

        public boolean isAtLeast(Lifecycle.State state) {
            return mLifecycle.getCurrentState().isAtLeast(state);
        }

        @Override
        public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
            mListener.onFragmentResult(requestKey, result);
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

    private final ConcurrentHashMap<String, Bundle> mResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LifecycleAwareResultListener> mResultListeners =
            new ConcurrentHashMap<>();

    private ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private ConcurrentHashMap<Fragment, HashSet<CancellationSignal>>
            mExitAnimationCancellationSignals = new ConcurrentHashMap<>();
    private final FragmentTransition.Callback mFragmentTransitionCallback =
            new FragmentTransition.Callback() {
                @Override
                public void onStart(@NonNull Fragment fragment,
                        @NonNull CancellationSignal signal) {
                    addCancellationSignal(fragment, signal);
                }

                @Override
                public void onComplete(@NonNull Fragment f, @NonNull CancellationSignal signal) {
                    if (!signal.isCanceled()) {
                        removeCancellationSignal(f, signal);
                    }
                }
            };
    private final FragmentLifecycleCallbacksDispatcher mLifecycleCallbacksDispatcher =
            new FragmentLifecycleCallbacksDispatcher(this);
    private final CopyOnWriteArrayList<FragmentOnAttachListener> mOnAttachListeners =
            new CopyOnWriteArrayList<>();

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

    // Key to retrieve the request code from any intents that are started
    private static final String EXTRA_KEY_REQUEST_CODE = "activity.result.requestCode";

    private ActivityResultLauncher<Intent> mStartActivityForResult;
    private ActivityResultLauncher<IntentSenderRequest> mStartIntentSenderForResult;
    private ActivityResultLauncher<String[]> mRequestPermissions;

    ArrayDeque<LaunchedFragmentInfo> mLaunchedFragments = new ArrayDeque<>();

    private boolean mNeedMenuInvalidate;
    private boolean mStateSaved;
    private boolean mStopped;
    private boolean mDestroyed;
    private boolean mHavePendingDeferredStart;

    // Temporary vars for removing redundant operations in BackStackRecords:
    private ArrayList<BackStackRecord> mTmpRecords;
    private ArrayList<Boolean> mTmpIsPop;
    private ArrayList<Fragment> mTmpAddedFragments;

    // Postponed transactions.
    private ArrayList<StartEnterTransitionListener> mPostponedTransactions;

    private FragmentManagerViewModel mNonConfig;

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
     * @hide -- remove once prebuilts are in.
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
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopBackStackState(null, id, flags), false);
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

    /**
     * Add new {@link CancellationSignal} for exit animation cancel callbacks
     */
    void addCancellationSignal(@NonNull Fragment f, @NonNull CancellationSignal signal) {
        if (mExitAnimationCancellationSignals.get(f) == null) {
            mExitAnimationCancellationSignals.put(f, new HashSet<CancellationSignal>());
        }
        mExitAnimationCancellationSignals.get(f).add(signal);
    }

    /**
     * Remove a {@link CancellationSignal} that was previously added with
     * {@link #addCancellationSignal(Fragment, CancellationSignal)}.
     *
     * Destroy the view of the Fragment associated with that listener and move it to the proper
     * state.
     */
    void removeCancellationSignal(@NonNull Fragment f, @NonNull CancellationSignal signal) {
        HashSet<CancellationSignal> signals = mExitAnimationCancellationSignals.get(f);
        if (signals != null && signals.remove(signal) && signals.isEmpty()) {
            mExitAnimationCancellationSignals.remove(f);
            // The Fragment state must be below STARTED before destroying the view to ensure we
            // support hide/show
            if (f.mState < Fragment.STARTED) {
                destroyFragmentView(f);
                moveToState(f);
            }
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
    }

    @Override
    public final void clearFragmentResult(@NonNull String requestKey) {
        mResults.remove(requestKey);
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
        lifecycle.addObserver(observer);
        mResultListeners.put(requestKey, new LifecycleAwareResultListener(lifecycle, listener));
    }

    @Override
    public final void clearFragmentResultListener(@NonNull String requestKey) {
        mResultListeners.remove(requestKey);
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
    private static Fragment findViewFragment(@NonNull View view) {
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
            if (USE_STATE_MANAGER) {
                fragmentStateManager.moveToExpectedState();
            } else {
                moveToState(f);
            }
        }
    }

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    @SuppressWarnings("deprecation")
    void moveToState(@NonNull Fragment f, int newState) {
        FragmentStateManager fragmentStateManager = mFragmentStore.getFragmentStateManager(f.mWho);
        if (fragmentStateManager == null) {
            // Ideally, we only call moveToState() on active Fragments. However,
            // in restoreSaveState() we can call moveToState() on retained Fragments
            // just to clean them up without them ever being added to mActive.
            // For these cases, a brand new FragmentStateManager is enough.
            fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                    mFragmentStore, f);
            // Only allow this FragmentStateManager to go up to CREATED at the most
            fragmentStateManager.setFragmentManagerState(Fragment.CREATED);
        }
        newState = Math.min(newState, fragmentStateManager.computeExpectedState());
        if (f.mState <= newState) {
            // If we are moving to the same state, we do not need to give up on the animation.
            if (f.mState < newState && !mExitAnimationCancellationSignals.isEmpty()) {
                // The fragment is currently being animated...  but!  Now we
                // want to move our state back up.  Give up on waiting for the
                // animation and proceed from where we are.
                cancelExitAnimation(f);
            }
            switch (f.mState) {
                case Fragment.INITIALIZING:
                    if (newState > Fragment.INITIALIZING) {
                        fragmentStateManager.attach();
                    }
                    // fall through
                case Fragment.ATTACHED:
                    if (newState > Fragment.ATTACHED) {
                        fragmentStateManager.create();
                    }
                    // fall through
                case Fragment.CREATED:
                    // We want to unconditionally run this anytime we do a moveToState that
                    // moves the Fragment above INITIALIZING, including cases such as when
                    // we move from CREATED => CREATED as part of the case fall through above.
                    if (newState > Fragment.INITIALIZING) {
                        fragmentStateManager.ensureInflatedView();
                    }

                    if (newState > Fragment.CREATED) {
                        fragmentStateManager.createView();
                        fragmentStateManager.activityCreated();
                    }
                    // fall through
                case Fragment.ACTIVITY_CREATED:
                    if (newState > Fragment.ACTIVITY_CREATED) {
                        fragmentStateManager.start();
                    }
                    // fall through
                case Fragment.STARTED:
                    if (newState > Fragment.STARTED) {
                        fragmentStateManager.resume();
                    }
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case Fragment.RESUMED:
                    if (newState < Fragment.RESUMED) {
                        fragmentStateManager.pause();
                    }
                    // fall through
                case Fragment.STARTED:
                    if (newState < Fragment.STARTED) {
                        fragmentStateManager.stop();
                    }
                    // fall through
                case Fragment.ACTIVITY_CREATED:
                    if (newState < Fragment.ACTIVITY_CREATED) {
                        if (isLoggingEnabled(Log.DEBUG)) {
                            Log.d(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        }
                        if (f.mView != null) {
                            // Need to save the current view state if not
                            // done already.
                            if (mHost.onShouldSaveFragmentState(f) && f.mSavedViewState == null) {
                                fragmentStateManager.saveViewState();
                            }
                        }
                        FragmentAnim.AnimationOrAnimator anim = null;
                        if (f.mView != null && f.mContainer != null) {
                            // Stop any current animations:
                            f.mContainer.endViewTransition(f.mView);
                            f.mView.clearAnimation();
                            // If parent is being removed, no need to handle child animations.
                            if (!f.isRemovingParent()) {
                                if (mCurState > Fragment.INITIALIZING && !mDestroyed
                                        && f.mView.getVisibility() == View.VISIBLE
                                        && f.mPostponedAlpha >= 0) {
                                    anim = FragmentAnim.loadAnimation(mHost.getContext(),
                                            f, false);
                                }
                                f.mPostponedAlpha = 0;
                                // Robolectric tests do not post the animation like a real device
                                // so we should keep up with the container and view in case the
                                // fragment view is destroyed before we can remove it.
                                ViewGroup container = f.mContainer;
                                View view = f.mView;
                                if (anim != null) {
                                    FragmentAnim.animateRemoveFragment(f, anim,
                                            mFragmentTransitionCallback);
                                }
                                container.removeView(view);
                                // If the local container is different from the fragment
                                // container, that means onAnimationEnd was called, onDestroyView
                                // was dispatched and the fragment was already moved to state, so
                                // we should early return here instead of attempting to move to
                                // state again.
                                if (container != f.mContainer) {
                                    return;
                                }
                            }
                        }
                        // If a fragment has an exit animation (or transition), do not destroy
                        // its view immediately and set the state after animating
                        if (mExitAnimationCancellationSignals.get(f) == null) {
                            fragmentStateManager.destroyFragmentView();
                        }
                    }
                    // fall through
                case Fragment.CREATED:
                    if (newState < Fragment.CREATED) {
                        if (mExitAnimationCancellationSignals.get(f) != null) {
                            // We are waiting for the fragment's view to finish animating away.
                            newState = Fragment.CREATED;
                        } else {
                            fragmentStateManager.destroy();
                        }
                    }
                    // fall through
                case Fragment.ATTACHED:
                    if (newState < Fragment.ATTACHED) {
                        fragmentStateManager.detach();
                    }
            }
        }

        if (f.mState != newState) {
            if (isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "moveToState: Fragment state for " + f + " not updated inline; "
                        + "expected state " + newState + " found " + f.mState);
            }
            f.mState = newState;
        }
    }

    // If there is a listener associated with the given fragment, remove that listener and
    // destroy the fragment's view.
    private void cancelExitAnimation(@NonNull Fragment f) {
        HashSet<CancellationSignal> signals = mExitAnimationCancellationSignals.get(f);
        if (signals != null) {
            for (CancellationSignal signal: signals) {
                signal.cancel();
            }
            signals.clear();
            destroyFragmentView(f);
            mExitAnimationCancellationSignals.remove(f);
        }
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

    private void destroyFragmentView(@NonNull Fragment fragment) {
        fragment.performDestroyView();
        mLifecycleCallbacksDispatcher.dispatchOnFragmentViewDestroyed(fragment, false);
        fragment.mContainer = null;
        fragment.mView = null;
        // Set here to ensure that Observers are called after
        // the Fragment's view is set to null
        fragment.mViewLifecycleOwner = null;
        fragment.mViewLifecycleOwnerLiveData.setValue(null);
        fragment.mInLayout = false;
    }

    void moveToState(@NonNull Fragment f) {
        moveToState(f, mCurState);
    }

    /**
     * Fragments that have been shown or hidden don't have their visibility changed or
     * animations run during the {@link #showFragment(Fragment)} or {@link #hideFragment(Fragment)}
     * calls. After fragments are brought to their final state in
     * {@link #moveFragmentToExpectedState(Fragment)} the fragments that have been shown or
     * hidden must have their visibility changed and their animations started here.
     *
     * @param fragment The fragment with mHiddenChanged = true that should change its View's
     *                 visibility and start the show or hide animation.
     */
    private void completeShowHideFragment(@NonNull final Fragment fragment) {
        if (fragment.mView != null) {
            FragmentAnim.AnimationOrAnimator anim = FragmentAnim.loadAnimation(
                    mHost.getContext(), fragment, !fragment.mHidden);
            if (anim != null && anim.animator != null) {
                anim.animator.setTarget(fragment.mView);
                if (fragment.mHidden) {
                    if (fragment.isHideReplaced()) {
                        fragment.setHideReplaced(false);
                    } else {
                        final ViewGroup container = fragment.mContainer;
                        final View animatingView = fragment.mView;
                        container.startViewTransition(animatingView);
                        // Delay the actual hide operation until the animation finishes,
                        // otherwise the fragment will just immediately disappear
                        anim.animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                container.endViewTransition(animatingView);
                                animation.removeListener(this);
                                if (fragment.mView != null && fragment.mHidden) {
                                    fragment.mView.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                } else {
                    fragment.mView.setVisibility(View.VISIBLE);
                }
                anim.animator.start();
            } else {
                if (anim != null) {
                    fragment.mView.startAnimation(anim.animation);
                    anim.animation.start();
                }
                final int visibility = fragment.mHidden && !fragment.isHideReplaced()
                        ? View.GONE
                        : View.VISIBLE;
                fragment.mView.setVisibility(visibility);
                if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                }
            }
        }
        if (fragment.mAdded && isMenuAvailable(fragment)) {
            mNeedMenuInvalidate = true;
        }
        fragment.mHiddenChanged = false;
        fragment.onHiddenChanged(fragment.mHidden);
    }

    /**
     * Moves a fragment to its expected final state or the fragment manager's state, depending
     * on whether the fragment manager's state is raised properly.
     *
     * @param f The fragment to change.
     */
    void moveFragmentToExpectedState(@NonNull Fragment f) {
        if (!mFragmentStore.containsActiveFragment(f.mWho)) {
            if (isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "Ignoring moving " + f + " to state " + mCurState
                        + "since it is not added to " + this);
            }
            return;
        }
        moveToState(f);

        if (f.mView != null) {
            if (f.mIsNewlyAdded && f.mContainer != null) {
                // Make it visible and run the animations
                if (f.mPostponedAlpha > 0f) {
                    f.mView.setAlpha(f.mPostponedAlpha);
                }
                f.mPostponedAlpha = 0f;
                f.mIsNewlyAdded = false;
                // run animations:
                FragmentAnim.AnimationOrAnimator anim = FragmentAnim.loadAnimation(
                        mHost.getContext(), f, true);
                if (anim != null) {
                    if (anim.animation != null) {
                        f.mView.startAnimation(anim.animation);
                    } else {
                        anim.animator.setTarget(f.mView);
                        anim.animator.start();
                    }
                }
            }
        }
        if (f.mHiddenChanged) {
            completeShowHideFragment(f);
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

        if (USE_STATE_MANAGER) {
            mFragmentStore.moveToExpectedState();
        } else {
            // Must add them in the proper order. mActive fragments may be out of order
            for (Fragment f : mFragmentStore.getFragments()) {
                moveFragmentToExpectedState(f);
            }

            // Now iterate through all active fragments. These will include those that are removed
            // and detached.
            for (FragmentStateManager fragmentStateManager :
                    mFragmentStore.getActiveFragmentStateManagers()) {
                Fragment f = fragmentStateManager.getFragment();
                if (!f.mIsNewlyAdded) {
                    moveFragmentToExpectedState(f);
                }
                boolean beingRemoved = f.mRemoving && !f.isInBackStack();
                if (beingRemoved) {
                    mFragmentStore.makeInactive(fragmentStateManager);
                }
            }
        }

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

    void addFragment(@NonNull Fragment fragment) {
        if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "add: " + fragment);
        FragmentStateManager fragmentStateManager = createOrGetFragmentStateManager(fragment);
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
     * Marks a fragment as hidden to be later animated in with
     * {@link #completeShowHideFragment(Fragment)}.
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
     * Marks a fragment as shown to be later animated in with
     * {@link #completeShowHideFragment(Fragment)}.
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
            boolean postponeReady =
                    mPostponedTransactions != null && !mPostponedTransactions.isEmpty();
            boolean pendingReady = mPendingActions.size() == 1;
            if (postponeReady || pendingReady) {
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
        mExecutingActions = true;
        try {
            executePostponedTransaction(null, null);
        } finally {
            mExecutingActions = false;
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
     * Complete the execution of transactions that have previously been postponed, but are
     * now ready.
     */
    private void executePostponedTransaction(@Nullable ArrayList<BackStackRecord> records,
            @Nullable ArrayList<Boolean> isRecordPop) {
        int numPostponed = mPostponedTransactions == null ? 0 : mPostponedTransactions.size();
        for (int i = 0; i < numPostponed; i++) {
            StartEnterTransitionListener listener = mPostponedTransactions.get(i);
            if (records != null && !listener.mIsBack) {
                int index = records.indexOf(listener.mRecord);
                if (index != -1 && isRecordPop != null && isRecordPop.get(index)) {
                    mPostponedTransactions.remove(i);
                    i--;
                    numPostponed--;
                    listener.cancelTransaction();
                    continue;
                }
            }
            if (listener.isReady() || (records != null
                    && listener.mRecord.interactsWith(records, 0, records.size()))) {
                mPostponedTransactions.remove(i);
                i--;
                numPostponed--;
                int index;
                if (records != null && !listener.mIsBack
                        && (index = records.indexOf(listener.mRecord)) != -1
                        && isRecordPop != null
                        && isRecordPop.get(index)) {
                    // This is popping a postponed transaction
                    listener.cancelTransaction();
                } else {
                    listener.completeTransaction();
                }
            }
        }
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

        // Force start of any postponed transactions that interact with scheduled transactions:
        executePostponedTransaction(records, isRecordPop);

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
            if (USE_STATE_MANAGER) {
                // When reordering isn't allowed, we may be operating on Fragments that haven't
                // been made active
                for (int index = startIndex; index < endIndex; index++) {
                    BackStackRecord record = records.get(index);
                    for (FragmentTransaction.Op op : record.mOps) {
                        Fragment fragment = op.mFragment;
                        if (fragment != null) {
                            FragmentStateManager fragmentStateManager =
                                    createOrGetFragmentStateManager(fragment);
                            mFragmentStore.makeActive(fragmentStateManager);
                        }
                    }
                }
            } else {
                FragmentTransition.startTransitions(mHost.getContext(), mContainer,
                        records, isRecordPop, startIndex, endIndex,
                        false, mFragmentTransitionCallback);
            }
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        if (USE_STATE_MANAGER) {
            if (allowReordering) {
                moveToState(mCurState, true);
            }
            // The last operation determines the overall direction, this ensures that operations
            // such as push, push, pop, push are correctly considered a push
            boolean isPop = isRecordPop.get(endIndex - 1);
            Set<SpecialEffectsController> changedControllers = collectChangedControllers(
                    records, startIndex, endIndex);
            for (SpecialEffectsController controller : changedControllers) {
                controller.updateOperationDirection(isPop);
                controller.executePendingOperations();
            }
        } else {
            int postponeIndex = endIndex;
            if (allowReordering) {
                ArraySet<Fragment> addedFragments = new ArraySet<>();
                addAddedFragments(addedFragments);
                postponeIndex = postponePostponableTransactions(records, isRecordPop,
                        startIndex, endIndex, addedFragments);
                makeRemovedFragmentsInvisible(addedFragments);
            }

            if (postponeIndex != startIndex && allowReordering) {
                // need to run something now
                if (mCurState >= Fragment.CREATED) {
                    FragmentTransition.startTransitions(mHost.getContext(), mContainer,
                            records, isRecordPop, startIndex,
                            postponeIndex, true, mFragmentTransitionCallback);
                }
                moveToState(mCurState, true);
            }
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
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
     * Any fragments that were removed because they have been postponed should have their views
     * made invisible by setting their alpha to 0.
     *
     * @param fragments The fragments that were added during operation execution. Only the ones
     *                  that are no longer added will have their alpha changed.
     */
    private void makeRemovedFragmentsInvisible(@NonNull ArraySet<Fragment> fragments) {
        final int numAdded = fragments.size();
        for (int i = 0; i < numAdded; i++) {
            final Fragment fragment = fragments.valueAt(i);
            if (!fragment.mAdded) {
                final View view = fragment.requireView();
                fragment.mPostponedAlpha = view.getAlpha();
                view.setAlpha(0f);
            }
        }
    }

    /**
     * Examine all transactions and determine which ones are marked as postponed. Those will
     * have their operations rolled back and moved to the end of the record list (up to endIndex).
     * It will also add the postponed transaction to the queue.
     *
     * @param records A list of BackStackRecords that should be checked.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex The index of the first record in <code>records</code> to be checked
     * @param endIndex One more than the final record index in <code>records</code> to be checked.
     * @return The index of the first postponed transaction or endIndex if no transaction was
     * postponed.
     */
    private int postponePostponableTransactions(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, int startIndex, int endIndex,
            @NonNull ArraySet<Fragment> added) {
        int postponeIndex = endIndex;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            boolean isPostponed = record.isPostponed()
                    && !record.interactsWith(records, i + 1, endIndex);
            if (isPostponed) {
                if (mPostponedTransactions == null) {
                    mPostponedTransactions = new ArrayList<>();
                }
                StartEnterTransitionListener listener =
                        new StartEnterTransitionListener(record, isPop);
                mPostponedTransactions.add(listener);
                record.setOnStartPostponedListener(listener);

                // roll back the transaction
                if (isPop) {
                    record.executeOps();
                } else {
                    record.executePopOps(false);
                }

                // move to the end
                postponeIndex--;
                if (i != postponeIndex) {
                    records.remove(i);
                    records.add(postponeIndex, record);
                }

                // different views may be visible now
                addAddedFragments(added);
            }
        }
        return postponeIndex;
    }

    /**
     * When a postponed transaction is ready to be started, this completes the transaction,
     * removing, hiding, or showing views as well as starting the animations and transitions.
     * <p>
     * {@code runtransitions} is set to false when the transaction postponement was interrupted
     * abnormally -- normally by a new transaction being started that affects the postponed
     * transaction.
     *
     * @param record The transaction to run
     * @param isPop true if record is popping or false if it is adding
     * @param runTransitions true if the fragment transition should be run or false otherwise.
     * @param moveToState true if the state should be changed after executing the operations.
     *                    This is false when the transaction is canceled when a postponed
     *                    transaction is popped.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void completeExecute(@NonNull BackStackRecord record, boolean isPop, boolean runTransitions,
            boolean moveToState) {
        if (isPop) {
            record.executePopOps(moveToState);
        } else {
            record.executeOps();
        }
        ArrayList<BackStackRecord> records = new ArrayList<>(1);
        ArrayList<Boolean> isRecordPop = new ArrayList<>(1);
        records.add(record);
        isRecordPop.add(isPop);
        if (runTransitions && mCurState >= Fragment.CREATED) {
            FragmentTransition.startTransitions(mHost.getContext(), mContainer,
                    records, isRecordPop, 0, 1, true,
                    mFragmentTransitionCallback);
        }
        if (moveToState) {
            moveToState(mCurState, true);
        }

        for (Fragment fragment : mFragmentStore.getActiveFragments()) {
            // Allow added fragments to be removed during the pop since we aren't going
            // to move them to the final state with moveToState(mCurState).
            if (fragment != null) {
                if (fragment.mView != null && fragment.mIsNewlyAdded
                        && record.interactsWith(fragment.mContainerId)) {
                    if (fragment.mPostponedAlpha > 0) {
                        fragment.mView.setAlpha(fragment.mPostponedAlpha);
                    }
                    if (moveToState) {
                        fragment.mPostponedAlpha = 0;
                    } else {
                        fragment.mPostponedAlpha = -1;
                        fragment.mIsNewlyAdded = false;
                    }
                }
            }
        }
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
                // Only execute the add operations at the end of
                // all transactions.
                boolean moveToState = i == (endIndex - 1);
                record.executePopOps(moveToState);
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
        if (container != null) {
            if (container.getTag(R.id.visible_removing_fragment_view_tag) == null) {
                container.setTag(R.id.visible_removing_fragment_view_tag, f);
            }
            ((Fragment) container.getTag(R.id.visible_removing_fragment_view_tag))
                    .setNextAnim(f.getNextAnim());
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
     * Ensure that fragments that are added are moved to at least the CREATED state.
     * Any newly-added Views are inserted into {@code added} so that the Transaction can be
     * postponed with {@link Fragment#postponeEnterTransition()}. They will later be made
     * invisible (by setting their alpha to 0) if they have been removed when postponed.
     */
    private void addAddedFragments(@NonNull ArraySet<Fragment> added) {
        if (mCurState < Fragment.CREATED) {
            return;
        }
        // We want to leave the fragment in the started state
        final int state = Math.min(mCurState, Fragment.STARTED);
        for (Fragment fragment : mFragmentStore.getFragments()) {
            if (fragment.mState < state) {
                moveToState(fragment, state);
                if (fragment.mView != null && !fragment.mHidden && fragment.mIsNewlyAdded) {
                    added.add(fragment);
                }
            }
        }
    }

    /**
     * Starts all postponed transactions regardless of whether they are ready or not.
     */
    private void forcePostponedTransactions() {
        if (mPostponedTransactions != null) {
            while (!mPostponedTransactions.isEmpty()) {
                mPostponedTransactions.remove(0).completeTransaction();
            }
        }
    }

    /**
     * Ends the animations of fragments so that they immediately reach the end state.
     * This is used prior to saving the state so that the correct state is saved.
     */
    private void endAnimatingAwayFragments() {
        if (USE_STATE_MANAGER) {
            Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
            for (SpecialEffectsController controller : controllers) {
                controller.cancelAllOperations();
            }
        } else {
            if (!mExitAnimationCancellationSignals.isEmpty()) {
                for (Fragment fragment: mExitAnimationCancellationSignals.keySet()) {
                    cancelExitAnimation(fragment);
                    moveToState(fragment);
                }
            }
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

            final int numActions = mPendingActions.size();
            for (int i = 0; i < numActions; i++) {
                didSomething |= mPendingActions.get(i).generateOps(records, isPop);
            }
            mPendingActions.clear();
            mHost.getHandler().removeCallbacks(mExecCommit);
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

    void addBackStackState(BackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<>();
        }
        mBackStack.add(state);
    }

    @SuppressWarnings({"unused", "WeakerAccess"}) /* synthetic access */
    boolean popBackStackState(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop, @Nullable String name, int id, int flags) {
        if (mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & POP_BACK_STACK_INCLUSIVE) == 0) {
            int last = mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            records.add(mBackStack.remove(last));
            isRecordPop.add(true);
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                // If a name or ID is specified, look for that place in
                // the stack.
                index = mBackStack.size() - 1;
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
                    return false;
                }
                if ((flags & POP_BACK_STACK_INCLUSIVE) != 0) {
                    index--;
                    // Consume all following entries that match.
                    while (index >= 0) {
                        BackStackRecord bss = mBackStack.get(index);
                        if ((name != null && name.equals(bss.getName()))
                                || (id >= 0 && id == bss.mIndex)) {
                            index--;
                            continue;
                        }
                        break;
                    }
                }
            }
            if (index == mBackStack.size() - 1) {
                return false;
            }
            for (int i = mBackStack.size() - 1; i > index; i--) {
                records.add(mBackStack.remove(i));
                isRecordPop.add(true);
            }
        }
        return true;
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
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions(true);

        mStateSaved = true;
        mNonConfig.setIsStateSaved(true);

        // First collect all active fragments.
        ArrayList<FragmentState> active = mFragmentStore.saveActiveFragments();

        if (active.isEmpty()) {
            if (isLoggingEnabled(Log.VERBOSE)) Log.v(TAG, "saveAllState: no fragments!");
            return null;
        }

        // Build list of currently added fragments.
        ArrayList<String> added = mFragmentStore.saveAddedFragments();

        // Now save back stack.
        BackStackState[] backStack = null;
        if (mBackStack != null) {
            int size = mBackStack.size();
            if (size > 0) {
                backStack = new BackStackState[size];
                for (int i = 0; i < size; i++) {
                    backStack[i] = new BackStackState(mBackStack.get(i));
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
        fms.mResultKeys.addAll(mResults.keySet());
        fms.mResults.addAll(mResults.values());
        fms.mLaunchedFragments = new ArrayList<>(mLaunchedFragments);
        return fms;
    }

    @SuppressWarnings("deprecation")
    void restoreAllState(@Nullable Parcelable state, @Nullable FragmentManagerNonConfig nonConfig) {
        if (mHost instanceof ViewModelStoreOwner) {
            throwException(new IllegalStateException("You must use restoreSaveState when your "
                    + "FragmentHostCallback implements ViewModelStoreOwner"));
        }
        mNonConfig.restoreFromSnapshot(nonConfig);
        restoreSaveState(state);
    }

    void restoreSaveState(@Nullable Parcelable state) {
        // If there is no saved state at all, then there's nothing else to do
        if (state == null) return;
        FragmentManagerState fms = (FragmentManagerState) state;
        if (fms.mActive == null) return;

        // Build the full list of active fragments, instantiating them from
        // their saved state.
        mFragmentStore.resetActiveFragments();
        for (FragmentState fs : fms.mActive) {
            if (fs != null) {
                FragmentStateManager fragmentStateManager;
                Fragment retainedFragment = mNonConfig.findRetainedFragmentByWho(fs.mWho);
                if (retainedFragment != null) {
                    if (isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(TAG, "restoreSaveState: re-attaching retained "
                                + retainedFragment);
                    }
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, retainedFragment, fs);
                } else {
                    fragmentStateManager = new FragmentStateManager(mLifecycleCallbacksDispatcher,
                            mFragmentStore, mHost.getContext().getClassLoader(),
                            getFragmentFactory(), fs);
                }
                Fragment f = fragmentStateManager.getFragment();
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

        ArrayList<String> savedResultKeys = fms.mResultKeys;
        if (savedResultKeys != null) {
            for (int i = 0; i < savedResultKeys.size(); i++) {
                mResults.put(savedResultKeys.get(i), fms.mResults.get(i));
            }
        }
        mLaunchedFragments = new ArrayDeque<>(fms.mLaunchedFragments);
    }

    @NonNull
    FragmentHostCallback<?> getHost() {
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
                            LaunchedFragmentInfo requestInfo = mLaunchedFragments.pollFirst();
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
                                Log.w(TAG,
                                        "Intent Sender result delivered for unknown Fragment "
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
            @SuppressLint("UnknownNullness") Intent intent,
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
            @SuppressLint("UnknownNullness") IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {
        if (mStartIntentSenderForResult != null) {
            IntentSenderRequest request =
                    new IntentSenderRequest.Builder(intent).setFillInIntent(fillInIntent)
                            .setFlags(flagsValues, flagsMask).build();
            if (options != null) {
                if (fillInIntent == null) {
                    fillInIntent = new Intent();
                }
                fillInIntent.putExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE, options);
            }
            LaunchedFragmentInfo info = new LaunchedFragmentInfo(f.mWho, requestCode);
            mLaunchedFragments.addLast(info);
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
        dispatchStateChange(Fragment.INITIALIZING);
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
            if (USE_STATE_MANAGER) {
                Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
                for (SpecialEffectsController controller : controllers) {
                    controller.executePendingOperations();
                }
            }
        } finally {
            mExecutingActions = false;
        }
        execPendingActions(true);
    }

    void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    void dispatchConfigurationChanged(@NonNull Configuration newConfig) {
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performConfigurationChanged(newConfig);
            }
        }
    }

    void dispatchLowMemory() {
        for (Fragment f : mFragmentStore.getFragments()) {
            if (f != null) {
                f.performLowMemory();
            }
        }
    }

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
                if (f.performPrepareOptionsMenu(menu)) {
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
        }
        return rev;

    }

    @NonNull
    LayoutInflater.Factory2 getLayoutInflaterFactory() {
        return mLayoutInflaterFactory;
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

    /**
     * A listener for a postponed transaction. This waits until
     * {@link Fragment#startPostponedEnterTransition()} is called or a transaction is started
     * that interacts with this one, based on interactions with the fragment container.
     */
    static class StartEnterTransitionListener
            implements Fragment.OnStartEnterTransitionListener {
        final boolean mIsBack;
        final BackStackRecord mRecord;
        private int mNumPostponed;

        StartEnterTransitionListener(@NonNull BackStackRecord record, boolean isBack) {
            mIsBack = isBack;
            mRecord = record;
        }

        /**
         * Called from {@link Fragment#startPostponedEnterTransition()}, this decreases the
         * number of Fragments that are postponed. This may cause the transaction to schedule
         * to finish running and run transitions and animations.
         */
        @Override
        public void onStartEnterTransition() {
            mNumPostponed--;
            if (mNumPostponed != 0) {
                return;
            }
            mRecord.mManager.scheduleCommit();
        }

        /**
         * Called from {@link Fragment#
         * setOnStartEnterTransitionListener(Fragment.OnStartEnterTransitionListener)}, this
         * increases the number of fragments that are postponed as part of this transaction.
         */
        @Override
        public void startListening() {
            mNumPostponed++;
        }

        /**
         * @return true if there are no more postponed fragments as part of the transaction.
         */
        public boolean isReady() {
            return mNumPostponed == 0;
        }

        /**
         * Completes the transaction and start the animations and transitions. This may skip
         * the transitions if this is called before all fragments have called
         * {@link Fragment#startPostponedEnterTransition()}.
         */
        void completeTransaction() {
            final boolean canceled;
            canceled = mNumPostponed > 0;
            FragmentManager manager = mRecord.mManager;
            for (Fragment fragment : manager.getFragments()) {
                fragment.setOnStartEnterTransitionListener(null);
                if (canceled && fragment.isPostponed()) {
                    fragment.startPostponedEnterTransition();
                }
            }
            mRecord.mManager.completeExecute(mRecord, mIsBack, !canceled, true);
        }

        /**
         * Cancels this transaction instead of completing it. That means that the state isn't
         * changed, so the pop results in no change to the state.
         */
        void cancelTransaction() {
            mRecord.mManager.completeExecute(mRecord, mIsBack, false, false);
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
            if (input.getFillInIntent() != null) {
                Bundle activityOptions =
                        input.getFillInIntent().getBundleExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE);
                int requestCode =
                        input.getFillInIntent().getIntExtra(EXTRA_KEY_REQUEST_CODE, -1);
                if (activityOptions != null) {
                    result.putExtra(EXTRA_KEY_REQUEST_CODE, requestCode);
                    result.putExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE, activityOptions);
                }
            }
            result.putExtra(EXTRA_INTENT_SENDER_REQUEST, input);
            return result;
        }

        @NonNull
        @Override
        public ActivityResult parseResult(int resultCode, @Nullable Intent intent) {
            return new ActivityResult(resultCode, intent);
        }
    }
}
