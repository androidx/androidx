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
package androidx.fragment.app

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.annotation.CallSuper
import androidx.fragment.R
import androidx.fragment.app.SpecialEffectsController.Operation.State.Companion.asOperationState

/**
 * Controller for all "special effects" (such as Animation, Animator, framework Transition, and
 * AndroidX Transition) that can be applied to a Fragment as part of the addition or removal of that
 * Fragment from its container.
 *
 * Each SpecialEffectsController is responsible for a single [ViewGroup] container.
 */
internal abstract class SpecialEffectsController(val container: ViewGroup) {
    private val pendingOperations = mutableListOf<Operation>()
    private val runningOperations = mutableListOf<Operation>()
    private var runningNonSeekableTransition = false
    private var operationDirectionIsPop = false
    private var isContainerPostponed = false

    /**
     * Checks what [lifecycle impact][Operation.LifecycleImpact] of special effect for the given
     * FragmentStateManager is still awaiting completion (or cancellation).
     *
     * This could be because the Operation is still pending (and [executePendingOperations] hasn't
     * been called).
     *
     * @param fragmentStateManager the FragmentStateManager to check for
     * @return The [Operation.LifecycleImpact] of the awaiting Operation, or null if there is no
     *   special effects still in progress.
     */
    fun getAwaitingCompletionLifecycleImpact(
        fragmentStateManager: FragmentStateManager
    ): Operation.LifecycleImpact? {
        val fragment = fragmentStateManager.fragment
        val pendingLifecycleImpact = findPendingOperation(fragment)?.lifecycleImpact
        val runningLifecycleImpact = findRunningOperation(fragment)?.lifecycleImpact
        // Only use the running operation if the pending operation is null or NONE
        return when (pendingLifecycleImpact) {
            null -> runningLifecycleImpact
            Operation.LifecycleImpact.NONE -> runningLifecycleImpact
            else -> pendingLifecycleImpact
        }
    }

    private fun findPendingOperation(fragment: Fragment) =
        pendingOperations.firstOrNull { operation ->
            operation.fragment == fragment && !operation.isCanceled
        }

    private fun findRunningOperation(fragment: Fragment) =
        runningOperations.firstOrNull { operation ->
            operation.fragment == fragment && !operation.isCanceled
        }

