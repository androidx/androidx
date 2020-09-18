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
import android.provider.ContactsContract
import android.provider.ContactsContract.RawContacts
import android.provider.ContactsContract.Contacts.DISPLAY_NAME
import android.provider.ContactsContract.Contacts.DISPLAY_NAME_SOURCE
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule

@MediumTest
open class ContactsBasedTest {

    val contentResolver =
        InstrumentationRegistry.getInstrumentation().context.contentResolver

    @get:Rule
    var storagePermissions =
        GrantPermissionRule.grant(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission
                .WRITE_CONTACTS
        )!!

    @Before
    fun setup() {
        val values = ContentValues().apply {
            putNull(RawContacts.ACCOUNT_TYPE)
            putNull(RawContacts.ACCOUNT_NAME)
        }
        val uri1 = contentResolver.insert(RawContacts.CONTENT_URI, values)!!
        val uri2 = contentResolver.insert(RawContacts.CONTENT_URI, values)!!
        values.clear()
        values.apply {
            put(DISPLAY_NAME, "displayName1")
            put(DISPLAY_NAME_SOURCE, "displayNameSource1")
        }
        contentResolver.update(uri1, values, null, null)
        values.clear()
        values.apply {
            put(DISPLAY_NAME, "displayName2")
            put(DISPLAY_NAME_SOURCE, "displayNameSource2")
        }
        contentResolver.update(uri2, values, null, null)
    }

    @After
    fun deleteAllAdded() {
        // For the tests to work properly in terms of the expected results, the provider should not
        // have prior rows, so make sure we delete everything at the end of the tests for proper
        // local testing.
        contentResolver.delete(ContactsContract.Data.CONTENT_URI, "", null)
        contentResolver.delete(RawContacts.CONTENT_URI, "", null)
    }
}