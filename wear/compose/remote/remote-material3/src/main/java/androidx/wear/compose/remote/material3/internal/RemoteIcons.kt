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

package androidx.wear.compose.remote.material3.internal

import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

internal object RemoteIcons {
    internal val Add: RemoteImageVector
        get() {
            if (_add != null) {
                return _add!!
            }
            _add =
                remoteMaterialIcon(name = "Add") {
                    path(fill = SolidColor(Color.White)) {
                        moveTo(11f.rf, 13f.rf)
                        lineTo(6f.rf, 13f.rf)
                        quadTo(5.575f.rf, 13f.rf, 5.2875f.rf, 12.7125f.rf)
                        quadTo(5f.rf, 12.425f.rf, 5f.rf, 12f.rf)
                        quadTo(5f.rf, 11.575f.rf, 5.2875f.rf, 11.2875f.rf)
                        quadTo(5.575f.rf, 11f.rf, 6f.rf, 11f.rf)
                        lineTo(11f.rf, 11f.rf)
                        lineTo(11f.rf, 6f.rf)
                        quadTo(11f.rf, 5.575f.rf, 11.2875f.rf, 5.2875f.rf)
                        quadTo(11.575f.rf, 5f.rf, 12f.rf, 5f.rf)
                        quadTo(12.425f.rf, 5f.rf, 12.7125f.rf, 5.2875f.rf)
                        quadTo(13f.rf, 5.575f.rf, 13f.rf, 6f.rf)
                        lineTo(13f.rf, 11f.rf)
                        lineTo(18f.rf, 11f.rf)
                        quadTo(18.425f.rf, 11f.rf, 18.7125f.rf, 11.2875f.rf)
                        quadTo(19f.rf, 11.575f.rf, 19f.rf, 12f.rf)
                        quadTo(19f.rf, 12.425f.rf, 18.7125f.rf, 12.7125f.rf)
                        quadTo(18.425f.rf, 13f.rf, 18f.rf, 13f.rf)
                        lineTo(13f.rf, 13f.rf)
                        lineTo(13f.rf, 18f.rf)
                        quadTo(13f.rf, 18.425f.rf, 12.7125f.rf, 18.7125f.rf)
                        quadTo(12.425f.rf, 19f.rf, 12f.rf, 19f.rf)
                        quadTo(11.575f.rf, 19f.rf, 11.2875f.rf, 18.7125f.rf)
                        quadTo(11f.rf, 18.425f.rf, 11f.rf, 18f.rf)
                        lineTo(11f.rf, 13f.rf)
                        close()
                    }
                }
            return _add!!
        }

    private var _add: RemoteImageVector? = null

    internal val Remove: RemoteImageVector
        get() {
            if (_remove != null) {
                return _remove!!
            }
            _remove =
                remoteMaterialIcon(name = "Remove") {
                    path(fill = SolidColor(Color.White)) {
                        moveTo(6f.rf, 13f.rf)
                        quadTo(5.575f.rf, 13f.rf, 5.2875f.rf, 12.7125f.rf)
                        quadTo(5f.rf, 12.425f.rf, 5f.rf, 12f.rf)
                        quadTo(5f.rf, 11.575f.rf, 5.2875f.rf, 11.2875f.rf)
                        quadTo(5.575f.rf, 11f.rf, 6f.rf, 11f.rf)
                        lineTo(18f.rf, 11f.rf)
                        quadTo(18.425f.rf, 11f.rf, 18.7125f.rf, 11.2875f.rf)
                        quadTo(19f.rf, 11.575f.rf, 19f.rf, 12f.rf)
                        quadTo(19f.rf, 12.425f.rf, 18.7125f.rf, 12.7125f.rf)
                        quadTo(18.425f.rf, 13f.rf, 18f.rf, 13f.rf)
                        lineTo(6f.rf, 13f.rf)
                        close()
                    }
                }
            return _remove!!
        }

    private var _remove: RemoteImageVector? = null
}

private fun remoteMaterialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: RemoteImageVector.Builder.() -> RemoteImageVector.Builder,
): RemoteImageVector =
    RemoteImageVector.Builder(
            name = name,
            viewportWidth = MaterialIconDimension.rf,
            viewportHeight = MaterialIconDimension.rf,
            tintColor = RemoteColor(Color.White),
            autoMirror = autoMirror,
        )
        .block()
        .build()

private const val MaterialIconDimension = 24f
