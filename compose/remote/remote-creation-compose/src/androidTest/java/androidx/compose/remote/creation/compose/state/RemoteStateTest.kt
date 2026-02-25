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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.state

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteStateTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun cachesRemoteColor() = runTest {
        withContext(Dispatchers.Main) {
            captureSingleRemoteDocument(context) {
                val blue = rememberNamedRemoteColor("blue", Color.Blue)
                val red = rememberNamedRemoteColor("red", Color.Red)
                val blue2 = rememberNamedRemoteColor("blue", Color.Blue)

                AssertSameSameDifferent(blue, blue2, red)
            }
        }
    }

    @Test
    fun cachesRemoteBitmap() = runTest {
        withContext(Dispatchers.Main) {
            captureSingleRemoteDocument(context) {
                val blue =
                    rememberNamedRemoteBitmap("blue") {
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                            .apply { setPixel(0, 0, Color.Blue.toArgb()) }
                            .asImageBitmap()
                    }
                val red =
                    rememberNamedRemoteBitmap("red") {
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                            .apply { setPixel(0, 0, Color.Red.toArgb()) }
                            .asImageBitmap()
                    }
                val blue2 =
                    rememberNamedRemoteBitmap("blue", url = "https://example.org/favicon.ico")

                AssertSameSameDifferent(blue, blue2, red)
            }
        }
    }

    @Test
    fun cachesRemoteInt() = runTest {
        withContext(Dispatchers.Main) {
            captureSingleRemoteDocument(context) {
                val one = rememberNamedRemoteInt(name = "one", defaultValue = 1)
                val two = rememberNamedRemoteInt(name = "two", defaultValue = 2)
                val one2 = rememberNamedRemoteInt(name = "one", defaultValue = 3)

                AssertSameSameDifferent(one, one2, two)
            }
        }
    }

    @Test
    fun cachesRemoteLong() = runTest {
        withContext(Dispatchers.Main) {
            captureSingleRemoteDocument(context) {
                val one = rememberNamedRemoteLong(name = "one", defaultValue = 1L)
                val two = rememberNamedRemoteLong(name = "two", defaultValue = 2L)
                val one2 = rememberNamedRemoteLong(name = "one", defaultValue = 3L)

                AssertSameSameDifferent(one, one2, two)
            }
        }
    }

    @Test
    fun cachesRemoteString() = runTest {
        withContext(Dispatchers.Main) {
            captureSingleRemoteDocument(context) {
                val blue = rememberNamedRemoteString(name = "blue", defaultValue = "blue")
                val red = rememberNamedRemoteString(name = "red", defaultValue = "red")
                val blue2 = rememberNamedRemoteString(name = "blue", defaultValue = "blue2")

                AssertSameSameDifferent(blue, blue2, red)
            }
        }
    }

    @Test
    fun withGlobalScope() = runTest {
        withContext(Dispatchers.Main) {
            val capturedDoc =
                captureSingleRemoteDocument(context) {
                    val s1 =
                        rememberNamedRemoteString(
                            name = "s1",
                            defaultValue = "1",
                            domain = RemoteState.Domain.User,
                        )
                    // Will be committed immediately in a global scope
                    val s2 =
                        rememberNamedRemoteString(
                                name = "s2",
                                defaultValue = "2",
                                domain = RemoteState.Domain.User,
                            )
                            .withGlobalScope()

                    RemoteBox { RemoteText(s1 + s2) }
                }

            val operations = ArrayList<Operation>()
            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(capturedDoc.bytes)).apply {
                inflateFromBuffer(operations)
            }

            // s2 has global scope so is created before ROOT
            val s2Index =
                operations.indexOfFirst { it is NamedVariable && it.mVarName == "USER:s2" }
            val rootIndex = operations.indexOfFirst { it is RootLayoutComponent }
            val boxIndex = operations.indexOfFirst { it is BoxLayout }
            val s1Index =
                operations.indexOfFirst { it is NamedVariable && it.mVarName == "USER:s1" }

            val operationIds = listOf(s2Index, rootIndex, boxIndex, s1Index)
            assertThat(operationIds).doesNotContain(-1)
            assertThat(operationIds).isInOrder()
        }
    }

    @RemoteComposable
    @Composable
    private fun <T : BaseRemoteState<*>> AssertSameSameDifferent(first: T, first2: T, second: T) {
        assertSame(first, first2)
        assertNotSame(first, second)

        val creationState = LocalRemoteComposeCreationState.current
        val firstId = first.getIdForCreationState(creationState)
        val firstId2 = first.getIdForCreationState(creationState)
        val first2Id1 = first2.getIdForCreationState(creationState)
        val secondId = second.getIdForCreationState(creationState)

        assertEquals(firstId, firstId2)
        assertEquals(firstId, first2Id1)
        assertNotEquals(firstId, secondId)
    }
}
