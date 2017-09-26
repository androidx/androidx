/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.util.SparseArrayCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Base class for activities that want to use the support-based
 * {@link android.support.v4.app.Fragment} and
 * {@link android.support.v4.content.Loader} APIs.
 *
 * <p>When using this class as opposed to new platform's built-in fragment
 * and loader support, you must use the {@link #getSupportFragmentManager()}
 * and {@link #getSupportLoaderManager()} methods respectively to access
 * those features.
 *
 * <p>Known limitations:</p>
 * <ul>
 * <li> <p>When using the <code>&lt;fragment></code> tag, this implementation can not
 * use the parent view's ID as the new fragment's ID.  You must explicitly
 * specify an ID (or tag) in the <code>&lt;fragment></code>.</p>
 * </ul>
 */
public class FragmentActivity extends BaseFragmentActivityApi16 implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        ActivityCompat.RequestPermissionsRequestCodeValidator {
    private static final String TAG = "FragmentActivity";

    static final String FRAGMENTS_TAG = "android:support:fragments";
    static final String NEXT_CANDIDATE_REQUEST_INDEX_TAG = "android:support:next_request_index";
    static final String ALLOCATED_REQUEST_INDICIES_TAG = "android:support:request_indicies";
    static final String REQUEST_FRAGMENT_WHO_TAG = "android:support:request_fragment_who";
    static final int MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS = 0xffff - 1;

    static final int MSG_REALLY_STOPPED = 1;
    static final int MSG_RESUME_PENDING = 2;

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REALLY_STOPPED:
                    if (mStopped) {
                        doReallyStop(false);
                    }
                    break;
                case MSG_RESUME_PENDING:
                    onResumeFragments();
                    mFragments.execPendingActions();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    };
    final FragmentController mFragments = FragmentController.createController(new HostCallbacks());

    boolean mCreated;
    boolean mResumed;
    boolean mStopped = true;
    boolean mReallyStopped = true;
    boolean mRetaining;

    boolean mRequestedPermissionsFromFragment;

    // A hint for the next candidate request index. Request indicies are ints between 0 and 2^16-1
    // which are encoded into the upper 16 bits of the requestCode for
    // Fragment.startActivityForResult(...) calls. This allows us to dispatch onActivityResult(...)
    // to the appropriate Fragment. Request indicies are allocated by allocateRequestIndex(...).
    int mNextCandidateRequestIndex;
    // A map from request index to Fragment "who" (i.e. a Fragment's unique identifier). Used to
    // keep track of the originating Fragment for Fragment.startActivityForResult(...) calls, so we
    // can dispatch the onActivityResult(...) to the appropriate Fragment. Will only contain entries
    // for startActivityForResult calls where a result has not yet been delivered.
    SparseArrayCompat<String> mPendingFragmentActivityResults;

    static final class NonConfigurationInstances {
        Object custom;
        FragmentManagerNonConfig fragments;
        SimpleArrayMap<String, LoaderManager> loaders;
    }

    // ------------------------------------------------------------------------
    // HOOKS INTO ACTIVITY
    // ------------------------------------------------------------------------

    /**
     * Dispatch incoming result to the correct fragment.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mFragments.noteStateNotSaved();
        int requestIndex = requestCode>>16;
        if (requestIndex != 0) {
            requestIndex--;

            String who = mPendingFragmentActivityResults.get(requestIndex);
            mPendingFragmentActivityResults.remove(requestIndex);
            if (who == null) {
                Log.w(TAG, "Activity result delivered for unknown Fragment.");
                return;
            }
            Fragment targetFragment = mFragments.findFragmentByWho(who);
            if (targetFragment == null) {
                Log.w(TAG, "Activity result no fragment exists for who: " + who);
            } else {
                targetFragment.onActivityResult(requestCode & 0xffff, resultCode, data);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = mFragments.getSupportFragmentManager();
        final boolean isStateSaved = fragmentManager.isStateSaved();
        if (isStateSaved && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            // Older versions will throw an exception from the framework
            // FragmentManager.popBackStackImmediate(), so we'll just
            // return here. The Activity is likely already on its way out
            // since the fragmentManager has already been saved.
            return;
        }
        if (isStateSaved || !fragmentManager.popBackStackImmediate()) {
            super.onBackPressed();
        }
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
    public void setEnterSharedElementCallback(SharedElementCallback callback) {
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
    public void setExitSharedElementCallback(SharedElementCallback listener) {
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
    @Override
    @CallSuper
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        mFragments.dispatchPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    /**
     * Dispatch configuration change to all fragments.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mFragments.dispatchConfigurationChanged(newConfig);
    }

    /**
     * Returns the Lifecycle of the provider.
     *
     * @return The lifecycle of the provider.
     */
    @Override
    public Lifecycle getLifecycle() {
        return super.getLifecycle();
    }

    /**
     * Perform initialization of all fragments and loaders.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mFragments.attachHost(null /*parent*/);

        super.onCreate(savedInstanceState);

        NonConfigurationInstances nc =
                (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            mFragments.restoreLoaderNonConfig(nc.loaders);
        }
        if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable(FRAGMENTS_TAG);
            mFragments.restoreAllState(p, nc != null ? nc.fragments : null);

            // Check if there are any pending onActivityResult calls to descendent Fragments.
            if (savedInstanceState.containsKey(NEXT_CANDIDATE_REQUEST_INDEX_TAG)) {
                mNextCandidateRequestIndex =
                        savedInstanceState.getInt(NEXT_CANDIDATE_REQUEST_INDEX_TAG);
                int[] requestCodes = savedInstanceState.getIntArray(ALLOCATED_REQUEST_INDICIES_TAG);
                String[] fragmentWhos = savedInstanceState.getStringArray(REQUEST_FRAGMENT_WHO_TAG);
                if (requestCodes == null || fragmentWhos == null ||
                            requestCodes.length != fragmentWhos.length) {
                    Log.w(TAG, "Invalid requestCode mapping in savedInstanceState.");
                } else {
                    mPendingFragmentActivityResults = new SparseArrayCompat<>(requestCodes.length);
                    for (int i = 0; i < requestCodes.length; i++) {
                        mPendingFragmentActivityResults.put(requestCodes[i], fragmentWhos[i]);
                    }
                }
            }
        }

        if (mPendingFragmentActivityResults == null) {
            mPendingFragmentActivityResults = new SparseArrayCompat<>();
            mNextCandidateRequestIndex = 0;
        }

        mFragments.dispatchCreate();
    }

    /**
     * Dispatch to Fragment.onCreateOptionsMenu().
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            boolean show = super.onCreatePanelMenu(featureId, menu);
            show |= mFragments.dispatchCreateOptionsMenu(menu, getMenuInflater());
            return show;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    final View dispatchFragmentsOnCreateView(View parent, String name, Context context,
            AttributeSet attrs) {
        return mFragments.onCreateView(parent, name, context, attrs);
    }

    /**
     * Destroy all fragments and loaders.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        doReallyStop(false);

        mFragments.dispatchDestroy();
        mFragments.doLoaderDestroy();
    }

    /**
     * Dispatch onLowMemory() to all fragments.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mFragments.dispatchLowMemory();
    }

    /**
     * Dispatch context and options menu to fragments.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
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
     * Call onOptionsMenuClosed() on fragments.
     */
    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        switch (featureId) {
            case Window.FEATURE_OPTIONS_PANEL:
                mFragments.dispatchOptionsMenuClosed(menu);
                break;
        }
        super.onPanelClosed(featureId, menu);
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        if (mHandler.hasMessages(MSG_RESUME_PENDING)) {
            mHandler.removeMessages(MSG_RESUME_PENDING);
            onResumeFragments();
        }
        mFragments.dispatchPause();
    }

    /**
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mFragments.noteStateNotSaved();
    }

    /**
     * Hook in to note that fragment state is no longer saved.
     */
    @Override
    public void onStateNotSaved() {
        mFragments.noteStateNotSaved();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(MSG_RESUME_PENDING);
        mResumed = true;
        mFragments.execPendingActions();
    }

    /**
     * Dispatch onResume() to fragments.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        mHandler.removeMessages(MSG_RESUME_PENDING);
        onResumeFragments();
        mFragments.execPendingActions();
    }

    /**
     * This is the fragment-orientated version of {@link #onResume()} that you
     * can override to perform operations in the Activity at the same point
     * where its fragments are resumed.  Be sure to always call through to
     * the super-class.
     */
    protected void onResumeFragments() {
        mFragments.dispatchResume();
    }

    /**
     * Dispatch onPrepareOptionsMenu() to fragments.
     */
    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
            boolean goforit = onPrepareOptionsPanel(view, menu);
            goforit |= mFragments.dispatchPrepareOptionsMenu(menu);
            return goforit;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        return super.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, view, menu);
    }

    /**
     * Retain all appropriate fragment and loader state.  You can NOT
     * override this yourself!  Use {@link #onRetainCustomNonConfigurationInstance()}
     * if you want to retain your own state.
     */
    @Override
    public final Object onRetainNonConfigurationInstance() {
        if (mStopped) {
            doReallyStop(true);
        }

        Object custom = onRetainCustomNonConfigurationInstance();

        FragmentManagerNonConfig fragments = mFragments.retainNestedNonConfig();
        SimpleArrayMap<String, LoaderManager> loaders = mFragments.retainLoaderNonConfig();

        if (fragments == null && loaders == null && custom == null) {
            return null;
        }

        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.custom = custom;
        nci.fragments = fragments;
        nci.loaders = loaders;
        return nci;
    }

    /**
     * Save all appropriate fragment state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        markState(getSupportFragmentManager(), Lifecycle.State.CREATED);
        Parcelable p = mFragments.saveAllState();
        if (p != null) {
            outState.putParcelable(FRAGMENTS_TAG, p);
        }
        if (mPendingFragmentActivityResults.size() > 0) {
            outState.putInt(NEXT_CANDIDATE_REQUEST_INDEX_TAG, mNextCandidateRequestIndex);

            int[] requestCodes = new int[mPendingFragmentActivityResults.size()];
            String[] fragmentWhos = new String[mPendingFragmentActivityResults.size()];
            for (int i = 0; i < mPendingFragmentActivityResults.size(); i++) {
                requestCodes[i] = mPendingFragmentActivityResults.keyAt(i);
                fragmentWhos[i] = mPendingFragmentActivityResults.valueAt(i);
            }
            outState.putIntArray(ALLOCATED_REQUEST_INDICIES_TAG, requestCodes);
            outState.putStringArray(REQUEST_FRAGMENT_WHO_TAG, fragmentWhos);
        }
    }

    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     */
    @Override
    protected void onStart() {
        super.onStart();

        mStopped = false;
        mReallyStopped = false;
        mHandler.removeMessages(MSG_REALLY_STOPPED);

        if (!mCreated) {
            mCreated = true;
            mFragments.dispatchActivityCreated();
        }

        mFragments.noteStateNotSaved();
        mFragments.execPendingActions();

        mFragments.doLoaderStart();

        // NOTE: HC onStart goes here.

        mFragments.dispatchStart();
        mFragments.reportLoaderStart();
    }

    /**
     * Dispatch onStop() to all fragments.  Ensure all loaders are stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();

        mStopped = true;
        markState(getSupportFragmentManager(), Lifecycle.State.CREATED);
        mHandler.sendEmptyMessage(MSG_REALLY_STOPPED);

        mFragments.dispatchStop();
    }

    // ------------------------------------------------------------------------
    // NEW METHODS
    // ------------------------------------------------------------------------

    /**
     * Use this instead of {@link #onRetainNonConfigurationInstance()}.
     * Retrieve later with {@link #getLastCustomNonConfigurationInstance()}.
     */
    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    /**
     * Return the value previously returned from
     * {@link #onRetainCustomNonConfigurationInstance()}.
     */
    @SuppressWarnings("deprecation")
    public Object getLastCustomNonConfigurationInstance() {
        NonConfigurationInstances nc = (NonConfigurationInstances)
                getLastNonConfigurationInstance();
        return nc != null ? nc.custom : null;
    }

    /**
     * Support library version of {@link Activity#invalidateOptionsMenu}.
     *
     * <p>Invalidate the activity's options menu. This will cause relevant presentations
     * of the menu to fully update via calls to onCreateOptionsMenu and
     * onPrepareOptionsMenu the next time the menu is requested.
     *
     * @deprecated Call {@link Activity#invalidateOptionsMenu} directly.
     */
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
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("Local FragmentActivity ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(" State:");
        String innerPrefix = prefix + "  ";
        writer.print(innerPrefix); writer.print("mCreated=");
                writer.print(mCreated); writer.print("mResumed=");
                writer.print(mResumed); writer.print(" mStopped=");
                writer.print(mStopped); writer.print(" mReallyStopped=");
                writer.println(mReallyStopped);
        mFragments.dumpLoaders(innerPrefix, fd, writer, args);
        mFragments.getSupportFragmentManager().dump(prefix, fd, writer, args);
    }

    void doReallyStop(boolean retaining) {
        if (!mReallyStopped) {
            mReallyStopped = true;
            mRetaining = retaining;
            mHandler.removeMessages(MSG_REALLY_STOPPED);
            onReallyStop();
        } else if (retaining) {
            // We're already really stopped, but we've been asked to retain.
            // Our fragments are taken care of but we need to mark the loaders for retention.
            // In order to do this correctly we need to restart the loaders first before
            // handing them off to the next activity.
            mFragments.doLoaderStart();
            mFragments.doLoaderStop(true);
        }
    }

    /**
     * Pre-HC, we didn't have a way to determine whether an activity was
     * being stopped for a config change or not until we saw
     * onRetainNonConfigurationInstance() called after onStop().  However
     * we need to know this, to know whether to retain fragments.  This will
     * tell us what we need to know.
     */
    void onReallyStop() {
        mFragments.doLoaderStop(mRetaining);

        mFragments.dispatchReallyStop();
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
     */
    @SuppressWarnings("unused")
    public void onAttachFragment(Fragment fragment) {
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this activity.
     */
    public FragmentManager getSupportFragmentManager() {
        return mFragments.getSupportFragmentManager();
    }

    public LoaderManager getSupportLoaderManager() {
        return mFragments.getSupportLoaderManager();
    }

    /**
     * Modifies the standard behavior to allow results to be delivered to fragments.
     * This imposes a restriction that requestCode be <= 0xffff.
     */
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // If this was started from a Fragment we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the Fragment's index.
        if (!mStartedActivityFromFragment) {
            if (requestCode != -1) {
                checkForValidRequestCode(requestCode);
            }
        }
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public final void validateRequestPermissionsRequestCode(int requestCode) {
        // We use 16 bits of the request code to encode the fragment id when
        // requesting permissions from a fragment. Hence, requestPermissions()
        // should validate the code against that but we cannot override it as
        // we can not then call super and also the ActivityCompat would call
        // back to this override. To handle this we use dependency inversion
        // where we are the validator of request codes when requesting
        // permissions in ActivityCompat.
        if (!mRequestedPermissionsFromFragment
                && requestCode != -1) {
            checkForValidRequestCode(requestCode);
        }
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
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        int index = (requestCode >> 16) & 0xffff;
        if (index != 0) {
            index--;

            String who = mPendingFragmentActivityResults.get(index);
            mPendingFragmentActivityResults.remove(index);
            if (who == null) {
                Log.w(TAG, "Activity result delivered for unknown Fragment.");
                return;
            }
            Fragment frag = mFragments.findFragmentByWho(who);
            if (frag == null) {
                Log.w(TAG, "Activity result no fragment exists for who: " + who);
            } else {
                frag.onRequestPermissionsResult(requestCode & 0xffff, permissions, grantResults);
            }
        }
    }

    /**
     * Called by Fragment.startActivityForResult() to implement its behavior.
     */
    public void startActivityFromFragment(Fragment fragment, Intent intent,
            int requestCode) {
        startActivityFromFragment(fragment, intent, requestCode, null);
    }

    /**
     * Called by Fragment.startActivityForResult() to implement its behavior.
     */
    public void startActivityFromFragment(Fragment fragment, Intent intent,
            int requestCode, @Nullable Bundle options) {
        mStartedActivityFromFragment = true;
        try {
            if (requestCode == -1) {
                ActivityCompat.startActivityForResult(this, intent, -1, options);
                return;
            }
            checkForValidRequestCode(requestCode);
            int requestIndex = allocateRequestIndex(fragment);
            ActivityCompat.startActivityForResult(
                    this, intent, ((requestIndex + 1) << 16) + (requestCode & 0xffff), options);
        } finally {
            mStartedActivityFromFragment = false;
        }
    }

    /**
     * Called by Fragment.startIntentSenderForResult() to implement its behavior.
     */
    public void startIntentSenderFromFragment(Fragment fragment, IntentSender intent,
            int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        mStartedIntentSenderFromFragment = true;
        try {
            if (requestCode == -1) {
                ActivityCompat.startIntentSenderForResult(this, intent, requestCode, fillInIntent,
                        flagsMask, flagsValues, extraFlags, options);
                return;
            }
            checkForValidRequestCode(requestCode);
            int requestIndex = allocateRequestIndex(fragment);
            ActivityCompat.startIntentSenderForResult(this, intent,
                    ((requestIndex + 1) << 16) + (requestCode & 0xffff), fillInIntent,
                    flagsMask, flagsValues, extraFlags, options);
        } finally {
            mStartedIntentSenderFromFragment = false;
        }
    }

    // Allocates the next available startActivityForResult request index.
    private int allocateRequestIndex(Fragment fragment) {
        // Sanity check that we havn't exhaused the request index space.
        if (mPendingFragmentActivityResults.size() >= MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS) {
            throw new IllegalStateException("Too many pending Fragment activity results.");
        }

        // Find an unallocated request index in the mPendingFragmentActivityResults map.
        while (mPendingFragmentActivityResults.indexOfKey(mNextCandidateRequestIndex) >= 0) {
            mNextCandidateRequestIndex =
                    (mNextCandidateRequestIndex + 1) % MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS;
        }

        int requestIndex = mNextCandidateRequestIndex;
        mPendingFragmentActivityResults.put(requestIndex, fragment.mWho);
        mNextCandidateRequestIndex =
                (mNextCandidateRequestIndex + 1) % MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS;

        return requestIndex;
    }

    /**
     * Called by Fragment.requestPermissions() to implement its behavior.
     */
    void requestPermissionsFromFragment(Fragment fragment, String[] permissions,
            int requestCode) {
        if (requestCode == -1) {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
            return;
        }
        checkForValidRequestCode(requestCode);
        try {
            mRequestedPermissionsFromFragment = true;
            int requestIndex = allocateRequestIndex(fragment);
            ActivityCompat.requestPermissions(this, permissions,
                    ((requestIndex + 1) << 16) + (requestCode & 0xffff));
        } finally {
            mRequestedPermissionsFromFragment = false;
        }
    }

    class HostCallbacks extends FragmentHostCallback<FragmentActivity> {
        public HostCallbacks() {
            super(FragmentActivity.this /*fragmentActivity*/);
        }

        @Override
        public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            FragmentActivity.this.dump(prefix, fd, writer, args);
        }

        @Override
        public boolean onShouldSaveFragmentState(Fragment fragment) {
            return !isFinishing();
        }

        @Override
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
        public void onStartActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
            FragmentActivity.this.startActivityFromFragment(fragment, intent, requestCode);
        }

        @Override
        public void onStartActivityFromFragment(
                Fragment fragment, Intent intent, int requestCode, @Nullable Bundle options) {
            FragmentActivity.this.startActivityFromFragment(fragment, intent, requestCode, options);
        }

        @Override
        public void onStartIntentSenderFromFragment(Fragment fragment, IntentSender intent,
                int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
                int extraFlags, Bundle options) throws IntentSender.SendIntentException {
            FragmentActivity.this.startIntentSenderFromFragment(fragment, intent, requestCode,
                    fillInIntent, flagsMask, flagsValues, extraFlags, options);
        }

        @Override
        public void onRequestPermissionsFromFragment(@NonNull Fragment fragment,
                @NonNull String[] permissions, int requestCode) {
            FragmentActivity.this.requestPermissionsFromFragment(fragment, permissions,
                    requestCode);
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

        @Override
        public void onAttachFragment(Fragment fragment) {
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
    }

    private static void markState(FragmentManager manager, Lifecycle.State state) {
        Collection<Fragment> fragments = manager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            fragment.mLifecycleRegistry.markState(state);
            markState(fragment.getChildFragmentManager(), state);
        }
    }
}
