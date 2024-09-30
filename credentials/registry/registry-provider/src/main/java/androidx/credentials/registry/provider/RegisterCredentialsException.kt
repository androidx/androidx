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

package androidx.credentials.registry.provider

/**
 * Represents an error thrown during a [RegistryManager.registerCredentials] transaction.
 *
 * @property type the type of the error
 * @property errorMessage the error message
 */
public abstract class RegisterCredentialsException(
    public val type: String,
    public val errorMessage: CharSequence? = null
) : Exception(errorMessage?.toString())
