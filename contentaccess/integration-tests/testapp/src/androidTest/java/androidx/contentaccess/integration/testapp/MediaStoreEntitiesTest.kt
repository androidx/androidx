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
import androidx.contentaccess.entities.MediaStore.Image
import androidx.contentaccess.entities.MediaStore.Video
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
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.MIME_TYPE + "=?", arrayOf("image/jpeg"))
    }

    @JvmField
    @Rule
    var storagePermissions =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission
            .WRITE_EXTERNAL_STORAGE)!!

    @ContentAccessObject
    @Suppress("deprecation")
    interface MediaStoreAccessor {
        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Image::class)
        fun getImage(): androidx.contentaccess.entities.MediaStore.Image?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Video::class)
        fun getVideo(): androidx.contentaccess.entities.MediaStore.Video?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Audio::class)
        fun getAudio(): androidx.contentaccess.entities.MediaStore.Audio?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Download::class)
        fun getDownload(): androidx.contentaccess.entities.MediaStore.Download?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Artist::class)
        fun getArtist(): androidx.contentaccess.entities.MediaStore.Artist?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Album::class)
        fun getAlbum(): androidx.contentaccess.entities.MediaStore.Album?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Genre::class)
        fun getGenre(): androidx.contentaccess.entities.MediaStore.Genre?

        @ContentQuery(contentEntity =
            androidx.contentaccess.entities.MediaStore.ImageThumbnail::class)
        fun getImageThumbnail(): androidx.contentaccess.entities.MediaStore.ImageThumbnail?

        @ContentQuery(contentEntity =
            androidx.contentaccess.entities.MediaStore.VideoThumbnail::class)
        fun getVideoThumbnail(): androidx.contentaccess.entities.MediaStore.VideoThumbnail?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.Playlist::class)
        fun getPlaylist(): androidx.contentaccess.entities.MediaStore.Playlist?

        @ContentQuery(contentEntity = androidx.contentaccess.entities.MediaStore.File::class,
            uri = ":uri")
        fun getFile(uri: String): androidx.contentaccess.entities.MediaStore.File?
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