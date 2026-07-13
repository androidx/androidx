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
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteStateOverrideTest {

    private lateinit var context: AndroidRemoteContext
    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    private val displayInfo = RemoteCreationDisplayInfo(500, 500, 1, 1.0f)

    @Before
    fun setUp() {
        context =
            AndroidRemoteContext().apply {
                useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
            }
    }

    private fun makeAndUpdateCoreDocument(bytes: ByteArray) =
        CoreDocument().apply {
            val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes))
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)
            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }

    @Ignore("b/534198733: Fix late-load override for Float")
    @Test
    fun floatOverride_loadedLate_fails() = runTest {
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myFloat = rememberNamedRemoteFloat("myFloat") { 5f.rf }
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(myFloat)))
            }

        // Set override BEFORE loading/updating the document
        context.setNamedFloatOverride("USER:myFloat", 20f)

        makeAndUpdateCoreDocument(document.bytes)

        val floatId = context.getVariableId("USER:myFloat")
        assertThat(context.getFloat(floatId)).isEqualTo(20f)
    }

    @Ignore("b/534198733: Fix late-load override for Color")
    @Test
    fun colorOverride_loadedLate_fails() = runTest {
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myColor = rememberNamedRemoteColor("myColor", Color.Red)
                RemoteBox(modifier = RemoteModifier.size(10.rdp).background(myColor))
            }

        val overrideColor = 0xFF00FF00.toInt() // Green
        context.setNamedColorOverride("USER:myColor", overrideColor)

        makeAndUpdateCoreDocument(document.bytes)

        val colorId = context.getVariableId("USER:myColor")
        assertThat(context.getColor(colorId)).isEqualTo(overrideColor)
    }

    @Ignore("b/534198733: Fix late-load override for Integer")
    @Test
    fun integerOverride_loadedLate_fails() = runTest {
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myInt = rememberNamedRemoteInt("myInt", 42)
                RemoteBox(modifier = RemoteModifier.size(RemoteDp(myInt.toRemoteFloat())))
            }

        context.setNamedIntegerOverride("USER:myInt", 100)

        makeAndUpdateCoreDocument(document.bytes)

        val intId = context.getVariableId("USER:myInt")
        assertThat(context.getInteger(intId)).isEqualTo(100)
    }

    @Ignore("b/534198733: Fix late-load override for String")
    @Test
    fun stringOverride_loadedLate_fails() = runTest {
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = displayInfo,
                context = applicationContext,
            ) {
                val myString = rememberNamedRemoteString("myString", "hello")
                RemoteText(myString)
            }

        context.setNamedStringOverride("USER:myString", "world")

        makeAndUpdateCoreDocument(document.bytes)

        val stringId = context.getVariableId("USER:myString")
        assertThat(context.getText(stringId)).isEqualTo("world")
    }
}
