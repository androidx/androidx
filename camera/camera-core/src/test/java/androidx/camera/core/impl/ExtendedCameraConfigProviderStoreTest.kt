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

package androidx.camera.core.impl

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExtendedCameraConfigProviderStoreTest {

    @Test
    public fun canRetrieveStoredCameraConfigProvider() {
        val id = Object()
        val cameraConfigProvider =
            CameraConfigProvider { _, _ -> CameraConfigs.emptyConfig() }

        ExtendedCameraConfigProviderStore.addConfig(id, cameraConfigProvider)

        assertThat(ExtendedCameraConfigProviderStore.getConfigProvider(id))
            .isEqualTo(cameraConfigProvider)
    }

    @Test
    public fun returnDefaultEmptyCameraConfigProvider_whenNoDataStored() {
        assertThat(ExtendedCameraConfigProviderStore.getConfigProvider(Object())).isEqualTo(
            CameraConfigProvider.EMPTY
        )
    }
}
