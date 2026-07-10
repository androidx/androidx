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

package androidx.appfunctions

import androidx.appfunctions.internal.AppFunctionTextResourceTestInventory.Companion.TEXT_RESOURCE_COMPONENTS_METADATA
import androidx.appfunctions.internal.AppFunctionTextResourceTestInventory.Companion.TEXT_RESOURCE_OBJECT_TYPE_METADATA
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionTextResourceTest {
    @Test
    fun serializeToAppFunctionData_shouldSucceed() {
        val textResource = AppFunctionTextResource(mimeType = "text/plain", content = "Hello World")

        val data = AppFunctionData.serialize(textResource, AppFunctionTextResource::class.java)

        assertThat(data.getString("mimeType")).isEqualTo("text/plain")
        assertThat(data.getString("content")).isEqualTo("Hello World")
    }

    @Test
    fun deserializeFromAppFunctionData_shouldSucceed() {
        val data =
            AppFunctionData.Builder(
                    TEXT_RESOURCE_OBJECT_TYPE_METADATA,
                    TEXT_RESOURCE_COMPONENTS_METADATA,
                )
                .setString("mimeType", "text/plain")
                .setString("content", "Hello World")
                .build()

        val textResource = data.deserialize(AppFunctionTextResource::class.java)

        assertThat(textResource)
            .isEqualTo(AppFunctionTextResource(mimeType = "text/plain", content = "Hello World"))
    }
}
