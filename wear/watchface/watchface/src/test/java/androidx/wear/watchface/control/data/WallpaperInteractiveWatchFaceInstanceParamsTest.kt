/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.wear.watchface.control.data

import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyle
import java.io.ByteArrayOutputStream
import org.junit.Test

class WallpaperInteractiveWatchFaceInstanceParamsTest {
    @Test
    fun canBeWrittenToOutputStream() {
        val params =
            WallpaperInteractiveWatchFaceInstanceParams(
                "instanceId",
                DeviceConfig(false, false, 10, 10),
                WatchUiState(
                    false,
                    0,
                ),
                UserStyle(emptyMap()).toWireFormat(),
                null,
                null,
                null
            )

        val dummyOutputStream = ByteArrayOutputStream()

        // Should not throw an exception
        ParcelUtils.toOutputStream(params, dummyOutputStream)
    }
}
