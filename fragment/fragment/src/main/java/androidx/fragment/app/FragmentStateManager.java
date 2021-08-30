/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.R;
import androidx.fragment.app.strictmode.FragmentStrictMode;
import androidx.lifecycle.ViewModelStoreOwner;

class FragmentStateManager {
    private static final String TAG = FragmentManager.TAG;

    private static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    private static final String TARGET_STATE_TAG = "android:target_state";
    private static final String VIEW_STATE_TAG = "android:view_state";
    private static final String VIEW_REGISTRY_STATE_TAG = "android:view_registry_state";
    private static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";

    private final FragmentLifecycleCallbacksDispatcher mDispatcher;
    private final FragmentStore mFragmentStore;
    @NonNull
    private final Fragment mFragment;

    private boolean mMovingToState = false;
    private int mFragmentManagerState = Fragment.INITIALIZING;

    /**
     * Create a FragmentStateManager from a brand new Fragment instance.
     *
     * @param dispatcher Dispatcher for any lifecycle callbacks triggered by this class
     * @param fragmentStore FragmentStore handling all Fragments
     * @param fragment The Fragment to manage
     */
    FragmentStateManager(@NonNull FragmentLifecycleCallbacksDispatcher dispatcher,
            @NonNull FragmentStore fragmentStore, @NonNull Fragment fragment) {
        mDispatcher = dispatcher;
        mFragmentStore = fragmentStore;
        mFragment = fragment;
    }

