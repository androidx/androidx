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

@file:OptIn(InternalComposeApi::class)
@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.runtime

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.changelist.ChangeList
import androidx.compose.runtime.changelist.ComposerChangeListWriter
import androidx.compose.runtime.changelist.FixupList
import androidx.compose.runtime.collection.MultiValueMap
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.fastFilter
import androidx.compose.runtime.collection.sortedBy
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.invokeComposable
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastToSet
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.runtime.tooling.LocalCompositionErrorContext
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.runtime.tooling.attachComposeStackTrace
import androidx.compose.runtime.tooling.buildTrace
import androidx.compose.runtime.tooling.findLocation
import androidx.compose.runtime.tooling.findSubcompositionContextGroup
import androidx.compose.runtime.tooling.traceForGroup
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

private class GroupInfo(
    /**
     * The current location of the slot relative to the start location of the pending slot changes
     */
    var slotIndex: Int,

    /**
     * The current location of the first node relative the start location of the pending node
     * changes
     */
    var nodeIndex: Int,

    /** The current number of nodes the group contains after changes have been applied */
    var nodeCount: Int,
)

/**
 * An interface used during [ControlledComposition.applyChanges] and [Composition.dispose] to track
 * when [RememberObserver] instances and leave the composition an also allows recording [SideEffect]
 * calls.
 */
internal interface RememberManager {
    /** The [RememberObserver] is being remembered by a slot in the slot table. */
    fun remembering(instance: RememberObserverHolder)

    /** The [RememberObserver] is being forgotten by a slot in the slot table. */
    fun forgetting(instance: RememberObserverHolder)

    /**
     * The [effect] should be called when changes are being applied but after the remember/forget
     * notifications are sent.
     */
    fun sideEffect(effect: () -> Unit)

    /** The [ComposeNodeLifecycleCallback] is being deactivated. */
    fun deactivating(instance: ComposeNodeLifecycleCallback)

    /** The [ComposeNodeLifecycleCallback] is being released. */
    fun releasing(instance: ComposeNodeLifecycleCallback)

    /** The restart scope is pausing */
    fun rememberPausingScope(scope: RecomposeScopeImpl)

    /** The restart scope is resuming */
    fun startResumingScope(scope: RecomposeScopeImpl)

    /** The restart scope is finished resuming */
    fun endResumingScope(scope: RecomposeScopeImpl)
}

/**
 * Pending starts when the key is different than expected indicating that the structure of the tree
 * changed. It is used to determine how to update the nodes and the slot table when changes to the
 * structure of the tree is detected.
 */
private class Pending(val keyInfos: MutableList<KeyInfo>, val startIndex: Int) {
    var groupIndex: Int = 0

    init {
        requirePrecondition(startIndex >= 0) { "Invalid start index" }
    }

    private val usedKeys = mutableListOf<KeyInfo>()
    private val groupInfos = run {
        var runningNodeIndex = 0
        val result = MutableIntObjectMap<GroupInfo>()
        for (index in 0 until keyInfos.size) {
            val keyInfo = keyInfos[index]
            result[keyInfo.location] = GroupInfo(index, runningNodeIndex, keyInfo.nodes)
            runningNodeIndex += keyInfo.nodes
        }
        result
    }

    /**
     * A multi-map of keys from the previous composition. The keys can be retrieved in the order
     * they were generated by the previous composition.
     */
    val keyMap by lazy {
        multiMap<Any, KeyInfo>(keyInfos.size).also {
            for (index in 0 until keyInfos.size) {
                val keyInfo = keyInfos[index]
                it.add(keyInfo.joinedKey, keyInfo)
            }
        }
    }

    /** Get the next key information for the given key. */
    fun getNext(key: Int, dataKey: Any?): KeyInfo? {
        val joinedKey: Any = if (dataKey != null) JoinedKey(key, dataKey) else key
        return keyMap.removeFirst(joinedKey)
    }

    /** Record that this key info was generated. */
    fun recordUsed(keyInfo: KeyInfo) = usedKeys.add(keyInfo)

    val used: List<KeyInfo>
        get() = usedKeys

    // TODO(chuckj): This is a correct but expensive implementation (worst cases of O(N^2)). Rework
    // to O(N)
    fun registerMoveSlot(from: Int, to: Int) {
        if (from > to) {
            groupInfos.forEachValue { group ->
                val position = group.slotIndex
                if (position == from) group.slotIndex = to
                else if (position in to until from) group.slotIndex = position + 1
            }
        } else if (to > from) {
            groupInfos.forEachValue { group ->
                val position = group.slotIndex
                if (position == from) group.slotIndex = to
                else if (position in (from + 1) until to) group.slotIndex = position - 1
            }
        }
    }

    fun registerMoveNode(from: Int, to: Int, count: Int) {
        if (from > to) {
            groupInfos.forEachValue { group ->
                val position = group.nodeIndex
                if (position in from until from + count) group.nodeIndex = to + (position - from)
                else if (position in to until from) group.nodeIndex = position + count
            }
        } else if (to > from) {
            groupInfos.forEachValue { group ->
                val position = group.nodeIndex
                if (position in from until from + count) group.nodeIndex = to + (position - from)
                else if (position in (from + 1) until to) group.nodeIndex = position - count
            }
        }
    }

    @OptIn(InternalComposeApi::class)
    fun registerInsert(keyInfo: KeyInfo, insertIndex: Int) {
        groupInfos[keyInfo.location] = GroupInfo(-1, insertIndex, 0)
    }

    fun updateNodeCount(group: Int, newCount: Int): Boolean {
        val groupInfo = groupInfos[group]
        if (groupInfo != null) {
            val index = groupInfo.nodeIndex
            val difference = newCount - groupInfo.nodeCount
            groupInfo.nodeCount = newCount
            if (difference != 0) {
                groupInfos.forEachValue { childGroupInfo ->
                    if (childGroupInfo.nodeIndex >= index && childGroupInfo != groupInfo) {
                        val newIndex = childGroupInfo.nodeIndex + difference
                        if (newIndex >= 0) childGroupInfo.nodeIndex = newIndex
                    }
                }
            }
            return true
        }
        return false
    }

    @OptIn(InternalComposeApi::class)
    fun slotPositionOf(keyInfo: KeyInfo) = groupInfos[keyInfo.location]?.slotIndex ?: -1

    @OptIn(InternalComposeApi::class)
    fun nodePositionOf(keyInfo: KeyInfo) = groupInfos[keyInfo.location]?.nodeIndex ?: -1

    @OptIn(InternalComposeApi::class)
    fun updatedNodeCountOf(keyInfo: KeyInfo) =
        groupInfos[keyInfo.location]?.nodeCount ?: keyInfo.nodes
}

private class Invalidation(
    /** The recompose scope being invalidate */
    val scope: RecomposeScopeImpl,

    /** The index of the group in the slot table being invalidated. */
    var location: Int,

    /**
     * The instances invalidating the scope. If this is `null` or empty then the scope is
     * unconditionally invalid. If it contains instances it is only invalid if at least on of the
     * instances is changed. This is used to track `DerivedState<*>` changes and only treat the
     * scope as invalid if the instance has changed.
     *
     * Can contain a [ScatterSet] of instances, single instance or null.
     */
    var instances: Any?,
) {
    fun isInvalid(): Boolean = scope.isInvalidFor(instances)
}

/**
 * Internal compose compiler plugin API that is used to update the function the composer will call
 * to recompose a recomposition scope. This should not be used or called directly.
 */
@ComposeCompilerApi
public interface ScopeUpdateScope {
    /**
     * Called by generated code to update the recomposition scope with the function to call
     * recompose the scope. This is called by code generated by the compose compiler plugin and
     * should not be called directly.
     */
    public fun updateScope(block: (Composer, Int) -> Unit)
}

internal enum class InvalidationResult {
    /**
     * The invalidation was ignored because the associated recompose scope is no longer part of the
     * composition or has yet to be entered in the composition. This could occur for invalidations
     * called on scopes that are no longer part of composition or if the scope was invalidated
     * before [ControlledComposition.applyChanges] was called that will enter the scope into the
     * composition.
     */
    IGNORED,

    /**
     * The composition is not currently composing and the invalidation was recorded for a future
     * composition. A recomposition requested to be scheduled.
     */
    SCHEDULED,

    /**
     * The composition that owns the recompose scope is actively composing but the scope has already
     * been composed or is in the process of composing. The invalidation is treated as SCHEDULED
     * above.
     */
    DEFERRED,

    /**
     * The composition that owns the recompose scope is actively composing and the invalidated scope
     * has not been composed yet but will be recomposed before the composition completes. A new
     * recomposition was not scheduled for this invalidation.
     */
    IMMINENT,
}

/**
 * An instance to hold a value provided by [CompositionLocalProvider] and is created by the
 * [ProvidableCompositionLocal.provides] infix operator. If [canOverride] is `false`, the provided
 * value will not overwrite a potentially already existing value in the scope.
 *
 * This value cannot be created directly. It can only be created by using one of the `provides`
 * operators of [ProvidableCompositionLocal].
 *
 * @see ProvidableCompositionLocal.provides
 * @see ProvidableCompositionLocal.providesDefault
 * @see ProvidableCompositionLocal.providesComputed
 */
public class ProvidedValue<T>
internal constructor(
    /**
     * The composition local that is provided by this value. This is the left-hand side of the
     * [ProvidableCompositionLocal.provides] infix operator.
     */
    public val compositionLocal: CompositionLocal<T>,
    value: T?,
    private val explicitNull: Boolean,
    internal val mutationPolicy: SnapshotMutationPolicy<T>?,
    internal val state: MutableState<T>?,
    internal val compute: (CompositionLocalAccessorScope.() -> T)?,
    internal val isDynamic: Boolean,
) {
    private val providedValue: T? = value

    /**
     * The value provided by the [ProvidableCompositionLocal.provides] infix operator. This is the
     * right-hand side of the operator.
     */
    @Suppress("UNCHECKED_CAST")
    public val value: T
        get() = providedValue as T

    /**
     * This value is `true` if the provided value will override any value provided above it. This
     * value is `true` when using [ProvidableCompositionLocal.provides] but `false` when using
     * [ProvidableCompositionLocal.providesDefault].
     *
     * @see ProvidableCompositionLocal.provides
     * @see ProvidableCompositionLocal.providesDefault
     */
    @get:JvmName("getCanOverride")
    public var canOverride: Boolean = true
        private set

    @Suppress("UNCHECKED_CAST")
    internal val effectiveValue: T
        get() =
            when {
                explicitNull -> null as T
                state != null -> state.value
                providedValue != null -> providedValue
                else -> composeRuntimeError("Unexpected form of a provided value")
            }

    internal val isStatic
        get() = (explicitNull || value != null) && !isDynamic

    internal fun ifNotAlreadyProvided() = this.also { canOverride = false }
}

/**
 * This class is used internally by [movableContentOf]. Please see [movableContentOf] which has
 * documentation and example for how to use movable content. This class cannot be used directly.
 *
 * A Compose compiler plugin API. DO NOT call directly.
 *
 * An instance used to track the identity of the movable content. Using a holder object allows
 * creating unique movable content instances from the same instance of a lambda. This avoids using
 * the identity of a lambda instance as it can be merged into a singleton or merged by later
 * rewritings and using its identity might lead to unpredictable results that might change from the
 * debug and release builds.
 *
 * @see movableContentOf
 */
@InternalComposeApi
public class MovableContent<P>(public val content: @Composable (parameter: P) -> Unit) {
    internal var used: Boolean = false
}

/**
 * A Compose compiler plugin API. DO NOT call directly.
 *
 * A reference to the movable content state prior to changes being applied.
 */
@InternalComposeApi
public class MovableContentStateReference
internal constructor(
    internal val content: MovableContent<Any?>,
    internal val parameter: Any?,
    internal val composition: ControlledComposition,
    internal val slotTable: SlotTable,
    internal val anchor: Anchor,
    internal var invalidations: List<Pair<RecomposeScopeImpl, Any?>>,
    internal val locals: PersistentCompositionLocalMap,
    internal val nestedReferences: List<MovableContentStateReference>?,
) {
    /** Transfer any invalidations that may have accumulated since this reference was created. */
    internal fun transferPendingInvalidations() {
        if (anchor.valid) {
            invalidations =
                invalidations + (composition as CompositionImpl).extractInvalidationsOf(anchor)
        }
    }
}

/**
 * A Compose compiler plugin API. DO NOT call directly.
 *
 * A reference to the state of a [MovableContent] after changes have being applied. This is the
 * state that was removed from the `from` composition during [ControlledComposition.applyChanges]
 * and before it is inserted during [ControlledComposition.insertMovableContent].
 */
@InternalComposeApi
public class MovableContentState internal constructor(internal val slotTable: SlotTable) {

    /** Extract one or more states for movable content that is nested in the [slotTable]. */
    internal fun extractNestedStates(
        applier: Applier<*>,
        references: ObjectList<MovableContentStateReference>,
    ): ScatterMap<MovableContentStateReference, MovableContentState> {
        // We can only remove states that are contained in this states slot table so the references
        // with anchors not owned by the slotTable should be removed. We also should traverse the
        // slot table in order to avoid thrashing the gap buffer so the references are sorted.
        val referencesToExtract =
            references
                .fastFilter { slotTable.ownsAnchor(it.anchor) }
                .sortedBy { slotTable.anchorIndex(it.anchor) }
        if (referencesToExtract.isEmpty()) return emptyScatterMap()
        val result = mutableScatterMapOf<MovableContentStateReference, MovableContentState>()
        slotTable.write { writer ->
            fun closeToGroupContaining(group: Int) {
                while (writer.parent >= 0 && writer.currentGroupEnd <= group) {
                    writer.skipToGroupEnd()
                    writer.endGroup()
                }
            }
            fun openParent(parent: Int) {
                closeToGroupContaining(parent)
                while (writer.currentGroup != parent && !writer.isGroupEnd) {
                    if (parent < writer.nextGroup) {
                        writer.startGroup()
                    } else {
                        writer.skipGroup()
                    }
                }
                runtimeCheck(writer.currentGroup == parent) { "Unexpected slot table structure" }
                writer.startGroup()
            }
            referencesToExtract.forEach { reference ->
                val newGroup = writer.anchorIndex(reference.anchor)
                val newParent = writer.parent(newGroup)
                closeToGroupContaining(newParent)
                openParent(newParent)
                writer.advanceBy(newGroup - writer.currentGroup)
                val content =
                    extractMovableContentAtCurrent(
                        composition = reference.composition,
                        reference = reference,
                        slots = writer,
                        applier = applier,
                    )
                result[reference] = content
            }
            closeToGroupContaining(Int.MAX_VALUE)
        }
        return result
    }
}

private val SlotWriter.nextGroup
    get() = currentGroup + groupSize(currentGroup)

/**
 * Composer is the interface that is targeted by the Compose Kotlin compiler plugin and used by code
 * generation helpers. It is highly recommended that direct calls these be avoided as the runtime
 * assumes that the calls are generated by the compiler and contain only a minimum amount of state
 * validation.
 */
