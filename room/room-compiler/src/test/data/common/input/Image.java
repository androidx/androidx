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

package foo.bar;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity
@TypeConverters(DateConverter.class)
public class Image {
    @PrimaryKey
    public final int mImageId;
    public final Long mImageYear;
    public final String mArtistInImage;
    public final byte[] mAlbumCover;
    public final Date mDateReleased;
    public final ImageFormat mFormat;

    public Image(int imageId, Long imageYear, String artistInImage, byte[] albumCover,
            Date dateReleased, ImageFormat format) {
        mImageId = imageId;
        mImageYear = imageYear;
        mArtistInImage = artistInImage;
        mAlbumCover = albumCover;
        mDateReleased = dateReleased;
        mFormat = format;
    }
}