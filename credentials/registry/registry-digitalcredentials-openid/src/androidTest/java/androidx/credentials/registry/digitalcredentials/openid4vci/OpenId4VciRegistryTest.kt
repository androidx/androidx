/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.openid4vci

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OpenId4VciRegistryTest {
    @Test
    fun create_buildsCorrectRegistry() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Act
        val registry =
            OpenId4VciRegistry.create(
                context = context,
                id = "test_id",
                filter = PassFilter(),
                displayData =
                    OpenId4VciDisplayData(
                        entries =
                            listOf(
                                OpenId4VciDisplayData.Entry(
                                    subtitle = "test_subtitle",
                                    explainer =
                                        OpenId4VciDisplayData.Explainer(
                                            perIssuer =
                                                mapOf("https://issuer.my" to "Issuer explainer"),
                                            default = "Default explainer",
                                        ),
                                )
                            ),
                        holderDisplayData =
                            OpenId4VciDisplayData.HolderDisplayData(
                                name = "test_title",
                                icon = byteArrayOf(1, 2, 3),
                            ),
                    ),
                preferredProtocols = listOf("openid4vci-1.0"),
                intentAction = "test_action",
            )

        // Assert
        assertThat(registry.id).isEqualTo("test_id")
        assertThat(registry.intentAction).isEqualTo("test_action")

        val creationOptions = registry.creationOptions
        assertThat(creationOptions.sliceArray(0 until 4)).isEqualTo(byteArrayOf(7, 0, 0, 0))
        assertThat(creationOptions.sliceArray(4 until 7)).isEqualTo(byteArrayOf(1, 2, 3))

        val jsonBytes = creationOptions.sliceArray(7 until creationOptions.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        assertThat(json.getString("entry_id")).isEqualTo("test_id")
        assertThat(json.getJSONArray("preferred_protocols").getString(0))
            .isEqualTo("openid4vci-1.0")

        val entries = json.getJSONArray("entries")
        assertThat(entries.length()).isEqualTo(1)
        val entry = entries.getJSONObject(0)
        assertThat(entry.has("title")).isFalse()
        assertThat(entry.has("icon")).isFalse()
        assertThat(entry.getString("subtitle")).isEqualTo("test_subtitle")

        val explainer = entry.getJSONObject("explainer")
        assertThat(explainer.getString("default")).isEqualTo("Default explainer")
        val perIssuer = explainer.getJSONObject("per_issuer")
        assertThat(perIssuer.getString("https://issuer.my")).isEqualTo("Issuer explainer")

        val packageInfo = json.getJSONObject("self_declared_package_info")
        assertThat(packageInfo.getString("name")).isEqualTo("test_title")
        val iconRange = packageInfo.getJSONArray("icon")
        assertThat(iconRange.getInt(0)).isEqualTo(4)
        assertThat(iconRange.getInt(1)).isEqualTo(7)
    }

    @Test
    fun create_withDefaultPreferredProtocols() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry =
            OpenId4VciRegistry.create(
                context = context,
                id = "test_id",
                filter = PassFilter(),
                displayData =
                    OpenId4VciDisplayData(
                        entries =
                            listOf(
                                OpenId4VciDisplayData.Entry(
                                    subtitle = "test_subtitle",
                                    explainer =
                                        OpenId4VciDisplayData.Explainer(
                                            perIssuer =
                                                mapOf("https://issuer.my" to "Issuer explainer"),
                                            default = "Default explainer",
                                        ),
                                )
                            ),
                        holderDisplayData =
                            OpenId4VciDisplayData.HolderDisplayData(
                                name = "test_title",
                                icon = byteArrayOf(1, 2, 3),
                            ),
                    ),
                intentAction = "test_action",
            )

        val creationOptions = registry.creationOptions
        assertThat(creationOptions.sliceArray(0 until 4)).isEqualTo(byteArrayOf(7, 0, 0, 0))
        assertThat(creationOptions.sliceArray(4 until 7)).isEqualTo(byteArrayOf(1, 2, 3))

        val jsonBytes = creationOptions.sliceArray(7 until creationOptions.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        assertThat(json.getString("entry_id")).isEqualTo("test_id")
        assertThat(json.has("preferred_protocols")).isFalse()

        val entries = json.getJSONArray("entries")
        assertThat(entries.length()).isEqualTo(1)
        val entry = entries.getJSONObject(0)
        assertThat(entry.has("title")).isFalse()
        assertThat(entry.has("icon")).isFalse()
        assertThat(entry.getString("subtitle")).isEqualTo("test_subtitle")

        val explainer = entry.getJSONObject("explainer")
        assertThat(explainer.getString("default")).isEqualTo("Default explainer")
        val perIssuer = explainer.getJSONObject("per_issuer")
        assertThat(perIssuer.getString("https://issuer.my")).isEqualTo("Issuer explainer")

        val packageInfo = json.getJSONObject("self_declared_package_info")
        assertThat(packageInfo.getString("name")).isEqualTo("test_title")
    }

    @Test
    fun create_withNullDisplayData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry =
            OpenId4VciRegistry.create(
                context = context,
                id = "test_id",
                filter = PassFilter(),
                displayData = null,
                intentAction = "test_action",
            )

        val creationOptions = registry.creationOptions
        val jsonOffset =
            ByteBuffer.wrap(creationOptions.sliceArray(0 until 4))
                .order(ByteOrder.LITTLE_ENDIAN)
                .int

        val jsonBytes = creationOptions.sliceArray(jsonOffset until creationOptions.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        assertThat(json.getString("entry_id")).isEqualTo("test_id")
        val entries = json.getJSONArray("entries")
        assertThat(entries.length()).isEqualTo(1)
        val entry = entries.getJSONObject(0)
        assertThat(entry.length()).isEqualTo(0) // Should be empty

        val packageInfo = json.getJSONObject("package_info")
        val expectedName = context.applicationInfo.loadLabel(context.packageManager).toString()
        assertThat(packageInfo.getString("name")).isEqualTo(expectedName)

        val hasIcon = jsonOffset > 4
        assertThat(packageInfo.has("icon")).isEqualTo(hasIcon)
        if (hasIcon) {
            val iconRange = packageInfo.getJSONArray("icon")
            assertThat(iconRange.getInt(0)).isEqualTo(4)
            assertThat(iconRange.getInt(1)).isEqualTo(jsonOffset)
        }
    }

    @Test
    fun create_withEmptyEntriesButHolderInfo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val iconBytes = byteArrayOf(1, 2, 3)
        val registry =
            OpenId4VciRegistry.create(
                context = context,
                id = "test_id",
                filter = PassFilter(),
                displayData =
                    OpenId4VciDisplayData(
                        entries = emptyList(),
                        holderDisplayData =
                            OpenId4VciDisplayData.HolderDisplayData(
                                name = "Holder",
                                icon = iconBytes,
                            ),
                    ),
                intentAction = "test_action",
            )

        val creationOptions = registry.creationOptions
        assertThat(creationOptions.sliceArray(0 until 4)).isEqualTo(byteArrayOf(7, 0, 0, 0))
        assertThat(creationOptions.sliceArray(4 until 7)).isEqualTo(iconBytes)

        val jsonBytes = creationOptions.sliceArray(7 until creationOptions.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        val entries = json.getJSONArray("entries")
        assertThat(entries.length()).isEqualTo(1)
        val entry = entries.getJSONObject(0)
        assertThat(entry.has("title")).isFalse()
        assertThat(entry.has("icon")).isFalse()
        assertThat(entry.has("subtitle")).isFalse()
        assertThat(entry.has("explainer")).isFalse()

        val packageInfo = json.getJSONObject("self_declared_package_info")
        assertThat(packageInfo.getString("name")).isEqualTo("Holder")
        val iconRange = packageInfo.getJSONArray("icon")
        assertThat(iconRange.getInt(0)).isEqualTo(4)
        assertThat(iconRange.getInt(1)).isEqualTo(7)
    }

    @Test
    fun create_withMultipleEntriesAndHolderInfo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val iconBytes = byteArrayOf(1, 2, 3)
        val registry =
            OpenId4VciRegistry.create(
                context = context,
                id = "test_id",
                filter = PassFilter(),
                displayData =
                    OpenId4VciDisplayData(
                        entries =
                            listOf(
                                OpenId4VciDisplayData.Entry(subtitle = "sub1"),
                                OpenId4VciDisplayData.Entry(
                                    subtitle = "sub2",
                                    explainer = OpenId4VciDisplayData.Explainer(default = "default"),
                                ),
                            ),
                        holderDisplayData =
                            OpenId4VciDisplayData.HolderDisplayData(
                                name = "Holder",
                                icon = iconBytes,
                            ),
                    ),
                intentAction = "test_action",
            )

        val creationOptions = registry.creationOptions
        assertThat(creationOptions.sliceArray(0 until 4)).isEqualTo(byteArrayOf(7, 0, 0, 0))
        assertThat(creationOptions.sliceArray(4 until 7)).isEqualTo(iconBytes)

        val jsonBytes = creationOptions.sliceArray(7 until creationOptions.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        val entries = json.getJSONArray("entries")
        assertThat(entries.length()).isEqualTo(2)

        val entry1 = entries.getJSONObject(0)
        assertThat(entry1.has("title")).isFalse()
        assertThat(entry1.has("icon")).isFalse()
        assertThat(entry1.getString("subtitle")).isEqualTo("sub1")
        assertThat(entry1.has("explainer")).isFalse()

        val entry2 = entries.getJSONObject(1)
        assertThat(entry2.has("title")).isFalse()
        assertThat(entry2.has("icon")).isFalse()
        assertThat(entry2.getString("subtitle")).isEqualTo("sub2")
        val explainer2 = entry2.getJSONObject("explainer")
        assertThat(explainer2.getString("default")).isEqualTo("default")

        val packageInfo = json.getJSONObject("self_declared_package_info")
        assertThat(packageInfo.getString("name")).isEqualTo("Holder")
        val iconRange = packageInfo.getJSONArray("icon")
        assertThat(iconRange.getInt(0)).isEqualTo(4)
        assertThat(iconRange.getInt(1)).isEqualTo(7)
    }

    @Test
    fun explainer_empty_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { OpenId4VciDisplayData.Explainer() }
    }

    @Test
    fun explainer_blankIssuer_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(perIssuer = mapOf(" " to "explainer"))
        }
    }

    @Test
    fun explainer_blankText_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(perIssuer = mapOf("https://issuer.com" to ""))
        }
    }

    @Test
    fun explainer_blankDefault_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = "  ")
        }
    }
}
