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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.privacysandbox.ui.client.view.SharedUiContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@RunWith(Enclosed::class)
@LargeTest
class SharedUiContainerTest() {
    private companion object {
        const val PARENT_WIDTH_PIXELS = 200
        const val PARENT_HEIGHT_PIXELS = 200
        const val CONTAINER_WIDTH_PIXELS = 100
        const val CONTAINER_HEIGHT_PIXELS = 100
        const val CHILD_WIDTH_PIXELS = 100
        const val CHILD_HEIGHT_PIXELS = 100
        const val PADDING_PIXELS = 10
        const val MARGIN_PIXELS = 1

        fun createMarginLayoutParams(
            widthInPixels: Int,
            heightInPixels: Int,
            marginInPixels: Int = MARGIN_PIXELS
        ): MarginLayoutParams =
            MarginLayoutParams(widthInPixels, heightInPixels).apply { setMargins(marginInPixels) }
    }

    @RunWith(Parameterized::class)
    @LargeTest
    class ParentMeasureSpecTest(private val sharedUiContainerLayoutParams: LayoutParams) {
        private lateinit var context: Context
        private lateinit var sharedUiContainer: SharedUiContainer
        private lateinit var childView: View
        private var widthMeasureSpec: Int = 0
        private var heightMeasureSpec: Int = 0

