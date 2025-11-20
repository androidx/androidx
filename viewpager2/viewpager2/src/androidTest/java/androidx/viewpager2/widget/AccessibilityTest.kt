/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class AccessibilityTest(private val config: TestConfig) : BaseTest() {
    private val enhancedA11yEnabled = ViewPager2.sFeatureEnhancedA11yEnabled
    private var uiDevice: Any? = null

    data class TestConfig(@ViewPager2.Orientation val orientation: Int, val rtl: Boolean)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    override fun setUp() {
        ViewPager2.sFeatureEnhancedA11yEnabled = true
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }
        // Make sure accessibility is enabled (side effect of creating a UI Automator instance)
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    override fun tearDown() {
        super.tearDown()
        ViewPager2.sFeatureEnhancedA11yEnabled = enhancedA11yEnabled
        uiDevice = null
    }

    @Test
    fun test_onPerformPageAction() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider.provider(stringSequence(6)))
            val initialPage = viewPager.currentItem
            assertBasicState(initialPage)

            listOf(1, 2, 3, 2, 3, 2, 3, 4, 5, 4, 5, 4, 3, 2, 1, 0, 1).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                val latch = viewPager.addWaitForScrolledLatch(targetPage)
                runOnUiThreadSync {
                    if (targetPage - currentPage == 1) {
                        viewPager.performAccessibilityAction(
                            getNextPageAction(config.orientation, viewPager.isRtl),
                            null,
                        )
                    } else {
                        viewPager.performAccessibilityAction(
                            getPreviousPageAction(config.orientation, viewPager.isRtl),
                            null,
                        )
                    }
                }
                latch.await(2, TimeUnit.SECONDS)
                assertBasicState(targetPage)
            }
        }
    }

    @Test
    fun test_collectionInfo() {
        test_collectionInfo(6)
    }

    @Test
    fun test_collectionInfo_zeroItems() {
        test_collectionInfo(0)
    }

    @Suppress("DEPRECATION") // AccessibilityNodeInfo.obtain()
    private fun test_collectionInfo(numberOfItems: Int) {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider.provider(stringSequence(numberOfItems)))
            assertBasicState(viewPager.currentItem, null)

            var node = AccessibilityNodeInfo.obtain()
            runOnUiThreadSync { viewPager.onInitializeAccessibilityNodeInfo(node) }
            var collectionInfo = node.collectionInfo
            if (config.orientation == ORIENTATION_VERTICAL) {
                assertThat(collectionInfo.rowCount, equalTo(numberOfItems))
                assertThat(collectionInfo.columnCount, equalTo(1))
            } else {
                assertThat(collectionInfo.columnCount, equalTo(numberOfItems))
                assertThat(collectionInfo.rowCount, equalTo(1))
            }
            assertThat(collectionInfo.isHierarchical, equalTo(false))
            assertThat(collectionInfo.selectionMode, equalTo(0))
        }
    }

    @Suppress("DEPRECATION") // AccessibilityNodeInfo.obtain()
    @Test
    fun test_collectionItemInfo() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider.provider(stringSequence(6)))
            listOf(1, 0, 2, 5).forEach { targetPage ->
                viewPager.setCurrentItemSync(targetPage, false, 2, TimeUnit.SECONDS)
                assertBasicState(targetPage)
                var nodeChild = AccessibilityNodeInfo.obtain()
                val item = viewPager.linearLayoutManager.findViewByPosition(targetPage)
                runOnUiThreadSync { item!!.onInitializeAccessibilityNodeInfo(nodeChild) }
                var collectionItemInfo = nodeChild.collectionItemInfo
                if (config.orientation == ORIENTATION_VERTICAL) {
                    assertThat(collectionItemInfo.rowIndex, equalTo(targetPage))
                    assertThat(collectionItemInfo.columnIndex, equalTo(0))
                } else {
                    assertThat(collectionItemInfo.rowIndex, equalTo(0))
                    assertThat(collectionItemInfo.columnIndex, equalTo(targetPage))
                }
            }
        }
    }

    @Test
    fun test_onOrientationChange() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider.provider(stringSequence(2)))

            val initialPage = viewPager.currentItem
            assertBasicState(initialPage)

            runOnUiThreadSync {
                viewPager.setOrientation(getOppositeOrientation(config.orientation))
            }
            assertBasicState(initialPage)
        }
    }

    private fun getNextPageAction(orientation: Int, isRtl: Boolean): Int {
        if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            if (isRtl) {
                return ACTION_ID_PAGE_LEFT
            } else {
                return ACTION_ID_PAGE_RIGHT
            }
        }
        return ACTION_ID_PAGE_DOWN
    }

    private fun getPreviousPageAction(orientation: Int, isRtl: Boolean): Int {
        if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            if (isRtl) {
                return ACTION_ID_PAGE_RIGHT
            } else {
                return ACTION_ID_PAGE_LEFT
            }
        }
        return ACTION_ID_PAGE_UP
    }

    private fun getOppositeOrientation(orientation: Int): Int {
        if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
            return ViewPager2.ORIENTATION_VERTICAL
        } else {
            return ViewPager2.ORIENTATION_HORIZONTAL
        }
    }
}

// region Test Suite creation

private fun createTestSet(): List<AccessibilityTest.TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).map { rtl -> AccessibilityTest.TestConfig(orientation, rtl) }
    }
}

// endregion
