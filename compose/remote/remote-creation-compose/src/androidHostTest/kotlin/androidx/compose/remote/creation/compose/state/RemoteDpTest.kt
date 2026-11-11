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

    @Test
    fun fromDp() {
        val dp = 10.5.dp
        val px = with(Density(context.density, 1f)) { dp.toPx() }

        val resultDpId = remoteComposeTestRule.initialise {
            val remoteFloatDp = dp.asRdp()
            remoteFloatDp.getIdForCreationState(it)
        }

        assertThat(context.getFloat(resultDpId)).isEqualTo(px)
    }

    @Test
    fun asRdpFloatIdIsConstant() {
        val dp = 152.dp

        val resultFloat = remoteComposeTestRule.initialise {
            val remoteFloatDp = dp.asRdp()
            with(it) { remoteFloatDp.floatId }
        }

        assertThat(resultFloat).isEqualTo(304f)
    }

    @Test
    fun createNamedRemoteDp_fromComposeDp() {
        val resultId = remoteComposeTestRule.initialise {
            val dp1 = RemoteDp.createNamedRemoteDp("dp1", 10f)
            val dp2 = RemoteDp.createNamedRemoteDp("dp2", 5f)
            val result = dp1 + dp2
            result.value.getIdForCreationState(it)
        }
        assertThat(context.getFloat(resultId)).isEqualTo(15f)
    }

    @Test
    fun transform_remoteDp_asDp() {
        val namedDp = RemoteDp.createNamedRemoteDp("padding_val", 10f)
        val dpVal = namedDp + 5.rdp

        val replaced = dpVal.transform { node ->
            if (node.isStructurallyEqual(namedDp.value)) 20f.rf else null
        }
        assertThat(replaced.hasConstantValue).isTrue()
        assertThat(replaced.asDp).isEqualTo(25.dp)
    }
}
