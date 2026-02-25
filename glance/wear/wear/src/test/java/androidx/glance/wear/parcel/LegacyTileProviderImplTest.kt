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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_FULLSCREEN
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.legacy.TileAddEventData
import androidx.glance.wear.parcel.legacy.TileRemoveEventData
import androidx.glance.wear.proto.legacy.TileAddEvent
import androidx.glance.wear.proto.legacy.TileRemoveEvent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class LegacyTileProviderImplTest {
    private val context: Context = getApplicationContext()
    private val testScope = TestScope()
    private val providerName = ComponentName("pkg", "cls")
    private val mockWidget = mock<GlanceWearWidget>()
    private val legacyTileProvider =
        LegacyTileProviderImpl(context, providerName, testScope, mockWidget)

    @Test
    fun onTileAddEvent_callsWidgetOnAdded() = runTest {
        val tileId = 123
        val addEvent = TileAddEvent(tile_id = tileId)
        val eventData = TileAddEventData(addEvent.encode(), TileAddEventData.VERSION_PROTOBUF)
        val expectedWidgetHandle =
            ActiveWearWidgetHandle(
                providerName,
                WidgetInstanceId("", tileId),
                CONTAINER_TYPE_FULLSCREEN,
            )

        legacyTileProvider.onTileAddEvent(eventData)
        testScope.advanceUntilIdle()

        verify(mockWidget).onAdded(context, expectedWidgetHandle)
    }

    @Test
    fun onTileRemoveEvent_callsWidgetOnRemoved() = runTest {
        val tileId = 456
        val removeEvent = TileRemoveEvent(tile_id = tileId)
        val eventData =
            TileRemoveEventData(removeEvent.encode(), TileRemoveEventData.VERSION_PROTOBUF)
        val expectedWidgetHandle =
            ActiveWearWidgetHandle(
                providerName,
                WidgetInstanceId("", tileId),
                CONTAINER_TYPE_FULLSCREEN,
            )

        legacyTileProvider.onTileRemoveEvent(eventData)
        testScope.advanceUntilIdle()

        verify(mockWidget).onRemoved(context, expectedWidgetHandle)
    }
}
