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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.LocalA2uiReadinessEvaluator
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.asReadinessEvaluator
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Verifies that every basic catalog component whose readiness depends on a dynamic binding returns
 * early from its `Content`, instead of throwing, when that binding stops resolving to a usable
 * value while the content is still composed.
 *
 * Hosts that animate state transitions keep the outgoing content composed after the component
 * leaves [A2uiComponentState.Success], and that content recomposes with the updated data model.
 */
@MediumTest
@RunWith(Parameterized::class)
class A2uiBasicCatalogV1ExitingContentUiTest(private val testCase: TestCase) {

    @Test
    fun content_requiredBindingStopsResolvingWhileComposed_returnsWithoutRendering() =
        runComposeUiTest {
            val catalog =
                A2uiCatalog(catalogId = "test_catalog", components = listOf(testCase.component))
            val controller =
                A2uiTestController(
                    catalog = catalog,
                    initialComponents =
                        listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = testCase.component.name,
                                properties = testCase.properties,
                            )
                        ),
                    initialData = mapOf(DataKey to testCase.initialData),
                )
            val surface = controller.start()

            setContent {
                RetainingTestSurface(
                    surface = surface,
                    catalog = catalog,
                    errorContent = { exception ->
                        if (testCase.reportsError) {
                            BasicText(ErrorText)
                        } else {
                            throw AssertionError(
                                "Component failed to render: ${exception.message}",
                                exception,
                            )
                        }
                    },
                )
            }

            onNodeWithTag(RenderedTag).assertIsDisplayed()
            onNodeWithText(LoadingText).assertDoesNotExist()

            // The agent removes the data behind the required binding, or replaces it with a value
            // the component cannot use. The component is not ready anymore, but the host keeps its
            // content composed, so the content recomposes and sees the binding evaluate to null.
            controller.updateData(DataPath, testCase.updatedData)
            controller.waitForIdle()
            waitForIdle()

            onNodeWithText(if (testCase.reportsError) ErrorText else LoadingText)
                .assertIsDisplayed()
            onNodeWithTag(RenderedTag).assertDoesNotExist()
        }

    /**
     * A basic catalog [component] with a required dynamic binding to [DataPath] in [properties],
     * and the [initialData] to store at [DataPath] so that the component renders initially.
     *
     * [updatedData] then replaces the data at [DataPath]; by default it removes it. [reportsError]
     * is `true` when the component reports [updatedData] as an error, and therefore ends up in
     * [A2uiComponentState.Error] rather than [A2uiComponentState.Loading].
     */
    class TestCase(
        private val name: String,
        val component: A2uiComponent,
        val properties: Map<String, Any?>,
        val initialData: Any,
        val updatedData: Any? = null,
        val reportsError: Boolean = false,
    ) {
        override fun toString(): String = name
    }

    companion object {
        private const val DataKey = "data"
        private const val DataPath = "/$DataKey"
        private val DataBinding = mapOf("path" to DataPath)
        private val DynamicChecks =
            listOf(mapOf("condition" to DataBinding, "message" to "Invalid"))

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<TestCase> =
            listOf(
                TestCase(
                    name = "Text.text",
                    component = TextStub,
                    properties = mapOf("text" to DataBinding),
                    initialData = "Hello",
                ),
                TestCase(
                    name = "Image.url",
                    component = ImageStub,
                    properties = mapOf("url" to DataBinding),
                    initialData = "https://example.com/image.png",
                ),
                TestCase(
                    name = "Icon.name",
                    component = IconStub,
                    properties = mapOf("name" to DataBinding),
                    initialData = "home",
                ),
                TestCase(
                    name = "Video.url",
                    component = VideoStub,
                    properties = mapOf("url" to DataBinding),
                    initialData = "https://example.com/video.mp4",
                ),
                TestCase(
                    name = "AudioPlayer.url",
                    component = AudioPlayerStub,
                    properties = mapOf("url" to DataBinding),
                    initialData = "https://example.com/audio.mp3",
                ),
                TestCase(
                    name = "Row.children",
                    component = RowStub,
                    properties =
                        mapOf("children" to mapOf("path" to DataPath, "componentId" to "item")),
                    initialData = listOf("first"),
                ),
                TestCase(
                    name = "Column.children",
                    component = ColumnStub,
                    properties =
                        mapOf("children" to mapOf("path" to DataPath, "componentId" to "item")),
                    initialData = listOf("first"),
                ),
                TestCase(
                    name = "List.children",
                    component = ListStub,
                    properties =
                        mapOf("children" to mapOf("path" to DataPath, "componentId" to "item")),
                    initialData = listOf("first"),
                ),
                TestCase(
                    name = "Tabs.title",
                    component = TabsStub,
                    properties =
                        mapOf("tabs" to listOf(mapOf("title" to DataBinding, "child" to "tab"))),
                    initialData = "First tab",
                ),
                TestCase(
                    name = "Button.checks",
                    component = ButtonStub,
                    properties =
                        mapOf(
                            "child" to "label",
                            "action" to mapOf("event" to mapOf("name" to "submit")),
                            "checks" to DynamicChecks,
                        ),
                    initialData = true,
                ),
                TestCase(
                    name = "TextField.label",
                    component = TextFieldStub,
                    properties = mapOf("label" to DataBinding),
                    initialData = "Name",
                ),
                TestCase(
                    name = "TextField.checks",
                    component = TextFieldStub,
                    properties = mapOf("label" to "Name", "checks" to DynamicChecks),
                    initialData = true,
                ),
                TestCase(
                    name = "CheckBox.label",
                    component = CheckBoxStub,
                    properties = mapOf("label" to DataBinding, "value" to true),
                    initialData = "Accept",
                ),
                TestCase(
                    name = "CheckBox.value",
                    component = CheckBoxStub,
                    properties = mapOf("label" to "Accept", "value" to DataBinding),
                    initialData = true,
                ),
                TestCase(
                    name = "CheckBox.checks",
                    component = CheckBoxStub,
                    properties =
                        mapOf("label" to "Accept", "value" to true, "checks" to DynamicChecks),
                    initialData = true,
                ),
                TestCase(
                    name = "ChoicePicker.options",
                    component = ChoicePickerStub,
                    properties =
                        mapOf(
                            "options" to listOf(mapOf("label" to DataBinding, "value" to "a")),
                            "value" to listOf("a"),
                        ),
                    initialData = "Option A",
                ),
                TestCase(
                    name = "ChoicePicker.value",
                    component = ChoicePickerStub,
                    properties =
                        mapOf(
                            "options" to listOf(mapOf("label" to "Option A", "value" to "a")),
                            "value" to DataBinding,
                        ),
                    initialData = listOf("a"),
                ),
                TestCase(
                    name = "ChoicePicker.checks",
                    component = ChoicePickerStub,
                    properties =
                        mapOf(
                            "options" to listOf(mapOf("label" to "Option A", "value" to "a")),
                            "value" to listOf("a"),
                            "checks" to DynamicChecks,
                        ),
                    initialData = true,
                ),
                TestCase(
                    name = "Slider.value",
                    component = SliderStub,
                    properties = mapOf("max" to 100, "value" to DataBinding),
                    initialData = 42,
                ),
                TestCase(
                    name = "Slider.checks",
                    component = SliderStub,
                    properties = mapOf("max" to 100, "value" to 42, "checks" to DynamicChecks),
                    initialData = true,
                ),
                TestCase(
                    name = "DateTimeInput.value",
                    component = DateTimeInputStub,
                    properties = mapOf("value" to DataBinding),
                    initialData = "2026-03-24T15:25:00",
                ),
                TestCase(
                    name = "DateTimeInput.invalidValue",
                    component = DateTimeInputStub,
                    properties = mapOf("value" to DataBinding),
                    initialData = "2026-03-24T15:25:00",
                    updatedData = "not-a-date",
                    reportsError = true,
                ),
                TestCase(
                    name = "DateTimeInput.checks",
                    component = DateTimeInputStub,
                    properties = mapOf("value" to "2026-03-24T15:25:00", "checks" to DynamicChecks),
                    initialData = true,
                ),
            )
    }
}

