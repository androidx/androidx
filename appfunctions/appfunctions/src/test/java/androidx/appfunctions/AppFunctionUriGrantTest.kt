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

import android.content.Intent
import android.net.Uri
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.URI_GRANT_COMPONENTS_METADATA
import androidx.appfunctions.internal.AppFunctionUriGrantTestInventory.Companion.URI_GRANT_OBJECT_TYPE_METADATA
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionUriGrantTest {
    @Test
    fun createAppFunctionUriGrant_withoutAccessFlag_shouldFail() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionUriGrant(
                uri = Uri.parse("content://com.example/1"),
                modeFlags = Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
        }
    }

    @Test
    fun serializeToAppFunctionData_shouldSucceed() {
        val uriGrant =
            AppFunctionUriGrant(
                uri = Uri.parse("content://com.example/1"),
                modeFlags =
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )

        val data = AppFunctionData.serialize(uriGrant, AppFunctionUriGrant::class.java)

        assertThat(data.getAppFunctionData("uri")?.deserialize(Uri::class.java))
            .isEqualTo(Uri.parse("content://com.example/1"))
        assertThat(data.getInt("modeFlags", 0))
            .isEqualTo(
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
    }

    @Test
    fun deserializeFromAppFunctionData_shouldSucceed() {
        val data =
            AppFunctionData.Builder(URI_GRANT_OBJECT_TYPE_METADATA, URI_GRANT_COMPONENTS_METADATA)
                .setAppFunctionData(
                    "uri",
                    AppFunctionData.serialize(Uri.parse("content://com.example/1"), Uri::class.java),
                )
                .setInt(
                    "modeFlags",
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                .build()

        val uriGrant = data.deserialize(AppFunctionUriGrant::class.java)

        assertThat(uriGrant)
            .isEqualTo(
                AppFunctionUriGrant(
                    uri = Uri.parse("content://com.example/1"),
                    modeFlags =
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            )
    }
}
