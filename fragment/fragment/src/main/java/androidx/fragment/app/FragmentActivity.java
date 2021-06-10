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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityCompat;
import androidx.core.app.SharedElementCallback;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.loader.app.LoaderManager;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

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
     * <p><strong>Note:</strong> If you override this method you must call
     * <code>super.onMultiWindowModeChanged</code> to correctly dispatch the event
     * to support fragments attached to this activity.</p>
     *
     * @param isInMultiWindowMode True if the activity is in multi-window mode.
     */
    @SuppressWarnings("deprecation")
    @Override
    @CallSuper
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        mFragments.dispatchMultiWindowModeChanged(isInMultiWindowMode);
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Note:</strong> If you override this method you must call
     * <code>super.onPictureInPictureModeChanged</code> to correctly dispatch the event
     * to support fragments attached to this activity.</p>
     *
     * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
     */
    @SuppressWarnings("deprecation")
    @Override
    @CallSuper
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        mFragments.dispatchPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch configuration change to all fragments.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mFragments.noteStateNotSaved();
        mFragments.dispatchConfigurationChanged(newConfig);
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

    /**
     * {@inheritDoc}
     *
     * Dispatch to Fragment.onCreateOptionsMenu().
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            boolean show = super.onCreatePanelMenu(featureId, menu);
            show |= mFragments.dispatchCreateOptionsMenu(menu, getMenuInflater());
            return show;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    @Nullable
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(parent, name, context, attrs);
        if (v == null) {
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(null, name, context, attrs);
        if (v == null) {
            return super.onCreateView(name, context, attrs);
        }
        return v;
    }

    @Nullable
    final View dispatchFragmentsOnCreateView(@Nullable View parent, @NonNull String name,
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

    /**
     * {@inheritDoc}
     *
     * Dispatch onLowMemory() to all fragments.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mFragments.dispatchLowMemory();
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch context and options menu to fragments.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }

        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                return mFragments.dispatchOptionsItemSelected(item);

            case Window.FEATURE_CONTEXT_MENU:
                return mFragments.dispatchContextItemSelected(item);

            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Call onOptionsMenuClosed() on fragments.
     */
    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            mFragments.dispatchOptionsMenuClosed(menu);
        }
        super.onPanelClosed(featureId, menu);
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
     * Handle onNewIntent() to inform the fragment manager that the
     * state is not saved.  If you are handling new intents and may be
     * making changes to the fragment state, you want to be sure to call
     * through to the super-class here first.  Otherwise, if your state
     * is saved but the activity is not stopped, you could get an
     * onNewIntent() call which happens before onResume() and trying to
     * perform fragment operations at that point will throw IllegalStateException
     * because the fragment manager thinks the state is still saved.
     */
    @Override
    @CallSuper
    protected void onNewIntent(@SuppressLint("UnknownNullness") Intent intent) {
        super.onNewIntent(intent);
        mFragments.noteStateNotSaved();
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
        super.onResume();
        mResumed = true;
        mFragments.noteStateNotSaved();
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
     * Dispatch onPrepareOptionsMenu() to fragments.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            boolean goforit = onPrepareOptionsPanel(view, menu);
            goforit |= mFragments.dispatchPrepareOptionsMenu(menu);
            return goforit;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    /**
     * @hide
     * @deprecated Override {@link #onPreparePanel(int, View, Menu)}.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Deprecated
    protected boolean onPrepareOptionsPanel(@Nullable View view, @NonNull Menu menu) {
        return super.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, view, menu);
    }

    /**
     * {@inheritDoc}
     *
     * Dispatch onStart() to all fragments.
     */
    @Override
    protected void onStart() {
        super.onStart();

        mStopped = false;

        if (!mCreated) {
            mCreated = true;
            mFragments.dispatchActivityCreated();
        }

        mFragments.noteStateNotSaved();
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
        invalidateOptionsMenu();
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
            @NonNull PrintWriter writer, @Nullable String[] args) {
        super.dump(prefix, fd, writer, args);
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
    @NonNull
    public FragmentManager getSupportFragmentManager() {
        return mFragments.getSupportFragmentManager();
    }

    /**
     * @deprecated Use
     * {@link LoaderManager#getInstance(LifecycleOwner) LoaderManager.getInstance(this)}.
     */
    @Deprecated
    @NonNull
    public LoaderManager getSupportLoaderManager() {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
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
            @SuppressLint("UnknownNullness") Intent intent, int requestCode) {
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
            @SuppressLint("UnknownNullness") Intent intent, int requestCode,
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
            @SuppressLint("UnknownNullness") IntentSender intent, int requestCode,
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
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner,
            ActivityResultRegistryOwner,
            SavedStateRegistryOwner,
            FragmentOnAttachListener {
        public HostCallbacks() {
            super(FragmentActivity.this /*fragmentActivity*/);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            // Instead of directly using the Activity's Lifecycle, we
            // use a LifecycleRegistry that is nested exactly outside of
            // when Fragments get their lifecycle changed
            // TODO(b/127528777) Drive Fragment Lifecycle with LifecycleObserver
            return mFragmentLifecycleRegistry;
        }

        @NonNull
        @Override
        public ViewModelStore getViewModelStore() {
            return FragmentActivity.this.getViewModelStore();
        }

        @NonNull
        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return FragmentActivity.this.getOnBackPressedDispatcher();
        }

        @Override
        public void onDump(@NonNull String prefix, @Nullable FileDescriptor fd,
                @NonNull PrintWriter writer, @Nullable String[] args) {
            FragmentActivity.this.dump(prefix, fd, writer, args);
        }

        @Override
        public boolean onShouldSaveFragmentState(@NonNull Fragment fragment) {
            return !isFinishing();
        }

        @Override
        @NonNull
        public LayoutInflater onGetLayoutInflater() {
            return FragmentActivity.this.getLayoutInflater().cloneInContext(FragmentActivity.this);
        }

        @Override
        public FragmentActivity onGetHost() {
            return FragmentActivity.this;
        }

        @Override
        public void onSupportInvalidateOptionsMenu() {
            FragmentActivity.this.supportInvalidateOptionsMenu();
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

        @Nullable
        @Override
        public View onFindViewById(int id) {
            return FragmentActivity.this.findViewById(id);
        }

        @Override
        public boolean onHasView() {
            final Window w = getWindow();
            return (w != null && w.peekDecorView() != null);
        }

        @NonNull
        @Override
        public ActivityResultRegistry getActivityResultRegistry() {
            return FragmentActivity.this.getActivityResultRegistry();
        }

        @NonNull
        @Override
        public SavedStateRegistry getSavedStateRegistry() {
            return FragmentActivity.this.getSavedStateRegistry();
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
