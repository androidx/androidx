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

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks as Camera2DeviceQuirks
import androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk as Camera2ExtraCroppingQuirk
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks as PipeDeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ExtraCroppingQuirk as PipeExtraCroppingQuirk
import androidx.camera.core.CameraInfo
import androidx.camera.core.UseCase
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.StopCodecAfterSurfaceRemovalCrashMediaServerQuirk
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue

@RequiresApi(21)
fun assumeExtraCroppingQuirk(implName: String) {
    assumeFalse(
        "Devices in ExtraCroppingQuirk will get a fixed resolution regardless of any settings",
        hasExtraCroppingQuirk(implName)
    )
}

@RequiresApi(21)
fun hasExtraCroppingQuirk(implName: String): Boolean {
    return (implName.contains(CameraPipeConfig::class.simpleName!!) &&
        PipeDeviceQuirks[PipeExtraCroppingQuirk::class.java] != null) ||
        Camera2DeviceQuirks.get(Camera2ExtraCroppingQuirk::class.java) != null
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

@RequiresApi(21)
fun getRotationNeeded(
    videoCapture: VideoCapture<Recorder>,
    cameraInfo: CameraInfo
) = cameraInfo.getSensorRotationDegrees(videoCapture.targetRotation)

@RequiresApi(21)
fun verifyVideoResolution(context: Context, file: File, expectedResolution: Size) {
    MediaMetadataRetriever().useAndRelease {
        it.setDataSource(context, Uri.fromFile(file))
        assertThat(it.getRotatedResolution()).isEqualTo(expectedResolution)
    }
}

@RequiresApi(21)
fun isStreamSharingEnabled(useCase: UseCase) = !useCase.camera!!.hasTransform

@RequiresApi(21)
fun isSurfaceProcessingEnabled(videoCapture: VideoCapture<*>) =
    videoCapture.node != null || isStreamSharingEnabled(videoCapture)
