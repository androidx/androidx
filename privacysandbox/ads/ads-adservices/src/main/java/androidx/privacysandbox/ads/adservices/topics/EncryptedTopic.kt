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
import java.util.Objects

/**
 * This class will be used to return encrypted topic cipher text along with necessary fields
 * required to decrypt it.
 *
 * <p>Decryption of {@link EncryptedTopic#getEncryptedTopic()} should give json string for {@link
 * Topic}. Example of decrypted json string: {@code { "taxonomy_version": 5, "model_version": 2,
 * "topic_id": 10010 }}
 *
 * <p>Decryption of cipher text is expected to happen on the server with the corresponding algorithm
 * and private key for the public key {@link EncryptedTopic#getKeyIdentifier()}}.
 *
 * <p>Detailed steps on decryption can be found on <a
 * href="https://developer.android.com/design-for-safety/privacy-sandbox/guides/topics">Developer
 * Guide</a>.
 */
@ExperimentalFeatures.Ext11OptIn
class EncryptedTopic
public constructor(
    val encryptedTopic: ByteArray,
    val keyIdentifier: String,
    val encapsulatedKey: ByteArray
) {
    override fun toString(): String {
        val encryptedTopicString =
            "EncryptedTopic=${encryptedTopic.decodeToString()}" +
                ", KeyIdentifier=$keyIdentifier" +
                ", EncapsulatedKey=${encapsulatedKey.decodeToString()} }"
        return "EncryptedTopic { $encryptedTopicString"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedTopic) return false
        return this.encryptedTopic.contentEquals(other.encryptedTopic) &&
            this.keyIdentifier.contentEquals(other.keyIdentifier) &&
            this.encapsulatedKey.contentEquals(other.encapsulatedKey)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            encryptedTopic.contentHashCode(),
            keyIdentifier,
            encapsulatedKey.contentHashCode()
        )
    }
}
