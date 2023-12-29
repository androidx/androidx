/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks as Camera2DeviceQuirks
import androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk as Camera2ExtraCroppingQuirk
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks as PipeDeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ExtraCroppingQuirk as PipeExtraCroppingQuirk
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.StopCodecAfterSurfaceRemovalCrashMediaServerQuirk
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue

@RequiresApi(21)
fun assumeExtraCroppingQuirk(implName: String) {
    val msg = "Devices in ExtraCroppingQuirk will get a fixed resolution regardless of any settings"
    if (implName.contains(CameraPipeConfig::class.simpleName!!)) {
        assumeTrue(msg, PipeDeviceQuirks[PipeExtraCroppingQuirk::class.java] == null)
    } else {
        assumeTrue(msg, Camera2DeviceQuirks.get(Camera2ExtraCroppingQuirk::class.java) == null)
    }
}

@RequiresApi(21)
fun assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk() {
    // Skip for b/293978082. For tests that will unbind the VideoCapture before stop the recording,
    // they should be skipped since media server will crash if the codec surface has been removed
    // before MediaCodec.stop() is called.
    assumeTrue(
        DeviceQuirks.get(StopCodecAfterSurfaceRemovalCrashMediaServerQuirk::class.java) == null
    )
}

@RequiresApi(21)
fun assumeSuccessfulSurfaceProcessing() {
    // Skip for b/253211491
    assumeFalse(
        "Skip tests for Cuttlefish API 30 eglCreateWindowSurface issue",
        Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 30
    )
}
