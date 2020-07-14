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

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme

/**
 * Opens an instance of encrypted SharedPreferences
 *
 * @param fileName The name of the file to open; can not contain path separators.
 * @param masterKey The master key to use.
 * @param prefKeyEncryptionScheme The scheme to use for encrypting keys.
 * @param prefValueEncryptionScheme The scheme to use for encrypting values.
 * @return The SharedPreferences instance that encrypts all data.
 */
fun EncryptedSharedPreferences(
    context: Context,
    fileName: String,
    masterKey: MasterKey,
    prefKeyEncryptionScheme: PrefKeyEncryptionScheme = PrefKeyEncryptionScheme.AES256_SIV,
    prefValueEncryptionScheme: PrefValueEncryptionScheme = PrefValueEncryptionScheme.AES256_GCM
) = EncryptedSharedPreferences.create(
    context,
    fileName,
    masterKey,
    prefKeyEncryptionScheme,
    prefValueEncryptionScheme
)
