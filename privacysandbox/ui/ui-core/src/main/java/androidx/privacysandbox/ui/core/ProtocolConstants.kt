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

package androidx.privacysandbox.ui.core

import androidx.annotation.RestrictTo

/**
 * Constants shared between UI library artifacts to establish an IPC protocol across library
 * versions. Adding new constants is allowed, but **never change the value of a constant**, or
 * you'll break binary compatibility between UI library versions.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ProtocolConstants {
    const val sdkActivityLauncherBinderKey = "sdkActivityLauncherBinderKey"
}