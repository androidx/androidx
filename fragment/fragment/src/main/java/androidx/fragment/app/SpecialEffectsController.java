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

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.CancellationSignal;
import androidx.core.view.ViewCompat;
import androidx.fragment.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Controller for all "special effects" (such as Animation, Animator, framework Transition, and
 * AndroidX Transition) that can be applied to a Fragment as part of the addition or removal
 * of that Fragment from its container.
 * <p>
 * Each SpecialEffectsController is responsible for a single {@link ViewGroup} container.
 */
abstract class SpecialEffectsController {

    /**
     * Get the {@link SpecialEffectsController} for a given container if it already exists
     * or create it. This will automatically find the containing FragmentManager and use the
     * factory provided by {@link FragmentManager#getSpecialEffectsControllerFactory()}.
     *
     * @param container ViewGroup to find the associated SpecialEffectsController for.
     * @return a SpecialEffectsController for the given container
     */
    @NonNull
    static SpecialEffectsController getOrCreateController(
            @NonNull ViewGroup container, @NonNull FragmentManager fragmentManager) {
        SpecialEffectsControllerFactory factory =
                fragmentManager.getSpecialEffectsControllerFactory();
        return getOrCreateController(container, factory);
    }

    /**
     * Get the {@link SpecialEffectsController} for a given container if it already exists
     * or create it using the given {@link SpecialEffectsControllerFactory} if it does not.
     *
     * @param container ViewGroup to find the associated SpecialEffectsController for.
     * @param factory The factory to use to create a new SpecialEffectsController if one does
     *                not already exist for this container.
     * @return a SpecialEffectsController for the given container
     */
    @NonNull
    static SpecialEffectsController getOrCreateController(
            @NonNull ViewGroup container,
            @NonNull SpecialEffectsControllerFactory factory) {
        Object controller = container.getTag(R.id.special_effects_controller_view_tag);
        if (controller instanceof SpecialEffectsController) {
            return (SpecialEffectsController) controller;
        }
        // Else, create a new SpecialEffectsController
        SpecialEffectsController newController = factory.createController(container);
        container.setTag(R.id.special_effects_controller_view_tag, newController);
        return newController;
    }

    private final ViewGroup mContainer;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayList<Operation> mPendingOperations = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayList<Operation> mRunningOperations = new ArrayList<>();

    boolean mOperationDirectionIsPop = false;
    boolean mIsContainerPostponed = false;

    SpecialEffectsController(@NonNull ViewGroup container) {
        mContainer = container;
    }

    @NonNull
    public ViewGroup getContainer() {
        return mContainer;
    }

    /**
     * Checks what {@link Operation.LifecycleImpact lifecycle impact} of special effect for the
     * given FragmentStateManager is still awaiting completion (or cancellation).
     * <p>
     * This could be because the Operation is still pending (and
     * {@link #executePendingOperations()} hasn't been called) or because all
     * {@link Operation#markStartedSpecialEffect(CancellationSignal) started special effects}
     * haven't {@link Operation#completeSpecialEffect(CancellationSignal) completed}.
     *
     * @param fragmentStateManager the FragmentStateManager to check for
     * @return The {@link Operation.LifecycleImpact} of the awaiting Operation, or null if there is
     * no special effects still in progress.
     */
    @Nullable
    Operation.LifecycleImpact getAwaitingCompletionLifecycleImpact(
            @NonNull FragmentStateManager fragmentStateManager) {
        Operation.LifecycleImpact lifecycleImpact = null;
        // First search through pending operations
        Operation pendingOperation = findPendingOperation(fragmentStateManager.getFragment());
        if (pendingOperation != null) {
            lifecycleImpact = pendingOperation.getLifecycleImpact();
        }
        // Then search through running operations
        Operation runningOperation = findRunningOperation(fragmentStateManager.getFragment());
        // Only use the running operation if the pending operation is null or NONE
        if (runningOperation != null
                && (lifecycleImpact == null || lifecycleImpact == Operation.LifecycleImpact.NONE)) {
            return runningOperation.getLifecycleImpact();
        }
        return lifecycleImpact;
    }

    @Nullable
    private Operation findPendingOperation(@NonNull Fragment fragment) {
        for (Operation operation : mPendingOperations) {
            if (operation.getFragment().equals(fragment) && !operation.isCanceled()) {
                return operation;
            }
        }
        return null;
    }

