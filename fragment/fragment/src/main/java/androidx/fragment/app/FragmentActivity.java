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

import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.core.app.ActivityCompat;
import androidx.core.app.MultiWindowModeChangedInfo;
import androidx.core.app.OnMultiWindowModeChangedProvider;
import androidx.core.app.OnPictureInPictureModeChangedProvider;
import androidx.core.app.PictureInPictureModeChangedInfo;
import androidx.core.app.SharedElementCallback;
import androidx.core.content.OnConfigurationChangedProvider;
import androidx.core.content.OnTrimMemoryProvider;
import androidx.core.util.Consumer;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Base class for activities that want to use the support-based
 * {@link Fragment Fragments}.
 *
 * <p>Known limitations:</p>
 * <ul>
 * <li> <p>When using the <code>&lt;fragment></code> tag, this implementation can not
 * use the parent view's ID as the new fragment's ID.  You must explicitly
 * specify an ID (or tag) in the <code>&lt;fragment></code>.</p>
 * </ul>
 */
public class FragmentActivity extends ComponentActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        ActivityCompat.RequestPermissionsRequestCodeValidator {

    static final String LIFECYCLE_TAG = "android:support:lifecycle";

    final FragmentController mFragments = FragmentController.createController(new HostCallbacks());
    /**
     * A {@link Lifecycle} that is exactly nested outside of when the FragmentController
     * has its state changed, providing the proper nesting of Lifecycle callbacks
     * <p>
     * TODO(b/127528777) Drive Fragment Lifecycle with LifecycleObserver
     */
    final LifecycleRegistry mFragmentLifecycleRegistry = new LifecycleRegistry(this);

    boolean mCreated;
    boolean mResumed;
    boolean mStopped = true;

    /**
     * Default constructor for FragmentActivity. All Activities must have a default constructor
     * for API 27 and lower devices or when using the default
     * {@link android.app.AppComponentFactory}.
     */
    public FragmentActivity() {
        super();
        init();
    }

    /**
     * Alternate constructor that can be used to provide a default layout
     * that will be inflated as part of <code>super.onCreate(savedInstanceState)</code>.
     *
     * <p>This should generally be called from your constructor that takes no parameters,
     * as is required for API 27 and lower or when using the default
     * {@link android.app.AppComponentFactory}.
     *
     * @see #FragmentActivity()
     */
    @ContentView
    public FragmentActivity(@LayoutRes int contentLayoutId) {
        super(contentLayoutId);
        init();
    }

    private void init() {
        getSavedStateRegistry().registerSavedStateProvider(LIFECYCLE_TAG, () -> {
            markFragmentsCreated();
            mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            return new Bundle();
        });
        // Ensure that the first OnConfigurationChangedListener
        // marks the FragmentManager's state as not saved
        addOnConfigurationChangedListener(newConfig -> mFragments.noteStateNotSaved());
        // Ensure that the first OnNewIntentListener
        // marks the FragmentManager's state as not saved
        addOnNewIntentListener(newConfig -> mFragments.noteStateNotSaved());
        addOnContextAvailableListener(context -> mFragments.attachHost(null /*parent*/));
    }

