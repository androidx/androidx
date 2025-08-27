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

import androidx.compose.runtime.annotation.RememberInComposition

/**
 * Convert a lambda into one that moves the remembered state and nodes created in a previous call to
 * the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun movableContentOf(content: @Composable () -> Unit): @Composable () -> Unit {
    val movableContent = MovableContent<Nothing?>({ content() })
    return { currentComposer.insertMovableContent(movableContent, null) }
}

/**
 * Convert a lambda into one that moves the remembered state and nodes created in a previous call to
 * the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <P> movableContentOf(content: @Composable (P) -> Unit): @Composable (P) -> Unit {
    val movableContent = MovableContent(content)
    return { currentComposer.insertMovableContent(movableContent, it) }
}

/**
 * Convert a lambda into one that moves the remembered state and nodes created in a previous call to
 * the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <P1, P2> movableContentOf(
    content: @Composable (P1, P2) -> Unit
): @Composable (P1, P2) -> Unit {
    val movableContent = MovableContent<Pair<P1, P2>> { content(it.first, it.second) }
    return { p1, p2 -> currentComposer.insertMovableContent(movableContent, p1 to p2) }
}

/**
 * Convert a lambda into one that moves the remembered state and nodes created in a previous call to
 * the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <P1, P2, P3> movableContentOf(
    content: @Composable (P1, P2, P3) -> Unit
): @Composable (P1, P2, P3) -> Unit {
    val movableContent =
        MovableContent<Triple<P1, P2, P3>> { content(it.first, it.second, it.third) }
    return { p1, p2, p3 ->
        currentComposer.insertMovableContent(movableContent, Triple(p1, p2, p3))
    }
}

/**
 * Convert a lambda into one that moves the remembered state and nodes created in a previous call to
 * the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <P1, P2, P3, P4> movableContentOf(
    content: @Composable (P1, P2, P3, P4) -> Unit
): @Composable (P1, P2, P3, P4) -> Unit {
    val movableContent =
        MovableContent<Array<Any?>> { (p1, p2, p3, p4) ->
            @Suppress("UNCHECKED_CAST") // Types are guaranteed below.
            content(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
        }
    return { p1, p2, p3, p4 ->
        currentComposer.insertMovableContent(movableContent, arrayOf(p1, p2, p3, p4))
    }
}

/**
 * Convert a lambda with a receiver into one that moves the remembered state and nodes created in a
 * previous call to the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <R> movableContentWithReceiverOf(
    content: @Composable R.() -> Unit
): @Composable R.() -> Unit {
    val movableContent = MovableContent<R>({ it.content() })
    return { currentComposer.insertMovableContent(movableContent, this) }
}

/**
 * Convert a lambda with a receiver into one that moves the remembered state and nodes created in a
 * previous call to the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <R, P> movableContentWithReceiverOf(
    content: @Composable R.(P) -> Unit
): @Composable R.(P) -> Unit {
    val movableContent = MovableContent<Pair<R, P>>({ it.first.content(it.second) })
    return { currentComposer.insertMovableContent(movableContent, this to it) }
}

/**
 * Convert a lambda with a receiver into one that moves the remembered state and nodes created in a
 * previous call to the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <R, P1, P2> movableContentWithReceiverOf(
    content: @Composable R.(P1, P2) -> Unit
): @Composable R.(P1, P2) -> Unit {
    val movableContent = MovableContent<Triple<R, P1, P2>> { it.first.content(it.second, it.third) }
    return { p1, p2 -> currentComposer.insertMovableContent(movableContent, Triple(this, p1, p2)) }
}

/**
 * Convert a lambda with a receiver into one that moves the remembered state and nodes created in a
 * previous call to the new location it is called.
 *
 * Tracking compositions can be used to produce a composable that moves its content between a row
 * and a column based on a parameter, such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentColumnRowSample
 *
 * Or they can be used to ensure the composition state tracks with a model as moves in the layout,
 * such as,
 *
 * @sample androidx.compose.runtime.samples.MovableContentMultiColumnSample
 * @param content The composable lambda to convert into a state tracking lambda.
 * @return A tracking composable lambda
 */
@OptIn(InternalComposeApi::class)
@RememberInComposition
public fun <R, P1, P2, P3> movableContentWithReceiverOf(
    content: @Composable R.(P1, P2, P3) -> Unit
): @Composable R.(P1, P2, P3) -> Unit {
    val movableContent =
        MovableContent<Array<Any?>> { (r, p1, p2, p3) ->
            @Suppress("UNCHECKED_CAST") // Types are guaranteed below.
            (r as R).content(p1 as P1, p2 as P2, p3 as P3)
        }
    return { p1, p2, p3 ->
        currentComposer.insertMovableContent(movableContent, arrayOf(this, p1, p2, p3))
    }
}

// An arbitrary key created randomly. This key is used for the group containing the movable content
internal const val movableContentKey = 0x078cc281

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
    internal val slotStorage: SlotStorage,
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
public class MovableContentState internal constructor(internal val slotStorage: SlotStorage)
