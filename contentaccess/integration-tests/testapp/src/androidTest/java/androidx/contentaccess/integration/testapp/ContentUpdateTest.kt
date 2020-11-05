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

import android.provider.ContactsContract.Contacts.DISPLAY_NAME
import android.provider.ContactsContract.Contacts.DISPLAY_NAME_SOURCE
import android.provider.ContactsContract.Contacts.CONTENT_URI
import androidx.contentaccess.ContentAccess
import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentUpdate
import androidx.test.filters.MediumTest
import kotlinx.coroutines.runBlocking
import org.junit.Test

@MediumTest
class ContentUpdateTest : ContactsBasedTest() {

    val contactsAccessor = ContentAccess.getAccessor(ContactsAccessor::class, contentResolver)

    @ContentAccessObject(Contact::class)
    interface ContactsAccessor {

        @ContentUpdate
        fun updateDisplayNamesAndDisplayNamesPrimary(
            @ContentColumn(DISPLAY_NAME) displayName: String,
            @ContentColumn(DISPLAY_NAME_SOURCE) displayNameSource: String
        ): Int

        @ContentUpdate
        fun suspendingUpdateDisplayNamesAndDisplayNamesPrimary(
            @ContentColumn(DISPLAY_NAME) displayName: String,
            @ContentColumn(DISPLAY_NAME_SOURCE) displayNameSource: String
        ): Int

        @ContentUpdate(where = "$DISPLAY_NAME = \"displayName1\"")
        fun updateDisplayNameWithWhereConditionNoSelectionArgs(
            @ContentColumn(DISPLAY_NAME) displayName: String
        ): Int

        @ContentUpdate(where = "$DISPLAY_NAME = :displayNameArg")
        fun updateDisplayNameWithWhereConditionAndSelectionArgs(
            @ContentColumn(DISPLAY_NAME) displayName: String,
            displayNameArg: String
        ): Int
    }

    @ContentAccessObject(ContactNoUri::class)
    interface ContactsAccessorEntityWithNoUri {
        @ContentUpdate(uri = "content://com.android.contacts/raw_contacts")
        fun updateAllDisplayNames(
            @ContentColumn(DISPLAY_NAME) displayName: String
        ): Int

        @ContentUpdate(uri = ":uriArg")
        fun updateDisplayNameWithUriArgument(
            @ContentColumn(DISPLAY_NAME) displayName: String,
            uriArg: String
        ): Int
    }

    // TODO(obenabde): add a test for overriding content entity through annotation parameter.

    @ContentAccessObject
    interface ContactsAccessorWithNoEntity {
        @ContentUpdate(contentEntity = Contact::class)
        fun updateDisplayName(@ContentColumn(DISPLAY_NAME) displayName: String): Int
    }

    @Test
    fun testUseUriInAnnotation() {
        val contactsAccessorEntityWithNoUri = ContentAccess.getAccessor(
            ContactsAccessorEntityWithNoUri::class, contentResolver
        )
        assertThat(contactsAccessorEntityWithNoUri.updateAllDisplayNames("updated-displayName"))
            .isEqualTo(2)
        val cursor = contentResolver.query(
            CONTENT_URI, arrayOf(DISPLAY_NAME), null, null, null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-displayName")
    }

    @Test
    fun testUseUriInArgument() {
        val contactsAccessorEntityWithNoUri = ContentAccess.getAccessor(
            ContactsAccessorEntityWithNoUri::class, contentResolver
        )
        assertThat(
            contactsAccessorEntityWithNoUri.updateDisplayNameWithUriArgument(
                "updated-displayName",
                "content://com.android.contacts/raw_contacts"
            )
        ).isEqualTo(2)
        val cursor = contentResolver.query(
            CONTENT_URI, arrayOf(DISPLAY_NAME), null, null, null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-displayName")
    }

    @Test
    fun testUseEntityInAnnotation() {
        val contactsAccessorWithNoEntity = ContentAccess.getAccessor(
            ContactsAccessorWithNoEntity::class, contentResolver
        )
        assertThat(
            contactsAccessorWithNoEntity
                .updateDisplayName("updated-description")
        ).isEqualTo(2)
        val cursor = contentResolver.query(
            CONTENT_URI,
            arrayOf(
                DISPLAY_NAME
            ),
            null, null, null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-description")
    }

    @Test
    fun testUpdatesAllColumns() {
        assertThat(
            contactsAccessor.updateDisplayNamesAndDisplayNamesPrimary(
                "updated-display-name",
                "updated-display-name-source"
            )
        )
            .isEqualTo(2)
        val cursor = contentResolver.query(
            CONTENT_URI,
            arrayOf(
                DISPLAY_NAME, DISPLAY_NAME_SOURCE
            ),
            null, null, null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-display-name")
        assertThat(cursor.getString(1)).isEqualTo("updated-display-name-source")
    }

    @Test
    fun testSuspendingUpdatesAllColumns() {
        runBlocking {
            assertThat(
                contactsAccessor.suspendingUpdateDisplayNamesAndDisplayNamesPrimary
                (
                    "updated-display-name",
                    "updated-display-name-source"
                )
            )
                .isEqualTo(2)
            val cursor = contentResolver.query(
                CONTENT_URI,
                arrayOf(
                    DISPLAY_NAME, DISPLAY_NAME_SOURCE
                ),
                null, null, null
            )!!
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("updated-display-name")
            assertThat(cursor.getString(1)).isEqualTo("updated-display-name-source")
        }
    }

    @Test
    fun testUpdatesColumnsWithSelection() {
        assertThat(
            contactsAccessor.updateDisplayNameWithWhereConditionNoSelectionArgs(
                "updated-display-name"
            )
        ).isEqualTo(1)
        val cursor = contentResolver.query(
            CONTENT_URI,
            arrayOf(
                DISPLAY_NAME
            ),
            null, null, null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-display-name")
    }

    @Test
    fun testUpdatesColumnsWithSelectionAndSelectionArgs() {
        assertThat(
            contactsAccessor.updateDisplayNameWithWhereConditionAndSelectionArgs(
                "updated-display-name",
                "displayName2"
            )
        ).isEqualTo(1)
        val cursor = contentResolver.query(
            CONTENT_URI,
            arrayOf(
                DISPLAY_NAME
            ),
            "$DISPLAY_NAME_SOURCE=?", arrayOf("displayNameSource2"), null
        )!!
        cursor.moveToFirst()
        assertThat(cursor.getString(0)).isEqualTo("updated-display-name")
    }
}