public sealed interface Composer {
    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Changes calculated and recorded during composition and are sent to [applier] which makes the
     * physical changes to the node tree implied by a composition.
     *
     * Composition has two discrete phases, 1) calculate and record changes and 2) making the
     * changes via the [applier]. While a [Composable] functions is executing, none of the [applier]
     * methods are called. The recorded changes are sent to the [applier] all at once after all
     * [Composable] functions have completed.
     */
    @ComposeCompilerApi public val applier: Applier<*>

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects that a new part of the composition is being created, that is, the composition will
     * insert new nodes into the resulting tree.
     */
    @ComposeCompilerApi public val inserting: Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects whether the [Composable] function can skip. Even if a [Composable] function is
     * called with the same parameters it might still need to run because, for example, a new value
     * was provided for a [CompositionLocal] created by [staticCompositionLocalOf].
     */
    @ComposeCompilerApi public val skipping: Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects whether the default parameter block of a [Composable] function is valid. This is
     * `false` if a [State] object read in the [startDefaults] group was modified since the last
     * time the [Composable] function was run.
     */
    @ComposeCompilerApi public val defaultsInvalid: Boolean

    /**
     * A Compose internal property. DO NOT call directly. Use [currentRecomposeScope] instead.
     *
     * The invalidation current invalidation scope. An new invalidation scope is created whenever
     * [startRestartGroup] is called. when this scope's [RecomposeScope.invalidate] is called then
     * lambda supplied to [endRestartGroup]'s [ScopeUpdateScope] will be scheduled to be run.
     */
    @InternalComposeApi public val recomposeScope: RecomposeScope?

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Return an object that can be used to uniquely identity of the current recomposition scope.
     * This identity will be the same even if the recompose scope instance changes.
     *
     * This is used internally by tooling track composable function invocations.
     */
    @ComposeCompilerApi public val recomposeScopeIdentity: Any?

    /**
     * A Compose internal property. DO NOT call directly. Use [currentCompositeKeyHash] instead.
     *
     * This a hash value used to map externally stored state to the composition. For example, this
     * is used by saved instance state to preserve state across activity lifetime boundaries.
     *
     * This value is likely but not guaranteed to be unique. There are known cases, such as for
     * loops without a unique [key], where the runtime does not have enough information to make the
     * compound key hash unique.
     */
    @Deprecated(
        "Prefer the higher-precision compositeKeyHashCode instead",
        ReplaceWith("compositeKeyHashCode"),
    )
    @InternalComposeApi
    public val compoundKeyHash: Int
        get() = compositeKeyHashCode.hashCode()

    /**
     * A Compose internal property. DO NOT call directly. Use [currentCompositeKeyHashCode] instead.
     *
     * This a hash value used to map externally stored state to the composition. For example, this
     * is used by saved instance state to preserve state across activity lifetime boundaries.
     *
     * This value is likely but not guaranteed to be unique. There are known cases, such as for
     * loops without a unique [key], where the runtime does not have enough information to make the
     * compound key hash unique.
     */
    @InternalComposeApi public val compositeKeyHashCode: CompositeKeyHashCode

    // Groups

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a replaceable group. A replaceable group is a group that cannot be moved during
     * execution and can only either inserted, removed, or replaced. For example, the group created
     * by most control flow constructs such as an `if` statement are replaceable groups.
     *
     * Warning: Versions of the compiler that generate calls to this function also contain subtle
     * bug that does not generate a group around a loop containing code that just creates composable
     * lambdas (AnimatedContent from androidx.compose.animation, for example) which makes replacing
     * the group unsafe and the this must treat this like a movable group. [startReplaceGroup] was
     * added that will replace the group as described above and is only called by versions of the
     * compiler that correctly generate code around loops that create lambdas. This method is kept
     * to maintain compatibility with code generated by older versions of the compose compiler
     * plugin.
     *
     * @param key A compiler generated key based on the source location of the call.
     */
    @ComposeCompilerApi public fun startReplaceableGroup(key: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a replaceable group.
     *
     * @see startRestartGroup
     */
    @ComposeCompilerApi public fun endReplaceableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a replace group. A replace group is a group that cannot be moved during must only
     * either be inserted, removed, or replaced. For example, the group created by most control flow
     * constructs such as an `if` statement are replaceable groups.
     *
     * Note: This method replaces [startReplaceableGroup] which is only generated by older versions
     * of the compose compiler plugin that predate the addition of this method. The runtime is now
     * required to replace the group if a different group is detected instead of treating it like a
     * movable group.
     *
     * @param key A compiler generated key based on the source location of the call.
     * @see endReplaceGroup
     */
    @ComposeCompilerApi public fun startReplaceGroup(key: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a replace group.
     *
     * @see startReplaceGroup
     */
    @ComposeCompilerApi public fun endReplaceGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a movable group. A movable group is one that can be moved based on the value of
     * [dataKey] which is typically supplied by the [key][androidx.compose.runtime.key] pseudo
     * compiler function.
     *
     * A movable group implements the semantics of [key][androidx.compose.runtime.key] which allows
     * the state and nodes generated by a loop to move with the composition implied by the key
     * passed to [key][androidx.compose.runtime.key].
     *
     * @param key a compiler generated key based on the source location of the call.
     * @param dataKey an additional object that is used as a second part of the key. This key
     *   produced from the `keys` parameter supplied to the [key][androidx.compose.runtime.key]
     *   pseudo compiler function.
     */
    @ComposeCompilerApi public fun startMovableGroup(key: Int, dataKey: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a movable group.
     *
     * @see startMovableGroup
     */
    @ComposeCompilerApi public fun endMovableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to start the group that calculates the default parameters of a [Composable] function.
     *
     * This method is called near the beginning of a [Composable] function with default parameters
     * and surrounds the remembered values or [Composable] calls necessary to produce the default
     * parameters. For example, for `model: Model = remember { DefaultModel() }` the call to
     * [remember] is called inside a [startDefaults] group.
     */
    @ComposeCompilerApi public fun startDefaults()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of defaults group.
     *
     * @see startDefaults
     */
    @ComposeCompilerApi public fun endDefaults()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to record a group for a [Composable] function and starts a group that can be
     * recomposed on demand based on the lambda passed to
     * [updateScope][ScopeUpdateScope.updateScope] when [endRestartGroup] is called
     *
     * @param key A compiler generated key based on the source location of the call.
     * @return the instance of the composer to use for the rest of the function.
     */
    @ComposeCompilerApi public fun startRestartGroup(key: Int): Composer

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to end a restart group.
     */
    @ComposeCompilerApi public fun endRestartGroup(): ScopeUpdateScope?

    /**
     * A Compose internal API. DO NOT call directly.
     *
     * Request movable content be inserted at the current location. This will schedule with the root
     * composition parent a call to [insertMovableContent] with the correct [MovableContentState] if
     * one was released in another part of composition.
     */
    @InternalComposeApi public fun insertMovableContent(value: MovableContent<*>, parameter: Any?)

    /**
     * A Compose internal API. DO NOT call directly.
     *
     * Perform a late composition that adds to the current late apply that will insert the given
     * references to [MovableContent] into the composition. If a [MovableContent] is paired then
     * this is a request to move a released [MovableContent] from a different location or from a
     * different composition. If it is not paired (i.e. the `second` [MovableContentStateReference]
     * is `null`) then new state for the [MovableContent] is inserted into the composition.
     */
    @InternalComposeApi
    public fun insertMovableContentReferences(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    )

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Record the source information string for a group. This must be immediately called after the
     * start of a group.
     *
     * @param sourceInformation An string value to that provides the compose tools enough
     *   information to calculate the source location of calls to composable functions.
     */
    public fun sourceInformation(sourceInformation: String)

    /**
     * A compose compiler plugin API. DO NOT call directly.
     *
     * Record a source information marker. This marker can be used in place of a group that would
     * have contained the information but was elided as the compiler plugin determined the group was
     * not necessary such as when a function is marked with [ReadOnlyComposable].
     *
     * @param key A compiler generated key based on the source location of the call.
     * @param sourceInformation An string value to that provides the compose tools enough
     *   information to calculate the source location of calls to composable functions.
     */
    public fun sourceInformationMarkerStart(key: Int, sourceInformation: String)

    /**
     * A compose compiler plugin API. DO NOT call directly.
     *
     * Record the end of the marked source information range.
     */
    public fun sourceInformationMarkerEnd()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Skips the composer to the end of the current group. This generated by the compiler to when
     * the body of a [Composable] function can be skipped typically because the parameters to the
     * function are equal to the values passed to it in the previous composition.
     */
    @ComposeCompilerApi public fun skipToGroupEnd()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Deactivates the content to the end of the group by treating content as if it was deleted and
     * replaces all slot table entries for calls to [cache] to be [Empty]. This must be called as
     * the first call for a group.
     */
    @ComposeCompilerApi public fun deactivateToEndGroup(changed: Boolean)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Skips the current group. This called by the compiler to indicate that the current group can
     * be skipped, for example, this is generated to skip the [startDefaults] group the default
     * group is was not invalidated.
     */
    @ComposeCompilerApi public fun skipCurrentGroup()

    // Nodes

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a group that tracks a the code that will create or update a node that is generated as
     * part of the tree implied by the composition.
     */
    @ComposeCompilerApi public fun startNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a group that tracks a the code that will create or update a node that is generated as
     * part of the tree implied by the composition. A reusable node can be reused in a reusable
     * group even if the group key is changed.
     */
    @ComposeCompilerApi public fun startReusableNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Report the [factory] that will be used to create the node that will be generated into the
     * tree implied by the composition. This will only be called if [inserting] is is `true`.
     *
     * @param factory a factory function that will generate a node that will eventually be supplied
     *   to [applier] though [Applier.insertBottomUp] and [Applier.insertTopDown].
     */
    @ComposeCompilerApi public fun <T> createNode(factory: () -> T)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Report that the node is still being used. This will be called in the same location as the
     * corresponding [createNode] when [inserting] is `false`.
     */
    @ComposeCompilerApi public fun useNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a node group.
     */
    @ComposeCompilerApi public fun endNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a reuse group. Unlike a movable group, in a reuse group if the [dataKey] changes the
     * composition shifts into a reusing state cause the composer to act like it is inserting (e.g.
     * [cache] acts as if all values are invalid, [changed] always returns true, etc.) even though
     * it is recomposing until it encounters a reusable node. If the node is reusable it temporarily
     * shifts into recomposition for the node and then shifts back to reusing for the children. If a
     * non-reusable node is generated the composer shifts to inserting for the node and all of its
     * children.
     *
     * @param key An compiler generated key based on the source location of the call.
     * @param dataKey A key provided by the [ReusableContent] composable function that is used to
     *   determine if the composition shifts into a reusing state for this group.
     */
    @ComposeCompilerApi public fun startReusableGroup(key: Int, dataKey: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a reusable group.
     */
    @ComposeCompilerApi public fun endReusableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Temporarily disable reusing if it is enabled.
     */
    @ComposeCompilerApi public fun disableReusing()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reenable reusing if it was previously enabled before the last call to [disableReusing].
     */
    @ComposeCompilerApi public fun enableReusing()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Return a marker for the current group that can be used in a call to [endToMarker].
     */
    @ComposeCompilerApi public val currentMarker: Int

    /**
     * Compose compiler plugin API. DO NOT call directly.
     *
     * Ends all the groups up to but not including the group that is the parent group when
     * [currentMarker] was called to produce [marker]. All groups ended must have been started with
     * either [startReplaceableGroup] or [startMovableGroup]. Ending other groups can cause the
     * state of the composer to become inconsistent.
     */
    @ComposeCompilerApi public fun endToMarker(marker: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Schedule [block] to called with [value]. This is intended to update the node generated by
     * [createNode] to changes discovered by composition.
     *
     * @param value the new value to be set into some property of the node.
     * @param block the block that sets the some property of the node to [value].
     */
    @ComposeCompilerApi public fun <V, T> apply(value: V, block: T.(V) -> Unit)

    // State

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Produce an object that will compare equal an iff [left] and [right] compare equal to some
     * [left] and [right] of a previous call to [joinKey]. This is used by [key] to handle multiple
     * parameters. Since the previous composition stored [left] and [right] in a "join key" object
     * this call is used to return the previous value without an allocation instead of blindly
     * creating a new value that will be immediately discarded.
     *
     * @param left the first part of a a joined key.
     * @param right the second part of a joined key.
     * @return an object that will compare equal to a value previously returned by [joinKey] iff
     *   [left] and [right] compare equal to the [left] and [right] passed to the previous call.
     */
    @ComposeCompilerApi public fun joinKey(left: Any?, right: Any?): Any

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Remember a value into the composition state. This is a primitive method used to implement
     * [remember].
     *
     * @return [Composer.Empty] when [inserting] is `true` or the value passed to
     *   [updateRememberedValue] from the previous composition.
     * @see cache
     */
    @ComposeCompilerApi public fun rememberedValue(): Any?

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Update the remembered value correspond to the previous call to [rememberedValue]. The [value]
     * will be returned by [rememberedValue] for the next composition.
     */
    @ComposeCompilerApi public fun updateRememberedValue(value: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Any?): Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Boolean): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Char): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Byte): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Short): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Int): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Float): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Long): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Double): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition using `===`
     * instead of `==` equality. This is used, for example, to check parameter values to determine
     * if they have changed for values that use value equality but, for correct behavior, the
     * composer needs reference equality.
     *
     * @param value the value to check
     * @return `true` if the value is === equal to the previous value and returns `false` when
     *   [value] is different.
     */
    @ComposeCompilerApi public fun changedInstance(value: Any?): Boolean = changed(value)

    // Scopes

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Mark [scope] as used. [endReplaceableGroup] will return `null` unless [recordUsed] is called
     * on the corresponding [scope]. This is called implicitly when [State] objects are read during
     * composition is called when [currentRecomposeScope] is called in the [Composable] function.
     */
    @InternalComposeApi public fun recordUsed(scope: RecomposeScope)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Generated by the compile to determine if the composable function should be executed. It may
     * not execute if parameter has not changed and the nothing else is forcing the function to
     * execute (such as its scope was invalidated or a static composition local it was changed) or
     * the composition is pausable and the composition is pausing.
     *
     * @param parametersChanged `true` if the parameters to the composable function have changed.
     *   This is also `true` if the composition is [inserting] or if content is being reused.
     * @param flags The `$changed` parameter that contains the forced recompose bit to allow the
     *   composer to disambiguate when the parameters changed due the execution being forced or if
     *   the parameters actually changed. This is only ambiguous in a [PausableComposition] and is
     *   necessary to determine if the function can be paused. The bits, other than 0, are reserved
     *   for future use (which would required the bit 31, which is unused in `$changed` values, to
     *   be set to indicate that the flags carry additional information). Passing the `$changed`
     *   flags directly, instead of masking the 0 bit, is more efficient as it allows less code to
     *   be generated per call to `shouldExecute` which is every called in every restartable
     *   function, as well as allowing for the API to be extended without a breaking changed.
     */
    @InternalComposeApi public fun shouldExecute(parametersChanged: Boolean, flags: Int): Boolean

    // Internal API

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Record a function to call when changes to the corresponding tree are applied to the
     * [applier]. This is used to implement [SideEffect].
     *
     * @param effect a lambda to invoke after the changes calculated up to this point have been
     *   applied.
     */
    @InternalComposeApi public fun recordSideEffect(effect: () -> Unit)

    /**
     * Returns the active set of CompositionLocals at the current position in the composition
     * hierarchy. This is a lower level API that can be used to export and access CompositionLocal
     * values outside of Composition.
     *
     * This API does not track reads of CompositionLocals and does not automatically dispatch new
     * values to previous readers when the value of a CompositionLocal changes. To use this API as
     * intended, you must set up observation manually. This means:
     * - For [non-static CompositionLocals][compositionLocalOf], composables reading this map need
     *   to observe the snapshot state for CompositionLocals being read to be notified when their
     *   values in this map change.
     * - For [static CompositionLocals][staticCompositionLocalOf], all composables including the
     *   composable reading this map will be recomposed and you will need to re-obtain this map to
     *   get the latest values.
     *
     * Most applications shouldn't use this API directly, and should instead use
     * [CompositionLocal.current].
     */
    public val currentCompositionLocalMap: CompositionLocalMap

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Return the [CompositionLocal] value associated with [key]. This is the primitive function
     * used to implement [CompositionLocal.current].
     *
     * @param key the [CompositionLocal] value to be retrieved.
     */
    @InternalComposeApi public fun <T> consume(key: CompositionLocal<T>): T

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Provide the given values for the associated [CompositionLocal] keys. This is the primitive
     * function used to implement [CompositionLocalProvider].
     *
     * @param values an array of value to provider key pairs.
     */
    @InternalComposeApi public fun startProviders(values: Array<out ProvidedValue<*>>)

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * End the provider group.
     *
     * @see startProviders
     */
    @InternalComposeApi public fun endProviders()

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Provide the given value for the associated [CompositionLocal] key. This is the primitive
     * function used to implement [CompositionLocalProvider].
     *
     * @param value a value to provider key pairs.
     */
    @InternalComposeApi public fun startProvider(value: ProvidedValue<*>)

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * End the provider group.
     *
     * @see startProvider
     */
    @InternalComposeApi public fun endProvider()

    /**
     * A tooling API function. DO NOT call directly.
     *
     * The data stored for the composition. This is used by Compose tools, such as the preview and
     * the inspector, to display or interpret the result of composition.
     */
    public val compositionData: CompositionData

    /**
     * A tooling API function. DO NOT call directly.
     *
     * Called by the inspector to inform the composer that it should collect additional information
     * about call parameters. By default, only collect parameter information for scopes that are
     * [recordUsed] has been called on. If [collectParameterInformation] is called it will attempt
     * to collect all calls even if the runtime doesn't need them.
     *
     * WARNING: calling this will result in a significant number of additional allocations that are
     * typically avoided.
     */
    public fun collectParameterInformation()

    /**
     * Schedules an [action] to be invoked when the recomposer finishes the next execution of a
     * frame. If a frame is currently in-progress, [action] will be invoked when the current frame
     * finishes. If a frame isn't currently in-progress, a new frame will be scheduled (if one
     * hasn't been already) and [action] will execute at the completion of the next frame.
     *
     * [action] will always execute on the applier thread.
     *
     * Note that [action] runs at the end of a frame scheduled by the recomposer. If a callback is
     * scheduled via this method during the initial composition, it will not execute until the
     * _next_ frame.
     *
     * @return A [CancellationHandle] that can be used to unregister the [action]. The returned
     *   handle is thread-safe and may be cancelled from any thread. Cancelling the handle only
     *   removes the callback from the queue. If [action] is currently executing, it will not be
     *   cancelled by this handle.
     */
    public fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Build a composition context that can be used to created a subcomposition. A composition
     * reference is used to communicate information from this composition to the subcompositions
     * such as the all the [CompositionLocal]s provided at the point the reference is created.
     */
    @InternalComposeApi public fun buildContext(): CompositionContext

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * The coroutine context for the composition. This is used, for example, to implement
     * [LaunchedEffect]. This context is managed by the [Recomposer].
     */
    @InternalComposeApi
    public val applyCoroutineContext: CoroutineContext
        @TestOnly get

    /** The composition that is used to control this composer. */
    public val composition: ControlledComposition
        @TestOnly get

    /**
     * Disable the collection of source information, that may introduce groups to store the source
     * information, in order to be able to more accurately calculate the actual number of groups a
     * composable function generates in a release build.
     *
     * This function is only safe to call in a test and will produce incorrect composition results
     * if called on a composer not under test.
     */
    @TestOnly public fun disableSourceInformation()

    public companion object {
        /**
         * A special value used to represent no value was stored (e.g. an empty slot). This is
         * returned, for example by [Composer.rememberedValue] while it is [Composer.inserting] is
         * `true`.
         */
        public val Empty: Any =
            object {
                override fun toString() = "Empty"
            }

        /**
         * Internal API for specifying a tracer used for instrumenting frequent operations, e.g.
         * recompositions.
         */
        @InternalComposeTracingApi
        public fun setTracer(tracer: CompositionTracer?) {
            compositionTracer = tracer
        }

        /**
         * Enable composition stack traces based on the source information. When this flag is
         * enabled, composition will record source information at runtime. When crash occurs,
         * Compose will append a suppressed exception that contains a stack trace pointing to the
         * place in composition closest to the crash.
         *
         * Note that:
         * - Recording source information introduces additional performance overhead, so this option
         *   should NOT be enabled in release builds.
         * - Compose ships with a minifier config that removes source information from the release
         *   builds. Enabling this flag in minified builds will have no effect.
         */
        @ExperimentalComposeRuntimeApi
        public fun setDiagnosticStackTraceEnabled(enabled: Boolean) {
            composeStackTraceEnabled = enabled
        }
    }
}

/**
 * A Compose compiler plugin API. DO NOT call directly.
 *
 * Cache, that is remember, a value in the composition data of a composition. This is used to
 * implement [remember] and used by the compiler plugin to generate more efficient calls to
 * [remember] when it determines these optimizations are safe.
 */
