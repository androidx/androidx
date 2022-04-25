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

package androidx.camera.camera2.internal.util

import android.hardware.camera2.CameraDevice
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.RequestProcessor

/**
 * An implementation / builder for RequestProcessor.Request
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class RequestProcessorRequest(
    private val targetOutputConfigIds: List<Int>,
    private val parameters: Config,
    private val templateId: Int
) : RequestProcessor.Request {
    override fun getTargetOutputConfigIds(): List<Int> {
        return targetOutputConfigIds
    }

    override fun getParameters(): Config {
        return parameters
    }

    override fun getTemplateId(): Int {
        return templateId
    }

    class Builder {
        private var targetOutputConfigIds: MutableList<Int> = ArrayList()
        private var parameters: Config = OptionsBundle.emptyBundle()
        private var templateId = CameraDevice.TEMPLATE_PREVIEW

        fun addTargetOutputConfigId(targetOutputConfigId: Int): Builder {
            targetOutputConfigIds.add(targetOutputConfigId)
            return this
        }

        fun setParameters(parameters: Config): Builder {
            this.parameters = parameters
            return this
        }

        fun setTemplateId(templateId: Int): Builder {
            this.templateId = templateId
            return this
        }

        fun build(): RequestProcessorRequest {
            return RequestProcessorRequest(
                targetOutputConfigIds.toList(),
                OptionsBundle.from(parameters),
                templateId
            )
        }
    }
}
