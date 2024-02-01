/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.collection.MutableObjectIntMap
import androidx.collection.MutableScatterMap
import androidx.compose.runtime.collection.IdentityArraySet
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.RecomposeScopeObserver

/**
 * Represents a recomposable scope or section of the composition hierarchy. Can be used to
 * manually invalidate the scope to schedule it for recomposition.
 */
interface RecomposeScope {
    /**
     * Invalidate the corresponding scope, requesting the composer recompose this scope.
     *
     * This method is thread safe.
     */
    fun invalidate()
}

private const val changedLowBitMask = 0b001_001_001_001_001_001_001_001_001_001_0
private const val changedHighBitMask = changedLowBitMask shl 1
private const val changedMask = (changedLowBitMask or changedHighBitMask).inv()

/**
 * A compiler plugin utility function to change $changed flags from Different(10) to Same(01) for
 * when captured by restart lambdas. All parameters are passed with the same value as it was
 * previously invoked with and the changed flags should reflect that.
 */
@PublishedApi
internal fun updateChangedFlags(flags: Int): Int {
    val lowBits = flags and changedLowBitMask
    val highBits = flags and changedHighBitMask
    return ((flags and changedMask) or
        (lowBits or (highBits shr 1)) or ((lowBits shl 1) and highBits))
}

private const val UsedFlag = 0x01
private const val DefaultsInScopeFlag = 0x02
private const val DefaultsInvalidFlag = 0x04
private const val RequiresRecomposeFlag = 0x08
private const val SkippedFlag = 0x10
private const val RereadingFlag = 0x20
private const val ForcedRecomposeFlag = 0x40

internal interface RecomposeScopeOwner {
    fun invalidate(scope: RecomposeScopeImpl, instance: Any?): InvalidationResult
    fun recomposeScopeReleased(scope: RecomposeScopeImpl)
    fun recordReadOf(value: Any)
}

private val callbackLock = Any()

/**
 * A RecomposeScope is created for a region of the composition that can be recomposed independently
 * of the rest of the composition. The composer will position the slot table to the location
 * stored in [anchor] and call [block] when recomposition is requested. It is created by
 * [Composer.startRestartGroup] and is used to track how to restart the group.
 */
