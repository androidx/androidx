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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.samples.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalRemoteCreationComposeApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconFromResTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        CreationDisplayInfo(500, 500, context.resources.displayMetrics.densityDpi)

    @org.junit.Before
    fun setup() {
        RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = true
    }

    @org.junit.After
    fun tearDown() {
        RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = false
    }

    @Test
    fun iconsFromRes() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            RemoteRow {
                Icon(resId = R.drawable.gs_map_wght500rond100_vd_theme_24)
                Icon(resId = R.drawable.gs_work_wght500rond100_vd_theme_24)
                Icon(resId = R.drawable.gs_category_search_wght500rond100_vd_theme_24)
                Icon(resId = R.drawable.test_vector)
            }
        }
    }

    @Composable
    private fun Icon(
        modifier: RemoteModifier = RemoteModifier.padding(8.dp).size(24.rdp),
        resId: Int,
    ) {
        RemoteIcon(
            modifier = modifier,
            imageVector = ImageVector.vectorResource(resId),
            contentDescription = null,
            tint = Color.Black.rc,
        )
    }
}