@ComposeCompilerApi
public inline fun <T> Composer.cache(invalid: Boolean, block: @DisallowComposableCalls () -> T): T {
    @Suppress("UNCHECKED_CAST")
    return rememberedValue().let {
        if (invalid || it === Composer.Empty) {
            val value = block()
            updateRememberedValue(value)
            value
        } else it
    } as T
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records source information that can be used for tooling to determine the source location of the
 * corresponding composable function. By default, this function is declared as having no
 * side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove it.
 */
@ComposeCompilerApi
public fun sourceInformation(composer: Composer, sourceInformation: String) {
    composer.sourceInformation(sourceInformation)
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records the start of a source information marker that can be used for tooling to determine the
 * source location of the corresponding composable function that otherwise don't require tracking
 * information such as [ReadOnlyComposable] functions. By default, this function is declared as
 * having no side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove
 * it.
 *
 * Important that both [sourceInformationMarkerStart] and [sourceInformationMarkerEnd] are removed
 * together or both kept. Removing only one will cause incorrect runtime behavior.
 */
@ComposeCompilerApi
public fun sourceInformationMarkerStart(composer: Composer, key: Int, sourceInformation: String) {
    composer.sourceInformationMarkerStart(key, sourceInformation)
}

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@InternalComposeTracingApi
public interface CompositionTracer {
    public fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String): Unit

    public fun traceEventEnd(): Unit

    public fun isTraceInProgress(): Boolean
}

@OptIn(InternalComposeTracingApi::class) private var compositionTracer: CompositionTracer? = null

internal var composeStackTraceEnabled: Boolean = false

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun isTraceInProgress(): Boolean =
    compositionTracer.let { it != null && it.isTraceInProgress() }

@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
@Deprecated(
    message = "Use the overload with \$dirty metadata instead",
    ReplaceWith("traceEventStart(key, dirty1, dirty2, info)"),
    DeprecationLevel.HIDDEN,
)
public fun traceEventStart(key: Int, info: String): Unit = traceEventStart(key, -1, -1, info)

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 *
 * @param key is a group key generated by the compiler plugin for the function being traced. This
 *   key is unique the function.
 * @param dirty1 $dirty metadata: forced-recomposition and function parameters 1..10 if present
 * @param dirty2 $dirty2 metadata: forced-recomposition and function parameters 11..20 if present
 * @param info is a user displayable string that describes the function for which this is the start
 *   event.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
    compositionTracer?.traceEventStart(key, dirty1, dirty2, info)
}

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun traceEventEnd() {
    compositionTracer?.traceEventEnd()
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records the end of a source information marker that can be used for tooling to determine the
 * source location of the corresponding composable function that otherwise don't require tracking
 * information such as [ReadOnlyComposable] functions. By default, this function is declared as
 * having no side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove
 * it.
 *
 * Important that both [sourceInformationMarkerStart] and [sourceInformationMarkerEnd] are removed
 * together or both kept. Removing only one will cause incorrect runtime behavior.
 */
@ComposeCompilerApi
public fun sourceInformationMarkerEnd(composer: Composer) {
    composer.sourceInformationMarkerEnd()
}

/** Implementation of a composer for a mutable tree. */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal class ComposerImpl(
    /** An adapter that applies changes to the tree using the Applier abstraction. */
    override val applier: Applier<*>,

    /** Parent of this composition; a [Recomposer] for root-level compositions. */
    private val parentContext: CompositionContext,

    /** The slot table to use to store composition data */
    private val slotTable: SlotTable,
    private val abandonSet: MutableSet<RememberObserver>,
    private var changes: ChangeList,
    private var lateChanges: ChangeList,
    private val observerHolder: CompositionObserverHolder,

    /** The composition that owns this composer */
    override val composition: CompositionImpl,
) : Composer {
    private val pendingStack = Stack<Pending?>()
    private var pending: Pending? = null
    private var nodeIndex: Int = 0
    private var groupNodeCount: Int = 0
    private var rGroupIndex: Int = 0
    private val parentStateStack = IntStack()
    private var nodeCountOverrides: IntArray? = null
    private var nodeCountVirtualOverrides: MutableIntIntMap? = null
    private var forceRecomposeScopes = false
    private var forciblyRecompose = false
    private var nodeExpected = false
    private val invalidations: MutableList<Invalidation> = mutableListOf()
    private val entersStack = IntStack()
    private var rootProvider: PersistentCompositionLocalMap = persistentCompositionLocalHashMapOf()
    private var providerUpdates: MutableIntObjectMap<PersistentCompositionLocalMap>? = null
    private var providersInvalid = false
    private val providersInvalidStack = IntStack()
    private var reusing = false
    private var reusingGroup = -1
    private var childrenComposing: Int = 0
    private var compositionToken: Int = 0

    private var sourceMarkersEnabled =
        parentContext.collectingSourceInformation || parentContext.collectingCallByInformation

    private val derivedStateObserver =
        object : DerivedStateObserver {
            override fun start(derivedState: DerivedState<*>) {
                childrenComposing++
            }

            override fun done(derivedState: DerivedState<*>) {
                childrenComposing--
            }
        }

    private val invalidateStack = Stack<RecomposeScopeImpl>()

    internal var isComposing = false
        private set

    internal var isDisposed = false
        private set

    internal val areChildrenComposing
        get() = childrenComposing > 0

    internal val hasPendingChanges: Boolean
        get() = changes.isNotEmpty()

    internal var reader: SlotReader = slotTable.openReader().also { it.close() }

    internal var insertTable =
        SlotTable().apply {
            if (parentContext.collectingSourceInformation) collectSourceInformation()
            if (parentContext.collectingCallByInformation) collectCalledByInformation()
        }

    private var writer: SlotWriter = insertTable.openWriter().also { it.close(true) }
    private var writerHasAProvider = false
    private var providerCache: PersistentCompositionLocalMap? = null
    internal var deferredChanges: ChangeList? = null

    private val changeListWriter = ComposerChangeListWriter(this, changes)
    private var insertAnchor: Anchor = insertTable.read { it.anchor(0) }
    private var insertFixups = FixupList()

    private var pausable: Boolean = false
    private var shouldPauseCallback: ShouldPauseCallback? = null

    internal val errorContext: CompositionErrorContextImpl? = CompositionErrorContextImpl(this)
        get() = if (sourceMarkersEnabled) field else null

    override val applyCoroutineContext: CoroutineContext =
        parentContext.effectCoroutineContext + (errorContext ?: EmptyCoroutineContext)

    /**
     * Inserts a "Replaceable Group" starting marker in the slot table at the current execution
     * position. A Replaceable Group is a group which cannot be moved between its siblings, but can
     * be removed or inserted. These groups are inserted by the compiler around branches of
     * conditional logic in Composable functions such as if expressions, when expressions, early
     * returns, and null-coalescing operators.
     *
     * A call to [startReplaceableGroup] must be matched with a corresponding call to
     * [endReplaceableGroup].
     *
     * Warning: Versions of the compiler that generate calls to this function also contain subtle
     * bug that does not generate a group around a loop containing code that just creates composable
     * lambdas (AnimatedContent from androidx.compose.animation, for example) which makes replacing
     * the group unsafe and the this must treat this like a movable group. [startReplaceGroup] was
     * added that will replace the group as described above and is only called by versions of the
     * compiler that correctly generate code around loops that create lambdas.
     *
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     *
     * @param key The source-location-based key for the group. Expected to be unique among its
     *   siblings.
     * @see [endReplaceableGroup]
     * @see [startMovableGroup]
     * @see [startRestartGroup]
     */
    @ComposeCompilerApi
    override fun startReplaceableGroup(key: Int) = start(key, null, GroupKind.Group, null)

    /**
     * Indicates the end of a "Replaceable Group" at the current execution position. A Replaceable
     * Group is a group which cannot be moved between its siblings, but can be removed or inserted.
     * These groups are inserted by the compiler around branches of conditional logic in Composable
     * functions such as if expressions, when expressions, early returns, and null-coalescing
     * operators.
     *
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     *
     * @see [startReplaceableGroup]
     */
    @ComposeCompilerApi override fun endReplaceableGroup() = endGroup()

    /** See [Composer.startReplaceGroup] */
    @ComposeCompilerApi
    override fun startReplaceGroup(key: Int) {
        val pending = pending
        if (pending != null) {
            start(key, null, GroupKind.Group, null)
            return
        }
        validateNodeNotExpected()

        updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, null, null)

        rGroupIndex++

        val reader = reader
        if (inserting) {
            reader.beginEmpty()
            writer.startGroup(key, Composer.Empty)
            enterGroup(false, null)
            return
        }
        val slotKey = reader.groupKey
        if (slotKey == key && !reader.hasObjectKey) {
            reader.startGroup()
            enterGroup(false, null)
            return
        }

        if (!reader.isGroupEnd) {
            // Delete the group that was not expected
            val removeIndex = nodeIndex
            val startSlot = reader.currentGroup
            recordDelete()
            val nodesToRemove = reader.skipGroup()
            changeListWriter.removeNode(removeIndex, nodesToRemove)

            invalidations.removeRange(startSlot, reader.currentGroup)
        }

        // Insert the new group
        reader.beginEmpty()
        inserting = true
        providerCache = null
        ensureWriter()
        val writer = writer
        writer.beginInsert()
        val startIndex = writer.currentGroup
        writer.startGroup(key, Composer.Empty)
        insertAnchor = writer.anchor(startIndex)
        enterGroup(false, null)
    }

    /** See [Composer.endReplaceGroup] */
    @ComposeCompilerApi override fun endReplaceGroup() = endGroup()

    /**
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     */
    @ComposeCompilerApi
    @Suppress("unused")
    override fun startDefaults() = start(defaultsKey, null, GroupKind.Group, null)

    /**
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     *
     * @see [startReplaceableGroup]
     */
    @ComposeCompilerApi
    @Suppress("unused")
    override fun endDefaults() {
        endGroup()
        val scope = currentRecomposeScope
        if (scope != null && scope.used) {
            scope.defaultsInScope = true
        }
    }

    @ComposeCompilerApi
    @Suppress("unused")
    override val defaultsInvalid: Boolean
        get() {
            return !skipping || providersInvalid || currentRecomposeScope?.defaultsInvalid == true
        }

    /**
     * Inserts a "Movable Group" starting marker in the slot table at the current execution
     * position. A Movable Group is a group which can be moved or reordered between its siblings and
     * retain slot table state, in addition to being removed or inserted. Movable Groups are more
     * expensive than other groups because when they are encountered with a mismatched key in the
     * slot table, they must be held on to temporarily until the entire parent group finishes
     * execution in case it moved to a later position in the group. Movable groups are only inserted
     * by the compiler as a result of calls to [key].
     *
     * A call to [startMovableGroup] must be matched with a corresponding call to [endMovableGroup].
     *
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     *
     * @param key The source-location-based key for the group. Expected to be unique among its
     *   siblings.
     * @param dataKey Additional identifying information to compound with [key]. If there are
     *   multiple values, this is expected to be compounded together with [joinKey]. Whatever value
     *   is passed in here is expected to have a meaningful [equals] and [hashCode] implementation.
     * @see [endMovableGroup]
     * @see [key]
     * @see [joinKey]
     * @see [startReplaceableGroup]
     * @see [startRestartGroup]
     */
    @ComposeCompilerApi
    override fun startMovableGroup(key: Int, dataKey: Any?) =
        start(key, dataKey, GroupKind.Group, null)

    /**
     * Indicates the end of a "Movable Group" at the current execution position. A Movable Group is
     * a group which can be moved or reordered between its siblings and retain slot table state, in
     * addition to being removed or inserted. These groups are only valid when they are inserted as
     * direct children of Container Groups. Movable Groups are more expensive than other groups
     * because when they are encountered with a mismatched key in the slot table, they must be held
     * on to temporarily until the entire parent group finishes execution in case it moved to a
     * later position in the group. Movable groups are only inserted by the compiler as a result of
     * calls to [key].
     *
     * Warning: This is expected to be executed by the compiler only and should not be called
     * directly from source code. Call this API at your own risk.
     *
     * @see [startMovableGroup]
     */
    @ComposeCompilerApi override fun endMovableGroup() = endGroup()

    /**
     * Start the composition. This should be called, and only be called, as the first group in the
     * composition.
     */
    @OptIn(InternalComposeApi::class)
    private fun startRoot() {
        rGroupIndex = 0
        reader = slotTable.openReader()
        startGroup(rootKey)

        // parent reference management
        parentContext.startComposing()
        val parentProvider = parentContext.getCompositionLocalScope()
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = changed(parentProvider)
        providerCache = null

        // Inform observer if one is defined
        if (!forceRecomposeScopes) {
            forceRecomposeScopes = parentContext.collectingParameterInformation
        }

        // Propagate collecting source information
        if (!sourceMarkersEnabled) {
            sourceMarkersEnabled = parentContext.collectingSourceInformation
        }

        rootProvider =
            if (sourceMarkersEnabled) {
                @Suppress("UNCHECKED_CAST") // ProvidableCompositionLocal to CompositionLocal
                parentProvider.putValue(
                    LocalCompositionErrorContext as CompositionLocal<Any?>,
                    StaticValueHolder(errorContext),
                )
            } else {
                parentProvider
            }

        rootProvider.read(LocalInspectionTables)?.let {
            it.add(compositionData)
            parentContext.recordInspectionTable(it)
        }

        startGroup(parentContext.compositeKeyHashCode.hashCode())
    }

    /**
     * End the composition. This should be called, and only be called, to end the first group in the
     * composition.
     */
    @OptIn(InternalComposeApi::class)
    private fun endRoot() {
        endGroup()
        parentContext.doneComposing()
        endGroup()
        changeListWriter.endRoot()
        finalizeCompose()
        reader.close()
        forciblyRecompose = false
        providersInvalid = providersInvalidStack.pop().asBool()
    }

    /** Discard a pending composition because an error was encountered during composition */
    @OptIn(InternalComposeApi::class)
    private fun abortRoot() {
        cleanUpCompose()
        pendingStack.clear()
        parentStateStack.clear()
        entersStack.clear()
        providersInvalidStack.clear()
        providerUpdates = null
        insertFixups.clear()
        compositeKeyHashCode = CompositeKeyHashCode(0)
        childrenComposing = 0
        nodeExpected = false
        inserting = false
        reusing = false
        isComposing = false
        forciblyRecompose = false
        reusingGroup = -1
        if (!reader.closed) {
            reader.close()
        }
        if (!writer.closed) {
            // We cannot just close the insert table as the state of the table is uncertain
            // here and writer.close() might throw.
            forceFreshInsertTable()
        }
    }

    internal fun changesApplied() {
        providerUpdates = null
    }

    /**
     * True if the composition is currently scheduling nodes to be inserted into the tree. During
     * first composition this is always true. During recomposition this is true when new nodes are
     * being scheduled to be added to the tree.
     */
    @ComposeCompilerApi
    override var inserting: Boolean = false
        private set

    /** True if the composition should be checking if the composable functions can be skipped. */
    @ComposeCompilerApi
    override val skipping: Boolean
        get() {
            return !inserting &&
                !reusing &&
                !providersInvalid &&
                currentRecomposeScope?.requiresRecompose == false &&
                !forciblyRecompose
        }

    /**
     * Returns the hash of the composite key calculated as a combination of the keys of all the
     * currently started groups via [startGroup].
     */
    @InternalComposeApi
    override var compositeKeyHashCode: CompositeKeyHashCode = EmptyCompositeKeyHashCode
        private set

    /**
     * Start collecting parameter information and line number information. This enables the tools
     * API to always be able to determine the parameter values of composable calls as well as the
     * source location of calls.
     */
    override fun collectParameterInformation() {
        forceRecomposeScopes = true
        sourceMarkersEnabled = true
        slotTable.collectSourceInformation()
        insertTable.collectSourceInformation()
        writer.updateToTableMaps()
    }

    override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
        return parentContext.scheduleFrameEndCallback(action)
    }

    @OptIn(InternalComposeApi::class)
    internal fun dispose() {
        trace("Compose:Composer.dispose") {
            parentContext.unregisterComposer(this)
            deactivate()
            applier.clear()
            isDisposed = true
        }
    }

    internal fun deactivate() {
        invalidateStack.clear()
        invalidations.clear()
        changes.clear()
        providerUpdates = null
    }

    internal fun forceRecomposeScopes(): Boolean {
        return if (!forceRecomposeScopes) {
            forceRecomposeScopes = true
            forciblyRecompose = true
            true
        } else {
            false
        }
    }

    /**
     * Start a group with the given key. During recomposition if the currently expected group does
     * not match the given key a group the groups emitted in the same parent group are inspected to
     * determine if one of them has this key and that group the first such group is moved (along
     * with any nodes emitted by the group) to the current position and composition continues. If no
     * group with this key is found, then the composition shifts into insert mode and new nodes are
     * added at the current position.
     *
     * @param key The key for the group
     */
    private fun startGroup(key: Int) = start(key, null, GroupKind.Group, null)

    private fun startGroup(key: Int, dataKey: Any?) = start(key, dataKey, GroupKind.Group, null)

    /** End the current group. */
    private fun endGroup() = end(isNode = false)

    @OptIn(InternalComposeApi::class)
    private fun skipGroup() {
        groupNodeCount += reader.skipGroup()
    }

    /**
     * Start emitting a node. It is required that [createNode] is called after [startNode]. Similar
     * to [startGroup], if, during recomposition, the current node does not have the provided key a
     * node with that key is scanned for and moved into the current position if found, if no such
     * node is found the composition switches into insert mode and a the node is scheduled to be
     * inserted at the current location.
     */
    override fun startNode() {
        start(nodeKey, null, GroupKind.Node, null)
        nodeExpected = true
    }

    override fun startReusableNode() {
        start(nodeKey, null, GroupKind.ReusableNode, null)
        nodeExpected = true
    }

    /**
     * Schedule a node to be created and inserted at the current location. This is only valid to
     * call when the composer is inserting.
     */
    @Suppress("UNUSED")
    override fun <T> createNode(factory: () -> T) {
        validateNodeExpected()
        runtimeCheck(inserting) { "createNode() can only be called when inserting" }
        val insertIndex = parentStateStack.peek()
        val groupAnchor = writer.anchor(writer.parent)
        groupNodeCount++
        insertFixups.createAndInsertNode(factory, insertIndex, groupAnchor)
    }

    /** Mark the node that was created by [createNode] as used by composition. */
    @OptIn(InternalComposeApi::class)
    override fun useNode() {
        validateNodeExpected()
        runtimeCheck(!inserting) { "useNode() called while inserting" }
        val node = reader.node
        changeListWriter.moveDown(node)

        if (reusing && node is ComposeNodeLifecycleCallback) {
            changeListWriter.useNode(node)
        }
    }

    /** Called to end the node group. */
    override fun endNode() = end(isNode = true)

    override fun startReusableGroup(key: Int, dataKey: Any?) {
        if (!inserting) {
            if (reader.groupKey == key && reader.groupAux != dataKey && reusingGroup < 0) {
                // Starting to reuse nodes
                reusingGroup = reader.currentGroup
                reusing = true
            }
        }
        start(key, null, GroupKind.Group, dataKey)
    }

    override fun endReusableGroup() {
        if (reusing && reader.parent == reusingGroup) {
            reusingGroup = -1
            reusing = false
        }
        end(isNode = false)
    }

    override fun disableReusing() {
        reusing = false
    }

    override fun enableReusing() {
        reusing = reusingGroup >= 0
    }

    fun startReuseFromRoot() {
        reusingGroup = rootKey
        reusing = true
    }

    fun endReuseFromRoot() {
        requirePrecondition(!isComposing && reusingGroup == rootKey) {
            "Cannot disable reuse from root if it was caused by other groups"
        }
        reusingGroup = -1
        reusing = false
    }

    override val currentMarker: Int
        get() = if (inserting) -writer.parent else reader.parent

    override fun endToMarker(marker: Int) {
        if (marker < 0) {
            // If the marker is negative then the marker is for the writer
            val writerLocation = -marker
            val writer = writer
            while (true) {
                val parent = writer.parent
                if (parent <= writerLocation) break
                end(writer.isNode(parent))
            }
        } else {
            // If the marker is positive then the marker is for the reader. However, if we are
            // inserting then we need to close the inserting groups first.
            if (inserting) {
                // We might be inserting, we need to close all the groups until we are no longer
                // inserting.
                val writer = writer
                while (inserting) {
                    end(writer.isNode(writer.parent))
                }
            }
            val reader = reader
            while (true) {
                val parent = reader.parent
                if (parent <= marker) break
                end(reader.isNode(parent))
            }
        }
    }

    /**
     * Schedule a change to be applied to a node's property. This change will be applied to the node
     * that is the current node in the tree which was either created by [createNode].
     */
    override fun <V, T> apply(value: V, block: T.(V) -> Unit) {
        if (inserting) {
            insertFixups.updateNode(value, block)
        } else {
            changeListWriter.updateNode(value, block)
        }
    }

    /**
     * Create a composed key that can be used in calls to [startGroup] or [startNode]. This will use
     * the key stored at the current location in the slot table to avoid allocating a new key.
     */
    @ComposeCompilerApi
    @OptIn(InternalComposeApi::class)
    override fun joinKey(left: Any?, right: Any?): Any =
        getKey(reader.groupObjectKey, left, right) ?: JoinedKey(left, right)

    /** Return the next value in the slot table and advance the current location. */
    @PublishedApi
    @OptIn(InternalComposeApi::class)
    internal fun nextSlot(): Any? =
        if (inserting) {
            validateNodeNotExpected()
            Composer.Empty
        } else
            reader.next().let {
                if (reusing && it !is ReusableRememberObserverHolder) Composer.Empty else it
            }

    @PublishedApi
    @OptIn(InternalComposeApi::class)
    internal fun nextSlotForCache(): Any? {
        return if (inserting) {
            validateNodeNotExpected()
            Composer.Empty
        } else
            reader.next().let {
                if (reusing && it !is ReusableRememberObserverHolder) Composer.Empty
                else if (it is RememberObserverHolder) it.wrapped else it
            }
    }

    /**
     * Determine if the current slot table value is equal to the given value, if true, the value is
     * scheduled to be skipped during [ControlledComposition.applyChanges] and [changes] return
     * false; otherwise [ControlledComposition.applyChanges] will update the slot table to [value].
     * In either case the composer's slot table is advanced.
     *
     * @param value the value to be compared.
     */
    @ComposeCompilerApi
    override fun changed(value: Any?): Boolean {
        return if (nextSlot() != value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    @ComposeCompilerApi
    override fun changedInstance(value: Any?): Boolean {
        return if (nextSlot() !== value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    @ComposeCompilerApi
    override fun changed(value: Char): Boolean {
        val next = nextSlot()
        if (next is Char) {
            val nextPrimitive: Char = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Byte): Boolean {
        val next = nextSlot()
        if (next is Byte) {
            val nextPrimitive: Byte = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Short): Boolean {
        val next = nextSlot()
        if (next is Short) {
            val nextPrimitive: Short = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Boolean): Boolean {
        val next = nextSlot()
        if (next is Boolean) {
            val nextPrimitive: Boolean = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Float): Boolean {
        val next = nextSlot()
        if (next is Float) {
            val nextPrimitive: Float = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Long): Boolean {
        val next = nextSlot()
        if (next is Long) {
            val nextPrimitive: Long = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Double): Boolean {
        val next = nextSlot()
        if (next is Double) {
            val nextPrimitive: Double = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    @ComposeCompilerApi
    override fun changed(value: Int): Boolean {
        val next = nextSlot()
        if (next is Int) {
            val nextPrimitive: Int = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    /**
     * Cache a value in the composition. During initial composition [block] is called to produce the
     * value that is then stored in the slot table. During recomposition, if [invalid] is false the
     * value is obtained from the slot table and [block] is not invoked. If [invalid] is false a new
     * value is produced by calling [block] and the slot table is updated to contain the new value.
     */
    @ComposeCompilerApi
    inline fun <T> cache(invalid: Boolean, block: () -> T): T {
        var result = nextSlotForCache()
        if (result === Composer.Empty || invalid) {
            val value = block()
            updateCachedValue(value)
            result = value
        }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun updateSlot(value: Any?) {
        nextSlot()
        updateValue(value)
    }

    /**
     * Schedule the current value in the slot table to be updated to [value].
     *
     * @param value the value to schedule to be written to the slot table.
     */
    @PublishedApi
    @OptIn(InternalComposeApi::class)
    internal fun updateValue(value: Any?) {
        if (inserting) {
            writer.update(value)
        } else {
            if (reader.hadNext) {
                // We need to update the slot we just read so which is is one previous to the
                // current group slot index.
                val groupSlotIndex = reader.groupSlotIndex - 1
                if (changeListWriter.pastParent) {
                    // The reader is after the first child of the group so we cannot reposition the
                    // writer to the parent to update it as this will cause the writer to navigate
                    // backward which violates the single pass, forward walking  nature of update.
                    // Using an anchored updated allows to to violate this principle just for
                    // updating slots as this is required if the update occurs after the writer has
                    // been moved past the parent.
                    changeListWriter.updateAnchoredValue(
                        value,
                        reader.anchor(reader.parent),
                        groupSlotIndex,
                    )
                } else {
                    // No children have been seen yet so we are still in a position where we can
                    // directly update the parent.
                    changeListWriter.updateValue(value, groupSlotIndex)
                }
            } else {
                // This uses an anchor for the same reason as `updateAnchoredValue` uses and anchor,
                // the writer might have advanced past the parent and we need to go back and update
                // the parent. As this is likely to never occur in an empty group, we don't bother
                // checking if the reader has moved so we don't need an anchored and un-anchored
                // version of the same function.
                changeListWriter.appendValue(reader.anchor(reader.parent), value)
            }
        }
    }

    /**
     * Schedule the current value in the slot table to be updated to [value].
     *
     * @param value the value to schedule to be written to the slot table.
     */
    @PublishedApi
    @OptIn(InternalComposeApi::class)
    internal fun updateCachedValue(value: Any?) {
        val toStore =
            if (value is RememberObserver) {
                val holder = RememberObserverHolder(value, rememberObserverAnchor())
                if (inserting) {
                    changeListWriter.remember(holder)
                }
                abandonSet.add(value)
                holder
            } else value
        updateValue(toStore)
    }

    private fun rememberObserverAnchor(): Anchor? =
        if (inserting) {
            if (writer.isAfterFirstChild) {
                var group = writer.currentGroup - 1
                var parent = writer.parent(group)
                while (parent != writer.parent && parent >= 0) {
                    group = parent
                    parent = writer.parent(group)
                }
                writer.anchor(group)
            } else null
        } else {
            if (reader.isAfterFirstChild) {
                var group = reader.currentGroup - 1
                var parent = reader.parent(group)
                while (parent != reader.parent && parent >= 0) {
                    group = parent
                    parent = reader.parent(group)
                }
                reader.anchor(group)
            } else null
        }

    private var _compositionData: CompositionData? = null

    override val compositionData: CompositionData
        get() {
            val data = _compositionData
            if (data == null) {
                val newData = CompositionDataImpl(composition)
                _compositionData = newData
                return newData
            }
            return data
        }

    /** Schedule a side effect to run when we apply composition changes. */
    override fun recordSideEffect(effect: () -> Unit) {
        changeListWriter.sideEffect(effect)
    }

    private fun currentCompositionLocalScope(): PersistentCompositionLocalMap {
        providerCache?.let {
            return it
        }
        return currentCompositionLocalScope(reader.parent)
    }

    override val currentCompositionLocalMap: CompositionLocalMap
        get() = currentCompositionLocalScope()

    /** Return the current [CompositionLocal] scope which was provided by a parent group. */
    private fun currentCompositionLocalScope(group: Int): PersistentCompositionLocalMap {
        if (inserting && writerHasAProvider) {
            var current = writer.parent
            while (current > 0) {
                if (
                    writer.groupKey(current) == compositionLocalMapKey &&
                        writer.groupObjectKey(current) == compositionLocalMap
                ) {
                    val providers = writer.groupAux(current) as PersistentCompositionLocalMap
                    providerCache = providers
                    return providers
                }
                current = writer.parent(current)
            }
        }
        if (reader.size > 0) {
            var current = group
            while (current > 0) {
                if (
                    reader.groupKey(current) == compositionLocalMapKey &&
                        reader.groupObjectKey(current) == compositionLocalMap
                ) {
                    val providers =
                        providerUpdates?.get(current)
                            ?: reader.groupAux(current) as PersistentCompositionLocalMap
                    providerCache = providers
                    return providers
                }
                current = reader.parent(current)
            }
        }
        providerCache = rootProvider
        return rootProvider
    }

    /**
     * Update (or create) the slots to record the providers. The providers maps are first the scope
     * followed by the map used to augment the parent scope. Both are needed to detect inserts,
     * updates and deletes to the providers.
     */
    private fun updateProviderMapGroup(
        parentScope: PersistentCompositionLocalMap,
        currentProviders: PersistentCompositionLocalMap,
    ): PersistentCompositionLocalMap {
        val providerScope = parentScope.mutate { it.putAll(currentProviders) }
        startGroup(providerMapsKey, providerMaps)
        updateSlot(providerScope)
        updateSlot(currentProviders)
        endGroup()
        return providerScope
    }

    @InternalComposeApi
    @Suppress("UNCHECKED_CAST")
    override fun startProvider(value: ProvidedValue<*>) {
        val parentScope = currentCompositionLocalScope()
        startGroup(providerKey, provider)
        val oldState =
            rememberedValue().let { if (it == Composer.Empty) null else it as ValueHolder<Any?> }
        val local = value.compositionLocal as CompositionLocal<Any?>
        val state = local.updatedStateOf(value as ProvidedValue<Any?>, oldState)
        val change = state != oldState
        if (change) {
            updateRememberedValue(state)
        }
        val providers: PersistentCompositionLocalMap
        val invalid: Boolean
        if (inserting) {
            providers =
                if (value.canOverride || !parentScope.contains(local)) {
                    parentScope.putValue(local, state)
                } else {
                    parentScope
                }
            invalid = false
            writerHasAProvider = true
        } else {
            val oldScope = reader.groupAux(reader.currentGroup) as PersistentCompositionLocalMap
            providers =
                when {
                    (!skipping || change) && (value.canOverride || !parentScope.contains(local)) ->
                        parentScope.putValue(local, state)
                    !change && !providersInvalid -> oldScope
                    providersInvalid -> parentScope
                    else -> oldScope
                }
            invalid = reusing || oldScope !== providers
        }
        if (invalid && !inserting) {
            recordProviderUpdate(providers)
        }
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = invalid
        providerCache = providers
        start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, providers)
    }

    private fun recordProviderUpdate(providers: PersistentCompositionLocalMap) {
        val providerUpdates =
            providerUpdates
                ?: run {
                    val newProviderUpdates = MutableIntObjectMap<PersistentCompositionLocalMap>()
                    this.providerUpdates = newProviderUpdates
                    newProviderUpdates
                }
        providerUpdates[reader.currentGroup] = providers
    }

    @InternalComposeApi
    override fun endProvider() {
        endGroup()
        endGroup()
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    @InternalComposeApi
    override fun startProviders(values: Array<out ProvidedValue<*>>) {
        val parentScope = currentCompositionLocalScope()
        startGroup(providerKey, provider)
        val providers: PersistentCompositionLocalMap
        val invalid: Boolean
        if (inserting) {
            val currentProviders = updateCompositionMap(values, parentScope)
            providers = updateProviderMapGroup(parentScope, currentProviders)
            invalid = false
            writerHasAProvider = true
        } else {
            val oldScope = reader.groupGet(0) as PersistentCompositionLocalMap
            val oldValues = reader.groupGet(1) as PersistentCompositionLocalMap
            val currentProviders = updateCompositionMap(values, parentScope, oldValues)
            // skipping is true iff parentScope has not changed.
            if (!skipping || reusing || oldValues != currentProviders) {
                providers = updateProviderMapGroup(parentScope, currentProviders)

                // Compare against the old scope as currentProviders might have modified the scope
                // back to the previous value. This could happen, for example, if currentProviders
                // and parentScope have a key in common and the oldScope had the same value as
                // currentProviders for that key. If the scope has not changed, because these
                // providers obscure a change in the parent as described above, re-enable skipping
                // for the child region.
                invalid = reusing || providers != oldScope
            } else {
                // Nothing has changed
                skipGroup()
                providers = oldScope
                invalid = false
            }
        }

        if (invalid && !inserting) {
            recordProviderUpdate(providers)
        }
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = invalid
        providerCache = providers
        start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, providers)
    }

    @InternalComposeApi
    override fun endProviders() {
        endGroup()
        endGroup()
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    @InternalComposeApi
    override fun <T> consume(key: CompositionLocal<T>): T = currentCompositionLocalScope().read(key)

    /**
     * Create or use a memoized [CompositionContext] instance at this position in the slot table.
     */
    override fun buildContext(): CompositionContext {
        startGroup(referenceKey, reference)
        if (inserting) writer.markGroup()

        var observerHolder = nextSlot() as? RememberObserverHolder
        if (observerHolder == null) {
            observerHolder =
                ReusableRememberObserverHolder(
                    CompositionContextHolder(
                        CompositionContextImpl(
                            this@ComposerImpl.compositeKeyHashCode,
                            forceRecomposeScopes,
                            sourceMarkersEnabled,
                            (composition as? CompositionImpl)?.observerHolder,
                        )
                    ),
                    after = null,
                )
            updateValue(observerHolder)
        }
        val holder = observerHolder.wrapped as CompositionContextHolder
        holder.ref.updateCompositionLocalScope(currentCompositionLocalScope())
        endGroup()

        return holder.ref
    }

    /**
     * The number of changes that have been scheduled to be applied during
     * [ControlledComposition.applyChanges].
     *
     * Slot table movement (skipping groups and nodes) will be coalesced so this number is possibly
     * less than the total changes detected.
     */
    internal val changeCount
        get() = changes.size

    internal val currentRecomposeScope: RecomposeScopeImpl?
        get() =
            invalidateStack.let {
                if (childrenComposing == 0 && it.isNotEmpty()) it.peek() else null
            }

    private fun ensureWriter() {
        if (writer.closed) {
            writer = insertTable.openWriter()
            // Append to the end of the table
            writer.skipToGroupEnd()
            writerHasAProvider = false
            providerCache = null
        }
    }

    private fun createFreshInsertTable() {
        runtimeCheck(writer.closed)
        forceFreshInsertTable()
    }

    private fun forceFreshInsertTable() {
        insertTable =
            SlotTable().apply {
                if (sourceMarkersEnabled) collectSourceInformation()
                if (parentContext.collectingCallByInformation) collectCalledByInformation()
            }
        writer = insertTable.openWriter().also { it.close(true) }
    }

    /** Start the reader group updating the data of the group if necessary */
    private fun startReaderGroup(isNode: Boolean, data: Any?) {
        if (isNode) {
            reader.startNode()
        } else {
            if (data != null && reader.groupAux !== data) {
                changeListWriter.updateAuxData(data)
            }
            reader.startGroup()
        }
    }

    private fun start(key: Int, objectKey: Any?, kind: GroupKind, data: Any?) {
        validateNodeNotExpected()

        updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, objectKey, data)

        if (objectKey == null) rGroupIndex++

        // Check for the insert fast path. If we are already inserting (creating nodes) then
        // there is no need to track insert, deletes and moves with a pending changes object.
        val isNode = kind.isNode
        if (inserting) {
            reader.beginEmpty()
            val startIndex = writer.currentGroup
            when {
                isNode -> writer.startNode(key, Composer.Empty)
                data != null -> writer.startData(key, objectKey ?: Composer.Empty, data)
                else -> writer.startGroup(key, objectKey ?: Composer.Empty)
            }
            pending?.let { pending ->
                val insertKeyInfo =
                    KeyInfo(
                        key = key,
                        objectKey = -1,
                        location = insertedGroupVirtualIndex(startIndex),
                        nodes = -1,
                        index = 0,
                    )
                pending.registerInsert(insertKeyInfo, nodeIndex - pending.startIndex)
                pending.recordUsed(insertKeyInfo)
            }
            enterGroup(isNode, null)
            return
        }

        val forceReplace = !kind.isReusable && reusing
        if (pending == null) {
            val slotKey = reader.groupKey
            if (!forceReplace && slotKey == key && objectKey == reader.groupObjectKey) {
                // The group is the same as what was generated last time.
                startReaderGroup(isNode, data)
            } else {
                pending = Pending(reader.extractKeys(), nodeIndex)
            }
        }

        val pending = pending
        var newPending: Pending? = null
        if (pending != null) {
            // Check to see if the key was generated last time from the keys collected above.
            val keyInfo = pending.getNext(key, objectKey)
            if (!forceReplace && keyInfo != null) {
                // This group was generated last time, use it.
                pending.recordUsed(keyInfo)

                // Move the slot table to the location where the information about this group is
                // stored. The slot information will move once the changes are applied so moving the
                // current of the slot table is sufficient.
                val location = keyInfo.location

                // Determine what index this group is in. This is used for inserting nodes into the
                // group.
                nodeIndex = pending.nodePositionOf(keyInfo) + pending.startIndex

                // Determine how to move the slot group to the correct position.
                val relativePosition = pending.slotPositionOf(keyInfo)
                val currentRelativePosition = relativePosition - pending.groupIndex
                pending.registerMoveSlot(relativePosition, pending.groupIndex)
                changeListWriter.moveReaderRelativeTo(location)
                reader.reposition(location)
                if (currentRelativePosition > 0) {
                    // The slot group must be moved, record the move to be performed during apply.
                    changeListWriter.moveCurrentGroup(currentRelativePosition)
                }
                startReaderGroup(isNode, data)
            } else {
                // The group is new, go into insert mode. All child groups will written to the
                // insertTable until the group is complete which will schedule the groups to be
                // inserted into in the table.
                reader.beginEmpty()
                inserting = true
                providerCache = null
                ensureWriter()
                writer.beginInsert()
                val startIndex = writer.currentGroup
                when {
                    isNode -> writer.startNode(key, Composer.Empty)
                    data != null -> writer.startData(key, objectKey ?: Composer.Empty, data)
                    else -> writer.startGroup(key, objectKey ?: Composer.Empty)
                }
                insertAnchor = writer.anchor(startIndex)
                val insertKeyInfo =
                    KeyInfo(
                        key = key,
                        objectKey = -1,
                        location = insertedGroupVirtualIndex(startIndex),
                        nodes = -1,
                        index = 0,
                    )
                pending.registerInsert(insertKeyInfo, nodeIndex - pending.startIndex)
                pending.recordUsed(insertKeyInfo)
                newPending = Pending(mutableListOf(), if (isNode) 0 else nodeIndex)
            }
        }

        enterGroup(isNode, newPending)
    }

    private fun enterGroup(isNode: Boolean, newPending: Pending?) {
        // When entering a group all the information about the parent should be saved, to be
        // restored when end() is called, and all the tracking counters set to initial state for the
        // group.
        pendingStack.push(pending)
        this.pending = newPending
        this.parentStateStack.push(groupNodeCount)
        this.parentStateStack.push(rGroupIndex)
        this.parentStateStack.push(nodeIndex)
        if (isNode) nodeIndex = 0
        groupNodeCount = 0
        rGroupIndex = 0
    }

    private fun exitGroup(expectedNodeCount: Int, inserting: Boolean) {
        // Restore the parent's state updating them if they have changed based on changes in the
        // children. For example, if a group generates nodes then the number of generated nodes will
        // increment the node index and the group's node count. If the parent is tracking structural
        // changes in pending then restore that too.
        val previousPending = pendingStack.pop()
        if (previousPending != null && !inserting) {
            previousPending.groupIndex++
        }
        this.pending = previousPending
        this.nodeIndex = parentStateStack.pop() + expectedNodeCount
        this.rGroupIndex = parentStateStack.pop()
        this.groupNodeCount = parentStateStack.pop() + expectedNodeCount
    }

    private fun end(isNode: Boolean) {
        // All the changes to the group (or node) have been recorded. All new nodes have been
        // inserted but it has yet to determine which need to be removed or moved. Note that the
        // changes are relative to the first change in the list of nodes that are changing.

        // The rGroupIndex for parent is two pack from the current stack top which has already been
        // incremented past this group needs to be offset by one.
        val rGroupIndex = parentStateStack.peek2() - 1
        if (inserting) {
            val parent = writer.parent
            updateCompositeKeyWhenWeExitGroup(
                writer.groupKey(parent),
                rGroupIndex,
                writer.groupObjectKey(parent),
                writer.groupAux(parent),
            )
        } else {
            val parent = reader.parent
            updateCompositeKeyWhenWeExitGroup(
                reader.groupKey(parent),
                rGroupIndex,
                reader.groupObjectKey(parent),
                reader.groupAux(parent),
            )
        }
        var expectedNodeCount = groupNodeCount
        val pending = pending
        if (pending != null && pending.keyInfos.size > 0) {
            // previous contains the list of keys as they were generated in the previous composition
            val previous = pending.keyInfos

            // current contains the list of keys in the order they need to be in the new composition
            val current = pending.used

            // usedKeys contains the keys that were used in the new composition, therefore if a key
            // doesn't exist in this set, it needs to be removed.
            val usedKeys = current.fastToSet()

            val placedKeys = mutableSetOf<KeyInfo>()
            var currentIndex = 0
            val currentEnd = current.size
            var previousIndex = 0
            val previousEnd = previous.size

            // Traverse the list of changes to determine startNode movement
            var nodeOffset = 0
            while (previousIndex < previousEnd) {
                val previousInfo = previous[previousIndex]
                if (!usedKeys.contains(previousInfo)) {
                    // If the key info was not used the group was deleted, remove the nodes in the
                    // group
                    val deleteOffset = pending.nodePositionOf(previousInfo)
                    changeListWriter.removeNode(
                        nodeIndex = deleteOffset + pending.startIndex,
                        count = previousInfo.nodes,
                    )
                    pending.updateNodeCount(previousInfo.location, 0)
                    changeListWriter.moveReaderRelativeTo(previousInfo.location)
                    reader.reposition(previousInfo.location)
                    recordDelete()
                    reader.skipGroup()

                    // Remove any invalidations pending for the group being removed. These are no
                    // longer part of the composition. The group being composed is one after the
                    // start of the group.
                    invalidations.removeRange(
                        previousInfo.location,
                        previousInfo.location + reader.groupSize(previousInfo.location),
                    )
                    previousIndex++
                    continue
                }

                if (previousInfo in placedKeys) {
                    // If the group was already placed in the correct location, skip it.
                    previousIndex++
                    continue
                }

                if (currentIndex < currentEnd) {
                    // At this point current should match previous unless the group is new or was
                    // moved.
                    val currentInfo = current[currentIndex]
                    if (currentInfo !== previousInfo) {
                        val nodePosition = pending.nodePositionOf(currentInfo)
                        placedKeys.add(currentInfo)
                        if (nodePosition != nodeOffset) {
                            val updatedCount = pending.updatedNodeCountOf(currentInfo)
                            changeListWriter.moveNode(
                                from = nodePosition + pending.startIndex,
                                to = nodeOffset + pending.startIndex,
                                count = updatedCount,
                            )
                            pending.registerMoveNode(nodePosition, nodeOffset, updatedCount)
                        } // else the nodes are already in the correct position
                    } else {
                        // The correct nodes are in the right location
                        previousIndex++
                    }
                    currentIndex++
                    nodeOffset += pending.updatedNodeCountOf(currentInfo)
                }
            }

            // If there are any current nodes left they where inserted into the right location
            // when the group began so the rest are ignored.
            changeListWriter.endNodeMovement()

            // We have now processed the entire list so move the slot table to the end of the list
            // by moving to the last key and skipping it.
            if (previous.size > 0) {
                changeListWriter.moveReaderRelativeTo(reader.groupEnd)
                reader.skipToGroupEnd()
            }
        }

        val inserting = inserting
        if (!inserting) {
            // Detect when slots were not used. This happens when a `remember` was removed at the
            // end of a group. Due to code generation issues (b/346821372) this may also see
            // remembers that were removed prior to the children being called so this must be done
            // before the children are deleted to ensure that the `RememberEventDispatcher` receives
            // the `leaving()` call in the correct order so the `onForgotten` is dispatched in the
            // correct order for the values being removed.
            val remainingSlots = reader.remainingSlots
            if (remainingSlots > 0) {
                changeListWriter.trimValues(remainingSlots)
            }
        }

        // Detect removing nodes at the end. No pending is created in this case we just have more
        // nodes in the previous composition than we expect (i.e. we are not yet at an end)
        val removeIndex = nodeIndex
        while (!reader.isGroupEnd) {
            val startSlot = reader.currentGroup
            recordDelete()
            val nodesToRemove = reader.skipGroup()
            changeListWriter.removeNode(removeIndex, nodesToRemove)
            invalidations.removeRange(startSlot, reader.currentGroup)
        }

        if (inserting) {
            if (isNode) {
                insertFixups.endNodeInsert()
                expectedNodeCount = 1
            }
            reader.endEmpty()
            val parentGroup = writer.parent
            writer.endGroup()
            if (!reader.inEmpty) {
                val virtualIndex = insertedGroupVirtualIndex(parentGroup)
                writer.endInsert()
                writer.close(true)
                recordInsert(insertAnchor)
                this.inserting = false
                if (!slotTable.isEmpty) {
                    updateNodeCount(virtualIndex, 0)
                    updateNodeCountOverrides(virtualIndex, expectedNodeCount)
                }
            }
        } else {
            if (isNode) changeListWriter.moveUp()
            changeListWriter.endCurrentGroup()
            val parentGroup = reader.parent
            val parentNodeCount = updatedNodeCount(parentGroup)
            if (expectedNodeCount != parentNodeCount) {
                updateNodeCountOverrides(parentGroup, expectedNodeCount)
            }
            if (isNode) {
                expectedNodeCount = 1
            }

            reader.endGroup()
            changeListWriter.endNodeMovement()
        }

        exitGroup(expectedNodeCount, inserting)
    }

    /**
     * Recompose any invalidate child groups of the current parent group. This should be called
     * after the group is started but on or before the first child group. It is intended to be
     * called instead of [skipReaderToGroupEnd] if any child groups are invalid. If no children are
     * invalid it will call [skipReaderToGroupEnd].
     */
    private fun recomposeToGroupEnd() {
        val wasComposing = isComposing
        isComposing = true
        var recomposed = false

        val parent = reader.parent
        val end = parent + reader.groupSize(parent)
        val recomposeIndex = nodeIndex
        val recomposeCompositeKey = this@ComposerImpl.compositeKeyHashCode
        val oldGroupNodeCount = groupNodeCount
        val oldRGroupIndex = rGroupIndex
        var oldGroup = parent

        var firstInRange = invalidations.firstInRange(reader.currentGroup, end)
        while (firstInRange != null) {
            val location = firstInRange.location
            val scope = firstInRange.scope

            invalidations.removeLocation(location)

            if (firstInRange.isInvalid()) {
                recomposed = true

                reader.reposition(location)
                val newGroup = reader.currentGroup
                // Record the changes to the applier location
                recordUpsAndDowns(oldGroup, newGroup, parent)
                oldGroup = newGroup

                // Calculate the node index (the distance index in the node this groups nodes are
                // located in the parent node).
                nodeIndex = nodeIndexOf(location, newGroup, parent, recomposeIndex)

                // Calculate the current rGroupIndex for this node, storing any parent rGroup
                // indexes we needed into the rGroup IntList
                rGroupIndex = rGroupIndexOf(newGroup)

                // Calculate the composite hash code (a semi-unique code for every group in the
                // composition used to restore saved state).
                val newParent = reader.parent(newGroup)
                this@ComposerImpl.compositeKeyHashCode =
                    compositeKeyOf(newParent, parent, recomposeCompositeKey)

                // We have moved so the cached lookup of the provider is invalid
                providerCache = null

                // Invoke the function with the same parameters as the last composition (which
                // were captured in the lambda set into the scope).
                scope.compose(this)

                // We could have moved out of a provider so the provider cache is invalid.
                providerCache = null

                // Restore the parent of the reader to the previous parent
                reader.restoreParent(parent)
            } else {
                // If the invalidation is not used restore the reads that were removed when the
                // the invalidation was recorded. This happens, for example, when on of a derived
                // state's dependencies changed but the derived state itself was not changed.
                invalidateStack.push(scope)
                val observer = observerHolder.current()
                if (observer != null) {
                    try {
                        observer.onScopeEnter(scope)
                        scope.rereadTrackedInstances()
                    } finally {
                        observer.onScopeExit(scope)
                    }
                } else {
                    scope.rereadTrackedInstances()
                }
                invalidateStack.pop()
            }

            // Using slots.current here ensures composition always walks forward even if a component
            // before the current composition is invalidated when performing this composition. Any
            // such components will be considered invalid for the next composition. Skipping them
            // prevents potential infinite recomposes at the cost of potentially missing a compose
            // as well as simplifies the apply as it always modifies the slot table in a forward
            // direction.
            firstInRange = invalidations.firstInRange(reader.currentGroup, end)
        }

        if (recomposed) {
            recordUpsAndDowns(oldGroup, parent, parent)
            reader.skipToGroupEnd()
            val parentGroupNodes = updatedNodeCount(parent)
            nodeIndex = recomposeIndex + parentGroupNodes
            groupNodeCount = oldGroupNodeCount + parentGroupNodes
            rGroupIndex = oldRGroupIndex
        } else {
            // No recompositions were requested in the range, skip it.
            skipReaderToGroupEnd()

            // No need to restore the parent state for nodeIndex, groupNodeCount and
            // rGroupIndex as they are going to be restored immediately by the endGroup
        }
        this@ComposerImpl.compositeKeyHashCode = recomposeCompositeKey

        isComposing = wasComposing
    }

    /**
     * The index in the insertTable overlap with indexes the slotTable so the group index used to
     * track newly inserted groups is set to be negative offset from -2. This reserves -1 as the
     * root index which is the parent value returned by the root groups of the slot table.
     *
     * This function will also restore a virtual index to its index in the insertTable which is not
     * needed here but could be useful for debugging.
     */
    private fun insertedGroupVirtualIndex(index: Int) = -2 - index

    /**
     * As operations to insert and remove nodes are recorded, the number of nodes that will be in
     * the group after changes are applied is maintained in a side overrides table. This method
     * updates that count and then updates any parent groups that include the nodes this group
     * emits.
     */
    private fun updateNodeCountOverrides(group: Int, newCount: Int) {
        // The value of group can be negative which indicates it is tracking an inserted group
        // instead of an existing group. The index is a virtual index calculated by
        // insertedGroupVirtualIndex which corresponds to the location of the groups to insert in
        // the insertTable.
        val currentCount = updatedNodeCount(group)
        if (currentCount != newCount) {
            // Update the overrides
            val delta = newCount - currentCount
            var current = group

            var minPending = pendingStack.size - 1
            while (current != -1) {
                val newCurrentNodes = updatedNodeCount(current) + delta
                updateNodeCount(current, newCurrentNodes)
                for (pendingIndex in minPending downTo 0) {
                    val pending = pendingStack.peek(pendingIndex)
                    if (pending != null && pending.updateNodeCount(current, newCurrentNodes)) {
                        minPending = pendingIndex - 1
                        break
                    }
                }
                @Suppress("LiftReturnOrAssignment")
                if (current < 0) {
                    current = reader.parent
                } else {
                    if (reader.isNode(current)) break
                    current = reader.parent(current)
                }
            }
        }
    }

    /**
     * Calculates the node index (the index in the child list of a node will appear in the resulting
     * tree) for [group]. Passing in [recomposeGroup] and its node index in [recomposeIndex] allows
     * the calculation to exit early if there is no node group between [group] and [recomposeGroup].
     */
    private fun nodeIndexOf(
        groupLocation: Int,
        group: Int,
        recomposeGroup: Int,
        recomposeIndex: Int,
    ): Int {
        // Find the anchor group which is either the recomposeGroup or the first parent node
        var anchorGroup = reader.parent(group)
        while (anchorGroup != recomposeGroup) {
            if (reader.isNode(anchorGroup)) break
            anchorGroup = reader.parent(anchorGroup)
        }

        var index = if (reader.isNode(anchorGroup)) 0 else recomposeIndex

        // An early out if the group and anchor are the same
        if (anchorGroup == group) return index

        // Walk down from the anchor group counting nodes of siblings in front of this group
        var current = anchorGroup
        val nodeIndexLimit = index + (updatedNodeCount(anchorGroup) - reader.nodeCount(group))
        loop@ while (index < nodeIndexLimit) {
            if (current == groupLocation) break
            current++
            while (current < groupLocation) {
                val end = current + reader.groupSize(current)
                if (groupLocation < end) continue@loop
                index += if (reader.isNode(current)) 1 else updatedNodeCount(current)
                current = end
            }
            break
        }
        return index
    }

    private fun rGroupIndexOf(group: Int): Int {
        var result = 0
        val parent = reader.parent(group)
        var child = parent + 1
        while (child < group) {
            if (!reader.hasObjectKey(child)) result++
            child += reader.groupSize(child)
        }
        return result
    }

    private fun updatedNodeCount(group: Int): Int {
        if (group < 0)
            return nodeCountVirtualOverrides?.let { if (it.contains(group)) it[group] else 0 } ?: 0
        val nodeCounts = nodeCountOverrides
        if (nodeCounts != null) {
            val override = nodeCounts[group]
            if (override >= 0) return override
        }
        return reader.nodeCount(group)
    }

    private fun updateNodeCount(group: Int, count: Int) {
        if (updatedNodeCount(group) != count) {
            if (group < 0) {
                val virtualCounts =
                    nodeCountVirtualOverrides
                        ?: run {
                            val newCounts = MutableIntIntMap()
                            nodeCountVirtualOverrides = newCounts
                            newCounts
                        }
                virtualCounts[group] = count
            } else {
                val nodeCounts =
                    nodeCountOverrides
                        ?: run {
                            val newCounts = IntArray(reader.size)
                            newCounts.fill(-1)
                            nodeCountOverrides = newCounts
                            newCounts
                        }
                nodeCounts[group] = count
            }
        }
    }

    private fun clearUpdatedNodeCounts() {
        nodeCountOverrides = null
        nodeCountVirtualOverrides = null
    }

    /**
     * Records the operations necessary to move the applier the node affected by the previous group
     * to the new group.
     */
    private fun recordUpsAndDowns(oldGroup: Int, newGroup: Int, commonRoot: Int) {
        val reader = reader
        val nearestCommonRoot = reader.nearestCommonRootOf(oldGroup, newGroup, commonRoot)

        // Record ups for the nodes between oldGroup and nearestCommonRoot
        var current = oldGroup
        while (current > 0 && current != nearestCommonRoot) {
            if (reader.isNode(current)) changeListWriter.moveUp()
            current = reader.parent(current)
        }

        // Record downs from nearestCommonRoot to newGroup
        doRecordDownsFor(newGroup, nearestCommonRoot)
    }

    private fun doRecordDownsFor(group: Int, nearestCommonRoot: Int) {
        if (group > 0 && group != nearestCommonRoot) {
            doRecordDownsFor(reader.parent(group), nearestCommonRoot)
            if (reader.isNode(group)) changeListWriter.moveDown(reader.nodeAt(group))
        }
    }

    /**
     * Calculate the composite key (a semi-unique key produced for every group in the composition)
     * for [group]. Passing in the [recomposeGroup] and [recomposeKey] allows this method to exit
     * early.
     */
    private fun compositeKeyOf(
        group: Int,
        recomposeGroup: Int,
        recomposeKey: CompositeKeyHashCode,
    ): CompositeKeyHashCode {
        // The general form of a group's compositeKey can be solved by recursively evaluating:
        // compositeKey(group) = ((compositeKey(parent(group)) rol 3)
        //      xor compositeKeyPart(group) rol 3) xor effectiveRGroupIndex
        //
        // To solve this without recursion, first expand the terms:
        // compositeKey(group) = (compositeKey(parent(group)) rol 6)
        //                      xor (compositeKeyPart(group) rol 3)
        //                      xor effectiveRGroupIndex
        //
        // Then rewrite this as an iterative XOR sum, where n represents the distance from the
        // starting node and takes the range 0 <= n < depth(group) and g - n represents the n-th
        // parent of g, and all terms are XOR-ed together:
        //
        // [compositeKeyPart(g - n) rol (6n + 3)] xor [rGroupIndexOf(g - n) rol (6n)]
        //
        // Because compositeKey(g - n) is known when (g - n) == recomposeGroup, we can terminate
        // early and substitute that iteration's terms with recomposeKey rol (6n).

        var keyRot = 3
        var rgiRot = 0
        var result = CompositeKeyHashCode(0)

        var parent = group
        while (parent >= 0) {
            if (parent == recomposeGroup) {
                result = result.bottomUpCompoundWith(recomposeKey, rgiRot)
                return result
            }

            val groupKey = reader.groupCompositeKeyPart(parent)
            if (groupKey == movableContentKey) {
                result = result.bottomUpCompoundWith(groupKey, rgiRot)
                return result
            }

            val effectiveRGroupIndex = if (reader.hasObjectKey(parent)) 0 else rGroupIndexOf(parent)
            result =
                result
                    .bottomUpCompoundWith(groupKey, keyRot)
                    .bottomUpCompoundWith(effectiveRGroupIndex, rgiRot)
            keyRot = (keyRot + 6) % CompositeKeyHashSizeBits
            rgiRot = (rgiRot + 6) % CompositeKeyHashSizeBits

            parent = reader.parent(parent)
        }

        return result
    }

    private fun SlotReader.groupCompositeKeyPart(group: Int): Int =
        if (hasObjectKey(group)) {
            groupObjectKey(group)?.let {
                when (it) {
                    is Enum<*> -> it.ordinal
                    is MovableContent<*> -> movableContentKey
                    else -> it.hashCode()
                }
            } ?: 0
        } else
            groupKey(group).let {
                if (it == reuseKey)
                    groupAux(group)?.let { aux ->
                        if (aux == Composer.Empty) it else aux.hashCode()
                    } ?: it
                else it
            }

    internal fun tryImminentInvalidation(scope: RecomposeScopeImpl, instance: Any?): Boolean {
        val anchor = scope.anchor ?: return false
        val slotTable = reader.table
        val location = anchor.toIndexFor(slotTable)
        if (isComposing && location >= reader.currentGroup) {
            // if we are invalidating a scope that is going to be traversed during this
            // composition.
            invalidations.insertIfMissing(location, scope, instance)
            return true
        }
        return false
    }

    @TestOnly
    internal fun parentKey(): Int {
        return if (inserting) {
            writer.groupKey(writer.parent)
        } else {
            reader.groupKey(reader.parent)
        }
    }

    /**
     * Skip a group. Skips the group at the current location. This is only valid to call if the
     * composition is not inserting.
     */
    @ComposeCompilerApi
    override fun skipCurrentGroup() {
        if (invalidations.isEmpty()) {
            skipGroup()
        } else {
            val reader = reader
            val key = reader.groupKey
            val dataKey = reader.groupObjectKey
            val aux = reader.groupAux
            val rGroupIndex = rGroupIndex
            updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, dataKey, aux)
            startReaderGroup(reader.isNode, null)
            recomposeToGroupEnd()
            reader.endGroup()
            updateCompositeKeyWhenWeExitGroup(key, rGroupIndex, dataKey, aux)
        }
    }

    private fun skipReaderToGroupEnd() {
        groupNodeCount = reader.parentNodes
        reader.skipToGroupEnd()
    }

    @ComposeCompilerApi
    override fun shouldExecute(parametersChanged: Boolean, flags: Int): Boolean {
        // We only want to pause when we are not resuming and only when inserting new content or
        // when reusing content. This 0 bit of `flags` is only 1 if this function was restarted by
        // the restart lambda. The other bits of this flags are currently all 0's and are reserved
        // for future use.
        if (((flags and 1) == 0) && (inserting || reusing)) {
            val callback = shouldPauseCallback ?: return true
            val scope = currentRecomposeScope ?: return true
            val pausing = callback.shouldPause()
            if (pausing && !scope.resuming) {
                scope.used = true
                // Force the composer back into the reusing state when this scope restarts.
                scope.reusing = reusing
                scope.paused = true
                // Remember a place-holder object to ensure all remembers are sent in the correct
                // order. The remember manager will record the remember callback for the resumed
                // content into a place-holder to ensure that, when the remember callbacks are
                // dispatched, the callbacks for the resumed content are dispatched in the same
                // order they would have been had the content not paused.
                changeListWriter.rememberPausingScope(scope)
                parentContext.reportPausedScope(scope)
                return false
            }
            return true
        }

        // Otherwise we should execute the function if the parameters have changed or when
        // skipping is disabled.
        return parametersChanged || !skipping
    }

    /** Skip to the end of the group opened by [startGroup]. */
    @ComposeCompilerApi
    override fun skipToGroupEnd() {
        runtimeCheck(groupNodeCount == 0) {
            "No nodes can be emitted before calling skipAndEndGroup"
        }

        // This can be called when inserting is true and `shouldExecute` returns false.
        // When `inserting` the writer is already at the end of the group so we don't need to
        // move the writer.
        if (!inserting) {
            currentRecomposeScope?.scopeSkipped()
            if (invalidations.isEmpty()) {
                skipReaderToGroupEnd()
            } else {
                recomposeToGroupEnd()
            }
        }
    }

    @ComposeCompilerApi
    override fun deactivateToEndGroup(changed: Boolean) {
        runtimeCheck(groupNodeCount == 0) {
            "No nodes can be emitted before calling dactivateToEndGroup"
        }
        if (!inserting) {
            if (!changed) {
                skipReaderToGroupEnd()
                return
            }
            val start = reader.currentGroup
            val end = reader.currentEnd
            changeListWriter.deactivateCurrentGroup()
            invalidations.removeRange(start, end)
            reader.skipToGroupEnd()
        }
    }

    /**
     * Start a restart group. A restart group creates a recompose scope and sets it as the current
     * recompose scope of the composition. If the recompose scope is invalidated then this group
     * will be recomposed. A recompose scope can be invalidated by calling invalidate on the object
     * returned by [androidx.compose.runtime.currentRecomposeScope].
     */
    @ComposeCompilerApi
    override fun startRestartGroup(key: Int): Composer {
        startReplaceGroup(key)
        addRecomposeScope()
        return this
    }

    private fun addRecomposeScope() {
        if (inserting) {
            val scope = RecomposeScopeImpl(composition as CompositionImpl)
            invalidateStack.push(scope)
            updateValue(scope)
            enterRecomposeScope(scope)
        } else {
            val invalidation = invalidations.removeLocation(reader.parent)
            val slot = reader.next()
            val scope =
                if (slot == Composer.Empty) {
                    // This code is executed when a previously deactivate region is becomes active
                    // again. See Composer.deactivateToEndGroup()
                    val newScope = RecomposeScopeImpl(composition as CompositionImpl)
                    updateValue(newScope)
                    newScope
                } else slot as RecomposeScopeImpl
            scope.requiresRecompose =
                invalidation != null ||
                    scope.forcedRecompose.also { forced ->
                        if (forced) scope.forcedRecompose = false
                    }
            invalidateStack.push(scope)
            enterRecomposeScope(scope)

            if (scope.paused) {
                scope.paused = false
                scope.resuming = true
                changeListWriter.startResumingScope(scope)
                if (!reusing && scope.reusing) {
                    reusing = true
                    scope.resetReusing = true
                }
            }
        }
    }

    private fun enterRecomposeScope(scope: RecomposeScopeImpl) {
        scope.start(compositionToken)
        observerHolder.current()?.onScopeEnter(scope)
    }

    /**
     * End a restart group. If the recompose scope was marked used during composition then a
     * [ScopeUpdateScope] is returned that allows attaching a lambda that will produce the same
     * composition as was produced by this group (including calling [startRestartGroup] and
     * [endRestartGroup]).
     */
    @ComposeCompilerApi
    override fun endRestartGroup(): ScopeUpdateScope? {
        // This allows for the invalidate stack to be out of sync since this might be called during
        // exception stack unwinding that might have not called the doneJoin/endRestartGroup in the
        // the correct order.
        val scope = if (invalidateStack.isNotEmpty()) invalidateStack.pop() else null
        if (scope != null) {
            scope.requiresRecompose = false
            exitRecomposeScope(scope)?.let { changeListWriter.endCompositionScope(it, composition) }
            if (scope.resuming) {
                scope.resuming = false
                changeListWriter.endResumingScope(scope)
                scope.reusing = false
                if (scope.resetReusing) {
                    scope.resetReusing = false
                    reusing = false
                }
            }
        }
        val result =
            if (scope != null && !scope.skipped && (scope.used || forceRecomposeScopes)) {
                if (scope.anchor == null) {
                    scope.anchor =
                        if (inserting) {
                            writer.anchor(writer.parent)
                        } else {
                            reader.anchor(reader.parent)
                        }
                }
                scope.defaultsInvalid = false
                scope
            } else {
                null
            }
        end(isNode = false)
        return result
    }

    private fun exitRecomposeScope(scope: RecomposeScopeImpl): ((Composition) -> Unit)? {
        observerHolder.current()?.onScopeExit(scope)
        return scope.end(compositionToken)
    }

    @InternalComposeApi
    override fun insertMovableContent(value: MovableContent<*>, parameter: Any?) {
        @Suppress("UNCHECKED_CAST")
        invokeMovableContentLambda(
            value as MovableContent<Any?>,
            currentCompositionLocalScope(),
            parameter,
            force = false,
        )
    }

    @OptIn(ExperimentalComposeApi::class)
    private fun invokeMovableContentLambda(
        content: MovableContent<Any?>,
        locals: PersistentCompositionLocalMap,
        parameter: Any?,
        force: Boolean,
    ) {
        // Start the movable content group
        startMovableGroup(movableContentKey, content)
        updateSlot(parameter)

        // All movable content has a composite hash value rooted at the content itself so the hash
        // value doesn't change as the content moves in the tree.
        val savedCompositeKeyHash = compositeKeyHashCode

        try {
            compositeKeyHashCode = CompositeKeyHashCode(movableContentKey)

            if (inserting) writer.markGroup()

            // Capture the local providers at the point of the invocation. This allows detecting
            // changes to the locals as the value moves well as enables finding the correct
            // providers
            // when applying late changes which might be very complicated otherwise.
            val providersChanged = if (inserting) false else reader.groupAux != locals
            if (providersChanged) recordProviderUpdate(locals)
            start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, locals)
            providerCache = null

            // Either insert a place-holder to be inserted later (either created new or moved from
            // another location) or (re)compose the movable content. This is forced if a new value
            // needs to be created as a late change.
            if (
                inserting &&
                    !force &&
                    (!ComposeRuntimeFlags.isMovableContentUsageTrackingEnabled || content.used)
            ) {
                writerHasAProvider = true

                // Create an anchor to the movable group
                val anchor = writer.anchor(writer.parent(writer.parent))
                val reference =
                    MovableContentStateReference(
                        content,
                        parameter,
                        composition,
                        insertTable,
                        anchor,
                        emptyList(),
                        currentCompositionLocalScope(),
                        null,
                    )
                parentContext.insertMovableContent(reference)
            } else {
                val savedProvidersInvalid = providersInvalid
                providersInvalid = providersChanged
                content.used = true
                invokeComposable(this, { content.content(parameter) })
                providersInvalid = savedProvidersInvalid
            }
        } catch (e: Throwable) {
            throw e.attachComposeStackTrace { currentStackTrace() }
        } finally {
            // Restore the state back to what is expected by the caller.
            endGroup()
            providerCache = null
            compositeKeyHashCode = savedCompositeKeyHash
            endMovableGroup()
        }
    }

    @InternalComposeApi
    override fun insertMovableContentReferences(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        var completed = false
        try {
            insertMovableContentGuarded(references)
            completed = true
        } finally {
            if (completed) {
                cleanUpCompose()
            } else {
                // if we finished with error, cleanup more aggressively
                abortRoot()
            }
        }
    }

    private fun insertMovableContentGuarded(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        changeListWriter.withChangeList(lateChanges) {
            changeListWriter.resetSlots()
            references.fastForEach { (to, from) ->
                val anchor = to.anchor
                val location = to.slotTable.anchorIndex(anchor)
                val effectiveNodeIndex = IntRef()
                // Insert content at the anchor point
                changeListWriter.determineMovableContentNodeIndex(effectiveNodeIndex, anchor)
                if (from == null) {
                    val toSlotTable = to.slotTable
                    if (toSlotTable == insertTable) {
                        // We are going to compose reading the insert table which will also
                        // perform an insert. This would then cause both a reader and a writer to
                        // be created simultaneously which will throw an exception. To prevent
                        // that we release the old insert table and replace it with a fresh one.
                        // This allows us to read from the old table and write to the new table.

                        // This occurs when the placeholder version of movable content was inserted
                        // but no content was available to move so we now need to create the
                        // content.

                        createFreshInsertTable()
                    }
                    to.slotTable.read { reader ->
                        reader.reposition(location)
                        changeListWriter.moveReaderToAbsolute(location)
                        val offsetChanges = ChangeList()
                        recomposeMovableContent {
                            changeListWriter.withChangeList(offsetChanges) {
                                withReader(reader) {
                                    changeListWriter.withoutImplicitRootStart {
                                        invokeMovableContentLambda(
                                            to.content,
                                            to.locals,
                                            to.parameter,
                                            force = true,
                                        )
                                    }
                                }
                            }
                        }
                        changeListWriter.includeOperationsIn(
                            other = offsetChanges,
                            effectiveNodeIndex = effectiveNodeIndex,
                        )
                    }
                } else {
                    // If the state was already removed from the from table then it will have a
                    // state recorded in the recomposer, retrieve that now if we can. If not the
                    // state is still in its original location, recompose over it there.
                    val resolvedState = parentContext.movableContentStateResolve(from)
                    val fromTable = resolvedState?.slotTable ?: from.slotTable
                    val fromAnchor = resolvedState?.slotTable?.anchor(0) ?: from.anchor
                    val nodesToInsert = fromTable.collectNodesFrom(fromAnchor)

                    // Insert nodes if necessary
                    if (nodesToInsert.isNotEmpty()) {
                        changeListWriter.copyNodesToNewAnchorLocation(
                            nodesToInsert,
                            effectiveNodeIndex,
                        )
                        if (to.slotTable == slotTable) {
                            // Inserting the content into the current slot table then we need to
                            // update the virtual node counts. Otherwise, we are inserting into
                            // a new slot table which is being created, not updated, so the virtual
                            // node counts do not need to be updated.
                            val group = slotTable.anchorIndex(anchor)
                            updateNodeCount(group, updatedNodeCount(group) + nodesToInsert.size)
                        }
                    }

                    // Copy the slot table into the anchor location
                    changeListWriter.copySlotTableToAnchorLocation(
                        resolvedState = resolvedState,
                        parentContext = parentContext,
                        from = from,
                        to = to,
                    )

                    fromTable.read { reader ->
                        withReader(reader) {
                            val newLocation = fromTable.anchorIndex(fromAnchor)
                            reader.reposition(newLocation)
                            changeListWriter.moveReaderToAbsolute(newLocation)
                            val offsetChanges = ChangeList()
                            changeListWriter.withChangeList(offsetChanges) {
                                changeListWriter.withoutImplicitRootStart {
                                    from.transferPendingInvalidations()
                                    recomposeMovableContent(
                                        from = from.composition,
                                        to = to.composition,
                                        reader.currentGroup,
                                        invalidations = from.invalidations,
                                    ) {
                                        invokeMovableContentLambda(
                                            to.content,
                                            to.locals,
                                            to.parameter,
                                            force = true,
                                        )
                                    }
                                }
                            }
                            changeListWriter.includeOperationsIn(
                                other = offsetChanges,
                                effectiveNodeIndex = effectiveNodeIndex,
                            )
                        }
                    }
                }
                changeListWriter.skipToEndOfCurrentGroup()
            }
            changeListWriter.endMovableContentPlacement()
            changeListWriter.moveReaderToAbsolute(0)
        }
    }

    private inline fun <R> withReader(reader: SlotReader, block: () -> R): R {
        val savedReader = this.reader
        val savedCountOverrides = nodeCountOverrides
        val savedProviderUpdates = providerUpdates
        nodeCountOverrides = null
        providerUpdates = null
        try {
            this.reader = reader
            return block()
        } finally {
            this.reader = savedReader
            nodeCountOverrides = savedCountOverrides
            providerUpdates = savedProviderUpdates
        }
    }

    private fun <R> recomposeMovableContent(
        from: ControlledComposition? = null,
        to: ControlledComposition? = null,
        index: Int? = null,
        invalidations: List<Pair<RecomposeScopeImpl, Any?>> = emptyList(),
        block: () -> R,
    ): R {
        val savedIsComposing = isComposing
        val savedNodeIndex = nodeIndex
        try {
            isComposing = true
            nodeIndex = 0
            invalidations.fastForEach { (scope, instances) ->
                if (instances != null) {
                    tryImminentInvalidation(scope, instances)
                } else {
                    tryImminentInvalidation(scope, null)
                }
            }
            return from?.delegateInvalidations(to, index ?: -1, block) ?: block()
        } finally {
            isComposing = savedIsComposing
            nodeIndex = savedNodeIndex
        }
    }

    @ComposeCompilerApi
    override fun sourceInformation(sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGroupSourceInformation(sourceInformation)
        }
    }

    @ComposeCompilerApi
    override fun sourceInformationMarkerStart(key: Int, sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGrouplessCallSourceInformationStart(key, sourceInformation)
        }
    }

    @ComposeCompilerApi
    override fun sourceInformationMarkerEnd() {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGrouplessCallSourceInformationEnd()
        }
    }

    override fun disableSourceInformation() {
        sourceMarkersEnabled = false
    }

    internal fun stackTraceForValue(value: Any?): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        return slotTable
            .findLocation { it === value || (it as? RememberObserverHolder)?.wrapped === value }
            ?.let { (groupIndex, dataIndex) ->
                stackTraceForGroup(groupIndex, dataIndex) + parentStackTrace()
            } ?: emptyList()
    }

    private fun currentStackTrace(): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        val trace = mutableListOf<ComposeStackTraceFrame>()
        trace.addAll(writer.buildTrace())
        trace.addAll(reader.buildTrace())

        return trace.apply { addAll(parentStackTrace()) }
    }

    private fun stackTraceForGroup(group: Int, dataOffset: Int?): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        return slotTable.read { it.traceForGroup(group, dataOffset) }
    }

    fun parentStackTrace(): List<ComposeStackTraceFrame> {
        val parentComposition = parentContext.composition as? CompositionImpl ?: return emptyList()
        val position = parentComposition.slotTable.findSubcompositionContextGroup(parentContext)

        return if (position != null) {
            parentComposition.slotTable.read { reader -> reader.traceForGroup(position, 0) } +
                parentComposition.composer.parentStackTrace()
        } else {
            emptyList()
        }
    }

    /**
     * Synchronously compose the initial composition of [content]. This collects all the changes
     * which must be applied by [ControlledComposition.applyChanges] to build the tree implied by
     * [content].
     */
    internal fun composeContent(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        content: @Composable () -> Unit,
        shouldPause: ShouldPauseCallback?,
    ) {
        runtimeCheck(changes.isEmpty()) { "Expected applyChanges() to have been called" }
        this.shouldPauseCallback = shouldPause
        try {
            doCompose(invalidationsRequested, content)
        } finally {
            this.shouldPauseCallback = null
        }
    }

    internal fun prepareCompose(block: () -> Unit) {
        runtimeCheck(!isComposing) { "Preparing a composition while composing is not supported" }
        isComposing = true
        try {
            block()
        } finally {
            isComposing = false
        }
    }

    /**
     * Synchronously recompose all invalidated groups. This collects the changes which must be
     * applied by [ControlledComposition.applyChanges] to have an effect.
     */
    internal fun recompose(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        shouldPause: ShouldPauseCallback?,
    ): Boolean {
        runtimeCheck(changes.isEmpty()) { "Expected applyChanges() to have been called" }
        // even if invalidationsRequested is empty we still need to recompose if the Composer has
        // some invalidations scheduled already. it can happen when during some parent composition
        // there were a change for a state which was used by the child composition. such changes
        // will be tracked and added into `invalidations` list.
        if (invalidationsRequested.size > 0 || invalidations.isNotEmpty() || forciblyRecompose) {
            shouldPauseCallback = shouldPause
            try {
                doCompose(invalidationsRequested, null)
            } finally {
                shouldPauseCallback = null
            }
            return changes.isNotEmpty()
        }
        return false
    }

    fun updateComposerInvalidations(invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>) {
        // Update any invalidations that have may have moved since they were added, removing any
        // that are no longer in the slot table.
        for (i in invalidations.lastIndex downTo 0) {
            val invalidation = invalidations[i]
            val anchor = invalidation.scope.anchor
            if (anchor != null && anchor.valid) {
                if (invalidation.location != anchor.location)
                    invalidation.location = anchor.location
            } else {
                invalidations.removeAt(i)
            }
        }

        // Add the requested invalidations
        invalidationsRequested.map.forEach { scope, instances ->
            scope as RecomposeScopeImpl
            val location = scope.anchor?.location ?: return@forEach
            invalidations.add(
                Invalidation(scope, location, instances.takeUnless { it === ScopeInvalidated })
            )
        }

        // Ensure the invalidations are in sorted order.
        invalidations.sortWith(InvalidationLocationAscending)
    }

    private fun doCompose(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        content: (@Composable () -> Unit)?,
    ) {
        runtimeCheck(!isComposing) { "Reentrant composition is not supported" }
        val observer = observerHolder.current()
        trace("Compose:recompose") {
            compositionToken = currentSnapshot().snapshotId.hashCode()
            providerUpdates = null
            updateComposerInvalidations(invalidationsRequested)
            nodeIndex = 0
            var complete = false
            isComposing = true
            observer?.onBeginComposition(composition)
            try {
                startRoot()

                // vv Experimental for forced
                val savedContent = nextSlot()
                if (savedContent !== content && content != null) {
                    updateValue(content as Any?)
                }
                // ^^ Experimental for forced

                // Ignore reads of derivedStateOf recalculations
                observeDerivedStateRecalculations(derivedStateObserver) {
                    if (content != null) {
                        startGroup(invocationKey, invocation)
                        invokeComposable(this, content)
                        endGroup()
                    } else if (
                        (forciblyRecompose || providersInvalid) &&
                            savedContent != null &&
                            savedContent != Composer.Empty
                    ) {
                        startGroup(invocationKey, invocation)
                        @Suppress("UNCHECKED_CAST")
                        invokeComposable(this, savedContent as @Composable () -> Unit)
                        endGroup()
                    } else {
                        skipCurrentGroup()
                    }
                }
                endRoot()
                complete = true
            } catch (e: Throwable) {
                throw e.attachComposeStackTrace { currentStackTrace() }
            } finally {
                observer?.onEndComposition(composition)
                isComposing = false
                invalidations.clear()
                if (!complete) abortRoot()
                createFreshInsertTable()
            }
        }
    }

    val hasInvalidations
        get() = invalidations.isNotEmpty()

    private val SlotReader.node
        get() = node(parent)

    private fun SlotReader.nodeAt(index: Int) = node(index)

    private fun validateNodeExpected() {
        runtimeCheck(nodeExpected) {
            "A call to createNode(), emitNode() or useNode() expected was not expected"
        }
        nodeExpected = false
    }

    private fun validateNodeNotExpected() {
        runtimeCheck(!nodeExpected) { "A call to createNode(), emitNode() or useNode() expected" }
    }

    private fun recordInsert(anchor: Anchor) {
        if (insertFixups.isEmpty()) {
            changeListWriter.insertSlots(anchor, insertTable)
        } else {
            changeListWriter.insertSlots(anchor, insertTable, insertFixups)
            insertFixups = FixupList()
        }
    }

    private fun recordDelete() {
        // It is import that the movable content is reported first so it can be removed before the
        // group itself is removed.
        reportFreeMovableContent(reader.currentGroup)
        changeListWriter.removeCurrentGroup()
    }

    /**
     * Report any movable content that the group contains as being removed and ready to be moved.
     * Returns true if the group itself was removed.
     *
     * Returns the number of nodes left in place which is used to calculate the node index of any
     * nested calls.
     *
     * @param groupBeingRemoved The group that is being removed from the table or 0 if the entire
     *   table is being removed.
     */
    private fun reportFreeMovableContent(groupBeingRemoved: Int) {

        fun createMovableContentReferenceForGroup(
            group: Int,
            nestedStates: List<MovableContentStateReference>?,
        ): MovableContentStateReference {
            @Suppress("UNCHECKED_CAST")
            val movableContent = reader.groupObjectKey(group) as MovableContent<Any?>
            val parameter = reader.groupGet(group, 0)
            val anchor = reader.anchor(group)
            val end = group + reader.groupSize(group)
            val invalidations = mutableListOf<Pair<RecomposeScopeImpl, Any?>>()
            this.invalidations.forEachInRange(group, end) {
                invalidations += it.scope to it.instances
            }
            val reference =
                MovableContentStateReference(
                    movableContent,
                    parameter,
                    composition,
                    slotTable,
                    anchor,
                    invalidations,
                    currentCompositionLocalScope(group),
                    nestedStates,
                )
            return reference
        }

        fun movableContentReferenceFor(group: Int): MovableContentStateReference? {
            val key = reader.groupKey(group)
            val objectKey = reader.groupObjectKey(group)
            return if (key == movableContentKey && objectKey is MovableContent<*>) {
                val nestedStates =
                    if (reader.containsMark(group)) {
                        val nestedStates = mutableListOf<MovableContentStateReference>()
                        fun traverseGroups(group: Int) {
                            val size = reader.groupSize(group)
                            val end = group + size
                            var current = group + 1
                            while (current < end) {
                                if (reader.hasMark(current)) {
                                    movableContentReferenceFor(current)?.let {
                                        nestedStates.add(it)
                                    }
                                } else if (reader.containsMark(current)) traverseGroups(current)
                                current += reader.groupSize(current)
                            }
                        }
                        traverseGroups(group)
                        nestedStates.takeIf { it.isNotEmpty() }
                    } else null
                createMovableContentReferenceForGroup(group, nestedStates)
            } else null
        }

        fun reportGroup(group: Int, needsNodeDelete: Boolean, nodeIndex: Int): Int {
            val reader = reader
            return if (reader.hasMark(group)) {
                // If the group has a mark then it is either a movable content group or a
                // composition context group
                val key = reader.groupKey(group)
                val objectKey = reader.groupObjectKey(group)
                if (key == movableContentKey && objectKey is MovableContent<*>) {
                    // If the group is a movable content block schedule it to be removed and report
                    // that it is free to be moved to the parentContext. Nested movable content is
                    // recomposed if necessary once the group has been claimed by another insert.
                    // reportMovableContentForGroup(group)
                    // reportMovableContentAt(group)
                    val reference = movableContentReferenceFor(group)
                    if (reference != null) {
                        parentContext.deletedMovableContent(reference)
                        changeListWriter.recordSlotEditing()
                        changeListWriter.releaseMovableGroupAtCurrent(
                            composition,
                            parentContext,
                            reference,
                        )
                    }
                    if (needsNodeDelete && group != groupBeingRemoved) {
                        changeListWriter.endNodeMovementAndDeleteNode(nodeIndex, group)
                        0 // These nodes were deleted
                    } else reader.nodeCount(group)
                } else if (key == referenceKey && objectKey == reference) {
                    // Group is a composition context reference. As this is being removed assume
                    // all movable groups in the composition that have this context will also be
                    // released when the compositions are disposed.
                    val observerHolder = reader.groupGet(group, 0) as? RememberObserverHolder
                    val contextHolder = observerHolder?.wrapped as? CompositionContextHolder
                    if (contextHolder != null) {
                        // The contextHolder can be EMPTY in cases where the content has been
                        // deactivated. Content is deactivated if the content is just being
                        // held onto for recycling and is not otherwise active. In this case
                        // the composers we are likely to find here have already been disposed.
                        val compositionContext = contextHolder.ref
                        compositionContext.composers.forEach { composer ->
                            composer.reportAllMovableContent()

                            // Mark the composition as being removed so it will not be recomposed
                            // this turn.
                            parentContext.reportRemovedComposition(composer.composition)
                        }
                    }
                    reader.nodeCount(group)
                } else if (reader.isNode(group)) 1 else reader.nodeCount(group)
            } else if (reader.containsMark(group)) {
                // Traverse the group freeing the child movable content. This group is known to
                // have at least one child that contains movable content because the group is
                // marked as containing a mark
                val size = reader.groupSize(group)
                val end = group + size
                var current = group + 1
                var runningNodeCount = 0
                while (current < end) {
                    // A tree is not disassembled when it is removed, the root nodes of the
                    // sub-trees are removed, therefore, if we enter a node that contains movable
                    // content, the nodes should be removed so some future composition can
                    // re-insert them at a new location. Otherwise the applier will attempt to
                    // insert a node that already has a parent. If there is no node between the
                    // group removed and this group then the nodes will be removed by normal
                    // recomposition.
                    val isNode = reader.isNode(current)
                    if (isNode) {
                        changeListWriter.endNodeMovement()
                        changeListWriter.moveDown(reader.node(current))
                    }
                    runningNodeCount +=
                        reportGroup(
                            group = current,
                            needsNodeDelete = isNode || needsNodeDelete,
                            nodeIndex = if (isNode) 0 else nodeIndex + runningNodeCount,
                        )
                    if (isNode) {
                        changeListWriter.endNodeMovement()
                        changeListWriter.moveUp()
                    }
                    current += reader.groupSize(current)
                }
                if (reader.isNode(group)) 1 else runningNodeCount
            } else if (reader.isNode(group)) 1 else reader.nodeCount(group)
        }
        // If the group that is being deleted is a node we need to remove any children that
        // are moved.
        val rootIsNode = reader.isNode(groupBeingRemoved)
        if (rootIsNode) {
            changeListWriter.endNodeMovement()
            changeListWriter.moveDown(reader.node(groupBeingRemoved))
        }
        reportGroup(groupBeingRemoved, needsNodeDelete = rootIsNode, nodeIndex = 0)
        changeListWriter.endNodeMovement()
        if (rootIsNode) {
            changeListWriter.moveUp()
        }
    }

    /**
     * Called during composition to report all the content of the composition will be released as
     * this composition is to be disposed.
     */
    private fun reportAllMovableContent() {
        if (slotTable.containsMark()) {
            composition.updateMovingInvalidations()
            val changes = ChangeList()
            deferredChanges = changes
            slotTable.read { reader ->
                this.reader = reader
                changeListWriter.withChangeList(changes) {
                    reportFreeMovableContent(0)
                    changeListWriter.releaseMovableContent()
                }
            }
        }
    }

    private fun finalizeCompose() {
        changeListWriter.finalizeComposition()
        runtimeCheck(pendingStack.isEmpty()) { "Start/end imbalance" }
        cleanUpCompose()
    }

    private fun cleanUpCompose() {
        pending = null
        nodeIndex = 0
        groupNodeCount = 0
        compositeKeyHashCode = EmptyCompositeKeyHashCode
        nodeExpected = false
        changeListWriter.resetTransientState()
        invalidateStack.clear()
        clearUpdatedNodeCounts()
    }

    internal fun verifyConsistent() {
        insertTable.verifyWellFormed()
    }

    /**
     * A holder that will dispose of its [CompositionContext] when it leaves the composition that
     * will not have its reference made visible to user code.
     */
    internal class CompositionContextHolder(val ref: CompositionContextImpl) : RememberObserver {

        override fun onRemembered() {}

        override fun onAbandoned() {
            ref.dispose()
        }

        override fun onForgotten() {
            ref.dispose()
        }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    internal inner class CompositionContextImpl(
        override val compositeKeyHashCode: CompositeKeyHashCode,
        override val collectingParameterInformation: Boolean,
        override val collectingSourceInformation: Boolean,
        override val observerHolder: CompositionObserverHolder?,
    ) : CompositionContext() {
        var inspectionTables: MutableSet<MutableSet<CompositionData>>? = null
        val composers = mutableSetOf<ComposerImpl>()

        override val collectingCallByInformation: Boolean
            get() = parentContext.collectingCallByInformation

        fun dispose() {
            if (composers.isNotEmpty()) {
                inspectionTables?.let {
                    for (composer in composers) {
                        for (table in it) table.remove(composer.compositionData)
                    }
                }
                composers.clear()
            }
        }

        override fun registerComposer(composer: Composer) {
            super.registerComposer(composer as ComposerImpl)
            composers.add(composer)
        }

        override fun unregisterComposer(composer: Composer) {
            inspectionTables?.forEach { it.remove((composer as ComposerImpl).compositionData) }
            composers.remove(composer)
        }

        override fun registerComposition(composition: ControlledComposition) {
            parentContext.registerComposition(composition)
        }

        override fun unregisterComposition(composition: ControlledComposition) {
            parentContext.unregisterComposition(composition)
        }

        override fun reportPausedScope(scope: RecomposeScopeImpl) {
            parentContext.reportPausedScope(scope)
        }

        override val effectCoroutineContext: CoroutineContext
            get() = parentContext.effectCoroutineContext

        @OptIn(ExperimentalComposeApi::class)
        override val recomposeCoroutineContext: CoroutineContext
            get() = this@ComposerImpl.composition.recomposeCoroutineContext

        override fun composeInitial(
            composition: ControlledComposition,
            content: @Composable () -> Unit,
        ) {
            parentContext.composeInitial(composition, content)
        }

        override fun composeInitialPaused(
            composition: ControlledComposition,
            shouldPause: ShouldPauseCallback,
            content: @Composable () -> Unit,
        ): ScatterSet<RecomposeScopeImpl> =
            parentContext.composeInitialPaused(composition, shouldPause, content)

        override fun recomposePaused(
            composition: ControlledComposition,
            shouldPause: ShouldPauseCallback,
            invalidScopes: ScatterSet<RecomposeScopeImpl>,
        ): ScatterSet<RecomposeScopeImpl> =
            parentContext.recomposePaused(composition, shouldPause, invalidScopes)

        override fun invalidate(composition: ControlledComposition) {
            // Invalidate ourselves with our parent before we invalidate a child composer.
            // This ensures that when we are scheduling recompositions, parents always
            // recompose before their children just in case a recomposition in the parent
            // would also cause other recomposition in the child.
            // If the parent ends up having no real invalidations to process we will skip work
            // for that composer along a fast path later.
            // This invalidation process could be made more efficient as it's currently N^2 with
            // subcomposition meta-tree depth thanks to the double recursive parent walk
            // performed here, but we currently assume a low N.
            parentContext.invalidate(this@ComposerImpl.composition)
            parentContext.invalidate(composition)
        }

        override fun invalidateScope(scope: RecomposeScopeImpl) {
            parentContext.invalidateScope(scope)
        }

        // This is snapshot state not because we need it to be observable, but because
        // we need changes made to it in composition to be visible for the rest of the current
        // composition and not become visible outside of the composition process until composition
        // succeeds.
        private var compositionLocalScope by
            mutableStateOf<PersistentCompositionLocalMap>(
                persistentCompositionLocalHashMapOf(),
                referentialEqualityPolicy(),
            )

        override fun getCompositionLocalScope(): PersistentCompositionLocalMap =
            compositionLocalScope

        fun updateCompositionLocalScope(scope: PersistentCompositionLocalMap) {
            compositionLocalScope = scope
        }

        override fun recordInspectionTable(table: MutableSet<CompositionData>) {
            (inspectionTables
                    ?: HashSet<MutableSet<CompositionData>>().also { inspectionTables = it })
                .add(table)
        }

        override fun startComposing() {
            childrenComposing++
        }

        override fun doneComposing() {
            childrenComposing--
        }

        override fun insertMovableContent(reference: MovableContentStateReference) {
            parentContext.insertMovableContent(reference)
        }

        override fun deletedMovableContent(reference: MovableContentStateReference) {
            parentContext.deletedMovableContent(reference)
        }

        override fun movableContentStateResolve(
            reference: MovableContentStateReference
        ): MovableContentState? = parentContext.movableContentStateResolve(reference)

        override fun movableContentStateReleased(
            reference: MovableContentStateReference,
            data: MovableContentState,
            applier: Applier<*>,
        ) {
            parentContext.movableContentStateReleased(reference, data, applier)
        }

        override fun reportRemovedComposition(composition: ControlledComposition) {
            parentContext.reportRemovedComposition(composition)
        }

        override val composition: Composition
            get() = this@ComposerImpl.composition

        override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
            return parentContext.scheduleFrameEndCallback(action)
        }
    }

    private inline fun updateCompositeKeyWhenWeEnterGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Empty)
                updateCompositeKeyWhenWeEnterGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeEnterGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.hashCode(), 0)
    }

    private inline fun updateCompositeKeyWhenWeEnterGroupKeyHash(groupKey: Int, rGroupIndex: Int) {
        compositeKeyHashCode =
            compositeKeyHashCode.compoundWith(groupKey, 3).compoundWith(rGroupIndex, 3)
    }

    private inline fun updateCompositeKeyWhenWeExitGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Empty)
                updateCompositeKeyWhenWeExitGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeExitGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.hashCode(), 0)
    }

    private inline fun updateCompositeKeyWhenWeExitGroupKeyHash(groupKey: Int, rGroupIndex: Int) {
        compositeKeyHashCode =
            compositeKeyHashCode.unCompoundWith(rGroupIndex, 3).unCompoundWith(groupKey, 3)
    }

    // This is only used in tests to ensure the stacks do not silently leak.
    internal fun stacksSize(): Int {
        return entersStack.size +
            invalidateStack.size +
            providersInvalidStack.size +
            pendingStack.size +
            parentStateStack.size
    }

    override val recomposeScope: RecomposeScope?
        get() = currentRecomposeScope

    override val recomposeScopeIdentity: Any?
        get() = currentRecomposeScope?.anchor

    override fun rememberedValue(): Any? = nextSlotForCache()

    override fun updateRememberedValue(value: Any?) = updateCachedValue(value)

    override fun recordUsed(scope: RecomposeScope) {
        (scope as? RecomposeScopeImpl)?.used = true
    }
}