    @Nullable
    private Operation findRunningOperation(@NonNull Fragment fragment) {
        for (Operation operation : mRunningOperations) {
            if (operation.getFragment().equals(fragment) && !operation.isCanceled()) {
                return operation;
            }
        }
        return null;
    }

    void enqueueAdd(@NonNull Operation.State finalState,
            @NonNull FragmentStateManager fragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG,
                    "SpecialEffectsController: Enqueuing add operation for fragment "
                            + fragmentStateManager.getFragment());
        }
        enqueue(finalState, Operation.LifecycleImpact.ADDING, fragmentStateManager);
    }

    void enqueueShow(@NonNull FragmentStateManager fragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG,
                    "SpecialEffectsController: Enqueuing show operation for fragment "
                            + fragmentStateManager.getFragment());
        }
        enqueue(Operation.State.VISIBLE, Operation.LifecycleImpact.NONE, fragmentStateManager);
    }

    void enqueueHide(@NonNull FragmentStateManager fragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG,
                    "SpecialEffectsController: Enqueuing hide operation for fragment "
                            + fragmentStateManager.getFragment());
        }
        enqueue(Operation.State.GONE, Operation.LifecycleImpact.NONE, fragmentStateManager);
    }

    void enqueueRemove(@NonNull FragmentStateManager fragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG,
                    "SpecialEffectsController: Enqueuing remove operation for fragment "
                            + fragmentStateManager.getFragment());
        }
        enqueue(Operation.State.REMOVED, Operation.LifecycleImpact.REMOVING, fragmentStateManager);
    }

    private void enqueue(@NonNull Operation.State finalState,
            @NonNull Operation.LifecycleImpact lifecycleImpact,
            @NonNull final FragmentStateManager fragmentStateManager) {
        synchronized (mPendingOperations) {
            final CancellationSignal signal = new CancellationSignal();
            Operation existingOperation =
                    findPendingOperation(fragmentStateManager.getFragment());
            if (existingOperation != null) {
                // Update the existing operation by merging in the new information
                // rather than creating a new Operation entirely
                existingOperation.mergeWith(finalState, lifecycleImpact);
                return;
            }
            final FragmentStateManagerOperation operation = new FragmentStateManagerOperation(
                    finalState, lifecycleImpact, fragmentStateManager, signal);
            mPendingOperations.add(operation);
            // Ensure that we still run the applyState() call for pending operations
            operation.addCompletionListener(new Runnable() {
                @Override
                public void run() {
                    if (mPendingOperations.contains(operation)) {
                        operation.getFinalState().applyState(operation.getFragment().mView);
                    }
                }
            });
            // Ensure that we remove the Operation from the list of
            // operations when the operation is complete
            operation.addCompletionListener(new Runnable() {
                @Override
                public void run() {
                    mPendingOperations.remove(operation);
                    mRunningOperations.remove(operation);
                }
            });
        }
    }

    void updateOperationDirection(boolean isPop) {
        mOperationDirectionIsPop = isPop;
    }

    void markPostponedState() {
        synchronized (mPendingOperations) {
            updateFinalState();
            // Default to not postponed
            mIsContainerPostponed = false;
            for (int index = mPendingOperations.size() - 1; index >= 0; index--) {
                Operation operation = mPendingOperations.get(index);
                // Only consider operations with entering transitions
                Operation.State currentState = Operation.State.from(operation.getFragment().mView);
                if (operation.getFinalState() == Operation.State.VISIBLE
                        && currentState != Operation.State.VISIBLE) {
                    Fragment fragment = operation.getFragment();
                    // The container is considered postponed if the Fragment
                    // associated with the last entering Operation is postponed
                    mIsContainerPostponed = fragment.isPostponed();
                    break;
                }
            }
        }
    }

    void forcePostponedExecutePendingOperations() {
        if (mIsContainerPostponed) {
            mIsContainerPostponed = false;
            executePendingOperations();
        }
    }

    void executePendingOperations() {
        if (mIsContainerPostponed) {
            // No operations should execute while the container is postponed
            return;
        }
        // If the container is not attached to the window, ignore the special effect
        // since none of the special effect systems will run them anyway.
        if (!ViewCompat.isAttachedToWindow(mContainer)) {
            forceCompleteAllOperations();
            mOperationDirectionIsPop = false;
            return;
        }
        synchronized (mPendingOperations) {
            if (!mPendingOperations.isEmpty()) {
                ArrayList<Operation> currentlyRunningOperations =
                        new ArrayList<>(mRunningOperations);
                mRunningOperations.clear();
                for (Operation operation : currentlyRunningOperations) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG,
                                "SpecialEffectsController: Cancelling operation " + operation);
                    }
                    operation.cancel();
                    if (!operation.isComplete()) {
                        // Re-add any animations that didn't synchronously call complete()
                        // to continue to track them as running operations
                        mRunningOperations.add(operation);
                    }
                }

                updateFinalState();

                ArrayList<Operation> newPendingOperations = new ArrayList<>(mPendingOperations);
                mPendingOperations.clear();
                mRunningOperations.addAll(newPendingOperations);
                for (Operation operation : newPendingOperations) {
                    operation.onStart();
                }
                executeOperations(newPendingOperations, mOperationDirectionIsPop);
                mOperationDirectionIsPop = false;
            }
        }
    }

    void forceCompleteAllOperations() {
        boolean attachedToWindow = ViewCompat.isAttachedToWindow(mContainer);
        synchronized (mPendingOperations) {
            updateFinalState();
            for (Operation operation : mPendingOperations) {
                operation.onStart();
            }

            // First cancel running operations
            ArrayList<Operation> runningOperations = new ArrayList<>(mRunningOperations);
            for (Operation operation : runningOperations) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                            "SpecialEffectsController: " + (attachedToWindow ? "" :
                                    "Container " + mContainer + " is not attached to window. ")
                                    + "Cancelling running operation " + operation);
                }
                operation.cancel();
            }

            // Then cancel pending operations
            ArrayList<Operation> pendingOperations = new ArrayList<>(mPendingOperations);
            for (Operation operation : pendingOperations) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                            "SpecialEffectsController: " + (attachedToWindow ? "" :
                                    "Container " + mContainer + " is not attached to window. ")
                                    + "Cancelling pending operation " + operation);
                }
                operation.cancel();
            }
        }
    }

    private void updateFinalState() {
        for (Operation operation: mPendingOperations) {
            // update the final state of adding operations
            if (operation.getLifecycleImpact() == Operation.LifecycleImpact.ADDING) {
                Fragment fragment = operation.getFragment();
                View view = fragment.requireView();
                Operation.State finalState = Operation.State.from(view.getVisibility());
                operation.mergeWith(finalState, Operation.LifecycleImpact.NONE);
            }
        }
    }

    /**
     * Execute all of the given operations.
     * <p>
     * If there are no special effects for a given operation, the SpecialEffectsController
     * should call {@link Operation#complete()}. Otherwise, a
     * {@link CancellationSignal} representing each special effect should be added via
     * {@link Operation#markStartedSpecialEffect(CancellationSignal)}, calling
     * {@link Operation#completeSpecialEffect(CancellationSignal)} when that specific
     * special effect finishes. When the last started special effect is completed,
     * {@link Operation#completeSpecialEffect(CancellationSignal)} will call
     * {@link Operation#complete()} automatically.
     * <p>
     * It is <strong>strongly recommended</strong> that each
     * {@link CancellationSignal} added with
     * {@link Operation#markStartedSpecialEffect(CancellationSignal)} listen for cancellation,
     * properly cancelling the special effect when the signal is cancelled.
     *
     * @param operations the list of operations to execute in order.
     * @param isPop whether this set of operations should be considered as triggered by a 'pop'.
     *              This can be used to control the direction of any special effects if they
     *              are not symmetric.
     */
    abstract void executeOperations(@NonNull List<Operation> operations, boolean isPop);

    /**
     * Class representing an ongoing special effects operation.
     *
     * @see #executeOperations(List, boolean)
     */
    static class Operation {

        /**
         * The state that the fragment's View should be in after applying this operation.
         *
         * @see #applyState(View)
         */
        enum State {
            /**
             * The fragment's view should be completely removed from the container.
             */
            REMOVED,
            /**
             * The fragment's view should be made {@link View#VISIBLE}.
             */
            VISIBLE,
            /**
             * The fragment's view should be made {@link View#GONE}.
             */
            GONE,
            /**
             * The fragment's view should be made {@link View#INVISIBLE}.
             */
            INVISIBLE;

            /**
             * Create a new State from the {@link View#getVisibility() view's visibility}.
             *
             * @param view The view to get the current visibility from.
             * @return A new State from the view's visibility.
             */
            @NonNull
            static State from(@NonNull View view) {
                // We should consider views with an alpha of 0 as INVISIBLE.
                if (view.getAlpha() == 0f && view.getVisibility() == View.VISIBLE) {
                    return INVISIBLE;
                }
                return from(view.getVisibility());
            }

            /**
             * Create a new State from the visibility of a View.
             *
             * @param visibility The visibility constant to translate into a State.
             * @return A new State from the visibility.
             */
            @NonNull
            static State from(int visibility) {
                switch (visibility) {
                    case View.VISIBLE:
                        return VISIBLE;
                    case View.INVISIBLE:
                        return INVISIBLE;
                    case View.GONE:
                        return GONE;
                    default:
                        throw new IllegalArgumentException("Unknown visibility " + visibility);
                }
            }

            /**
             * Applies this state to the given View.
             *
             * @param view The View to apply this state to.
             */
            void applyState(@NonNull View view) {
                switch (this) {
                    case REMOVED:
                        ViewGroup parent = (ViewGroup) view.getParent();
                        if (parent != null) {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(FragmentManager.TAG, "SpecialEffectsController: Removing "
                                        + "view " + view + " from container " + parent);
                            }
                            parent.removeView(view);
                        }
                        break;
                    case VISIBLE:
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "SpecialEffectsController: Setting view "
                                    + view + " to VISIBLE");
                        }
                        view.setVisibility(View.VISIBLE);
                        break;
                    case GONE:
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG,
                                    "SpecialEffectsController: Setting view " + view + " to GONE");
                        }
                        view.setVisibility(View.GONE);
                        break;
                    case INVISIBLE:
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG,
                                    "SpecialEffectsController: Setting view " + view + " to "
                                            + "INVISIBLE");
                        }
                        view.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        }

        /**
         * The impact that this operation has on the lifecycle of the fragment.
         */
        enum LifecycleImpact {
            /**
             * No impact on the fragment's lifecycle.
             */
            NONE,
            /**
             * This operation is associated with adding a fragment.
             */
            ADDING,
            /**
             * This operation is associated with removing a fragment.
             */
            REMOVING,
        }

        @NonNull
        private State mFinalState;
        @NonNull
        private LifecycleImpact mLifecycleImpact;
        @NonNull
        private final Fragment mFragment;
        @NonNull
        private final List<Runnable> mCompletionListeners = new ArrayList<>();
        @NonNull
        private final HashSet<CancellationSignal> mSpecialEffectsSignals = new HashSet<>();

        private boolean mIsCanceled = false;
        private boolean mIsComplete = false;

        /**
         * Construct a new Operation.
         *
         * @param finalState What the final state after this operation should be.
         * @param lifecycleImpact The impact on the fragment's lifecycle.
         * @param fragment The Fragment being affected.
         * @param cancellationSignal A signal for handling cancellation
         */
        Operation(@NonNull State finalState, @NonNull LifecycleImpact lifecycleImpact,
                @NonNull Fragment fragment, @NonNull CancellationSignal cancellationSignal) {
            mFinalState = finalState;
            mLifecycleImpact = lifecycleImpact;
            mFragment = fragment;
            // Connect the CancellationSignal to our own
            cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    cancel();
                }
            });
        }

        /**
         * Returns what the final state after this operation should be.
         *
         * @return The final state after this operation should be.
         */
        @NonNull
        public State getFinalState() {
            return mFinalState;
        }

        /**
         * Returns how this Operation affects the lifecycle of the fragment.
         *
         * @return How this Operation affects the lifecycle of the fragment.
         */
        @NonNull
        LifecycleImpact getLifecycleImpact() {
            return mLifecycleImpact;
        }

        /**
         * The Fragment being added / removed.
         * @return An {@link Fragment#isAdded() added} Fragment.
         */
        @NonNull
        public final Fragment getFragment() {
            return mFragment;
        }

        final boolean isCanceled() {
            return mIsCanceled;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Operation ");
            sb.append("{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append("} ");
            sb.append("{");
            sb.append("mFinalState = ");
            sb.append(mFinalState);
            sb.append("} ");
            sb.append("{");
            sb.append("mLifecycleImpact = ");
            sb.append(mLifecycleImpact);
            sb.append("} ");
            sb.append("{");
            sb.append("mFragment = ");
            sb.append(mFragment);
            sb.append("}");
            return sb.toString();
        }

        final void cancel() {
            if (isCanceled()) {
                return;
            }
            mIsCanceled = true;
            if (mSpecialEffectsSignals.isEmpty()) {
                complete();
            } else {
                ArrayList<CancellationSignal> signals = new ArrayList<>(mSpecialEffectsSignals);
                for (CancellationSignal signal : signals) {
                    signal.cancel();
                }
            }
        }

        final void mergeWith(@NonNull State finalState, @NonNull LifecycleImpact lifecycleImpact) {
            switch (lifecycleImpact) {
                case ADDING:
                    if (mFinalState == State.REMOVED) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "SpecialEffectsController: For fragment "
                                    + mFragment + " mFinalState = REMOVED -> VISIBLE. "
                                    + "mLifecycleImpact = " + mLifecycleImpact + " to ADDING.");
                        }
                        // Applying an ADDING operation to a REMOVED fragment
                        // moves it back to ADDING
                        mFinalState = State.VISIBLE;
                        mLifecycleImpact = LifecycleImpact.ADDING;
                    }
                    break;
                case REMOVING:
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG, "SpecialEffectsController: For fragment "
                                + mFragment + " mFinalState = " + mFinalState + " -> REMOVED. "
                                + "mLifecycleImpact  = " + mLifecycleImpact + " to REMOVING.");
                    }
                    // Any REMOVING operation overrides whatever we had before
                    mFinalState = State.REMOVED;
                    mLifecycleImpact = LifecycleImpact.REMOVING;
                    break;
                case NONE:
                    // This is a hide or show operation
                    if (mFinalState != State.REMOVED) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "SpecialEffectsController: For fragment "
                                    + mFragment + " mFinalState = " + mFinalState + " -> "
                                    + finalState + ". ");
                        }
                        mFinalState = finalState;
                    }
            }
        }

        final void addCompletionListener(@NonNull Runnable listener) {
            mCompletionListeners.add(listener);
        }

        /**
         * Callback for when the operation is about to start.
         */
        void onStart() {}

        /**
         * Add new {@link CancellationSignal} for special effects.
         *
         * @param signal A CancellationSignal that can be used to cancel this special effect.
         */
        public final void markStartedSpecialEffect(@NonNull CancellationSignal signal) {
            onStart();
            mSpecialEffectsSignals.add(signal);
        }

        /**
         * Complete a {@link CancellationSignal} that was previously added with
         * {@link #markStartedSpecialEffect(CancellationSignal)}.
         *
         * This calls through to {@link Operation#complete()} when the last special effect is
         * complete.
         */
        public final void completeSpecialEffect(@NonNull CancellationSignal signal) {
            if (mSpecialEffectsSignals.remove(signal) && mSpecialEffectsSignals.isEmpty()) {
                complete();
            }
        }

        final boolean isComplete() {
            return mIsComplete;
        }

        /**
         * Mark this Operation as complete. This should only be called when all
         * special effects associated with this Operation have completed successfully.
         */
        @CallSuper
        public void complete() {
            if (mIsComplete) {
                return;
            }
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG,
                        "SpecialEffectsController: " + this + " has called complete.");
            }
            mIsComplete = true;
            for (Runnable listener : mCompletionListeners) {
                listener.run();
            }
        }
    }

    private static class FragmentStateManagerOperation extends Operation {
        @NonNull
        private final FragmentStateManager mFragmentStateManager;

        FragmentStateManagerOperation(@NonNull State finalState,
                @NonNull LifecycleImpact lifecycleImpact,
                @NonNull FragmentStateManager fragmentStateManager,
                @NonNull CancellationSignal cancellationSignal) {
            super(finalState, lifecycleImpact, fragmentStateManager.getFragment(),
                    cancellationSignal);
            mFragmentStateManager = fragmentStateManager;
        }

        @Override
        void onStart() {
            if (getLifecycleImpact() == Operation.LifecycleImpact.ADDING) {
                Fragment fragment = mFragmentStateManager.getFragment();
                View focusedView = fragment.mView.findFocus();
                if (focusedView != null) {
                    fragment.setFocusedView(focusedView);
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG, "requestFocus: Saved focused view " + focusedView
                                + " for Fragment " + fragment);
                    }
                }
                View view = getFragment().requireView();
                // We need to ensure that the fragment's view is re-added
                // for ADDING operations to properly handle cases where the
                // exit animation was interrupted.
                if (view.getParent() == null) {
                    mFragmentStateManager.addViewToContainer();
                    view.setAlpha(0f);
                }
                // Change the view alphas back to their original values before we execute our
                // transitions.
                if (view.getAlpha() == 0f && view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.INVISIBLE);
                }
                view.setAlpha(fragment.getPostOnViewCreatedAlpha());
            }
        }

        @Override
        public void complete() {
            super.complete();
            mFragmentStateManager.moveToExpectedState();
        }
    }
}
