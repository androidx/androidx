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

package androidx.datastore.tink

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * A [Serializer] implementation that provides transparent encryption and decryption using Tink's
 * [Aead] (Authenticated Encryption with Associated Data).
 *
 * This class wraps another [Serializer], encrypting the data before it is written to the
 * [OutputStream] and decrypting it after it is read from the [InputStream].
 *
 * @param aead The [Aead] instance used for encryption and decryption.
 * @param wrappedSerializer The underlying serializer for the data type [T].
 * @param associatedData The associated data to be used for encryption and decryption. It is very
 *   important to use a unique [associatedData] value so that if the same key is used to encrypt
 *   multiple sets of data, it will still be safe from a ciphertext swapping/substitution attack.
 *   Users should use something unique like the filename as the associated data. If they are sure
 *   that the [Aead] instance is not reused across multiple data stores, they can set a default
 *   associated data value of `byteArrayOf()`.
 */
public class AeadSerializer<T>(
    private val aead: Aead,
    private val wrappedSerializer: Serializer<T>,
    private val associatedData: ByteArray = byteArrayOf(),
) : Serializer<T> {

    /** Returns the default value for the data type [T]. */
    override val defaultValue: T = wrappedSerializer.defaultValue

    /**
     * Reads the data from the [input], decrypts it using the provided [Aead] instance, and
     * delegates to the [wrappedSerializer] to deserialize the decrypted bytes.
     *
     * @throws java.security.GeneralSecurityException if the data cannot be decrypted
     * @throws androidx.datastore.core.CorruptionException if the [wrappedSerializer] fails to read
     *   the data.
     */
    @Throws(CorruptionException::class, GeneralSecurityException::class)
    override suspend fun readFrom(input: InputStream): T {
        val encrypted = input.readBytes()
        val decrypted =
            if (encrypted.isNotEmpty()) {
                aead.decrypt(encrypted, associatedData)
            } else {
                encrypted
            }
        return wrappedSerializer.readFrom(ByteArrayInputStream(decrypted))
    }

    /**
     * Delegates to the [wrappedSerializer] to serialize the data, encrypts it using the provided
     * [Aead] instance, and writes the encrypted bytes to the [output].
     */
    override suspend fun writeTo(t: T, output: OutputStream) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        wrappedSerializer.writeTo(t, byteArrayOutputStream)
        val encrypted = aead.encrypt(byteArrayOutputStream.toByteArray(), associatedData)
        output.write(encrypted)
    }
}