/**
 * A helper receiver scope class used by [ComposeNode] to help write code to initialized and update
 * a node.
 *
 * @see ComposeNode
 */
@JvmInline
public value class Updater<T> constructor(@PublishedApi internal val composer: Composer) {
    /**
     * Set the value property of the emitted node.
     *
     * Schedules [block] to be run when the node is first created or when [value] is different than
     * the previous composition.
     *
     * @see update
     */
    @Suppress("NOTHING_TO_INLINE") // Inlining the compare has noticeable impact
    public inline fun set(value: Int, noinline block: T.(value: Int) -> Unit): Unit =
        with(composer) {
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                composer.apply(value, block)
            }
        }

    /**
     * Set the value property of the emitted node.
     *
     * Schedules [block] to be run when the node is first created or when [value] is different than
     * the previous composition.
     *
     * @see update
     */
    public fun <V> set(value: V, block: T.(value: V) -> Unit): Unit =
        with(composer) {
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                composer.apply(value, block)
            }
        }

    /**
     * Update the value of a property of the emitted node.
     *
     * Schedules [block] to be run when [value] is different than the previous composition. It is
     * different than [set] in that it does not run when the node is created. This is used when
     * initial value set by the [ComposeNode] in the constructor callback already has the correct
     * value. For example, use [update} when [value] is passed into of the classes constructor
     * parameters.
     *
     * @see set
     */
    @Suppress("NOTHING_TO_INLINE") // Inlining the compare has noticeable impact
    public inline fun update(value: Int, noinline block: T.(value: Int) -> Unit): Unit =
        with(composer) {
            val inserting = inserting
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                if (!inserting) apply(value, block)
            }
        }

    /**
     * Update the value of a property of the emitted node.
     *
     * Schedules [block] to be run when [value] is different than the previous composition. It is
     * different than [set] in that it does not run when the node is created. This is used when
     * initial value set by the [ComposeNode] in the constructor callback already has the correct
     * value. For example, use [update} when [value] is passed into of the classes constructor
     * parameters.
     *
     * @see set
     */
    public fun <V> update(value: V, block: T.(value: V) -> Unit): Unit =
        with(composer) {
            val inserting = inserting
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                if (!inserting) apply(value, block)
            }
        }

    /**
     * Initialize emitted node.
     *
     * Schedule [block] to be executed after the node is created.
     *
     * This is only executed once. The can be used to call a method or set a value on a node
     * instance that is required to be set after one or more other properties have been set.
     *
     * @see reconcile
     */
    public fun init(block: T.() -> Unit) {
        if (composer.inserting) composer.apply<Unit, T>(Unit) { block() }
    }

    /**
     * Reconcile the node to the current state.
     *
     * This is used when [set] and [update] are insufficient to update the state of the node based
     * on changes passed to the function calling [ComposeNode].
     *
     * Schedules [block] to execute. As this unconditionally schedules [block] to executed it might
     * be executed unnecessarily as no effort is taken to ensure it only executes when the values
     * [block] captures have changed. It is highly recommended that [set] and [update] be used
     * instead as they will only schedule their blocks to executed when the value passed to them has
     * changed.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun reconcile(block: T.() -> Unit) {
        composer.apply<Unit, T>(Unit) { this.block() }
    }
}

@JvmInline
public value class SkippableUpdater<T> constructor(@PublishedApi internal val composer: Composer) {
    public inline fun update(block: Updater<T>.() -> Unit) {
        composer.startReplaceableGroup(0x1e65194f)
        Updater<T>(composer).block()
        composer.endReplaceableGroup()
    }
}

internal fun SlotWriter.removeCurrentGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.

    forAllDataInRememberOrder(currentGroup) { _, slot ->
        // even that in the documentation we claim ComposeNodeLifecycleCallback should be only
        // implemented on the nodes we do not really enforce it here as doing so will be expensive.
        if (slot is ComposeNodeLifecycleCallback) {
            rememberManager.releasing(slot)
        }
        if (slot is RememberObserverHolder) {
            rememberManager.forgetting(slot)
        }
        if (slot is RecomposeScopeImpl) {
            slot.release()
        }
    }

    removeGroup()
}

internal inline fun <R> SlotWriter.withAfterAnchorInfo(anchor: Anchor?, cb: (Int, Int) -> R) {
    var priority = -1
    var endRelativeAfter = -1
    if (anchor != null && anchor.valid) {
        priority = anchorIndex(anchor)
        endRelativeAfter = slotsSize - slotsEndAllIndex(priority)
    }
    cb(priority, endRelativeAfter)
}

internal val SlotWriter.isAfterFirstChild
    get() = currentGroup > parent + 1
internal val SlotReader.isAfterFirstChild
    get() = currentGroup > parent + 1

internal fun SlotWriter.deactivateCurrentGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.
    forAllDataInRememberOrder(currentGroup) { slotIndex, data ->
        when (data) {
            is ComposeNodeLifecycleCallback -> {
                rememberManager.deactivating(data)
            }
            is ReusableRememberObserverHolder -> {
                // do nothing, the value should be preserved on reuse
            }
            is RememberObserverHolder -> {
                removeData(slotIndex, data)
                rememberManager.forgetting(data)
            }
            is RecomposeScopeImpl -> {
                removeData(slotIndex, data)
                data.release()
            }
        }
    }
}

private fun SlotWriter.removeData(index: Int, data: Any?) {
    val result = clear(index)
    runtimeCheck(data === result) { "Slot table is out of sync (expected $data, got $result)" }
}

private fun <K : Any, V : Any> multiMap(initialCapacity: Int) =
    MultiValueMap<K, V>(MutableScatterMap(initialCapacity))

private fun getKey(value: Any?, left: Any?, right: Any?): Any? =
    (value as? JoinedKey)?.let {
        if (it.left == left && it.right == right) value
        else getKey(it.left, left, right) ?: getKey(it.right, left, right)
    }

// Invalidation helpers
private fun List<Invalidation>.findLocation(location: Int): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = midVal.location.compareTo(location)

        when {
            cmp < 0 -> low = mid + 1
            cmp > 0 -> high = mid - 1
            else -> return mid // key found
        }
    }
    return -(low + 1) // key not found
}

private fun List<Invalidation>.findInsertLocation(location: Int): Int =
    findLocation(location).let { if (it < 0) -(it + 1) else it }

private fun MutableList<Invalidation>.insertIfMissing(
    location: Int,
    scope: RecomposeScopeImpl,
    instance: Any?,
) {
    val index = findLocation(location)
    if (index < 0) {
        add(
            -(index + 1),
            Invalidation(
                scope,
                location,
                // Only derived state instance is important for composition
                instance.takeIf { it is DerivedState<*> },
            ),
        )
    } else {
        val invalidation = get(index)
        // Only derived state instance is important for composition
        if (instance is DerivedState<*>) {
            when (val oldInstance = invalidation.instances) {
                null -> {
                    invalidation.instances = instance
                }
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    oldInstance as MutableScatterSet<Any?>
                    oldInstance.add(instance)
                }
                else -> {
                    invalidation.instances = mutableScatterSetOf(oldInstance, instance)
                }
            }
        } else {
            invalidation.instances = null
        }
    }
}

private fun MutableList<Invalidation>.firstInRange(start: Int, end: Int): Invalidation? {
    val index = findInsertLocation(start)
    if (index < size) {
        val firstInvalidation = get(index)
        if (firstInvalidation.location < end) return firstInvalidation
    }
    return null
}

private fun MutableList<Invalidation>.removeLocation(location: Int): Invalidation? {
    val index = findLocation(location)
    return if (index >= 0) removeAt(index) else null
}

private fun MutableList<Invalidation>.removeRange(start: Int, end: Int) {
    val index = findInsertLocation(start)
    while (index < size) {
        val validation = get(index)
        if (validation.location < end) removeAt(index) else break
    }
}

private inline fun List<Invalidation>.forEachInRange(
    start: Int,
    end: Int,
    block: (Invalidation) -> Unit,
) {
    var index = findInsertLocation(start)
    while (index < size) {
        val invalidation = get(index)
        if (invalidation.location >= end) break

        block(invalidation)
        index++
    }
}

private fun Boolean.asInt() = if (this) 1 else 0

private fun Int.asBool() = this != 0

private fun SlotTable.collectNodesFrom(anchor: Anchor): List<Any?> {
    val result = mutableListOf<Any?>()
    read { reader ->
        val index = anchorIndex(anchor)
        fun collectFromGroup(group: Int) {
            if (reader.isNode(group)) {
                result.add(reader.node(group))
            } else {
                var current = group + 1
                val end = group + reader.groupSize(group)
                while (current < end) {
                    collectFromGroup(current)
                    current += reader.groupSize(current)
                }
            }
        }
        collectFromGroup(index)
    }
    return result
}

private fun SlotReader.distanceFrom(index: Int, root: Int): Int {
    var count = 0
    var current = index
    while (current > 0 && current != root) {
        current = parent(current)
        count++
    }
    return count
}

// find the nearest common root
private fun SlotReader.nearestCommonRootOf(a: Int, b: Int, common: Int): Int {
    // Early outs, to avoid calling distanceFrom in trivial cases
    if (a == b) return a // A group is the nearest common root of itself
    if (a == common || b == common) return common // If either is common then common is nearest
    if (parent(a) == b) return b // if b is a's parent b is the nearest common root
    if (parent(b) == a) return a // if a is b's parent a is the nearest common root
    if (parent(a) == parent(b)) return parent(a) // if a an b share a parent it is common

    // Find the nearest using distance from common
    var currentA = a
    var currentB = b
    val aDistance = distanceFrom(a, common)
    val bDistance = distanceFrom(b, common)
    repeat(aDistance - bDistance) { currentA = parent(currentA) }
    repeat(bDistance - aDistance) { currentB = parent(currentB) }

    // Both ca and cb are now the same distance from a known common root,
    // therefore, the first parent that is the same is the lowest common root.
    while (currentA != currentB) {
        currentA = parent(currentA)
        currentB = parent(currentB)
    }

    // ca == cb so it doesn't matter which is returned
    return currentA
}

private val KeyInfo.joinedKey: Any
    get() = if (objectKey != null) JoinedKey(key, objectKey) else key

/*
 * Group types used with [Composer.start] to differentiate between different types of groups
 */
