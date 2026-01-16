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

package androidx.wear.compose.material3

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
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
class TransformingLazyColumnScreenshotTest(
    @TestParameter val screenSize: ScreenSize,
    @TestParameter val component: ComponentType,
    @TestParameter val isAnimated: IsAnimated,
    @TestParameter val isReverseLayout: IsReverseLayout,
) {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun transforming_lazy_column_initial_layout() = verifyTransformingLazyColumnScreenshot()

    @Test
    fun transforming_lazy_column_initial_layout_fits_screen() =
        verifyTransformingLazyColumnScreenshot(itemsCount = 1)

    @Test
    fun transforming_lazy_column_single_item_with_padding() =
        verifyTransformingLazyColumnScreenshot(
            itemsCount = 1,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 48.dp),
        )

    @Test
    fun transforming_lazy_column_multiple_items_with_padding() =
        verifyTransformingLazyColumnScreenshot(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 48.dp)
        )

    @Test
    fun transforming_lazy_column_scrollBy() = verifyTransformingLazyColumnScreenshot {
        scrollBy(100f)
    }

    @Test
    fun transforming_lazy_column_scrollTo() = verifyTransformingLazyColumnScreenshot {
        scrollToItem(50)
    }

    @Test
    fun transforming_lazy_column_overscroll() = verifyTransformingLazyColumnScreenshot {
        scrollBy(100f)
        scrollBy(-200f)
    }

    private val ARRANGEMENT_TEST_ITEMS = 3 // Only 4 items fit in the screen

    @Test
    fun transforming_lazy_column_center_arrangement() =
        verifyTransformingLazyColumnScreenshot(
            itemsCount = ARRANGEMENT_TEST_ITEMS,
            verticalArrangement = Arrangement.Center,
        )

    @Test
    fun transforming_lazy_column_bottom_arrangement() =
        verifyTransformingLazyColumnScreenshot(
            itemsCount = ARRANGEMENT_TEST_ITEMS,
            verticalArrangement = Arrangement.Bottom,
        )

    @Test
    fun transforming_lazy_column_center_arrangement_with_spaced_by() =
        verifyTransformingLazyColumnScreenshot(
            itemsCount = ARRANGEMENT_TEST_ITEMS,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        )

    @Test
    fun transforming_lazy_column_bottom_arrangement_with_spaced_by() =
        verifyTransformingLazyColumnScreenshot(
            itemsCount = ARRANGEMENT_TEST_ITEMS,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
        )

    enum class ComponentType {
        BUTTON,
        CARD,
        BORDERED_BUTTON,
        FULL_WIDTH_BUTTON,
    }

    enum class IsAnimated {
        ANIMATED,
        NOT_ANIMATED,
    }

    enum class IsReverseLayout {
        REVERSE_LAYOUT,
        NOT_REVERSE_LAYOUT,
    }

    data class TestContext(
        val transformationSpec: TransformationSpec,
        val isAnimated: Boolean,
        val itemsCount: Int,
    ) {
        fun Component(type: ComponentType, scope: TransformingLazyColumnScope) {
            when (type) {
                ComponentType.BUTTON -> Buttons(scope)
                ComponentType.CARD -> Cards(scope)
                ComponentType.BORDERED_BUTTON -> BorderedButtons(scope)
                ComponentType.FULL_WIDTH_BUTTON -> Buttons(scope, Modifier.fillMaxWidth())
            }
        }

        private fun Buttons(scope: TransformingLazyColumnScope, modifier: Modifier = Modifier) =
            with(scope) {
                items(count = itemsCount) {
                    Button(
                        onClick = {},
                        modifier =
                            modifier
                                .transformedHeight(this, transformationSpec)
                                .then(if (isAnimated) Modifier.animateItem() else Modifier),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Button $it")
                    }
                }
            }

        private fun BorderedButtons(scope: TransformingLazyColumnScope) =
            with(scope) {
                items(count = itemsCount) {
                    OutlinedButton(
                        onClick = {},
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec)
                                .then(if (isAnimated) Modifier.animateItem() else Modifier),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Button $it")
                    }
                }
            }

        private fun Cards(scope: TransformingLazyColumnScope) =
            with(scope) {
                items(count = itemsCount) {
                    Card(
                        onClick = {},
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec)
                                .then(if (isAnimated) Modifier.animateItem() else Modifier),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $it")
                    }
                }
            }
    }

    private fun verifyTransformingLazyColumnScreenshot(
        itemsCount: Int = 100,
        contentPadding: PaddingValues = PaddingValues(),
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(
                4.dp,
                alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom,
            ),
        onIdle: suspend TransformingLazyColumnState.() -> Unit = {},
    ) {
        lateinit var state: TransformingLazyColumnState
        lateinit var coroutineScope: CoroutineScope
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                val transformationSpec = rememberTransformationSpec()
                state = rememberTransformingLazyColumnState()
                coroutineScope = rememberCoroutineScope()

                TransformingLazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                    modifier = Modifier.testTag(TEST_TAG),
                    reverseLayout = reverseLayout,
                ) {
                    with(
                        TestContext(
                            transformationSpec,
                            isAnimated = isAnimated == IsAnimated.ANIMATED,
                            itemsCount = itemsCount,
                        )
                    ) {
                        Component(component, this@TransformingLazyColumn)
                    }
                }
            }
        }

        rule.waitForIdle()
        coroutineScope.launch(Dispatchers.Main) { onIdle(state) }

        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    private val reverseLayout
        get() = isReverseLayout == IsReverseLayout.REVERSE_LAYOUT
}
