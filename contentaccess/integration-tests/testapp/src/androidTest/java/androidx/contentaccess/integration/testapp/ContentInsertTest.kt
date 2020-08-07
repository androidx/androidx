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
import android.net.Uri
import android.provider.MediaStore
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.entities.ContentAccessMediaStore.Image
import androidx.contentaccess.entities.ContentAccessMediaStore.Video
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentInsert
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
@MediumTest
class ContentInsertTest {

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

    @After
    fun deleteAllAdded() {
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "", null)
        contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "", null)
    }

    @ContentAccessObject(Image::class)
    interface ImageAccessor {
        @ContentInsert
        fun insertItem(image: Image): Uri?
        @ContentInsert
        suspend fun insertItemSuspend(image: Image): Uri?
        @ContentInsert(uri = "content://media/external/images/media")
        fun insertItemWithUriArgument(image: ImageNoUri): Uri?
        @ContentInsert
        fun insertDifferentItem(video: Video): Uri?
    }

    @Test
    fun testInsertItem() {
        val newItem = Image()
        newItem.title = "Title1"
        newItem.description = "Description1"
        newItem.mimeType = "image/png"
        newItem.dateTaken = 1000
        val newUri = imageAccessor.insertItem(image = newItem)!!
        val cursor = contentResolver.query(newUri, null, null, null, null)!!
        val titleIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE)
        val descriptionIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION)
        val mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)
        val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
        cursor.moveToFirst()
        assertThat(cursor.getString(titleIndex)).isEqualTo("Title1")
        assertThat(cursor.getString(descriptionIndex)).isEqualTo("Description1")
        assertThat(cursor.getString(mimeTypeIndex)).isEqualTo("image/png")
        assertThat(cursor.getInt(dateTakenIndex)).isEqualTo(1000)
    }

    @Test
    fun testInsertItemSuspend() {
        val newItem = Image()
        newItem.title = "Title1"
        newItem.description = "Description1"
        newItem.mimeType = "image/png"
        newItem.dateTaken = 1000
        runBlocking {
            assertThat(imageAccessor.insertItem(image = newItem)).isNotNull()
        }
    }

    @Test
    fun testInsertItemWithUriArgument() {
        val newItem = ImageNoUri(
            iD = null,
            title = "Title1",
            description = "Description1",
            mimeType = "image/png",
            dateAdded = null,
            dateTaken = null
        )
        val newUri = imageAccessor.insertItemWithUriArgument(image = newItem)!!
        assertThat(
            newUri.toString().startsWith(prefix = "content://media/external/images/media")
        ).isEqualTo(true)
    }

    @Test
    fun testInsertDifferentItem() {
        val newVideo = Video()
        newVideo.title = "Title1"

        val newUri = imageAccessor.insertDifferentItem(video = newVideo)!!
        val cursor = contentResolver.query(newUri, null, null, null, null)!!
        val titleIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE)
        cursor.moveToFirst()
        assertThat(cursor.getString(titleIndex)).isEqualTo("Title1")
    }
}
