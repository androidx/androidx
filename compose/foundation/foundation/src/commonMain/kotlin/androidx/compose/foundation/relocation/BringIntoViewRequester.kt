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

@file:JvmMultifileClass
@file:JvmName("BringIntoViewRequesterKt")

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Can be used to send [bringIntoView] requests. Pass it as a parameter to
 * [Modifier.bringIntoViewRequester()][bringIntoViewRequester].
 *
 * For instance, you can call [bringIntoView()][bringIntoView] to make all the
 * scrollable parents scroll so that the specified item is brought into the
 * scroll viewport.
 *
 * Note: this API is experimental while we optimise the performance and find the right API shape
 * for it.
 *
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 */
@ExperimentalFoundationApi
sealed interface BringIntoViewRequester {
    /**
     * Bring this item into bounds by making all the scrollable parents scroll appropriately.
     *
     * This method will not return until this request is satisfied or a newer request interrupts it.
     * If this call is interrupted by a newer call, this method will throw a
     * [CancellationException][kotlinx.coroutines.CancellationException].
     *
     * @param rect The rectangle (In local coordinates) that should be brought into view. If you
     * don't specify the coordinates, the coordinates of the
     * [Modifier.bringIntoViewRequester()][bringIntoViewRequester] associated with this
     * [BringIntoViewRequester] will be used.
     *
     * @sample androidx.compose.foundation.samples.BringIntoViewSample
     * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
     */
    suspend fun bringIntoView(rect: Rect? = null)
}

/**
 * Create an instance of [BringIntoViewRequester] that can be used with
 * [Modifier.bringIntoViewRequester][bringIntoViewRequester]. A child can then call
 * [BringIntoViewRequester.bringIntoView] to send a request any scrollable parents so that they
 * scroll to bring this item into view.
 *
 * Here is a sample where a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * Here is a sample where a part of a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 *
 * Note: this API is experimental while we optimise the performance and find the right API shape
 * for it
 */
@ExperimentalFoundationApi
fun BringIntoViewRequester(): BringIntoViewRequester {
    return BringIntoViewRequesterImpl()
}

/**
 * Modifier that can be used to send
 * [scrollIntoView][BringIntoViewRequester.bringIntoView] requests.
 *
 * The following example uses a `bringIntoViewRequester` to bring an item into
 * the parent bounds. The example demonstrates how a composable can ask its
 * parents to scroll so that the component using this modifier is brought into
 * the bounds of all its parents.
 *
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * @param bringIntoViewRequester An instance of [BringIntoViewRequester]. This
 *     hoisted object can be used to send
 *     [scrollIntoView][BringIntoViewRequester.scrollIntoView] requests to parents
 *     of the current composable.
 *
 * Note: this API is experimental while we optimise the performance and find the right API shape
 * for it
 */
@Suppress("ModifierInspectorInfo")
@ExperimentalFoundationApi
fun Modifier.bringIntoViewRequester(
    bringIntoViewRequester: BringIntoViewRequester
): Modifier = this.then(BringIntoViewRequesterElement(bringIntoViewRequester))

@ExperimentalFoundationApi
private class BringIntoViewRequesterImpl : BringIntoViewRequester {
    val modifiers = mutableVectorOf<BringIntoViewRequesterNode>()

    override suspend fun bringIntoView(rect: Rect?) {
        modifiers.forEach {
            it.scrollIntoView(rect)
        }
    }
}

@ExperimentalFoundationApi
private class BringIntoViewRequesterElement(
    private val requester: BringIntoViewRequester
) : ModifierNodeElement<BringIntoViewRequesterNode>() {
    override fun create(): BringIntoViewRequesterNode {
        return BringIntoViewRequesterNode(requester)
    }

    override fun update(node: BringIntoViewRequesterNode) {
        node.updateRequester(requester)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewRequester"
        properties["bringIntoViewRequester"] = requester
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) ||
            (other is BringIntoViewRequesterElement) && (requester == other.requester)
    }

    override fun hashCode(): Int {
        return requester.hashCode()
    }
}

/**
 * A modifier that holds state and modifier implementations for [bringIntoViewRequester]. It has
 * access to the next [BringIntoViewParent] via [findBringIntoViewParent], and uses that parent
 * to respond to requests to [scrollIntoView].
 */
@ExperimentalFoundationApi
internal class BringIntoViewRequesterNode(
    private var requester: BringIntoViewRequester
) : Modifier.Node() {
    override val shouldAutoInvalidate: Boolean = false

    override fun onAttach() {
        updateRequester(requester)
    }

    fun updateRequester(requester: BringIntoViewRequester) {
        disposeRequester()
        if (requester is BringIntoViewRequesterImpl) {
            requester.modifiers += this
        }
        this.requester = requester
    }

    private fun disposeRequester() {
        if (requester is BringIntoViewRequesterImpl) {
            (requester as BringIntoViewRequesterImpl).modifiers -= this
        }
    }

    override fun onDetach() {
        disposeRequester()
    }
}
