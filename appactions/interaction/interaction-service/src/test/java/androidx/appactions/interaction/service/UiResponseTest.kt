/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appactions.interaction.service

import android.content.Context
import android.util.SizeF
import android.widget.RemoteViews
import androidx.appactions.interaction.service.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiResponseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteViewsFactoryId = 123

    @Test
    fun uiResponse_remoteViewsBuilder_withFactory_success() {
        val views = RemoteViews(context.packageName, R.layout.remote_view)
        val uiResponse: UiResponse =
            UiResponse.RemoteViewsUiBuilder()
                .setRemoteViews(views, SizeF(10f, 15f))
                .addRemoteViewsFactory(remoteViewsFactoryId, FakeRemoteViewsFactory())
                .build()

        assertThat(uiResponse.tileLayoutInternal).isNull()
        assertThat(uiResponse.remoteViewsInternal?.size?.width).isEqualTo(10)
        assertThat(uiResponse.remoteViewsInternal?.size?.height).isEqualTo(15)
        assertThat(uiResponse.remoteViewsInternal?.remoteViews?.`package`)
            .isEqualTo(context.packageName)
        assertThat(uiResponse.remoteViewsInternal?.collectionViewFactories)
            .containsKey(remoteViewsFactoryId)
    }

    @Test
    fun uiResponse_remoteViewsBuilder_withoutFactory_success() {
        val views = RemoteViews(context.packageName, R.layout.remote_view)
        val uiResponse: UiResponse =
            UiResponse.RemoteViewsUiBuilder().setRemoteViews(views, SizeF(10f, 15f)).build()

        assertThat(uiResponse.tileLayoutInternal).isNull()
        assertThat(uiResponse.remoteViewsInternal?.size?.width).isEqualTo(10)
        assertThat(uiResponse.remoteViewsInternal?.size?.height).isEqualTo(15)
        assertThat(uiResponse.remoteViewsInternal?.remoteViews?.`package`)
            .isEqualTo(context.packageName)
    }

    @Test
    fun uiResponse_remoteViewsBuilder_failure() {
        assertThrows(NullPointerException::class.java) { UiResponse.RemoteViewsUiBuilder().build() }

        // No remote views.
        assertThrows(NullPointerException::class.java) {
            UiResponse.RemoteViewsUiBuilder()
                .addRemoteViewsFactory(remoteViewsFactoryId, FakeRemoteViewsFactory())
                .build()
        }
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun uiResponse_tileLayoutBuilder_success() {
        val layout =
            androidx.wear.tiles.LayoutElementBuilders.Layout.Builder()
                .setRoot(
                    androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                        .addContent(
                            androidx.wear.tiles.LayoutElementBuilders.Column.Builder()
                                .addContent(
                                    androidx.wear.tiles.LayoutElementBuilders.Text.Builder()
                                        .setText("LA8JE92")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        val resources = androidx.wear.tiles.ResourceBuilders.Resources.Builder()
            .setVersion("1234")
            .build()
        val uiResponse: UiResponse =
            UiResponse.TileLayoutBuilder().setTileLayout(layout, resources).build()

        assertThat(uiResponse.remoteViewsInternal).isNull()
        assertThat(uiResponse.tileLayoutInternal?.layout).isNotNull()
        assertThat(uiResponse.tileLayoutInternal?.resources).isNotNull()
        assertThat(uiResponse.tileLayoutInternal?.toProto()?.layout).isNotEmpty()
        assertThat(uiResponse.tileLayoutInternal?.toProto()?.resources).isNotEmpty()
    }

    @Test
    fun uiResponse_tileLayoutBuilder_failure() {
        assertThrows(NullPointerException::class.java) { UiResponse.TileLayoutBuilder().build() }
    }
}
