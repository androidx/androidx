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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.core.os.CancellationSignal
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.FragmentTransition.callSharedElementStartEnd
import androidx.fragment.app.FragmentTransition.findKeyForValue
import androidx.fragment.app.FragmentTransition.retainValues
import androidx.fragment.app.FragmentTransition.setViewVisibility
import androidx.fragment.app.SpecialEffectsController.Operation.State.Companion.asOperationState

/**
 * A SpecialEffectsController that hooks into the existing Fragment APIs to run
 * animations and transitions.
 */
internal class DefaultSpecialEffectsController(
    container: ViewGroup
) : SpecialEffectsController(container) {
    override fun executeOperations(operations: List<Operation>, isPop: Boolean) {
        // Shared element transitions are done between the first fragment leaving and
        // the last fragment coming in. Finding these operations is the first priority
        val firstOut = operations.firstOrNull { operation ->
            val currentState = operation.fragment.mView.asOperationState()
            // The firstOut Operation is the first Operation moving from VISIBLE
            currentState == Operation.State.VISIBLE &&
                operation.finalState != Operation.State.VISIBLE
        }
        val lastIn = operations.lastOrNull { operation ->
            val currentState = operation.fragment.mView.asOperationState()
            // The last Operation that moves to VISIBLE is the lastIn Operation
            currentState != Operation.State.VISIBLE &&
                operation.finalState == Operation.State.VISIBLE
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Executing operations from $firstOut to $lastIn")
        }

        // Now iterate through the operations, collecting the set of animations
        // and transitions that need to be executed
        val animations = mutableListOf<AnimationInfo>()
        val transitions = mutableListOf<TransitionInfo>()
        val awaitingContainerChanges = operations.toMutableList()

        // sync animations together before we start loading them.
        syncAnimations(operations)
        for (operation: Operation in operations) {
            // Create the animation CancellationSignal
            val animCancellationSignal = CancellationSignal()
            operation.markStartedSpecialEffect(animCancellationSignal)
            // Add the animation special effect
            animations.add(AnimationInfo(operation, animCancellationSignal, isPop))

            // Create the transition CancellationSignal
            val transitionCancellationSignal = CancellationSignal()
            operation.markStartedSpecialEffect(transitionCancellationSignal)
            // Add the transition special effect
            transitions.add(TransitionInfo(operation, transitionCancellationSignal, isPop,
                    if (isPop) operation === firstOut else operation === lastIn))

            // Ensure that if the Operation is synchronously complete, we still
            // apply the container changes before the Operation completes
            operation.addCompletionListener {
                if (awaitingContainerChanges.contains(operation)) {
                    awaitingContainerChanges.remove(operation)
                    applyContainerChanges(operation)
                }
            }
        }

        // Start transition special effects
        val startedTransitions = startTransitions(transitions, awaitingContainerChanges, isPop,
            firstOut, lastIn)
        val startedAnyTransition = startedTransitions.containsValue(true)

        // Start animation special effects
        startAnimations(animations, awaitingContainerChanges, startedAnyTransition,
            startedTransitions)
        for (operation: Operation in awaitingContainerChanges) {
            applyContainerChanges(operation)
        }
        awaitingContainerChanges.clear()
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG,
                "Completed executing operations from $firstOut to $lastIn")
        }
    }

    /**
     * Syncs the animations of all other operations with the animations of the last operation.
     */
    private fun syncAnimations(operations: List<Operation>) {
        // get the last operation's fragment
        val lastOpFragment = operations.last().fragment
        // change the animations of all other fragments to match the last one.
        for (operation: Operation in operations) {
            operation.fragment.mAnimationInfo.mEnterAnim = lastOpFragment.mAnimationInfo.mEnterAnim
            operation.fragment.mAnimationInfo.mExitAnim = lastOpFragment.mAnimationInfo.mExitAnim
            operation.fragment.mAnimationInfo.mPopEnterAnim =
                lastOpFragment.mAnimationInfo.mPopEnterAnim
            operation.fragment.mAnimationInfo.mPopExitAnim =
                lastOpFragment.mAnimationInfo.mPopExitAnim
        }
    }

    @SuppressLint("NewApi", "PrereleaseSdkCoreDependency")
    private fun startAnimations(
        animationInfos: List<AnimationInfo>,
        awaitingContainerChanges: MutableList<Operation>,
        startedAnyTransition: Boolean,
        startedTransitions: Map<Operation, Boolean>
    ) {
        val context = container.context
        val animationsToRun = mutableListOf<AnimationInfo>()

        // First run Animators
        var startedAnyAnimator = false
        for (animationInfo: AnimationInfo in animationInfos) {
            if (animationInfo.isVisibilityUnchanged) {
                // No change in visibility, so we can immediately complete the animation
                animationInfo.completeSpecialEffect()
                continue
            }
            val anim = animationInfo.getAnimation(context)
            if (anim == null) {
                // No Animator or Animation, so we can immediately complete the animation
                animationInfo.completeSpecialEffect()
                continue
            }
            val animator = anim.animator
            if (animator == null) {
                // We must have an Animation to run. Save those for a second pass
                animationsToRun.add(animationInfo)
                continue
            }

            // First make sure we haven't already started a Transition for this Operation
            val operation: Operation = animationInfo.operation
            val fragment = operation.fragment
            val startedTransition = startedTransitions[operation] == true
            if (startedTransition) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                        "Ignoring Animator set on $fragment as this Fragment was involved " +
                            "in a Transition.")
                }
                animationInfo.completeSpecialEffect()
                continue
            }

            // Okay, let's run the Animator!
            startedAnyAnimator = true
            val isHideOperation = operation.finalState === Operation.State.GONE
            if (isHideOperation) {
                // We don't want to immediately applyState() to hide operations as that
                // immediately stops the Animator. Instead we'll applyState() manually
                // when the Animator ends.
                awaitingContainerChanges.remove(operation)
            }
            val viewToAnimate = fragment.mView
            container.startViewTransition(viewToAnimate)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(anim: Animator) {
                    container.endViewTransition(viewToAnimate)
                    if (isHideOperation) {
                        // Specifically for hide operations with Animator, we can't
                        // applyState until the Animator finishes
                        operation.finalState.applyState(viewToAnimate)
                    }
                    animationInfo.completeSpecialEffect()
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG,
                            "Animator from operation $operation has ended.")
                    }
                }
            })
            animator.setTarget(viewToAnimate)
            if (Build.VERSION.SDK_INT >= 34 && operation.fragment.mTransitioning) {
                val animatorSet = animationInfo.getAnimation(container.context)?.animator
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Adding BackProgressCallbacks for Animators to operation $operation"
                    )
                }
                operation.addBackProgressCallbacks({ backEvent ->
                    if (animatorSet != null) {
                        val totalDuration = Api24Impl.totalDuration(animatorSet)
                        var time = (backEvent.progress * totalDuration).toLong()
                        // We cannot let the time get to 0 or the totalDuration to avoid
                        // completing the operation accidentally.
                        if (time == 0L) {
                            time = 1L
                        }
                        if (time == totalDuration) {
                            time = totalDuration - 1
                        }
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "Setting currentPlayTime to $time for Animator $animatorSet on " +
                                    "operation $operation"
                            )
                        }
                        Api26Impl.setCurrentPlayTime(animatorSet, time)
                    }
                }) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "Back Progress Callback Animator has been started."
                        )
                    }
                    animatorSet?.start()
                }
            } else {
                animator.start()
            }
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG,
                    "Animator from operation $operation has started.")
            }
            // Listen for cancellation and use that to cancel the Animator
            val signal: CancellationSignal = animationInfo.signal
            signal.setOnCancelListener {
                if (operation.isSeeking) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Api26Impl.reverse(animator)
                    }
                } else {
                    animator.end()
                }
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Animator from operation $operation has been canceled" +
                            "${if (operation.isSeeking) " with seeking." else "."} "
                    )
                }
            }
        }

        // Now run Animations
        for (animationInfo: AnimationInfo in animationsToRun) {
            // First make sure we haven't already started any Transition
            val operation: Operation = animationInfo.operation
            val fragment = operation.fragment
            if (startedAnyTransition) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                        "Ignoring Animation set on $fragment as Animations cannot " +
                            "run alongside Transitions.")
                }
                animationInfo.completeSpecialEffect()
                continue
            }
            // Then make sure we haven't already started any Animator
            if (startedAnyAnimator) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                        "Ignoring Animation set on $fragment as Animations cannot " +
                            "run alongside Animators.")
                }
                animationInfo.completeSpecialEffect()
                continue
            }

            // Okay, let's run the Animation!
            val viewToAnimate = fragment.mView
            val anim = checkNotNull(checkNotNull(animationInfo.getAnimation(context)).animation)
            val finalState = operation.finalState
            if (finalState !== Operation.State.REMOVED) {
                // If the operation does not remove the view, we can't use a
                // AnimationSet due that causing the introduction of visual artifacts (b/163084315).
                viewToAnimate.startAnimation(anim)
                // This means we can't use setAnimationListener() without overriding
                // any listener that the Fragment has set themselves, so we
                // just mark the special effect as complete immediately.
                animationInfo.completeSpecialEffect()
            } else {
                container.startViewTransition(viewToAnimate)
                val animation: Animation = FragmentAnim.EndViewTransitionAnimation(anim,
                    container, viewToAnimate)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG,
                                "Animation from operation $operation has reached " +
                                    "onAnimationStart.")
                        }
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        // onAnimationEnd() comes during draw(), so there can still be some
                        // draw events happening after this call. We don't want to complete the
                        // animation until after the onAnimationEnd()
                        container.post {
                            container.endViewTransition(viewToAnimate)
                            animationInfo.completeSpecialEffect()
                        }
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG,
                                "Animation from operation $operation has ended.")
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
                viewToAnimate.startAnimation(animation)
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                        "Animation from operation $operation has started.")
                }
            }
            // Listen for cancellation and use that to cancel the Animation
            val signal: CancellationSignal = animationInfo.signal
            signal.setOnCancelListener {
                viewToAnimate.clearAnimation()
                container.endViewTransition(viewToAnimate)
                animationInfo.completeSpecialEffect()
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG,
                        "Animation from operation $operation has been cancelled.")
                }
            }
        }
    }

    private fun startTransitions(
        transitionInfos: List<TransitionInfo>,
        awaitingContainerChanges: MutableList<Operation>,
        isPop: Boolean,
        firstOut: Operation?,
        lastIn: Operation?
    ): Map<Operation, Boolean> {
        val startedTransitions = mutableMapOf<Operation, Boolean>()
        // First verify that we can run all transitions together
        val transitionImpl = transitionInfos.filterNot { transitionInfo ->
            // If there is no change in visibility, we can skip the TransitionInfo
            transitionInfo.isVisibilityUnchanged
        }.filter { transitionInfo ->
            transitionInfo.handlingImpl != null
        }.fold(null as FragmentTransitionImpl?) { chosenImpl, transitionInfo ->
            val handlingImpl = transitionInfo.handlingImpl
            require(chosenImpl == null || handlingImpl === chosenImpl) {
                "Mixing framework transitions and AndroidX transitions is not allowed. Fragment " +
                    "${transitionInfo.operation.fragment} returned Transition " +
                    "${transitionInfo.transition} which uses a different Transition " +
                    "type than other Fragments."
            }
            handlingImpl
        }
        if (transitionImpl == null) {
            // There were no transitions at all so we can just complete all of them
            for (transitionInfo: TransitionInfo in transitionInfos) {
                startedTransitions[transitionInfo.operation] = false
                transitionInfo.completeSpecialEffect()
            }
            return startedTransitions
        }

        // Every transition needs to target at least one View so that they
        // don't interfere with one another. This is the view we use
        // in cases where there are no real views to target
        val nonExistentView = View(container.context)

        // Now find the shared element transition if it exists
        var sharedElementTransition: Any? = null
        var firstOutEpicenterView: View? = null
        var hasLastInEpicenter = false
        val lastInEpicenterRect = Rect()
        val sharedElementFirstOutViews = ArrayList<View>()
        val sharedElementLastInViews = ArrayList<View>()
        val sharedElementNameMapping = ArrayMap<String, String>()
        for (transitionInfo: TransitionInfo in transitionInfos) {
            val hasSharedElementTransition = transitionInfo.hasSharedElementTransition()
            // Compute the shared element transition between the firstOut and lastIn Fragments
            if (hasSharedElementTransition && (firstOut != null) && (lastIn != null)) {
                // swapSharedElementTargets requires wrapping this in a TransitionSet
                sharedElementTransition = transitionImpl.wrapTransitionInSet(
                    transitionImpl.cloneTransition(transitionInfo.sharedElementTransition))
                // The exiting shared elements default to the source names from the
                // last in fragment
                val exitingNames = lastIn.fragment.sharedElementSourceNames
                // But if we're doing multiple transactions, we may need to re-map
                // the names from the first out fragment
                val firstOutSourceNames = firstOut.fragment.sharedElementSourceNames
                val firstOutTargetNames = firstOut.fragment.sharedElementTargetNames
                // We do this by iterating through each first out target,
                // seeing if there is a match from the last in sources
                for (index in firstOutTargetNames.indices) {
                    val nameIndex = exitingNames.indexOf(firstOutTargetNames[index])
                    if (nameIndex != -1) {
                        // If we found a match, replace the last in source name
                        // with the first out source name
                        exitingNames[nameIndex] = firstOutSourceNames[index]
                    }
                }
                val enteringNames = lastIn.fragment.sharedElementTargetNames
                val (exitingCallback, enteringCallback) = if (!isPop) {
                    // Forward transitions have firstOut fragment exiting and the
                    // lastIn fragment entering
                    firstOut.fragment.exitTransitionCallback to
                        lastIn.fragment.enterTransitionCallback
                } else {
                    // A pop is the reverse: the firstOut fragment is entering and the
                    // lastIn fragment is exiting
                    firstOut.fragment.enterTransitionCallback to
                        lastIn.fragment.exitTransitionCallback
                }
                val numSharedElements = exitingNames.size
                for (i in 0 until numSharedElements) {
                    val exitingName = exitingNames[i]
                    val enteringName = enteringNames[i]
                    sharedElementNameMapping[exitingName] = enteringName
                }
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG, ">>> entering view names <<<")
                    for (name: String? in enteringNames) {
                        Log.v(FragmentManager.TAG, "Name: $name")
                    }
                    Log.v(FragmentManager.TAG, ">>> exiting view names <<<")
                    for (name: String? in exitingNames) {
                        Log.v(FragmentManager.TAG, "Name: $name")
                    }
                }

                // Find all of the Views from the firstOut fragment that are
                // part of the shared element transition
                val firstOutViews = ArrayMap<String, View>()
                findNamedViews(firstOutViews, firstOut.fragment.mView)
                firstOutViews.retainAll(exitingNames)
                if (exitingCallback != null) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG,
                            "Executing exit callback for operation $firstOut")
                    }
                    // Give the SharedElementCallback a chance to override the default mapping
                    exitingCallback.onMapSharedElements(exitingNames, firstOutViews)
                    for (i in exitingNames.indices.reversed()) {
                        val name = exitingNames[i]
                        val view = firstOutViews[name]
                        if (view == null) {
                            sharedElementNameMapping.remove(name)
                        } else if (name != ViewCompat.getTransitionName(view)) {
                            val targetValue = sharedElementNameMapping.remove(name)
                            sharedElementNameMapping[ViewCompat.getTransitionName(view)] =
                                targetValue
                        }
                    }
                } else {
                    // Only keep the mapping of elements that were found in the firstOut Fragment
                    sharedElementNameMapping.retainAll(firstOutViews.keys)
                }

                // Find all of the Views from the lastIn fragment that are
                // part of the shared element transition
                val lastInViews = ArrayMap<String, View>()
                findNamedViews(lastInViews, lastIn.fragment.mView)
                lastInViews.retainAll(enteringNames)
                lastInViews.retainAll(sharedElementNameMapping.values)
                if (enteringCallback != null) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG,
                            "Executing enter callback for operation $lastIn")
                    }
                    // Give the SharedElementCallback a chance to override the default mapping
                    enteringCallback.onMapSharedElements(enteringNames, lastInViews)
                    for (i in enteringNames.indices.reversed()) {
                        val name = enteringNames[i]
                        val view = lastInViews[name]
                        if (view == null) {
                            val key = sharedElementNameMapping.findKeyForValue(name)
                            if (key != null) {
                                sharedElementNameMapping.remove(key)
                            }
                        } else if (name != ViewCompat.getTransitionName(view)) {
                            val key = sharedElementNameMapping.findKeyForValue(name)
                            if (key != null) {
                                sharedElementNameMapping[key] = ViewCompat.getTransitionName(view)
                            }
                        }
                    }
                } else {
                    // Only keep the mapping of elements that were found in the lastIn Fragment
                    sharedElementNameMapping.retainValues(lastInViews)
                }

                // Now make a final pass through the Views list to ensure they
                // don't still have elements that were removed from the mapping
                firstOutViews.retainMatchingViews(sharedElementNameMapping.keys)
                lastInViews.retainMatchingViews(sharedElementNameMapping.values)
                @Suppress("UsePropertyAccessSyntax") /* Collection 1.3.X requires isEmpty() */
                if (sharedElementNameMapping.isEmpty()) {
                    // We couldn't find any valid shared element mappings, so clear out
                    // the shared element transition information entirely
                    sharedElementTransition = null
                    sharedElementFirstOutViews.clear()
                    sharedElementLastInViews.clear()
                } else {
                    // Call through to onSharedElementStart() before capturing the
                    // starting values for the shared element transition
                    callSharedElementStartEnd(lastIn.fragment, firstOut.fragment, isPop,
                        firstOutViews, true)
                    // Trigger the onSharedElementEnd callback in the next frame after
                    // the starting values are captured and before capturing the end states
                    OneShotPreDrawListener.add(container) {
                        callSharedElementStartEnd(lastIn.fragment, firstOut.fragment, isPop,
                            lastInViews, false)
                    }
                    sharedElementFirstOutViews.addAll(firstOutViews.values)

                    // Compute the epicenter of the firstOut transition
                    if (exitingNames.isNotEmpty()) {
                        val epicenterViewName = exitingNames[0]
                        firstOutEpicenterView = firstOutViews[epicenterViewName]
                        transitionImpl.setEpicenter(sharedElementTransition, firstOutEpicenterView)
                    }
                    sharedElementLastInViews.addAll(lastInViews.values)

                    // Compute the epicenter of the lastIn transition
                    if (enteringNames.isNotEmpty()) {
                        val epicenterViewName = enteringNames[0]
                        val lastInEpicenterView = lastInViews[epicenterViewName]
                        if (lastInEpicenterView != null) {
                            hasLastInEpicenter = true
                            // We can't set the epicenter here directly since the View might
                            // not have been laid out as of yet, so instead we set a Rect as
                            // the epicenter and compute the bounds one frame later
                            val impl: FragmentTransitionImpl = transitionImpl
                            OneShotPreDrawListener.add(container) {
                                impl.getBoundsOnScreen(lastInEpicenterView, lastInEpicenterRect)
                            }
                        }
                    }

                    // Now set the transition's targets to only the firstOut Fragment's views
                    // It'll be swapped to the lastIn Fragment's views after the
                    // transition is started
                    transitionImpl.setSharedElementTargets(sharedElementTransition,
                        nonExistentView, sharedElementFirstOutViews)
                    // After the swap to the lastIn Fragment's view (done below), we
                    // need to clean up those targets. We schedule this here so that it
                    // runs directly after the swap
                    transitionImpl.scheduleRemoveTargets(sharedElementTransition, null, null,
                        null, null, sharedElementTransition, sharedElementLastInViews)
                    // Both the firstOut and lastIn Operations are now associated
                    // with a Transition
                    startedTransitions[firstOut] = true
                    startedTransitions[lastIn] = true
                }
            }
        }
        val enteringViews = ArrayList<View>()
        // These transitions run together, overlapping one another
        var mergedTransition: Any? = null
        // These transitions run only after all of the other transitions complete
        var mergedNonOverlappingTransition: Any? = null
        // Now iterate through the set of transitions and merge them together
        for (transitionInfo: TransitionInfo in transitionInfos) {
            if (transitionInfo.isVisibilityUnchanged) {
                // No change in visibility, so we can immediately complete the transition
                startedTransitions[transitionInfo.operation] = false
                transitionInfo.completeSpecialEffect()
                continue
            }
            val transition = transitionImpl.cloneTransition(transitionInfo.transition)
            val operation: Operation = transitionInfo.operation
            val involvedInSharedElementTransition = (sharedElementTransition != null &&
                (operation === firstOut || operation === lastIn))
            if (transition == null) {
                // Nothing more to do if the transition is null
                if (!involvedInSharedElementTransition) {
                    // Only complete the transition if this fragment isn't involved
                    // in the shared element transition (as otherwise we need to wait
                    // for that to finish)
                    startedTransitions[operation] = false
                    transitionInfo.completeSpecialEffect()
                }
            } else {
                // Target the Transition to *only* the set of transitioning views
                val transitioningViews = ArrayList<View>()
                captureTransitioningViews(transitioningViews, operation.fragment.mView)
                if (involvedInSharedElementTransition) {
                    // Remove all of the shared element views from the transition
                    if (operation === firstOut) {
                        transitioningViews.removeAll(sharedElementFirstOutViews.toSet())
                    } else {
                        transitioningViews.removeAll(sharedElementLastInViews.toSet())
                    }
                }
                if (transitioningViews.isEmpty()) {
                    transitionImpl.addTarget(transition, nonExistentView)
                } else {
                    transitionImpl.addTargets(transition, transitioningViews)
                    transitionImpl.scheduleRemoveTargets(transition, transition,
                        transitioningViews, null, null, null, null)
                    if (operation.finalState === Operation.State.GONE) {
                        // We're hiding the Fragment. This requires a bit of extra work
                        // First, we need to avoid immediately applying the container change as
                        // that will stop the Transition from occurring.
                        awaitingContainerChanges.remove(operation)
                        // Then schedule the actual hide of the fragment's view,
                        // essentially doing what applyState() would do for us
                        val transitioningViewsToHide = ArrayList(transitioningViews)
                        transitioningViewsToHide.remove(operation.fragment.mView)
                        transitionImpl.scheduleHideFragmentView(transition,
                            operation.fragment.mView, transitioningViewsToHide)
                        // This OneShotPreDrawListener gets fired before the delayed start of
                        // the Transition and changes the visibility of any exiting child views
                        // that *ARE NOT* shared element transitions. The TransitionManager then
                        // properly considers exiting views and marks them as disappearing,
                        // applying a transition and a listener to take proper actions once the
                        // transition is complete.
                        OneShotPreDrawListener.add(container) {
                            setViewVisibility(transitioningViews, View.INVISIBLE)
                        }
                    }
                }
                if (operation.finalState === Operation.State.VISIBLE) {
                    enteringViews.addAll(transitioningViews)
                    if (hasLastInEpicenter) {
                        transitionImpl.setEpicenter(transition, lastInEpicenterRect)
                    }
                } else {
                    transitionImpl.setEpicenter(transition, firstOutEpicenterView)
                }
                startedTransitions[operation] = true
                // Now determine how this transition should be merged together
                if (transitionInfo.isOverlapAllowed) {
                    // Overlap is allowed, so add them to the mergeTransition set
                    mergedTransition = transitionImpl.mergeTransitionsTogether(
                        mergedTransition, transition, null)
                } else {
                    // Overlap is not allowed, add them to the mergedNonOverlappingTransition
                    mergedNonOverlappingTransition = transitionImpl.mergeTransitionsTogether(
                        mergedNonOverlappingTransition, transition, null)
                }
            }
        }

        // Make sure that the mergedNonOverlappingTransition set
        // runs after the mergedTransition set is complete
        mergedTransition = transitionImpl.mergeTransitionsInSequence(mergedTransition,
            mergedNonOverlappingTransition, sharedElementTransition)

        // If there's no transitions playing together, no non-overlapping transitions,
        // and no shared element transitions, mergedTransition will be null and
        // there's nothing else we need to do
        if (mergedTransition == null) {
            return startedTransitions
        }

        // Now set up our completion signal on the completely merged transition set
        transitionInfos.filterNot { transitionInfo ->
            // If there's change in visibility, we've already completed the transition
            transitionInfo.isVisibilityUnchanged
        }.forEach { transitionInfo: TransitionInfo ->
            val transition: Any? = transitionInfo.transition
            val operation: Operation = transitionInfo.operation
            val involvedInSharedElementTransition = sharedElementTransition != null &&
                (operation === firstOut || operation === lastIn)
            if (transition != null || involvedInSharedElementTransition) {
                // If the container has never been laid out, transitions will not start so
                // so lets instantly complete them.
                if (!ViewCompat.isLaidOut(container)) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG,
                            "SpecialEffectsController: Container $container has not been " +
                                "laid out. Completing operation $operation")
                    }
                    transitionInfo.completeSpecialEffect()
                } else {
                    transitionImpl.setListenerForTransitionEnd(
                        transitionInfo.operation.fragment,
                        mergedTransition,
                        transitionInfo.signal,
                        Runnable {
                            transitionInfo.completeSpecialEffect()
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(FragmentManager.TAG,
                                    "Transition for operation $operation has completed")
                            }
                        })
                }
            }
        }
        // Transitions won't run if the container isn't laid out so
        // we can return early here to avoid doing unnecessary work.
        if (!ViewCompat.isLaidOut(container)) {
            return startedTransitions
        }
        // First, hide all of the entering views so they're in
        // the correct initial state
        setViewVisibility(enteringViews, View.INVISIBLE)
        val inNames = transitionImpl.prepareSetNameOverridesReordered(sharedElementLastInViews)
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, ">>>>> Beginning transition <<<<<")
            Log.v(FragmentManager.TAG, ">>>>> SharedElementFirstOutViews <<<<<")
            for (view: View in sharedElementFirstOutViews) {
                Log.v(FragmentManager.TAG,
                    "View: $view Name: ${ViewCompat.getTransitionName(view)}")
            }
            Log.v(FragmentManager.TAG, ">>>>> SharedElementLastInViews <<<<<")
            for (view: View in sharedElementLastInViews) {
                Log.v(FragmentManager.TAG,
                    "View: $view Name: ${ViewCompat.getTransitionName(view)}")
            }
        }
        // Now actually start the transition
        transitionImpl.beginDelayedTransition(container, mergedTransition)
        transitionImpl.setNameOverridesReordered(container, sharedElementFirstOutViews,
            sharedElementLastInViews, inNames, sharedElementNameMapping)
        // Then, show all of the entering views, putting them into
        // the correct final state
        setViewVisibility(enteringViews, View.VISIBLE)
        transitionImpl.swapSharedElementTargets(sharedElementTransition,
            sharedElementFirstOutViews, sharedElementLastInViews)
        return startedTransitions
    }

    /**
     * Retain only the views that have a transition name that is in the set of [names].
     */
    private fun ArrayMap<String, View>.retainMatchingViews(names: Collection<String>) {
        entries.retainAll { entry ->
            names.contains(ViewCompat.getTransitionName(entry.value))
        }
    }

    /**
     * Gets the Views in the hierarchy affected by entering and exiting transitions.
     *
     * @param transitioningViews This View will be added to transitioningViews if it has a
     * transition name, is VISIBLE and a normal View, or a ViewGroup with
     * [android.view.ViewGroup.isTransitionGroup] true.
     * @param view The base of the view hierarchy to look in.
     */
    private fun captureTransitioningViews(transitioningViews: ArrayList<View>, view: View) {
        if (view is ViewGroup) {
            if (ViewGroupCompat.isTransitionGroup(view)) {
                if (!transitioningViews.contains(view)) {
                    transitioningViews.add(view)
                }
            } else {
                val count = view.childCount
                for (i in 0 until count) {
                    val child = view.getChildAt(i)
                    if (child.visibility == View.VISIBLE) {
                        captureTransitioningViews(transitioningViews, child)
                    }
                }
            }
        } else {
            if (!transitioningViews.contains(view)) {
                transitioningViews.add(view)
            }
        }
    }

    /**
     * Finds all views that have transition names in the hierarchy under the given view and
     * stores them in [namedViews] map with the name as the key.
     */
    private fun findNamedViews(namedViews: MutableMap<String, View>, view: View) {
        val transitionName = ViewCompat.getTransitionName(view)
        if (transitionName != null) {
            namedViews[transitionName] = view
        }
        if (view is ViewGroup) {
            val count = view.childCount
            for (i in 0 until count) {
                val child = view.getChildAt(i)
                if (child.visibility == View.VISIBLE) {
                    findNamedViews(namedViews, child)
                }
            }
        }
    }

    private fun applyContainerChanges(operation: Operation) {
        val view = operation.fragment.mView
        operation.finalState.applyState(view)
    }

    private open class SpecialEffectsInfo(
        val operation: Operation,
        val signal: CancellationSignal
    ) {

        val isVisibilityUnchanged: Boolean
            get() {
                val currentState = operation.fragment.mView.asOperationState()
                val finalState = operation.finalState
                return currentState === finalState || (currentState !== Operation.State.VISIBLE &&
                    finalState !== Operation.State.VISIBLE)
            }

        fun completeSpecialEffect() {
            operation.completeSpecialEffect(signal)
        }
    }

    private class AnimationInfo(
        operation: Operation,
        signal: CancellationSignal,
        private val isPop: Boolean
    ) : SpecialEffectsInfo(operation, signal) {
        private var isAnimLoaded = false
        private var animation: FragmentAnim.AnimationOrAnimator? = null

        fun getAnimation(
            context: Context
        ): FragmentAnim.AnimationOrAnimator? = if (isAnimLoaded) {
            animation
        } else {
            FragmentAnim.loadAnimation(
                context,
                operation.fragment,
                operation.finalState === Operation.State.VISIBLE,
                isPop
            ).also {
                animation = it
                isAnimLoaded = true
            }
        }
    }

    private class TransitionInfo(
        operation: Operation,
        signal: CancellationSignal,
        isPop: Boolean,
        providesSharedElementTransition: Boolean
    ) : SpecialEffectsInfo(operation, signal) {
        val transition: Any? = if (operation.finalState === Operation.State.VISIBLE) {
            if (isPop) operation.fragment.reenterTransition else operation.fragment.enterTransition
        } else {
            if (isPop) operation.fragment.returnTransition else operation.fragment.exitTransition
        }

        val isOverlapAllowed = if (operation.finalState === Operation.State.VISIBLE) {
            // Entering transitions can choose to run after all exit
            // transitions complete, rather than overlapping with them
            if (isPop) {
                operation.fragment.allowReturnTransitionOverlap
            } else {
                operation.fragment.allowEnterTransitionOverlap
            }
        } else {
            // Removing Fragments always overlap other transitions
            true
        }

        val sharedElementTransition: Any? = if (providesSharedElementTransition) {
            if (isPop) {
                operation.fragment.sharedElementReturnTransition
            } else {
                operation.fragment.sharedElementEnterTransition
            }
        } else {
            null
        }

        fun hasSharedElementTransition(): Boolean {
            return sharedElementTransition != null
        }

        val handlingImpl: FragmentTransitionImpl?
            get() {
                val transitionImpl = getHandlingImpl(transition)
                val sharedElementTransitionImpl = getHandlingImpl(sharedElementTransition)
                require(transitionImpl == null || sharedElementTransitionImpl == null ||
                    transitionImpl === sharedElementTransitionImpl
                ) {
                    "Mixing framework transitions and AndroidX transitions is not allowed. " +
                        "Fragment ${operation.fragment} returned Transition $transition " +
                        "which uses a different Transition  type than its shared element " +
                        "transition $sharedElementTransition"
                }
                return transitionImpl ?: sharedElementTransitionImpl
            }

        private fun getHandlingImpl(transition: Any?): FragmentTransitionImpl? {
            if (transition == null) {
                return null
            }
            if ((FragmentTransition.PLATFORM_IMPL != null &&
                    FragmentTransition.PLATFORM_IMPL.canHandle(transition))
            ) {
                return FragmentTransition.PLATFORM_IMPL
            }
            if ((FragmentTransition.SUPPORT_IMPL != null &&
                    FragmentTransition.SUPPORT_IMPL.canHandle(transition))
            ) {
                return FragmentTransition.SUPPORT_IMPL
            }
            throw IllegalArgumentException(
                "Transition $transition for fragment ${operation.fragment} is not a valid " +
                    "framework Transition or AndroidX Transition"
            )
        }
    }

    @RequiresApi(24)
    internal object Api24Impl {
        @DoNotInline
        fun totalDuration(animatorSet: AnimatorSet): Long {
            return animatorSet.totalDuration
        }
    }

    @RequiresApi(26)
    internal object Api26Impl {
        @DoNotInline
        fun reverse(animatorSet: AnimatorSet) {
            animatorSet.reverse()
        }

        @DoNotInline
        fun setCurrentPlayTime(animatorSet: AnimatorSet, time: Long) {
            animatorSet.currentPlayTime = time
        }
    }
}