    fun enqueueAdd(finalState: Operation.State, fragmentStateManager: FragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Enqueuing add operation for fragment " +
                    fragmentStateManager.fragment
            )
        }
        enqueue(finalState, Operation.LifecycleImpact.ADDING, fragmentStateManager)
    }

    fun enqueueShow(fragmentStateManager: FragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Enqueuing show operation for fragment " +
                    fragmentStateManager.fragment
            )
        }
        enqueue(Operation.State.VISIBLE, Operation.LifecycleImpact.NONE, fragmentStateManager)
    }

    fun enqueueHide(fragmentStateManager: FragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Enqueuing hide operation for fragment " +
                    fragmentStateManager.fragment
            )
        }
        enqueue(Operation.State.GONE, Operation.LifecycleImpact.NONE, fragmentStateManager)
    }

    fun enqueueRemove(fragmentStateManager: FragmentStateManager) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Enqueuing remove operation for fragment " +
                    fragmentStateManager.fragment
            )
        }
        enqueue(Operation.State.REMOVED, Operation.LifecycleImpact.REMOVING, fragmentStateManager)
    }

    private fun enqueue(
        finalState: Operation.State,
        lifecycleImpact: Operation.LifecycleImpact,
        fragmentStateManager: FragmentStateManager
    ) {
        synchronized(pendingOperations) {
            val existingOperation =
                findPendingOperation(fragmentStateManager.fragment)
                    // Get the running operation if the fragment is current transitioning as that
                    // means
                    // we can reverse the effect via the merge if needed.
                    ?: if (fragmentStateManager.fragment.mTransitioning) {
                        findRunningOperation(fragmentStateManager.fragment)
                    } else {
                        null
                    }
            if (existingOperation != null) {
                // Update the existing operation by merging in the new information
                // rather than creating a new Operation entirely
                existingOperation.mergeWith(finalState, lifecycleImpact)
                return
            }
            val operation =
                FragmentStateManagerOperation(finalState, lifecycleImpact, fragmentStateManager)
            pendingOperations.add(operation)
            // Ensure that we still run the applyState() call for pending operations
            operation.addCompletionListener {
                if (pendingOperations.contains(operation)) {
                    operation.finalState.applyState(operation.fragment.mView, container)
                }
            }
            // Ensure that we remove the Operation from the list of
            // operations when the operation is complete
            operation.addCompletionListener {
                pendingOperations.remove(operation)
                runningOperations.remove(operation)
            }
        }
    }

    fun updateOperationDirection(isPop: Boolean) {
        operationDirectionIsPop = isPop
    }

    fun markPostponedState() {
        synchronized(pendingOperations) {
            updateFinalState()
            val lastEnteringFragment =
                pendingOperations
                    .lastOrNull { operation ->
                        // Only consider operations with entering transitions
                        val currentState = operation.fragment.mView.asOperationState()
                        operation.finalState == Operation.State.VISIBLE &&
                            currentState != Operation.State.VISIBLE
                    }
                    ?.fragment
            // The container is considered postponed if the Fragment
            // associated with the last entering Operation is postponed
            isContainerPostponed = lastEnteringFragment?.isPostponed ?: false
        }
    }

    fun isPendingExecute(): Boolean {
        return pendingOperations.isNotEmpty()
    }

    fun forcePostponedExecutePendingOperations() {
        if (isContainerPostponed) {
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, "SpecialEffectsController: Forcing postponed operations")
            }
            isContainerPostponed = false
            executePendingOperations()
        }
    }

    fun executePendingOperations() {
        if (isContainerPostponed) {
            // No operations should execute while the container is postponed
            return
        }
        // If the container is not attached to the window, ignore the special effect
        // since none of the special effect systems will run them anyway.
        if (!container.isAttachedToWindow()) {
            forceCompleteAllOperations()
            operationDirectionIsPop = false
            return
        }
        synchronized(pendingOperations) {
            val currentlyRunningOperations = runningOperations.toMutableList()
            runningOperations.clear()
            // If we have no pendingOperations, we should always cancel without seeking,
            // otherwise, we should check if the fragment has mTransitioning set.
            for (operation in currentlyRunningOperations) {
                operation.isSeeking =
                    pendingOperations.isNotEmpty() && operation.fragment.mTransitioning
            }
            for (operation in currentlyRunningOperations) {
                // Another operation is about to run while we already have operations running
                // There are 2 cases that need to be handled:
                // 1. The previous running operations were transitioning, but not seeking. Here
                // we were holding the animation until we the gesture was committed so we never
                // started the effects and need to complete immediately.
                // 2. The previous running operations were either transitioning and seeking, or
                // not transitioning at all. In this case we are guaranteed to have starting the
                // effect so we can just call cancel, passing in the transitioning status.
                if (runningNonSeekableTransition) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "SpecialEffectsController: Completing non-seekable " +
                                "operation $operation"
                        )
                    }
                    operation.complete()
                } else {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "SpecialEffectsController: Cancelling operation $operation"
                        )
                    }
                    operation.cancel(container)
                }
                runningNonSeekableTransition = false
                if (!operation.isComplete) {
                    // Re-add any animations that didn't synchronously call complete()
                    // to continue to track them as running operations
                    runningOperations.add(operation)
                }
            }
            if (pendingOperations.isNotEmpty()) {
                updateFinalState()
                val newPendingOperations = pendingOperations.toMutableList()
                if (newPendingOperations.isEmpty()) {
                    return
                }
                pendingOperations.clear()
                runningOperations.addAll(newPendingOperations)
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "SpecialEffectsController: Executing pending operations"
                    )
                }
                collectEffects(newPendingOperations, operationDirectionIsPop)
                val seekable = isOperationSeekable(newPendingOperations)
                val transitioning = isOperationTransitioning(newPendingOperations)
                runningNonSeekableTransition = transitioning && !seekable

                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "SpecialEffectsController: Operation seekable = $seekable \n" +
                            "transition = $transitioning"
                    )
                }

                if (!transitioning) {
                    processStart(newPendingOperations)
                    commitEffects(newPendingOperations)
                } else {
                    if (seekable) {
                        processStart(newPendingOperations)
                        for (i in newPendingOperations.indices) {
                            val operation = newPendingOperations[i]
                            applyContainerChangesToOperation(operation)
                        }
                    }
                }
                operationDirectionIsPop = false
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "SpecialEffectsController: Finished executing pending operations"
                    )
                }
            }
        }
    }

    private fun isOperationTransitioning(newPendingOperations: MutableList<Operation>): Boolean {
        var transitioning = true
        newPendingOperations.forEach { operation ->
            if (!operation.fragment.mTransitioning) {
                transitioning = false
            }
        }
        return transitioning
    }

    private fun isOperationSeekable(newPendingOperations: MutableList<Operation>): Boolean {
        var seekable = true
        newPendingOperations.forEach { operation ->
            seekable =
                operation.effects.isNotEmpty() &&
                    operation.effects.all { effect -> effect.isSeekingSupported }
        }
        seekable = seekable && newPendingOperations.flatMap { it.effects }.isNotEmpty()
        return seekable
    }

    internal fun applyContainerChangesToOperation(operation: Operation) {
        if (operation.isAwaitingContainerChanges) {
            operation.finalState.applyState(operation.fragment.requireView(), container)
            operation.isAwaitingContainerChanges = false
        }
    }

    fun forceCompleteAllOperations() {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Forcing all operations to complete"
            )
        }
        val attachedToWindow = container.isAttachedToWindow()
        synchronized(pendingOperations) {
            updateFinalState()
            processStart(pendingOperations)

            // First cancel running operations
            val runningOperations = runningOperations.toMutableList()
            for (operation in runningOperations) {
                operation.isSeeking = false
            }
            for (operation in runningOperations) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    val notAttachedMessage =
                        if (attachedToWindow) {
                            ""
                        } else {
                            "Container $container is not attached to window. "
                        }
                    Log.v(
                        FragmentManager.TAG,
                        "SpecialEffectsController: " +
                            notAttachedMessage +
                            "Cancelling running operation $operation"
                    )
                }
                operation.cancel(container)
            }

            // Then cancel pending operations
            val pendingOperations = pendingOperations.toMutableList()
            for (operation in pendingOperations) {
                operation.isSeeking = false
            }
            for (operation in pendingOperations) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    val notAttachedMessage =
                        if (attachedToWindow) {
                            ""
                        } else {
                            "Container $container is not attached to window. "
                        }
                    Log.v(
                        FragmentManager.TAG,
                        "SpecialEffectsController: " +
                            notAttachedMessage +
                            "Cancelling pending operation $operation"
                    )
                }
                operation.cancel(container)
            }
        }
    }

    private fun updateFinalState() {
        for (operation in pendingOperations) {
            // update the final state of adding operations
            if (operation.lifecycleImpact == Operation.LifecycleImpact.ADDING) {
                val fragment = operation.fragment
                val view = fragment.requireView()
                val finalState = Operation.State.from(view.visibility)
                operation.mergeWith(finalState, Operation.LifecycleImpact.NONE)
            }
        }
    }

    /**
     * Collect all of the given operations.
     *
     * If there are no special effects for a given operation, the SpecialEffectsController should
     * call [Operation.complete].
     *
     * @param operations the list of operations to execute in order.
     * @param isPop whether this set of operations should be considered as triggered by a 'pop'.
     *   This can be used to control the direction of any special effects if they are not symmetric.
     */
    abstract fun collectEffects(operations: List<@JvmSuppressWildcards Operation>, isPop: Boolean)

    /**
     * Commit all of the given operations.
     *
     * This commits all of the effects of the operations. When the last started special effect is
     * completed, [Operation.completeEffect] will call [Operation.complete] automatically.
     *
     * @param operations the list of operations to execute in order.
     */
    internal open fun commitEffects(operations: List<@JvmSuppressWildcards Operation>) {
        val set = operations.flatMap { it.effects }.toSet().toList()

        // Commit all of the Animation, Animator, Transition and NoOp Effects we have collected
        for (i in set.indices) {
            val effect = set[i]
            effect.onCommit(container)
        }

        for (i in operations.indices) {
            val operation = operations[i]
            applyContainerChangesToOperation(operation)
        }

        // Making a copy cause complete modifies the list.
        val operationsCopy = operations.toList()
        for (i in operationsCopy.indices) {
            val operation = operationsCopy[i]
            if (operation.effects.isEmpty()) {
                operation.complete()
            }
        }
    }

    private fun processStart(operations: List<@JvmSuppressWildcards Operation>) {
        for (i in operations.indices) {
            val operation = operations[i]
            operation.onStart()
        }
        val set = operations.flatMap { it.effects }.toSet().toList()
        // Start all of the Animation, Animator, Transition and NoOp Effects we have collected
        for (j in set.indices) {
            val effect = set[j]
            effect.performStart(container)
        }
    }

    fun processProgress(backEvent: BackEventCompat) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(
                FragmentManager.TAG,
                "SpecialEffectsController: Processing Progress ${backEvent.progress}"
            )
        }

        val set = runningOperations.flatMap { it.effects }.toSet().toList()
        for (j in set.indices) {
            val effect = set[j]
            effect.onProgress(backEvent, container)
        }
    }

    fun completeBack() {
        if (FragmentManager.isLoggingEnabled(Log.DEBUG)) {
            Log.d(FragmentManager.TAG, "SpecialEffectsController: Completing Back ")
        }
        processStart(runningOperations)
        commitEffects(runningOperations)
    }

    /**
     * Class representing an ongoing special effects operation.
     *
     * @see collectEffects
     */
    internal open class Operation(
        /** The final state after this operation should be. */
        var finalState: State,
        /** How this Operation affects the lifecycle of the fragment. */
        var lifecycleImpact: LifecycleImpact,
        /** The Fragment being added / removed. */
        val fragment: Fragment,
    ) {
        /**
         * The state that the fragment's View should be in after applying this operation.
         *
         * @see applyState
         */
        internal enum class State {
            /** The fragment's view should be completely removed from the container. */
            REMOVED,
            /** The fragment's view should be made [View.VISIBLE]. */
            VISIBLE,
            /** The fragment's view should be made [View.GONE]. */
            GONE,
            /** The fragment's view should be made [View.INVISIBLE]. */
            INVISIBLE;

            /**
             * Applies this state to the given View.
             *
             * @param view The View to apply this state to.
             * @param container The ViewGroup to add the view too if it does not have a parent.
             */
            fun applyState(view: View, container: ViewGroup) {
                when (this) {
                    REMOVED -> {
                        val parent = view.parent as? ViewGroup
                        if (parent != null) {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(
                                    FragmentManager.TAG,
                                    "SpecialEffectsController: " +
                                        "Removing view $view from container $parent"
                                )
                            }
                            parent.removeView(view)
                        }
                    }
                    VISIBLE -> {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: " + "Setting view $view to VISIBLE"
                            )
                        }
                        val parent = view.parent as? ViewGroup
                        if (parent == null) {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(
                                    FragmentManager.TAG,
                                    "SpecialEffectsController: " +
                                        "Adding view $view to Container $container"
                                )
                            }
                            container.addView(view)
                        }
                        view.visibility = View.VISIBLE
                    }
                    GONE -> {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: Setting view $view to GONE"
                            )
                        }
                        view.visibility = View.GONE
                    }
                    INVISIBLE -> {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: Setting view $view to INVISIBLE"
                            )
                        }
                        view.visibility = View.INVISIBLE
                    }
                }
            }

            companion object {
                /** Create a new State from the [view's visibility][View.getVisibility]. */
                fun View.asOperationState() =
                    if (alpha == 0f && visibility == View.VISIBLE) {
                        // We should consider views with an alpha of 0 as INVISIBLE.
                        INVISIBLE
                    } else {
                        from(visibility)
                    }

                /**
                 * Create a new State from the visibility of a View.
                 *
                 * @param visibility The visibility constant to translate into a State.
                 * @return A new State from the visibility.
                 */
                @JvmStatic
                fun from(visibility: Int): State {
                    return when (visibility) {
                        View.VISIBLE -> VISIBLE
                        View.INVISIBLE -> INVISIBLE
                        View.GONE -> GONE
                        else -> throw IllegalArgumentException("Unknown visibility $visibility")
                    }
                }
            }
        }

        /** The impact that this operation has on the lifecycle of the fragment. */
        internal enum class LifecycleImpact {
            /** No impact on the fragment's lifecycle. */
            NONE,
            /** This operation is associated with adding a fragment. */
            ADDING,
            /** This operation is associated with removing a fragment. */
            REMOVING,
        }

        private val completionListeners = mutableListOf<Runnable>()
        var isCanceled = false
            private set

        var isComplete = false
            private set

        var isSeeking = false
            internal set

        var isStarted = false
            private set

        var isAwaitingContainerChanges = true

        private val _effects = mutableListOf<Effect>()

        internal val effects: List<Effect> = _effects

        override fun toString(): String {
            val identityHash = Integer.toHexString(System.identityHashCode(this))
            return "Operation {$identityHash} {" +
                "finalState = $finalState " +
                "lifecycleImpact = $lifecycleImpact " +
                "fragment = $fragment}"
        }

        fun cancel(container: ViewGroup) {
            isStarted = false
            if (isCanceled) {
                return
            }
            isCanceled = true
            if (_effects.isEmpty()) {
                complete()
            } else {
                effects.toList().forEach { it.cancel(container) }
            }
        }

        fun mergeWith(finalState: State, lifecycleImpact: LifecycleImpact) {
            when (lifecycleImpact) {
                LifecycleImpact.ADDING ->
                    if (this.finalState == State.REMOVED) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: For fragment $fragment " +
                                    "mFinalState = REMOVED -> VISIBLE. " +
                                    "mLifecycleImpact = ${this.lifecycleImpact} to ADDING."
                            )
                        }
                        // Applying an ADDING operation to a REMOVED fragment
                        // moves it back to ADDING
                        this.finalState = State.VISIBLE
                        this.lifecycleImpact = LifecycleImpact.ADDING
                        this.isAwaitingContainerChanges = true
                    }
                LifecycleImpact.REMOVING -> {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "SpecialEffectsController: For fragment $fragment " +
                                "mFinalState = ${this.finalState} -> REMOVED. " +
                                "mLifecycleImpact  = ${this.lifecycleImpact} to REMOVING."
                        )
                    }
                    // Any REMOVING operation overrides whatever we had before
                    this.finalState = State.REMOVED
                    this.lifecycleImpact = LifecycleImpact.REMOVING
                    this.isAwaitingContainerChanges = true
                }
                LifecycleImpact.NONE -> // This is a hide or show operation
                if (this.finalState != State.REMOVED) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: For fragment $fragment " +
                                    "mFinalState = ${this.finalState} -> $finalState."
                            )
                        }
                        this.finalState = finalState
                    }
            }
        }

        fun addCompletionListener(listener: Runnable) {
            completionListeners.add(listener)
        }

        fun addEffect(effect: Effect) {
            _effects.add(effect)
        }

        fun completeEffect(effect: Effect) {
            if (_effects.remove(effect) && _effects.isEmpty()) {
                complete()
            }
        }

        /** Callback for when the operation is about to start. */
        @CallSuper
        open fun onStart() {
            isStarted = true
        }

        /**
         * Mark this Operation as complete. This should only be called when all special effects
         * associated with this Operation have completed successfully.
         */
        @CallSuper
        internal open fun complete() {
            isStarted = false
            if (isComplete) {
                return
            }
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, "SpecialEffectsController: $this has called complete.")
            }
            isComplete = true
            completionListeners.forEach { listener -> listener.run() }
        }
    }

    private class FragmentStateManagerOperation(
        finalState: State,
        lifecycleImpact: LifecycleImpact,
        private val fragmentStateManager: FragmentStateManager,
    ) :
        Operation(
            finalState,
            lifecycleImpact,
            fragmentStateManager.fragment,
        ) {
        override fun onStart() {
            if (isStarted) {
                return
            }
            super.onStart()
            if (lifecycleImpact == LifecycleImpact.ADDING) {
                val fragment = fragmentStateManager.fragment
                val focusedView = fragment.mView.findFocus()
                if (focusedView != null) {
                    fragment.focusedView = focusedView
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "requestFocus: Saved focused view $focusedView for Fragment $fragment"
                        )
                    }
                }
                val view = this.fragment.requireView()
                // We need to ensure that the fragment's view is re-added
                // for ADDING operations to properly handle cases where the
                // exit animation was interrupted.
                if (view.parent == null) {
                    fragmentStateManager.addViewToContainer()
                    view.alpha = 0f
                }
                // Change the view alphas back to their original values before we execute our
                // transitions.
                if (view.alpha == 0f && view.visibility == View.VISIBLE) {
                    view.visibility = View.INVISIBLE
                }
                view.alpha = fragment.postOnViewCreatedAlpha
            } else if (lifecycleImpact == LifecycleImpact.REMOVING) {
                val fragment = fragmentStateManager.fragment
                val view = fragment.requireView()
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Clearing focus ${view.findFocus()} on view $view for Fragment $fragment"
                    )
                }
                view.clearFocus()
            }
        }

        override fun complete() {
            super.complete()
            // Since we are completing, ensure that the transitioning flag is set to false before
            // we move to state
            fragment.mTransitioning = false
            fragmentStateManager.moveToExpectedState()
        }
    }

    internal open class Effect {
        open val isSeekingSupported = false

        private var isStarted = false

        private var isCancelled = false

        fun performStart(container: ViewGroup) {
            if (!isStarted) {
                onStart(container)
            }
            isStarted = true
        }

        open fun onStart(container: ViewGroup) {}

        open fun onProgress(backEvent: BackEventCompat, container: ViewGroup) {}

        open fun onCommit(container: ViewGroup) {}

        fun cancel(container: ViewGroup) {
            if (!isCancelled) {
                onCancel(container)
            }
            isCancelled = true
        }

        open fun onCancel(container: ViewGroup) {}
    }

    companion object {
        /**
         * Get the [SpecialEffectsController] for a given container if it already exists or create
         * it. This will automatically find the containing FragmentManager and use the factory
         * provided by [FragmentManager.getSpecialEffectsControllerFactory].
         *
         * @param container ViewGroup to find the associated SpecialEffectsController for.
         * @return a SpecialEffectsController for the given container
         */
        @JvmStatic
        fun getOrCreateController(
            container: ViewGroup,
            fragmentManager: FragmentManager
        ): SpecialEffectsController {
            val factory = fragmentManager.specialEffectsControllerFactory
            return getOrCreateController(container, factory)
        }

        /**
         * Get the [SpecialEffectsController] for a given container if it already exists or create
         * it using the given [SpecialEffectsControllerFactory] if it does not.
         *
         * @param container ViewGroup to find the associated SpecialEffectsController for.
         * @param factory The factory to use to create a new SpecialEffectsController if one does
         *   not already exist for this container.
         * @return a SpecialEffectsController for the given container
         */
        @JvmStatic
        fun getOrCreateController(
            container: ViewGroup,
            factory: SpecialEffectsControllerFactory
        ): SpecialEffectsController {
            val controller = container.getTag(R.id.special_effects_controller_view_tag)
            if (controller is SpecialEffectsController) {
                return controller
            }
            // Else, create a new SpecialEffectsController
            val newController = factory.createController(container)
            container.setTag(R.id.special_effects_controller_view_tag, newController)
            return newController
        }
    }
}
