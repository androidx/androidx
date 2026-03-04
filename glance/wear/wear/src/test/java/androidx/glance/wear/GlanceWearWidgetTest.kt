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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.ui.graphics.Color
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GlanceWearWidgetTest {

    @Test
    fun triggerUpdate_clientRequestsUpdateForAll() {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient)

        widget.triggerUpdate(getApplicationContext(), TEST_COMPONENT)

        verify(mockUpdateClient).requestUpdate(any(), eq(TEST_COMPONENT))
    }

    @Test
    fun triggerUpdate_debuggable_sendsUpdateBroadcast() {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient)
        val context = getApplicationContext<Context>()
        context.applicationInfo.flags =
            context.applicationInfo.flags or android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE

        widget.triggerUpdate(context, TEST_COMPONENT)

        verify(mockUpdateClient).sendUpdateBroadcast(any(), eq(TEST_COMPONENT))
    }

    @Test
    fun triggerUpdate_notDebuggable_doesNotSendUpdateBroadcast() {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient)
        val context = getApplicationContext<Context>()
        context.applicationInfo.flags =
            context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv()

        widget.triggerUpdate(context, TEST_COMPONENT)

        verify(mockUpdateClient, never()).sendUpdateBroadcast(any(), eq(TEST_COMPONENT))
    }

    private class TestWidget(updateClient: WidgetUpdateClient) : GlanceWearWidget(updateClient) {

        override suspend fun provideWidgetData(context: Context, params: WearWidgetParams) =
            WearWidgetDocument(backgroundColor = Color.Transparent) { RemoteText("Testing...") }
    }

    private companion object {
        val TEST_COMPONENT = ComponentName("my.package", "my.package.MyClass")
    }
}
