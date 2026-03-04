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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.util.RemoteDocumentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class RemoteDpTest {

    @get:Rule val remoteComposeTestRule = RemoteDocumentTestRule()

    private val context: RemoteContext
        get() = remoteComposeTestRule.context

    private val testScope =
        NoRemoteCompose().apply { remoteDensity = remoteComposeTestRule.density }

    @Test
    fun constructor_createsCorrectly() {
        val floatValue = 10.5f
        val remoteFloat = RemoteFloat(floatValue)
        val remoteFloatDp = RemoteDp(remoteFloat)

        assertThat(remoteFloatDp.value).isEqualTo(remoteFloat)
        assertThat(remoteFloatDp.value.constantValue).isEqualTo(floatValue)
    }

    @Test
    fun constructor_extensionFunction() {
        val intValue = 10
        val rdpValue = intValue.rdp
        assertThat(rdpValue.value.constantValue).isEqualTo(intValue)
    }

    @Test
    fun newInstance_hasSameFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f

        val (resultId, resultDpId) =
            remoteComposeTestRule.initialise {
                val remoteFloat = RemoteFloat(floatValue)
                val remoteFloatDp = RemoteDp(remoteFloat)
                val resultId = remoteFloat.getIdForCreationState(it)
                val resultDpId = remoteFloatDp.value.getIdForCreationState(it)
                Pair(resultId, resultDpId)
            }
        assertThat(context.getFloat(resultId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
    }

    @Test
    fun newInstance_hasSameIdFromOriginalRemoteFloat() {
        val floatValue = 10.5f

        val (resultId, resultDpId) =
            remoteComposeTestRule.initialise {
                val remoteFloat = RemoteFloat(floatValue)
                val remoteFloatDp = RemoteDp(remoteFloat)

                val resultId = remoteFloat.getIdForCreationState(it)
                val resultDpId = remoteFloatDp.value.getIdForCreationState(it)
                Pair(resultId, resultDpId)
            }

        assertThat(resultId).isEqualTo(resultDpId)
    }

    @Test
    fun toPx_hasDifferentFloatValueAsOriginalRemoteFloat() {
        val floatValue = 10.5f
        val density = 2f

        val (resultDpId, resultPxId, resultPxId2) =
            remoteComposeTestRule.initialise {
                val remoteFloatDp = RemoteDp(floatValue.rf)
                val remoteFloatPx = remoteFloatDp.toPx(testScope.remoteDensity)
                val remoteFloatPx2 = remoteFloatDp.toPx()

                val resultDpId = remoteFloatDp.value.getIdForCreationState(it)
                val resultPxId = remoteFloatPx.getIdForCreationState(it)
                val resultPxId2 = remoteFloatPx2.getIdForCreationState(it)
                Triple(resultDpId, resultPxId, resultPxId2)
            }

        assertThat(context.getFloat(resultDpId)).isEqualTo(floatValue)
        assertThat(context.getFloat(resultPxId)).isEqualTo(floatValue * density)
        assertThat(context.getFloat(resultPxId2)).isEqualTo(floatValue * density)
    }

    @Test
    fun toPx_remoteFloatHasDifferentIdFromOriginal() {
        val floatValue = 10.5f

        val (resultDpId, resultPxId) =
            remoteComposeTestRule.initialise {
                val remoteFloatDp = RemoteDp(floatValue.rf)
                val remoteFloatPx = remoteFloatDp.toPx(testScope.remoteDensity)

                val resultDpId = remoteFloatDp.value.getIdForCreationState(it)
                val resultPxId = remoteFloatPx.getIdForCreationState(it)
                Pair(resultDpId, resultPxId)
            }

        assertThat(resultDpId).isNotEqualTo(resultPxId)
    }

    @Test
    fun fromDp() {
        val dp = 10.5.dp
        val px = with(Density(context.density, 1f)) { dp.toPx() }

        val resultDpId =
            remoteComposeTestRule.initialise {
                val remoteFloatDp = dp.asRdp()
                remoteFloatDp.getIdForCreationState(it)
            }

        assertThat(context.getFloat(resultDpId)).isEqualTo(px)
    }

    @Test
    fun asRdpFloatIdIsConstant() {
        val dp = 152.dp

        val resultFloat =
            remoteComposeTestRule.initialise {
                val remoteFloatDp = dp.asRdp()
                with(it) { remoteFloatDp.floatId }
            }

        assertThat(resultFloat).isEqualTo(304f)
    }

    @Test
    fun remoteDp_cacheKey() {
        val dp1 = 10.rdp
        val dp2 = 10.rdp
        assertThat(dp1.cacheKey).isNotNull()
        assertThat(dp1.cacheKey).isEqualTo(dp2.cacheKey)
    }
}
