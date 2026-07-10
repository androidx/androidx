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

package androidx.compose.remote.integration.demos.widget

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.compose.remote.integration.demos.ListActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.boundsInScreen
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based uiAutomator test of a scrolling column. Test on API 35+ for now. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 36)
@RunWith(AndroidJUnit4::class)
@MediumTest
class WidgetListA11yTest {
    private val checkSize = false

    @Test
    fun listActivitySemantics() {
        testSemantics(ListActivity::class.java)
    }

    @SdkSuppress(minSdkVersion = 36, maxSdkVersion = 36)
    @Test
    fun widgetListActivitySemantics() {
        testSemantics(ListRemoteViewActivity::class.java)
    }

    fun testSemantics(activity: Class<*>) {
        uiAutomator {
            startActivity(activity)

            val list = onElement { isScrollable }

            val listAni = list.accessibilityNodeInfo
            assertThat(listAni.childCount).isEqualTo(50)
            assertThat(listAni.className).isEqualTo("android.widget.ScrollView")
            val collectionInfo = listAni.collectionInfo
            assertThat(collectionInfo.itemCount)
                .isEqualTo(AccessibilityNodeInfo.CollectionInfo.UNDEFINED)
            assertThat(collectionInfo.importantForAccessibilityItemCount)
                .isEqualTo(AccessibilityNodeInfo.CollectionInfo.UNDEFINED)
            assertThat(collectionInfo.selectionMode)
                .isEqualTo(CollectionInfoCompat.SELECTION_MODE_NONE)

            val listBounds = listAni.boundsInScreen().toComposeRect()
            assertThat(listBounds.left).isEqualTo(20f)
            assertThat(listBounds.top).isEqualTo(200f)

            if (checkSize) {
                assertThat(listBounds).isEqualTo(Rect(20f, 200f, 1060f, 2200f))
            }

            assertThat(listAni.actionList)
                .containsAtLeast(
                    AccessibilityAction.ACTION_SCROLL_DOWN,
                    AccessibilityAction.ACTION_SCROLL_FORWARD,
                    AccessibilityAction.ACTION_SCROLL_TO_POSITION,
                )

            val firstListItem = listAni.getChild(0)

            assertThat(firstListItem.isVisibleToUser).isEqualTo(true)
            val firstListItemBounds = firstListItem.boundsInScreen().toComposeRect()
            assertThat(firstListItemBounds.left).isEqualTo(20f)
            assertThat(firstListItemBounds.top).isEqualTo(200f)

            if (checkSize) {
                assertThat(firstListItemBounds).isEqualTo(Rect(20f, 200f, 1060f, 452f))
            }

            assertThat(firstListItem.actionList)
                .containsAtLeast(
                    AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS,
                    AccessibilityAction.ACTION_SHOW_ON_SCREEN,
                )

            val secondListItem = listAni.getChild(2)

            assertThat(secondListItem.isVisibleToUser).isEqualTo(true)
            val secondListItemBounds = secondListItem.boundsInScreen().toComposeRect()
            assertThat(secondListItemBounds.left).isEqualTo(20f)
            assertThat(secondListItemBounds.top).isGreaterThan(200f)

            if (checkSize) {
                assertThat(secondListItemBounds).isEqualTo(Rect(20f, 704f, 1060f, 956f))
            }

            val twentiethListItem = listAni.getChild(20)

            assertThat(twentiethListItem.isVisibleToUser).isEqualTo(false)

            repeat(5) {
                assertThat(listAni.performAction(AccessibilityAction.ACTION_SCROLL_DOWN.id))
                    .isTrue()

                TimeUnit.MILLISECONDS.sleep(500)
            }

            repeat(5) {
                assertThat(listAni.performAction(AccessibilityAction.ACTION_SCROLL_UP.id)).isTrue()

                TimeUnit.MILLISECONDS.sleep(500)
            }
        }
    }
}
