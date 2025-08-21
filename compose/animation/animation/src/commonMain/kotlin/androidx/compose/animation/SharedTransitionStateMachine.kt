/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull

internal const val SharedTransitionDebug = false

/**
 * This StateMachine manages the shared element's state in terms of finding match and setting up
 * animations. The possible state transitions are:
 *
 * NoMatch ->
 *
 * ActiveMatchFoundConfigPending ->
 *
 * ActiveMatchConfigured (i.e. animations are configured and are ready to run) ->
 *
 * ActiveMatchRemovedDuringTransition (i.e. The visible sharedElem entry is removed during trans)
 *
 * and state resets:
 *
 * ActiveMatchFoundConfigPending -> NoMatch
 *
 * ActiveMatchFoundConfigPending -> ActiveMatchRemovedDuringTransition
 *
 * ActiveMatchConfigured -> NoMatch
 *
 * ActiveMatchRemovedDuringTransition -> NoMatch
 */
internal class SharedTransitionStateMachine(val sharedElement: SharedElement) {

    /**
     * Base class for ActiveMatchFoundConfigPending, ActiveMatchRemovedDuringTransition,
     * ActiveMatchConfigured, NoMatch.
     */
    internal sealed class State {

        open val targetData: TargetData?
            get() = null

        open val currentBounds: Rect?
            get() = null

        open val activeMatchFound
            get() = false

        open val matchIsOrHasBeenConfigured
            get() = false

        open fun updateBounds(bounds: Rect) {}

        abstract fun onMatchFound(previousTargetBoundsProvider: BoundsProvider?): State

        abstract fun onVisibleContentRemovedDuringTransition(): State

        open fun initializeCurrentBounds(sharedElement: SharedElement): Rect? = currentBounds

        open fun configureActiveMatch(
            sharedElement: SharedElement,
            targetBoundsProvider: BoundsProvider,
            lookaheadSize: Size,
            topLeft: Offset,
            structuralOffset: Offset,
        ): State {
            error(
                "Active match can only be configured in ActiveMatchFoundConfigPending or" +
                    " ActiveMatchConfigured state. Current state: $this"
            )
        }
    }

    val activeMatchDeferred: Boolean
        get() = requestToBeHandled == StateChangeRequest.MatchFound

    var state: State by mutableStateOf(NoMatchFound)
        private set

    private var lastHandledRequestId: Int = 0

    private var requestId: Int by mutableIntStateOf(0)

    private var requestToBeHandled: StateChangeRequest = StateChangeRequest.NoRequest

    /** Resets matchState to NoMatchFound. */
    fun resetState() {
        if (SharedTransitionDebug) {
            println("SharedTransition StateMachine, reset state machine to NoMatchFound")
        }
        requestToBeHandled = StateChangeRequest.NoRequest
        lastHandledRequestId = requestId
        state = NoMatchFound
    }

    private val allEntries
        get() = sharedElement.allEntries

    private val enabledEntries
        get() = sharedElement.enabledEntries

    private var targetBoundsProvider: BoundsProvider? = null

    // Use a request id rather than a Boolean to track whether the targetBoundsProvider
    // update request has been handled. The benefit of this is that once we handle the request,
    // we update our bookkeeping (i.e. lastHandled...Id) rather than setting some dirty flag
    // back to false, as resetting the dirty flag (backed by a mutable state) would cause
    // another invalidation.
    private var targetBoundsProviderUpdateRequestId by mutableIntStateOf(0)

    private var lastHandledTargetProviderUpdateRequestId = 0

    /**
     * Defer the StateChangeRequest to a later time. This is needed because two shared element
     * entries can be in two separate (sub-)compositions. It is common that: the visible shared
     * element entry becomes not visible in the first composition, and the not visible one becomes
     * visible in the second composition. If we process each event immediately, we will first
     * conclude NoMatch, or ActiveMatchRemovedDuringTransition, then in the subsequent composition
     * arrive at ActiveMatchFoundConfigPending. This creates unnecessary state changes and potential
     * pauses in animation because we do not animate in ActiveMatchFoundConfigPending state.
     */
    fun deferRequest(request: StateChangeRequest) {
        if (SharedTransitionDebug) {
            println(
                "SharedTransition StateMachine: new request posted: $request for current state: $state"
            )
        }
        requestToBeHandled = request
        requestId = lastHandledRequestId + 1
    }

