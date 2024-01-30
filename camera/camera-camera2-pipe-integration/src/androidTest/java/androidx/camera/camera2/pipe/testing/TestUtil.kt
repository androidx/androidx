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

package androidx.camera.camera2.pipe.testing

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
fun CameraControl.toCameraControlAdapter(): CameraControlAdapter {
    return ((this as CameraControlInternal).implementation) as CameraControlAdapter
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
fun CameraInfo.toCameraInfoAdapter(): CameraInfoAdapter {
    return ((this as CameraInfoInternal).implementation) as CameraInfoAdapter
}
