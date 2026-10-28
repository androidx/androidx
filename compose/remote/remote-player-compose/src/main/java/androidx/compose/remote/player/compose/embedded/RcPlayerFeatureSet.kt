/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX")
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.draw.ComposeLocalPaint
import androidx.compose.remote.player.compose.embedded.draw.GraphPaintContext
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

/**
 * The [RcPlayerFeature]s a player runs with. Pass one to [RcPlayer] to fix the features at build
 * time, so R8 can remove the features an app doesn't use.
 *
 * If several features share a [RcPlayerFeature.Key], the last one wins.
 *
 * Two sets are equal when they hold the same feature instances in the same order, so building an
 * equal set on recomposition doesn't restart the player's feature effects.
 */
@Immutable
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcPlayerFeatureSet(features: List<RcPlayerFeature>) {
    public constructor(vararg features: RcPlayerFeature) : this(features.asList())

    /** The features, without any that a later feature with the same key replaced. */
    public val features: List<RcPlayerFeature> =
        ArrayList<RcPlayerFeature>(features.size).also { result ->
            features.fastForEachIndexed { index, feature ->
                if ((index + 1 until features.size).none { features[it].key === feature.key }) {
                    result.add(feature)
                }
            }
        }

    /** Profile bits the player supports with these features, see [unsupportedOpcodes]. */
    public val profiles: Int =
        this.features.fastFold(BuiltInProfiles) { profiles, feature ->
            profiles or feature.profiles
        }

    /** Returns a set with [other]'s features added, replacing any with the same key. */
    public operator fun plus(other: RcPlayerFeatureSet): RcPlayerFeatureSet =
        RcPlayerFeatureSet(features + other.features)

    /**
     * Returns the opcodes that documents with [profileMask] may contain but that neither the
     * built-in loop nor these features execute. Empty when the player fully supports the profile.
     */
    public fun unsupportedOpcodes(profileMask: Int): IntSet {
        val unsupported = MutableIntSet()
        val required =
            Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, profileMask)
                ?: return unsupported
        val builtIn = Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, BuiltInProfiles)
        for (opcode in 0..MAX_OPCODE) {
            if (required.get(opcode) == null) continue
            val builtInOp = builtIn?.get(opcode) != null && opcode !in FeatureOpcodes
            if (builtInOp || opcode in BuiltInExtraOpcodes) continue
            if (features.fastAny { opcode in it.opcodes }) continue
            unsupported.add(opcode)
        }
        return unsupported
    }

    internal fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean = features.fastAny {
        it.paint(op, scope)
    }

    override fun toString(): String = "RcPlayerFeatureSet($features)"

    override fun equals(other: Any?): Boolean =
        this === other || (other is RcPlayerFeatureSet && features == other.features)

    override fun hashCode(): Int = features.hashCode()

    public companion object {
        /** No features: the player executes only its built-in profiles. */
        public val Empty: RcPlayerFeatureSet = RcPlayerFeatureSet(emptyList())

        /** Profiles the player's draw loop executes without any feature. */
        public const val BuiltInProfiles: Int = RcProfiles.PROFILE_ANDROIDX

        /**
         * Experimental operations the player still executes built in. They move to features once
         * features can hook into layout.
         */
        internal val BuiltInExtraOpcodes: IntSet =
            intSetOf(
                Operations.LAYOUT_FLOW,
                Operations.LAYOUT_CUSTOM,
                Operations.MODIFIER_MULTI_CLICK,
                Operations.MODIFIER_DIMENSION_CONSTRAINTS,
            )

        /**
         * Operations of the built-in profiles that the draw loop leaves to features, so an app that
         * doesn't need them can leave them out, see [MatrixFeature].
         */
        internal val FeatureOpcodes: IntSet =
            intSetOf(
                Operations.MATRIX_EXPRESSION,
                Operations.MATRIX_VECTOR_MATH,
                Operations.MATRIX_FROM_PATH,
            )

        private const val MAX_OPCODE = 255
    }
}

/**
 * Every feature the library provides, for the [RcPlayer] overloads that don't take an
 * [RcPlayerFeatureSet]. Referencing this keeps all of them in the app.
 */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RcPlayerFeatures {
    /** All features the library provides. */
    public val All: List<RcPlayerFeature>
        get() = listOf(MatrixFeature, MeshFeature, SoundFeature.Default, SensorFeature)

    /**
     * The features for documents with [profileMask]: those whose [RcPlayerFeature.profiles] the
     * mask includes, plus host capabilities that belong to no profile.
     */
    public fun forProfiles(profileMask: Int): RcPlayerFeatureSet =
        RcPlayerFeatureSet(All.fastFilter { (it.profiles and profileMask) == it.profiles })
}

/** What an [RcPlayerFeature] can access while executing an operation in the draw pass. */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcFeatureDrawScope
internal constructor(
    /** The canvas, size and density of the current draw. */
    public val drawScope: DrawScope,
    /** The player's [RemoteContext], holding the document's state. */
    public val remoteContext: RemoteContext,
    internal val paintState: ComposeLocalPaint,
    internal val graph: GraphContext?,
    internal val evalPaintContext: () -> GraphPaintContext,
)

/** What an [RcPlayerFeature] can access for the lifetime of a document in the player. */
@ExperimentalRemotePlayerApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcFeatureDocumentScope
internal constructor(
    /** The document the player is showing. */
    public val document: CoreDocument,
    /** The player's [RemoteContext], holding the document's state. */
    public val remoteContext: RemoteContext,
)

/** Returns true if the document contains an operation matching [predicate], at any depth. */
internal fun CoreDocument.containsOperation(predicate: (Operation) -> Boolean): Boolean =
    operations.fastAny {
        it.containsOperation(predicate)
    }

private fun Operation.containsOperation(predicate: (Operation) -> Boolean): Boolean {
    if (predicate(this)) return true
    if (this is LayoutComponent) {
        componentModifiers?.list?.fastForEach { if (it.containsOperation(predicate)) return true }
        if (getCanvasOperations()?.containsOperation(predicate) == true) return true
    }
    if (this is Container) {
        val list = getList()
        for (i in 0 until list.size) {
            if (list[i].containsOperation(predicate)) return true
        }
    }
    return false
}
