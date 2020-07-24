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

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.CancellationSignal;
import androidx.fragment.R;

import java.util.ArrayList;
import java.util.HashMap;
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
    final HashMap<Fragment, Operation> mAwaitingCompletionOperations = new HashMap<>();

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
     * {@link #executePendingOperations()} hasn't been called) or because the
     * controller hasn't called {@link Operation#complete()}.
     *
     * @param fragmentStateManager the FragmentStateManager to check for
     * @return The {@link Operation.LifecycleImpact} of the awaiting Operation, or null if there is
     * no special effects still in progress.
     */
    @Nullable
    Operation.LifecycleImpact getAwaitingCompletionLifecycleImpact(
            @NonNull FragmentStateManager fragmentStateManager) {
        Operation operation = mAwaitingCompletionOperations.get(
                fragmentStateManager.getFragment());
        if (operation != null) {
            return operation.getLifecycleImpact();
        }
        return null;
    }

    void enqueueAdd(@NonNull Operation.State finalState,
            @NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(finalState, Operation.LifecycleImpact.ADDING,
                fragmentStateManager, cancellationSignal);
    }

    void enqueueShow(@NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(Operation.State.VISIBLE, Operation.LifecycleImpact.NONE,
                fragmentStateManager, cancellationSignal);
    }

    void enqueueHide(@NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(Operation.State.GONE, Operation.LifecycleImpact.NONE,
                fragmentStateManager, cancellationSignal);
    }

    void enqueueRemove(@NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(Operation.State.REMOVED, Operation.LifecycleImpact.REMOVING,
                fragmentStateManager, cancellationSignal);
    }

    private void enqueue(@NonNull Operation.State finalState,
            @NonNull Operation.LifecycleImpact lifecycleImpact,
            @NonNull final FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        if (cancellationSignal.isCanceled()) {
            // Ignore enqueue operations that are already cancelled
            return;
        }
        synchronized (mPendingOperations) {
            final CancellationSignal signal = new CancellationSignal();
            Operation existingOperation =
                    mAwaitingCompletionOperations.get(fragmentStateManager.getFragment());
            if (existingOperation != null) {
                // Update the existing operation by merging in the new information
                // rather than creating a new Operation entirely
                existingOperation.mergeWith(finalState, lifecycleImpact, cancellationSignal);
                return;
            }
            final FragmentStateManagerOperation operation = new FragmentStateManagerOperation(
                    finalState, lifecycleImpact, fragmentStateManager, signal);
            mPendingOperations.add(operation);
            mAwaitingCompletionOperations.put(operation.getFragment(), operation);
            // Ensure that pending operations are removed when cancelled
            cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    synchronized (mPendingOperations) {
                        mPendingOperations.remove(operation);
                        mAwaitingCompletionOperations.remove(operation.getFragment());
                        signal.cancel();
                    }
                }
            });
            // Ensure that we remove the Operation from the list of
            // awaiting completion operations when the operation is complete
            operation.addCompletionListener(new Runnable() {
                @Override
                public void run() {
                    if (!operation.getCancellationSignal().isCanceled()) {
                        mAwaitingCompletionOperations.remove(operation.getFragment());
                    }
                }
            });
        }
    }

    void updateOperationDirection(boolean isPop) {
        mOperationDirectionIsPop = isPop;
    }

    void markPostponedState() {
        synchronized (mPendingOperations) {
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
        synchronized (mPendingOperations) {
            if (!mPendingOperations.isEmpty()) {
                executeOperations(new ArrayList<>(mPendingOperations), mOperationDirectionIsPop);
                mPendingOperations.clear();
                mOperationDirectionIsPop = false;
            }
        }
    }

    void forceCompleteAllOperations() {
        synchronized (mPendingOperations) {
            for (Operation operation : mAwaitingCompletionOperations.values()) {
                operation.getCancellationSignal().cancel();
                operation.getFinalState().applyState(operation.getFragment().mView);
                operation.complete();
            }
            mAwaitingCompletionOperations.clear();
            // mPendingOperations is a subset of mAwaitingCompletionOperations
            // so cancellation is already done, we just need to clear out the operations
            mPendingOperations.clear();
        }
    }

    /**
     * Execute all of the given operations.
     * <p>
     * At a minimum, the SpecialEffectsController should call
     * {@link Operation#complete()} on each operation when all of the special effects
     * for the given Operation are complete.
     * <p>
     * It is <strong>strongly recommended</strong> that the SpecialEffectsController
     * should call {@link Operation#getCancellationSignal()} and listen for cancellation,
     * properly cancelling all special effects when the signal is cancelled.
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
                            parent.removeView(view);
                        }
                        break;
                    case VISIBLE:
                        view.setVisibility(View.VISIBLE);
                        break;
                    case GONE:
                        view.setVisibility(View.GONE);
                        break;
                    case INVISIBLE:
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
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull
        final CancellationSignal mCancellationSignal = new CancellationSignal();
        @NonNull
        private final List<Runnable> mCompletionListeners = new ArrayList<>();

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
                    mCancellationSignal.cancel();
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

        /**
         * The {@link CancellationSignal} that signals that the operation should be
         * cancelled and any currently running special effects should be cancelled.
         *
         * @return A signal for handling cancellation
         */
        @NonNull
        public final CancellationSignal getCancellationSignal() {
            return mCancellationSignal;
        }

        final void mergeWith(@NonNull State finalState, @NonNull LifecycleImpact lifecycleImpact,
                @NonNull CancellationSignal cancellationSignal) {
            switch (lifecycleImpact) {
                case ADDING:
                    if (mFinalState == State.REMOVED) {
                        // Applying an ADDING operation to a REMOVED fragment
                        // moves it back to ADDING
                        mFinalState = State.VISIBLE;
                        mLifecycleImpact = LifecycleImpact.ADDING;
                    }
                    break;
                case REMOVING:
                    // Any REMOVING operation overrides whatever we had before
                    mFinalState = State.REMOVED;
                    mLifecycleImpact = LifecycleImpact.REMOVING;
                    break;
                case NONE:
                    // This is a hide or show operation
                    if (mFinalState != State.REMOVED) {
                        mFinalState = finalState;
                    }
            }
            // Connect the CancellationSignal to our own
            cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    mCancellationSignal.cancel();
                }
            });
        }

        final void addCompletionListener(@NonNull Runnable listener) {
            mCompletionListeners.add(listener);
        }

        /**
         * Mark this Operation as complete. This should only be called when all
         * special effects associated with this Operation have completed successfully.
         */
        @CallSuper
        public void complete() {
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
        public void complete() {
            super.complete();
            mFragmentStateManager.moveToExpectedState();
        }
    }
}
