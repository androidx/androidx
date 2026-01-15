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

package androidx.compose.remote.player.compose.utils

import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType

internal fun RemoteComposeState.getPath(id: Int, start: Float, end: Float): Path {
    val p: Path? = getPath(id) as Path?
    val w: Int = getPathWinding(id)
    if (p != null) {
        return p
    }
    val path = Path()
    val pathData: FloatArray? = getPathData(id)
    if (pathData != null) {
        FloatsToPath.genPath(path, pathData, start, end)
        if (w == 1) {
            path.fillType = PathFillType.EvenOdd
        }
        putPath(id, path)
    }

    return path
}
