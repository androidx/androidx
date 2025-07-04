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
@file:JvmName("DirectBootExceptionUtilKt") // Workaround for b/313964643

package androidx.datastore.core

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmName

/**
 * In Android, this function wraps the provided [Exception] with a `DirectBootUsageException` if the
 * [DataStore] in the provided path is in Credential Encrypted Storage and the device is locked (in
 * direct boot mode).
 *
 * In all other platforms, this function is NO-OP, and returns the provided [exception].
 *
 * @param parentDirPath Path of the parent directory containing the [DataStore] file.
 * @param exception the `FileNotFoundException` encountered
 * @return An instance of a `DirectBootUsageException` or the provided [exception].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
expect fun wrapExceptionIfDueToDirectBoot(parentDirPath: String?, exception: Exception): Exception