@JvmInline
private value class GroupKind private constructor(val value: Int) {
    inline val isNode
        get() = value != Group.value

    inline val isReusable
        get() = value != Node.value

    companion object {
        val Group = GroupKind(0)
        val Node = GroupKind(1)
        val ReusableNode = GroupKind(2)
    }
}

/*
 * Remember observer which is not removed during reuse/deactivate of the group.
 * It is used to preserve composition locals between group deactivation.
 */
internal class ReusableRememberObserverHolder(wrapped: RememberObserver, after: Anchor?) :
    RememberObserverHolder(wrapped, after)

internal open class RememberObserverHolder(var wrapped: RememberObserver, var after: Anchor?)

/*
 * Integer keys are arbitrary values in the biload range. The do not need to be unique as if
 * there is a chance they will collide with a compiler generated key they are paired with a
 * OpaqueKey to ensure they are unique.
 */

// rootKey doesn't need a corresponding OpaqueKey as it never has sibling nodes and will always
// a unique key.
private const val rootKey = 100

// An arbitrary key value for a node.
private const val nodeKey = 125

// An arbitrary key value that marks the default parameter group
internal const val defaultsKey = -127

@PublishedApi internal const val invocationKey: Int = 200

@PublishedApi internal val invocation: Any = OpaqueKey("provider")

