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

package androidx.sqlite.driver.bundled

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

const val SQLITE_OPEN_READONLY = 0x00000001
const val SQLITE_OPEN_READWRITE = 0x00000002
const val SQLITE_OPEN_CREATE = 0x00000004
const val SQLITE_OPEN_URI = 0x00000040
const val SQLITE_OPEN_MEMORY = 0x00000080
const val SQLITE_OPEN_NOMUTEX = 0x00008000
const val SQLITE_OPEN_FULLMUTEX = 0x00010000
const val SQLITE_OPEN_NOFOLLOW = 0x01000000

/**
 * The flags constant that can be used with [BundledSQLiteDriver.open].
 */
@IntDef(
    flag = true,
    value = [
        SQLITE_OPEN_READONLY,
        SQLITE_OPEN_READWRITE,
        SQLITE_OPEN_CREATE,
        SQLITE_OPEN_URI,
        SQLITE_OPEN_MEMORY,
        SQLITE_OPEN_NOMUTEX,
        SQLITE_OPEN_FULLMUTEX,
        SQLITE_OPEN_NOFOLLOW
    ])
@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
expect annotation class OpenFlag()
