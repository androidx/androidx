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

import androidx.annotation.NonNull;
import androidx.fragment.R;

import java.util.ArrayList;
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
            @NonNull ViewGroup container) {
        FragmentManager fragmentManager = FragmentManager.findFragmentManager(container);
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

    SpecialEffectsController(@NonNull ViewGroup container) {
        mContainer = container;
    }

    @NonNull
    public ViewGroup getContainer() {
        return mContainer;
    }

    void enqueueAdd(@NonNull FragmentStateManager fragmentStateManager) {
        enqueue(Operation.Type.ADD, fragmentStateManager);
    }

    void enqueueRemove(@NonNull FragmentStateManager fragmentStateManager) {
        enqueue(Operation.Type.REMOVE, fragmentStateManager);
    }

    private void enqueue(@NonNull Operation.Type type,
            @NonNull FragmentStateManager fragmentStateManager) {
        synchronized (mPendingOperations) {
            final FragmentStateManagerOperation operation = new FragmentStateManagerOperation(
                    type, fragmentStateManager);
            mPendingOperations.add(operation);
        }
    }

    void executePendingOperations() {
        synchronized (mPendingOperations) {
            executeOperations(new ArrayList<>(mPendingOperations));
            mPendingOperations.clear();
        }
    }

    /**
     * Execute all of the given operations.
     * <p>
     * At a minimum, the SpecialEffectsController should call
     * {@link Operation#complete()} on each operation when all of the special effects
     * for the given Operation are complete.
     *
     * @param operations the list of operations to execute in order.
     */
    abstract void executeOperations(@NonNull List<Operation> operations);

    /**
     * Class representing an ongoing special effects operation.
     *
     * @see #executeOperations(List)
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

        /**
         * Construct a new Operation.
         *
         * @param type What type of operation this is.
         * @param fragment The Fragment being added / removed.
         */
        Operation(@NonNull Type type, @NonNull Fragment fragment) {
            mType = type;
            mFragment = fragment;
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
         * Mark this Operation as complete. This should only be called when all
         * special effects associated with this Operation have completed successfully.
         */
        public void complete() {
        }
    }

    private static class FragmentStateManagerOperation extends Operation {
        @NonNull
        private final FragmentStateManager mFragmentStateManager;

        FragmentStateManagerOperation(@NonNull Type type,
                @NonNull FragmentStateManager fragmentStateManager) {
            super(type, fragmentStateManager.getFragment());
            mFragmentStateManager = fragmentStateManager;
        }

        @Override
        public void complete() {
            mFragmentStateManager.moveToExpectedState();
        }
    }
}