@PublishedApi internal const val providerKey: Int = 201

@PublishedApi internal val provider: Any = OpaqueKey("provider")

@PublishedApi internal const val compositionLocalMapKey: Int = 202

@PublishedApi internal val compositionLocalMap: Any = OpaqueKey("compositionLocalMap")

@PublishedApi internal const val providerValuesKey: Int = 203

@PublishedApi internal val providerValues: Any = OpaqueKey("providerValues")

@PublishedApi internal const val providerMapsKey: Int = 204

@PublishedApi internal val providerMaps: Any = OpaqueKey("providers")

@PublishedApi internal const val referenceKey: Int = 206

@PublishedApi internal val reference: Any = OpaqueKey("reference")

@PublishedApi internal const val reuseKey: Int = 207

private const val invalidGroupLocation = -2

internal class ComposeRuntimeError(override val message: String) : IllegalStateException()

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun runtimeCheck(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        composeImmediateRuntimeError(lazyMessage())
    }
}

internal const val EnableDebugRuntimeChecks = false

/**
 * A variation of [composeRuntimeError] that gets stripped from R8-minified builds. Use this for
 * more expensive checks or assertions along a hotpath that, if failed, would still lead to an
 * application crash that could be traced back to this assertion if removed from the final program
 * binary.
 */
