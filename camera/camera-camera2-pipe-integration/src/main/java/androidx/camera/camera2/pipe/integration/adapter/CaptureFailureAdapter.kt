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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CaptureFailure
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.compat.AndroidCaptureFailure
import androidx.camera.core.impl.CameraCaptureFailure

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CaptureFailureAdapter(
    private val requestFailure: RequestFailure,
) : CameraCaptureFailure(Reason.ERROR) {
    override fun getCaptureFailure(): CaptureFailure =
        (requestFailure as AndroidCaptureFailure).unwrapAs(CaptureFailure::class)!!
}
