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

package androidx.privacysandbox.sdkruntime.core

import android.os.Binder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, codeName = "UpsideDownCakePrivacySandbox")
class AppOwnedSdkSandboxInterfaceCompatTest {

    @Test
    fun converterTest() {
        val converter = AppOwnedInterfaceConverter()

        val compatObj = AppOwnedSdkSandboxInterfaceCompat(
            name = "SDK",
            version = 1,
            binder = Binder()
        )

        val platformObj = converter.toPlatform(compatObj)
        assertThat(platformObj.javaClass.name)
            .isEqualTo("android.app.sdksandbox.AppOwnedSdkSandboxInterface")

        val convertedCompatObj = converter.toCompat(platformObj)

        assertThat(convertedCompatObj.getName()).isEqualTo(compatObj.getName())
        assertThat(convertedCompatObj.getVersion()).isEqualTo(compatObj.getVersion())
        assertThat(convertedCompatObj.getInterface()).isEqualTo(compatObj.getInterface())
    }
}
