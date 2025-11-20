/*
 * Copyright 2025 The Android Open Source Project
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

package flaggedapi

import android.flagging.FlaggedApiContainer
import androidx.annotation.RequiresAconfigFlag
import androidx.core.flagging.Flags

@Suppress("unused")
class FlaggedUsageInOutline {
    fun testWithCheck() {
        if (Flags.getBooleanFlagValue("flaggedapi", "myFlag")) {
            FlagFlaggedApiImpl.innerApi()
        }
    }

    fun testWithoutCheck() {
        FlagFlaggedApiImpl.innerApi()
    }

    @RequiresAconfigFlag("flaggedapi.myFlag")
    object FlagFlaggedApiImpl {
        fun innerApi(): Boolean {
            return FlaggedApiContainer.innerApi()
        }
    }
}
