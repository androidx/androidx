/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.topics

import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@ExperimentalFeatures.Ext11OptIn
class EncryptedTopicTest {
    @Test
    fun testToString() {
        val result =
            "EncryptedTopic { EncryptedTopic=encryptedTopic1, KeyIdentifier=publicKey1," +
                " EncapsulatedKey=encapsulatedKey1 }"
        var encryptedTopic =
            EncryptedTopic(
                "encryptedTopic1".toByteArray(),
                "publicKey1",
                "encapsulatedKey1".toByteArray(),
            )
        Truth.assertThat(encryptedTopic.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        var encryptedTopic1 =
            EncryptedTopic(
                "encryptedTopic".toByteArray(),
                "publicKey",
                "encapsulatedKey".toByteArray(),
            )
        var encryptedTopic2 =
            EncryptedTopic(
                "encryptedTopic".toByteArray(),
                "publicKey",
                "encapsulatedKey".toByteArray(),
            )

        Truth.assertThat(encryptedTopic1 == encryptedTopic2).isTrue()
    }
}
