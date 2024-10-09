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
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.core.view.ViewCompat;
import androidx.fragment.R;
import androidx.fragment.app.strictmode.FragmentStrictMode;
import androidx.lifecycle.ViewModelStoreOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class FragmentStateManager {
    private static final String TAG = FragmentManager.TAG;

    static final String FRAGMENT_STATE_KEY = "state";
    static final String SAVED_INSTANCE_STATE_KEY = "savedInstanceState";
    static final String REGISTRY_STATE_KEY = "registryState";
    static final String CHILD_FRAGMENT_MANAGER_KEY = "childFragmentManager";
    static final String VIEW_STATE_KEY = "viewState";
    static final String VIEW_REGISTRY_STATE_KEY = "viewRegistryState";
    static final String ARGUMENTS_KEY = "arguments";

    private final FragmentLifecycleCallbacksDispatcher mDispatcher;
    private final FragmentStore mFragmentStore;
    private final @NonNull Fragment mFragment;

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
     * @param state Bundle used to restore the state correctly
     */
    @SuppressWarnings("deprecation")
    FragmentStateManager(@NonNull FragmentLifecycleCallbacksDispatcher dispatcher,
            @NonNull FragmentStore fragmentStore,
            @NonNull ClassLoader classLoader,
            @NonNull FragmentFactory fragmentFactory,
            @NonNull Bundle state) {
        mDispatcher = dispatcher;
        mFragmentStore = fragmentStore;

        // Instantiate the fragment's library states in FragmentState
        FragmentState fs = state.getParcelable(FRAGMENT_STATE_KEY);
        mFragment = fs.instantiate(fragmentFactory, classLoader);
        mFragment.mSavedFragmentState = state;

        // Instantiate the fragment's arguments
        Bundle arguments = state.getBundle(ARGUMENTS_KEY);
        if (arguments != null) {
            arguments.setClassLoader(classLoader);
        }
        mFragment.setArguments(arguments);

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
     * @param state Bundle used to restore the state correctly
     */
    @SuppressWarnings("deprecation")
    FragmentStateManager(@NonNull FragmentLifecycleCallbacksDispatcher dispatcher,
            @NonNull FragmentStore fragmentStore,
            @NonNull Fragment retainedFragment,
            @NonNull Bundle state) {
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

        mFragment.mSavedFragmentState = state;
        mFragment.mArguments = state.getBundle(ARGUMENTS_KEY);
    }

    @NonNull Fragment getFragment() {
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
        // For fragments that are added via FragmentTransaction.add(ViewGroup)
        if (mFragment.mInDynamicContainer) {
            if (mFragment.mContainer == null) {
                // If their container is not available yet (onContainerAvailable hasn't been
                // called), don't allow the fragment to go beyond ACTIVITY_CREATED
                maxState = Math.min(maxState, Fragment.ACTIVITY_CREATED);
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
        // Fragments that are transitioning are part of a seeking effect and must be at least
        // AWAITING_EXIT_EFFECTS
        if (mFragment.mTransitioning) {
            maxState = Math.max(maxState, Fragment.AWAITING_EXIT_EFFECTS);
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

            boolean stateWasChanged = false;
            int newState;
            while ((newState = computeExpectedState()) != mFragment.mState) {
                stateWasChanged = true;
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
                                mFragmentStore.setSavedState(mFragment.mWho, saveState());
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
                                mFragmentStore.setSavedState(mFragment.mWho, saveState());
                            }
                            destroy();
                            break;
                        case Fragment.INITIALIZING:
                            detach();
                            break;
                    }
                }
            }
            if (!stateWasChanged && mFragment.mState == Fragment.INITIALIZING) {
                // If the state wasn't changed and the Fragment should be removed
                // then we need to do the work of destroy()+detach() here
                // to ensure the FragmentManager is in a cleaned up state
                if (mFragment.mRemoving && !mFragment.isInBackStack() && !mFragment.mBeingSaved) {
                    if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                        Log.d(TAG, "Cleaning up state of never attached fragment: " + mFragment);
                    }
                    mFragmentStore.getNonConfig().clearNonConfigState(mFragment, true);
                    mFragmentStore.makeInactive(this);
                    if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                        Log.d(TAG, "initState called for fragment: " + mFragment);
                    }
                    mFragment.initState();
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
                mFragment.mChildFragmentManager.dispatchOnHiddenChanged();
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
            Bundle savedInstanceState = null;
            if (mFragment.mSavedFragmentState != null) {
                savedInstanceState = mFragment.mSavedFragmentState.getBundle(
                        SAVED_INSTANCE_STATE_KEY);
            }
            mFragment.performCreateView(mFragment.performGetLayoutInflater(
                    savedInstanceState), null, savedInstanceState);
            if (mFragment.mView != null) {
                mFragment.mView.setSaveFromParentEnabled(false);
                mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
                if (mFragment.mHidden) mFragment.mView.setVisibility(View.GONE);
                mFragment.performViewCreated();
                mDispatcher.dispatchOnFragmentViewCreated(
                        mFragment, mFragment.mView, savedInstanceState, false);
                mFragment.mState = Fragment.VIEW_CREATED;
            }
        }
    }

    @SuppressWarnings("deprecation")
    void restoreState(@NonNull ClassLoader classLoader) {
        if (mFragment.mSavedFragmentState == null) {
            return;
        }
        mFragment.mSavedFragmentState.setClassLoader(classLoader);
        Bundle savedInstanceState = mFragment.mSavedFragmentState.getBundle(
                SAVED_INSTANCE_STATE_KEY);
        if (savedInstanceState == null) {
            // When restoring a Fragment, always ensure we have a
            // non-null Bundle so that developers have a signal for
            // when the Fragment is being restored
            mFragment.mSavedFragmentState.putBundle(SAVED_INSTANCE_STATE_KEY,
                    new Bundle());
        }

        try {
            mFragment.mSavedViewState = mFragment.mSavedFragmentState.getSparseParcelableArray(
                    VIEW_STATE_KEY);
        } catch (BadParcelableException e) {
            throw new IllegalStateException(
                    "Failed to restore view hierarchy state for fragment " + getFragment(), e
            );
        }
        mFragment.mSavedViewRegistryState = mFragment.mSavedFragmentState.getBundle(
                VIEW_REGISTRY_STATE_KEY);

        FragmentState fs =
                mFragment.mSavedFragmentState.getParcelable(FRAGMENT_STATE_KEY);
        if (fs != null) {
            mFragment.mTargetWho = fs.mTargetWho;
            mFragment.mTargetRequestCode = fs.mTargetRequestCode;
            if (mFragment.mSavedUserVisibleHint != null) {
                mFragment.mUserVisibleHint = mFragment.mSavedUserVisibleHint;
                mFragment.mSavedUserVisibleHint = null;
            } else {
                mFragment.mUserVisibleHint = fs.mUserVisibleHint;
            }
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
        Bundle savedInstanceState = null;
        if (mFragment.mSavedFragmentState != null) {
            savedInstanceState = mFragment.mSavedFragmentState.getBundle(SAVED_INSTANCE_STATE_KEY);
        }
        if (!mFragment.mIsCreated) {
            mDispatcher.dispatchOnFragmentPreCreated(mFragment, savedInstanceState, false);
            mFragment.performCreate(savedInstanceState);
            mDispatcher.dispatchOnFragmentCreated(mFragment, savedInstanceState, false);
        } else {
            // The retained fragment has already gone through onCreate()
            // so we move up its state first, then restore any childFragmentManager state
            mFragment.mState = Fragment.CREATED;
            mFragment.restoreChildFragmentState();
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
        Bundle savedInstanceState = null;
        if (mFragment.mSavedFragmentState != null) {
            savedInstanceState = mFragment.mSavedFragmentState.getBundle(SAVED_INSTANCE_STATE_KEY);
        }
        LayoutInflater layoutInflater = mFragment.performGetLayoutInflater(savedInstanceState);
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
                if (!mFragment.mRestored && !mFragment.mInDynamicContainer) {
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
        mFragment.performCreateView(layoutInflater, container, savedInstanceState);
        if (mFragment.mView != null) {
            if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
                Log.d(TAG, "moveto VIEW_CREATED: " + mFragment);
            }
            mFragment.mView.setSaveFromParentEnabled(false);
            mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
            if (container != null) {
                addViewToContainer();
            }
            if (mFragment.mHidden) {
                mFragment.mView.setVisibility(View.GONE);
            }
            // How I wish we could use doOnAttach
            if (mFragment.mView.isAttachedToWindow()) {
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
                    mFragment, mFragment.mView, savedInstanceState, false);
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
        Bundle savedInstanceState = null;
        if (mFragment.mSavedFragmentState != null) {
            savedInstanceState = mFragment.mSavedFragmentState.getBundle(SAVED_INSTANCE_STATE_KEY);
        }
        mFragment.performActivityCreated(savedInstanceState);
        mDispatcher.dispatchOnFragmentActivityCreated(
                mFragment, savedInstanceState, false);
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
        mFragmentStore.setSavedState(mFragment.mWho, null);
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

    @NonNull Bundle saveState() {
        Bundle stateBundle = new Bundle();
        if (mFragment.mState == Fragment.INITIALIZING) {
            // We never even got to ATTACHED, but we could still have some state
            // set by setInitialSavedState so we'll add that to our initial Bundle
            if (mFragment.mSavedFragmentState != null) {
                stateBundle.putAll(mFragment.mSavedFragmentState);
            }
        }

        // Save the library state associated with the Fragment
        FragmentState fs = new FragmentState(mFragment);
        stateBundle.putParcelable(FRAGMENT_STATE_KEY, fs);

        // Save the user state associated with the Fragment
        if (mFragment.mState > Fragment.INITIALIZING) {
            Bundle savedInstanceState = new Bundle();
            mFragment.performSaveInstanceState(savedInstanceState);
            if (!savedInstanceState.isEmpty()) {
                stateBundle.putBundle(SAVED_INSTANCE_STATE_KEY, savedInstanceState);
            }
            mDispatcher.dispatchOnFragmentSaveInstanceState(mFragment, savedInstanceState, false);

            Bundle savedStateRegistryState = new Bundle();
            mFragment.mSavedStateRegistryController.performSave(savedStateRegistryState);
            if (!savedStateRegistryState.isEmpty()) {
                stateBundle.putBundle(REGISTRY_STATE_KEY, savedStateRegistryState);
            }

            Bundle childFragmentManagerState =
                    mFragment.mChildFragmentManager.saveAllStateInternal();
            if (!childFragmentManagerState.isEmpty()) {
                stateBundle.putBundle(CHILD_FRAGMENT_MANAGER_KEY, childFragmentManagerState);
            }

            if (mFragment.mView != null) {
                saveViewState();
            }
            if (mFragment.mSavedViewState != null) {
                stateBundle.putSparseParcelableArray(VIEW_STATE_KEY, mFragment.mSavedViewState);
            }
            if (mFragment.mSavedViewRegistryState != null) {
                stateBundle.putBundle(VIEW_REGISTRY_STATE_KEY, mFragment.mSavedViewRegistryState);
            }
        }

        if (mFragment.mArguments != null) {
            stateBundle.putBundle(ARGUMENTS_KEY, mFragment.mArguments);
        }
        return stateBundle;
    }

    Fragment.@Nullable SavedState saveInstanceState() {
        if (mFragment.mState > Fragment.INITIALIZING) {
            return new Fragment.SavedState(saveState());
        }
        return null;
    }

    void saveViewState() {
        if (mFragment.mView == null) {
            return;
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG,
                    "Saving view state for fragment " + mFragment + " with view " + mFragment.mView
            );
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
                mFragmentStore.getNonConfig().clearNonConfigState(mFragment, false);
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
        Fragment expectedParent = FragmentManager.findViewFragment(mFragment.mContainer);
        Fragment actualParent = mFragment.getParentFragment();
        // onFindViewById prevents any wrong nested hierarchies when expectedParent is null already
        if (expectedParent != null) {
            if (!expectedParent.equals(actualParent)) {
                FragmentStrictMode.onWrongNestedHierarchy(mFragment, expectedParent,
                        mFragment.mContainerId);
            }
        }

        // Ensure that our new Fragment is placed in the right index
        // based on its relative position to Fragments already in the
        // same container
        int index = mFragmentStore.findFragmentIndexInContainer(mFragment);
        mFragment.mContainer.addView(mFragment.mView, index);
    }
}
