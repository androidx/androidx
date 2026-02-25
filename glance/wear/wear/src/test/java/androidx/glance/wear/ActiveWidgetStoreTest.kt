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
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.wire.Duration
import com.squareup.wire.Instant
import kotlin.test.Test
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ActiveWidgetStoreTest {

    private val getCurrentTimeInstant: () -> Instant = mock {
        on { invoke() } doReturn Instant.now()
    }
    private val activeWidgetStore: ActiveWidgetStore =
        ActiveWidgetStore(ApplicationProvider.getApplicationContext(), getCurrentTimeInstant)

    @After
    fun tearDown() {
        activeWidgetStore.markWidgetAsInactive(COMPONENT1, 1)
        activeWidgetStore.markWidgetAsInactive(COMPONENT2, 2)
    }

    @Test
    fun markWidgetAsActive_marksWidgetAsActive() {
        activeWidgetStore.markWidgetAsActive(COMPONENT1, 1)

        val widgets = activeWidgetStore.getActiveWidgets()
        assertThat(widgets.size).isEqualTo(1)
        assertThat(widgets[0].instanceId.id).isEqualTo(1)
        assertThat(widgets[0].instanceId.namespace)
            .isEqualTo(WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE)
        assertThat(widgets[0].provider).isEqualTo(COMPONENT1)
        assertThat(widgets[0].containerType).isEqualTo(ContainerInfo.CONTAINER_TYPE_FULLSCREEN)
    }

    @Test
    fun markWidgetAsInactive_marksWidgetAsInactive() {
        activeWidgetStore.markWidgetAsActive(COMPONENT1, 1)

        activeWidgetStore.markWidgetAsInactive(COMPONENT1, 1)

        val widgets = activeWidgetStore.getActiveWidgets()
        assertThat(widgets.size).isEqualTo(0)
    }

    @Test
    fun getActiveWidgetsApi33_cleansUpInactiveWidgets() {
        activeWidgetStore.markWidgetAsActive(COMPONENT1, 1)
        whenever(getCurrentTimeInstant.invoke()) doReturn Instant.now().plus(Duration.ofDays(61))

        val widgets = activeWidgetStore.getActiveWidgets()

        assertThat(widgets.size).isEqualTo(0)
    }

    private companion object {
        val COMPONENT1: ComponentName = ComponentName("pkg", "cls1")
        val COMPONENT2: ComponentName = ComponentName("pkg", "cls2")
    }
}
