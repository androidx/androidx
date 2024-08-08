/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials

import android.graphics.drawable.Icon
import androidx.credentials.CreateCredentialRequest.DisplayInfo
import androidx.credentials.CreateCredentialRequest.DisplayInfo.Companion.createFrom
import androidx.credentials.internal.getFinalCreateCredentialData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreateCredentialRequestDisplayInfoTest {

    private val mContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun constructor_emptyUserId_throws() {
        assertThrows(IllegalArgumentException::class.java) { DisplayInfo("") }
    }

    @Test
    fun constructWithUserIdOnly_success() {
        val expectedUserId = "userId"

        val displayInfo = DisplayInfo(expectedUserId)

        assertThat(displayInfo.userId).isEqualTo(expectedUserId)
        assertThat(displayInfo.userDisplayName).isNull()
        assertThat(displayInfo.credentialTypeIcon).isNull()
    }

    @Test
    fun constructWithUserIdAndDisplayName_success() {
        val expectedUserId: CharSequence = "userId"
        val expectedDisplayName: CharSequence = "displayName"

        val displayInfo = DisplayInfo(expectedUserId, expectedDisplayName)

        assertThat(displayInfo.userId).isEqualTo(expectedUserId)
        assertThat(displayInfo.userDisplayName).isEqualTo(expectedDisplayName)
        assertThat(displayInfo.credentialTypeIcon).isNull()
        assertThat(displayInfo.preferDefaultProvider).isNull()
    }

    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    @Test
    fun constructWithUserIdAndDisplayNameAndDefaultProvider_success() {
        val expectedUserId: CharSequence = "userId"
        val expectedDisplayName: CharSequence = "displayName"
        val expectedDefaultProvider = "com.test/com.test.TestProviderComponent"

        val displayInfo =
            DisplayInfo(
                userId = expectedUserId,
                userDisplayName = expectedDisplayName,
                preferDefaultProvider = expectedDefaultProvider
            )

        assertThat(displayInfo.userId).isEqualTo(expectedUserId)
        assertThat(displayInfo.userDisplayName).isEqualTo(expectedDisplayName)
        assertThat(displayInfo.credentialTypeIcon).isNull()
        assertThat(displayInfo.preferDefaultProvider).isEqualTo(expectedDefaultProvider)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun constructWithOptionalParameters_success() {
        val expectedUserId: CharSequence = "userId"
        val expectedDisplayName: CharSequence = "displayName"
        val expectedIcon = Icon.createWithResource(mContext, R.drawable.ic_passkey)
        val expectedDefaultProvider = "defaultProvider"

        val displayInfo =
            DisplayInfo(expectedUserId, expectedDisplayName, expectedIcon, expectedDefaultProvider)

        assertThat(displayInfo.userId).isEqualTo(expectedUserId)
        assertThat(displayInfo.userDisplayName).isEqualTo(expectedDisplayName)
        assertThat(displayInfo.credentialTypeIcon).isEqualTo(expectedIcon)
        assertThat(displayInfo.preferDefaultProvider).isEqualTo(expectedDefaultProvider)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun constructFromBundle_success() {
        val expectedUserId = "userId"
        val request = CreatePasswordRequest(expectedUserId, "password")

        val displayInfo = createFrom(getFinalCreateCredentialData(request, mContext))

        assertThat(displayInfo.userId).isEqualTo(expectedUserId)
        assertThat(displayInfo.userDisplayName).isNull()
        assertThat(displayInfo.credentialTypeIcon?.resId).isEqualTo(R.drawable.ic_password)
        assertThat(displayInfo.preferDefaultProvider).isNull()
    }
}
