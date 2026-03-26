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

package androidx.compose.animation.core

import platform.Foundation.NSThread

// TODO: https://youtrack.jetbrains.com/issue/CMP-9987/Propose-a-better-solution-for-checking-the-main-thread-in-compose-animation-rememberTransition
// appleMain implementation is valid only for the use case of rememberTransition. See CL: https://android-review.googlesource.com/c/platform/frameworks/support/+/3992815
internal actual fun getCurrentThread(): Any {
    return NSThread.currentThread()
}