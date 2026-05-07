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
package androidx.compose.remote.creation.compose.state

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteState.Domain
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import kotlin.enums.enumEntries
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteEnumTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    private enum class Checked {
        Off,
        On,
    }

    @Test
    fun ordinal() {
        val b0 = RemoteEnum(Checked.Off)
        val b1 = RemoteEnum(Checked.On)
        val i0 = b0.ordinal
        val i1 = b1.ordinal
        val i0Id = i0.getIdForCreationState(creationState)
        val i1Id = i1.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(i0Id)).isEqualTo(0)
        assertThat(context.getInteger(i1Id)).isEqualTo(1)
    }

    @Test
    fun toRemoteString() {
        val b0 = RemoteEnum(Checked.Off)
        val b1 = RemoteEnum(Checked.On)
        val i0 = b0.toRemoteString { it.name.rs }
        val i1 = b1.toRemoteString()
        val i0Id = i0.getIdForCreationState(creationState)
        val i1Id = i1.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(i0Id)).isEqualTo("Off")
        assertThat(context.getText(i1Id)).isEqualTo("On")
    }

    @Test
    fun constantValue() {
        assertThat(RemoteEnum(Checked.Off).constantValue).isEqualTo(Checked.Off)

        val variableInt = (RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) % 1.rf).toRemoteInt()
        assertThat(RemoteEnum(variableInt, enumEntries<Checked>()).constantValueOrNull).isNull()
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteEnum(Checked.Off)
        // implemented as RemoteInt
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(0))

        val named = RemoteEnum.createNamedRemoteEnum<Checked>("test", Checked.Off)
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(Domain.User, "test"))
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
