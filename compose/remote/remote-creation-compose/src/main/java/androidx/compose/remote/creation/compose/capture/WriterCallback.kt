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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo

/**
 * A callback interface used during the capture process to write out the captured composable
 * information. This allows the capture system to be pass on types that can't be serialized into the
 * document such asn PendingIntent.
 *
 * Implementations of this interface will handle the serialization or transformation of the captured
 * composable tree into a desired output format, such as a binary file, a JSON representation, or a
 * network stream.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface WriterCallback {}
