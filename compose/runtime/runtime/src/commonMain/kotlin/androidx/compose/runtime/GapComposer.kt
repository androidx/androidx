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

package androidx.compose.runtime

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.collection.MultiValueMap
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.Stack
import androidx.compose.runtime.composer.GroupInfo
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.GapAnchor
import androidx.compose.runtime.composer.gapbuffer.KeyInfo
import androidx.compose.runtime.composer.gapbuffer.SlotReader
import androidx.compose.runtime.composer.gapbuffer.SlotTable
import androidx.compose.runtime.composer.gapbuffer.SlotWriter
import androidx.compose.runtime.composer.gapbuffer.asGapAnchor
import androidx.compose.runtime.composer.gapbuffer.asGapBufferSlotTable
import androidx.compose.runtime.composer.gapbuffer.buildTrace
import androidx.compose.runtime.composer.gapbuffer.changelist.ChangeList
import androidx.compose.runtime.composer.gapbuffer.changelist.FixupList
import androidx.compose.runtime.composer.gapbuffer.changelist.GapComposerChangeListWriter
import androidx.compose.runtime.composer.gapbuffer.changelist.asGapBufferChangeList
import androidx.compose.runtime.composer.gapbuffer.compositionGroupOf
import androidx.compose.runtime.composer.gapbuffer.findLocation
import androidx.compose.runtime.composer.gapbuffer.findSubcompositionContextGroup
import androidx.compose.runtime.composer.gapbuffer.isAfterFirstChild
import androidx.compose.runtime.composer.gapbuffer.joinedKey
import androidx.compose.runtime.composer.gapbuffer.traceForGroup
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.invokeComposable
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastToSet
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.runtime.tooling.LocalCompositionErrorContext
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.runtime.tooling.attachComposeStackTrace
import kotlin.collections.plus
import kotlin.collections.remove
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(InternalComposeApi::class, ExperimentalComposeRuntimeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal class GapComposer(
    applier: Applier<*>,
    parentContext: CompositionContext,
    abandonSet: MutableSet<RememberObserver>,
    private val slotTable: SlotTable,
    private var changes: Changes,
    private var lateChanges: Changes,
    observerHolder: CompositionObserverHolder,
    composition: CompositionImpl,
) :
    ComposerCommon(
        applier = applier,
        parentContext = parentContext,
        abandonSet = abandonSet,
        observerHolder = observerHolder,
        composition = composition,
    ) {
    // Needs to be first as it is used to initialize other fields
    internal var insertTable =
        SlotTable().apply {
            if (parentContext.collectingSourceInformation) collectSourceInformation()
            if (parentContext.collectingCallByInformation) collectCalledByInformation()
        }

    // Private fields
    private var _compositionData: CompositionData? = null
    private val changeListWriter =
        GapComposerChangeListWriter(this, changes.asGapBufferChangeList())
    private var insertAnchor: GapAnchor = insertTable.read { it.anchor(0) }
    private var insertFixups = FixupList()
    private val invalidations: MutableList<Invalidation> = mutableListOf()
    private var pending: PendingChange? = null
    private val pendingStack = Stack<PendingChange?>()
    private var writer: SlotWriter = insertTable.openWriter().also { it.close(true) }
    private var writerHasAProvider = false

    // Internal fields
    internal var reader: SlotReader = slotTable.openReader().also { it.close() }

    // Overrides
    override fun <V, T> apply(value: V, block: T.(V) -> Unit) {
        if (inserting) {
            insertFixups.updateNode(value, block)
        } else {
            changeListWriter.updateNode(value, block)
        }
    }

    override val applyCoroutineContext: CoroutineContext =
        parentContext.effectCoroutineContext + (errorContext ?: EmptyCoroutineContext)

    override fun buildContext(): CompositionContext {
        start(referenceKey, reference, GroupKind.Group, null)
        if (inserting) writer.markGroup()

        var observerHolder = nextSlot() as? RememberObserverHolder
        if (observerHolder == null) {
            observerHolder =
                ReusableRememberObserverHolder(
                    CompositionContextHolder(
                        CompositionContextImpl(
                            compositeKeyHashCode,
                            forceRecomposeScopes,
                            sourceMarkersEnabled,
                            composition.observerHolder,
                        )
                    ),
                    after = null,
                )
            updateValue(observerHolder)
        }
        val holder = observerHolder.wrapped as CompositionContextHolder
        holder.ref.updateCompositionLocalScope(currentCompositionLocalScope())
        end(isNode = false)

        return holder.ref
    }

    override fun changed(value: Any?): Boolean {
        return if (nextSlot() != value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    override fun changedInstance(value: Any?): Boolean {
        return if (nextSlot() !== value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    override fun changed(value: Char): Boolean {
        val next = nextSlot()
        if (next is Char) {
            val nextPrimitive: Char = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Byte): Boolean {
        val next = nextSlot()
        if (next is Byte) {
            val nextPrimitive: Byte = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Short): Boolean {
        val next = nextSlot()
        if (next is Short) {
            val nextPrimitive: Short = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Boolean): Boolean {
        val next = nextSlot()
        if (next is Boolean) {
            val nextPrimitive: Boolean = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Float): Boolean {
        val next = nextSlot()
        if (next is Float) {
            val nextPrimitive: Float = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Long): Boolean {
        val next = nextSlot()
        if (next is Long) {
            val nextPrimitive: Long = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Double): Boolean {
        val next = nextSlot()
        if (next is Double) {
            val nextPrimitive: Double = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun changed(value: Int): Boolean {
        val next = nextSlot()
        if (next is Int) {
            val nextPrimitive: Int = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    override fun collectParameterInformation() {
        forceRecomposeScopes = true
        sourceMarkersEnabled = true
        slotTable.collectSourceInformation()
        insertTable.collectSourceInformation()
        writer.updateToTableMaps()
    }

    override fun composeContent(
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

    override val compositionData: CompositionData
        get() {
            val data = _compositionData
            if (data == null) {
                val newData = GapCompositionDataImpl(composition)
                _compositionData = newData
                return newData
            }
            return data
        }

    override fun <T> consume(key: CompositionLocal<T>): T = currentCompositionLocalScope().read(key)

    override fun <T> createNode(factory: () -> T) {
        validateNodeExpected()
        runtimeCheck(inserting) { "createNode() can only be called when inserting" }
        val insertIndex = parentStateStack.peek()
        val groupAnchor = writer.anchor(writer.parent)
        groupNodeCount++
        insertFixups.createAndInsertNode(factory, insertIndex, groupAnchor)
    }

    override val currentCompositionLocalMap: CompositionLocalMap
        get() = currentCompositionLocalScope()

    override val currentMarker: Int
        get() = if (inserting) -writer.parent else reader.parent

    override fun deactivate() {
        invalidateStack.clear()
        invalidations.clear()
        changes.clear()
        providerUpdates = null
    }

    override fun deactivateToEndGroup(changed: Boolean) {
        runtimeCheck(groupNodeCount == 0) {
            "No nodes can be emitted before calling deactivateToEndGroup"
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

    override fun dispose() {
        trace("Compose:Composer.dispose") {
            parentContext.unregisterComposer(this)
            deactivate()
            applier.clear()
            isDisposed = true
        }
    }

    override fun endDefaults() {
        end(isNode = false)
        val scope = currentRecomposeScope
        if (scope != null && scope.used) {
            scope.defaultsInScope = true
        }
    }

    override fun endMovableGroup() = end(isNode = false)

    override fun endNode() = end(isNode = true)

    override fun endProvider() {
        end(isNode = false)
        end(isNode = false)
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    override fun startProvider(value: ProvidedValue<*>) {
        val parentScope = currentCompositionLocalScope()
        start(providerKey, provider, GroupKind.Group, null)
        @Suppress("UNCHECKED_CAST")
        val oldState =
            rememberedValue().let { if (it == Composer.Empty) null else it as ValueHolder<Any?> }
        val local = value.compositionLocal as CompositionLocal<Any?>
        @Suppress("UNCHECKED_CAST")
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

    override fun endProviders() {
        end(isNode = false)
        end(isNode = false)
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    override fun endReplaceableGroup() = end(isNode = false)

    override fun endReplaceGroup() = end(isNode = false)

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

    override fun endReusableGroup() {
        if (reusing && reader.parent == reusingGroup) {
            reusingGroup = -1
            reusing = false
        }
        end(isNode = false)
    }

    override fun endReuseFromRoot() {
        requirePrecondition(!isComposing && reusingGroup == rootKey) {
            "Cannot disable reuse from root if it was caused by other groups"
        }
        reusingGroup = -1
        reusing = false
    }

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

    override fun getInsertStorage(): SlotStorage = insertTable

    override val hasPendingChanges: Boolean
        get() = changes.isNotEmpty()

    override fun insertMovableContent(value: MovableContent<*>, parameter: Any?) {
        @Suppress("UNCHECKED_CAST")
        invokeMovableContentLambda(
            value as MovableContent<Any?>,
            currentCompositionLocalScope(),
            parameter,
            force = false,
        )
    }

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

    override fun joinKey(left: Any?, right: Any?): Any =
        getKey(reader.groupObjectKey, left, right) ?: JoinedKey(left, right)

    @TestOnly
    override fun parentKey(): Int {
        return if (inserting) {
            writer.groupKey(writer.parent)
        } else {
            reader.groupKey(reader.parent)
        }
    }

    override fun recompose(
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

    override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
        return parentContext.scheduleFrameEndCallback(action)
    }

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

    override fun sourceInformation(sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGroupSourceInformation(sourceInformation)
        }
    }

    override fun sourceInformationMarkerEnd() {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGrouplessCallSourceInformationEnd()
        }
    }

    override fun sourceInformationMarkerStart(key: Int, sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            writer.recordGrouplessCallSourceInformationStart(key, sourceInformation)
        }
    }

    override fun stacksSize(): Int {
        return entersStack.size +
            invalidateStack.size +
            providersInvalidStack.size +
            pendingStack.size +
            parentStateStack.size
    }

    override fun startDefaults() = start(defaultsKey, null, GroupKind.Group, null)

    override fun startMovableGroup(key: Int, dataKey: Any?) =
        start(key, dataKey, GroupKind.Group, null)

    override fun startNode() {
        start(nodeKey, null, GroupKind.Node, null)
        nodeExpected = true
    }

    override fun startProviders(values: Array<out ProvidedValue<*>>) {
        val parentScope = currentCompositionLocalScope()
        start(providerKey, provider, GroupKind.Group, null)
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

    override fun startReplaceableGroup(key: Int) = start(key, null, GroupKind.Group, null)

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

    override fun startReuseFromRoot() {
        reusingGroup = rootKey
        reusing = true
    }

    override fun startReusableNode() {
        start(nodeKey, null, GroupKind.ReusableNode, null)
        nodeExpected = true
    }

    override fun startRestartGroup(key: Int): Composer {
        startReplaceGroup(key)
        addRecomposeScope()
        return this
    }

    override fun tryImminentInvalidation(scope: RecomposeScopeImpl, instance: Any?): Boolean {
        val anchor = scope.anchor ?: return false
        val slotTable = reader.table
        val location = anchor.asGapAnchor().toIndexFor(slotTable)
        if (isComposing && location >= reader.currentGroup) {
            // if we are invalidating a scope that is going to be traversed during this
            // composition.
            invalidations.insertIfMissing(location, scope, instance)
            return true
        }
        return false
    }

    override fun recordSideEffect(effect: () -> Unit) {
        changeListWriter.sideEffect(effect)
    }

    override fun rememberedValue(): Any? = nextSlotForCache()

    override fun updateRememberedValue(value: Any?) = updateCachedValue(value)

    override fun updateComposerInvalidations(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>
    ) {
        // Update any invalidations that have may have moved since they were added, removing any
        // that are no longer in the slot table.
        for (i in invalidations.lastIndex downTo 0) {
            val invalidation = invalidations[i]
            val anchor = invalidation.scope.anchor?.asGapAnchor()
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
            val location = scope.anchor?.asGapAnchor()?.location ?: return@forEach
            invalidations.add(
                Invalidation(scope, location, instances.takeUnless { it === ScopeInvalidated })
            )
        }

        // Ensure the invalidations are in sorted order.
        invalidations.sortWith(InvalidationLocationAscending)
    }

    override fun useNode() {
        validateNodeExpected()
        runtimeCheck(!inserting) { "useNode() called while inserting" }
        val node = reader.node
        changeListWriter.moveDown(node)

        if (reusing && node is ComposeNodeLifecycleCallback) {
            changeListWriter.useNode(node)
        }
    }

    override fun verifyConsistent() {
        insertTable.verifyWellFormedNoScopes()
    }

    // Internal methods
    internal fun nextSlot(): Any? =
        if (inserting) {
            validateNodeNotExpected()
            Composer.Empty
        } else
            reader.next().let {
                if (reusing && it !is ReusableRememberObserverHolder) Composer.Empty else it
            }

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

    override fun parentStackTrace(): List<ComposeStackTraceFrame> {
        val parentComposition = parentContext.composition as? CompositionImpl ?: return emptyList()
        val position =
            parentComposition.slotStorage
                .asGapBufferSlotTable()
                .findSubcompositionContextGroup(parentContext)

        return if (position != null) {
            parentComposition.slotStorage.asGapBufferSlotTable().read { reader ->
                reader.traceForGroup(position, 0)
            } + parentComposition.composer.parentStackTrace()
        } else {
            emptyList()
        }
    }

    override fun stackTraceForValue(value: Any?): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        return slotTable
            .findLocation { it === value || (it as? RememberObserverHolder)?.wrapped === value }
            ?.let { (groupIndex, dataIndex) ->
                stackTraceForGroup(groupIndex, dataIndex) + parentStackTrace()
            } ?: emptyList()
    }

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

    // Private methods
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

    private fun addRecomposeScope() {
        if (inserting) {
            val scope = RecomposeScopeImpl(composition)
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
                    val newScope = RecomposeScopeImpl(composition)
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

    private fun clearUpdatedNodeCounts() {
        nodeCountOverrides = null
        nodeCountVirtualOverrides = null
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

    private fun createFreshInsertTable() {
        runtimeCheck(writer.closed)
        forceFreshInsertTable()
    }

    private fun currentCompositionLocalScope(): PersistentCompositionLocalMap {
        providerCache?.let {
            return it
        }
        return currentCompositionLocalScope(reader.parent)
    }

    private fun currentStackTrace(): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        val trace = mutableListOf<ComposeStackTraceFrame>()
        trace.addAll(writer.buildTrace())
        trace.addAll(reader.buildTrace())

        return trace.apply { addAll(parentStackTrace()) }
    }

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
                        start(invocationKey, invocation, GroupKind.Group, null)
                        invokeComposable(this, content)
                        end(isNode = false)
                    } else if (
                        (forciblyRecompose || providersInvalid) &&
                            savedContent != null &&
                            savedContent != Composer.Empty
                    ) {
                        start(invocationKey, invocation, GroupKind.Group, null)
                        @Suppress("UNCHECKED_CAST")
                        invokeComposable(this, savedContent as @Composable () -> Unit)
                        end(isNode = false)
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

    private fun doRecordDownsFor(group: Int, nearestCommonRoot: Int) {
        if (group > 0 && group != nearestCommonRoot) {
            doRecordDownsFor(reader.parent(group), nearestCommonRoot)
            if (reader.isNode(group)) changeListWriter.moveDown(reader.nodeAt(group))
        }
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
        if (pending != null && pending.keyInfos.isNotEmpty()) {
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
            if (previous.isNotEmpty()) {
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

    private fun endRoot() {
        end(isNode = false)
        parentContext.doneComposing()
        end(isNode = false)
        changeListWriter.endRoot()
        finalizeCompose()
        reader.close()
        forciblyRecompose = false
        providersInvalid = providersInvalidStack.pop().asBool()
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

    private fun enterGroup(isNode: Boolean, newPending: PendingChange?) {
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

    private fun finalizeCompose() {
        changeListWriter.finalizeComposition()
        runtimeCheck(pendingStack.isEmpty()) { "Start/end imbalance" }
        cleanUpCompose()
    }

    private fun forceFreshInsertTable() {
        insertTable =
            SlotTable().apply {
                if (sourceMarkersEnabled) collectSourceInformation()
                if (parentContext.collectingCallByInformation) collectCalledByInformation()
            }
        writer = insertTable.openWriter().also { it.close(true) }
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

    private inline fun insertedGroupVirtualIndex(index: Int) = -2 - index

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
            end(isNode = false)
            providerCache = null
            compositeKeyHashCode = savedCompositeKeyHash
            endMovableGroup()
        }
    }

    private fun insertMovableContentGuarded(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        changeListWriter.withChangeList(lateChanges.asGapBufferChangeList()) {
            changeListWriter.resetSlots()
            references.fastForEach { (to, from) ->
                val anchor = to.anchor.asGapAnchor()
                val toSlotTable = to.slotStorage.asGapBufferSlotTable()
                val location = toSlotTable.anchorIndex(anchor.asGapAnchor())
                val effectiveNodeIndex = IntRef()
                // Insert content at the anchor point
                changeListWriter.determineMovableContentNodeIndex(effectiveNodeIndex, anchor)
                if (from == null) {
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
                    toSlotTable.read { reader ->
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
                    val fromTable =
                        resolvedState?.slotStorage?.asGapBufferSlotTable()
                            ?: from.slotStorage.asGapBufferSlotTable()
                    val fromAnchor =
                        resolvedState?.slotStorage?.asGapBufferSlotTable()?.anchor(0)
                            ?: from.anchor.asGapAnchor()
                    val nodesToInsert = fromTable.collectNodesFrom(fromAnchor)

                    // Insert nodes if necessary
                    if (nodesToInsert.isNotEmpty()) {
                        changeListWriter.copyNodesToNewAnchorLocation(
                            nodesToInsert,
                            effectiveNodeIndex,
                        )
                        if (to.slotStorage == slotTable) {
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

    private val SlotReader.node
        get() = node(parent)

    private fun SlotReader.nodeAt(index: Int) = node(index)

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

    private fun rememberObserverAnchor(): GapAnchor? =
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

    private fun skipGroup() {
        groupNodeCount += reader.skipGroup()
    }

    private fun skipReaderToGroupEnd() {
        groupNodeCount = reader.parentNodes
        reader.skipToGroupEnd()
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
                pending = PendingChange(reader.extractKeys(), nodeIndex)
            }
        }

        val pending = pending
        var newPending: PendingChange? = null
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
                newPending = PendingChange(mutableListOf(), if (isNode) 0 else nodeIndex)
            }
        }

        enterGroup(isNode, newPending)
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

    private fun startRoot() {
        rGroupIndex = 0
        reader = slotTable.openReader()
        start(rootKey, null, GroupKind.Group, null)

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

        start(parentContext.compositeKeyHashCode.hashCode(), null, GroupKind.Group, null)
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
        val recomposeCompositeKey = compositeKeyHashCode
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
                compositeKeyHashCode = compositeKeyOf(newParent, parent, recomposeCompositeKey)

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
        compositeKeyHashCode = recomposeCompositeKey

        isComposing = wasComposing
    }

    private fun recordDelete() {
        // It is import that the movable content is reported first so it can be removed before the
        // group itself is removed.
        reportFreeMovableContent(reader.currentGroup)
        changeListWriter.removeCurrentGroup()
    }

    private fun recordInsert(anchor: GapAnchor) {
        if (insertFixups.isEmpty()) {
            changeListWriter.insertSlots(anchor, insertTable)
        } else {
            changeListWriter.insertSlots(anchor, insertTable, insertFixups)
            insertFixups = FixupList()
        }
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

    private fun stackTraceForGroup(group: Int, dataOffset: Int?): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        return slotTable.read { it.traceForGroup(group, dataOffset) }
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
        start(providerMapsKey, providerMaps, GroupKind.Group, null)
        updateSlot(providerScope)
        updateSlot(currentProviders)
        end(isNode = false)
        return providerScope
    }

    private fun updatedNodeCount(group: Int): Int {
        if (group < 0)
            return nodeCountVirtualOverrides?.let { if (it.contains(group)) it[group] else 0 } ?: 0
        val nodeCounts = nodeCountOverrides
        if (nodeCounts != null) {
            val override = nodeCounts.getOrDefault(group, -1)
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
                            val newCounts = MutableIntIntMap()
                            nodeCountOverrides = newCounts
                            newCounts
                        }
                nodeCounts[group] = count
            }
        }
    }

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

    private fun updateSlot(value: Any?) {
        nextSlot()
        updateValue(value)
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

    // Classes

    /**
     * A holder that will dispose of its [CompositionContext] when it leaves the composition that
     * will not have its reference made visible to user code.
     */
    internal class CompositionContextHolder(override val ref: CompositionContextImpl) :
        RememberObserver, Reference<CompositionContext> {
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
        val composers = mutableSetOf<GapComposer>()

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
            super.registerComposer(composer as GapComposer)
            composers.add(composer)
        }

        override fun unregisterComposer(composer: Composer) {
            inspectionTables?.forEach { it.remove((composer as GapComposer).compositionData) }
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
            get() = this@GapComposer.composition.recomposeCoroutineContext

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
            parentContext.invalidate(this@GapComposer.composition)
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
            get() = this@GapComposer.composition

        override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
            return parentContext.scheduleFrameEndCallback(action)
        }
    }
}

/**
 * Pending starts when the key is different than expected indicating that the structure of the tree
 * changed. It is used to determine how to update the nodes and the slot table when changes to the
 * structure of the tree is detected.
 */
private class PendingChange(val keyInfos: MutableList<KeyInfo>, val startIndex: Int) {
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

internal class GapCompositionDataImpl(val composition: Composition) :
    CompositionData, CompositionInstance {
    private val slotTable
        get() = (composition as CompositionImpl).slotStorage.asGapBufferSlotTable()

    override val compositionGroups: Iterable<CompositionGroup>
        get() = slotTable.compositionGroups

    override val isEmpty: Boolean
        get() = slotTable.isEmpty

    override fun find(identityToFind: Any): CompositionGroup? = slotTable.find(identityToFind)

    override fun hashCode(): Int = composition.hashCode() * 31

    override fun equals(other: Any?): Boolean =
        other is GapCompositionDataImpl && composition == other.composition

    override val parent: CompositionInstance?
        get() = composition.parent?.let { GapCompositionDataImpl(it) }

    override val data: CompositionData
        get() = this

    override fun findContextGroup(): CompositionGroup? {
        val parentSlotTable = composition.parent?.slotTable?.asGapBufferSlotTable() ?: return null
        val context = composition.context ?: return null

        return parentSlotTable.findSubcompositionContextGroup(context)?.let {
            parentSlotTable.compositionGroupOf(it)
        }
    }

    private val Composition.slotTable
        get() = (this as? CompositionImpl)?.slotStorage

    private val Composition.context
        get() = (this as? CompositionImpl)?.parent

    private val Composition.parent
        get() = context?.composition
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

private fun Boolean.asInt() = if (this) 1 else 0

private fun Int.asBool() = this != 0

private fun SlotTable.collectNodesFrom(anchor: GapAnchor): List<Any?> {
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

private fun <K : Any, V : Any> multiMap(initialCapacity: Int) =
    MultiValueMap<K, V>(MutableScatterMap(initialCapacity))

private val InvalidationLocationAscending =
    Comparator<Invalidation> { i1, i2 -> i1.location.compareTo(i2.location) }
