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

package androidx.tracing.capture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

class TracingEngineTest {
    @Test
    fun testTracingEngineLinux() {
        assumeTrue(isLinux())
        assumeTrue(isTraceboxOnPath())

        val engine = SystemTracing()
        val output = File("/tmp/perfetto/linux.perfetto-trace")
        // Delete the file if it exists already.
        if (output.exists()) {
            output.delete()
        }
        engine.start(output = output)
        @Suppress(
            "BanThreadSleep"
        ) // The amount of time we are sleeping has no outcome on the result of the test.
        engine.use { Thread.sleep(/* millis= */ 200) }
        // Ideally we should parse and validate the output file with Perfetto TraceProcessor
        // That will be done as a follow-up.
        assertTrue(output.exists(), "File ${output.absolutePath} does not exist")
    }
}
