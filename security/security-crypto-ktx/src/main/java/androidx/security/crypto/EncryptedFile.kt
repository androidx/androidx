/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.security.crypto

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedFile.FileEncryptionScheme
import java.io.File

/**
 * Creates an [EncryptedFile]
 *
 * @param context The context to work with.
 * @param file The backing [File].
 * @param masterKey The [MasterKey] that should be used.
 * @param fileEncryptionScheme The [FileEncryptionScheme] to use, defaulting to
 * [FileEncryptionScheme.AES256_GCM_HKDF_4KB].
 * @param keysetPrefName The `SharedPreferences` file to store the keyset for this [EncryptedFile].
 * @param keysetAlias The alias in the `SharedPreferences` file to store the keyset for this
 * [EncryptedFile].
 */
@SuppressLint("StreamFiles")
fun EncryptedFile(
    context: Context,
    file: File,
    masterKey: MasterKey,
    fileEncryptionScheme: FileEncryptionScheme = FileEncryptionScheme.AES256_GCM_HKDF_4KB,
    keysetPrefName: String? = null,
    keysetAlias: String? = null
) = EncryptedFile.Builder(context, file, masterKey, fileEncryptionScheme).apply {
    if (keysetPrefName != null) setKeysetPrefName(keysetPrefName)
    if (keysetAlias != null) setKeysetAlias(keysetAlias)
}.build()
