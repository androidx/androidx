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

package androidx.benchmark.macro

import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.macro.NativeLoadOrderQuery.nativeLoadOrderEntries
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadOrderQueryTest {
    @Test
    fun loadOrderQueryTest() {
        assumeTrue(!isEmulator)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("load-order-test", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val entries =
                nativeLoadOrderEntries(
                        session = this,
                        targetPackageName = "com.google.android.youtube",
                    )
                    .toList()
            assertTrue { entries.isNotEmpty() }
            assertTrue { entries.all { it.buildId.isNotEmpty() && it.relPc > 0L } }
        }
    }

    @Test
    fun largeLoadOrderQueryTest() {
        assumeTrue(!isEmulator)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("load-order-test-large", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            val entries =
                nativeLoadOrderEntries(
                    session = this,
                    targetPackageName = "com.google.android.youtube",
                )
            assertNotNull(entries.firstOrNull())
            var index = 0
            for (entry in entries) {
                assertTrue { entry.buildId.isNotEmpty() && entry.relPc > 0L }
                if (index > 100) {
                    break
                }
                index += 1
            }
        }
    }
}
