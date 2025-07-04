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

package androidx.compose.ui.relocation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class BringIntoViewModifierNodeTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private fun Float.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    @Test
    fun zeroSizedItem_zeroSizedParent_bringIntoView() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        var requestedRect: Rect? = null
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        requestedRect = rect()?.translate(coordinates.positionInRoot())
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect.Zero) }
    }

    @Test
    fun bringIntoView_rectInChild() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        var requestedRect: Rect? = null
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        requestedRect = rect()?.translate(coordinates.positionInRoot())
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView(Rect(1f, 2f, 3f, 4f)) }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect(1f, 2f, 3f, 4f)) }
    }

    @Test
    fun bringIntoView_childWithSize() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        var requestedRect: Rect? = null
        rule.setContent {
            Box(Modifier) {
                Box(
                    Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                            requestedRect = rect()?.translate(coordinates.positionInRoot())
                        }
                        .size(20f.toDp(), 10f.toDp())
                        .offset { IntOffset(40, 30) }
                        .bringIntoViewRequester(bringIntoViewRequester)
                )
            }
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect(40f, 30f, 60f, 40f)) }
    }

    @Test
    fun bringIntoView_childBiggerThanParent() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        var requestedRect: Rect? = null
        rule.setContent {
            Box(
                Modifier.size(1f.toDp())
                    .fakeBringIntoViewResponder { coordinates, rect ->
                        requestedRect = rect()?.translate(coordinates.positionInRoot())
                    }
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .size(20f.toDp(), 10f.toDp())
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect(0f, 0f, 20f, 10f)) }
    }

    @Test
    fun bringIntoView_onlyPropagatesUp() {
        // Arrange.
        var parentRequest: Rect? = null
        var childRequest: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        parentRequest = rect()?.translate(coordinates.positionInRoot())
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .fakeBringIntoViewResponder { coordinates, rect ->
                        childRequest = rect()?.translate(coordinates.positionInRoot())
                    }
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentRequest).isEqualTo(Rect.Zero)
            assertThat(childRequest).isNull()
        }
    }

    @Test
    fun bringIntoView_callNearestHandler() {
        // Arrange.
        var parentRequest: Rect? = null
        var childRequest: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        parentRequest = rect()?.translate(coordinates.positionInRoot())
                    }
                    .fakeBringIntoViewResponder { coordinates, rect ->
                        childRequest = rect()?.translate(coordinates.positionInRoot())
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle {
            assertThat(parentRequest).isNull()
            assertThat(childRequest).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun bringIntoView_translatesByCalculateRectForParent() {
        // Arrange.
        var requestedRect: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        requestedRect = rect()?.translate(coordinates.positionInRoot())
                    }
                    .offset { IntOffset(2, 3) }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect(2f, 3f, 2f, 3f)) }
    }

    @Test
    fun bringIntoView_interruptsCurrentRequest_whenNewRequestOverlapsButNotContainedByCurrent() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        val requests = mutableListOf<CancellableContinuation<Unit>>()
        val requestScope = TestScope()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { _, _ ->
                        suspendCancellableCoroutine { requests += it }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        requestScope.launch { bringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 10f, 10f)) }
        requestScope.advanceUntilIdle()
        val initialRequest = requests.single()
        assertThat(initialRequest.isActive).isTrue()

        requestScope.launch { bringIntoViewRequester.bringIntoView(rect = Rect(5f, 5f, 15f, 15f)) }
        requestScope.advanceUntilIdle()
        assertThat(requests).hasSize(2)
        val newRequest = requests.last()
        assertThat(newRequest).isNotSameInstanceAs(initialRequest)
        assertThat(newRequest.isActive).isTrue()
    }

    @Test
    fun bringIntoView_interruptsCurrentRequest_whenNewRequestOutsideCurrent() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        val requests = mutableListOf<CancellableContinuation<Unit>>()
        val requestScope = TestScope()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { _, _ ->
                        suspendCancellableCoroutine { requests += it }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        requestScope.launch { bringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 10f, 10f)) }
        requestScope.advanceUntilIdle()
        val initialRequest = requests.single()
        assertThat(initialRequest.isActive).isTrue()

        requestScope.launch {
            bringIntoViewRequester.bringIntoView(rect = Rect(15f, 15f, 20f, 20f))
        }
        requestScope.advanceUntilIdle()
        assertThat(requests).hasSize(2)
        val newRequest = requests.last()
        assertThat(newRequest).isNotSameInstanceAs(initialRequest)
        assertThat(newRequest.isActive).isTrue()
    }

    /**
     * When an ongoing request is interrupted, it shouldn't be cancelled: the implementor is
     * responsible for cancelling ongoing work.
     */
    @Test
    fun bringIntoView_doesNotCancelOngoingRequest_whenInterrupted() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        val requests = mutableListOf<CancellableContinuation<Unit>>()
        val requestScope = TestScope()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { _, _ ->
                        suspendCancellableCoroutine { requests += it }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }
        requestScope.launch { bringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 10f, 10f)) }
        requestScope.advanceUntilIdle()
        assertThat(requests).hasSize(1)

        // Act.
        requestScope.launch {
            // Send an interrupting request.
            bringIntoViewRequester.bringIntoView(rect = Rect(15f, 15f, 20f, 20f))
        }
        requestScope.advanceUntilIdle()

        // Assert.
        assertThat(requests).hasSize(2)
        assertThat(requests.first().isActive).isTrue()
    }

    @Test
    fun bringIntoView_invokesResponder_whenPreviousRequestStillSuspended() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        val requests = mutableListOf<CancellableContinuation<Unit>>()
        val requestScope = TestScope()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { _, _ ->
                        suspendCancellableCoroutine { requests += it }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }
        requestScope.launch { bringIntoViewRequester.bringIntoView(rect = Rect(0f, 0f, 10f, 10f)) }
        requestScope.advanceUntilIdle()
        assertThat(requests).hasSize(1)

        // Act.
        requestScope.launch {
            bringIntoViewRequester.bringIntoView(rect = Rect(20f, 20f, 30f, 30f))
        }
        requestScope.advanceUntilIdle()

        // Assert.
        assertThat(requests).hasSize(2)
        assertThat(requests.all { it.isActive }).isTrue()

        requestScope.cancel()
    }

    @Test
    fun bringChildIntoView_isCalled_whenRectForParentDoesNotReturnInput() {
        // Arrange.
        var requestedRect: Rect? = null
        val bringIntoViewRequester = BringIntoViewRequester()
        rule.setContent {
            Box(
                Modifier.fakeBringIntoViewResponder { coordinates, rect ->
                        requestedRect = rect()?.translate(coordinates.positionInRoot())
                    }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }

        // Act.
        runBlocking { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.runOnIdle { assertThat(requestedRect).isEqualTo(Rect.Zero) }
    }

    @Test
    fun isChildOf_returnsTrue_whenDirectChild() {
        val parent = Job()
        val child = Job(parent)
        assertThat(child.isChildOf(parent)).isTrue()
    }

    @Test
    fun isChildOf_returnsTrue_whenIndirectChild() {
        val root = Job()
        val parent = Job(root)
        val child = Job(parent)
        assertThat(child.isChildOf(root)).isTrue()
    }

    @Test
    fun isChildOf_returnsFalse_whenReceiverIsParent() {
        val parent = Job()
        val child = Job(parent)
        assertThat(parent.isChildOf(child)).isFalse()
    }

    @Test
    fun isChildOf_returnsFalse_whenUnrelated() {
        val job1 = Job()
        val job2 = Job()
        assertThat(job1.isChildOf(job2)).isFalse()
    }

    private fun Job.isChildOf(expectedParent: Job): Boolean =
        expectedParent.children.any { it === this || this.isChildOf(it) }
}

/**
 * Returns a modifier that provides an implementation of the [FakeBringIntoViewModifierNode] that
 * redirects any [bringIntoView] to the [onBringIntoView] lambda.
 */
private fun Modifier.fakeBringIntoViewResponder(
    onBringIntoView: suspend (LayoutCoordinates, () -> Rect?) -> Unit
): Modifier =
    this.then(
        object : ModifierNodeElement<FakeBringIntoViewModifierNode>() {
            override fun create(): FakeBringIntoViewModifierNode {
                return FakeBringIntoViewModifierNode(onBringIntoView)
            }

            override fun update(node: FakeBringIntoViewModifierNode) {
                node.onBringIntoView = onBringIntoView
            }

            override fun hashCode(): Int = onBringIntoView.hashCode()

            override fun equals(other: Any?): Boolean {
                return (this === other)
            }
        }
    )

private class FakeBringIntoViewModifierNode(
    var onBringIntoView: suspend (LayoutCoordinates, () -> Rect?) -> Unit
) : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) = onBringIntoView(childCoordinates, boundsProvider)
}
