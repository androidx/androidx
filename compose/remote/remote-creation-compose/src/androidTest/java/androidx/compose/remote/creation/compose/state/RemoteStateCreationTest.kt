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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.state

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.painterRemoteBitmap
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteStateCreationTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun rememberNamedRemoteInt_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedInt = rememberNamedRemoteInt("testInt", 42).withGlobalScope()
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedInt.toRemoteFloat())))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testInt")
    }

    @Test
    fun rememberNamedRemoteFloat_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedFloat = rememberNamedRemoteFloat("testFloat") { 42.42f.rf }
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedFloat)))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testFloat")
    }

    @Test
    fun rememberNamedRemoteLong_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedLong = rememberNamedRemoteLong("testLong", 42L)
                namedLong.writeToDocument(LocalRemoteComposeCreationState.current)
            }

        // No way to test this currently
    }

    @Test
    fun rememberNamedRemoteBoolean_isPresent() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedBoolean = rememberNamedRemoteBoolean("isRed", true).withGlobalScope()

                //                RemoteText(text = namedBoolean.select("a".rs, "b".rs))
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(namedBoolean.select(Color.Red.rc, Color.Blue.rc))
                )
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:isRed")
    }

    @Test
    fun rememberNamedRemoteString_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedString = rememberNamedRemoteString("testString", "Hello")
                RemoteText(text = namedString)
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.STRING_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testString")
    }

    @Test
    fun rememberNamedRemoteDp_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedDp = rememberNamedRemoteDp("testDp") { 10.rdp }
                RemoteBox(modifier = RemoteModifier.padding(namedDp.value))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testDp")
    }

    @Test
    fun rememberNamedRemoteColor_isTracked() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedColor =
                    rememberNamedRemoteColor("testColor", Color.Magenta).withGlobalScope()
                RemoteBox(modifier = RemoteModifier.size(10.rdp).background(namedColor))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.COLOR_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testColor")
    }

    @Test
    fun rememberNamedRemoteBitmap_FromUrl_isRendered() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedBitmap =
                    rememberNamedRemoteBitmap(
                        name = "testBitmapUrl",
                        url = "android.resource://androidx.compose.remote.foundation/drawable/dummy",
                    )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp).background(painterRemoteBitmap(namedBitmap))
                )
            }
    }

    @Test
    fun rememberNamedRemoteBitmap_FromImageBitmap_isRendered() = runTest {
        val coreDoc =
            remoteComposeTestRule.captureDocument(context) {
                val namedBitmap =
                    rememberNamedRemoteBitmap("testBitmapImage") {
                        Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                            .apply { eraseColor(android.graphics.Color.GREEN) }
                            .asImageBitmap()
                    }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp).background(painterRemoteBitmap(namedBitmap))
                )
            }
    }

    @Test
    fun creation_invoke_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            RemoteInt(42).writeToDocument(state)
            RemoteFloat(42f).writeToDocument(state)
            RemoteLong(42L).writeToDocument(state)
            RemoteBoolean(true).writeToDocument(state)
            RemoteString("hello").writeToDocument(state)
            RemoteColor(Color.Red).writeToDocument(state)
            RemoteDp(10.dp).writeToDocument(state)
        }
    }

    @Test
    fun creation_createForId_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            val iId = RemoteInt(1).writeToDocument(state) + 0x100000000L
            RemoteInt.createForId(iId).writeToDocument(state)

            val fId = RemoteFloat(1f).getFloatIdForCreationState(state)
            RemoteFloat.createForId(fId).writeToDocument(state)

            val lId = RemoteLong(1L).writeToDocument(state)
            RemoteLong.createForId(lId).writeToDocument(state)

            val bId = RemoteBoolean(true).writeToDocument(state)
            RemoteBoolean.createForId(bId).writeToDocument(state)

            val sId = RemoteString("h").writeToDocument(state)
            RemoteString.createForId(sId).writeToDocument(state)

            val cId = RemoteColor(Color.Red).writeToDocument(state)
            RemoteColor.createForId(cId).writeToDocument(state)

            val dpRef = RemoteFloat(10f)
            RemoteDp.createForId(dpRef).writeToDocument(state)
        }
    }

    @Test
    fun creation_createNamedRemoteX_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            RemoteInt.createNamedRemoteInt("i", defaultValue = 1).writeToDocument(state)
            RemoteFloat.createNamedRemoteFloat("f", defaultValue = 1f).writeToDocument(state)
            RemoteLong.createNamedRemoteLong("l", defaultValue = 1L).writeToDocument(state)
            RemoteBoolean.createNamedRemoteBoolean("b", defaultValue = true).writeToDocument(state)
            RemoteString.createNamedRemoteString("s", defaultValue = "h").writeToDocument(state)
            RemoteColor.createNamedRemoteColor("c", defaultValue = Color.Red).writeToDocument(state)
            RemoteDp.createNamedRemoteDp("d", defaultValue = 1.dp).writeToDocument(state)
        }
    }

    @Test
    fun creation_mutableCreate_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            MutableRemoteInt.createMutable(1).writeToDocument(state)
            MutableRemoteFloat.createMutable(1f).writeToDocument(state)
            MutableRemoteLong.createMutable(1L).writeToDocument(state)
            MutableRemoteBoolean.createMutable(true).writeToDocument(state)
            MutableRemoteString.createMutable("h").writeToDocument(state)
            // Color and Dp do not have MutableRemoteX.create versions.
        }
    }

    @Test
    fun creation_mutableForId_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            val iId = MutableRemoteInt.createMutable(1).writeToDocument(state).toLong()
            MutableRemoteInt.createMutableForId(iId).writeToDocument(state)

            val fId = MutableRemoteFloat.createMutable(1f).getFloatIdForCreationState(state)
            MutableRemoteFloat.createMutableForId(fId).writeToDocument(state)

            val lId = MutableRemoteLong.createMutable(initialValue = 1L).writeToDocument(state)
            MutableRemoteLong.createMutableForId(lId).writeToDocument(state)

            val bId = MutableRemoteBoolean.createMutable(true).writeToDocument(state).toLong()
            MutableRemoteBoolean.createMutableForId(bId).writeToDocument(state)

            val sId = MutableRemoteString.createMutable("h").writeToDocument(state)
            MutableRemoteString.createMutableForId(sId).writeToDocument(state)
            // Color and Dp do not have MutableRemoteX.forId versions.
        }
    }

    @Test
    fun creation_rememberMutableRemote_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            rememberMutableRemoteInt(1).writeToDocument(state)
            rememberMutableRemoteFloat(1f).writeToDocument(state)
            rememberMutableRemoteLong(1L).writeToDocument(state)
            rememberMutableRemoteBoolean(true).writeToDocument(state)
            rememberMutableRemoteString("h").writeToDocument(state)
            // Color and Dp do not have rememberMutableRemoteX versions.
        }
    }

    @Test
    fun creation_rememberNamedRemote_isStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current

            // Taking expression values
            rememberNamedRemoteFloat("f") { 1f.rf }.writeToDocument(state)
            rememberNamedRemoteColor("c", Color.Red).writeToDocument(state)
            rememberNamedRemoteDp("d") { 1.rdp }.writeToDocument(state)

            // Taking primitive values
            rememberNamedRemoteLong("l", 1L).writeToDocument(state)
            rememberNamedRemoteInt("i", 1).writeToDocument(state)
            rememberNamedRemoteString("s", "h").writeToDocument(state)
            rememberNamedRemoteBoolean("b", true).writeToDocument(state)
        }
    }

    @Test
    fun creation_rememberRemote_nonStandardized() = runTest {
        remoteComposeTestRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current

            // Only non-deprecated unnamed rememberRemoteX functions are tested here.

            // This form provides a RemoteContext
            rememberRemoteFloatExpression { 1f.rf }.writeToDocument(state)
        }
    }
}
