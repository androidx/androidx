/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.mock

class ContactModel(
    var filter: String = "",
    var contacts: List<Contact>,
    var selected: Contact? = null,
) {
    val filtered
        @Suppress("ListIterator") get() = contacts.filter { it.name.contains(filter) }

    fun add(contact: Contact, after: Contact? = null) {
        val retList = mutableListOf<Contact>().apply { addAll(contacts) }
        if (after == null) {
            retList.add(contact)
        } else {
            retList.add(find(retList, after) + 1, contact)
        }

        contacts = retList
    }

    fun move(contact: Contact, after: Contact?) {
        val retList = mutableListOf<Contact>().apply { addAll(contacts) }
        if (after == null) {
            retList.removeAt(find(retList, contact))
            retList.add(0, contact)
        } else {
            retList.removeAt(find(retList, contact))
            retList.add(find(retList, after) + 1, contact)
        }
        contacts = retList
    }

    private fun find(list: List<Contact>, contact: Contact): Int {
        val index = list.indexOf(contact)
        if (index < 0) error("Contact $contact not found")
        return index
    }
}
