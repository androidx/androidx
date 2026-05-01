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

package androidx.compose.remote.testing

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteDocContentTestRuleScreenshotTest {
    @get:Rule val remoteDocContentTestRule = RemoteDocContentTestRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun textValueChange() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val doc = CoreDocument()

        val documentStream = context.assets.open("screenshottest.rc")
        val buffer = RemoteComposeBuffer.fromInputStream(documentStream)
        doc.initFromBuffer(buffer)

        remoteDocContentTestRule.setContent(coreDocument = doc, size = Size(100f, 100f))

        val screenshotBefore = remoteDocContentTestRule.composeTestRule.onRoot().captureToImage()
        screenshotBefore.assertAgainstGolden(
            screenshotRule,
            "RemoteDocContentTestRuleScreenshotTest_textValueChange_before",
        )

        uiAutomator {
            val item = onElement { isClickable }
            assertThat(item).isNotNull()
            item.click()
        }

        val screenshotAfter = remoteDocContentTestRule.captureRootToImage()
        screenshotAfter.assertAgainstGolden(
            screenshotRule,
            "RemoteDocContentTestRuleScreenshotTest_textValueChange_after",
        )
    }
}
