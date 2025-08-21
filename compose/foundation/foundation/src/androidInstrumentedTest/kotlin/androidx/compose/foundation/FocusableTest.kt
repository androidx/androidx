/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusableTest {

    @get:Rule val rule = createComposeRule()

    private val focusTag = "myFocusable"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun focusable_defaultSemantics() {
        rule.setFocusableContent {
            Box { BasicText("focusableText", modifier = Modifier.testTag(focusTag).focusable()) }
        }

        rule.onNodeWithTag(focusTag).assertIsEnabled().assert(isFocusable())
    }

    @Test
    fun focusable_disabledSemantics() {
        rule.setFocusableContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusable(enabled = false),
                )
            }
        }

        rule.onNodeWithTag(focusTag).assert(isNotFocusable())
    }

    @Test
    fun requestFocus_touchMode() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.testTag(focusTag).size(10.dp).focusRequester(focusRequester).focusable())
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Touch) }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.onNodeWithTag(focusTag).assertIsFocused()
    }

    @Test
    fun requestFocus_keyboardMode() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.testTag(focusTag).size(10.dp).focusRequester(focusRequester).focusable())
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.onNodeWithTag(focusTag).assertIsFocused()
    }

    @Test
    fun requestFocus_withTestApi_touchMode() {
        // Arrange.
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.testTag(focusTag).size(10.dp).focusable())
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Touch) }

        // Act.
        rule.onNodeWithTag(focusTag).requestFocus()

        // Assert.
        rule.onNodeWithTag(focusTag).assertIsFocused()
    }

    @Test
    fun requestFocus_withTestApi_keyboardMode() {
        // Arrange.
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.focusRequester(focusRequester).testTag(focusTag).size(10.dp).focusable())
        }
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        // Act.
        rule.onNodeWithTag(focusTag).requestFocus()

        // Assert.
        rule.onNodeWithTag(focusTag).assertIsFocused()
    }

    @Test
    fun focusable_focusAcquire() {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        rule.setFocusableContent {
            Box {
                BasicText(
                    "focusableText",
                    modifier = Modifier.testTag(focusTag).focusRequester(focusRequester).focusable(),
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier.focusRequester(otherFocusRequester).focusable(),
                )
            }
        }

        rule.onNodeWithTag(focusTag).assertIsNotFocused()

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(focusTag).assertIsFocused()

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.onNodeWithTag(focusTag).assertIsNotFocused()
    }

    @Test
    fun focusable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "focusableText",
                    modifier =
                        Modifier.testTag(focusTag)
                            .focusRequester(focusRequester)
                            .focusable(interactionSource = interactionSource),
                )
                BasicText(
                    "otherFocusableText",
                    modifier = Modifier.focusRequester(otherFocusRequester).focusable(),
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_interactionSource_resetWhenDisabled() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var enabled by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "focusableText",
                    modifier =
                        Modifier.testTag(focusTag)
                            .focusRequester(focusRequester)
                            .focusable(enabled = enabled, interactionSource),
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Make focusable disabled, Interaction should be gone
        rule.runOnIdle { enabled = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_interactionSource_resetWhenModifierDetached() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var emitFocusableNode by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "focusableText",
                    modifier =
                        Modifier.testTag(focusTag)
                            .focusRequester(focusRequester)
                            .then(
                                if (emitFocusableNode) {
                                    Modifier.focusable(interactionSource = interactionSource)
                                } else {
                                    Modifier
                                }
                            ),
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Dispose focusable, Interaction should be gone
        rule.runOnIdle { emitFocusableNode = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_interactionSource_resetWhenLayoutDetached() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var emitFocusableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitFocusableText) {
                    BasicText(
                        "focusableText",
                        modifier =
                            Modifier.testTag(focusTag)
                                .focusRequester(focusRequester)
                                .focusable(interactionSource = interactionSource),
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Dispose the layout node, Interaction should be gone
        rule.runOnIdle { emitFocusableText = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusable_pins_whenItIsFocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        var onPinInvoked = false
        val pinnableContainer =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    onPinInvoked = true
                    return PinnedHandle {}
                }
            }
        rule.setFocusableContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(Modifier.size(100.dp).focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnUiThread { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(onPinInvoked).isTrue() }
    }

    @Test
    fun focusable_unpins_whenItIsUnfocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        val focusRequester2 = FocusRequester()
        var onUnpinInvoked = false
        val pinnableContainer =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    return PinnedHandle { onUnpinInvoked = true }
                }
            }
        rule.setFocusableContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(Modifier.size(100.dp).focusRequester(focusRequester).focusable())
            }
            Box(Modifier.size(100.dp).focusRequester(focusRequester2).focusable())
        }

        // Act.
        rule.runOnUiThread { focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(onUnpinInvoked).isFalse()
            focusRequester2.requestFocus()
        }

        // Assert.
        rule.runOnIdle { assertThat(onUnpinInvoked).isTrue() }
    }

    @Test
    fun focusable_inspectorValue() {
        val modifier = Modifier.focusable().first() as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("focusable")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.asIterable())
            .containsExactly("enabled", "interactionSource")
    }

    @Test
    fun focusable_equality() {
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.focusable(enabled = toggleInput, interactionSource = interactionSource)
        }
    }

    @Suppress("DEPRECATION") // b/376080744
    @Test
    fun focusable_requestsBringIntoView_whenFocused() {
        // Arrange.
        val requestedRects = mutableListOf<Rect?>()
        val bringIntoViewResponder =
            object : androidx.compose.foundation.relocation.BringIntoViewResponder {
                override fun calculateRectForParent(localRect: Rect): Rect = localRect

                override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                    requestedRects += localRect()
                }
            }
        val focusRequester = FocusRequester()

        rule.setFocusableContent {
            with(rule.density) {
                Box(
                    Modifier.bringIntoViewResponder(bringIntoViewResponder)
                        .focusRequester(focusRequester)
                        .focusable()
                        // Needs a non-zero size.
                        .size(1f.toDp())
                )
            }
        }

        rule.runOnIdle { assertThat(requestedRects).isEmpty() }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle {
            assertThat(requestedRects).containsExactly(Rect(Offset.Zero, Size(1f, 1f)))
        }
    }

    // This test also verifies that the internal API autoInvalidateRemovedNode()
    // is called when a modifier node is disposed.
    @Test
    fun removingFocusableFromLazyList_clearsFocus() {
        // Arrange.
        var lazyRowHasFocus = false
        lateinit var state: LazyListState
        lateinit var coroutineScope: CoroutineScope
        var items by mutableStateOf((1..20).toList())
        rule.setFocusableContent {
            state = rememberLazyListState()
            coroutineScope = rememberCoroutineScope()
            LazyRow(
                modifier =
                    Modifier.requiredSize(100.dp).onFocusChanged { lazyRowHasFocus = it.hasFocus },
                state = state,
            ) {
                items(items.size) { Box(Modifier.requiredSize(10.dp).testTag("$it").focusable()) }
            }
        }
        rule.runOnIdle { coroutineScope.launch { state.scrollToItem(19) } }
        rule.onNodeWithTag("19").requestFocus()

        // Act.
        rule.runOnIdle { items = (1..11).toList() }

        // Assert.
        rule.runOnIdle { assertThat(lazyRowHasFocus).isFalse() }
    }

    @Test
    fun removingFocusableFromSubcomposeLayout_clearsFocus() {
        // Arrange.
        var hasFocus = false
        var itemVisible by mutableStateOf(true)
        rule.setFocusableContent {
            SubcomposeLayout(
                modifier = Modifier.requiredSize(100.dp).onFocusChanged { hasFocus = it.hasFocus }
            ) { constraints ->
                val measurable =
                    if (itemVisible) {
                        subcompose(Unit) {
                                Box(Modifier.requiredSize(10.dp).testTag("0").focusable())
                            }
                            .single()
                    } else null
                val placeable = measurable?.measure(constraints)
                layout(constraints.minWidth, constraints.minHeight) {
                    placeable?.place(IntOffset.Zero)
                }
            }
        }
        rule.onNodeWithTag("0").requestFocus()

        // Act.
        rule.runOnIdle { itemVisible = false }

        // Assert.
        rule.runOnIdle { assertThat(hasFocus).isFalse() }
    }

    @Test
    fun focusable_updatePinnableContainer_staysPinned() {
        // Arrange.
        val focusRequester = FocusRequester()
        var container1Pinned = false
        val pinnableContainer1 =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    container1Pinned = true
                    return PinnedHandle { container1Pinned = false }
                }
            }
        var container2Pinned = false
        val pinnableContainer2 =
            object : PinnableContainer {
                override fun pin(): PinnedHandle {
                    container2Pinned = true
                    return PinnedHandle { container2Pinned = false }
                }
            }
        var pinnableContainer by mutableStateOf<PinnableContainer>(pinnableContainer1)
        rule.setFocusableContent {
            CompositionLocalProvider(LocalPinnableContainer provides pinnableContainer) {
                Box(Modifier.size(100.dp).focusRequester(focusRequester).focusable())
            }
        }

        // Act.
        rule.runOnUiThread { focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(container1Pinned).isTrue()
            assertThat(container2Pinned).isFalse()
            pinnableContainer = pinnableContainer2
        }

        // Assert.
        rule.runOnIdle {
            assertThat(container1Pinned).isFalse()
            assertThat(container2Pinned).isTrue()
        }
    }

    @Test
    fun reusingFocusedItem_itemIsNotFocusedAnymore() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var state: FocusState
        var key by mutableStateOf(0)
        rule.setFocusableContent {
            ReusableContent(key) {
                BasicText(
                    "focusableText",
                    modifier =
                        Modifier.testTag(focusTag)
                            .focusRequester(focusRequester)
                            .onFocusEvent { state = it }
                            .focusable(),
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
            assertThat(state.isFocused).isTrue()
        }
        rule.onNodeWithTag(focusTag).assertIsFocused()

        // Act.
        rule.runOnIdle { key = 1 }

        // Assert.
        rule.runOnIdle { assertThat(state.isFocused).isFalse() }
        rule.onNodeWithTag(focusTag).assertIsNotFocused()
    }

    @Test
    fun movableContent_movedContentBecomesUnfocused() {
        var moveContent by mutableStateOf(false)
        val focusRequester = FocusRequester()
        val interactionSource = MutableInteractionSource()
        lateinit var state: FocusState
        lateinit var scope: CoroutineScope
        val content = movableContentOf {
            Box(
                Modifier.testTag(focusTag)
                    .size(5.dp)
                    .focusRequester(focusRequester)
                    .onFocusEvent { state = it }
                    .focusable(interactionSource = interactionSource)
            )
        }

        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            if (moveContent) {
                Box(Modifier.size(5.dp)) { content() }
            } else {
                Box(Modifier.size(10.dp)) { content() }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle {
            focusRequester.requestFocus() // request focus
            assertThat(state.isFocused).isTrue()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.onNodeWithTag(focusTag).assertIsFocused()

        rule.runOnIdle {
            moveContent = true // moving content
        }

        // Assert that focus is reset
        rule.runOnIdle { assertThat(state.isFocused).isFalse() }
        rule.onNodeWithTag(focusTag).assertIsNotFocused()

        rule.runOnIdle {
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
        }

        rule.runOnIdle {
            focusRequester.requestFocus() // request focus again
            assertThat(state.isFocused).isTrue()
        }

        rule.runOnIdle {
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    @Test
    fun clickingOutside_clearsFocus() =
        withFlags(isClearFocusOnPointerDownEnabled = true) {
            rule.setFocusableContent {
                Column {
                    Spacer(Modifier.size(32.dp).testTag("emptySpace"))
                    Spacer(Modifier.size(32.dp).testTag(focusTag).focusable())
                }
            }

            rule.onNodeWithTag(focusTag).requestFocus()

            rule.onNodeWithTag(focusTag).assertIsFocused()

            rule.onNodeWithTag("emptySpace").performMouseInput { click() }

            rule.onNodeWithTag(focusTag).assertIsNotFocused()
        }

    @Test
    fun clickingOutside_doesNotClearFocusWithViewTagDisabled() =
        withFlags(isClearFocusOnPointerDownEnabled = true) {
            lateinit var view: View

            rule.setFocusableContent {
                view = LocalView.current
                Column {
                    Spacer(Modifier.size(32.dp).testTag("emptySpace"))
                    Spacer(Modifier.size(32.dp).testTag(focusTag).focusable())
                }
            }

            (view.parent as AbstractComposeView).isClearFocusOnPointerDownEnabled = false

            rule.onNodeWithTag(focusTag).requestFocus()

            rule.onNodeWithTag(focusTag).assertIsFocused()

            rule.onNodeWithTag("emptySpace").performMouseInput { click() }

            rule.onNodeWithTag(focusTag).assertIsFocused()
        }

    @Test
    fun clickingOnClickable_clearsFocus() =
        withFlags(isClearFocusOnPointerDownEnabled = true) {
            var wasClickableSpaceClicked = false

            rule.setFocusableContent {
                Column {
                    Spacer(
                        Modifier.size(32.dp).testTag("clickableSpace").clickable {
                            wasClickableSpaceClicked = true
                        }
                    )
                    Spacer(Modifier.size(32.dp).testTag(focusTag).focusable())
                }
            }

            rule.onNodeWithTag(focusTag).requestFocus()

            rule.onNodeWithTag(focusTag).assertIsFocused()

            rule.onNodeWithTag("clickableSpace").performMouseInput { click() }

            rule.onNodeWithTag(focusTag).assertIsNotFocused()
            assertThat(wasClickableSpaceClicked).isTrue()
        }

    @Test
    fun clickingOnClickableFocusable_reassignsFocus() {
        val focusRequesterClickableSpace = FocusRequester()
        var wasClickableSpaceClicked = false

        rule.setFocusableContent {
            Column {
                Spacer(
                    Modifier.size(32.dp)
                        .testTag("clickableSpace")
                        .focusRequester(focusRequesterClickableSpace)
                        .focusProperties { canFocus = true }
                        .clickable {
                            wasClickableSpaceClicked = true
                            focusRequesterClickableSpace.requestFocus()
                        }
                )
                Spacer(Modifier.size(32.dp).testTag(focusTag).focusable())
            }
        }

        rule.onNodeWithTag(focusTag).requestFocus()

        rule.onNodeWithTag(focusTag).assertIsFocused()

        rule.onNodeWithTag("clickableSpace").performMouseInput { click() }

        rule.onNodeWithTag(focusTag).assertIsNotFocused()
        assertThat(wasClickableSpaceClicked).isTrue()
        rule.onNodeWithTag("clickableSpace").assertIsFocused()
    }

    @Test
    fun clickingOnAndroidViewThroughCompose_clearsFocus() =
        withFlags(isClearFocusOnPointerDownEnabled = true) {
            val focusRequester = FocusRequester()
            var wasClickableSpaceClicked = false
            lateinit var density: Density

            rule.setFocusableContent {
                density = LocalDensity.current
                Box(Modifier.testTag("box")) {
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                addView(
                                    ComposeView(context).apply {
                                        setContent {
                                            Column {
                                                Spacer(
                                                    Modifier.size(64.dp)
                                                        .testTag("clickableSpace")
                                                        .clickable {
                                                            wasClickableSpaceClicked = true
                                                        }
                                                )
                                                Spacer(Modifier.size(64.dp))
                                            }
                                        }
                                    },
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                    ),
                                )
                                addView(
                                    ComposeView(context).apply {
                                        setContent {
                                            Column {
                                                Spacer(Modifier.size(64.dp))
                                                Spacer(
                                                    Modifier.size(64.dp)
                                                        .testTag(focusTag)
                                                        .focusRequester(focusRequester)
                                                        .focusable()
                                                )
                                            }
                                        }
                                    },
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                    ),
                                )
                            }
                        }
                    )
                }
            }

            rule.onNodeWithTag(focusTag).requestFocus()

            rule.onNodeWithTag(focusTag).assertIsFocused()

            rule.onNodeWithTag("box").performMouseInput {
                click(with(density) { Offset(32.dp.toPx(), 32.dp.toPx()) })
            }

            rule.onNodeWithTag(focusTag).assertIsNotFocused()
            assertThat(wasClickableSpaceClicked).isTrue()
        }

    @OptIn(ExperimentalComposeUiApi::class)
    private inline fun withFlags(
        isClearFocusOnPointerDownEnabled: Boolean = ComposeUiFlags.isClearFocusOnPointerDownEnabled,
        block: () -> Unit,
    ) {
        val defaultValue = ComposeUiFlags.isClearFocusOnPointerDownEnabled
        try {
            ComposeUiFlags.isClearFocusOnPointerDownEnabled = isClearFocusOnPointerDownEnabled
            block()
        } finally {
            ComposeUiFlags.isClearFocusOnPointerDownEnabled = defaultValue
        }
    }
}
