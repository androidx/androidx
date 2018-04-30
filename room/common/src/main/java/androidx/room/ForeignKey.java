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
package androidx.room;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * Declares a foreign key on another {@link Entity}.
 * <p>
 * Foreign keys allows you to specify constraints across Entities such that SQLite will ensure that
 * the relationship is valid when you modify the database.
 * <p>
 * When a foreign key constraint is specified, SQLite requires the referenced columns to be part of
 * a unique index in the parent table or the primary key of that table. You must create a unique
 * index in the parent entity that covers the referenced columns (Room will verify this at compile
 * time and print an error if it is missing).
 * <p>
 * It is also recommended to create an index on the child table to avoid full table scans when the
 * parent table is modified. If a suitable index on the child table is missing, Room will print
 * {@link RoomWarnings#MISSING_INDEX_ON_FOREIGN_KEY_CHILD} warning.
 * <p>
 * A foreign key constraint can be deferred until the transaction is complete. This is useful if
 * you are doing bulk inserts into the database in a single transaction. By default, foreign key
 * constraints are immediate but you can change this value by setting {@link #deferred()} to
 * {@code true}. You can also use
 * <a href="https://sqlite.org/pragma.html#pragma_defer_foreign_keys">defer_foreign_keys</a> PRAGMA
 * to defer them depending on your transaction.
 * <p>
 * Please refer to the SQLite <a href="https://sqlite.org/foreignkeys.html">foreign keys</a>
 * documentation for details.
 */
public @interface ForeignKey {
    /**
     * The parent Entity to reference. It must be a class annotated with {@link Entity} and
     * referenced in the same database.
     *
     * @return The parent Entity.
     */
    Class entity();

    /**
     * The list of column names in the parent {@link Entity}.
     * <p>
     * Number of columns must match the number of columns specified in {@link #childColumns()}.
     *
     * @return The list of column names in the parent Entity.
     * @see #childColumns()
     */
    String[] parentColumns();

    /**
     * The list of column names in the current {@link Entity}.
     * <p>
     * Number of columns must match the number of columns specified in {@link #parentColumns()}.
     *
     * @return The list of column names in the current Entity.
     */
    String[] childColumns();

    /**
     * Action to take when the parent {@link Entity} is deleted from the database.
     * <p>
     * By default, {@link #NO_ACTION} is used.
     *
     * @return The action to take when the referenced entity is deleted from the database.
     */
    @Action int onDelete() default NO_ACTION;

    /**
     * Action to take when the parent {@link Entity} is updated in the database.
     * <p>
     * By default, {@link #NO_ACTION} is used.
     *
     * @return The action to take when the referenced entity is updated in the database.
     */
    @Action int onUpdate() default NO_ACTION;

    /**
     * * A foreign key constraint can be deferred until the transaction is complete. This is useful
     * if you are doing bulk inserts into the database in a single transaction. By default, foreign
     * key constraints are immediate but you can change it by setting this field to {@code true}.
     * You can also use
     * <a href="https://sqlite.org/pragma.html#pragma_defer_foreign_keys">defer_foreign_keys</a>
     * PRAGMA to defer them depending on your transaction.
     *
     * @return Whether the foreign key constraint should be deferred until the transaction is
     * complete. Defaults to {@code false}.
     */
    boolean deferred() default false;

    /**
     * Possible value for {@link #onDelete()} or {@link #onUpdate()}.
     * <p>
     * When a parent key is modified or deleted from the database, no special action is taken.
     * This means that SQLite will not make any effort to fix the constraint failure, instead,
     * reject the change.
     */
    int NO_ACTION = 1;

    /**
     * Possible value for {@link #onDelete()} or {@link #onUpdate()}.
     * <p>
     * The RESTRICT action means that the application is prohibited from deleting
     * (for {@link #onDelete()}) or modifying (for {@link #onUpdate()}) a parent key when there
     * exists one or more child keys mapped to it. The difference between the effect of a RESTRICT
     * action and normal foreign key constraint enforcement is that the RESTRICT action processing
     * happens as soon as the field is updated - not at the end of the current statement as it would
     * with an immediate constraint, or at the end of the current transaction as it would with a
     * {@link #deferred()} constraint.
     * <p>
     * Even if the foreign key constraint it is attached to is {@link #deferred()}, configuring a
     * RESTRICT action causes SQLite to return an error immediately if a parent key with dependent
     * child keys is deleted or modified.
     */
    int RESTRICT = 2;

    /**
     * Possible value for {@link #onDelete()} or {@link #onUpdate()}.
     * <p>
     * If the configured action is "SET NULL", then when a parent key is deleted
     * (for {@link #onDelete()}) or modified (for {@link #onUpdate()}), the child key columns of all
     * rows in the child table that mapped to the parent key are set to contain {@code NULL} values.
     */
    int SET_NULL = 3;

    /**
     * Possible value for {@link #onDelete()} or {@link #onUpdate()}.
     * <p>
     * The "SET DEFAULT" actions are similar to {@link #SET_NULL}, except that each of the child key
     * columns is set to contain the columns default value instead of {@code NULL}.
     */
    int SET_DEFAULT = 4;

    /**
     * Possible value for {@link #onDelete()} or {@link #onUpdate()}.
     * <p>
     * A "CASCADE" action propagates the delete or update operation on the parent key to each
     * dependent child key. For {@link #onDelete()} action, this means that each row in the child
     * entity that was associated with the deleted parent row is also deleted. For an
     * {@link #onUpdate()} action, it means that the values stored in each dependent child key are
     * modified to match the new parent key values.
     */
    int CASCADE = 5;

    /**
     * Constants definition for values that can be used in {@link #onDelete()} and
     * {@link #onUpdate()}.
     */
    @IntDef({NO_ACTION, RESTRICT, SET_NULL, SET_DEFAULT, CASCADE})
    @Retention(SOURCE)
    @interface Action {
    }
}
