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

package androidx.compose.remote.player.compose.embedded

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.collection.emptyIntSet
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.MapRcDocVariables
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

internal val LocalCoreDocument: ProvidableCompositionLocal<CoreDocument> = compositionLocalOf {
    throw IllegalStateException("No document")
}

/**
 * The pure-Compose [GraphContext] that resolves *computed* operations (color/text/float/int
 * expressions, attributes, lookups) via `derivedStateOf`. Resolvers route a computed id through it;
 * leaf variables read the snapshot store directly. Null when no graph is installed (shouldn't
 * happen inside [RcPlayer]).
 */
internal val LocalGraphContext: ProvidableCompositionLocal<GraphContext?> = compositionLocalOf {
    null
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRemoteContext: ProvidableCompositionLocal<RemoteContext> = compositionLocalOf {
    throw IllegalStateException("No remote context")
}

internal val LocalComponentValueMap: ProvidableCompositionLocal<Map<Int, List<ComponentValue>>> =
    compositionLocalOf {
        emptyMap()
    }

internal val LocalComponentValueStateMap:
    ProvidableCompositionLocal<Map<Int, MutableState<Float>>> =
    compositionLocalOf {
        emptyMap()
    }

internal val LocalComponentTouchExpressionsMap:
    ProvidableCompositionLocal<Map<Int, List<TouchExpression>>> =
    compositionLocalOf {
        emptyMap()
    }

internal val LocalHasTouchExpressions: ProvidableCompositionLocal<Boolean> = compositionLocalOf {
    false
}

internal val LocalTouchExpressionIds: ProvidableCompositionLocal<IntSet> = compositionLocalOf {
    emptyIntSet()
}

internal class RootLayoutCoordinatesHolder(var coordinates: LayoutCoordinates? = null) {
    /** The player size last published to the core document, or null before the first placement. */
    var publishedSize: IntSize? = null
}

internal val LocalRootLayoutCoordinates: ProvidableCompositionLocal<RootLayoutCoordinatesHolder> =
    compositionLocalOf {
        RootLayoutCoordinatesHolder()
    }

internal val LocalCurrentTimeMillis: ProvidableCompositionLocal<State<Float>> = compositionLocalOf {
    mutableFloatStateOf(0f)
}

/** Host-action callback (id, value) for `HostAction`/`RunAction` clicks. Default no-op. */
internal val LocalRemoteActionHandler: ProvidableCompositionLocal<(Int, String?) -> Unit> =
    compositionLocalOf {
        { _, _ -> }
    }

/**
 * Host named-action callback (name, resolved value) for `HostNamedAction` clicks. Default no-op.
 */
internal val LocalRemoteNamedActionHandler: ProvidableCompositionLocal<(String, Any?) -> Unit> =
    compositionLocalOf {
        { _, _ -> }
    }

/**
 * Raw host `onAction` callback without parent `PendingIntentAction` interception, used by embedded
 * child documents so untrusted child action IDs cannot invoke parent `PendingIntent`s.
 */
internal val LocalHostRemoteActionHandler: ProvidableCompositionLocal<(Int, String?) -> Unit> =
    compositionLocalOf {
        { _, _ -> }
    }

/**
 * Raw host `onNamedAction` callback without parent `LambdaAction` / `PendingIntentAction`
 * interception, used by embedded child documents so untrusted child actions cannot invoke parent
 * closures or `PendingIntent`s.
 */
internal val LocalHostRemoteNamedActionHandler:
    ProvidableCompositionLocal<(String, Any?, StateUpdater) -> Unit> =
    compositionLocalOf {
        { _, _, _ -> }
    }

/**
 * [RcTypefaceResolver] for resolving typefaces in the embedded player. Null when using the default
 * built-in resolver.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRcTypefaceResolver: ProvidableCompositionLocal<RcTypefaceResolver?> =
    compositionLocalOf {
        null
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> =
    compositionLocalOf {
        null
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalAnimatedVisibilityScope: ProvidableCompositionLocal<AnimatedVisibilityScope?> =
    compositionLocalOf {
        null
    }
/**
 * True when composing unplaced `FitBoxSlot.Probe` measurement candidates so probe subtrees do not
 * allocate interaction/ripple nodes or register duplicate Compose focus targets.
 */
internal val LocalRcIsMeasureProbe: ProvidableCompositionLocal<Boolean> = compositionLocalOf {
    false
}

internal const val DEFAULT_MAX_RC_DOC_NESTING_DEPTH: Int = 1

/**
 * Maximum nesting depth for embedded `RcDocLayout` documents in [RcPlayer]. Defaults to `1` (host
 * -> child only). Set to `0` to disable embedded documents, or a higher value (e.g. `2` for
 * grandchildren) to permit deeper nesting.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRcDocMaxNestingDepth: ProvidableCompositionLocal<Int> = compositionLocalOf {
    DEFAULT_MAX_RC_DOC_NESTING_DEPTH
}

internal val LocalRcDocMappingsMap: ProvidableCompositionLocal<Map<Int, List<MapRcDocVariables>>> =
    compositionLocalOf {
        emptyMap()
    }

@OptIn(ExperimentalRemotePlayerApi::class)
internal val LocalRcPlayerFeatures: ProvidableCompositionLocal<RcPlayerFeatureSet> =
    compositionLocalOf {
        RcPlayerFeatureSet.Empty
    }

/**
 * Resolves an [RcPlayerFeatureSet] for an embedded child's `profileMask` when the host `RcPlayer`
 * did not pin an explicit [RcPlayerFeatureSet]. Null for pinned players so R8 can strip unused
 * features.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
internal val LocalRcPlayerFeatureResolver:
    ProvidableCompositionLocal<((Int) -> RcPlayerFeatureSet)?> =
    compositionLocalOf {
        null
    }

internal val LocalRcPlayerTheme: ProvidableCompositionLocal<Int> = compositionLocalOf {
    Theme.SYSTEM
}
