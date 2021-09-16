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

import android.annotation.SuppressLint
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
    private val APK_NAME = "base.apk"

    @Before
    fun setupTempDir() {
        mTempCurFile = File.createTempFile("ProfileInstallerTranscodeBenchmark", ".prof")
    }

    @After
    fun rmTempFile() {
        mTempCurFile?.delete()
    }

    @SuppressLint("NewApi")
    private inline fun BenchmarkRule.Scope.newTranscoderUntimed(
        block: (DeviceProfileWriter) -> Unit
    ): DeviceProfileWriter {
        var transcoder: DeviceProfileWriter? = null
        runWithTimingDisabled {
            transcoder = DeviceProfileWriter(
                assets,
                Runnable::run,
                Diagnostics(),
                APK_NAME,
                PROFILE_LOCATION,
                PROFILE_META_LOCATION,
                mTempCurFile!!
            ).also(block)
        }
        return transcoder!!
    }

    @SuppressLint("NewApi")
    private fun assumeDeviceSupportsAot() {
        val transcoder = DeviceProfileWriter(
            assets,
            Runnable::run,
            Diagnostics(),
            APK_NAME,
            PROFILE_LOCATION,
            PROFILE_META_LOCATION,
            mTempCurFile!!
        )
        assumeTrue(
            "Device must support AOT to run this benchmark",
            transcoder.deviceAllowsProfileInstallerAotWrites()
        )
    }

    @Test
    @SuppressLint("NewApi")
    fun deviceAllowsProfileInstallerAotWrites() {
        val transcoder = DeviceProfileWriter(
            assets,
            Runnable::run,
            Diagnostics(),
            APK_NAME,
            PROFILE_LOCATION,
            PROFILE_META_LOCATION,
            mTempCurFile!!
        )
        benchmarkRule.measureRepeated {
            transcoder.deviceAllowsProfileInstallerAotWrites()
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun copyProfileOrRead() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
            }.read()
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun transcodeIfNeeded() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
                it.read()
            }
            transcoder.transcodeIfNeeded()
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun writeIfNeeded() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = newTranscoderUntimed {
                it.deviceAllowsProfileInstallerAotWrites()
                it.read()
                it.transcodeIfNeeded()
            }
            transcoder.write()
        }
    }

    @Test
    @SuppressLint("NewApi")
    fun fullProfileReadTranscodeWrite() {
        assumeDeviceSupportsAot()
        benchmarkRule.measureRepeated {
            val transcoder = DeviceProfileWriter(
                assets,
                Runnable::run,
                Diagnostics(),
                APK_NAME,
                PROFILE_LOCATION,
                PROFILE_META_LOCATION,
                mTempCurFile!!
            )
            transcoder.deviceAllowsProfileInstallerAotWrites()

            transcoder.read()
                .transcodeIfNeeded()
                .write()
        }
    }

    companion object {
        const val PROFILE_LOCATION = "golden/profileinstaller.prof"
        const val PROFILE_META_LOCATION = "golden/profileinstaller.profm"
    }

    class Diagnostics : ProfileInstaller.DiagnosticsCallback {
        override fun onDiagnosticReceived(code: Int, data: Any?) {
            /* no-op */
        }

        override fun onResultReceived(code: Int, data: Any?) {
            /* no-op */
        }
    }
}