internal inline fun debugRuntimeCheck(value: Boolean, lazyMessage: () -> String) {
    if (EnableDebugRuntimeChecks && !value) {
        composeImmediateRuntimeError(lazyMessage())
    }
}

internal inline fun debugRuntimeCheck(value: Boolean) = debugRuntimeCheck(value) { "Check failed" }

internal inline fun runtimeCheck(value: Boolean) = runtimeCheck(value) { "Check failed" }

internal fun composeRuntimeError(message: String): Nothing {
    throw ComposeRuntimeError(
        "Compose Runtime internal error. Unexpected or incorrect use of the Compose " +
            "internal runtime API ($message). Please report to Google or use " +
            "https://goo.gle/compose-feedback"
    )
}

// Unit variant of composeRuntimeError() so the call site doesn't add 3 extra
// instructions to throw a KotlinNothingValueException
internal fun composeImmediateRuntimeError(message: String) {
    throw ComposeRuntimeError(
        "Compose Runtime internal error. Unexpected or incorrect use of the Compose " +
            "internal runtime API ($message). Please report to Google or use " +
            "https://goo.gle/compose-feedback"
    )
}

private val InvalidationLocationAscending =
    Comparator<Invalidation> { i1, i2 -> i1.location.compareTo(i2.location) }

/**
 * Extract the state of movable content from the given writer. A new slot table is created and the
 * content is removed from [slots] (leaving a movable content group that, if composed over, will
 * create new content) and added to this new slot table. The invalidations that occur to recompose
 * scopes in the movable content state will be collected and forwarded to the new composition if the
 * state is used.
 */
