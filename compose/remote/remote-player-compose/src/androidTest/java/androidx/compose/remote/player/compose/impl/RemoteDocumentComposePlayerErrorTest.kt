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

package androidx.compose.remote.player.compose.impl

import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemoteDocumentComposePlayerErrorTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testComposePlayer_paintExceptionShowsErrorUI() {
        val coreDoc = CoreDocument()
        val failingDocument =
            object : RemoteDocument(coreDoc) {
                override fun paint(context: RemoteContext, theme: Int) {
                    throw RuntimeException("Simulated Paint Exception")
                }
            }

        composeTestRule.setContent {
            RemoteComposePlayer(
                document = failingDocument,
                modifier = Modifier.size(200.dp),
                theme = 0,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⚠").assertExists()
        composeTestRule.onNodeWithText("Simulated Paint Exception").assertExists()
    }

    @Test
    fun testComposePlayer_initExceptionShowsErrorUI() {
        val coreDoc = CoreDocument()
        val failingDocument =
            object : RemoteDocument(coreDoc) {
                override fun initializeContext(context: RemoteContext) {
                    throw RuntimeException("Simulated Init Exception")
                }
            }

        composeTestRule.setContent {
            RemoteComposePlayer(
                document = failingDocument,
                modifier = Modifier.size(200.dp),
                theme = 0,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⚠").assertExists()
        composeTestRule.onNodeWithText("Simulated Init Exception").assertExists()
    }
}
