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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.node

import androidx.collection.IntObjectMap
import androidx.collection.intObjectMapOf
import androidx.compose.runtime.RetainScope
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.EmptySemanticsModifier
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModifierLocalConsumerEntityTest {

    private val default = "Default"
    private val ModifierLocalString = modifierLocalOf { "Default" }

    private val owner = FakeOwner()
    private val layoutNode = LayoutNode()

    @Test
    fun `unattached modifier local consumer does not invoke lambda`() {
        // Arrange.
        var receivedValue = ""
        TestBox(Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current })

        // Assert.
        assertThat(receivedValue).isEmpty()
    }

    @Test
    fun `attached modifier local consumer with no provider reads default value`() {
        // Arrange.
        lateinit var receivedValue: String
        TestBox(Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current })

        // Act.
        attach()

        // Assert.
        assertThat(receivedValue).isEqualTo(default)
    }

    @Test
    fun `changing the consumer modifier with no provider reads default value`() {
        // Arrange.
        lateinit var receivedValue: String
        TestBox(Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current })
        attach()
        receivedValue = ""

        // Act.
        changeModifier(
            Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )

        // Assert.
        assertThat(receivedValue).isEqualTo(default)
    }

    @Test
    fun `detached modifier local consumer with no provider invokes with default providers`() {
        // Arrange.
        lateinit var receivedValue: String
        TestBox(Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current })
        attach()
        receivedValue = ""

        // Act.
        detach()

        // Assert.
        assertThat(receivedValue).isEqualTo(default)
    }

    @Test
    fun `unattached modifier local consumer with provider does not invoke lambda`() {
        // Arrange.
        var receivedValue = ""
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { "Initial Value" }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )

        // Assert.
        assertThat(receivedValue).isEmpty()
    }

    @Test
    fun `attached modifier local consumer with provider reads provided value`() {
        // Arrange.
        val providedValue = "Provided Value"
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { providedValue }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        // Act.
        attach()

        // Assert.
        assertThat(receivedValue).isEqualTo(providedValue)
    }

    @Test
    fun `changing provided value causes consumer to receive new provided value`() {
        // Arrange.
        val initialValue = "Initial Value"
        val finalValue = "Final Value"
        var providedValue by mutableStateOf(initialValue)
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { providedValue }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        attach()

        // Act.
        Snapshot.withMutableSnapshot { providedValue = finalValue }

        // Assert.
        assertThat(receivedValue).isEqualTo(finalValue)
    }

    @Test
    fun `changing provided value after detaching modifier does not invoke consumer lambda`() {
        // Arrange.
        val initialValue = "Initial Value"
        val finalValue = "Final Value"
        var providedValue by mutableStateOf(initialValue)
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { providedValue }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        attach()
        detach()
        receivedValue = ""

        // Act.
        Snapshot.withMutableSnapshot { providedValue = finalValue }

        // Assert.
        assertThat(receivedValue).isEmpty()
    }

    @Test
    fun `changing modifiers after detaching modifier does not invoke consumer lambda`() {
        // Arrange.
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { "Provided Value" }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        attach()
        detach()
        receivedValue = ""

        // Act.
        changeModifier(
            Modifier.modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )

        // Assert.
        assertThat(receivedValue).isEmpty()
    }

    @Test
    fun `changing the consumer modifier with provider reads provided value`() {
        // Arrange.
        val providedValue = "Provided Value"
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { providedValue }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        attach()
        receivedValue = ""

        // Act.
        changeModifier(
            Modifier.modifierLocalProvider(ModifierLocalString) { providedValue }
                .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )

        // Assert.
        assertThat(receivedValue).isEqualTo(providedValue)
    }

    @Test
    fun `detached modifier local consumer with provider invokes with default provider`() {
        // Arrange.
        lateinit var receivedValue: String
        TestBox(
            modifier =
                Modifier.modifierLocalProvider(ModifierLocalString) { "Provided Value" }
                    .modifierLocalConsumer { receivedValue = ModifierLocalString.current }
        )
        attach()
        receivedValue = ""

        // Act.
        detach()

        // Assert.
        assertThat(receivedValue).isEqualTo(default)
    }

    private fun TestBox(modifier: Modifier = Modifier) {
        owner.snapshotObserver.startObserving()
        layoutNode.modifier = modifier
    }

    private fun attach() {
        // Apply changes after attaching
        layoutNode.attach(owner)
        owner.onEndApplyChanges()
    }

    private fun detach() {
        // Apply changes after detaching
        layoutNode.detach()
        owner.onEndApplyChanges()
    }

    private fun changeModifier(modifier: Modifier) {
        with(layoutNode) {
            this.modifier = modifier
            owner?.onEndApplyChanges()
        }
    }

    @OptIn(InternalComposeUiApi::class)
    private class FakeOwner : Owner {
        val listeners = mutableVectorOf<() -> Unit>()

        @OptIn(InternalCoreApi::class) override var showLayoutBounds: Boolean = false
        override val snapshotObserver: OwnerSnapshotObserver = OwnerSnapshotObserver { it.invoke() }

        override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)
        override val dragAndDropManager: DragAndDropManager
            get() = TODO("Not yet implemented")

        override val coroutineContext: CoroutineContext =
            Executors.newFixedThreadPool(3).asCoroutineDispatcher()

        override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
            listeners += listener
        }

        override fun onEndApplyChanges() {
            while (listeners.isNotEmpty()) {
                listeners.removeAt(0).invoke()
            }
        }

        override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
            TODO("Not yet implemented")
        }

        override fun onRequestMeasure(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean,
            scheduleMeasureAndLayout: Boolean,
        ) {}

        override fun onPreAttach(node: LayoutNode) {}

        override fun onPostAttach(node: LayoutNode) {}

        override fun onDetach(node: LayoutNode) {}

        override val root: LayoutNode = LayoutNode()

        override val semanticsOwner: SemanticsOwner =
            SemanticsOwner(root, EmptySemanticsModifier(), intObjectMapOf())

        override val layoutNodes: IntObjectMap<LayoutNode>
            get() = TODO("Not yet implemented")

        override val sharedDrawScope: LayoutNodeDrawScope
            get() = TODO("Not yet implemented")

        override val rootForTest: RootForTest
            get() = TODO("Not yet implemented")

        override val hapticFeedBack: HapticFeedback
            get() = TODO("Not yet implemented")

        override val inputModeManager: InputModeManager
            get() = TODO("Not yet implemented")

        override val clipboardManager: ClipboardManager
            get() = TODO("Not yet implemented")

        override val clipboard: Clipboard
            get() = TODO("Not yet implemented")

        override val accessibilityManager: AccessibilityManager
            get() = TODO("Not yet implemented")

        override val graphicsContext: GraphicsContext
            get() = TODO("Not yet implemented")

        override val textToolbar: TextToolbar
            get() = TODO("Not yet implemented")

        override val density: Density
            get() = TODO("Not yet implemented")

        override val textInputService: TextInputService
            get() = TODO("Not yet implemented")

        override val softwareKeyboardController: SoftwareKeyboardController
            get() = TODO("Not yet implemented")

        override val pointerIconService: PointerIconService
            get() = TODO("Not yet implemented")

        override val focusOwner: FocusOwner
            get() = TODO("Not yet implemented")

        override val retainScope: RetainScope
            get() = TODO("Not yet implemented")

        override val windowInfo: WindowInfo
            get() = TODO("Not yet implemented")

        override val rectManager: RectManager = RectManager()

        @Deprecated(
            "fontLoader is deprecated, use fontFamilyResolver",
            replaceWith = ReplaceWith("fontFamilyResolver"),
        )
        @Suppress("DEPRECATION")
        override val fontLoader: Font.ResourceLoader
            get() = TODO("Not yet implemented")

        override val fontFamilyResolver: FontFamily.Resolver
            get() = TODO("Not yet implemented")

        override val layoutDirection: LayoutDirection
            get() = TODO("Not yet implemented")

        override val measureIteration: Long
            get() = TODO("Not yet implemented")

        override val viewConfiguration: ViewConfiguration
            get() = TODO("Not yet implemented")

        override val autofillTree: AutofillTree
            get() = TODO("Not yet implemented")

        override val autofill: Autofill
            get() = TODO("Not yet implemented")

        override val autofillManager: AutofillManager
            get() = TODO("Not yet implemented")

        override fun createLayer(
            drawBlock: (Canvas, GraphicsLayer?) -> Unit,
            invalidateParentLayer: () -> Unit,
            explicitLayer: GraphicsLayer?,
        ) = TODO("Not yet implemented")

        override fun onRequestRelayout(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean,
        ) = TODO("Not yet implemented")

        override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
            TODO("Not yet implemented")
        }

        override fun calculatePositionInWindow(localPosition: Offset) = TODO("Not yet implemented")

        override fun calculateLocalPosition(positionInWindow: Offset) = TODO("Not yet implemented")

        override fun requestAutofill(node: LayoutNode) {
            TODO("Not yet implemented")
        }

        override fun measureAndLayout(sendPointerUpdate: Boolean) = TODO("Not yet implemented")

        override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
            TODO("Not yet implemented")
        }

        override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) =
            TODO("Not yet implemented")

        override fun onSemanticsChange() {}

        override fun onLayoutChange(layoutNode: LayoutNode) = TODO("Not yet implemented")

        override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {}

        override fun onInteropViewLayoutChange(view: InteropView) = TODO("Not yet implemented")

        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ): Nothing {
            TODO("Not yet implemented")
        }

        override fun screenToLocal(positionOnScreen: Offset): Offset {
            TODO("Not yet implemented")
        }

        override fun localToScreen(localPosition: Offset): Offset {
            TODO("Not yet implemented")
        }
    }
}

private fun LayoutNode.forEachNodeCoordinator(action: (NodeCoordinator) -> Unit) {
    var coordinator: NodeCoordinator? = outerCoordinator
    while (coordinator != null) {
        action.invoke(coordinator)
        coordinator = coordinator.wrapped
    }
}
