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

import java.util.Locale

/** Internal representation of supported warnings */
// If these warnings are updated also update androidx.room.RoomWarnings
enum class Warning(val publicKey: String) {
    ALL("ALL"),
    @Deprecated("Replaced by QUERY_MISMATCH.", ReplaceWith("QUERY_MISMATCH"))
    CURSOR_MISMATCH("ROOM_CURSOR_MISMATCH"),
    QUERY_MISMATCH("ROOM_QUERY_MISMATCH"),
    DOES_NOT_IMPLEMENT_EQUALS_HASHCODE("ROOM_TYPE_DOES_NOT_IMPLEMENT_EQUALS_HASHCODE"),
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
    DEFAULT_CONSTRUCTOR("ROOM_DEFAULT_CONSTRUCTOR"),
    // TODO(danysantiago): These warning keys should have 'ROOM_' prefix.
    MISSING_COPY_ANNOTATIONS("MISSING_COPY_ANNOTATIONS"),
    MISSING_INDEX_ON_JUNCTION("MISSING_INDEX_ON_JUNCTION"),
    JDK_VERSION_HAS_BUG("JDK_VERSION_HAS_BUG"),
    MISMATCHED_GETTER_TYPE("ROOM_MISMATCHED_GETTER_TYPE"),
    MISMATCHED_SETTER_TYPE("ROOM_MISMATCHED_SETTER_TYPE"),
    // NOTE there is no constant for this in RoomWarnings since this is a temporary case until
    // expand projection is removed.
    EXPAND_PROJECTION_WITH_REMOVE_UNUSED_COLUMNS("ROOM_EXPAND_PROJECTION_WITH_UNUSED_COLUMNS"),
    // We shouldn't let devs suppress this error via Room so there is no runtime constant for it
    JVM_NAME_ON_OVERRIDDEN_METHOD("ROOM_JVM_NAME_IN_OVERRIDDEN_METHOD"),
    AMBIGUOUS_COLUMN_IN_RESULT("ROOM_AMBIGUOUS_COLUMN_IN_RESULT"),
    UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE("ROOM_UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE");

    companion object {
        val PUBLIC_KEY_MAP = values().associateBy { it.publicKey }

        fun fromPublicKey(publicKey: String): Warning? {
            return PUBLIC_KEY_MAP[publicKey.uppercase(Locale.US)]
        }
    }
}
