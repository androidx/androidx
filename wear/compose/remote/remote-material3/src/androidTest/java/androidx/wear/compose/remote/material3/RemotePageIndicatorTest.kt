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
 * See the License for the doSpecific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemotePageIndicatorTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val creationDisplayInfo =
        createCreationDisplayInfo(ApplicationProvider.getApplicationContext(), Size(500f, 500f))

    @Test
    fun horizontal_page_indicator_pageCount3() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun horizontal_page_indicator_pageCount6() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 2.ri, pageCount = 6)
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun horizontal_page_indicator_pageCount10_settled0() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 0.ri, pageCount = 10)
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun horizontal_page_indicator_pageCount10_settled5() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 5.ri, pageCount = 10)
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun horizontal_page_indicator_pageCount10_transition() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state =
                rememberRemotePageIndicatorState(
                    selectedPage = 4.ri,
                    pageOffset = 0.5f.rf,
                    pageCount = 10,
                )
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun horizontal_page_indicator_pageCount10_settled9() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 9.ri, pageCount = 10)
            RemoteHorizontalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount3() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
            RemoteVerticalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount6() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 2.ri, pageCount = 6)
            RemoteVerticalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount10_settled0() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 0.ri, pageCount = 10)
            RemoteVerticalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount10_settled5() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 5.ri, pageCount = 10)
            RemoteVerticalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount10_transition() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state =
                rememberRemotePageIndicatorState(
                    selectedPage = 4.ri,
                    pageOffset = 0.5f.rf,
                    pageCount = 10,
                )
            RemoteVerticalPageIndicator(state = state)
        }
    }

    @Test
    fun vertical_page_indicator_pageCount10_settled9() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            val state = rememberRemotePageIndicatorState(selectedPage = 9.ri, pageCount = 10)
            RemoteVerticalPageIndicator(state = state)
        }
    }
}
