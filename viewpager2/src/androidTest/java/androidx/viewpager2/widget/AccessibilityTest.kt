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

import android.os.Build
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
@LargeTest
class AccessibilityTest(private val config: TestConfig) : BaseTest() {
    private val enhancedA11yEnabled = ViewPager2.sFeatureEnhancedA11yEnabled

    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean
    )

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
    }

    override fun tearDown() {
        super.tearDown()
        ViewPager2.sFeatureEnhancedA11yEnabled = enhancedA11yEnabled
    }

    @Test
    @SdkSuppress(minSdkVersion = 16)
    fun test_onPerformPageAction() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(6)))

            val initialPage = viewPager.currentItem
            assertBasicState(initialPage)

            listOf(1, 2, 3, 2, 3, 2, 3, 4, 5, 4, 5, 4, 3, 2, 1, 0, 1).forEach {
                    targetPage ->
                val currentPage = viewPager.currentItem
                val latch = viewPager.addWaitForScrolledLatch(targetPage)
                runOnUiThreadSync {
                    if (targetPage - currentPage == 1) {
                        ViewCompat.performAccessibilityAction(viewPager,
                            getNextPageAction(config.orientation, viewPager.isRtl), null)
                    } else {
                        ViewCompat.performAccessibilityAction(viewPager,
                            getPreviousPageAction(config.orientation, viewPager.isRtl), null)
                    }
                }
                latch.await(2, TimeUnit.SECONDS)
                assertBasicState(targetPage)
            }
        }
    }

    @Test
    fun test_collectionInfo() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(6)))

            val initialPage = viewPager.currentItem
            assertBasicState(initialPage)

            var node = AccessibilityNodeInfoCompat.obtain()
            runOnUiThreadSync {
                ViewCompat.onInitializeAccessibilityNodeInfo(viewPager, node)
            }
            var collectionInfo = node.collectionInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (config.orientation == ORIENTATION_VERTICAL) {
                    assertThat(collectionInfo.rowCount, equalTo(6))
                    assertThat(collectionInfo.columnCount, equalTo(0))
                } else {
                    assertThat(collectionInfo.columnCount, equalTo(6))
                    assertThat(collectionInfo.rowCount, equalTo(0))
                }
                assertThat(collectionInfo.isHierarchical, equalTo(false))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    assertThat(collectionInfo.selectionMode, equalTo(0))
                }
            } else {
                assertNull(collectionInfo)
            }
        }
    }

    @Test
    fun test_onOrientationChange() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(2)))

            val initialPage = viewPager.currentItem
            assertBasicState(initialPage)

            runOnUiThreadSync {
                viewPager.setOrientation(getOppositeOrientation(config.orientation))
            }
            assertBasicState(initialPage)
        }
    }

    private fun getNextPageAction(orientation: Int, isRtl: Boolean): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (isRtl) {
                    return ACTION_ID_PAGE_LEFT
                } else {
                    return ACTION_ID_PAGE_RIGHT
                }
            }
            return ACTION_ID_PAGE_DOWN
        }
        return AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
    }

    private fun getPreviousPageAction(orientation: Int, isRtl: Boolean): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (isRtl) {
                    return ACTION_ID_PAGE_RIGHT
                } else {
                    return ACTION_ID_PAGE_LEFT
                }
            }
            return ACTION_ID_PAGE_UP
        }
        return AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
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
        listOf(true, false).map { rtl ->
            AccessibilityTest.TestConfig(orientation, rtl)
        }
    }
}

// endregion
