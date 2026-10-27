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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Intent
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearWidgetConfigExtrasTest {

    @Test
    fun fromIntent_withValidExtras_returnsConfigExtras() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                addCategory(WearWidgetConfigExtras.CATEGORY_PROVIDER_CONFIG)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_WIDGET_ID, TEST_WIDGET_ID)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_PROVIDER_COMPONENT, TEST_PROVIDER)
            }

        val configExtras = WearWidgetConfigExtras.fromIntent(intent)

        assertThat(configExtras).isEqualTo(WearWidgetConfigExtras(TEST_INSTANCE_ID, TEST_PROVIDER))
        assertThat(configExtras?.instanceId).isEqualTo(TEST_INSTANCE_ID)
        assertThat(configExtras?.provider).isEqualTo(TEST_PROVIDER)
    }

    @Test
    fun fromIntent_missingCategory_returnsNull() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_WIDGET_ID, TEST_WIDGET_ID)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_PROVIDER_COMPONENT, TEST_PROVIDER)
            }

        assertThat(WearWidgetConfigExtras.fromIntent(intent)).isNull()
    }

    @Test
    fun fromIntent_withoutExtras_returnsNull() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                addCategory(WearWidgetConfigExtras.CATEGORY_PROVIDER_CONFIG)
            }

        assertThat(WearWidgetConfigExtras.fromIntent(intent)).isNull()
    }

    @Test
    fun fromIntent_missingWidgetId_returnsNull() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                addCategory(WearWidgetConfigExtras.CATEGORY_PROVIDER_CONFIG)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_PROVIDER_COMPONENT, TEST_PROVIDER)
            }

        assertThat(WearWidgetConfigExtras.fromIntent(intent)).isNull()
    }

    @Test
    fun fromIntent_invalidWidgetId_returnsNull() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                addCategory(WearWidgetConfigExtras.CATEGORY_PROVIDER_CONFIG)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_WIDGET_ID, -1)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_PROVIDER_COMPONENT, TEST_PROVIDER)
            }

        assertThat(WearWidgetConfigExtras.fromIntent(intent)).isNull()
    }

    @Test
    fun fromIntent_missingProviderComponent_returnsNull() {
        val intent =
            Intent("com.example.ACTION_CONFIGURE_WIDGET").apply {
                addCategory(WearWidgetConfigExtras.CATEGORY_PROVIDER_CONFIG)
                putExtra(WearWidgetConfigExtras.EXTRA_CONFIG_WIDGET_ID, TEST_WIDGET_ID)
            }

        assertThat(WearWidgetConfigExtras.fromIntent(intent)).isNull()
    }

    @Test
    fun equals_and_hashCode() {
        val extras1 = WearWidgetConfigExtras(TEST_INSTANCE_ID, TEST_PROVIDER)
        val extras2 = WearWidgetConfigExtras(TEST_INSTANCE_ID, TEST_PROVIDER)
        val differentId =
            WearWidgetConfigExtras(
                WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, 999),
                TEST_PROVIDER,
            )
        val differentProvider =
            WearWidgetConfigExtras(
                TEST_INSTANCE_ID,
                ComponentName("other.package", "other.package.OtherService"),
            )

        assertThat(extras1).isEqualTo(extras1)
        assertThat(extras1).isEqualTo(extras2)
        assertThat(extras1.hashCode()).isEqualTo(extras2.hashCode())
        assertThat(extras1).isNotEqualTo(differentId)
        assertThat(extras1).isNotEqualTo(differentProvider)
        assertThat(extras1).isNotEqualTo(null)
        assertThat(extras1).isNotEqualTo("not a WearWidgetConfigExtras")
    }

    private companion object {
        const val TEST_WIDGET_ID = 42
        val TEST_INSTANCE_ID =
            WidgetInstanceId(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE, TEST_WIDGET_ID)
        val TEST_PROVIDER = ComponentName("my.package", "my.package.MyWidgetService")
    }
}
