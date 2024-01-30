/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A boolean representing whether or not we are in touch mode or not.
 *
 * This is a temporary workaround and should be removed after proper mouse handling is settled
 * (b/171402426).
 *
 * Until then, it is recommended to use [Modifier.pointerInput] to read whether an event comes
 * from a touch or mouse source. A more global way of determining this mode will also be added
 * as part of b/171402426.
 */
@Deprecated(message = "Avoid using if possible, see kdoc.", level = DeprecationLevel.WARNING)
internal actual val isInTouchMode = true
