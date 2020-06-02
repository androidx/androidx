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

/**
 * Creates a [MasterKey] with the provided parameters.
 *
 * @param context The context to work with.
 * @param keyAlias The alias to use for the `MasterKey`.
 * @param keyScheme The [MasterKey.KeyScheme] to have the `MasterKey` use.
 * @param authenticationRequired `true` if the user must authenticate for the `MasterKey` to be
 * used.
 * @param userAuthenticationValidityDurationSeconds Duration in seconds that the `MasterKey` is
 * valid for after the user has authenticated. Must be a value > 0.
 * @param requestStrongBoxBacked `true` if the key should be stored in Strong Box, if possible.
 */
fun MasterKey(
    context: Context,
    keyAlias: String = MasterKey.DEFAULT_MASTER_KEY_ALIAS,
    keyScheme: MasterKey.KeyScheme = MasterKey.KeyScheme.AES256_GCM,
    authenticationRequired: Boolean = false,
    userAuthenticationValidityDurationSeconds: Int =
        MasterKey.getDefaultAuthenticationValidityDurationSeconds(),
    requestStrongBoxBacked: Boolean = false
) = MasterKey.Builder(context, keyAlias)
    .setKeyScheme(keyScheme)
    .setUserAuthenticationRequired(
        authenticationRequired,
        userAuthenticationValidityDurationSeconds
    )
    .setRequestStrongBoxBacked(requestStrongBoxBacked)
    .build()
