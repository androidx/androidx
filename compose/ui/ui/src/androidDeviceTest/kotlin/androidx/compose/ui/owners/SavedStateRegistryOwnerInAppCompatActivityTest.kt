/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.owners

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateRegistryOwnerInAppCompatActivityTest {
    @get:Rule val rule = createAndroidComposeRule<AppCompatActivity>(StandardTestDispatcher())

    @Test
    fun ownerIsAvailable() {
        var owner: SavedStateRegistryOwner? = null

        rule.setContent { owner = LocalSavedStateRegistryOwner.current }

        rule.waitForIdle()
        assertEquals(rule.activity, owner)
    }

    @Test
    fun ownerIsAvailableWhenComposedIntoView() {
        var owner: SavedStateRegistryOwner? = null

        rule.runOnUiThread {
            val view = ComposeView(rule.activity)
            rule.activity.setContentView(view)
            view.setContent { owner = LocalSavedStateRegistryOwner.current }
        }

        rule.waitForIdle()
        assertEquals(rule.activity, owner)
    }
}
