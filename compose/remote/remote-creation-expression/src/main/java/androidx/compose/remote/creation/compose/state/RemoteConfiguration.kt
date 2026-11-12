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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteConfiguration {
    public val density: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_DENSITY)

    public val fontSizePx: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_FONT_SIZE)

    public val fontSize: RemoteDp
        get() = (fontSizePx / density).asRemoteDp()

    public val windowWidthPx: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_WINDOW_WIDTH)

    public val windowWidth: RemoteDp
        get() = (windowWidthPx / density).asRemoteDp()

    public val windowHeightPx: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_WINDOW_HEIGHT)

    public val windowHeight: RemoteDp
        get() = (windowHeightPx / density).asRemoteDp()
}