    /**
     * [processPendingRequest] is where deferred request is processed. It is important to defer the
     * requests until all the compositions have happened, so that we can re-evaluate the matches
     * after all the visibility changes for shared element entries have taken place.
     *
     * This is currently called in measure, and lookahead placement, both are after lookahead
     * measure, where shared elements may be composed.
     */
    fun processPendingRequest() {
        if (requestId != lastHandledRequestId) {
            if (SharedTransitionDebug) {
                println("SharedTransition StateMachine: handle request: $requestToBeHandled")
            }
            lastHandledRequestId = requestId
            state =
                when (requestToBeHandled) {
                    StateChangeRequest.NoMatchFound -> NoMatchFound
                    StateChangeRequest.NoRequest -> state
                    StateChangeRequest.MatchFound -> state.onMatchFound(targetBoundsProvider)
                    StateChangeRequest.VisibleContentAbsentDuringTransition -> {
                        if (enabledEntries.fastAny { it.boundsProvider == targetBoundsProvider }) {
                            // last target still exists, but no longer visible
                            NoMatchFound
                        } else {
                            state.onVisibleContentRemovedDuringTransition()
                        }
                    }
                }
            requestToBeHandled = StateChangeRequest.NoRequest
        }
        updateTargetBoundsProvider()
    }

    // This function is being observed for state changes, and will be re-invoked when the states
    // read in the function change.
    fun checkForAndDeferStateUpdates(hasVisibleContent: Boolean) {
        // Defer the StateChangeRequest to a later time. This is needed because two shared
        // element entries can be in two separate (sub-)compositions. It is common that: the
        // visible shared element entry becomes not visible in the first composition, and the
        // not visible one becomes visible in the second composition. If we process each event
        // immediately, we will first conclude NoMatch, or ActiveMatchRemovedDuringTransition,
        // then in the subsequent composition arrive at ActiveMatchFoundConfigPending.
        // This creates unnecessary state changes and potential pauses in animation because we
        // do not animate in ActiveMatchFoundConfigPending state.
        if (enabledEntries.size > 1 && hasVisibleContent) {
            deferRequest(StateChangeRequest.MatchFound)
        } else if (sharedElement.scope.isTransitionActive) {
            if (!hasVisibleContent) {
                deferRequest(StateChangeRequest.VisibleContentAbsentDuringTransition)
            }
            // No op if hasVisibleContent == true here because that means enabledEntries.size would
            // be <= 1, based on the check 7 lines above.
        } else {
            // Transition not active
            resetState()
        }
        invalidateTargetBoundsProvider()
    }

    /**
     * This gets called to update the target bounds. The 3 scenarios where
     * [invalidateTargetBoundsProvider] is needed are: when a shared element is 1) added, 2)
     * removed, or 3) getting a target state change.
     *
     * This is always called from an effect. Assume all compositional changes have been made in this
     * call.
     */
    fun invalidateTargetBoundsProvider() {
        val target = enabledEntries.fastFirstOrNull { it.target }
        if (target == null && targetBoundsProvider == null) return
        if (target?.boundsProvider == targetBoundsProvider) return

        // Do a round of filter before setting this, to reduce unnecessary churns.
        // Note: requestId is set to handled id + 1 no matter how much times it's requested
        // before it's handled. This avoids changing the mutable state many times, causing
        // more than 1 invalidations.
        targetBoundsProviderUpdateRequestId = lastHandledTargetProviderUpdateRequestId + 1
    }

    /**
     * During composition foundMatch may change, the visibility of shared element at different call
     * sites may change due to the direction of the animation (e.g. enter -> exit) changing. These
     * changes may happen across different composition/subcompositions, and they may affect which
     * shared element defines the target bounds for the shared element bounds transform.
     *
     * Since the target bounds provider needed in later stages: lookahead place, and approach
     * measure, we follow a pattern of mark the property dirty and update it just-in-time before
     * it's used in lookahead place and approach measure. This allows us to avoid updating the
     * provider and resetting corresponding flags with only partial info, e.g. before all
     * (sub)compositions finish.
     */
    internal fun updateTargetBoundsProvider() {
        // TODO: Should this happen when the node gets lookahead placement for the first time?
        if (targetBoundsProviderUpdateRequestId != lastHandledTargetProviderUpdateRequestId) {
            val newTargetBoundsProvider =
                if (sharedElement.scope.isTransitionActive) {
                    enabledEntries.fastFirstOrNull { it.target }?.boundsProvider
                } else {
                    allEntries.fastFirstOrNull { it.target }?.boundsProvider
                }

            if (newTargetBoundsProvider != targetBoundsProvider) {
                targetBoundsProvider = newTargetBoundsProvider
            }
            lastHandledTargetProviderUpdateRequestId = targetBoundsProviderUpdateRequestId
        }
    }