internal class RecomposeScopeImpl(
    owner: RecomposeScopeOwner?
) : ScopeUpdateScope, RecomposeScope {

    private var flags: Int = 0

    private var owner: RecomposeScopeOwner? = owner

    /**
     * An anchor to the location in the slot table that start the group associated with this
     * recompose scope.
     */
    var anchor: Anchor? = null

    /**
     * Return whether the scope is valid. A scope becomes invalid when the slots it updates are
     * removed from the slot table. For example, if the scope is in the then clause of an if
     * statement that later becomes false.
     */
    val valid: Boolean get() = owner != null && anchor?.valid ?: false

    val canRecompose: Boolean get() = block != null

    /**
     * Used is set when the [RecomposeScopeImpl] is used by, for example, [currentRecomposeScope].
     * This is used as the result of [Composer.endRestartGroup] and indicates whether the lambda
     * that is stored in [block] will be used.
     */
    var used: Boolean
        get() = flags and UsedFlag != 0
        set(value) {
            if (value) {
                flags = flags or UsedFlag
            } else {
                flags = flags and UsedFlag.inv()
            }
        }

    /**
     * Set to true when the there are function default calculations in the scope. These are
     * treated as a special case to avoid having to create a special scope for them. If these
     * change the this scope needs to be recomposed but the default values can be skipped if they
     * where not invalidated.
     */
    var defaultsInScope: Boolean
        get() = flags and DefaultsInScopeFlag != 0
        set(value) {
            if (value) {
                flags = flags or DefaultsInScopeFlag
            } else {
                flags = flags and DefaultsInScopeFlag.inv()
            }
        }

    /**
     * Tracks whether any of the calculations in the default values were changed. See
     * [defaultsInScope] for details.
     */
    var defaultsInvalid: Boolean
        get() = flags and DefaultsInvalidFlag != 0
        set(value) {
            if (value) {
                flags = flags or DefaultsInvalidFlag
            } else {
                flags = flags and DefaultsInvalidFlag.inv()
            }
        }

    /**
     * Tracks whether the scope was invalidated directly but was recomposed because the caller
     * was recomposed. This ensures that a scope invalidated directly will recompose even if its
     * parameters are the same as the previous recomposition.
     */
    var requiresRecompose: Boolean
        get() = flags and RequiresRecomposeFlag != 0
        set(value) {
            if (value) {
                flags = flags or RequiresRecomposeFlag
            } else {
                flags = flags and RequiresRecomposeFlag.inv()
            }
        }

    /**
     * The lambda to call to restart the scopes composition.
     */
    private var block: ((Composer, Int) -> Unit)? = null

    /**
     * The recompose scope observer, if one is registered.
     */
    @ExperimentalComposeRuntimeApi
    private var observer: RecomposeScopeObserver? = null

    /**
     * Restart the scope's composition. It is an error if [block] was not updated. The code
     * generated by the compiler ensures that when the recompose scope is used then [block] will
     * be set but it might occur if the compiler is out-of-date (or ahead of the runtime) or
     * incorrect direct calls to [Composer.startRestartGroup] and [Composer.endRestartGroup].
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    fun compose(composer: Composer) {
        val block = block
        val observer = observer
        if (observer != null && block != null) {
            observer.onBeginScopeComposition(this)
            try {
                block(composer, 1)
            } finally {
                observer.onEndScopeComposition(this)
            }
            return
        }
        block?.invoke(composer, 1) ?: error("Invalid restart scope")
    }

    @ExperimentalComposeRuntimeApi
    internal fun observe(observer: RecomposeScopeObserver): CompositionObserverHandle {
        synchronized(callbackLock) {
            this.observer = observer
        }
        return object : CompositionObserverHandle {
            override fun dispose() {
                synchronized(callbackLock) {
                    if (this@RecomposeScopeImpl.observer == observer) {
                        this@RecomposeScopeImpl.observer = null
                    }
                }
            }
        }
    }

    /**
     * Invalidate the group which will cause [owner] to request this scope be recomposed,
     * and an [InvalidationResult] will be returned.
     */
    fun invalidateForResult(value: Any?): InvalidationResult =
        owner?.invalidate(this, value) ?: InvalidationResult.IGNORED

    /**
     * Release the recompose scope. This is called when the recompose scope has been removed by the
     * compostion because the part of the composition it was tracking was removed.
     */
    fun release() {
        owner?.recomposeScopeReleased(this)
        owner = null
        trackedInstances = null
        trackedDependencies = null
        @OptIn(ExperimentalComposeRuntimeApi::class)
        observer?.onScopeDisposed(this)
    }

    /**
     * Called when the data tracked by this recompose scope moves to a different composition when
     * for example, the movable content it is part of has moved.
     */
    fun adoptedBy(owner: RecomposeScopeOwner) {
        this.owner = owner
    }

    /**
     * Invalidate the group which will cause [owner] to request this scope be recomposed.
     *
     * Unlike [invalidateForResult], this method is thread safe and calls the thread safe
     * invalidate on the composer.
     */
    override fun invalidate() {
        owner?.invalidate(this, null)
    }

    /**
     * Update [block]. The scope is returned by [Composer.endRestartGroup] when [used] is true
     * and implements [ScopeUpdateScope].
     */
    override fun updateScope(block: (Composer, Int) -> Unit) { this.block = block }

    private var currentToken = 0
    private var trackedInstances: MutableObjectIntMap<Any>? = null
    private var trackedDependencies: MutableScatterMap<DerivedState<*>, Any?>? = null
    private var rereading: Boolean
        get() = flags and RereadingFlag != 0
        set(value) {
            if (value) {
                flags = flags or RereadingFlag
            } else {
                flags = flags and RereadingFlag.inv()
            }
        }

    /**
     * Used to explicitly force recomposition. This is used during live edit to force a
     * recompose scope that doesn't have a restart callback to recompose as its parent (or
     * some parent above it) was invalidated and the path to this scope has also been forced.
     */
    var forcedRecompose: Boolean
        get() = flags and ForcedRecomposeFlag != 0
        set(value) {
            if (value) {
                flags = flags or ForcedRecomposeFlag
            } else {
                flags = flags and ForcedRecomposeFlag.inv()
            }
        }

    /**
     * Indicates whether the scope was skipped (e.g. [scopeSkipped] was called.
     */
    internal var skipped: Boolean
        get() = flags and SkippedFlag != 0
        private set(value) {
            if (value) {
                flags = flags or SkippedFlag
            } else {
                flags = flags and SkippedFlag.inv()
            }
        }

    /**
     * Called when composition start composing into this scope. The [token] is a value that is
     * unique everytime this is called. This is currently the snapshot id but that shouldn't be
     * relied on.
     */
    fun start(token: Int) {
        currentToken = token
        skipped = false
    }

    fun scopeSkipped() {
        skipped = true
    }

    /**
     * Track instances that were read in scope.
     * @return whether the value was already read in scope during current pass
     */
    fun recordRead(instance: Any): Boolean {
        if (rereading) return false // Re-reading should force composition to update its tracking

        val token = (trackedInstances ?: MutableObjectIntMap<Any>().also { trackedInstances = it })
            .put(instance, currentToken, default = -1)

        if (token == currentToken) {
            return true
        }

        if (instance is DerivedState<*>) {
            val tracked = trackedDependencies ?: MutableScatterMap<DerivedState<*>, Any?>().also {
                trackedDependencies = it
            }
            tracked[instance] = instance.currentRecord.currentValue
        }

        return false
    }

    /**
     * Returns true if the scope is observing derived state which might make this scope
     * conditionally invalidated.
     */
    val isConditional: Boolean get() = trackedDependencies != null

    /**
     * Determine if the scope should be considered invalid.
     *
     * @param instances The set of objects reported as invalidating this scope.
     */
    fun isInvalidFor(instances: IdentityArraySet<Any>?): Boolean {
        // If a non-empty instances exists and contains only derived state objects with their
        // default values, then the scope should not be considered invalid. Otherwise the scope
        // should if it was invalidated by any other kind of instance.
        if (instances == null) return true
        val trackedDependencies = trackedDependencies ?: return true
        if (
            instances.isNotEmpty() &&
            instances.all { instance ->
                instance is DerivedState<*> && instance.let {
                    @Suppress("UNCHECKED_CAST")
                    it as DerivedState<Any?>
                    val policy = it.policy ?: structuralEqualityPolicy()
                    policy.equivalent(it.currentRecord.currentValue, trackedDependencies[it])
                }
            }
        )
            return false
        return true
    }

    fun rereadTrackedInstances() {
        owner?.let { owner ->
            trackedInstances?.let { trackedInstances ->
                rereading = true
                try {
                    trackedInstances.forEach { value, _ ->
                        owner.recordReadOf(value)
                    }
                } finally {
                    rereading = false
                }
            }
        }
    }

    /**
     * Called when composition is completed for this scope. The [token] is the same token passed
     * in the previous call to [start]. If [end] returns a non-null value the lambda returned
     * will be called during [ControlledComposition.applyChanges].
     */
    fun end(token: Int): ((Composition) -> Unit)? {
        return trackedInstances?.let { instances ->
            // If any value previous observed was not read in this current composition
            // schedule the value to be removed from the observe scope and removed from the
            // observations tracked by the composition.
            // [skipped] is true if the scope was skipped. If the scope was skipped we should
            // leave the observations unmodified.
            if (
                !skipped && instances.any { _, instanceToken -> instanceToken != token }
            ) { composition ->
                if (
                    currentToken == token && instances == trackedInstances &&
                    composition is CompositionImpl
                ) {
                    instances.removeIf { instance, instanceToken ->
                        (instanceToken != token).also { remove ->
                            if (remove) {
                                composition.removeObservation(instance, this)
                                (instance as? DerivedState<*>)?.let {
                                    composition.removeDerivedStateObservation(it)
                                    trackedDependencies?.let { dependencies ->
                                        dependencies.remove(it)
                                        if (dependencies.size == 0) {
                                            trackedDependencies = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (instances.size == 0) trackedInstances = null
                }
            } else null
        }
    }

    companion object {
        internal fun adoptAnchoredScopes(
            slots: SlotWriter,
            anchors: List<Anchor>,
            newOwner: RecomposeScopeOwner
        ) {
            if (anchors.isNotEmpty()) {
                anchors.fastForEach { anchor ->
                    // The recompose scope is always at slot 0 of a restart group.
                    val recomposeScope = slots.slot(anchor, 0) as? RecomposeScopeImpl
                    // Check for null as the anchor might not be for a recompose scope
                    recomposeScope?.adoptedBy(newOwner)
                }
            }
        }

        internal fun hasAnchoredRecomposeScopes(slots: SlotTable, anchors: List<Anchor>) =
            anchors.isNotEmpty() && anchors.fastAny {
                slots.ownsAnchor(it) && slots.slot(slots.anchorIndex(it), 0) is RecomposeScopeImpl
            }
    }
}
