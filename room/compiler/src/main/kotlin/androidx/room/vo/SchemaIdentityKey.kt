/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.vo

import org.apache.commons.codec.digest.DigestUtils
import java.util.Locale

interface HasSchemaIdentity {
    fun getIdKey(): String
}

/**
 * A class that can be converted into a unique identifier for an object
 */
class SchemaIdentityKey {
    companion object {
        private val SEPARATOR = "?:?"
        private val ENGLISH_SORT = Comparator<String> { o1, o2 ->
            o1.toLowerCase(Locale.ENGLISH).compareTo(o2.toLowerCase(Locale.ENGLISH))
        }
    }

    private val sb = StringBuilder()
    fun append(identity: HasSchemaIdentity) {
        append(identity.getIdKey())
    }

    fun appendSorted(identities: List<HasSchemaIdentity>) {
        identities.map { it.getIdKey() }.sortedWith(ENGLISH_SORT).forEach {
            append(it)
        }
    }

    fun hash() = DigestUtils.md5Hex(sb.toString())
    fun append(identity: String) {
        sb.append(identity).append(SEPARATOR)
    }
}