    /**
     * Recreate a FragmentStateManager from a FragmentState instance, instantiating
     * a new Fragment from the {@link FragmentFactory}.
     *
     * @param dispatcher Dispatcher for any lifecycle callbacks triggered by this class
     * @param fragmentStore FragmentStore handling all Fragments
     * @param classLoader ClassLoader used to instantiate the Fragment
     * @param fragmentFactory FragmentFactory used to instantiate the Fragment
     * @param fs FragmentState used to restore the state correctly
     */
    FragmentStateManager(@NonNull FragmentLifecycleCallbacksDispatcher dispatcher,
            @NonNull FragmentStore fragmentStore,
            @NonNull ClassLoader classLoader,
            @NonNull FragmentFactory fragmentFactory,
            @NonNull FragmentState fs) {
        mDispatcher = dispatcher;
        mFragmentStore = fragmentStore;
        mFragment = fs.instantiate(fragmentFactory, classLoader);
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Instantiated fragment " + mFragment);
        }
    }

    /**
     * Recreate the FragmentStateManager from a retained Fragment and a
     * FragmentState instance.
     *
     * @param dispatcher Dispatcher for any lifecycle callbacks triggered by this class
     * @param fragmentStore FragmentStore handling all Fragments
     * @param retainedFragment A retained fragment
     * @param fs FragmentState used to restore the state correctly
     */
    FragmentStateManager(@NonNull FragmentLifecycleCallbacksDispatcher dispatcher,
            @NonNull FragmentStore fragmentStore,
            @NonNull Fragment retainedFragment,
            @NonNull FragmentState fs) {
        mDispatcher = dispatcher;
        mFragmentStore = fragmentStore;
        mFragment = retainedFragment;
        mFragment.mSavedViewState = null;
        mFragment.mSavedViewRegistryState = null;
        mFragment.mBackStackNesting = 0;
        mFragment.mInLayout = false;
        mFragment.mAdded = false;
        mFragment.mTargetWho = mFragment.mTarget != null ? mFragment.mTarget.mWho : null;
        mFragment.mTarget = null;
        if (fs.mSavedFragmentState != null) {
            mFragment.mSavedFragmentState = fs.mSavedFragmentState;
        } else {
            // When restoring a Fragment, always ensure we have a
            // non-null Bundle so that developers have a signal for
            // when the Fragment is being restored
            mFragment.mSavedFragmentState = new Bundle();
        }
    }

    @NonNull
    Fragment getFragment() {
        return mFragment;
    }

    /**
     * Set the state of the FragmentManager. This will be used by
     * {@link #computeExpectedState()} to limit the max state of the Fragment.
     *
     * @param state one of the constants in {@link Fragment}
     */
    void setFragmentManagerState(int state) {
        mFragmentManagerState = state;
    }

    /**
     * Compute the state that the Fragment should be in given the internal
     * state of the Fragment and the signals passed into FragmentStateManager.
     *
     * @return the state that the Fragment should be in
     */
    int computeExpectedState() {
        // If the FragmentManager is null, disallow changing the state at all
        if (mFragment.mFragmentManager == null) {
            return mFragment.mState;
        }
        // Assume the Fragment can go as high as the FragmentManager's state
        int maxState = mFragmentManagerState;

        // Don't allow the Fragment to go above its max lifecycle state
        switch (mFragment.mMaxState) {
            case RESUMED:
                // maxState can't go any higher than RESUMED, so there's nothing to do here
                break;
            case STARTED:
                maxState = Math.min(maxState, Fragment.STARTED);
                break;
            case CREATED:
                maxState = Math.min(maxState, Fragment.CREATED);
                break;
            case INITIALIZED:
                maxState = Math.min(maxState, Fragment.ATTACHED);
                break;
            default:
                maxState = Math.min(maxState, Fragment.INITIALIZING);
        }

        // For fragments that are created from a layout using the <fragment> tag (mFromLayout)
        if (mFragment.mFromLayout) {
            if (mFragment.mInLayout) {
                // Move them immediately to VIEW_CREATED when they are
                // actually added to the layout (mInLayout).
                maxState = Math.max(mFragmentManagerState, Fragment.VIEW_CREATED);
                // But don't move to higher than VIEW_CREATED until the view is added to its parent
                // and the LayoutInflater call has returned
                if (mFragment.mView != null && mFragment.mView.getParent() == null) {
                    maxState = Math.min(maxState, Fragment.VIEW_CREATED);
                }
            } else {
                if (mFragmentManagerState < Fragment.ACTIVITY_CREATED) {
                    // But while they are not in the layout, don't allow their
                    // state to progress upward until the FragmentManager state
                    // is at least ACTIVITY_CREATED. This ensures they get the onInflate()
                    // callback before being attached or created.
                    maxState = Math.min(maxState, mFragment.mState);
                } else {
                    // Once the FragmentManager state is at least ACTIVITY_CREATED
                    // their state can progress up to CREATED as we assume that
                    // they are not ever going to be in layout
                    maxState = Math.min(maxState, Fragment.CREATED);
                }
            }
        }
        // Fragments that are not currently added will sit in the CREATED state.
        if (!mFragment.mAdded) {
            maxState = Math.min(maxState, Fragment.CREATED);
        }
        SpecialEffectsController.Operation.LifecycleImpact awaitingEffect = null;
        if (mFragment.mContainer != null) {
            SpecialEffectsController controller = SpecialEffectsController.getOrCreateController(
                    mFragment.mContainer, mFragment.getParentFragmentManager());
            awaitingEffect = controller.getAwaitingCompletionLifecycleImpact(this);
        }
        if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.ADDING) {
            // Fragments awaiting their enter effects cannot proceed beyond that state
            maxState = Math.min(maxState, Fragment.AWAITING_ENTER_EFFECTS);
        } else if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.REMOVING) {
            // Fragments that are in the process of being removed shouldn't go below that state
            maxState = Math.max(maxState, Fragment.AWAITING_EXIT_EFFECTS);
        } else if (mFragment.mRemoving) {
            if (mFragment.isInBackStack()) {
                // Fragments on the back stack shouldn't go higher than CREATED
                maxState = Math.min(maxState, Fragment.CREATED);
            } else {
                // While removing a fragment, we always move to INITIALIZING
                maxState = Math.min(maxState, Fragment.INITIALIZING);
            }
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (mFragment.mDeferStart && mFragment.mState < Fragment.STARTED) {
            maxState = Math.min(maxState, Fragment.ACTIVITY_CREATED);
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "computeExpectedState() of " + maxState + " for "
                    + mFragment);
        }
        return maxState;
    }

    void moveToExpectedState() {
        if (mMovingToState) {
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, "Ignoring re-entrant call to "
                        + "moveToExpectedState() for " + getFragment());
            }
            return;
        }
        try {
            mMovingToState = true;

            int newState;
            while ((newState = computeExpectedState()) != mFragment.mState) {
                if (newState > mFragment.mState) {
                    // Moving upward
                    int nextStep = mFragment.mState + 1;
                    switch (nextStep) {
                        case Fragment.ATTACHED:
                            attach();
                            break;
                        case Fragment.CREATED:
                            create();
                            break;
                        case Fragment.VIEW_CREATED:
                            ensureInflatedView();
                            createView();
                            break;
                        case Fragment.AWAITING_EXIT_EFFECTS:
                            activityCreated();
                            break;
                        case Fragment.ACTIVITY_CREATED:
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                int visibility = mFragment.mView.getVisibility();
                                SpecialEffectsController.Operation.State finalState =
                                        SpecialEffectsController.Operation.State.from(visibility);
                                controller.enqueueAdd(finalState, this);
                            }
                            mFragment.mState = Fragment.ACTIVITY_CREATED;
                            break;
                        case Fragment.STARTED:
                            start();
                            break;
                        case Fragment.AWAITING_ENTER_EFFECTS:
                            mFragment.mState = Fragment.AWAITING_ENTER_EFFECTS;
                            break;
                        case Fragment.RESUMED:
                            resume();
                            break;
                    }
                } else {
                    // Moving downward
                    int nextStep = mFragment.mState - 1;
                    switch (nextStep) {
                        case Fragment.AWAITING_ENTER_EFFECTS:
                            pause();
                            break;
                        case Fragment.STARTED:
                            mFragment.mState = Fragment.STARTED;
                            break;
                        case Fragment.ACTIVITY_CREATED:
                            stop();
                            break;
                        case Fragment.AWAITING_EXIT_EFFECTS:
                            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                                Log.d(TAG, "movefrom ACTIVITY_CREATED: " + mFragment);
                            }
                            if (mFragment.mBeingSaved) {
                                saveState();
                            } else if (mFragment.mView != null) {
                                // Need to save the current view state if not done already
                                // by saveInstanceState()
                                if (mFragment.mSavedViewState == null) {
                                    saveViewState();
                                }
                            }
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                controller.enqueueRemove(this);
                            }
                            mFragment.mState = Fragment.AWAITING_EXIT_EFFECTS;
                            break;
                        case Fragment.VIEW_CREATED:
                            mFragment.mInLayout = false;
                            mFragment.mState = Fragment.VIEW_CREATED;
                            break;
                        case Fragment.CREATED:
                            destroyFragmentView();
                            mFragment.mState = Fragment.CREATED;
                            break;
                        case Fragment.ATTACHED:
                            if (mFragment.mBeingSaved
                                    && mFragmentStore.getSavedState(mFragment.mWho) == null) {
                                saveState();
                            }
                            destroy();
                            break;
                        case Fragment.INITIALIZING:
                            detach();
                            break;
                    }
                }
            }
            if (mFragment.mHiddenChanged) {
                if (mFragment.mView != null && mFragment.mContainer != null) {
                    // Get the controller and enqueue the show/hide
                    SpecialEffectsController controller = SpecialEffectsController
                            .getOrCreateController(mFragment.mContainer,
                                    mFragment.getParentFragmentManager());
                    if (mFragment.mHidden) {
                        controller.enqueueHide(this);
                    } else {
                        controller.enqueueShow(this);
                    }
                }
                if (mFragment.mFragmentManager != null) {
                    mFragment.mFragmentManager.invalidateMenuForFragment(mFragment);
                }
                mFragment.mHiddenChanged = false;
                mFragment.onHiddenChanged(mFragment.mHidden);
            }
        } finally {
            mMovingToState = false;
        }
    }

    void ensureInflatedView() {
        if (mFragment.mFromLayout && mFragment.mInLayout && !mFragment.mPerformedCreateView) {
            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "moveto CREATE_VIEW: " + mFragment);
            }
            mFragment.performCreateView(mFragment.performGetLayoutInflater(
                    mFragment.mSavedFragmentState), null, mFragment.mSavedFragmentState);
            if (mFragment.mView != null) {
                mFragment.mView.setSaveFromParentEnabled(false);
                mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
                if (mFragment.mHidden) mFragment.mView.setVisibility(View.GONE);
                mFragment.performViewCreated();
                mDispatcher.dispatchOnFragmentViewCreated(
                        mFragment, mFragment.mView, mFragment.mSavedFragmentState, false);
                mFragment.mState = Fragment.VIEW_CREATED;
            }
        }
    }

    void restoreState(@NonNull ClassLoader classLoader) {
        if (mFragment.mSavedFragmentState == null) {
            return;
        }
        mFragment.mSavedFragmentState.setClassLoader(classLoader);
        mFragment.mSavedViewState = mFragment.mSavedFragmentState.getSparseParcelableArray(
                VIEW_STATE_TAG);
        mFragment.mSavedViewRegistryState = mFragment.mSavedFragmentState.getBundle(
                VIEW_REGISTRY_STATE_TAG);
        mFragment.mTargetWho = mFragment.mSavedFragmentState.getString(
                TARGET_STATE_TAG);
        if (mFragment.mTargetWho != null) {
            mFragment.mTargetRequestCode = mFragment.mSavedFragmentState.getInt(
                    TARGET_REQUEST_CODE_STATE_TAG, 0);
        }
        if (mFragment.mSavedUserVisibleHint != null) {
            mFragment.mUserVisibleHint = mFragment.mSavedUserVisibleHint;
            mFragment.mSavedUserVisibleHint = null;
        } else {
            mFragment.mUserVisibleHint = mFragment.mSavedFragmentState.getBoolean(
                    USER_VISIBLE_HINT_TAG, true);
        }
        if (!mFragment.mUserVisibleHint) {
            mFragment.mDeferStart = true;
        }
    }

    void attach() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto ATTACHED: " + mFragment);
        }
        // If we have a target fragment, ensure it moves to its expected state first
        // so that this fragment can rely on it as an initialized dependency.
        FragmentStateManager targetFragmentStateManager;
        if (mFragment.mTarget != null) {
            targetFragmentStateManager = mFragmentStore.getFragmentStateManager(
                    mFragment.mTarget.mWho);
            if (targetFragmentStateManager == null) {
                throw new IllegalStateException("Fragment " + mFragment
                        + " declared target fragment " + mFragment.mTarget
                        + " that does not belong to this FragmentManager!");
            }
            mFragment.mTargetWho = mFragment.mTarget.mWho;
            mFragment.mTarget = null;
        } else if (mFragment.mTargetWho != null) {
            targetFragmentStateManager = mFragmentStore.getFragmentStateManager(
                    mFragment.mTargetWho);
            if (targetFragmentStateManager == null) {
                throw new IllegalStateException("Fragment " + mFragment
                        + " declared target fragment " + mFragment.mTargetWho
                        + " that does not belong to this FragmentManager!");
            }
        } else {
            targetFragmentStateManager = null;
        }
        if (targetFragmentStateManager != null) {
            targetFragmentStateManager.moveToExpectedState();
        }
        mFragment.mHost = mFragment.mFragmentManager.getHost();
        mFragment.mParentFragment = mFragment.mFragmentManager.getParent();
        mDispatcher.dispatchOnFragmentPreAttached(mFragment, false);
        mFragment.performAttach();
        mDispatcher.dispatchOnFragmentAttached(mFragment, false);
    }

    void create() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto CREATED: " + mFragment);
        }
        if (!mFragment.mIsCreated) {
            mDispatcher.dispatchOnFragmentPreCreated(
                    mFragment, mFragment.mSavedFragmentState, false);
            mFragment.performCreate(mFragment.mSavedFragmentState);
            mDispatcher.dispatchOnFragmentCreated(
                    mFragment, mFragment.mSavedFragmentState, false);
        } else {
            mFragment.restoreChildFragmentState(mFragment.mSavedFragmentState);
            mFragment.mState = Fragment.CREATED;
        }
    }

    void createView() {
        if (mFragment.mFromLayout) {
            // This case is handled by ensureInflatedView(), so there's nothing
            // else we need to do here.
            return;
        }
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto CREATE_VIEW: " + mFragment);
        }
        LayoutInflater layoutInflater = mFragment.performGetLayoutInflater(
                mFragment.mSavedFragmentState);
        ViewGroup container = null;
        if (mFragment.mContainer != null) {
            container = mFragment.mContainer;
        } else if (mFragment.mContainerId != 0) {
            if (mFragment.mContainerId == View.NO_ID) {
                throw new IllegalArgumentException("Cannot create fragment " + mFragment
                        + " for a container view with no id");
            }
            FragmentContainer fragmentContainer = mFragment.mFragmentManager.getContainer();
            container = (ViewGroup) fragmentContainer.onFindViewById(mFragment.mContainerId);
            if (container == null) {
                if (!mFragment.mRestored) {
                    String resName;
                    try {
                        resName = mFragment.getResources().getResourceName(mFragment.mContainerId);
                    } catch (Resources.NotFoundException e) {
                        resName = "unknown";
                    }
                    throw new IllegalArgumentException("No view found for id 0x"
                            + Integer.toHexString(mFragment.mContainerId) + " ("
                            + resName + ") for fragment " + mFragment);
                }
            } else {
                if (!(container instanceof FragmentContainerView)) {
                    FragmentStrictMode.onWrongFragmentContainer(mFragment, container);
                }
            }
        }
        mFragment.mContainer = container;
        mFragment.performCreateView(layoutInflater, container, mFragment.mSavedFragmentState);
        if (mFragment.mView != null) {
            mFragment.mView.setSaveFromParentEnabled(false);
            mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
            if (container != null) {
                addViewToContainer();
            }
            if (mFragment.mHidden) {
                mFragment.mView.setVisibility(View.GONE);
            }
            // How I wish we could use doOnAttach
            if (ViewCompat.isAttachedToWindow(mFragment.mView)) {
                ViewCompat.requestApplyInsets(mFragment.mView);
            } else {
                final View fragmentView = mFragment.mView;
                fragmentView.addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {
                                fragmentView.removeOnAttachStateChangeListener(this);
                                ViewCompat.requestApplyInsets(fragmentView);
                            }

                            @Override
                            public void onViewDetachedFromWindow(View v) {
                            }
                        });
            }
            mFragment.performViewCreated();
            mDispatcher.dispatchOnFragmentViewCreated(
                    mFragment, mFragment.mView, mFragment.mSavedFragmentState, false);
            int postOnViewCreatedVisibility = mFragment.mView.getVisibility();
            float postOnViewCreatedAlpha = mFragment.mView.getAlpha();
            mFragment.setPostOnViewCreatedAlpha(postOnViewCreatedAlpha);
            if (mFragment.mContainer != null && postOnViewCreatedVisibility == View.VISIBLE) {
                // Save the focused view if one was set via requestFocus()
                View focusedView = mFragment.mView.findFocus();
                if (focusedView != null) {
                    mFragment.setFocusedView(focusedView);
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(TAG, "requestFocus: Saved focused view " + focusedView
                                + " for Fragment " + mFragment);
                    }
                }
                // Set the view alpha to 0
                mFragment.mView.setAlpha(0f);
            }
        }
        mFragment.mState = Fragment.VIEW_CREATED;
    }

    void activityCreated() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto ACTIVITY_CREATED: " + mFragment);
        }
        mFragment.performActivityCreated(mFragment.mSavedFragmentState);
        mDispatcher.dispatchOnFragmentActivityCreated(
                mFragment, mFragment.mSavedFragmentState, false);
    }

    void start() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto STARTED: " + mFragment);
        }
        mFragment.performStart();
        mDispatcher.dispatchOnFragmentStarted(mFragment, false);
    }

    void resume() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "moveto RESUMED: " + mFragment);
        }
        View focusedView = mFragment.getFocusedView();
        if (focusedView != null && isFragmentViewChild(focusedView)) {
            boolean success = focusedView.requestFocus();
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, "requestFocus: Restoring focused view "
                        + focusedView + " " + (success ? "succeeded" : "failed") + " on Fragment "
                        + mFragment + " resulting in focused view " + mFragment.mView.findFocus());
            }
        }
        mFragment.setFocusedView(null);
        mFragment.performResume();
        mDispatcher.dispatchOnFragmentResumed(mFragment, false);
        mFragment.mSavedFragmentState = null;
        mFragment.mSavedViewState = null;
        mFragment.mSavedViewRegistryState = null;
    }

    private boolean isFragmentViewChild(@NonNull View view) {
        if (view == mFragment.mView) {
            return true;
        }
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == mFragment.mView) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    void pause() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "movefrom RESUMED: " + mFragment);
        }
        mFragment.performPause();
        mDispatcher.dispatchOnFragmentPaused(mFragment, false);
    }

    void stop() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "movefrom STARTED: " + mFragment);
        }
        mFragment.performStop();
        mDispatcher.dispatchOnFragmentStopped(mFragment, false);
    }

    void saveState() {
        FragmentState fs = new FragmentState(mFragment);

        if (mFragment.mState > Fragment.INITIALIZING && fs.mSavedFragmentState == null) {
            fs.mSavedFragmentState = saveBasicState();

            if (mFragment.mTargetWho != null) {
                if (fs.mSavedFragmentState == null) {
                    fs.mSavedFragmentState = new Bundle();
                }
                fs.mSavedFragmentState.putString(
                        TARGET_STATE_TAG,
                        mFragment.mTargetWho);
                if (mFragment.mTargetRequestCode != 0) {
                    fs.mSavedFragmentState.putInt(
                            TARGET_REQUEST_CODE_STATE_TAG,
                            mFragment.mTargetRequestCode);
                }
            }

        } else {
            fs.mSavedFragmentState = mFragment.mSavedFragmentState;
        }
        mFragmentStore.setSavedState(mFragment.mWho, fs);
    }

    @Nullable
    Fragment.SavedState saveInstanceState() {
        if (mFragment.mState > Fragment.INITIALIZING) {
            Bundle result = saveBasicState();
            return result != null ? new Fragment.SavedState(result) : null;
        }
        return null;
    }

    private Bundle saveBasicState() {
        Bundle result = new Bundle();

        mFragment.performSaveInstanceState(result);
        mDispatcher.dispatchOnFragmentSaveInstanceState(mFragment, result, false);
        if (result.isEmpty()) {
            result = null;
        }

        if (mFragment.mView != null) {
            saveViewState();
        }
        if (mFragment.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(
                    VIEW_STATE_TAG, mFragment.mSavedViewState);
        }
        if (mFragment.mSavedViewRegistryState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putBundle(VIEW_REGISTRY_STATE_TAG, mFragment.mSavedViewRegistryState);
        }
        if (!mFragment.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            // Only add this if it's not the default value
            result.putBoolean(USER_VISIBLE_HINT_TAG, mFragment.mUserVisibleHint);
        }

        return result;
    }

    void saveViewState() {
        if (mFragment.mView == null) {
            return;
        }
        SparseArray<Parcelable> mStateArray = new SparseArray<>();
        mFragment.mView.saveHierarchyState(mStateArray);
        if (mStateArray.size() > 0) {
            mFragment.mSavedViewState = mStateArray;
        }
        Bundle outBundle = new Bundle();
        mFragment.mViewLifecycleOwner.performSave(outBundle);
        if (!outBundle.isEmpty()) {
            mFragment.mSavedViewRegistryState = outBundle;
        }
    }

    void destroyFragmentView() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "movefrom CREATE_VIEW: " + mFragment);
        }
        // In cases where we never got up to AWAITING_EXIT_EFFECTS, we
        // need to manually remove the view from the container to reverse
        // what we did in createView()
        if (mFragment.mContainer != null && mFragment.mView != null) {
            mFragment.mContainer.removeView(mFragment.mView);
        }
        mFragment.performDestroyView();
        mDispatcher.dispatchOnFragmentViewDestroyed(mFragment, false);
        mFragment.mContainer = null;
        mFragment.mView = null;
        // Set here to ensure that Observers are called after
        // the Fragment's view is set to null
        mFragment.mViewLifecycleOwner = null;
        mFragment.mViewLifecycleOwnerLiveData.setValue(null);
        mFragment.mInLayout = false;
    }

    void destroy() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "movefrom CREATED: " + mFragment);
        }
        boolean beingRemoved = mFragment.mRemoving && !mFragment.isInBackStack();
        // Clear any previous saved state
        if (beingRemoved && !mFragment.mBeingSaved) {
            mFragmentStore.setSavedState(mFragment.mWho, null);
        }
        boolean shouldDestroy = beingRemoved
                || mFragmentStore.getNonConfig().shouldDestroy(mFragment);
        if (shouldDestroy) {
            FragmentHostCallback<?> host = mFragment.mHost;
            boolean shouldClear;
            if (host instanceof ViewModelStoreOwner) {
                shouldClear = mFragmentStore.getNonConfig().isCleared();
            } else if (host.getContext() instanceof Activity) {
                Activity activity = (Activity) host.getContext();
                shouldClear = !activity.isChangingConfigurations();
            } else {
                shouldClear = true;
            }
            if ((beingRemoved && !mFragment.mBeingSaved) || shouldClear) {
                mFragmentStore.getNonConfig().clearNonConfigState(mFragment);
            }
            mFragment.performDestroy();
            mDispatcher.dispatchOnFragmentDestroyed(mFragment, false);
            // Ensure that any Fragment that had this Fragment as its
            // target Fragment retains a reference to the Fragment
            for (FragmentStateManager fragmentStateManager :
                    mFragmentStore.getActiveFragmentStateManagers()) {
                if (fragmentStateManager != null) {
                    Fragment fragment = fragmentStateManager.getFragment();
                    if (mFragment.mWho.equals(fragment.mTargetWho)) {
                        fragment.mTarget = mFragment;
                        fragment.mTargetWho = null;
                    }
                }
            }
            if (mFragment.mTargetWho != null) {
                // Restore the target Fragment so that it can be accessed
                // even after the Fragment is removed.
                mFragment.mTarget = mFragmentStore.findActiveFragment(mFragment.mTargetWho);
            }
            mFragmentStore.makeInactive(this);
        } else {
            if (mFragment.mTargetWho != null) {
                Fragment target = mFragmentStore.findActiveFragment(mFragment.mTargetWho);
                if (target != null && target.mRetainInstance) {
                    // Only keep references to other retained Fragments
                    // to avoid developers accessing Fragments that
                    // are never coming back
                    mFragment.mTarget = target;
                }
            }
            mFragment.mState = Fragment.ATTACHED;
        }
    }

    void detach() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(TAG, "movefrom ATTACHED: " + mFragment);
        }
        mFragment.performDetach();
        mDispatcher.dispatchOnFragmentDetached(
                mFragment, false);
        mFragment.mState = Fragment.INITIALIZING;
        mFragment.mHost = null;
        mFragment.mParentFragment = null;
        mFragment.mFragmentManager = null;
        boolean beingRemoved = mFragment.mRemoving && !mFragment.isInBackStack();
        if (beingRemoved || mFragmentStore.getNonConfig().shouldDestroy(mFragment)) {
            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "initState called for fragment: " + mFragment);
            }
            mFragment.initState();
        }
    }

    void addViewToContainer() {
        // Ensure that our new Fragment is placed in the right index
        // based on its relative position to Fragments already in the
        // same container
        int index = mFragmentStore.findFragmentIndexInContainer(mFragment);
        mFragment.mContainer.addView(mFragment.mView, index);
    }
}
