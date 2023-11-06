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

import androidx.annotation.IntDef
import kotlin.reflect.KClass

/**
 * Declares a foreign key on another [Entity].
 *
 * Foreign keys allows you to specify constraints across Entities such that SQLite will ensure that
 * the relationship is valid when you modify the database.
 *
 * When a foreign key constraint is specified, SQLite requires the referenced columns to be part of
 * a unique index in the parent table or the primary key of that table. You must create a unique
 * index in the parent entity that covers the referenced columns (Room will verify this at compile
 * time and print an error if it is missing).
 *
 * It is also recommended to create an index on the child table to avoid full table scans when the
 * parent table is modified. If a suitable index on the child table is missing, Room will print
 * [RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD] warning.
 *
 * A foreign key constraint can be deferred until the transaction is complete. This is useful if
 * you are doing bulk inserts into the database in a single transaction. By default, foreign key
 * constraints are immediate but you can change this value by setting [deferred] to
 * `true`. You can also use
 * [defer_foreign_keys](https://sqlite.org/pragma.html#pragma_defer_foreign_keys) to defer them
 * depending on your transaction.
 *
 * Please refer to the SQLite [foreign keys](https://sqlite.org/foreignkeys.html) documentation for
 * details.
 */
@Target(allowedTargets = []) // Complex annotation target
@Retention(AnnotationRetention.BINARY)
public annotation class ForeignKey(
    /**
     * The parent Entity to reference. It must be a class annotated with [Entity] and
     * referenced in the same database.
     *
     * @return The parent Entity.
     */
    val entity: KClass<*>,

    /**
     * The list of column names in the parent [Entity].
     *
     * Number of columns must match the number of columns specified in [childColumns].
     *
     * @return The list of column names in the parent Entity.
     * @see [childColumns]
     */
    val parentColumns: Array<String>,

    /**
     * The list of column names in the current [Entity].
     *
     * Number of columns must match the number of columns specified in [parentColumns].
     *
     * @return The list of column names in the current Entity.
     */
    val childColumns: Array<String>,

    /**
     * Action to take when the parent [Entity] is deleted from the database.
     *
     * By default, [NO_ACTION] is used.
     *
     * @return The action to take when the referenced entity is deleted from the database.
     */
    @get:Action
    val onDelete: Int = NO_ACTION,

    /**
     * Action to take when the parent [Entity] is updated in the database.
     *
     * By default, [NO_ACTION] is used.
     *
     * @return The action to take when the referenced entity is updated in the database.
     */
    @get:Action
    val onUpdate: Int = NO_ACTION,

    /**
     * A foreign key constraint can be deferred until the transaction is complete. This is useful
     * if you are doing bulk inserts into the database in a single transaction. By default, foreign
     * key constraints are immediate but you can change it by setting this field to `true`.
     * You can also use
     * [defer_foreign_keys](https://sqlite.org/pragma.html#pragma_defer_foreign_keys)
     * PRAGMA to defer them depending on your transaction.
     *
     * @return Whether the foreign key constraint should be deferred until the transaction is
     * complete. Defaults to `false`.
     */
    val deferred: Boolean = false
) {
    public companion object {
        /**
         * Possible value for [onDelete] or [onUpdate].
         *
         * When a parent key is modified or deleted from the database, no special action is taken.
         * This means that SQLite will not make any effort to fix the constraint failure, instead,
         * reject the change.
         */
        public const val NO_ACTION: Int = 1

        /**
         * Possible value for [onDelete] or [onUpdate].
         *
         * The RESTRICT action means that the application is prohibited from deleting
         * (for [onDelete]) or modifying (for [onUpdate]) a parent key when there
         * exists one or more child keys mapped to it. The difference between the effect of a RESTRICT
         * action and normal foreign key constraint enforcement is that the RESTRICT action processing
         * happens as soon as the field is updated - not at the end of the current statement as it would
         * with an immediate constraint, or at the end of the current transaction as it would with a
         * [deferred] constraint.
         *
         * Even if the foreign key constraint it is attached to is [deferred], configuring a
         * RESTRICT action causes SQLite to return an error immediately if a parent key with dependent
         * child keys is deleted or modified.
         */
        public const val RESTRICT: Int = 2

        /**
         * Possible value for [onDelete] or [onUpdate].
         *
         * If the configured action is "SET NULL", then when a parent key is deleted
         * (for [onDelete]) or modified (for [onUpdate]), the child key columns of all
         * rows in the child table that mapped to the parent key are set to contain `NULL` values.
         */
        public const val SET_NULL: Int = 3

        /**
         * Possible value for [onDelete] or [onUpdate].
         *
         * The "SET DEFAULT" actions are similar to [SET_NULL], except that each of the child key
         * columns is set to contain the columns default value instead of `NULL`.
         */
        public const val SET_DEFAULT: Int = 4

        /**
         * Possible value for [onDelete] or [onUpdate].
         *
         * A "CASCADE" action propagates the delete or update operation on the parent key to each
         * dependent child key. For [onDelete] action, this means that each row in the child
         * entity that was associated with the deleted parent row is also deleted. For an
         * [onUpdate] action, it means that the values stored in each dependent child key are
         * modified to match the new parent key values.
         */
        public const val CASCADE: Int = 5
    }

    /**
     * Constants definition for values that can be used in [onDelete] and
     * [onUpdate].
     */
    @IntDef(NO_ACTION, RESTRICT, SET_NULL, SET_DEFAULT, CASCADE)
    @Retention(AnnotationRetention.BINARY)
    public annotation class Action
}
