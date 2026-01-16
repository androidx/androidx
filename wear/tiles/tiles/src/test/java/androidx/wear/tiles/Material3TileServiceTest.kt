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
package androidx.wear.tiles

import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.Color.YELLOW
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.layout.androidImageResource
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.backgroundImage
import androidx.wear.protolayout.material3.imageButton
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Material3TileServiceTest {
    @Test
    fun onTileRequest_returnsTileFromM3Provider() {
        var layoutElement: LayoutElement = EMPTY_BOX
        val service: TileService =
            object : Material3TileService() {
                // Needed for dynamic theme which is a default on
                override fun getContentResolver(): ContentResolver? =
                    ApplicationProvider.getApplicationContext<Context>().contentResolver

                override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
                    layoutElement = layoutElement()
                    return Tile.Builder()
                        .setTileTimeline(Timeline.fromLayoutElement(layoutElement))
                        .build()
                }
            }
        val tileFuture = service.onTileRequest(REQUEST_PARAMS)
        val tile = tileFuture.get()
        assertThat(tile.tileTimeline!!.timelineEntries).hasSize(1)
        assertThat(tile.tileTimeline!!.timelineEntries[0].layout!!.root!!.toLayoutElementProto())
            .isEqualTo(layoutElement.toLayoutElementProto())
    }

    @Test
    fun onTileRequest_returnsTile_withCustomThemeFromM3Provider() {
        val service: TileService =
            object :
                Material3TileService(
                    defaultColorScheme = ColorScheme(primary = YELLOW.argb),
                    allowDynamicTheme = false,
                ) {
                override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile =
                    Tile.Builder()
                        .setTileTimeline(Timeline.fromLayoutElement(layoutElement()))
                        .build()
            }
        val tileFuture = service.onTileRequest(REQUEST_PARAMS)
        val tile = tileFuture.get()
        val provider =
            LayoutElementAssertionsProvider(tile.tileTimeline!!.timelineEntries[0].layout!!.root!!)
        provider.onElement(hasText(TEST_TEXT).and(hasColor(Color.YELLOW))).assertExists()
    }

    private companion object {
        private val DEVICE_CONFIG =
            DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build()
        // Default ProtoLayoutScope is added automatically
        private val REQUEST_PARAMS =
            TileRequest.Builder().setDeviceConfiguration(DEVICE_CONFIG).build()
        private val EMPTY_BOX = Box.Builder().build()
        private const val TEST_TEXT = "Test"

        private fun MaterialScope.layoutElement() =
            primaryLayout(
                mainSlot = {
                    imageButton(
                        onClick = clickable(),
                        backgroundContent = {
                            backgroundImage(
                                resource = imageResource(androidImage = androidImageResource(1))
                            )
                        },
                    )
                },
                titleSlot = { text(text = TEST_TEXT.layoutString, color = colorScheme.primary) },
            )
    }
}
