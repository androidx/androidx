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

package androidx.compose.remote.creation.compose.state

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.painterRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteBoolean.Companion.createNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp.Companion.createNamedRemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray.Companion.createNamedRemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteLong.Companion.createNamedRemoteLong
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.remote.testing.LimitsRule
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@OptIn(ExperimentalRemoteCreationComposeApi::class)
class RemoteStateCreationTest {

    @get:Rule val remoteCaptureRule = RemoteCaptureTestRule()
    @get:Rule val limitsRule = LimitsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
    }

    @After
    fun cleanup() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
    }

    @Test
    fun rememberNamedRemoteInt_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedInt = remember { createNamedRemoteInt("testInt", 42) }.withGlobalScope()
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedInt.toRemoteFloat())))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testInt")
    }

    @Test
    fun rememberNamedRemoteFloat_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedFloat = remember { createNamedRemoteFloat("testFloat", 42.42f) }
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedFloat)))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testFloat")
    }

    @Test
    fun rememberNamedRemoteLong_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedLong = remember { createNamedRemoteLong("testLong", 42L) }
                LocalRemoteComposeCreationState.current.getId(namedLong)
            }

        // No way to test this currently
    }

    @Test
    fun rememberNamedRemoteBoolean_isPresent() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBoolean = remember {
                    createNamedRemoteBoolean("isRed", true)
                }
                    .withGlobalScope()

                //                RemoteText(text = namedBoolean.select("a".rs, "b".rs))
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(namedBoolean.select(RemoteColor.Red, RemoteColor.Blue))
                )
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:isRed")
    }

    @Test
    fun createNamedRemote_duplicateName_throwsIllegalStateException() = runTest {
        val exception =
            kotlin.test.assertFailsWith<IllegalStateException> {
                remoteCaptureRule.captureDocument(context) {
                    val namedFloat = remember { createNamedRemoteFloat("shared", 10f) }
                    val namedInt = remember { createNamedRemoteInt("shared", 5) }

                    val selectedFloat = namedFloat + 1f.rf
                    val selectedInt = namedInt + 1.ri
                    LocalRemoteComposeCreationState.current.getId(selectedFloat)
                    LocalRemoteComposeCreationState.current.getId(selectedInt)
                }
            }
        assertThat(exception).hasMessageThat().contains("Duplicate named variable: USER:shared")
    }

    @Test
    fun rememberNamedRemoteString_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedString = remember { createNamedRemoteString("testString", "Hello") }
                RemoteText(text = namedString)
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.STRING_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testString")
    }

    @Test
    fun rememberNamedRemoteDp_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedDp = rememberNamedRemoteDp("testDp", 10.rdp)
                RemoteBox(modifier = RemoteModifier.padding(namedDp.value))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testDp")
    }

    @Test
    fun rememberNamedRemoteColor_isTracked() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedColor = remember {
                    RemoteColor.createNamedRemoteColor("testColor", RemoteColor.Magenta)
                }
                    .withGlobalScope()
                RemoteBox(modifier = RemoteModifier.size(10.rdp).background(namedColor))
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.COLOR_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testColor")
    }

    @Test
    fun rememberNamedRemoteBitmap_FromUrl_isRendered() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "testBitmapUrl",
                        url =
                            "android.resource://androidx.compose.remote.foundation/drawable/dummy",
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        val box = coreDoc.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()
        val bitmapData = box.canvasOperations!!.list.filterIsInstance<BitmapData>().single()
        assertThat(bitmapData.width).isEqualTo(1)
        assertThat(bitmapData.height).isEqualTo(1)
    }

    @Test
    fun rememberNamedRemoteBitmap_FromUrlWithExplicitDimensions_encodesDimensions() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "testBitmapUrl",
                        url =
                            "android.resource://androidx.compose.remote.foundation/drawable/dummy",
                        width = 300,
                        height = 200,
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        val box = coreDoc.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()
        val bitmapData = box.canvasOperations!!.list.filterIsInstance<BitmapData>().single()
        assertThat(bitmapData.width).isEqualTo(300)
        assertThat(bitmapData.height).isEqualTo(200)
    }

    @Test
    fun rememberNamedRemoteBitmap_FromImageBitmap_isRendered() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        "testBitmapImage",
                        Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                            .apply { eraseColor(android.graphics.Color.GREEN) }
                            .asImageBitmap(),
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
    }

    @Test
    fun rememberCreateNamedRemoteImageBitmap_fromUrl_matchesPreviousBehavior() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        val url = "android.resource://androidx.compose.remote.foundation/drawable/dummy"
        val prevDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "testBitmapUrl",
                        url = url,
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        val newDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "testBitmapUrl",
                        url = url,
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        assertThat(newDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
            .asList()
            .containsExactlyElementsIn(prevDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
    }

    @Test
    fun rememberCreateNamedRemoteImageBitmap_fromImageBitmap_matchesPreviousBehavior() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        fun createBitmap() =
            Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.GREEN) }
                .asImageBitmap()

        val prevDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        "testBitmapImage",
                        createBitmap(),
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        val newDoc =
            remoteCaptureRule.captureDocument(context) {
                val namedBitmap = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        "testBitmapImage",
                        createBitmap(),
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(painterRemoteImageBitmap(namedBitmap))
                )
            }
        assertThat(newDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
            .asList()
            .containsExactlyElementsIn(prevDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
    }

    @Test
    fun rememberCreateNamedRemoteImageBitmap_sameName_isDeduplicated() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        var id1 = -1
        var id2 = -1
        var id3 = -1
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val creationState = LocalRemoteComposeCreationState.current
                val bitmap1 = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "sharedBitmap",
                        url = "url1",
                    )
                }
                val bitmap2 = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "sharedBitmap",
                        url = "url1",
                    )
                }
                val bitmap3 = remember {
                    RemoteImageBitmap.createNamedRemoteImageBitmap(
                        name = "otherBitmap",
                        url = "url2",
                    )
                }

                id1 = creationState.getId(bitmap1)
                id2 = creationState.getId(bitmap2)
                id3 = creationState.getId(bitmap3)
            }

        assertThat(id1).isNotEqualTo(-1)
        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)

        val namedImageVars = coreDoc.getNamedVariables(NamedVariable.IMAGE_TYPE).toList()
        assertThat(namedImageVars.filter { it == "${RemoteDomains.USER}:sharedBitmap" }).hasSize(1)
        assertThat(namedImageVars.filter { it == "${RemoteDomains.USER}:otherBitmap" }).hasSize(1)
    }

    @Test
    fun rememberRemoteBitmap_FromUrl_isRendered() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val bitmap = remember {
                    RemoteImageBitmap(
                        url = "android.resource://androidx.compose.remote.foundation/drawable/dummy"
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp).background(painterRemoteImageBitmap(bitmap))
                )
            }
    }

    @Test
    fun rememberRemoteBitmap_sameUrl_isDeduplicated() = runTest {
        limitsRule.setEnableImageUrls(true)
        limitsRule.setEnableImageFiles(true)
        var id1 = -1
        var id2 = -1
        var id3 = -1
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val creationState = LocalRemoteComposeCreationState.current
                val bitmap1 = remember { RemoteImageBitmap(url = "url1") }
                val bitmap2 = remember { RemoteImageBitmap(url = "url1") }
                val bitmap3 = remember { RemoteImageBitmap(url = "url2") }

                id1 = creationState.getId(bitmap1)
                id2 = creationState.getId(bitmap2)
                id3 = creationState.getId(bitmap3)
            }

        assertThat(id1).isNotEqualTo(-1)
        assertThat(id1).isEqualTo(id2) // Same URL should have same ID
        assertThat(id1).isNotEqualTo(id3) // Different URL should have different ID
    }

    @Test
    fun rememberMutableRemoteBitmap_isRendered() = runTest {
        val imageBitmap =
            Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.GREEN) }
                .asImageBitmap()
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val bitmap = remember { MutableRemoteImageBitmap(imageBitmap) }
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp).background(painterRemoteImageBitmap(bitmap))
                )
            }
    }

    @Test
    fun creation_invoke_isStandardized() = runTest {
        remoteCaptureRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            state.getId(RemoteInt(42))
            state.getId(RemoteFloat(42f))
            state.getId(RemoteLong(42L))
            state.getId(RemoteBoolean(true))
            state.getId(RemoteString("hello"))
            state.getId(RemoteColor.Red)
            state.getId(10.rdp)
        }
    }

    @Test
    fun creation_createForId_isStandardized() = runTest {
        remoteCaptureRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            val iId = state.getId(RemoteInt(1)) + 0x100000000L
            state.getId(RemoteInt.createForId(iId))

            val fId = state.getFloatId(RemoteFloat(1f))
            state.getId(RemoteFloat.createForId(fId))

            val lId = state.getId(RemoteLong(1L))
            state.getId(RemoteLong.createForId(lId))

            val bId = state.getId(RemoteBoolean(true))
            state.getId(RemoteBoolean.createForId(bId))

            val sId = state.getId(RemoteString("h"))
            state.getId(RemoteString.createForId(sId))

            val cId = state.getId(RemoteColor.Red)
            state.getId(RemoteColor.createForId(cId))

            val dpRef = RemoteFloat(10f)
            state.getId(RemoteDp.createForId(dpRef))
        }
    }

    @Test
    fun creation_createNamedRemoteX_isStandardized() = runTest {
        remoteCaptureRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            state.getId(createNamedRemoteInt("i", defaultValue = 1))
            state.getId(createNamedRemoteFloat("f", defaultValue = 1f))
            state.getId(createNamedRemoteLong("l", defaultValue = 1L))
            state.getId(createNamedRemoteBoolean("b", defaultValue = true))
            state.getId(createNamedRemoteString("s", defaultValue = "h"))
            state.getId(RemoteColor.createNamedRemoteColor("c", defaultValue = RemoteColor.Red))
            state.getId(createNamedRemoteDp("d", defaultValue = 1f))
        }
    }

    @Test
    fun creation_mutableCreate_isStandardized() = runTest {
        remoteCaptureRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current
            state.getId(remember { MutableRemoteInt(1) })
            state.getId(remember { MutableRemoteFloat(1f) })
            state.getId(remember { MutableRemoteLong(1L) })
            state.getId(remember { MutableRemoteBoolean(true) })
            state.getId(remember { MutableRemoteString("h") })
            // Color and Dp do not have MutableRemoteX.create versions.
        }
    }

    @Test
    fun creation_rememberNamedRemote_isStandardized() = runTest {
        remoteCaptureRule.captureDocument(context) {
            val state = LocalRemoteComposeCreationState.current

            // Taking expression values
            state.getId(rememberNamedRemoteFloat("f", 1f.rf))
            state.getId(rememberNamedRemoteColor("c", RemoteColor.Red))
            state.getId(rememberNamedRemoteDp("d", 1.rdp))

            // Taking primitive values
            state.getId(rememberNamedRemoteLong("l", 1L))
            state.getId(rememberNamedRemoteInt("i", 1))
            state.getId(rememberNamedRemoteString("s", "h"))
            state.getId(rememberNamedRemoteBoolean("b", true))
        }
    }

    @Test
    fun creation_rememberRemote_withInt_doesNotCrash() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val useLargeScroll = remember { MutableRemoteBoolean(true) }
                RemoteText(useLargeScroll.select("True".rs, "False".rs), color = RemoteColor.Black)
            }
        val playbackContext = AndroidRemoteContext()
        playbackContext.androidContext = context
        coreDoc.initializeContext(playbackContext, null)
        coreDoc.applyDataOperations(playbackContext)
    }

    @Test
    fun rememberWithCreateNamedRemote_reusesVariableIdAcrossCalls() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val state = LocalRemoteComposeCreationState.current
                val intA = remember { createNamedRemoteInt("cachedInt", 10) }
                val intB = remember { createNamedRemoteInt("cachedInt", 20) }

                val idA = state.getId(intA)
                val idB = state.getId(intB)

                assertThat(idA).isEqualTo(idB)
            }
        val intVars = coreDoc.getNamedVariables(NamedVariable.INT_TYPE).toList()
        assertThat(intVars.filter { it == "${RemoteDomains.USER}:cachedInt" }).hasSize(1)
    }

    @Test
    fun rememberCreateNamedRemoteFloatArray_sameName_isDeduplicated() = runTest {
        val defaultData1 = floatArrayOf(10f, 20f, 30f)
        val defaultData2 = floatArrayOf(40f, 50f, 60f)
        var id1 = -1
        var id2 = -1
        var id3 = -1
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                val creationState = LocalRemoteComposeCreationState.current
                val array1 = remember {
                    createNamedRemoteFloatArray("sharedArray", defaultData1)
                }
                val array2 = remember {
                    createNamedRemoteFloatArray("sharedArray", defaultData2)
                }
                val array3 = remember {
                    createNamedRemoteFloatArray("otherArray", defaultData1)
                }

                id1 = creationState.getId(array1)
                id2 = creationState.getId(array2)
                id3 = creationState.getId(array3)
            }

        assertThat(id1).isNotEqualTo(-1)
        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)

        val namedArrayVars = coreDoc.getNamedVariables(NamedVariable.FLOAT_ARRAY_TYPE).toList()
        assertThat(namedArrayVars.filter { it == "${RemoteDomains.USER}:sharedArray" }).hasSize(1)
        assertThat(namedArrayVars.filter { it == "${RemoteDomains.USER}:otherArray" }).hasSize(1)
    }
}
