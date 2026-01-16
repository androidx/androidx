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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.glance.Button
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import kotlin.test.assertContains
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test that the various Button methods produce correct output.
 *
 * TODO: this is one very basic test, improve
 * - test lambda and action buttons
 * - test the material3 buttons
 * - test more button params.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateEmittableButtonTest : BaseRemoteComposeTest() {
    @Test
    fun translateEmittableButton() {
        fakeCoroutineScope.runTest {
            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Button("Lambda Button", onClick = {})
                    //                    Button(
                    //                        "Action Button",
                    //                        onClick =
                    //                            StartActivityIntentAction(
                    //                                Intent(), // TODO
                    //                                activityOptions = null,
                    //                            ),
                    //                    )
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)
            val textLayout = getSimpleLeaf(doc) as TextLayout

            // TODO: check properties on the text layout

            val docString = doc.toNestedString()

            // the entire text is likely truncated
            assertContains(docString, "Lamb")

            // There should be a callback operation on the button
            assertContains(docString, "HostActionOperation")

            // our button should be a box with a text
            assertContains(docString, "BOX [")

            assertContains(docString, "TEXT_LAYOUT")
        }
    }
}
