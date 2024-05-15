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
package bench.flame.diff.config

import java.nio.file.Path
import kotlin.io.path.name

internal object Paths {
    val currentDir get() = Path.of("").toAbsolutePath()
    private val dependenciesDir get() = currentDir.resolve(".deps")
    val savedTracesDir get() = currentDir.resolve("saved-traces")
    val savedDiffsDir get() = currentDir.resolve("saved-diffs")
    val outDir get() = frameworksSupportDir.parent.parent.resolve("out")
    private val frameworksSupportDir get() = currentDir.parent.parent.also {
        check(it.parent.name == "frameworks" && it.name == "support")
    }
    val simpleperfDir get() = dependenciesDir.resolve("simpleperf")
    val stackcollapsePy get() = simpleperfDir.resolve("stackcollapse.py")
    val flamegraphDir get() = dependenciesDir.resolve("Flamegraph")
    val flamegraphPl get() = flamegraphDir.resolve("flamegraph.pl")
    val difffoldedPl get() = flamegraphDir.resolve("difffolded.pl")

    val traceFileNamePattern = ".*stackSampling.*\\.trace"
}

internal object Uris {
    // Using jgielzak@ fork of https://github.com/brendangregg/FlameGraph until
    // https://github.com/brendangregg/FlameGraph/pull/329 is merged.
    val flamegraphGitHub = "https://github.com/gielzak/FlameGraph"

    // Using a snapshot of Simpleperf until https://r.android.com/2980531 makes it into the NDK.
    // Next check: 2024-Q4.
    val simpleperfGoogleSource =
        "https://android.googlesource.com/platform/system/extras/+archive/" +
                "436786af3a357db5fd72cdac97903d6d587944a1/simpleperf/scripts.tar.gz"
}
