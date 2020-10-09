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
import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.MIME_TYPE
import android.provider.MediaStore.Images.Media.DATE_TAKEN
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.entities.ContentAccessMediaStore.Image
import androidx.contentaccess.entities.ContentAccessMediaStore.Video
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentDelete
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class ContentDeleteTest {

    val contentResolver: ContentResolver =
        InstrumentationRegistry.getInstrumentation().context.contentResolver
    private val imageAccessor = ContentAccess.getAccessor(ImageAccessor::class, contentResolver)

    @JvmField
    @Rule
    var storagePermissions =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )!!

    @Before
    fun setup() {
        val imageValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "title1")
            put(MediaStore.Images.Media.DESCRIPTION, "description1")
            put(MIME_TYPE, "image/jpeg")
            put(DATE_TAKEN, 1000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)

        imageValues.apply {
            put(MediaStore.Images.Media.TITLE, "title2")
            put(MediaStore.Images.Media.DESCRIPTION, "description2")
            put(MIME_TYPE, "image/jpeg")
            put(DATE_TAKEN, 2000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)

        imageValues.apply {
            put(MediaStore.Images.Media.TITLE, "title3")
            put(MediaStore.Images.Media.DESCRIPTION, "description3")
            put(MIME_TYPE, "image/png")
            put(DATE_TAKEN, 3000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)

        val videoValues = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, "title4")
            put(MediaStore.Video.Media.DESCRIPTION, "description4")
            put(MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_TAKEN, 4000L)
        }
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)
    }

    @After
    fun deleteAllAdded() {
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "", null)
        contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "", null)
    }

    @ContentAccessObject(Image::class)
    interface ImageAccessor {
        @ContentDelete
        fun deleteAll(): Int

        @ContentDelete
        suspend fun deleteAllSuspend(): Int

        @ContentDelete(where = "$MIME_TYPE != 'image/png'")
        fun deleteItemsWithWhereArgument(): Int

        @ContentDelete(where = "$DATE_TAKEN < :date")
        fun deleteItemsWithWhereParameter(date: Long): Int

        @ContentDelete(contentEntity = Video::class)
        fun deleteItemsFromDifferentEntity(): Int

        @ContentDelete(uri = "content://media/external/images/media")
        fun deleteItemsWithUriArgument(): Int
    }

    @Test
    fun testDeleteAllItems() {
        val beforeDeletionCursor = contentResolver.query(
            MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.TITLE), null, null, null
        )!!
        assertThat(beforeDeletionCursor.count).isEqualTo(3)
        assertThat(imageAccessor.deleteAll()).isEqualTo(3)
        val afterDeletionCursor = contentResolver.query(
            MediaStore.Images.Media
                .EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.TITLE), null, null, null
        )!!
        assertThat(afterDeletionCursor.count).isEqualTo(0)
    }

    @Test
    fun testDeleteAllItemsSuspend() {
        runBlocking {
            assertThat(imageAccessor.deleteAllSuspend()).isEqualTo(3)
        }
    }

    @Test
    fun testDeleteItemsWithWhereArgument() {
        assertThat(imageAccessor.deleteItemsWithWhereArgument()).isEqualTo(2)
    }

    @Test
    fun testDeleteItemsWithWhereParameter() {
        assertThat(imageAccessor.deleteItemsWithWhereParameter(date = 3000L)).isEqualTo(2)
    }

    @Test
    fun testDeleteItemsWithUriArgument() {
        assertThat(imageAccessor.deleteItemsWithUriArgument()).isEqualTo(3)
    }
}