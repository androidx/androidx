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
package androidx.room.integration.kotlintestapp.vo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
class Image(
    @PrimaryKey val mImageId: Int,
    val mImageYear: Long,
    val mArtistInImage: String,
    val mAlbumCover: ByteArray,
    val mDateReleased: Date,
    format: ImageFormat
) {
    val mFormat: ImageFormat

    init {
        mFormat = format
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val image = other as Image
        if (mImageId != image.mImageId) return false
        return mArtistInImage == image.mArtistInImage
    }

    override fun hashCode(): Int {
        var result = mImageId
        result = (31 * result + mImageYear).toInt()
        result = 31 * result + mArtistInImage.hashCode()
        return result
    }
}
