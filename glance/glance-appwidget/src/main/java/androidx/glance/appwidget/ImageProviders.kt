/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.net.Uri
import androidx.glance.ImageProvider

internal class UriImageProvider(val uri: Uri) : ImageProvider {
    override fun toString() = "UriImageProvider(uri='$uri')"
}

/**
 * Image resource from a URI.
 *
 * @param uri The URI of the image to be displayed.
 */
fun ImageProvider(uri: Uri): ImageProvider = UriImageProvider(uri)
