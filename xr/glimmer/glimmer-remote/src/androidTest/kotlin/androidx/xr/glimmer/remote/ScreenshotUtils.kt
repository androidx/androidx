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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.xr.glimmer.remote

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher

internal fun RcPlayerTestRule.setRemoteContent(
    context: Context,
    size: DpSize = DpSize(300.dp, 150.dp),
    content: @Composable @RemoteComposable () -> Unit,
) {
    setRemoteContent(
        remoteCreationDisplayInfo =
            createCreationDisplayInfo(
                context = context,
                size = with(Density(context)) { size.toSize() },
            ),
        playComposableWrapper = { player ->
            Box(modifier = Modifier.size(size).testTag(ROOT_TEST_TAG)) { player() }
        },
        content = content,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.assertRootAgainstGolden(
    goldenName: String,
    screenshotRule: AndroidXScreenshotTestRule,
) {
    onNodeWithTag(ROOT_TEST_TAG)
        .captureToImage()
        .asAndroidBitmap()
        .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(threshold = 0.995))
}

internal const val GOLDEN_DIRECTORY = "xr/glimmer/glimmer-remote"

internal const val ROOT_TEST_TAG = "root_test_tag"