        @get:Rule var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "sharedUiContainerLayoutParams={0}")
            fun data(): Array<Any> =
                arrayOf(
                    createMarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
                    createMarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                    createMarginLayoutParams(PARENT_WIDTH_PIXELS - 10, PARENT_HEIGHT_PIXELS - 10),
                    createMarginLayoutParams(PARENT_WIDTH_PIXELS + 10, PARENT_HEIGHT_PIXELS + 10)
                )
        }

        @Before
        fun setup() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
            activityScenarioRule.withActivity {
                sharedUiContainer = SharedUiContainer(this).apply { setPadding(PADDING_PIXELS) }

                childView =
                    View(context).apply {
                        layoutParams = LayoutParams(CHILD_WIDTH_PIXELS, CHILD_HEIGHT_PIXELS)
                    }
            }
        }

        @Test
        fun onMeasure_measureSpecAtMost_withinParentBounds() {
            activityScenarioRule.withActivity {
                sharedUiContainer.layoutParams = sharedUiContainerLayoutParams
            }
            widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_WIDTH_PIXELS, View.MeasureSpec.AT_MOST)
            heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_HEIGHT_PIXELS, View.MeasureSpec.AT_MOST)

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(sharedUiContainer.measuredWidth).isAtMost(PARENT_WIDTH_PIXELS)
            assertThat(sharedUiContainer.measuredHeight).isAtMost(PARENT_HEIGHT_PIXELS)
        }

        @Test
        fun onMeasure_measureSpecExactly_fitsParentBounds() {
            activityScenarioRule.withActivity {
                sharedUiContainer.layoutParams = sharedUiContainerLayoutParams
            }
            widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_WIDTH_PIXELS, View.MeasureSpec.EXACTLY)
            heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_HEIGHT_PIXELS, View.MeasureSpec.EXACTLY)

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(sharedUiContainer.measuredWidth).isEqualTo(PARENT_WIDTH_PIXELS)
            assertThat(sharedUiContainer.measuredHeight).isEqualTo(PARENT_HEIGHT_PIXELS)
        }

        @Test
        fun onMeasure_measureSpecUnspecified_withoutChildren_minimumSize() {
            activityScenarioRule.withActivity {
                sharedUiContainer.layoutParams = sharedUiContainerLayoutParams
            }
            widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_WIDTH_PIXELS, View.MeasureSpec.UNSPECIFIED)
            heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_HEIGHT_PIXELS, View.MeasureSpec.UNSPECIFIED)

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(sharedUiContainer.measuredWidth)
                .isEqualTo(sharedUiContainer.paddingLeft + sharedUiContainer.paddingRight)
            assertThat(sharedUiContainer.measuredHeight)
                .isEqualTo(sharedUiContainer.paddingTop + sharedUiContainer.paddingBottom)
        }

        @Test
        fun onMeasure_measureSpecUnspecified_withChildren_fitsChildPlusPadding() {
            activityScenarioRule.withActivity {
                sharedUiContainer.apply {
                    layoutParams = sharedUiContainerLayoutParams
                    addView(childView)
                }
            }
            widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_WIDTH_PIXELS, View.MeasureSpec.UNSPECIFIED)
            heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_HEIGHT_PIXELS, View.MeasureSpec.UNSPECIFIED)

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(sharedUiContainer.measuredWidth)
                .isEqualTo(
                    CHILD_WIDTH_PIXELS +
                        sharedUiContainer.paddingLeft +
                        sharedUiContainer.paddingRight
                )
            assertThat(sharedUiContainer.measuredHeight)
                .isEqualTo(
                    CHILD_HEIGHT_PIXELS +
                        sharedUiContainer.paddingTop +
                        sharedUiContainer.paddingBottom
                )
        }
    }

    @RunWith(AndroidJUnit4::class)
    @LargeTest
    class ChildrenMeasureAndLayoutTest {
        private lateinit var context: Context
        private lateinit var sharedUiContainer: SharedUiContainer
        private lateinit var childView: View
        private var widthMeasureSpec: Int = 0
        private var heightMeasureSpec: Int = 0

        @get:Rule var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

        @Before
        fun setup() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
            widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_WIDTH_PIXELS, View.MeasureSpec.AT_MOST)
            heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PARENT_HEIGHT_PIXELS, View.MeasureSpec.AT_MOST)
            activityScenarioRule.withActivity {
                // Add padding and margins to the container to make sure children are measured and
                // laid out correctly
                // for such cases.
                sharedUiContainer =
                    SharedUiContainer(this).apply {
                        layoutParams =
                            createMarginLayoutParams(
                                CONTAINER_WIDTH_PIXELS,
                                CONTAINER_HEIGHT_PIXELS
                            )
                        setPadding(PADDING_PIXELS)
                    }

                childView =
                    View(context).apply {
                        layoutParams = LayoutParams(CHILD_WIDTH_PIXELS, CHILD_HEIGHT_PIXELS)
                    }
            }
        }

        @Test
        fun onMeasure_containerWrapContent_childrenFixedSize_containerFitsBiggestChildPlusPadding() {
            activityScenarioRule.withActivity {
                sharedUiContainer.apply {
                    layoutParams =
                        createMarginLayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT
                        )
                    addView(
                        View(context).apply {
                            layoutParams = LayoutParams(CHILD_WIDTH_PIXELS, CHILD_HEIGHT_PIXELS)
                        }
                    )
                    addView(
                        View(context).apply {
                            layoutParams =
                                LayoutParams(CHILD_WIDTH_PIXELS + 10, CHILD_HEIGHT_PIXELS + 10)
                        }
                    )
                }
            }

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(sharedUiContainer.measuredWidth)
                .isEqualTo(
                    CHILD_WIDTH_PIXELS +
                        10 +
                        sharedUiContainer.paddingLeft +
                        sharedUiContainer.paddingRight
                )
            assertThat(sharedUiContainer.measuredHeight)
                .isEqualTo(
                    CHILD_HEIGHT_PIXELS +
                        10 +
                        sharedUiContainer.paddingTop +
                        sharedUiContainer.paddingBottom
                )
        }

        @Test
        fun onMeasure_containerFixedSize_childSmaller_childGetsRequestedSize() {
            activityScenarioRule.withActivity {
                childView.layoutParams =
                    LayoutParams(CONTAINER_WIDTH_PIXELS - 10, CONTAINER_HEIGHT_PIXELS - 10)
                sharedUiContainer.apply {
                    layoutParams =
                        createMarginLayoutParams(CONTAINER_WIDTH_PIXELS, CONTAINER_HEIGHT_PIXELS)
                    addView(childView)
                }
            }

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(childView.measuredWidth).isEqualTo(CONTAINER_WIDTH_PIXELS - 10)
            assertThat(childView.measuredHeight).isEqualTo(CONTAINER_HEIGHT_PIXELS - 10)
        }

        @Test
        fun onMeasure_containerFixedSize_childBigger_childGetsRequestedSize() {
            activityScenarioRule.withActivity {
                childView.layoutParams =
                    LayoutParams(CONTAINER_WIDTH_PIXELS + 10, CONTAINER_HEIGHT_PIXELS + 10)
                sharedUiContainer.apply {
                    layoutParams =
                        createMarginLayoutParams(CONTAINER_WIDTH_PIXELS, CONTAINER_HEIGHT_PIXELS)
                    addView(childView)
                }
            }

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(childView.measuredWidth).isEqualTo(CONTAINER_WIDTH_PIXELS + 10)
            assertThat(childView.measuredHeight).isEqualTo(CONTAINER_HEIGHT_PIXELS + 10)
        }

        @Test
        fun onMeasure_containerFixedSize_childMatchParent_childGetsParentSizeMinusPadding() {
            activityScenarioRule.withActivity {
                childView.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                sharedUiContainer.apply {
                    layoutParams =
                        createMarginLayoutParams(CONTAINER_WIDTH_PIXELS, CONTAINER_HEIGHT_PIXELS)
                    addView(childView)
                }
            }

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(childView.measuredWidth)
                .isEqualTo(
                    sharedUiContainer.measuredWidth -
                        (sharedUiContainer.paddingLeft + sharedUiContainer.paddingRight)
                )
            assertThat(childView.measuredHeight)
                .isEqualTo(
                    sharedUiContainer.measuredHeight -
                        (sharedUiContainer.paddingTop + sharedUiContainer.paddingBottom)
                )
        }

        @Test
        fun onMeasure_childVisibilityGone_childNotMeasured() {
            activityScenarioRule.withActivity {
                childView.visibility = View.GONE
                sharedUiContainer.addView(childView)
            }

            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            assertThat(childView.measuredWidth).isEqualTo(0)
            assertThat(childView.measuredHeight).isEqualTo(0)
        }

        @Test
        fun onLayout_childrenInTopLeftCorner() {
            lateinit var childView1: View
            lateinit var childView2: View
            activityScenarioRule.withActivity {
                childView1 =
                    View(context).apply {
                        layoutParams = LayoutParams(CHILD_WIDTH_PIXELS, CHILD_HEIGHT_PIXELS)
                        visibility = View.VISIBLE
                    }
                childView2 =
                    View(context).apply {
                        layoutParams =
                            LayoutParams(CONTAINER_WIDTH_PIXELS + 10, CONTAINER_HEIGHT_PIXELS + 10)
                        visibility = View.INVISIBLE
                    }
                sharedUiContainer.addView(childView1)
                sharedUiContainer.addView(childView2)
            }
            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            sharedUiContainer.layout(
                0,
                0,
                sharedUiContainer.measuredWidth,
                sharedUiContainer.measuredHeight
            )

            assertLayoutCoordinates(
                childView1,
                PADDING_PIXELS,
                PADDING_PIXELS,
                CHILD_WIDTH_PIXELS + sharedUiContainer.paddingLeft,
                CHILD_HEIGHT_PIXELS + sharedUiContainer.paddingTop
            )
            assertLayoutCoordinates(
                childView2,
                PADDING_PIXELS,
                PADDING_PIXELS,
                CONTAINER_WIDTH_PIXELS + 10 + sharedUiContainer.paddingLeft,
                CONTAINER_HEIGHT_PIXELS + 10 + sharedUiContainer.paddingTop
            )
        }

        @Test
        fun onLayout_childVisibilityGone_childNotLaidOut() {
            activityScenarioRule.withActivity {
                childView.visibility = View.GONE
                sharedUiContainer.addView(childView)
            }
            sharedUiContainer.measure(widthMeasureSpec, heightMeasureSpec)

            sharedUiContainer.layout(
                0,
                0,
                sharedUiContainer.measuredWidth,
                sharedUiContainer.measuredHeight
            )

            assertLayoutCoordinates(childView, 0, 0, 0, 0)
        }

        private fun assertLayoutCoordinates(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {
            assertThat(view.left).isEqualTo(left)
            assertThat(view.top).isEqualTo(top)
            assertThat(view.right).isEqualTo(right)
            assertThat(view.bottom).isEqualTo(bottom)
        }
    }
}
