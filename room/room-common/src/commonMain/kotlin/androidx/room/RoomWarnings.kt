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
package androidx.room

/**
 * The list of warnings that are produced by Room.
 *
 * You can use these values inside a [SuppressWarnings] annotation to disable the warnings.
 */
// If you change this, don't forget to change androidx.room.vo.Warning
@Suppress("unused", "WeakerAccess")
public open class RoomWarnings {
    public companion object {
        /**
         * The warning dispatched by Room when the return value of a [Query] method does not exactly
         * match the columns in the query result.
         */
        public const val QUERY_MISMATCH: String = "ROOM_QUERY_MISMATCH"

        /**
         * The warning dispatched by Room when the return value of a [Query] method does not exactly
         * match the fields in the query result.
         */
        @Deprecated("Replaced by QUERY_MISMATCH.", ReplaceWith("QUERY_MISMATCH"))
        public const val CURSOR_MISMATCH: String = "ROOM_CURSOR_MISMATCH"

        /**
         * The warning dispatched by Room when the object in the provided method's multimap return
         * type does not implement equals() and hashCode().
         */
        public const val DOES_NOT_IMPLEMENT_EQUALS_HASHCODE: String =
            "ROOM_TYPE_DOES_NOT_IMPLEMENT_EQUALS_HASHCODE"

        /**
         * Reported when Room cannot verify database queries during compilation due to lack of tmp
         * dir access in JVM.
         */
        public const val MISSING_JAVA_TMP_DIR: String = "ROOM_MISSING_JAVA_TMP_DIR"

        /**
         * Reported when Room cannot verify database queries during compilation. This usually
         * happens when it cannot find the SQLite JDBC driver on the host machine.
         *
         * Room can function without query verification but its functionality will be limited.
         */
        public const val CANNOT_CREATE_VERIFICATION_DATABASE: String =
            "ROOM_CANNOT_CREATE_VERIFICATION_DATABASE"

        /**
         * Reported when an [Entity] field that is annotated with [Embedded] has a sub field which
         * is annotated with [PrimaryKey] but the [PrimaryKey] is dropped while composing it into
         * the parent object.
         */
        public const val PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED: String =
            "ROOM_EMBEDDED_PRIMARY_KEY_IS_DROPPED"

        /**
         * Reported when an [Entity] field that is annotated with [Embedded] has a sub field which
         * has a [ColumnInfo] annotation with `index = true`.
         *
         * You can re-define the index in the containing [Entity].
         */
        public const val INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED: String =
            "ROOM_EMBEDDED_INDEX_IS_DROPPED"

        /**
         * Reported when an [Entity] that has a [Embedded] field whose type is another [Entity] and
         * that [Entity] has some indices defined. These indices will NOT be created in the
         * containing [Entity]. If you want to preserve them, you can re-define them in the
         * containing [Entity].
         */
        public const val INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED: String =
            "ROOM_EMBEDDED_ENTITY_INDEX_IS_DROPPED"

        /**
         * Reported when an [Entity]'s parent declares an [Index]. Room does not automatically
         * inherit these indices to avoid hidden costs or unexpected constraints.
         *
         * If you want your child class to have the indices of the parent, you must re-declare them
         * in the child class. Alternatively, you can set [Entity.inheritSuperIndices] to `true`.
         */
        public const val INDEX_FROM_PARENT_IS_DROPPED: String = "ROOM_PARENT_INDEX_IS_DROPPED"

        /**
         * Reported when an [Entity] inherits a field from its super class and the field has a
         * [ColumnInfo] annotation with `index = true`.
         *
         * These indices are dropped for the [Entity] and you would need to re-declare them if you
         * want to keep them. Alternatively, you can set [Entity.inheritSuperIndices] to `true`.
         */
        public const val INDEX_FROM_PARENT_FIELD_IS_DROPPED: String =
            "ROOM_PARENT_FIELD_INDEX_IS_DROPPED"

        /**
         * Reported when a [Relation] [Entity]'s SQLite column type does not match the type in the
         * parent. Room will still do the matching using `String` representations.
         */
        public const val RELATION_TYPE_MISMATCH: String = "ROOM_RELATION_TYPE_MISMATCH"

        /**
         * Reported when a `room.schemaLocation` argument is not provided into the annotation
         * processor. You can either set [Database.exportSchema] to `false` or provide
         * `room.schemaLocation` to the annotation processor. You are strongly advised to provide it
         * and also commit them into your version control system.
         */
        public const val MISSING_SCHEMA_LOCATION: String = "ROOM_MISSING_SCHEMA_LOCATION"

        /**
         * When there is a foreign key from Entity A to Entity B, it is a good idea to index the
         * reference columns in B, otherwise, each modification on Entity A will trigger a full
         * table scan on Entity B.
         *
         * If Room cannot find a proper index in the child entity (Entity B in this case), Room will
         * print this warning.
         */
        public const val MISSING_INDEX_ON_FOREIGN_KEY_CHILD: String =
            "ROOM_MISSING_FOREIGN_KEY_CHILD_INDEX"

        /**
         * Reported when a junction entity whose column is used in a `@Relation` field with a
         * `@Junction` does not contain an index. If the column is not covered by any index then a
         * full table scan might be performed when resolving the relationship.
         *
         * It is recommended that columns on entities used as junctions contain indices, otherwise
         * Room will print this warning.
         */
        public const val MISSING_INDEX_ON_JUNCTION: String = "MISSING_INDEX_ON_JUNCTION"

        /**
         * Reported when a POJO has multiple constructors, one of which is a no-arg constructor.
         * Room will pick that one by default but will print this warning in case the constructor
         * choice is important. You can always guide Room to use the right constructor using
         * the @Ignore annotation.
         */
        public const val DEFAULT_CONSTRUCTOR: String = "ROOM_DEFAULT_CONSTRUCTOR"

        /**
         * Reported when a @Query method returns a POJO that has relations but the method is not
         * annotated with @Transaction. Relations are run as separate queries and if the query is
         * not run inside a transaction, it might return inconsistent results from the database.
         */
        public const val RELATION_QUERY_WITHOUT_TRANSACTION: String =
            "ROOM_RELATION_QUERY_WITHOUT_TRANSACTION"

        /**
         * Reported when an `@Entity` field's type do not exactly match the getter type. For
         * instance, in the following class:
         * ```
         * @Entity
         * class Foo {
         *     ...
         *     private val value: Boolean
         *     public fun getValue(): Boolean {
         *         return value == null ? false : value
         *     }
         * }
         * ```
         *
         * Trying to insert this entity into database will always set `value` column to `false` when
         * `Foo.value` is `null` since Room will use the `getValue` method to read the value. So
         * even thought the database column is nullable, it will never be inserted as `null` if
         * inserted as a `Foo` instance.
         */
        public const val MISMATCHED_GETTER: String = "ROOM_MISMATCHED_GETTER_TYPE"

        /**
         * Reported when an `@Entity` field's type do not exactly match the setter type. For
         * instance, in the following class:
         * ```
         * @Entity
         * class Foo {
         *     ...
         *     private val value: Boolean
         *     public fun setValue(value: Boolean) {
         *         this.value = value
         *     }
         * }
         * ```
         *
         * If Room reads this entity from the database, it will always set `Foo.value` to `false`
         * when the column value is `null` since Room will use the `setValue` method to write the
         * value.
         */
        public const val MISMATCHED_SETTER: String = "ROOM_MISMATCHED_SETTER_TYPE"

        /** Reported when there is an ambiguous column on the result of a multimap query. */
        public const val AMBIGUOUS_COLUMN_IN_RESULT: String = "ROOM_AMBIGUOUS_COLUMN_IN_RESULT"

        /**
         * Reported when a nullable Collection, Array or Optional is returned from a DAO method.
         * Room will return an empty Collection, Array or Optional respectively if no results are
         * returned by such a query, hence using a nullable return type is unnecessary in this case.
         */
        public const val UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE: String =
            "ROOM_UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE"
    }

    @Deprecated("This type should not be instantiated as it contains only static methods. ")
    @Suppress("PrivateConstructorForUtilityClass")
    public constructor()
}
