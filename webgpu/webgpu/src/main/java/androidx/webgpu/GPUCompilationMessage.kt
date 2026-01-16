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

package androidx.webgpu

/** A single message generated during shader compilation. */
public class GPUCompilationMessage(
    /** The content of the compilation message. */
    public var message: String,
    /** The type or severity of the message (error, warning, info). */
    @CompilationMessageType public var type: Int,
    public var lineNum: Long,
    public var linePos: Long,
    /** The byte offset in the source code where the message originates. */
    public var offset: Long,
    /** The length in bytes of the affected source code span. */
    public var length: Long,
)