    // This gets called in approach measurement.
    fun tryInitializingCurrentBounds(): Rect? {
        processPendingRequest()
        return state.initializeCurrentBounds(sharedElement).also {
            if (SharedTransitionDebug) {
                println(
                    "SharedTransition, try initializing current bounds. state = $state, key =" +
                        " ${sharedElement.key}. Current bounds: $it"
                )
            }
        }
    }

    fun configureActiveMatch(lookaheadSize: Size, topLeft: Offset, structuralOffset: Offset) {
        state =
            state.configureActiveMatch(
                sharedElement,
                targetBoundsProvider!!,
                lookaheadSize,
                topLeft,
                structuralOffset,
            )
    }
}

private fun updateTargetData(
    targetData: TargetData,
    lookaheadSize: Size,
    topLeft: Offset,
    structuralOffset: Offset,
    targetBoundsProviderChanged: Boolean,
) {
    // Only update bounds when offset is updated so as to not accidentally fire
    // up animations, only to interrupt them in the same frame later on.
    if (
        targetData.targetStructuralOffset != structuralOffset ||
            targetData.size != lookaheadSize ||
            targetBoundsProviderChanged
    ) {
        // update existing target data
        targetData.size = lookaheadSize
        targetData.targetStructuralOffset = structuralOffset
        if (targetBoundsProviderChanged) {
            targetData.initialMfrOffset =
                (topLeft - structuralOffset) -
                    (targetData.currentMfrOffset - targetData.initialMfrOffset)
        }
    }

    targetData.currentMfrOffset = topLeft - structuralOffset
}

/**
 * This should be called after [SharedTransitionStateMachine.updateTargetBoundsProvider] so that
 * [lastTargetBoundsProvider] is up to date. This call will return null if there is no
 * [lastTargetBoundsProvider] recorded or the [lastTargetBoundsProvider] has never been placed.
 */
private fun SharedElement.obtainBoundsFromLastTarget(
    lastTargetBoundsProvider: BoundsProvider?
): Rect? {
    return if (
        lastTargetBoundsProvider != null &&
            // Search the last provider in all states, not just enabled states. This would allow
            // states that became disabled to still provide just-in-time initial bounds.
            allEntries.fastAny { state -> state.boundsProvider == lastTargetBoundsProvider }
    ) {
        lastTargetBoundsProvider.lastBoundsInSharedTransitionScope
    } else {
        // Old target never got placed
        null
    }
}

internal enum class StateChangeRequest {
    NoRequest,
    MatchFound,
    VisibleContentAbsentDuringTransition,
    NoMatchFound,
}

internal object NoMatchFound : SharedTransitionStateMachine.State() {
    override fun onMatchFound(
        previousTargetBoundsProvider: BoundsProvider?
    ): SharedTransitionStateMachine.State {
        if (SharedTransitionDebug) {
            println(
                "SharedTransition StateMachine: Transitioning from NoMatch to ActiveMatchPending," +
                    " previous target bounds provider: $previousTargetBoundsProvider"
            )
        }
        return ActiveMatchFoundConfigPending(previousTargetBoundsProvider)
    }

    override fun onVisibleContentRemovedDuringTransition(): SharedTransitionStateMachine.State =
        this
}