internal fun extractMovableContentAtCurrent(
    composition: ControlledComposition,
    reference: MovableContentStateReference,
    slots: SlotWriter,
    applier: Applier<*>?,
): MovableContentState {
    val slotTable = SlotTable()
    if (slots.collectingSourceInformation) {
        slotTable.collectSourceInformation()
    }
    if (slots.collectingCalledInformation) {
        slotTable.collectCalledByInformation()
    }

    // If an applier is provided then we are extracting a state from the middle of an
    // already extracted state. If the group has nodes then the nodes need to be removed
    // from their parent so they can potentially be inserted into a destination.
    val currentGroup = slots.currentGroup
    if (applier != null && slots.nodeCount(currentGroup) > 0) {
        @Suppress("UNCHECKED_CAST")
        applier as Applier<Any?>

        // Find the parent node by going up until the first node group
        var parentNodeGroup = slots.parent
        while (parentNodeGroup > 0 && !slots.isNode(parentNodeGroup)) {
            parentNodeGroup = slots.parent(parentNodeGroup)
        }

        // If we don't find a node group the nodes in the state have already been removed
        // as they are the nodes that were removed when the state was removed from the original
        // table.
        if (parentNodeGroup >= 0 && slots.isNode(parentNodeGroup)) {
            val node = slots.node(parentNodeGroup)
            var currentChild = parentNodeGroup + 1
            val end = parentNodeGroup + slots.groupSize(parentNodeGroup)

            // Find the node index
            var nodeIndex = 0
            while (currentChild < end) {
                val size = slots.groupSize(currentChild)
                if (currentChild + size > currentGroup) {
                    break
                }
                nodeIndex += if (slots.isNode(currentChild)) 1 else slots.nodeCount(currentChild)
                currentChild += size
            }

            // Remove the nodes
            val count = if (slots.isNode(currentGroup)) 1 else slots.nodeCount(currentGroup)
            applier.down(node)
            applier.remove(nodeIndex, count)
            applier.up()
        }
    }

    // Write a table that as if it was written by a calling invokeMovableContentLambda because this
    // might be removed from the composition before the new composition can be composed to receive
    // it. When the new composition receives the state it must recompose over the state by calling
    // invokeMovableContentLambda.
    val anchors =
        slotTable.write { writer ->
            writer.beginInsert()

            // This is the prefix created by invokeMovableContentLambda
            writer.startGroup(movableContentKey, reference.content)
            writer.markGroup()
            writer.update(reference.parameter)

            // Move the content into current location
            val anchors = slots.moveTo(reference.anchor, 1, writer)

            // skip the group that was just inserted.
            writer.skipGroup()

            // End the group that represents the call to invokeMovableContentLambda
            writer.endGroup()

            writer.endInsert()

            anchors
        }

    val state = MovableContentState(slotTable)
    if (RecomposeScopeImpl.hasAnchoredRecomposeScopes(slotTable, anchors)) {
        // If any recompose scopes are invalidated while the movable content is outside a
        // composition, ensure the reference is updated to contain the invalidation.
        val movableContentRecomposeScopeOwner =
            object : RecomposeScopeOwner {
                override fun invalidate(
                    scope: RecomposeScopeImpl,
                    instance: Any?,
                ): InvalidationResult {
                    // Try sending this to the original owner first.
                    val result =
                        (composition as? RecomposeScopeOwner)?.invalidate(scope, instance)
                            ?: InvalidationResult.IGNORED

                    // If the original owner ignores this then we need to record it in the
                    // reference
                    if (result == InvalidationResult.IGNORED) {
                        reference.invalidations += scope to instance
                        return InvalidationResult.SCHEDULED
                    }
                    return result
                }

                // The only reason [recomposeScopeReleased] is called is when the recompose scope is
                // removed from the table. First, this never happens for content that is moving, and
                // 2) even if it did the only reason we tell the composer is to clear tracking
                // tables that contain this information which is not relevant here.
                override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
                    // Nothing to do
                }

                // [recordReadOf] this is also something that would happen only during active
                // recomposition which doesn't happened to a slot table that is moving.
                override fun recordReadOf(value: Any) {
                    // Nothing to do
                }
            }
        slotTable.write { writer ->
            RecomposeScopeImpl.adoptAnchoredScopes(
                slots = writer,
                anchors = anchors,
                newOwner = movableContentRecomposeScopeOwner,
            )
        }
    }
    return state
}

internal class CompositionDataImpl(val composition: Composition) :
    CompositionData, CompositionInstance {
    private val slotTable
        get() = (composition as CompositionImpl).slotTable

    override val compositionGroups: Iterable<CompositionGroup>
        get() = slotTable.compositionGroups

    override val isEmpty: Boolean
        get() = slotTable.isEmpty

    override fun find(identityToFind: Any): CompositionGroup? = slotTable.find(identityToFind)

    override fun hashCode(): Int = composition.hashCode() * 31

    override fun equals(other: Any?): Boolean =
        other is CompositionDataImpl && composition == other.composition

    override val parent: CompositionInstance?
        get() = composition.parent?.let { CompositionDataImpl(it) }

    override val data: CompositionData
        get() = this

    override fun findContextGroup(): CompositionGroup? {
        val parentSlotTable = composition.parent?.slotTable ?: return null
        val context = composition.context ?: return null

        return parentSlotTable.findSubcompositionContextGroup(context)?.let {
            parentSlotTable.compositionGroupOf(it)
        }
    }

    private val Composition.slotTable
        get() = (this as? CompositionImpl)?.slotTable

    private val Composition.context
        get() = (this as? CompositionImpl)?.parent

    private val Composition.parent
        get() = context?.composition
}
