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

package androidx.compose.remote.a11y

import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based uiAutomator test of a scrolling column. Test on API 35+ for now. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 36)
@RunWith(AndroidJUnit4::class)
@MediumTest
class ListA11yTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    @Ignore("b/484916070")
    fun listSemantics() {
        remoteComposeTestRule.runTest {
            ScrollableList(modifier = RemoteModifier.fillMaxSize(), items = 30, notches = 0)
        }

        uiAutomator {
            device.dumpWindowHierarchy(System.out)

            val list = onElement { isScrollable }

            val listAni = list.accessibilityNodeInfo
            assertThat(listAni.childCount).isEqualTo(30)

            assertThat(listAni.actionList)
                .containsAtLeast(
                    AccessibilityAction.ACTION_SCROLL_DOWN,
                    AccessibilityAction.ACTION_SCROLL_FORWARD,
                    AccessibilityAction.ACTION_SCROLL_TO_POSITION,
                )

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

    @Test
    @Ignore("b/484916070")
    fun listWithSnapSemantics() {
        remoteComposeTestRule.runTest {
            ScrollableList(modifier = RemoteModifier.fillMaxSize(), items = 30, notches = 29)
        }

        uiAutomator {
            val list = onElement { isScrollable }

            val listAni = list.accessibilityNodeInfo

            repeat(5) {
                assertThat(listAni.performAction(AccessibilityAction.ACTION_SCROLL_DOWN.id))
                    .isTrue()

                TimeUnit.MILLISECONDS.sleep(500)
            }
        }
    }

    @RemoteComposable
    @Composable
    fun ScrollableList(
        modifier: RemoteModifier = RemoteModifier,
        items: Int = 50,
        notches: Int = items,
    ) {
        val scrollState = rememberRemoteScrollState(notches)
        RemoteColumn(
            modifier = modifier.verticalScroll(scrollState).background(Color.White),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            repeat(items) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(192.rdp)
                            .border(1.rdp, Color.LightGray.rc)
                            // Must be direct child of the scrollable item
                            .semantics(mergeDescendants = true) {},
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.Center,
                ) {
                    RemoteText("Item $it", color = RemoteColor(Color.Black), fontSize = 36.rsp)
                }
            }
        }
    }
}
