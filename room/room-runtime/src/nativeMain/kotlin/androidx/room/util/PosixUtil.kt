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

<<<<<<<< HEAD:compose/ui/ui-test/src/jsMain/kotlin/androidx/compose/ui/test/Actions.jsMain.kt
package androidx.compose.ui.test

@OptIn(ExperimentalTestApi::class)
internal actual fun SemanticsNodeInteraction.performClickImpl(): SemanticsNodeInteraction {
    return performMouseInput {
        click()
    }
}
========
package androidx.room.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.strerror

/** Convenience function to get a String description of the last error number. */
@OptIn(ExperimentalForeignApi::class)
fun stringError(): String = strerror(errno)?.toKString() ?: "Unknown error"
>>>>>>>> 4ddd0ca0d2033b1ddbe4311683067312bc2be853:room/room-runtime/src/nativeMain/kotlin/androidx/room/util/PosixUtil.kt
