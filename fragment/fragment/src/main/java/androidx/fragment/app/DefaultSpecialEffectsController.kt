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
import androidx.activity.BackEventCompat
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.FragmentTransition.callSharedElementStartEnd
import androidx.fragment.app.FragmentTransition.findKeyForValue
import androidx.fragment.app.FragmentTransition.retainValues
import androidx.fragment.app.FragmentTransition.setViewVisibility
import androidx.fragment.app.SpecialEffectsController.Operation.State.Companion.asOperationState

/**
 * A SpecialEffectsController that hooks into the existing Fragment APIs to run animations and
 * transitions.
 */
internal class DefaultSpecialEffectsController(container: ViewGroup) :
    SpecialEffectsController(container) {
    override fun collectEffects(operations: List<Operation>, isPop: Boolean) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(FragmentManager.TAG, "Collecting Effects")
        }
        // Shared element transitions are done between the first fragment leaving and
        // the last fragment coming in. Finding these operations is the first priority
        val firstOut =
            operations.firstOrNull { operation ->
                val currentState = operation.fragment.mView.asOperationState()
                // The firstOut Operation is the first Operation moving from VISIBLE
                currentState == Operation.State.VISIBLE &&
                    operation.finalState != Operation.State.VISIBLE
            }
        val lastIn =
            operations.lastOrNull { operation ->
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

        // sync animations together before we start loading them.
        syncAnimations(operations)
        for (operation: Operation in operations) {
            // Add the animation special effect
            animations.add(AnimationInfo(operation, isPop))

            // Add the transition special effect
            transitions.add(
                TransitionInfo(
                    operation,
                    isPop,
                    if (isPop) operation === firstOut else operation === lastIn,
                )
            )

            // Ensure that if the Operation is synchronously complete, we still
            // apply the container changes before the Operation completes
            operation.addCompletionListener { applyContainerChangesToOperation(operation) }
        }

        // Start transition special effects
        createTransitionEffect(transitions, isPop, firstOut, lastIn)

        // Collect Animation and Animator Effects
        collectAnimEffects(animations)
    }

    /** Syncs the animations of all other operations with the animations of the last operation. */
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
    private fun collectAnimEffects(animationInfos: List<AnimationInfo>) {
        val animationsToRun = mutableListOf<AnimationInfo>()
        val startedAnyTransition = animationInfos.flatMap { it.operation.effects }.isNotEmpty()
        var startedAnyAnimator = false
        // Find all Animators and add the effect to the operation
        for (animatorInfo: AnimationInfo in animationInfos) {
            val context = container.context
            val operation: Operation = animatorInfo.operation
            val anim = animatorInfo.getAnimation(context)
            if (anim == null) {
                continue
            }
            val animator = anim.animator
            if (animator == null) {
                // We must have an Animation to run. Save those for a second pass
                animationsToRun.add(animatorInfo)
                continue
            }

            // First make sure we haven't already started a Transition for this Operation

            val fragment = operation.fragment
            val startedTransition = operation.effects.isNotEmpty()
            if (startedTransition) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Ignoring Animator set on $fragment as this Fragment was involved " +
                            "in a Transition.",
                    )
                }
                continue
            }
            startedAnyAnimator = true
            val isHideOperation = operation.finalState === Operation.State.GONE
            if (isHideOperation) {
                // We don't want to immediately applyState() to hide operations as that
                // immediately stops the Animator. Instead we'll applyState() manually
                // when the Animator ends.
                operation.isAwaitingContainerChanges = false
            }
            operation.addEffect(AnimatorEffect(animatorInfo))
        }

        // Find all Animations and add the effect to the operation
        for (animationInfo: AnimationInfo in animationsToRun) {
            val operation: Operation = animationInfo.operation
            val fragment = operation.fragment
            if (startedAnyTransition) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Ignoring Animation set on $fragment as Animations cannot " +
                            "run alongside Transitions.",
                    )
                }
                continue
            }
            // Then make sure we haven't already started any Animator
            if (startedAnyAnimator) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Ignoring Animation set on $fragment as Animations cannot " +
                            "run alongside Animators.",
                    )
                }
                continue
            }
            operation.addEffect(AnimationEffect(animationInfo))
        }
    }

    private fun createTransitionEffect(
        transitionInfos: List<TransitionInfo>,
        isPop: Boolean,
        firstOut: Operation?,
        lastIn: Operation?,
    ) {
        val filteredInfos =
            transitionInfos
                .filterNot { transitionInfo ->
                    // If there is no change in visibility, we can skip the TransitionInfo
                    transitionInfo.isVisibilityUnchanged
                }
                .filter { transitionInfo -> transitionInfo.handlingImpl != null }
        // First verify that we can run all transitions together
        val transitionImpl =
            filteredInfos.fold(null as FragmentTransitionImpl?) { chosenImpl, transitionInfo ->
                val handlingImpl = transitionInfo.handlingImpl
                require(chosenImpl == null || handlingImpl === chosenImpl) {
                    "Mixing framework transitions and AndroidX transitions is not allowed. Fragment " +
                        "${transitionInfo.operation.fragment} returned Transition " +
                        "${transitionInfo.transition} which uses a different Transition " +
                        "type than other Fragments."
                }
                handlingImpl
            }
                ?: // Early return if there were no transitions at all
                return

        // Now find the shared element transition if it exists
        var sharedElementTransition: Any? = null
        val sharedElementFirstOutViews = ArrayList<View>()
        val sharedElementLastInViews = ArrayList<View>()
        val sharedElementNameMapping = ArrayMap<String, String>()
        var enteringNames = ArrayList<String>()
        var exitingNames = ArrayList<String>()
        val firstOutViews = ArrayMap<String, View>()
        val lastInViews = ArrayMap<String, View>()
        for (transitionInfo: TransitionInfo in filteredInfos) {
            val hasSharedElementTransition = transitionInfo.hasSharedElementTransition()
            // Compute the shared element transition between the firstOut and lastIn Fragments
            if (hasSharedElementTransition && (firstOut != null) && (lastIn != null)) {
                // swapSharedElementTargets requires wrapping this in a TransitionSet
                sharedElementTransition =
                    transitionImpl.wrapTransitionInSet(
                        transitionImpl.cloneTransition(transitionInfo.sharedElementTransition)
                    )
                // The exiting shared elements default to the source names from the
                // last in fragment
                exitingNames = lastIn.fragment.sharedElementSourceNames
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
                enteringNames = lastIn.fragment.sharedElementTargetNames
                val (exitingCallback, enteringCallback) =
                    if (!isPop) {
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
                findNamedViews(firstOutViews, firstOut.fragment.mView)
                firstOutViews.retainAll(exitingNames)
                if (exitingCallback != null) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "Executing exit callback for operation $firstOut",
                        )
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
                findNamedViews(lastInViews, lastIn.fragment.mView)
                lastInViews.retainAll(enteringNames)
                lastInViews.retainAll(sharedElementNameMapping.values)
                if (enteringCallback != null) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG, "Executing enter callback for operation $lastIn")
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
                    Log.i(
                        FragmentManager.TAG,
                        "Ignoring shared elements transition $sharedElementTransition between " +
                            "$firstOut and $lastIn as there are no matching elements " +
                            "in both the entering and exiting fragment. In order to run a " +
                            "SharedElementTransition, both fragments involved must have the " +
                            "element.",
                    )
                    sharedElementTransition = null
                    sharedElementFirstOutViews.clear()
                    sharedElementLastInViews.clear()
                }
            }
        }

        if (sharedElementTransition == null && filteredInfos.all { it.transition == null }) {
            // Return without creating a TransitionEffect since there are no Transitions to run
            return
        }

        val transitionEffect =
            TransitionEffect(
                filteredInfos,
                firstOut,
                lastIn,
                transitionImpl,
                sharedElementTransition,
                sharedElementFirstOutViews,
                sharedElementLastInViews,
                sharedElementNameMapping,
                enteringNames,
                exitingNames,
                firstOutViews,
                lastInViews,
                isPop,
            )

        filteredInfos.forEach { transitionInfo ->
            transitionInfo.operation.addEffect(transitionEffect)
        }
    }

    /** Retain only the views that have a transition name that is in the set of [names]. */
    private fun ArrayMap<String, View>.retainMatchingViews(names: Collection<String>) {
        entries.retainAll { entry -> names.contains(ViewCompat.getTransitionName(entry.value)) }
    }

    /**
     * Finds all views that have transition names in the hierarchy under the given view and stores
     * them in [namedViews] map with the name as the key.
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

    internal open class SpecialEffectsInfo(val operation: Operation) {

        val isVisibilityUnchanged: Boolean
            get() {
                val currentState = operation.fragment.mView?.asOperationState()
                val finalState = operation.finalState
                return currentState === finalState ||
                    (currentState !== Operation.State.VISIBLE &&
                        finalState !== Operation.State.VISIBLE)
            }
    }

    private class AnimationInfo(operation: Operation, private val isPop: Boolean) :
        SpecialEffectsInfo(operation) {
        private var isAnimLoaded = false
        private var animation: FragmentAnim.AnimationOrAnimator? = null

        fun getAnimation(context: Context): FragmentAnim.AnimationOrAnimator? =
            if (isAnimLoaded) {
                animation
            } else {
                FragmentAnim.loadAnimation(
                        context,
                        operation.fragment,
                        operation.finalState === Operation.State.VISIBLE,
                        isPop,
                    )
                    .also {
                        animation = it
                        isAnimLoaded = true
                    }
            }
    }

    private class TransitionInfo(
        operation: Operation,
        isPop: Boolean,
        providesSharedElementTransition: Boolean,
    ) : SpecialEffectsInfo(operation) {

        val transition: Any? =
            if (operation.finalState === Operation.State.VISIBLE) {
                if (isPop) operation.fragment.reenterTransition
                else operation.fragment.enterTransition
            } else {
                if (isPop) operation.fragment.returnTransition
                else operation.fragment.exitTransition
            }

        val isOverlapAllowed =
            if (operation.finalState === Operation.State.VISIBLE) {
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

        val sharedElementTransition: Any? =
            if (providesSharedElementTransition) {
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
                require(
                    transitionImpl == null ||
                        sharedElementTransitionImpl == null ||
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
            if (
                (FragmentTransition.PLATFORM_IMPL != null &&
                    FragmentTransition.PLATFORM_IMPL.canHandle(transition))
            ) {
                return FragmentTransition.PLATFORM_IMPL
            }
            if (
                (FragmentTransition.SUPPORT_IMPL != null &&
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

    private class AnimationEffect(val animationInfo: AnimationInfo) : Effect() {
        override fun onCommit(container: ViewGroup) {
            if (animationInfo.isVisibilityUnchanged) {
                // No change in visibility, so we can immediately complete the animation
                animationInfo.operation.completeEffect(this)
                return
            }
            val context = container.context
            val operation: Operation = animationInfo.operation
            val fragment = operation.fragment

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
                animationInfo.operation.completeEffect(this)
            } else {
                container.startViewTransition(viewToAnimate)
                val animation: Animation =
                    FragmentAnim.EndViewTransitionAnimation(anim, container, viewToAnimate)
                animation.setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(
                                    FragmentManager.TAG,
                                    "Animation from operation $operation has reached " +
                                        "onAnimationStart.",
                                )
                            }
                        }

                        override fun onAnimationEnd(animation: Animation) {
                            // onAnimationEnd() comes during draw(), so there can still be some
                            // draw events happening after this call. We don't want to complete the
                            // animation until after the onAnimationEnd()
                            container.post {
                                container.endViewTransition(viewToAnimate)
                                animationInfo.operation.completeEffect(this@AnimationEffect)
                            }
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(
                                    FragmentManager.TAG,
                                    "Animation from operation $operation has ended.",
                                )
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    }
                )
                viewToAnimate.startAnimation(animation)
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(FragmentManager.TAG, "Animation from operation $operation has started.")
                }
            }
        }

        override fun onCancel(container: ViewGroup) {
            val operation: Operation = animationInfo.operation
            val fragment = operation.fragment
            val viewToAnimate = fragment.mView

            viewToAnimate.clearAnimation()
            container.endViewTransition(viewToAnimate)
            animationInfo.operation.completeEffect(this)
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(
                    FragmentManager.TAG,
                    "Animation from operation $operation has been cancelled.",
                )
            }
        }
    }

    private class AnimatorEffect(val animatorInfo: AnimationInfo) : Effect() {
        override val isSeekingSupported: Boolean
            get() = true

        var animator: AnimatorSet? = null

        override fun onStart(container: ViewGroup) {
            if (animatorInfo.isVisibilityUnchanged) {
                // No change in visibility, so we can avoid starting the animator
                return
            }
            val context = container.context
            animator = animatorInfo.getAnimation(context)?.animator
            val operation: Operation = animatorInfo.operation
            val fragment = operation.fragment

            // Okay, let's run the Animator!
            val isHideOperation = operation.finalState === Operation.State.GONE
            val viewToAnimate = fragment.mView
            container.startViewTransition(viewToAnimate)
            animator?.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(anim: Animator) {
                        container.endViewTransition(viewToAnimate)
                        if (isHideOperation || operation.finalState === Operation.State.GONE) {
                            // Specifically for hide operations with Animator, we can't
                            // applyState until the Animator finishes
                            operation.finalState.applyState(viewToAnimate, container)
                        }
                        animatorInfo.operation.completeEffect(this@AnimatorEffect)
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(
                                FragmentManager.TAG,
                                "Animator from operation $operation has ended.",
                            )
                        }
                    }
                }
            )
            animator?.setTarget(viewToAnimate)
        }

        override fun onProgress(backEvent: BackEventCompat, container: ViewGroup) {
            val operation = animatorInfo.operation
            val animatorSet = animator
            if (animatorSet == null) {
                // No change in visibility, so we can go ahead and complete the effect
                animatorInfo.operation.completeEffect(this)
                return
            }

            if (Build.VERSION.SDK_INT >= 34 && operation.fragment.mTransitioning) {
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Adding BackProgressCallbacks for Animators to operation $operation",
                    )
                }
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
                            "operation $operation",
                    )
                }
                Api26Impl.setCurrentPlayTime(animatorSet, time)
            }
        }

        override fun onCommit(container: ViewGroup) {
            val operation = animatorInfo.operation
            val animatorSet = animator
            if (animatorSet == null) {
                // No change in visibility, so we can go ahead and complete the effect
                animatorInfo.operation.completeEffect(this)
                return
            }
            animatorSet.start()
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, "Animator from operation $operation has started.")
            }
        }

        override fun onCancel(container: ViewGroup) {
            val animator = animator
            if (animator == null) {
                // No change in visibility, so we can go ahead and complete the effect
                animatorInfo.operation.completeEffect(this)
            } else {
                val operation = animatorInfo.operation
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
                            "${if (operation.isSeeking) " with seeking." else "."} ",
                    )
                }
            }
        }
    }

    private class TransitionEffect(
        val transitionInfos: List<TransitionInfo>,
        val firstOut: Operation?,
        val lastIn: Operation?,
        val transitionImpl: FragmentTransitionImpl,
        val sharedElementTransition: Any?,
        val sharedElementFirstOutViews: ArrayList<View>,
        val sharedElementLastInViews: ArrayList<View>,
        val sharedElementNameMapping: ArrayMap<String, String>,
        val enteringNames: ArrayList<String>,
        val exitingNames: ArrayList<String>,
        val firstOutViews: ArrayMap<String, View>,
        val lastInViews: ArrayMap<String, View>,
        val isPop: Boolean,
    ) : Effect() {
        @Suppress("DEPRECATION") val transitionSignal = androidx.core.os.CancellationSignal()

        var controller: Any? = null
        var noControllerReturned: Boolean = false

        override val isSeekingSupported: Boolean
            get() =
                transitionImpl.isSeekingSupported &&
                    transitionInfos.all {
                        Build.VERSION.SDK_INT >= 34 &&
                            it.transition != null &&
                            transitionImpl.isSeekingSupported(it.transition)
                    } &&
                    (sharedElementTransition == null ||
                        transitionImpl.isSeekingSupported(sharedElementTransition))

        val transitioning: Boolean
            get() = transitionInfos.all { it.operation.fragment.mTransitioning }

        override fun onStart(container: ViewGroup) {
            // If the container has never been laid out, transitions will not start so
            // so lets instantly complete them.
            if (!container.isLaidOut()) {
                transitionInfos.forEach { transitionInfo: TransitionInfo ->
                    val operation: Operation = transitionInfo.operation
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "SpecialEffectsController: Container $container has not been " +
                                "laid out. Skipping onStart for operation $operation",
                        )
                    }
                }
                return
            }
            if (transitioning && sharedElementTransition != null && !isSeekingSupported) {
                Log.i(
                    FragmentManager.TAG,
                    "Ignoring shared elements transition $sharedElementTransition between " +
                        "$firstOut and $lastIn as neither fragment has set a Transition. In " +
                        "order to run a SharedElementTransition, you must also set either an " +
                        "enter or exit transition on a fragment involved in the transaction. The " +
                        "sharedElementTransition will run after the back gesture has been " +
                        "committed.",
                )
            }
            if (isSeekingSupported && transitioning) {
                // We need to set the listener before we create the controller, but we need the
                // controller to do the desired cancel behavior (animateToStart). So we use this
                // lambda to set the proper cancel behavior to pass into the listener before the
                // function is created.
                var seekCancelLambda: (() -> Unit)? = null
                // Now set up our completion signal on the completely merged transition set
                val (enteringViews, mergedTransition) =
                    createMergedTransition(container, lastIn, firstOut)
                transitionInfos
                    .map { it.operation }
                    .forEach { operation ->
                        val cancelRunnable = Runnable { seekCancelLambda?.invoke() }
                        transitionImpl.setListenerForTransitionEnd(
                            operation.fragment,
                            mergedTransition,
                            transitionSignal,
                            cancelRunnable,
                            Runnable {
                                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                    Log.v(
                                        FragmentManager.TAG,
                                        "Transition for operation $operation has completed",
                                    )
                                }
                                operation.completeEffect(this)
                            },
                        )
                    }

                runTransition(enteringViews, container) {
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(FragmentManager.TAG, "Attempting to create TransitionSeekController")
                    }
                    controller =
                        transitionImpl.controlDelayedTransition(container, mergedTransition)
                    // If we fail to create a controller, it must be because of the container or
                    // the transition so we will immediately complete.
                    if (controller == null) {
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "TransitionSeekController was not created.")
                        }
                        noControllerReturned = true
                        return@runTransition
                    }
                    seekCancelLambda = {
                        if (transitionInfos.all { it.operation.isSeeking }) {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(FragmentManager.TAG, "Animating to start")
                            }
                            transitionImpl.animateToStart(controller!!) {
                                transitionInfos.forEach { transitionInfo ->
                                    val operation = transitionInfo.operation
                                    val view = operation.fragment.view
                                    if (view != null) {
                                        operation.finalState.applyState(view, container)
                                    }
                                }
                            }
                        } else {
                            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                Log.v(FragmentManager.TAG, "Completing animating immediately")
                            }
                            @Suppress("DEPRECATION")
                            val cancelSignal = androidx.core.os.CancellationSignal()
                            transitionImpl.setListenerForTransitionEnd(
                                transitionInfos[0].operation.fragment,
                                mergedTransition,
                                cancelSignal,
                            ) {
                                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                    Log.v(
                                        FragmentManager.TAG,
                                        "Transition for all operations has completed",
                                    )
                                }
                                transitionInfos.forEach { it.operation.completeEffect(this) }
                            }
                            cancelSignal.cancel()
                        }
                    }
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        Log.v(
                            FragmentManager.TAG,
                            "Started executing operations from $firstOut to $lastIn",
                        )
                    }
                }
            }
        }

        override fun onProgress(backEvent: BackEventCompat, container: ViewGroup) {
            controller?.let { transitionImpl.setCurrentPlayTime(it, backEvent.progress) }
        }

        override fun onCommit(container: ViewGroup) {
            // If the container has never been laid out, transitions will not start so
            // so lets instantly complete them.
            if (!container.isLaidOut() || noControllerReturned) {
                transitionInfos.forEach { transitionInfo: TransitionInfo ->
                    val operation: Operation = transitionInfo.operation
                    if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                        if (noControllerReturned) {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: TransitionSeekController was not " +
                                    "created. Completing operation $operation",
                            )
                        } else {
                            Log.v(
                                FragmentManager.TAG,
                                "SpecialEffectsController: Container $container has not been " +
                                    "laid out. Completing operation $operation",
                            )
                        }
                    }
                    transitionInfo.operation.completeEffect(this)
                }
                noControllerReturned = false
                return
            }
            if (controller != null) {
                transitionImpl.animateToEnd(controller!!)
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Ending execution of operations from $firstOut to $lastIn",
                    )
                }
            } else {
                val (enteringViews, mergedTransition) =
                    createMergedTransition(container, lastIn, firstOut)
                // Now set up our completion signal on the completely merged transition set
                transitionInfos
                    .map { it.operation }
                    .forEach { operation ->
                        transitionImpl.setListenerForTransitionEnd(
                            operation.fragment,
                            mergedTransition,
                            transitionSignal,
                            Runnable {
                                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                                    Log.v(
                                        FragmentManager.TAG,
                                        "Transition for operation $operation has completed",
                                    )
                                }
                                operation.completeEffect(this)
                            },
                        )
                    }
                runTransition(enteringViews, container) {
                    transitionImpl.beginDelayedTransition(container, mergedTransition)
                }
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(
                        FragmentManager.TAG,
                        "Completed executing operations from $firstOut to $lastIn",
                    )
                }
            }
        }

        private fun createMergedTransition(
            container: ViewGroup,
            lastIn: Operation?,
            firstOut: Operation?,
        ): Pair<ArrayList<View>, Any> {
            // Every transition needs to target at least one View so that they
            // don't interfere with one another. This is the view we use
            // in cases where there are no real views to target
            val nonExistentView = View(container.context)
            var firstOutEpicenterView: View? = null
            var hasLastInEpicenter = false
            val lastInEpicenterRect = Rect()
            for (transitionInfo: TransitionInfo in transitionInfos) {
                val hasSharedElementTransition = transitionInfo.hasSharedElementTransition()
                // Compute the shared element transition between the firstOut and lastIn Fragments
                if (hasSharedElementTransition && (firstOut != null) && (lastIn != null)) {
                    if (sharedElementNameMapping.isNotEmpty() && sharedElementTransition != null) {
                        // Call through to onSharedElementStart() before capturing the
                        // starting values for the shared element transition
                        callSharedElementStartEnd(
                            lastIn.fragment,
                            firstOut.fragment,
                            isPop,
                            firstOutViews,
                            true,
                        )
                        // Trigger the onSharedElementEnd callback in the next frame after
                        // the starting values are captured and before capturing the end states
                        OneShotPreDrawListener.add(container) {
                            callSharedElementStartEnd(
                                lastIn.fragment,
                                firstOut.fragment,
                                isPop,
                                lastInViews,
                                false,
                            )
                        }
                        sharedElementFirstOutViews.addAll(firstOutViews.values)

                        // Compute the epicenter of the firstOut transition
                        if (exitingNames.isNotEmpty()) {
                            val epicenterViewName = exitingNames[0]
                            firstOutEpicenterView = firstOutViews[epicenterViewName]
                            transitionImpl.setEpicenter(
                                sharedElementTransition,
                                firstOutEpicenterView,
                            )
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
                        transitionImpl.setSharedElementTargets(
                            sharedElementTransition,
                            nonExistentView,
                            sharedElementFirstOutViews,
                        )
                        // After the swap to the lastIn Fragment's view (done below), we
                        // need to clean up those targets. We schedule this here so that it
                        // runs directly after the swap
                        transitionImpl.scheduleRemoveTargets(
                            sharedElementTransition,
                            null,
                            null,
                            null,
                            null,
                            sharedElementTransition,
                            sharedElementLastInViews,
                        )
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
                val operation: Operation = transitionInfo.operation
                val transition = transitionImpl.cloneTransition(transitionInfo.transition)
                if (transition != null) {
                    // Target the Transition to *only* the set of transitioning views
                    val transitioningViews = ArrayList<View>()
                    captureTransitioningViews(transitioningViews, operation.fragment.mView)
                    val involvedInSharedElementTransition =
                        (sharedElementTransition != null &&
                            (operation === firstOut || operation === lastIn))
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
                        transitionImpl.scheduleRemoveTargets(
                            transition,
                            transition,
                            transitioningViews,
                            null,
                            null,
                            null,
                            null,
                        )
                        if (operation.finalState === Operation.State.GONE) {
                            // We're hiding the Fragment. This requires a bit of extra work
                            // First, we need to avoid immediately applying the container change as
                            // that will stop the Transition from occurring.
                            operation.isAwaitingContainerChanges = false
                            // Then schedule the actual hide of the fragment's view,
                            // essentially doing what applyState() would do for us
                            val transitioningViewsToHide = ArrayList(transitioningViews)
                            transitioningViewsToHide.remove(operation.fragment.mView)
                            transitionImpl.scheduleHideFragmentView(
                                transition,
                                operation.fragment.mView,
                                transitioningViewsToHide,
                            )
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
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "Entering Transition: $transition")
                            Log.v(FragmentManager.TAG, ">>>>> EnteringViews <<<<<")
                            for (view: View in transitioningViews) {
                                Log.v(FragmentManager.TAG, "View: $view")
                            }
                        }
                    } else {
                        transitionImpl.setEpicenter(transition, firstOutEpicenterView)
                        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                            Log.v(FragmentManager.TAG, "Exiting Transition: $transition")
                            Log.v(FragmentManager.TAG, ">>>>> ExitingViews <<<<<")
                            for (view: View in transitioningViews) {
                                Log.v(FragmentManager.TAG, "View: $view")
                            }
                        }
                    }
                    // Now determine how this transition should be merged together
                    if (transitionInfo.isOverlapAllowed) {
                        // Overlap is allowed, so add them to the mergeTransition set
                        mergedTransition =
                            transitionImpl.mergeTransitionsTogether(
                                mergedTransition,
                                transition,
                                null,
                            )
                    } else {
                        // Overlap is not allowed, add them to the mergedNonOverlappingTransition
                        mergedNonOverlappingTransition =
                            transitionImpl.mergeTransitionsTogether(
                                mergedNonOverlappingTransition,
                                transition,
                                null,
                            )
                    }
                }
            }

            // Make sure that the mergedNonOverlappingTransition set
            // runs after the mergedTransition set is complete
            mergedTransition =
                transitionImpl.mergeTransitionsInSequence(
                    mergedTransition,
                    mergedNonOverlappingTransition,
                    sharedElementTransition,
                )

            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(
                    FragmentManager.TAG,
                    "Final merged transition: $mergedTransition for container $container",
                )
            }

            return Pair(enteringViews, mergedTransition)
        }

        private fun runTransition(
            enteringViews: ArrayList<View>,
            container: ViewGroup,
            executeTransition: (() -> Unit),
        ) {
            // First, hide all of the entering views so they're in
            // the correct initial state
            setViewVisibility(enteringViews, View.INVISIBLE)
            val inNames = transitionImpl.prepareSetNameOverridesReordered(sharedElementLastInViews)
            if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                Log.v(FragmentManager.TAG, ">>>>> Beginning transition <<<<<")
                Log.v(FragmentManager.TAG, ">>>>> SharedElementFirstOutViews <<<<<")
                for (view: View in sharedElementFirstOutViews) {
                    Log.v(
                        FragmentManager.TAG,
                        "View: $view Name: ${ViewCompat.getTransitionName(view)}",
                    )
                }
                Log.v(FragmentManager.TAG, ">>>>> SharedElementLastInViews <<<<<")
                for (view: View in sharedElementLastInViews) {
                    Log.v(
                        FragmentManager.TAG,
                        "View: $view Name: ${ViewCompat.getTransitionName(view)}",
                    )
                }
            }
            // Now actually start the transition
            executeTransition.invoke()
            transitionImpl.setNameOverridesReordered(
                container,
                sharedElementFirstOutViews,
                sharedElementLastInViews,
                inNames,
                sharedElementNameMapping,
            )
            // Then, show all of the entering views, putting them into
            // the correct final state
            setViewVisibility(enteringViews, View.VISIBLE)
            transitionImpl.swapSharedElementTargets(
                sharedElementTransition,
                sharedElementFirstOutViews,
                sharedElementLastInViews,
            )
        }

        override fun onCancel(container: ViewGroup) {
            transitionSignal.cancel()
        }

        /**
         * Gets the Views in the hierarchy affected by entering and exiting transitions.
         *
         * @param transitioningViews This View will be added to transitioningViews if it has a
         *   transition name, is VISIBLE and a normal View, or a ViewGroup with
         *   [android.view.ViewGroup.isTransitionGroup] true.
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
    }

    @RequiresApi(24)
    internal object Api24Impl {
        fun totalDuration(animatorSet: AnimatorSet): Long {
            return animatorSet.totalDuration
        }
    }

    @RequiresApi(26)
    internal object Api26Impl {
        fun reverse(animatorSet: AnimatorSet) {
            animatorSet.reverse()
        }

        fun setCurrentPlayTime(animatorSet: AnimatorSet, time: Long) {
            animatorSet.currentPlayTime = time
        }
    }
}
