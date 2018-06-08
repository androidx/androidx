/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * Internal representation of supported warnings
 */
enum class Warning(val publicKey: String) {
    ALL("ALL"),
    CURSOR_MISMATCH("ROOM_CURSOR_MISMATCH"),
    MISSING_JAVA_TMP_DIR("ROOM_MISSING_JAVA_TMP_DIR"),
    CANNOT_CREATE_VERIFICATION_DATABASE("ROOM_CANNOT_CREATE_VERIFICATION_DATABASE"),
    PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED("ROOM_EMBEDDED_PRIMARY_KEY_IS_DROPPED"),
    INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED("ROOM_EMBEDDED_INDEX_IS_DROPPED"),
    INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED("ROOM_EMBEDDED_ENTITY_INDEX_IS_DROPPED"),
    INDEX_FROM_PARENT_IS_DROPPED("ROOM_PARENT_INDEX_IS_DROPPED"),
    INDEX_FROM_PARENT_FIELD_IS_DROPPED("ROOM_PARENT_FIELD_INDEX_IS_DROPPED"),
    RELATION_TYPE_MISMATCH("ROOM_RELATION_TYPE_MISMATCH"),
    MISSING_SCHEMA_LOCATION("ROOM_MISSING_SCHEMA_LOCATION"),
    MISSING_INDEX_ON_FOREIGN_KEY_CHILD("ROOM_MISSING_FOREIGN_KEY_CHILD_INDEX"),
    RELATION_QUERY_WITHOUT_TRANSACTION("ROOM_RELATION_QUERY_WITHOUT_TRANSACTION"),
    DEFAULT_CONSTRUCTOR("ROOM_DEFAULT_CONSTRUCTOR");

    companion object {
        val PUBLIC_KEY_MAP = Warning.values().associateBy { it.publicKey }
        fun fromPublicKey(publicKey: String): Warning? {
            return PUBLIC_KEY_MAP[publicKey.toUpperCase()]
        }
    }
}
