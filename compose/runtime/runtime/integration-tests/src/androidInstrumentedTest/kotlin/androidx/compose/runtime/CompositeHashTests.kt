/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompositeHashTests : BaseComposeTest() {

    @get:Rule override val activityRule = makeTestActivityRule()

    @Test // b/338478720
    @MediumTest
    fun testMultipleSetContentCalls() {
        val activity = activityRule.activity
        var useFirstModifier by mutableStateOf(true)

        activity.show {
            AnimatedVisibility(
                true,
                modifier =
                    if (useFirstModifier) {
                        Modifier.consumeWindowInsets(PaddingValues(0.dp))
                    } else {
                        Modifier
                    },
            ) {
                val hash = currentCompositeKeyHash
                val original = remember { hash }
                assertEquals(original, hash)
            }
        }

        activity.waitForAFrame()
        useFirstModifier = !useFirstModifier
        activity.waitForAFrame()
    }
}
