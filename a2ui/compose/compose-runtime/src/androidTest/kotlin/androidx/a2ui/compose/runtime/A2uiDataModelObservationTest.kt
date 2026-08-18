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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiDataModelObservationTest {

    private val dataModel = A2uiDataModel()

    @Test
    fun observe_dataModelUpdate_triggersRecomposition() = runComposeUiTest {
        val path = path("/user/name")
        dataModel.update(path, "Alice")

        setContent { BasicText(text = "Hello, ${dataModel[path]}") }

        onNodeWithText("Hello, Alice").assertIsDisplayed()

        dataModel.update(path, "Bob")
        waitForIdle()

        onNodeWithText("Hello, Bob").assertIsDisplayed()
    }

    @Test
    fun observe_hydratedPath_triggersRecomposition() = runComposeUiTest {
        val targetPath = path("/deep/nested/title")

        setContent {
            val title = dataModel[targetPath] as? String ?: "Pending..."
            BasicText(text = title)
        }

        onNodeWithText("Pending...").assertIsDisplayed()

        dataModel.update(targetPath, "Loaded")
        waitForIdle()

        onNodeWithText("Loaded").assertIsDisplayed()
    }

    @Test
    fun observe_parentSubtreeReplacement_triggersRecomposition() = runComposeUiTest {
        dataModel.update(path("/profile"), mapOf("user" to mapOf("name" to "Alice")))

        setContent {
            val name = dataModel[path("/profile/user/name")] as? String ?: "No Name"
            BasicText(text = name)
        }

        onNodeWithText("Alice").assertIsDisplayed()

        // Replace the entire "/profile" subtree, removing the "user" object
        dataModel.update(path("/profile"), mapOf("settings" to "dark"))
        waitForIdle()

        // The child component should reactively update to null/fallback
        onNodeWithText("No Name").assertIsDisplayed()
    }

    private fun path(pathString: String): A2uiDataPath = A2uiDataPath(pathString)
}
