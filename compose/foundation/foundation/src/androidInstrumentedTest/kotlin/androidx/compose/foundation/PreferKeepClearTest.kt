/*
 * Copyright 2023 The Android Open Source Project
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

import android.graphics.Rect as AndroidRect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.forEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testing the support for Android Views in Compose UI.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PreferKeepClearTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    /**
     * Make sure that when an rect using the bounds of a layout is used, the
     * bounds should be marked as prefer keep clear.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun preferClearBounds() {
        val composeView = setComposeContent {
            Box(Modifier.size(50.dp).preferKeepClear())
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(1)

            val expectedRect = AndroidRect(0, 0, composeView.width, composeView.height)
            assertThat(composeView.preferKeepClearRects[0]).isEqualTo(expectedRect)
        }
    }

    /**
     * Make sure that when an area using a supplied rect, the
     * rect should be marked as prefer keep clear.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun preferClearWithRect() {
        val composeView = setComposeContent {
            Box(Modifier.size(50.dp).preferKeepClear {
                Rect(0f, 0f, 10f, 20f)
            })
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(1)

            val expectedRect = AndroidRect(0, 0, 10, 20)
            assertThat(composeView.preferKeepClearRects[0]).isEqualTo(expectedRect)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun removeWhenModifierRemovedBounds() {
        var setPreferClear by mutableStateOf(true)
        val composeView = setComposeContent {
            val modifier = if (setPreferClear) Modifier.preferKeepClear() else Modifier
            Box(Modifier.size(50.dp).then(modifier))
        }
        rule.runOnUiThread {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(1)

            val expectedRect = AndroidRect(0, 0, composeView.width, composeView.height)
            assertThat(composeView.preferKeepClearRects[0]).isEqualTo(expectedRect)
        }
        setPreferClear = false
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isEmpty()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun removeWhenModifierRemovedRect() {
        var setPreferClear by mutableStateOf(true)
        val composeView = setComposeContent {
            val modifier = if (setPreferClear) Modifier.preferKeepClear {
                Rect(0f, 0f, 10f, 20f)
            } else Modifier
            Box(Modifier.size(50.dp).then(modifier))
        }
        rule.runOnUiThread {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(1)

            val expectedRect = AndroidRect(0, 0, 10, 20)
            assertThat(composeView.preferKeepClearRects[0]).isEqualTo(expectedRect)
        }
        setPreferClear = false
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isEmpty()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun markPreferClearMultipleBounds() {
        val composeView = setComposeContent {
            Column(Modifier.wrapContentSize()) {
                Box(Modifier.size(50.dp).preferKeepClear())
                Box(Modifier.size(50.dp).preferKeepClear())
            }
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(2)
            val first = composeView.preferKeepClearRects[0]
            val second = composeView.preferKeepClearRects[1]
            // we don't really care about the order
            val topBox: AndroidRect
            val bottomBox: AndroidRect
            if (first.top == 0) {
                topBox = first
                bottomBox = second
            } else {
                topBox = second
                bottomBox = first
            }
            assertThat(topBox.top).isEqualTo(0)
            assertThat(topBox.left).isEqualTo(0)
            assertThat(topBox.right).isEqualTo(composeView.width)
            assertThat(topBox.bottom).isEqualTo(bottomBox.top)
            assertThat(bottomBox.left).isEqualTo(0)
            assertThat(bottomBox.right).isEqualTo(composeView.width)
            assertThat(bottomBox.bottom).isEqualTo(composeView.height)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun markPreferClearMultipleBoundsSingleComposable() {
        val composeView = setComposeContent {
            Column(Modifier.wrapContentSize()) {
                Box(Modifier.size(100.dp)
                    .preferKeepClear { Rect(0f, 0f, 100f, 50f) }
                    .preferKeepClear { Rect(0f, 50f, 100f, 100f) }
                )
            }
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).isNotNull()
            assertThat(composeView.preferKeepClearRects).hasSize(2)
            val first = composeView.preferKeepClearRects[0]
            val second = composeView.preferKeepClearRects[1]
            // we don't really care about the order
            val topBox: AndroidRect
            val bottomBox: AndroidRect
            if (first.top == 0) {
                topBox = first
                bottomBox = second
            } else {
                topBox = second
                bottomBox = first
            }
            assertThat(topBox.top).isEqualTo(0)
            assertThat(topBox.left).isEqualTo(0)
            assertThat(topBox.right).isEqualTo(100)
            assertThat(topBox.bottom).isEqualTo(50)
            assertThat(bottomBox.left).isEqualTo(0)
            assertThat(bottomBox.right).isEqualTo(100)
            assertThat(bottomBox.bottom).isEqualTo(100)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun removeOneOfMultiple() {
        var setPreferClear by mutableStateOf(true)
        val composeView = setComposeContent {
            Column(Modifier.wrapContentSize()) {
                val modifier = if (setPreferClear) Modifier.preferKeepClear() else Modifier
                Box(Modifier.size(50.dp).then(modifier))
                Box(Modifier.size(50.dp).preferKeepClear())
            }
        }
        rule.runOnIdle {
            setPreferClear = false
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).hasSize(1)
            val rect = composeView.preferKeepClearRects[0]
            assertThat(rect.bottom).isEqualTo(composeView.height)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun replaceWithEmptyRect() {
        var useEmpty by mutableStateOf(false)
        val composeView = setComposeContent {
            Column(Modifier.wrapContentSize()) {
                val lambda: (LayoutCoordinates) -> Rect = if (useEmpty) { _ ->
                    Rect.Zero
                } else { _ ->
                    Rect(0f, 0f, 10f, 10f)
                }
                Box(Modifier.size(50.dp).preferKeepClear(lambda))
                Box(Modifier.size(50.dp).preferKeepClear())
            }
        }
        rule.runOnIdle {
            useEmpty = true
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).hasSize(1)
            val rect = composeView.preferKeepClearRects[0]
            assertThat(rect.bottom).isEqualTo(composeView.height)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun replaceWithAnotherRect() {
        var useFirst by mutableStateOf(true)
        val composeView = setComposeContent {
            Column(Modifier.wrapContentSize()) {
                val lambda: (LayoutCoordinates) -> Rect = if (useFirst) { _ ->
                    Rect(0f, 10f, 10f, 5f)
                } else { _ ->
                    Rect(0f, 0f, 10f, 10f)
                }
                Box(Modifier.size(50.dp).preferKeepClear(lambda))
            }
        }
        rule.runOnIdle {
            useFirst = false
        }
        rule.runOnIdle {
            assertThat(composeView.preferKeepClearRects).hasSize(1)
            val rect = composeView.preferKeepClearRects[0]
            assertThat(rect.bottom).isEqualTo(10)
        }
    }

    private fun setComposeContent(content: @Composable () -> Unit): View {
        rule.setContent(content)
        return findAndroidComposeView(rule.activity.window.decorView as ViewGroup)!!
    }

    private fun findAndroidComposeView(viewGroup: ViewGroup): View? {
        viewGroup.forEach { view ->
            if (view is ViewRootForTest) {
                return view
            } else if (view is ViewGroup) {
                val composeView = findAndroidComposeView(view)
                if (composeView != null) {
                    return composeView
                }
            }
        }
        return null
    }
}
