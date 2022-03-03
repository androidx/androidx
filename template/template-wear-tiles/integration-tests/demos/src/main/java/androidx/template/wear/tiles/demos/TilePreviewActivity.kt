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

package androidx.template.wear.tiles.demos

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.widget.FrameLayout
import androidx.wear.tiles.manager.TileUiClient

/** Activity view displays [DemoTile] UI */
class TilePreviewActivity : Activity() {
    lateinit var tileUiClient: TileUiClient

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)
        setContentView(R.layout.activity_preview)

        tileUiClient = TileUiClient(
            context = this,
            component = ComponentName(this, DemoTile::class.java),
            parentView = findViewById<FrameLayout>(R.id.tile_container)
        )
        tileUiClient.connect()
    }

    override fun onDestroy() {
        tileUiClient.close()
        super.onDestroy()
    }
}