private const val RenderedTag = "rendered"
private const val LoadingText = "Loading..."
private const val ErrorText = "Error"

/**
 * Mounts the root component of [surface] the way a host that animates state transitions does: the
 * content of the last [A2uiComponentState.Success] state stays composed after the component leaves
 * that state, next to the content of the new state, instead of being removed immediately.
 */
@Composable
private fun RetainingTestSurface(
    surface: A2uiSurfaceModel,
    catalog: A2uiCatalog,
    errorContent: @Composable (A2uiException) -> Unit,
) {
    val readinessEvaluator = remember(catalog) { catalog.asReadinessEvaluator() }
    CompositionLocalProvider(LocalA2uiReadinessEvaluator provides readinessEvaluator) {
        val state = observeA2uiComponentState(surface)
        val retained = remember { RetainedState() }
        when (state) {
            is A2uiComponentState.Success -> retained.lastSuccess = state
            is A2uiComponentState.Loading -> BasicText(LoadingText)
            is A2uiComponentState.Error -> errorContent(state.exception)
        }
        retained.lastSuccess?.let { A2uiComponent(component = it.component) }
    }
}

private class RetainedState {
    var lastSuccess: A2uiComponentState.Success? = null
}

/** Marks the component as rendered; every stub below renders this instead of real UI. */
@Composable
private fun Rendered(modifier: Modifier) {
    BasicText(text = "Rendered", modifier = modifier.testTag(RenderedTag))
}

