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
package androidx.compose.runtime.tooling

internal actual class DiagnosticComposeException
actual constructor(private val trace: ComposeStackTrace) : RuntimeException() {
    init {
        // Only group key traces use the stack frames to ensure that frames are present even
        // when the message is stripped, since source information based traces do not fit the
        // format (source information does not encode package or class names).
        if (!trace.hasSourceInformation) {
            // We cannot fill the trace straight away, as the `trace` field is not initialized when
            // `fillInStackTrace` is called. Instead, it will be initialized to an empty trace in
            // `fillInStackTrace` and filled in here when required.
            stackTrace =
                trace.filterInternalFramesByGroupKey().mapToArray {
                    StackTraceElement("$\$compose", "m$${it.groupKey}", "SourceFile", 1)
                }
        }
    }

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    override val message: String?
        get() =
            if (trace.hasSourceInformation) {
                buildString {
                    appendLine("Composition stack when thrown:")
                    appendStackTrace(trace)
                }
            } else {
                "Composition stack when thrown:"
            }

    private inline fun <T, reified R> List<T>.mapToArray(map: (T) -> R): Array<R> =
        Array(size) { map(get(it)) }
}
