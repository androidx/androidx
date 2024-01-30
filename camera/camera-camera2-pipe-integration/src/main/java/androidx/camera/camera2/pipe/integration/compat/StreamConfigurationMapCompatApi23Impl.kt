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

package androidx.camera.camera2.pipe.integration.compat

import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.annotation.RequiresApi

@RequiresApi(23)
internal class StreamConfigurationMapCompatApi23Impl(map: StreamConfigurationMap?) :
    StreamConfigurationMapCompatBaseImpl(map) {
    override fun getOutputSizes(format: Int): Array<Size>? {
        return streamConfigurationMap?.getOutputSizes(format)
    }

    override fun getHighResolutionOutputSizes(format: Int): Array<Size>? {
        return streamConfigurationMap?.getHighResolutionOutputSizes(format)
    }
}
