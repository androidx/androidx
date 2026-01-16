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

package androidx.glance.appwidget.remotecompose.todo

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.BaseRemoteComposeTest
import androidx.glance.layout.Box
import androidx.glance.layout.size
import kotlin.test.Ignore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteViewsTest : BaseRemoteComposeTest() {

    // TODO(b/450985714): Fix and re-enable this test
    @Ignore("Test not yet written, see b/450985714")
    @Test
    fun testCorrectnessOfRemoteViews() =
        fakeCoroutineScope.runTest {
            val content: @Composable () -> Unit = {
                Box(modifier = GlanceModifier.Companion.size(100.dp, 100.dp)) {
                    // no content
                }
            }

            TODO(
                "What tests, if any, can we perform on the RemoteViews object we create with" +
                    "draw instructions in order to verify it is well formed?"
            )
        }
}
