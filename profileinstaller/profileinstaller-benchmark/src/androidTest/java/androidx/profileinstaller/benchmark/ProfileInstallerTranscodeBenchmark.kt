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

package androidx.profileinstaller.benchmark

import android.content.res.AssetManager
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.profileinstaller.DeviceProfileWriter
import androidx.profileinstaller.ProfileInstaller
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileInstallerTranscodeBenchmark {

    private var mTempCurFile: File? = null

    @get:Rule
    val benchmarkRule = BenchmarkRule()
    private val assets: AssetManager = InstrumentationRegistry.getInstrumentation().context.assets

    @Before
    fun setupTempDir() {
        mTempCurFile = File.createTempFile("ProfileInstallerTranscodeBenchmark", ".prof")
    }

    @After
    fun rmTempFile() {
        mTempCurFile?.delete()
    }

    private inline fun BenchmarkRule.Scope.newTranscoderUntimed(
        block: (DeviceProfileWriter) -> Unit
    ): DeviceProfileWriter {
        var transcoder: DeviceProfileWriter? = null
        runWithTimingDisabled {
            transcoder = DeviceProfileWriter(
                assets,
                Diagnostics(),
                PROFILE_LOCATION,
                mTempCurFile!!,
                File("")
            ).also(block)
        }
        return transcoder!!
    }

    private fun assumeDeviceSupportsAot() {
        val transcoder = DeviceProfileWriter(
            assets,
            Diagnostics(),
            PROFILE_LOCATION,
            mTempCurFile!!,
            File("")
        )
        assumeTrue(
            "Device must support AOT to run this benchmark",
            transcoder.deviceAllowsProfileInstallerAotWrites()
        )
    }

    @Test
    fun deviceAllowsProfileInstallerAotWrites() {
        val transcoder = DeviceProfileWriter(
            assets,
            Diagnostics(),
            PROFILE_LOCATION,
            mTempCurFile!!,
            File("")
        )
        benchmarkRule.measureRepeated {
            transcoder.deviceAllowsProfileInstallerAotWrites()
        }
    }

    @Test
    fun copyProfileOrRead() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
            }.copyProfileOrRead(NeverSkip)
        }
    }

    @Test
    fun transcodeIfNeeded() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
                it.copyProfileOrRead(NeverSkip)
            }
            transcoder.transcodeIfNeeded()
        }
    }

    @Test
    fun writeIfNeeded() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
                it.copyProfileOrRead(NeverSkip)
                it.transcodeIfNeeded()
            }
            transcoder.writeIfNeeded(NeverSkip)
        }
    }

    @Test
    fun fullProfileReadTranscodeWrite() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = DeviceProfileWriter(
                assets,
                Diagnostics(),
                PROFILE_LOCATION,
                mTempCurFile!!,
                File("")
            )
            transcoder.deviceAllowsProfileInstallerAotWrites()

            transcoder.copyProfileOrRead(NeverSkip)
                .transcodeIfNeeded()
                .writeIfNeeded(NeverSkip)
        }
    }

    companion object {
        const val PROFILE_LOCATION = "golden/profileinstaller.prof"
    }

    class Diagnostics : ProfileInstaller.Diagnostics {
        override fun diagnostic(code: Int, data: Any?) {
            /* no-op */
        }

        override fun result(code: Int, data: Any?) {
            /* no-op */
        }
    }

    object NeverSkip : DeviceProfileWriter.SkipStrategy {
        override fun shouldSkip(
            newProfileLength: Long,
            existingProfileState: DeviceProfileWriter.ExistingProfileState
        ) = false
    }
}