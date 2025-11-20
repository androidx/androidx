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
import androidx.glance.wear.parcel.WidgetUpdateClient
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GlanceWearWidgetTest {

    @Test
    fun triggerUpdate_clientRequestsUpdateForAll() {
        val mockUpdateClient = mock<WidgetUpdateClient>()
        val widget = TestWidget(mockUpdateClient)
        val componentName = ComponentName("my.package", "my.package.MyClass")

        widget.triggerUpdate(getApplicationContext(), componentName)

        verify(mockUpdateClient).requestUpdate(any(), eq(componentName))
    }

    @Suppress("RestrictedApiAndroidX")
    private class TestWidget(updateClient: WidgetUpdateClient) : GlanceWearWidget(updateClient) {

        override suspend fun provideWidgetContent(context: Context, request: WearWidgetRequest) =
            WearWidgetContent {
                RemoteText("Testing...")
            }
    }
}
