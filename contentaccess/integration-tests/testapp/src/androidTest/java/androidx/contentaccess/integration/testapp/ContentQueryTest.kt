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

import com.google.common.truth.Truth.assertThat

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.DESCRIPTION
import android.provider.MediaStore.Images.Media.TITLE
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.entities.ContentAccessMediaStore.Image
import androidx.contentaccess.entities.ContentAccessMediaStore.Video
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentQuery
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Optional
import kotlinx.coroutines.runBlocking

@MediumTest
class ContentQueryTest {

    val contentResolver =
        InstrumentationRegistry.getInstrumentation().context.contentResolver
    val imageAccessor = ContentAccess.getAccessor(ImageAccessor::class, contentResolver)

    @JvmField
    @Rule
    var storagePermissions =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission
                .WRITE_EXTERNAL_STORAGE
        )!!

    @Before
    fun setup() {
        val imageValues = ContentValues().apply {
            put(TITLE, "title1")
            put(DESCRIPTION, "description1")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 100000)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)

        imageValues.apply {
            put(TITLE, "title2")
            put(DESCRIPTION, "description2")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 2000)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)
    }

    @After
    fun deleteAllAdded() {
        // TODO(obenabde): create a rule that results in specified URIs being cleared
        //   after tests (and asserted cleared.)
        // For the tests to work properly in terms of the expected results, the provider should not
        // have prior rows, so make sure we delete everything at the end of the tests for proper
        // local testing.
        contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.MIME_TYPE + "=?", arrayOf("image/jpeg")
        )
        contentResolver.delete(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            "", null
        )
    }

    @ContentAccessObject(Image::class)
    interface ImageAccessor {
        @ContentQuery
        fun getSingleEntity(): Image?

        @ContentQuery
        suspend fun getSingleEntitySuspend(): Image?

        @ContentQuery(contentEntity = Video::class)
        fun getSingleVideo(): Video?

        @ContentQuery
        fun getSingleEntityAsOptional(): Optional<Image>

        @ContentQuery
        suspend fun getSingleEntityAsOptionalSuspend(): Optional<Image>

        @ContentQuery
        fun getAllEntitiesAsList(): List<Image>

        @ContentQuery
        suspend fun getAllEntitiesAsListSuspend(): List<Image>

        @ContentQuery
        fun getAllEntitiesAsSet(): Set<Image>

        @ContentQuery
        suspend fun getAllEntitiesAsSetSuspend(): Set<Image>

        @ContentQuery
        fun getSinglePojo(): TitleDescriptionPojo?

        @ContentQuery
        fun getSingleAnnotatedPojo(): TitleDescriptionAnnotatedPojo?

        @ContentQuery
        fun getSinglePojoAsOptional(): Optional<TitleDescriptionPojo>

        @ContentQuery
        fun getAllPojosAsList(): List<TitleDescriptionPojo>

        @ContentQuery
        fun getAllPojosAsSet(): Set<TitleDescriptionPojo>

        @ContentQuery(projection = arrayOf(TITLE))
        fun getSingleColumn(): String?

        @ContentQuery(projection = arrayOf(TITLE))
        fun getSingleColumnOptional(): Optional<String>

        @ContentQuery(projection = arrayOf(TITLE))
        fun getSingleColumnList(): List<String>

        @ContentQuery(projection = arrayOf(TITLE))
        fun getSingleColumnSet(): Set<String>

        @ContentQuery(projection = arrayOf(TITLE))
        fun getPartiallyFilledPojo(): TitlePlusUnrelatedFieldPojo?

        @ContentQuery
        fun getPublicFieldsPojo(): PublicFieldsPojo?

        @ContentQuery
        fun getPojoWithIgnoredConstructor(): JavaPojoWithIgnoredConstructor?

        @ContentQuery(selection = "$TITLE = 'title2'")
        fun getPojoMatchingSelectionNoSelectionArgs(): List<TitleDescriptionPojo>

        @ContentQuery(selection = "$TITLE = :titleArg and $DESCRIPTION = :descriptionArg")
        fun getPojoMatchingSelectionWithSelectionArgs(
            titleArg: String,
            descriptionArg: String
        ): List<TitleDescriptionPojo>

        @ContentQuery(orderBy = arrayOf("$TITLE desc"))
        fun getAllOrderByTitleDescending(): List<TitleDescriptionPojo>

        @ContentQuery(orderBy = arrayOf("$TITLE asc"))
        fun getAllOrderByTitleAscending(): List<TitleDescriptionPojo>

        @ContentQuery(orderBy = arrayOf("$TITLE asc", "$DESCRIPTION desc"))
        fun getAllOrderByTitleAscDescriptionDesc(): List<TitleDescriptionPojo>
    }

    @ContentAccessObject(ImageNoUri::class)
    interface ImageAccessorEntityWithNoUri {
        @ContentQuery(uri = "content://media/external/images/media")
        fun getSingleElement(): ImageNoUri?

        @ContentQuery(uri = ":uriArg")
        fun getSingleElementWithArgumentUri(uriArg: String): ImageNoUri?
    }

    @ContentAccessObject
    interface ImageAccessorWithNoEntity {
        @ContentQuery(contentEntity = Image::class)
        fun getSingleElement(): Image?
    }

    data class TitleDescriptionPojo(val title: String?, val description: String?)

    data class TitlePlusUnrelatedFieldPojo(val title: String?, val unrelated: String?)

    data class TitleDescriptionAnnotatedPojo(
        @ContentColumn("title") val theTitle: String?,
        @ContentColumn("description") val theDescription: String?
    )

    @Test
    fun testUseUriInAnnotation() {
        val imageAccessorEntityWithNoUri = ContentAccess.getAccessor(
            ImageAccessorEntityWithNoUri::class, contentResolver
        )
        assertThat(imageAccessorEntityWithNoUri.getSingleElement()!!.title).isEqualTo("title1")
    }

    @Test
    fun testUseUriInArgument() {
        val imageAccessorEntityWithNoUri = ContentAccess.getAccessor(
            ImageAccessorEntityWithNoUri::class, contentResolver
        )
        assertThat(
            imageAccessorEntityWithNoUri
                .getSingleElementWithArgumentUri("content://media/external/images/media")!!.title
        )
            .isEqualTo("title1")
    }

    @Test
    fun testUseEntityInAnnotation() {
        val imageAccessorWithNoEntity = ContentAccess.getAccessor(
            ImageAccessorWithNoEntity::class, contentResolver
        )
        assertThat(imageAccessorWithNoEntity.getSingleElement()!!.title).isEqualTo("title1")
    }

    @Test
    fun testPrioritizesContentEntityInAnnotation() {
        val videoValues = ContentValues()
        videoValues.put(MediaStore.Video.Media.TITLE, "videotitle")
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoValues)
        assertThat(imageAccessor.getSingleVideo()!!.title).isEqualTo("videotitle")
    }

    @Test
    fun testGetsSingleEntityAndPopulatesProperly() {
        val result = imageAccessor.getSingleEntity()!!
        assertThat(result.title).isEqualTo("title1")
        assertThat(result.description).isEqualTo("description1")
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.dateTaken).isEqualTo(100000)
    }

    @Test
    fun testSuspendingGetSingleEntityAndPopulatesProperly() {
        runBlocking {
            val result = imageAccessor.getSingleEntitySuspend()!!
            assertThat(result.title).isEqualTo("title1")
            assertThat(result.description).isEqualTo("description1")
            assertThat(result.mimeType).isEqualTo("image/jpeg")
            assertThat(result.dateTaken).isEqualTo(100000)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testGetsSingleEntityAsOptional() {
        assertThat(imageAccessor.getSingleEntityAsOptional().get().title).isEqualTo("title1")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSuspendingGetSingleEntityAsOptional() {
        runBlocking {
            assertThat(imageAccessor.getSingleEntityAsOptionalSuspend().get().title)
                .isEqualTo("title1")
        }
    }

    @Test
    fun testGetAllEntitiesAsList() {
        assertThat(imageAccessor.getAllEntitiesAsList().size).isEqualTo(2)
    }

    @Test
    fun testSuspendingGetAllEntitiesAsList() {
        runBlocking {
            assertThat(imageAccessor.getAllEntitiesAsListSuspend().size).isEqualTo(2)
        }
    }

    // This test is somehow flaky in build server... makes no sense given that it does the same
    // thing as testGetAllEntitiesAsList or testSuspendingGetAllEntitiesAsSet but :shrug:...
    // TODO(obenabde): figure out why this is flaky.
    // @Test
    fun testGetAllEntitiesAsSet() {
        assertThat(imageAccessor.getAllEntitiesAsSet().size).isEqualTo(2)
    }

    @Test
    fun testSuspendingGetAllEntitiesAsSet() {
        runBlocking {
            assertThat(imageAccessor.getAllEntitiesAsSetSuspend().size).isEqualTo(2)
        }
    }

    @Test
    fun testGetSinglePojoAndPopulatesProperly() {
        val result = imageAccessor.getSinglePojo()!!
        assertThat(result.title).isEqualTo("title1")
        assertThat(result.description).isEqualTo("description1")
    }

    @Test
    fun testGetSingleAnnotatedPojoAndPopulatesProperly() {
        val result = imageAccessor.getSingleAnnotatedPojo()!!
        assertThat(result.theTitle).isEqualTo("title1")
        assertThat(result.theDescription).isEqualTo("description1")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testGetSinglePojoAsOptional() {
        assertThat(imageAccessor.getSinglePojoAsOptional().get().title).isEqualTo("title1")
    }

    @Test
    fun testGetAllPojosAsList() {
        assertThat(imageAccessor.getAllPojosAsList().size).isEqualTo(2)
    }

    @Test
    fun testGetAllPojosAsSet() {
        assertThat(imageAccessor.getAllPojosAsSet().size).isEqualTo(2)
    }

    @Test
    fun testGetSingleColumn() {
        assertThat(imageAccessor.getSingleColumn()).isEqualTo("title1")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testGetSingleColumnOptional() {
        assertThat(imageAccessor.getSinglePojoAsOptional().get().title).isEqualTo("title1")
    }

    @Test
    fun testGetSingleColumnList() {
        assertThat(imageAccessor.getAllPojosAsList().size).isEqualTo(2)
    }

    @Test
    fun testGetSingleColumnsSet() {
        assertThat(imageAccessor.getAllPojosAsSet().size).isEqualTo(2)
    }

    @Test
    fun testGetPartiallyPopulatedPojo() {
        val result = imageAccessor.getPartiallyFilledPojo()!!
        assertThat(result.title).isEqualTo("title1")
        assertThat(result.unrelated).isNull()
    }

    @Test
    fun testGetPojoWithPublicFields() {
        val result = imageAccessor.getPublicFieldsPojo()!!
        assertThat(result.title).isEqualTo("title1")
        assertThat(result.theDescription).isEqualTo("description1")
    }

    @Test
    fun testGetJavaPojoWithIgnoredConstructor() {
        val result = imageAccessor.getPojoWithIgnoredConstructor()!!
        assertThat(result.mTitle).isEqualTo("title1")
        assertThat(result.mDescription).isEqualTo("description1")
    }

    @Test
    fun testProperlyQueriesWithSelectionAndNoSelectionArgs() {
        val result = imageAccessor.getPojoMatchingSelectionNoSelectionArgs()
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().description).isEqualTo("description2")
    }

    @Test
    fun testProperlyQueriesWithSelectionAndSelectionArgs() {
        val result = imageAccessor.getPojoMatchingSelectionWithSelectionArgs(
            "title2",
            "description2"
        )
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().description).isEqualTo("description2")
    }

    @Test
    fun testOrderBySingleField() {
        val result1 = imageAccessor.getAllOrderByTitleDescending()
        assertThat(result1.size).isEqualTo(2)
        assertThat(result1.first().title).isEqualTo("title2")
        val result2 = imageAccessor.getAllOrderByTitleAscending()
        assertThat(result2.size).isEqualTo(2)
        assertThat(result2.first().title).isEqualTo("title1")
    }

    @Test
    fun testOrderByMultipleFields() {
        // Delete all the added ones
        contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.MIME_TYPE + "=?", arrayOf("image/jpeg")
        )

        val values = ContentValues().apply {
            put(TITLE, "title1")
            put(DESCRIPTION, "description1")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 1000000000000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        values.apply {
            put(TITLE, "title1")
            put(DESCRIPTION, "description2")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 2000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        values.apply {
            put(TITLE, "title2")
            put(DESCRIPTION, "description1")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 1000000000000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        values.apply {
            put(TITLE, "title2")
            put(DESCRIPTION, "description2")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, 2000L)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val result = imageAccessor.getAllOrderByTitleAscDescriptionDesc()
        assertThat(result.size).isEqualTo(4)

        assertThat(result.get(0).title).isEqualTo("title1")
        assertThat(result.get(0).description).isEqualTo("description2")

        assertThat(result.get(1).title).isEqualTo("title1")
        assertThat(result.get(1).description).isEqualTo("description1")

        assertThat(result.get(2).title).isEqualTo("title2")
        assertThat(result.get(2).description).isEqualTo("description2")

        assertThat(result.get(3).title).isEqualTo("title2")
        assertThat(result.get(3).description).isEqualTo("description1")
    }
}