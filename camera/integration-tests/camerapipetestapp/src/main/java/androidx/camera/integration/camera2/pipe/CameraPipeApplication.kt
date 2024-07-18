/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe

import android.app.Application
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.camera.camera2.pipe.CameraPipe
import kotlin.system.measureNanoTime

class CameraPipeApplication : Application() {
    private val _cameraPipe = lazy {
        Trace.beginSection("CXCP-App#cameraPipe")
        var result: CameraPipe?
        val time = measureNanoTime {
            result = CameraPipe(CameraPipe.Config(appContext = this))
        }
        Log.i("CXCP-App", "Configured CameraPipe in ${time.formatNanoTime()}")
        Trace.endSection()
        return@lazy result!!
    }
    val cameraPipe: CameraPipe
        get() = _cameraPipe.value

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            val now = SystemClock.elapsedRealtime()
            val elapsedRealtime = Process.getStartElapsedRealtime()
            val time = now - elapsedRealtime
            Log.i("CXCP-App", "Application (${Process.myPid()}) created in $time ms")
        } else {
            Log.i("CXCP-App", "Application (${Process.myPid()}) created.")
        }
    }
}
