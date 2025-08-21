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

package androidx.compose.remote.creation

import android.graphics.Bitmap
import androidx.compose.remote.core.Platform
import androidx.compose.remote.creation.profile.PlatformProfile
import androidx.compose.remote.creation.profile.Profile

class RemoteComposeContextAndroid : RemoteComposeContext {

    val painter: Painter
        get() {
            if (mRemoteWriter !is RemoteComposeWriterAndroid) {
                throw (Exception("This RemoteComposeContext is not an Android one"))
            }
            return (mRemoteWriter as RemoteComposeWriterAndroid).painter
        }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        platform: Platform,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(width, height, contentDescription, platform)) {
        content()
    }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        apiLevel: Int,
        profiles: Int,
        platform: Platform,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(
        RemoteComposeWriterAndroid(width, height, contentDescription, apiLevel, profiles, platform)
    ) {
        content()
    }

    constructor(
        width: Int,
        height: Int,
        contentDescription: String,
        profile: Profile = PlatformProfile.ANDROIDX,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(width, height, contentDescription, profile) {
        content()
    }

    constructor(
        platform: Platform,
        vararg tags: RemoteComposeWriter.HTag,
        content: RemoteComposeContextAndroid.() -> Unit,
    ) : super(RemoteComposeWriterAndroid(platform, *tags)) {
        content()
    }

    fun addBitmap(image: Bitmap): Int {
        return mRemoteWriter.addBitmap(image)
    }

    public fun drawBitmap(image: Bitmap, contentDescription: String) {
        mRemoteWriter.drawBitmap(image, image.width, image.height, contentDescription)
    }

    public fun createCirclePath(x: Float, y: Float, rad: Float): RemotePath {
        return RemotePath.createCirclePath(mRemoteWriter, x, y, rad)
    }
}