    // ------------------------------------------------------------------------
    // HOOKS INTO ACTIVITY
    // ------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    @CallSuper
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mFragments.noteStateNotSaved();
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Reverses the Activity Scene entry Transition and triggers the calling Activity
     * to reverse its exit Transition. When the exit Transition completes,
     * {@link #finish()} is called. If no entry Transition was used, finish() is called
     * immediately and the Activity exit Transition is run.
     *
     * <p>On Android 4.4 or lower, this method only finishes the Activity with no
     * special exit transition.</p>
     */
    public void supportFinishAfterTransition() {
        ActivityCompat.finishAfterTransition(this);
    }

    /**
     * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
     * android.view.View, String)} was used to start an Activity, <var>callback</var>
     * will be called to handle shared elements on the <i>launched</i> Activity. This requires
     * {@link Window#FEATURE_CONTENT_TRANSITIONS}.
     *
     * @param callback Used to manipulate shared element transitions on the launched Activity.
     */
    public void setEnterSharedElementCallback(@Nullable SharedElementCallback callback) {
        ActivityCompat.setEnterSharedElementCallback(this, callback);
    }

    /**
     * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
     * android.view.View, String)} was used to start an Activity, <var>listener</var>
     * will be called to handle shared elements on the <i>launching</i> Activity. Most
     * calls will only come when returning from the started Activity.
     * This requires {@link Window#FEATURE_CONTENT_TRANSITIONS}.
     *
     * @param listener Used to manipulate shared element transitions on the launching Activity.
     */
    public void setExitSharedElementCallback(@Nullable SharedElementCallback listener) {
        ActivityCompat.setExitSharedElementCallback(this, listener);
    }

    /**
     * Support library version of {@link android.app.Activity#postponeEnterTransition()} that works
     * only on API 21 and later.
     */
    public void supportPostponeEnterTransition() {
        ActivityCompat.postponeEnterTransition(this);
    }

    /**
     * Support library version of {@link android.app.Activity#startPostponedEnterTransition()}
     * that only works with API 21 and later.
     */
    public void supportStartPostponedEnterTransition() {
        ActivityCompat.startPostponedEnterTransition(this);
    }

    /**
     * {@inheritDoc}
     *
     * Perform initialization of all fragments.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragments.dispatchCreate();
    }

    @Override
    public @Nullable View onCreateView(@Nullable View parent, @NonNull String name,
            @NonNull Context context, @NonNull AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(parent, name, context, attrs);
        if (v == null) {
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }

    @Override
    public @Nullable View onCreateView(@NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(null, name, context, attrs);
        if (v == null) {
            return super.onCreateView(name, context, attrs);
        }
        return v;
    }

    final @Nullable View dispatchFragmentsOnCreateView(@Nullable View parent, @NonNull String name,
            @NonNull Context context, @NonNull AttributeSet attrs) {
        return mFragments.onCreateView(parent, name, context, attrs);
    }

    /**
     * {@inheritDoc}
     *
     * Destroy all fragments.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFragments.dispatchDestroy();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }

        if (featureId == Window.FEATURE_CONTEXT_MENU) {
            return mFragments.dispatchContextItemSelected(item);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        mFragments.dispatchPause();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    }

    /**
     * {@inheritDoc}
     *
     * Hook in to note that fragment state is no longer saved.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onStateNotSaved() {
        mFragments.noteStateNotSaved();
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.
     */
    @Override
    protected void onResume() {
        mFragments.noteStateNotSaved();
        super.onResume();
        mResumed = true;
        mFragments.execPendingActions();
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onResume() to fragments.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        onResumeFragments();
    }

    /**
     * This is the fragment-orientated version of {@link #onResume()} that you
     * can override to perform operations in the Activity at the same point
     * where its fragments are resumed.  Be sure to always call through to
     * the super-class.
     */
    protected void onResumeFragments() {
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mFragments.dispatchResume();
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onStart() to all fragments.
     */
    @Override
    protected void onStart() {
        mFragments.noteStateNotSaved();
        super.onStart();

        mStopped = false;

        if (!mCreated) {
            mCreated = true;
            mFragments.dispatchActivityCreated();
        }

        mFragments.execPendingActions();

        // NOTE: HC onStart goes here.

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragments.dispatchStart();
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onStop() to all fragments.
     */
    @Override
    protected void onStop() {
        super.onStop();

        mStopped = true;
        markFragmentsCreated();

        mFragments.dispatchStop();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }

    // ------------------------------------------------------------------------
    // NEW METHODS
    // ------------------------------------------------------------------------

    /**
     * Support library version of {@link Activity#invalidateOptionsMenu}.
     *
     * <p>Invalidate the activity's options menu. This will cause relevant presentations
     * of the menu to fully update via calls to onCreateOptionsMenu and
     * onPrepareOptionsMenu the next time the menu is requested.
     *
     * @deprecated Call {@link Activity#invalidateOptionsMenu} directly.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void supportInvalidateOptionsMenu() {
        invalidateMenu();
    }

    /**
     * Print the Activity's state into the given stream.  This gets invoked if
     * you run "adb shell dumpsys activity <activity_component_name>".
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, String @Nullable [] args) {
        super.dump(prefix, fd, writer, args);

        if (!shouldDumpInternalState(args)) {
            return;
        }

        writer.print(prefix); writer.print("Local FragmentActivity ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(" State:");
        String innerPrefix = prefix + "  ";
        writer.print(innerPrefix); writer.print("mCreated=");
                writer.print(mCreated); writer.print(" mResumed=");
                writer.print(mResumed); writer.print(" mStopped=");
                writer.print(mStopped);

        if (getApplication() != null) {
            LoaderManager.getInstance(this).dump(innerPrefix, fd, writer, args);
        }
        mFragments.getSupportFragmentManager().dump(prefix, fd, writer, args);
    }

    // ------------------------------------------------------------------------
    // FRAGMENT SUPPORT
    // ------------------------------------------------------------------------

    /**
     * Called when a fragment is attached to the activity.
     *
     * <p>This is called after the attached fragment's <code>onAttach</code> and before
     * the attached fragment's <code>onCreate</code> if the fragment has not yet had a previous
     * call to <code>onCreate</code>.</p>
     *
     * @deprecated The responsibility for listening for fragments being attached has been moved
     * to FragmentManager. You can add a listener to
     * {@link #getSupportFragmentManager() this Activity's FragmentManager} by calling
     * {@link FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}
     * in your constructor to get callbacks when a fragment is attached directly to
     * the activity's FragmentManager.
     */
    @SuppressWarnings({"unused", "DeprecatedIsStillUsed"})
    @Deprecated
    @MainThread
    public void onAttachFragment(@NonNull Fragment fragment) {
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this activity.
     */
    public @NonNull FragmentManager getSupportFragmentManager() {
        return mFragments.getSupportFragmentManager();
    }

    /**
     * @deprecated Use
     * {@link LoaderManager#getInstance(LifecycleOwner) LoaderManager.getInstance(this)}.
     */
    @Deprecated
    public @NonNull LoaderManager getSupportLoaderManager() {
        return LoaderManager.getInstance(this);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated there are no longer any restrictions on permissions requestCodes.
     */
    @Override
    @Deprecated
    public final void validateRequestPermissionsRequestCode(int requestCode) { }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, String @NonNull [] permissions,
            int @NonNull [] grantResults) {
        mFragments.noteStateNotSaved();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Called by Fragment.startActivityForResult() to implement its behavior.
     *
     * @param fragment the Fragment to start the activity from.
     * @param intent The intent to start.
     * @param requestCode The request code to be returned in
     * {@link Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
     *                    between 0 and 65535 to be considered valid. If given requestCode is
     *                    greater than 65535, an IllegalArgumentException would be thrown.
     */
    public void startActivityFromFragment(@NonNull Fragment fragment,
            @NonNull Intent intent, int requestCode) {
        startActivityFromFragment(fragment, intent, requestCode, null);
    }

    /**
     * Called by Fragment.startActivityForResult() to implement its behavior.
     *
     * @param fragment the Fragment to start the activity from.
     * @param intent The intent to start.
     * @param requestCode The request code to be returned in
     * {@link Fragment#onActivityResult(int, int, Intent)} when the activity exits. Must be
     *                    between 0 and 65535 to be considered valid. If given requestCode is
     *                    greater than 65535, an IllegalArgumentException would be thrown.
     * @param options Additional options for how the Activity should be started. See
     * {@link Context#startActivity(Intent, Bundle)} for more details. This value may be null.
     */
    @SuppressWarnings("deprecation")
    public void startActivityFromFragment(@NonNull Fragment fragment,
            @NonNull Intent intent, int requestCode,
            @Nullable Bundle options) {
        // request code will be -1 if called from fragment.startActivity
        if (requestCode == -1) {
            ActivityCompat.startActivityForResult(this, intent, -1, options);
            return;
        }
        // If for some reason this method is being called directly with a requestCode that is not
        // -1, redirect it to the fragment.startActivityForResult method
        fragment.startActivityForResult(intent, requestCode, options);
    }

    /**
     * Called by Fragment.startIntentSenderForResult() to implement its behavior.
     *
     * @param fragment the Fragment to start the intent sender from.
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
     * @throws IntentSender.SendIntentException if the call fails to execute.
     *
     * @deprecated Fragments should use
     * {@link Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with the {@link StartIntentSenderForResult} contract. This method will still be called when
     * Fragments call the deprecated <code>startIntentSenderForResult()</code> method.
     */
    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void startIntentSenderFromFragment(@NonNull Fragment fragment,
            @NonNull IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            @Nullable Bundle options) throws IntentSender.SendIntentException {
        if (requestCode == -1) {
            ActivityCompat.startIntentSenderForResult(this, intent, requestCode, fillInIntent,
                    flagsMask, flagsValues, extraFlags, options);
            return;
        }
        fragment.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask,
                flagsValues, extraFlags, options);
    }

    class HostCallbacks extends FragmentHostCallback<FragmentActivity> implements
            OnConfigurationChangedProvider,
            OnTrimMemoryProvider,
            OnMultiWindowModeChangedProvider,
            OnPictureInPictureModeChangedProvider,
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner,
            ActivityResultRegistryOwner,
            SavedStateRegistryOwner,
            FragmentOnAttachListener,
            MenuHost {

        public HostCallbacks() {
            super(FragmentActivity.this /*fragmentActivity*/);
        }

        @Override
        public @NonNull Lifecycle getLifecycle() {
            // Instead of directly using the Activity's Lifecycle, we
            // use a LifecycleRegistry that is nested exactly outside of
            // when Fragments get their lifecycle changed
            // TODO(b/127528777) Drive Fragment Lifecycle with LifecycleObserver
            return mFragmentLifecycleRegistry;
        }

        @Override
        public @NonNull ViewModelStore getViewModelStore() {
            return FragmentActivity.this.getViewModelStore();
        }

        @Override
        public @NonNull OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return FragmentActivity.this.getOnBackPressedDispatcher();
        }

        @Override
        public void onDump(@NonNull String prefix, @Nullable FileDescriptor fd,
                @NonNull PrintWriter writer, String @Nullable [] args) {
            FragmentActivity.this.dump(prefix, fd, writer, args);
        }

        @Override
        public boolean onShouldSaveFragmentState(@NonNull Fragment fragment) {
            return !isFinishing();
        }

        @Override
        public @NonNull LayoutInflater onGetLayoutInflater() {
            return FragmentActivity.this.getLayoutInflater().cloneInContext(FragmentActivity.this);
        }

        @Override
        public FragmentActivity onGetHost() {
            return FragmentActivity.this;
        }

        @Override
        public void onSupportInvalidateOptionsMenu() {
            invalidateMenu();
        }

        @Override
        public boolean onShouldShowRequestPermissionRationale(@NonNull String permission) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    FragmentActivity.this, permission);
        }

        @Override
        public boolean onHasWindowAnimations() {
            return getWindow() != null;
        }

        @Override
        public int onGetWindowAnimations() {
            final Window w = getWindow();
            return (w == null) ? 0 : w.getAttributes().windowAnimations;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onAttachFragment(@NonNull FragmentManager fragmentManager,
                @NonNull Fragment fragment) {
            FragmentActivity.this.onAttachFragment(fragment);
        }

        @Override
        public @Nullable View onFindViewById(int id) {
            return FragmentActivity.this.findViewById(id);
        }

        @Override
        public boolean onHasView() {
            final Window w = getWindow();
            return (w != null && w.peekDecorView() != null);
        }

        @Override
        public @NonNull ActivityResultRegistry getActivityResultRegistry() {
            return FragmentActivity.this.getActivityResultRegistry();
        }

        @Override
        public @NonNull SavedStateRegistry getSavedStateRegistry() {
            return FragmentActivity.this.getSavedStateRegistry();
        }

        @Override
        public void addOnConfigurationChangedListener(
                @NonNull Consumer<Configuration> listener
        ) {
            FragmentActivity.this.addOnConfigurationChangedListener(listener);
        }

        @Override
        public void removeOnConfigurationChangedListener(
                @NonNull Consumer<Configuration> listener
        ) {
            FragmentActivity.this.removeOnConfigurationChangedListener(listener);
        }

        @Override
        public void addOnTrimMemoryListener(@NonNull Consumer<Integer> listener) {
            FragmentActivity.this.addOnTrimMemoryListener(listener);
        }

        @Override
        public void removeOnTrimMemoryListener(@NonNull Consumer<Integer> listener) {
            FragmentActivity.this.removeOnTrimMemoryListener(listener);
        }

        @Override
        public void addOnMultiWindowModeChangedListener(
                @NonNull Consumer<MultiWindowModeChangedInfo> listener) {
            FragmentActivity.this.addOnMultiWindowModeChangedListener(listener);
        }

        @Override
        public void removeOnMultiWindowModeChangedListener(
                @NonNull Consumer<MultiWindowModeChangedInfo> listener) {
            FragmentActivity.this.removeOnMultiWindowModeChangedListener(listener);
        }

        @Override
        public void addOnPictureInPictureModeChangedListener(
                @NonNull Consumer<PictureInPictureModeChangedInfo> listener) {
            FragmentActivity.this.addOnPictureInPictureModeChangedListener(listener);
        }

        @Override
        public void removeOnPictureInPictureModeChangedListener(
                @NonNull Consumer<PictureInPictureModeChangedInfo> listener) {
            FragmentActivity.this.removeOnPictureInPictureModeChangedListener(listener);
        }

        @Override
        public void addMenuProvider(@NonNull MenuProvider provider) {
            FragmentActivity.this.addMenuProvider(provider);
        }

        @Override
        public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner) {
            FragmentActivity.this.addMenuProvider(provider, owner);
        }

        @Override
        public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
                Lifecycle.@NonNull State state) {
            FragmentActivity.this.addMenuProvider(provider, owner, state);
        }

        @Override
        public void removeMenuProvider(@NonNull MenuProvider provider) {
            FragmentActivity.this.removeMenuProvider(provider);
        }

        @Override
        public void invalidateMenu() {
            FragmentActivity.this.invalidateMenu();
        }
    }

    void markFragmentsCreated() {
        boolean reiterate;
        do {
            reiterate = markState(getSupportFragmentManager(), Lifecycle.State.CREATED);
        } while (reiterate);
    }

    private static boolean markState(FragmentManager manager, Lifecycle.State state) {
        boolean hadNotMarked = false;
        Collection<Fragment> fragments = manager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            if (fragment.getHost() != null) {
                FragmentManager childFragmentManager = fragment.getChildFragmentManager();
                hadNotMarked |= markState(childFragmentManager, state);
            }
            if (fragment.mViewLifecycleOwner != null && fragment.mViewLifecycleOwner
                    .getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                fragment.mViewLifecycleOwner.setCurrentState(state);
                hadNotMarked = true;
            }
            if (fragment.mLifecycleRegistry.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                fragment.mLifecycleRegistry.setCurrentState(state);
                hadNotMarked = true;
            }
        }
        return hadNotMarked;
    }
}
