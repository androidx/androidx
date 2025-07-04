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

package androidx.appfunctions.internal.serializableproxies

import android.net.Uri
import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertIs

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AppFunctionUriTest {

    @Test
    fun toAndroidUri() {
        val uriString = "https://www.google.com/"
        val appFunctionUri = AppFunctionUri(uriString)

        val resultAndroidUri = appFunctionUri.toUri()

        assertIs<Uri>(resultAndroidUri)
        assertThat(resultAndroidUri.toString()).isEqualTo(uriString)
    }

    @Test
    fun fromAndroidUri() {
        val uriString = "https://www.google.com/"
        val androidUri = Uri.parse(uriString)

        val resultAppFunctionUri = AppFunctionUri.fromUri(androidUri)

        assertIs<AppFunctionUri>(resultAppFunctionUri)
        assertThat(resultAppFunctionUri.uri).isEqualTo(uriString)
    }
}
