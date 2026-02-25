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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.util.RemoteDocumentTestRule
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(
    sdk = [org.robolectric.annotation.Config.TARGET_SDK],
    qualifiers = "xhdpi",
)
class RemoteTextUnitTest {

    @get:Rule val remoteComposeTestRule = RemoteDocumentTestRule()

    private val context: RemoteContext
        get() = remoteComposeTestRule.context

    private val testScope =
        NoRemoteCompose().apply { remoteDensity = remoteComposeTestRule.density }

    @Test
    fun constructor_createsCorrectly() {
        val floatValue = 16f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteTextUnit = RemoteTextUnit(remoteFloat, TextUnitType.Sp)

        assertThat(remoteTextUnit.value).isEqualTo(remoteFloat)
        assertThat(remoteTextUnit.type).isEqualTo(TextUnitType.Sp)
        assertThat(remoteTextUnit.constantValueOrNull).isEqualTo(floatValue.sp)
    }

    @Test
    fun extensionProperties_createCorrectly() {
        assertThat(16.rsp.value.constantValue).isEqualTo(16f)
        assertThat(16.sp.asRemoteTextUnit().value.constantValue).isEqualTo(16f)
        assertThat(16.sp.asRemoteTextUnit().type).isEqualTo(TextUnitType.Sp)
    }

    @Test
    fun toPx_sp_calculatesCorrectly() {
        val intValue = 16
        // Default xhdpi density is 2.0
        val density = 2f
        // Default font scale is 1.0 from RemoteDensity.HOST or similar
        val fontScale = 1.0f

        val (resultId, resultPxId) =
            remoteComposeTestRule.initialise {
                val remoteTextUnit = intValue.rsp
                val remoteFloatPx = remoteTextUnit.toPx(testScope.remoteDensity)

                val resultId = remoteTextUnit.value.getIdForCreationState(it)
                val resultPxId = remoteFloatPx.getIdForCreationState(it)
                Pair(resultId, resultPxId)
            }

        assertThat(context.getFloat(resultId)).isEqualTo(intValue)
        assertThat(context.getFloat(resultPxId)).isEqualTo(intValue * density * fontScale)
    }

    @Test
    fun constantValueOrNull_handlesEmCorrectly() {
        val floatValue = 1.5f
        val remoteTextUnit = RemoteTextUnit(floatValue.rf, TextUnitType.Em)
        val textUnit = remoteTextUnit.constantValueOrNull

        assertThat(textUnit?.type).isEqualTo(TextUnitType.Em)
        assertThat(textUnit?.value).isEqualTo(floatValue)
    }
}
