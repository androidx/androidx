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

import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey

@ContentEntity
data class ImageNoUri(
    @ContentPrimaryKey(MediaStore.Images.Media._ID)
    var iD: Long?,
    @ContentColumn(MediaStore.Images.Media.TITLE)
    var title: String?,
    @ContentColumn(MediaStore.Images.Media.DESCRIPTION)
    var description: String?,
    @ContentColumn(MediaStore.Images.Media.MIME_TYPE)
    var mimeType: String?,
    @ContentColumn(MediaStore.Images.Media.DATE_ADDED)
    var dateAdded: Long?,
    @ContentColumn(MediaStore.Images.Media.DATE_TAKEN)
    var dateTaken: Long?
)

@ContentEntity("content://com.android.contacts/raw_contacts")
data class Contact(
    @ContentPrimaryKey(ContactsContract.Contacts._ID)
    var iD: Long,
    @ContentColumn(ContactsContract.Contacts.DISPLAY_NAME)
    var displayName: String?,
    @ContentColumn(ContactsContract.Contacts.DISPLAY_NAME_SOURCE)
    var displayNameSource: String?,
    @ContentColumn(ContactsContract.Contacts.HAS_PHONE_NUMBER)
    var hasPhoneNumber: Int?
)

@ContentEntity
data class ContactNoUri(
    @ContentPrimaryKey(ContactsContract.Contacts._ID)
    var iD: Long,
    @ContentColumn(ContactsContract.Contacts.DISPLAY_NAME)
    var displayName: String?,
    @ContentColumn(ContactsContract.Contacts.DISPLAY_NAME_SOURCE)
    var displayNameSource: String?,
    @ContentColumn(ContactsContract.Contacts.HAS_PHONE_NUMBER)
    var hasPhoneNumber: Int?
)
