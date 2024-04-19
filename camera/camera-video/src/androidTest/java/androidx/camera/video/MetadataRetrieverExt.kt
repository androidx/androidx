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

package androidx.camera.video

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
import android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION
import android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.util.Rational
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rotateSize

fun MediaMetadataRetriever.useAndRelease(block: (MediaMetadataRetriever) -> Unit) {
    try {
        block(this)
    } finally {
        release()
    }
}

fun MediaMetadataRetriever.hasAudio(): Boolean = extractMetadata(METADATA_KEY_HAS_AUDIO) == "yes"

fun MediaMetadataRetriever.hasVideo(): Boolean = extractMetadata(METADATA_KEY_HAS_VIDEO) == "yes"

fun MediaMetadataRetriever.getDurationMs(): Long = extractMetadata(METADATA_KEY_DURATION)!!.toLong()

fun MediaMetadataRetriever.getRotation(): Int =
    extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0

fun MediaMetadataRetriever.getWidth(): Int = extractMetadata(METADATA_KEY_VIDEO_WIDTH)!!.toInt()

fun MediaMetadataRetriever.getHeight(): Int = extractMetadata(METADATA_KEY_VIDEO_HEIGHT)!!.toInt()

@RequiresApi(21)
fun MediaMetadataRetriever.getResolution(): Size = Size(getWidth(), getHeight())

@RequiresApi(21)
fun MediaMetadataRetriever.getRotatedResolution(): Size = rotateSize(getResolution(), getRotation())

@RequiresApi(21)
fun MediaMetadataRetriever.getAspectRatio(): Rational = Rational(getWidth(), getHeight())

@RequiresApi(21)
fun MediaMetadataRetriever.getRotatedAspectRatio(): Rational = if (is90or270(getRotation()))
    Rational(getHeight(), getWidth()) else getAspectRatio()

fun MediaMetadataRetriever.getMimeType(): String = extractMetadata(METADATA_KEY_MIMETYPE)!!

fun MediaMetadataRetriever.getLocation(): String = extractMetadata(METADATA_KEY_LOCATION)!!
