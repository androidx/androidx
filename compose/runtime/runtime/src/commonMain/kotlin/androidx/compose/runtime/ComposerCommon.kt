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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.runtime

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.collection.IntStack
import androidx.compose.runtime.collection.Stack
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import kotlin.jvm.JvmInline

@OptIn(ExperimentalComposeRuntimeApi::class, InternalComposeApi::class)
@ComposeCompilerApi
internal abstract class ComposerCommon(
    /** An adapter that applies changes to the tree using the Applier abstraction. */
    override val applier: Applier<*>,

    /** Parent of this composition; a [Recomposer] for root-level compositions. */
    protected val parentContext: CompositionContext,

    /** The set of objects added to slot storage that have yet to have their onRemembered called */
    protected val abandonSet: MutableSet<RememberObserver>,

    /** The holder of composition observers registered using the composition observer API */
    protected val observerHolder: CompositionObserverHolder,

    /** The composition that owns this composer */
    override val composition: CompositionImpl,
) : InternalComposer {
    // Protected fields
    protected var childrenComposing: Int = 0
    protected var compositionToken: Int = 0
    override val currentRecomposeScope: RecomposeScopeImpl?
        get() =
            invalidateStack.let {
                if (childrenComposing == 0 && it.isNotEmpty()) it.peek() else null
            }

    protected val derivedStateObserver =
        object : DerivedStateObserver {
            override fun start(derivedState: DerivedState<*>) {
                childrenComposing++
            }

            override fun done(derivedState: DerivedState<*>) {
                childrenComposing--
            }
        }
    protected val entersStack = IntStack()
    protected var forceRecomposeScopes = false
    protected var forciblyRecompose = false
    protected var groupNodeCount: Int = 0
    protected val invalidateStack = Stack<RecomposeScopeImpl>()
    protected var nodeCountOverrides: MutableIntIntMap? = null
    protected var nodeCountVirtualOverrides: MutableIntIntMap? = null
    protected var nodeExpected = false
    protected var nodeIndex: Int = 0
    protected val parentStateStack = IntStack()
    protected var providersInvalid = false
    protected val providersInvalidStack = IntStack()
    protected var providerUpdates: MutableIntObjectMap<PersistentCompositionLocalMap>? = null
    protected var reusing = false
    protected var reusingGroup = -1
    protected var rGroupIndex: Int = 0
    protected var rootProvider: PersistentCompositionLocalMap =
        persistentCompositionLocalHashMapOf()
    protected var sourceMarkersEnabled =
        parentContext.collectingSourceInformation || parentContext.collectingCallByInformation
    protected var providerCache: PersistentCompositionLocalMap? = null
    protected var shouldPauseCallback: ShouldPauseCallback? = null

    // Internal fields
    override var isComposing = false
    internal var isDisposed = false

    // Overrides
    override val areChildrenComposing
        get() = childrenComposing > 0

    override fun changesApplied() {
        providerUpdates = null
    }

    override var compositeKeyHashCode: CompositeKeyHashCode = EmptyCompositeKeyHashCode
        protected set

    override val defaultsInvalid: Boolean
        get() {
            return !skipping || providersInvalid || currentRecomposeScope?.defaultsInvalid == true
        }

    override var deferredChanges: Changes? = null

    override fun disableReusing() {
        reusing = false
    }

    override fun disableSourceInformation() {
        sourceMarkersEnabled = false
    }

    override fun enableReusing() {
        reusing = reusingGroup >= 0
    }

    override val errorContext: CompositionErrorContextImpl? = CompositionErrorContextImpl(this)
        get() = if (sourceMarkersEnabled) field else null

    override fun forceRecomposeScopes(): Boolean {
        return if (!forceRecomposeScopes) {
            forceRecomposeScopes = true
            forciblyRecompose = true
            true
        } else {
            false
        }
    }

    override var inserting: Boolean = false

    override fun prepareCompose(block: () -> Unit) {
        runtimeCheck(!isComposing) { "Preparing a composition while composing is not supported" }
        isComposing = true
        try {
            block()
        } finally {
            isComposing = false
        }
    }

    override val recomposeScope: RecomposeScope?
        get() = currentRecomposeScope

    override val recomposeScopeIdentity: Any?
        get() = currentRecomposeScope?.anchor

    override fun recordUsed(scope: RecomposeScope) {
        (scope as? RecomposeScopeImpl)?.used = true
    }

    override val skipping: Boolean
        get() {
            return !inserting &&
                !reusing &&
                !providersInvalid &&
                currentRecomposeScope?.requiresRecompose == false &&
                !forciblyRecompose
        }

    // Protected methods
    protected fun enterRecomposeScope(scope: RecomposeScopeImpl) {
        scope.start(compositionToken)
        observerHolder.current()?.onScopeEnter(scope)
    }

    protected fun exitRecomposeScope(scope: RecomposeScopeImpl): ((Composition) -> Unit)? {
        observerHolder.current()?.onScopeExit(scope)
        return scope.end(compositionToken)
    }

    protected inline fun updateCompositeKeyWhenWeEnterGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Companion.Empty)
                updateCompositeKeyWhenWeEnterGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeEnterGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.hashCode(), 0)
    }

    protected inline fun updateCompositeKeyWhenWeEnterGroupKeyHash(
        groupKey: Int,
        rGroupIndex: Int,
    ) {
        compositeKeyHashCode =
            compositeKeyHashCode.compoundWith(groupKey, 3).compoundWith(rGroupIndex, 3)
    }

    protected inline fun updateCompositeKeyWhenWeExitGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Companion.Empty)
                updateCompositeKeyWhenWeExitGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeExitGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.hashCode(), 0)
    }

    protected inline fun updateCompositeKeyWhenWeExitGroupKeyHash(groupKey: Int, rGroupIndex: Int) {
        compositeKeyHashCode =
            compositeKeyHashCode.unCompoundWith(rGroupIndex, 3).unCompoundWith(groupKey, 3)
    }

    protected fun validateNodeExpected() {
        runtimeCheck(nodeExpected) {
            "A call to createNode(), emitNode() or useNode() expected was not expected"
        }
        nodeExpected = false
    }

    protected fun validateNodeNotExpected() {
        runtimeCheck(!nodeExpected) { "A call to createNode(), emitNode() or useNode() expected" }
    }
}

/*
 * Group types used with [Composer.start] to differentiate between different types of groups
 */
@JvmInline
internal value class GroupKind(internal val value: Int) {
    internal inline val isNode
        get() = value != Group.value

    internal inline val isReusable
        get() = value != Node.value

    companion object {
        internal val Group = GroupKind(0)
        internal val Node = GroupKind(1)
        internal val ReusableNode = GroupKind(2)
    }
}

internal fun getKey(value: Any?, left: Any?, right: Any?): Any? =
    (value as? JoinedKey)?.let {
        if (it.left == left && it.right == right) value
        else getKey(it.left, left, right) ?: getKey(it.right, left, right)
    }
