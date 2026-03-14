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

/**
 * [SystemTracing] is meant to be complimentary to `androidx.tracing.TraceDriver`. The library helps
 * capture `system` level information that can be merged with in-process tracing to get deeper
 * performance insights.
 *
 * This library supports `Linux` based platforms using Perfetto. The library itself does **not**
 * bundle Perfetto binaries. Instead, the user is responsible for bootstrapping the latest release
 * of `tracebox` from the latest Perfetto [release]((https://github.com/google/perfetto/releases).
 *
 * **Note**: Ensure that `tracebox` is an **executable**, and has been added to your shell's `PATH`.
 *
 * For more information refer to the Project's `README.md`.
 */
public class SystemTracing internal constructor() : AutoCloseable {
    @Volatile private var engine: PerfettoEngine? = null
    @Volatile private var started: Boolean = false

    private val lock: Any = Any()

    public fun start(configProtoText: File? = null, output: File) {
        if (started) return

        val supported = isLinux()
        require(supported) { "Unsupported operating system." }
        synchronized(lock) {
            if (started) return

            val engine = PerfettoEngine()
            this.engine = engine
            started = true
            engine.start(configProtoText = configProtoText, output = output)
        }
    }

    override fun close() {
        if (!started) return

        synchronized(lock) {
            if (!started) return

            engine?.close()
            started = false
        }
    }
}
