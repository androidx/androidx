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

    SpecialEffectsController(@NonNull ViewGroup container) {
        mContainer = container;
    }

    @NonNull
    public ViewGroup getContainer() {
        return mContainer;
    }

    /**
     * Checks what {@link Operation.Type type} of special effect for the given
     * FragmentStateManager is still awaiting completion (or cancellation).
     * <p>
     * This could be because the Operation is still pending (and
     * {@link #executePendingOperations()} hasn't been called) or because the
     * controller hasn't called {@link Operation#complete()}.
     *
     * @param fragmentStateManager the FragmentStateManager to check for
     * @return The {@link Operation.Type} of the awaiting Operation, or null if there is
     * no special effects still in progress.
     */
    @Nullable
    Operation.Type getAwaitingCompletionType(@NonNull FragmentStateManager fragmentStateManager) {
        Operation operation = mAwaitingCompletionOperations.get(
                fragmentStateManager.getFragment());
        if (operation != null) {
            return operation.getType();
        }
        return null;
    }

    void enqueueAdd(@NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(Operation.Type.ADD, fragmentStateManager, cancellationSignal);
    }

    void enqueueRemove(@NonNull FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        enqueue(Operation.Type.REMOVE, fragmentStateManager, cancellationSignal);
    }

    private void enqueue(@NonNull Operation.Type type,
            @NonNull final FragmentStateManager fragmentStateManager,
            @NonNull CancellationSignal cancellationSignal) {
        if (cancellationSignal.isCanceled()) {
            // Ignore enqueue operations that are already cancelled
            return;
        }
        synchronized (mPendingOperations) {
            final CancellationSignal signal = new CancellationSignal();
            final FragmentStateManagerOperation operation = new FragmentStateManagerOperation(
                    type, fragmentStateManager, signal);
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
                    mAwaitingCompletionOperations.remove(operation.getFragment());
                }
            });
        }
    }

    void updateOperationDirection(boolean isPop) {
        mOperationDirectionIsPop = isPop;
    }

    void executePendingOperations() {
        synchronized (mPendingOperations) {
            executeOperations(new ArrayList<>(mPendingOperations), mOperationDirectionIsPop);
            mPendingOperations.clear();
            mOperationDirectionIsPop = false;
        }
    }

    void cancelAllOperations() {
        synchronized (mPendingOperations) {
            for (Operation operation : mAwaitingCompletionOperations.values()) {
                operation.getCancellationSignal().cancel();
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
         * The type of operation
         */
        enum Type {
            /**
             * An ADD operation indicates that the Fragment should be added to the
             * {@link Operation#getContainer() container} and any "enter" special effects
             * should be run before calling {@link #complete()}.
             */
            ADD,
            /**
             * A REMOVE operation indicates that the Fragment should be removed to the
             * {@link Operation#getContainer() container} and any "exit" special effects
             * should be run before calling {@link #complete()}.
             */
            REMOVE
        }

        @NonNull
        private final Type mType;
        @NonNull
        private final Fragment mFragment;
        @NonNull
        private final CancellationSignal mCancellationSignal;
        @NonNull
        private final List<Runnable> mCompletionListeners = new ArrayList<>();

        /**
         * Construct a new Operation.
         *
         * @param type What type of operation this is.
         * @param fragment The Fragment being added / removed.
         * @param cancellationSignal A signal for handling cancellation
         */
        Operation(@NonNull Type type, @NonNull Fragment fragment,
                @NonNull CancellationSignal cancellationSignal) {
            mType = type;
            mFragment = fragment;
            mCancellationSignal = cancellationSignal;
        }

        /**
         * Returns what type of operation this is.
         *
         * @return the type of operation
         */
        @NonNull
        public final Type getType() {
            return mType;
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

        FragmentStateManagerOperation(@NonNull Type type,
                @NonNull FragmentStateManager fragmentStateManager,
                @NonNull CancellationSignal cancellationSignal) {
            super(type, fragmentStateManager.getFragment(), cancellationSignal);
            mFragmentStateManager = fragmentStateManager;
        }

        @Override
        public void complete() {
            super.complete();
            mFragmentStateManager.moveToExpectedState();
        }
    }
}