internal class ActiveMatchFoundConfigPending(
    var targetBoundsProviderBeforeConfig: BoundsProvider?,
    override val targetData: TargetData? = null,
    // This tracks the current visible bounds of shared elements on screen. It is reset when
    // foundMatch == false, NOT when the transition finishes.
    currentBounds: Rect? = null,
) : SharedTransitionStateMachine.State() {

    override val activeMatchFound
        get() = true

    override var currentBounds by mutableStateOf(currentBounds)
        private set

    override fun initializeCurrentBounds(sharedElement: SharedElement): Rect? {
        val bounds = currentBounds
        if (bounds != null) return bounds
        if (currentBounds == null) {
            val lastTarget =
                targetBoundsProviderBeforeConfig
                    ?: sharedElement.allEntries
                        .fastFirstOrNull { sharedElement.enabledEntries.contains(it) }
                        ?.boundsProvider
            sharedElement.obtainBoundsFromLastTarget(lastTarget)?.let { currentBounds = it }
        }
        return currentBounds
    }

    override fun configureActiveMatch(
        sharedElement: SharedElement,
        targetBoundsProvider: BoundsProvider,
        lookaheadSize: Size,
        topLeft: Offset,
        structuralOffset: Offset,
    ): SharedTransitionStateMachine.State {
        // Initialize targetData if needed. From here on until the transition finishes
        // the target data should be non null.
        val targetData =
            targetData ?: TargetData(lookaheadSize, topLeft - structuralOffset, structuralOffset)
        val currentBounds =
            currentBounds
                ?: sharedElement.obtainBoundsFromLastTarget(
                    targetBoundsProviderBeforeConfig
                        ?: sharedElement.allEntries
                            .fastFirstOrNull { sharedElement.enabledEntries.contains(it) }
                            ?.boundsProvider
                )
                ?: Rect(topLeft, lookaheadSize)
        updateTargetData(
            targetData,
            lookaheadSize,
            topLeft,
            structuralOffset,
            targetBoundsProviderChanged = true,
        )

        return ActiveMatchConfigured(targetData, targetBoundsProvider, currentBounds)
    }

    override fun onMatchFound(
        previousTargetBoundsProvider: BoundsProvider?
    ): SharedTransitionStateMachine.State {
        if (targetBoundsProviderBeforeConfig == null) {
            targetBoundsProviderBeforeConfig = previousTargetBoundsProvider
        }
        return this
    }

    override fun updateBounds(bounds: Rect) {
        if (SharedTransitionDebug) {
            println("SharedTransition, updating currentBounds to $bounds for state $this")
        }
        currentBounds = bounds
    }

    override fun onVisibleContentRemovedDuringTransition(): SharedTransitionStateMachine.State {
        if (SharedTransitionDebug) {
            println(
                "SharedTransition StateMachine: Transitioning from ActiveMatchPending to NoMatch"
            )
        }
        return NoMatchFound
    }
}

internal abstract class MatchIsOrHasBeenConfigured : SharedTransitionStateMachine.State() {
    abstract override val targetData: TargetData
    abstract override val currentBounds: Rect
    override val matchIsOrHasBeenConfigured: Boolean
        get() = true
}

internal class ActiveMatchConfigured(
    override val targetData: TargetData,
    targetBoundsProvider: BoundsProvider,
    currentBounds: Rect,
) : MatchIsOrHasBeenConfigured() {
    override val activeMatchFound
        get() = true

    var targetBoundsProvider by mutableStateOf(targetBoundsProvider)

    // This tracks the current visible bounds of shared elements on screen. It is reset when
    // foundMatch == false, NOT when the transition finishes.
    override var currentBounds by mutableStateOf(currentBounds)
        private set

    override fun onMatchFound(
        previousTargetBoundsProvider: BoundsProvider?
    ): SharedTransitionStateMachine.State = this

    override fun configureActiveMatch(
        sharedElement: SharedElement,
        targetBoundsProvider: BoundsProvider,
        lookaheadSize: Size,
        topLeft: Offset,
        structuralOffset: Offset,
    ): SharedTransitionStateMachine.State {
        val targetBoundsProviderChanged: Boolean = this.targetBoundsProvider != targetBoundsProvider
        updateTargetData(
            targetData,
            lookaheadSize,
            topLeft,
            structuralOffset,
            targetBoundsProviderChanged,
        )
        this.targetBoundsProvider = targetBoundsProvider
        return this
    }

    override fun onVisibleContentRemovedDuringTransition(): SharedTransitionStateMachine.State {
        if (SharedTransitionDebug) {
            println(
                "SharedTransition StateMachine: Transitioning from ActiveMatchConfigured to" +
                    " ActiveMatchRemovedDuringTransition"
            )
        }
        val lastTarget =
            Rect(targetData.currentMfrOffset + targetData.targetStructuralOffset, targetData.size)
        // Update target data
        val newTarget = targetBoundsProvider.calculateAlternativeTargetBounds(lastTarget)
        if (newTarget == null) {
            // Alternative target is null after losing target, terminate ongoing
            // animations
            return NoMatchFound
        } else {
            val updatedTargetData =
                TargetData(
                        size = newTarget.size,
                        initialMfrOffset = targetData.initialMfrOffset,
                        targetStructuralOffset = newTarget.topLeft - targetData.currentMfrOffset,
                    )
                    .also { it.currentMfrOffset = targetData.currentMfrOffset }
            return ActiveMatchRemovedDuringTransition(updatedTargetData, currentBounds)
        }
    }

    override fun updateBounds(bounds: Rect) {
        if (SharedTransitionDebug) {
            println("SharedTransition, updating currentBounds to $bounds for state $this")
        }
        currentBounds = bounds
    }
}