private object TextStub : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object ImageStub : A2uiBasicCatalogV1.Image {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        fit: A2uiBasicCatalogV1.Image.Fit,
        variant: A2uiBasicCatalogV1.Image.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object IconStub : A2uiBasicCatalogV1.Icon {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        source: A2uiBasicCatalogV1.Icon.Source,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object VideoStub : A2uiBasicCatalogV1.Video {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object AudioPlayerStub : A2uiBasicCatalogV1.AudioPlayer {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object RowStub : A2uiBasicCatalogV1.Row {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Row.Justify,
        align: A2uiBasicCatalogV1.Row.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object ColumnStub : A2uiBasicCatalogV1.Column {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Column.Justify,
        align: A2uiBasicCatalogV1.Column.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object ListStub : A2uiBasicCatalogV1.List {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        direction: A2uiBasicCatalogV1.List.Direction,
        align: A2uiBasicCatalogV1.List.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object TabsStub : A2uiBasicCatalogV1.Tabs {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        tabs: List<A2uiBasicCatalogV1.Tabs.Tab>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object ButtonStub : A2uiBasicCatalogV1.Button {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object TextFieldStub : A2uiBasicCatalogV1.TextField {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        value: String?,
        variant: A2uiBasicCatalogV1.TextField.Variant,
        validationRegexp: String?,
        onValueChange: (String) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object CheckBoxStub : A2uiBasicCatalogV1.CheckBox {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object ChoicePickerStub : A2uiBasicCatalogV1.ChoicePicker {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        options: List<A2uiBasicCatalogV1.ChoicePicker.Option>,
        value: List<String>,
        variant: A2uiBasicCatalogV1.ChoicePicker.Variant,
        displayStyle: A2uiBasicCatalogV1.ChoicePicker.DisplayStyle,
        filterable: Boolean,
        onValueChange: (List<String>) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object SliderStub : A2uiBasicCatalogV1.Slider {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        min: Float,
        max: Float,
        value: Float,
        onValueChange: (Float) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}

private object DateTimeInputStub : A2uiBasicCatalogV1.DateTimeInput {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        value: Long?,
        onValueChange: ((Long?) -> Unit)?,
        enableDate: Boolean,
        enableTime: Boolean,
        min: Long?,
        max: Long?,
        label: String?,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) = Rendered(modifier)
}
