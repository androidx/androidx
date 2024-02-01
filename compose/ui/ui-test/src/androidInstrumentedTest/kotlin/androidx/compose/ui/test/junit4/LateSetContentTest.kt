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

package androidx.compose.ui.test.junit4

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.util.BoundaryNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.filters.LargeTest
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class LateSetContentTest {

    @Test
    fun test() = runAndroidComposeUiTest<Activity> {
        onNodeWithTag("Node").assertExists()
    }

    class Activity : ComponentActivity() {
        private val handler = Handler(Looper.getMainLooper())
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            handler.postDelayed(
                {
                    setContent { BoundaryNode("Node") }
                },
                500
            )
        }
    }
}
