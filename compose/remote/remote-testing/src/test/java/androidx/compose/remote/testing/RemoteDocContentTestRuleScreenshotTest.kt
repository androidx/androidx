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
import androidx.compose.remote.testing.util.assertAgainstGolden
import androidx.compose.remote.testing.util.captureToImage
import androidx.compose.remote.testing.util.saveToFile
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteDocContentTestRuleScreenshotTest {
    @get:Rule val remoteDocContentTestRule = RemoteDocContentTestRule()

    private val saveScreenshots = false

    @Test
    fun textValueChange() {
        val documentStream = javaClass.classLoader!!.getResourceAsStream("screenshottest.rc")!!

        val doc = CoreDocument()
        val buffer = RemoteComposeBuffer.fromInputStream(documentStream)
        doc.initFromBuffer(buffer)

        remoteDocContentTestRule.setContent(coreDocument = doc, size = Size(100f, 100f))

        val screenshot = captureToImage(remoteDocContentTestRule.composeTestRule)

        if (saveScreenshots) {
            screenshot
                .asAndroidBitmap()
                .saveToFile("RemoteDocContentTestRuleScreenshotTest_textValueChange")
        }
        screenshot.assertAgainstGolden("RemoteDocContentTestRuleScreenshotTest_textValueChange")
    }
}
