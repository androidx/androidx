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

import androidx.compose.remote.core.RcPlatformServices

/** Implelentation of [RcPlatformServices] intended for multi-platform use. */
public class JvmRcPlatformServices : RcPlatformServices {
    override fun imageToByteArray(image: Any): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getImageWidth(image: Any): Int {
        TODO("Not yet implemented")
    }

    override fun getImageHeight(image: Any): Int {
        TODO("Not yet implemented")
    }

    override fun isAlpha8Image(image: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun pathToFloatArray(path: Any): FloatArray? {
        if (path is RemotePath) {
            return path.createFloatArray()
        }
        return null
    }

    override fun parsePath(pathData: String): Any {
        return RemotePath(pathData)
    }

    override fun log(category: RcPlatformServices.LogCategory, message: String) {}
}
