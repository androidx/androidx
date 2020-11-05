/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.integration.testapp

import android.Manifest
import android.content.ContentValues
import android.provider.MediaStore
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.entities.ContentAccessMediaStore
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Rule
import org.junit.Test

@MediumTest
class MediaStoreEntitiesTest {

    val contentResolver = InstrumentationRegistry.getInstrumentation().context.contentResolver
    val accessor = ContentAccess.getAccessor(MediaStoreAccessor::class, contentResolver)

    @After
    fun deleteAllAdded() {
        contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.MIME_TYPE + "=?", arrayOf("image/jpeg")
        )
    }

    @JvmField
    @Rule
    var storagePermissions =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission
                .WRITE_EXTERNAL_STORAGE
        )!!

    @ContentAccessObject
    @Suppress("deprecation")
    interface MediaStoreAccessor {
        @ContentQuery(contentEntity = ContentAccessMediaStore.Image::class)
        fun getImage(): ContentAccessMediaStore.Image?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Video::class)
        fun getVideo(): ContentAccessMediaStore.Video?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Audio::class)
        fun getAudio(): ContentAccessMediaStore.Audio?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Download::class)
        fun getDownload(): ContentAccessMediaStore.Download?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Artist::class)
        fun getArtist(): ContentAccessMediaStore.Artist?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Album::class)
        fun getAlbum(): ContentAccessMediaStore.Album?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Genre::class)
        fun getGenre(): ContentAccessMediaStore.Genre?

        @ContentQuery(contentEntity = ContentAccessMediaStore.ImageThumbnail::class)
        fun getImageThumbnail(): ContentAccessMediaStore.ImageThumbnail?

        @ContentQuery(contentEntity = ContentAccessMediaStore.VideoThumbnail::class)
        fun getVideoThumbnail(): ContentAccessMediaStore.VideoThumbnail?

        @ContentQuery(contentEntity = ContentAccessMediaStore.Playlist::class)
        fun getPlaylist(): ContentAccessMediaStore.Playlist?

        @ContentQuery(contentEntity = ContentAccessMediaStore.File::class, uri = ":uri")
        fun getFile(uri: String): ContentAccessMediaStore.File?
    }

    @Test
    fun testEntities() {
        // The results should be empty, but we want to make sure we are querying the correct uris
        // and the correct columns on all API levels, as we should get an error if something
        // is mal-defined.
        accessor.getImage()
        accessor.getVideo()
        accessor.getAudio()
        accessor.getArtist()
        accessor.getAlbum()
        accessor.getGenre()
        accessor.getImageThumbnail()
        accessor.getVideoThumbnail()
        accessor.getPlaylist()
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testApi29IntroducedEntities() {
        accessor.getDownload()
    }

    @Test
    fun testFileEntity() {
        val contentValues = ContentValues()
        // Media type is image, so should be cleaned up.
        contentValues.put("media_type", 1)
        contentValues.put("_data", "random")
        val uri = MediaStore.Files.getContentUri("external")
        contentResolver.insert(uri, contentValues)
        accessor.getFile(uri.toString())
    }
}