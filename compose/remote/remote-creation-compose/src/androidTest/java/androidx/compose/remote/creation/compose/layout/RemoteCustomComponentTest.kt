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

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import android.view.View
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.TestProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemoteCustomComponentTest {
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()

    @Test
    fun customComponent_textReturn() {
        val customSupport = AndroidCustomContextImpl()
        val textReturnDelegate =
            object : AndroidComponentSupport {
                override fun createView(context: Context): View = View(context)

                override fun configure(view: View, type: Int, value: String) {}

                override fun configure(view: View, type: Int, value: Int) {
                    if (type == 1) {
                        (customSupport.getRemoteContext() as? AndroidRemoteContext)?.overrideText(
                            value,
                            "Returned from Custom",
                        )
                    }
                }

                override fun configure(view: View, type: Int, value: Float) {}
            }
        customSupport.registerDelegate("TextReturnCustom", textReturnDelegate)

        val creationDisplayInfo = RemoteCreationDisplayInfo(200, 200, 160, 1.0f)
        val profile = TestProfiles.androidXExperimental

        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            profile = profile,
            player =
                object : RemoteBaseContentTestRule.Player {
                    @Composable
                    override fun Play(coreDocument: CoreDocument, size: Size) {
                        RemoteDocumentPlayer(
                            document = coreDocument,
                            documentWidth = size.width.toInt(),
                            documentHeight = size.height.toInt(),
                            customSupport = customSupport,
                        )
                    }
                },
        ) {
            val textState = rememberNamedRemoteString("named_text_return", "Initial")
            RemoteCustomComponent(name = "TextReturnCustom") { bindReturn(1, textState) }
        }

        val remoteContext = customSupport.getRemoteContext() as? AndroidRemoteContext
        assertThat(remoteContext).isNotNull()
        val textVarId = remoteContext!!.getVariableId("USER:named_text_return")
        assertThat(remoteContext.getText(textVarId)).isEqualTo("Returned from Custom")
    }

    @Test
    fun customComponent_floatReturn() {
        val customSupport = AndroidCustomContextImpl()
        val floatReturnDelegate =
            object : AndroidComponentSupport {
                override fun createView(context: Context): View = View(context)

                override fun configure(view: View, type: Int, value: String) {}

                override fun configure(view: View, type: Int, value: Int) {
                    if (type == 2) {
                        customSupport.getRemoteContext()?.overrideFloat(value, 42.5f)
                    }
                }

                override fun configure(view: View, type: Int, value: Float) {}
            }
        customSupport.registerDelegate("FloatReturnCustom", floatReturnDelegate)

        val creationDisplayInfo = RemoteCreationDisplayInfo(200, 200, 160, 1.0f)
        val profile = TestProfiles.androidXExperimental

        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            profile = profile,
            player =
                object : RemoteBaseContentTestRule.Player {
                    @Composable
                    override fun Play(coreDocument: CoreDocument, size: Size) {
                        RemoteDocumentPlayer(
                            document = coreDocument,
                            documentWidth = size.width.toInt(),
                            documentHeight = size.height.toInt(),
                            customSupport = customSupport,
                        )
                    }
                },
        ) {
            val floatState = rememberNamedRemoteFloat("named_float_return") { 0f.rf }
            RemoteCustomComponent(name = "FloatReturnCustom") { bindReturn(2, floatState) }
        }

        val remoteContext = customSupport.getRemoteContext() as? AndroidRemoteContext
        assertThat(remoteContext).isNotNull()
        val floatVarId = remoteContext!!.getVariableId("USER:named_float_return")
        assertThat(remoteContext.getFloat(floatVarId)).isEqualTo(42.5f)
    }
}
