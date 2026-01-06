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

package androidx.wear.compose.material3.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SCREENSHOT_GOLDEN_PATH
import androidx.wear.compose.material3.ScreenConfiguration
import androidx.wear.compose.material3.ScreenSize
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.TEST_TAG
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.setContentWithTheme
import androidx.wear.compose.material3.verifyScreenshot
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ResponsiveTransformingLazyColumnScreenshotTest(
    @TestParameter val screenSize: ScreenSize,
    @TestParameter val component: ComponentType,
) {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun responsive_transforming_lazy_column_initial_layout() =
        verifyResponsiveTransformingLazyColumnScreenshot()

    @Test
    fun responsive_transforming_lazy_column_reverse_layout() =
        verifyResponsiveTransformingLazyColumnScreenshot(reverseLayout = true)

    @Test
    fun responsive_transforming_lazy_column_content_padding_merges() =
        verifyResponsiveTransformingLazyColumnScreenshot(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 50.dp)
        )

    // Use Semantic types to verify responsive padding logic (e.g. 13% vs 23%)
    enum class ComponentType(val itemType: ResponsiveItemType) {
        BUTTON(ResponsiveItemType.Button), // 23%
        CARD(ResponsiveItemType.Card), // 23%
        LIST_HEADER(ResponsiveItemType.ListHeader), // 13% top, 23% bottom
        COMPACT_BUTTON(ResponsiveItemType.CompactButton), // 13%
    }

    data class TestContext(val transformationSpec: TransformationSpec, val itemsCount: Int) {
        fun Component(type: ComponentType, scope: ResponsiveTransformingLazyColumnScope) {
            when (type) {
                ComponentType.BUTTON -> Buttons(scope, type.itemType)
                ComponentType.CARD -> Cards(scope, type.itemType)
                ComponentType.LIST_HEADER -> Headers(scope, type.itemType)
                ComponentType.COMPACT_BUTTON -> CompactButtons(scope, type.itemType)
            }
        }

        private fun Buttons(
            scope: ResponsiveTransformingLazyColumnScope,
            itemType: ResponsiveItemType,
            modifier: Modifier = Modifier,
        ) =
            with(scope) {
                items(count = itemsCount, itemType = { itemType }) {
                    Button(
                        onClick = {},
                        modifier = modifier.transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Button $it")
                    }
                }
            }

        private fun Cards(
            scope: ResponsiveTransformingLazyColumnScope,
            itemType: ResponsiveItemType,
        ) =
            with(scope) {
                items(count = itemsCount, itemType = { itemType }) {
                    Card(
                        onClick = {},
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $it")
                    }
                }
            }

        private fun Headers(
            scope: ResponsiveTransformingLazyColumnScope,
            itemType: ResponsiveItemType,
        ) =
            with(scope) {
                items(count = itemsCount, itemType = { itemType }) {
                    ListHeader(modifier = Modifier.transformedHeight(this, transformationSpec)) {
                        Text("Header $it")
                    }
                }
            }

        private fun CompactButtons(
            scope: ResponsiveTransformingLazyColumnScope,
            itemType: ResponsiveItemType,
        ) =
            with(scope) {
                items(count = itemsCount, itemType = { itemType }) {
                    CompactButton(
                        onClick = {},
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Btn $it")
                    }
                }
            }
    }

    private fun verifyResponsiveTransformingLazyColumnScreenshot(
        itemsCount: Int = 10,
        contentPadding: PaddingValues = PaddingValues(),
        reverseLayout: Boolean = false,
        onIdle: suspend TransformingLazyColumnState.() -> Unit = {},
    ) {
        lateinit var state: TransformingLazyColumnState
        lateinit var coroutineScope: CoroutineScope
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                val transformationSpec = rememberTransformationSpec()
                state = rememberTransformingLazyColumnState()
                coroutineScope = rememberCoroutineScope()

                ResponsiveTransformingLazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp,
                            alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom,
                        ),
                    modifier = Modifier.testTag(TEST_TAG),
                    reverseLayout = reverseLayout,
                ) {
                    with(TestContext(transformationSpec, itemsCount = itemsCount)) {
                        Component(component, this@ResponsiveTransformingLazyColumn)
                    }
                }
            }
        }

        rule.waitForIdle()
        coroutineScope.launch(Dispatchers.Main) { onIdle(state) }

        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }
}
