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
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.painter.painterRemoteImageBitmap
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.remote.testing.LimitsRule
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@OptIn(ExperimentalRemoteCreationComposeApi::class)
class NamedVariableDiscardedTest {

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

    // --- Color ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun color_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedColor = rememberNamedRemoteColor("testColor", Color.Red)
                    RemoteBox(modifier = RemoteModifier.size(10.rdp).background(namedColor))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.COLOR_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testColor")
    }

    @Test
    fun color_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedColor =
                        rememberNamedRemoteColor("testColor", Color.Red).withGlobalScope()
                    RemoteBox(modifier = RemoteModifier.size(10.rdp).background(namedColor))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.COLOR_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testColor")
    }

    // --- Float ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun float_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedFloat = rememberNamedRemoteFloat("testFloat") { 10f.rf }
                    RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedFloat)))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testFloat")
    }

    @Test
    fun float_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedFloat =
                        rememberNamedRemoteFloat("testFloat") { 10f.rf }.withGlobalScope()
                    RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedFloat)))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testFloat")
    }

    // --- Dp ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun dp_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedDp = rememberNamedRemoteDp("testDp") { 10.rdp }
                    RemoteBox(modifier = RemoteModifier.size(namedDp))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testDp")
    }

    @Test
    fun dp_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedDp = rememberNamedRemoteDp("testDp") { 10.rdp }.withGlobalScope()
                    RemoteBox(modifier = RemoteModifier.size(namedDp))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.FLOAT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testDp")
    }

    // --- Int ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun int_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedInt = rememberNamedRemoteInt("testInt", 42)
                    RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedInt.toRemoteFloat())))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testInt")
    }

    @Test
    fun int_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedInt = rememberNamedRemoteInt("testInt", 42).withGlobalScope()
                    RemoteBox(modifier = RemoteModifier.size(RemoteDp(namedInt.toRemoteFloat())))
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testInt")
    }

    // --- Boolean ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun boolean_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedBoolean = rememberNamedRemoteBoolean("testBoolean", true)
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(10.rdp)
                                .background(namedBoolean.select(Color.Red.rc, Color.Blue.rc))
                    )
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testBoolean")
    }

    @Test
    fun boolean_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedBoolean =
                        rememberNamedRemoteBoolean("testBoolean", true).withGlobalScope()
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(10.rdp)
                                .background(namedBoolean.select(Color.Red.rc, Color.Blue.rc))
                    )
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.INT_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testBoolean")
    }

    // --- Long ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun long_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedLong = rememberNamedRemoteLong("testLong", 42L)
                    with(LocalRemoteComposeCreationState.current) { namedLong.id }
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.LONG_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testLong")
    }

    @Test
    fun long_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedLong = rememberNamedRemoteLong("testLong", 42L).withGlobalScope()
                    with(LocalRemoteComposeCreationState.current) { namedLong.id }
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.LONG_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testLong")
    }

    // --- String ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun string_discarded_without_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedString = rememberNamedRemoteString("testString", "hello")
                    RemoteText(namedString)
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.STRING_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testString")
    }

    @Test
    fun string_preserved_with_global_scope() = runTest {
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedString =
                        rememberNamedRemoteString("testString", "hello").withGlobalScope()
                    RemoteText(namedString)
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.STRING_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testString")
    }

    // --- Bitmap ---

    @Ignore("b/533137513: Named variables inside LayoutComponents are discarded")
    @Test
    fun bitmap_discarded_without_global_scope() = runTest {
        val bitmap = createBitmap(10, 10).asImageBitmap()
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedBitmap = rememberNamedRemoteImageBitmap("testBitmap") { bitmap }
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(10.rdp)
                                .background(painterRemoteImageBitmap(namedBitmap))
                    )
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testBitmap")
    }

    @Test
    fun bitmap_preserved_with_global_scope() = runTest {
        val bitmap = createBitmap(10, 10).asImageBitmap()
        val coreDoc =
            remoteCaptureRule.captureDocument(context) {
                RemoteBox {
                    val namedBitmap =
                        rememberNamedRemoteImageBitmap("testBitmap") { bitmap }.withGlobalScope()
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(10.rdp)
                                .background(painterRemoteImageBitmap(namedBitmap))
                    )
                }
            }
        assertThat(coreDoc.getNamedVariables(NamedVariable.IMAGE_TYPE))
            .asList()
            .contains("${RemoteDomains.USER}:testBitmap")
    }
}
