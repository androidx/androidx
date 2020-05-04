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

import android.app.Activity
import android.os.Bundle
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey
import androidx.contentaccess.ContentColumn

public class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // val im = _ContactsAccessorImpl(contentResolver).getDisplayNameById("id!")
    }

    // @ContentAccessObject(Contact::class)
    interface ContactsAccessor {
        @ContentQuery(query = "displayNamePrimary", selection = "iD = :iD")
        fun getDisplayNameById(iD: String): String

        @ContentQuery(query = "math", selection = "iD = :iD", contentEntity =
        ContactGrades::class, uri = "customuri")
        fun getNamRawContactId(iD: Long): Int

        @ContentQuery(selection = "toy = :toy")
        fun getAnyContact(toy: String): Contact

        @ContentQuery(contentEntity = ContactGrades::class, uri = "custom2")
        fun getContactsMathPhysicsGrades(): List<MathPhysicsGrades>
    }

    data class MathPhysicsGrades(val math: Int, val physics: Int)

    // This content entity should be supplied by us for system providers.
    @ContentEntity("content://com.android.contacts/contacts")
    data class Contact(
        @ContentPrimaryKey("_id")
        var iD: Long,
        @ContentColumn("display_name")
        var displayNamePrimary: String,
        @ContentColumn("favorite_toy")
        var toy: String,
        @ContentColumn("contact_height")
        var height: Int,
        @ContentColumn("contact_weight")
        var weight: String
    )

    // Other entity to know we can differentiate when specifying a custom entity somewhere else.
    @ContentEntity()
    data class ContactGrades(
        @ContentPrimaryKey("_id")
        var iD: Long,
        @ContentColumn("name")
        var nameRawContactId: Long,
        @ContentColumn("math_grade")
        var math: Int,
        @ContentColumn("physics_grade")
        var physics: Int,
        @ContentColumn("cs_grade")
        var cs: Int
    )
}
