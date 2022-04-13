/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.client

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.RectF
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import org.junit.Test
import org.junit.runner.RunWith
import com.nhaarman.mockitokotlin2.mock
import org.mockito.Mockito.`when`

@RunWith(ClientTestRunner::class)
class WatchFaceMetadataClientTest {
    @Test
    public fun partialComplicationBounds() {
        val mockService = mock<IWatchFaceControlService>()
        val mockServiceConnection = mock<ServiceConnection>()
        val watchFaceName = ComponentName("test.com", "test.com")

        `when`(mockService.apiVersion).thenReturn(3)

        val id = 123
        val shortTextBounds = RectF(0.1f, 0.1f, 0.5f, 0.2f)
        val longTextBounds = RectF(0.1f, 0.3f, 0.9f, 0.5f)

        // Return a ComplicationSlotMetadataWireFormat with partial complicationBounds
        `when`(mockService.getComplicationSlotMetadata(any())).thenReturn(
            arrayOf(
                ComplicationSlotMetadataWireFormat(
                    id,
                    intArrayOf(
                        ComplicationType.SHORT_TEXT.toWireComplicationType(),
                        ComplicationType.LONG_TEXT.toWireComplicationType()
                    ),
                    arrayOf(shortTextBounds, longTextBounds),
                    ComplicationSlotBoundsType.ROUND_RECT,
                    intArrayOf(ComplicationType.SHORT_TEXT.toWireComplicationType()),
                    emptyList(),
                    SystemDataSources.DATA_SOURCE_DATE,
                    ComplicationType.SHORT_TEXT.toWireComplicationType(),
                    ComplicationType.SHORT_TEXT.toWireComplicationType(),
                    ComplicationType.SHORT_TEXT.toWireComplicationType(),
                    false,
                    false,
                    Bundle()
                )
            )
        )

        val client = WatchFaceMetadataClientImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            mockService,
            mockServiceConnection,
            watchFaceName
        )

        // This should not crash.
        val map = client.getComplicationSlotMetadataMap()

        // SHORT_TEXT and LONG_TEXT should match the input
        assertThat(
            map[id]!!.bounds!!.perComplicationTypeBounds[ComplicationType.SHORT_TEXT]
        ).isEqualTo(shortTextBounds)

        assertThat(
            map[id]!!.bounds!!.perComplicationTypeBounds[ComplicationType.LONG_TEXT]
        ).isEqualTo(longTextBounds)

        // All other types should have been backfilled with an empty rect.
        for (type in ComplicationType.values()) {
            if (type != ComplicationType.SHORT_TEXT && type != ComplicationType.LONG_TEXT) {
                assertThat(map[id]!!.bounds!!.perComplicationTypeBounds[type]).isEqualTo(RectF())
            }
        }
    }
}