internal class ActiveMatchRemovedDuringTransition(
    override val targetData: TargetData,
    currentBounds: Rect,
) : MatchIsOrHasBeenConfigured() {

    // This tracks the current visible bounds of shared elements on screen. It is reset when
    // foundMatch == false, NOT when the transition finishes.
    override var currentBounds: Rect by mutableStateOf(currentBounds)
        private set

    var alternativeTargetConfigured: Boolean = false

    override fun onMatchFound(
        previousTargetBoundsProvider: BoundsProvider?
    ): SharedTransitionStateMachine.State {
        if (SharedTransitionDebug) {
            println(
                "SharedTransition StateMachine: Transitioning from" +
                    " ActiveMatchRemovedDuringTransition to ActiveMatchPending"
            )
        }
        return ActiveMatchFoundConfigPending(
            previousTargetBoundsProvider,
            targetData,
            currentBounds,
        )
    }

    override fun updateBounds(bounds: Rect) {
        if (SharedTransitionDebug) {
            println("SharedTransition, updating currentBounds to $bounds for state $this")
        }
        currentBounds = bounds
    }

    override fun onVisibleContentRemovedDuringTransition(): SharedTransitionStateMachine.State =
        this
}

/**
 * TargetData includes fine grained information to calculate bounds. Instead of a single target
 * offset, it tracks structural offset and MFR (i.e. motion frame of reference) offset: structural
 * offset + MFR offset = target offset.
 *
 * By tracking them separately, we are able to animate structural changes while applying MFR changes
 * directly to accommodate changing MFRs during scrolling, instead of animating the scrolled amount.
 */
@Stable
internal class TargetData(size: Size, initialMfrOffset: Offset, targetStructuralOffset: Offset) {
    var size: Size by mutableStateOf(size)

    /**
     * Initial motion frame of reference offset when the target changes, obtained from lookahead
     * placement.
     */
    var initialMfrOffset: Offset by mutableStateOf(initialMfrOffset)

    /**
     * Structural offset obtained from lookahead placement, this is equivalent to: target offset -
     * motion frame of reference offset.
     *
     * Structural offset is intended to only track offsets from structural changes, as opposed to
     * scrolling/dragging offset changes.
     */
    var targetStructuralOffset: Offset by mutableStateOf(targetStructuralOffset)

    /**
     * Current motion frame of reference offset for the current frame, obtained from lookahead
     * placement.
     *
     * Note: This assumes MFR offset is the same for lookahead and approach. If/When we need to
     * support use cases where this isn't true, we need to add an API to allow disabling MFR
     * support.
     */
    var currentMfrOffset: Offset by mutableStateOf(initialMfrOffset)
}

/**
 * targetBounds here is the combination of target structural offset and initialMfrOffset. This
 * ensures:
 * 1) We don't update the target when MFR offset changes. The MFR changes will be directly applied
 *    to the animated value in [calculateOffsetFromDirectManipulation] below. And
 * 2) When target structural offset changes, we animate that change.
 */
internal val TargetData.targetBounds: Rect
    get() = Rect(initialMfrOffset + targetStructuralOffset, size)

/**
 * Once the animation starts, we will only change target bounds when the target structural offset
 * changes. When MFR (e.g. scrolling) changes, we will track the current MFR, and apply the total
 * offset incurred since the start of the animation (i.e. currentMFR - initialMFR) directly to the
 * animated value.
 */
internal fun TargetData.calculateOffsetFromDirectManipulation(animatedBounds: Rect): Offset =
    animatedBounds.topLeft - initialMfrOffset + currentMfrOffset
