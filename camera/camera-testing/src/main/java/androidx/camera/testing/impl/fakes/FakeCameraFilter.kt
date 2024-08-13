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

package androidx.camera.testing.impl.fakes

import androidx.annotation.RestrictTo
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.Identifier

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeCameraFilter(private val id: Identifier = CameraFilter.DEFAULT_ID) : CameraFilter {

    override fun filter(cameraInfos: List<CameraInfo>): List<CameraInfo> {
        return ArrayList(cameraInfos)
    }

    override fun getIdentifier(): Identifier {
        return id
    }
}
