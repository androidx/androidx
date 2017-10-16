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

package android.arch.persistence.room;

/**
 * The list of warnings that are produced by Room.
 * <p>
 * You can use these values inside a {@link SuppressWarnings} annotation to disable the warnings.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RoomWarnings {
    /**
     * The warning dispatched by Room when the return value of a {@link Query} method does not
     * exactly match the fields in the query result.
     */
    // if you change this, don't forget to change android.arch.persistence.room.vo.Warning
    public static final String CURSOR_MISMATCH = "ROOM_CURSOR_MISMATCH";

    /**
     * Reported when Room cannot verify database queries during compilation due to lack of
     * tmp dir access in JVM.
     */
    public static final String MISSING_JAVA_TMP_DIR = "ROOM_MISSING_JAVA_TMP_DIR";

    /**
     * Reported when Room cannot verify database queries during compilation. This usually happens
     * when it cannot find the SQLite JDBC driver on the host machine.
     * <p>
     * Room can function without query verification but its functionality will be limited.
     */
    public static final String CANNOT_CREATE_VERIFICATION_DATABASE =
            "ROOM_CANNOT_CREATE_VERIFICATION_DATABASE";

    /**
     * Reported when an {@link Entity} field that is annotated with {@link Embedded} has a
     * sub field which is annotated with {@link PrimaryKey} but the {@link PrimaryKey} is dropped
     * while composing it into the parent object.
     */
    public static final String PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED =
            "ROOM_EMBEDDED_PRIMARY_KEY_IS_DROPPED";

    /**
     * Reported when an {@link Entity} field that is annotated with {@link Embedded} has a
     * sub field which has a {@link ColumnInfo} annotation with {@code index = true}.
     * <p>
     * You can re-define the index in the containing {@link Entity}.
     */
    public static final String INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED =
            "ROOM_EMBEDDED_INDEX_IS_DROPPED";

    /**
     * Reported when an {@link Entity} that has a {@link Embedded}d field whose type is another
     * {@link Entity} and that {@link Entity} has some indices defined.
     * These indices will NOT be created in the containing {@link Entity}. If you want to preserve
     * them, you can re-define them in the containing {@link Entity}.
     */
    public static final String INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED =
            "ROOM_EMBEDDED_ENTITY_INDEX_IS_DROPPED";

    /**
     * Reported when an {@link Entity}'s parent declares an {@link Index}. Room does not
     * automatically inherit these indices to avoid hidden costs or unexpected constraints.
     * <p>
     * If you want your child class to have the indices of the parent, you must re-declare
     * them in the child class. Alternatively, you can set {@link Entity#inheritSuperIndices()}
     * to {@code true}.
     */
    public static final String INDEX_FROM_PARENT_IS_DROPPED =
            "ROOM_PARENT_INDEX_IS_DROPPED";

    /**
     * Reported when an {@link Entity} inherits a field from its super class and the field has a
     * {@link ColumnInfo} annotation with {@code index = true}.
     * <p>
     * These indices are dropped for the {@link Entity} and you would need to re-declare them if
     * you want to keep them. Alternatively, you can set {@link Entity#inheritSuperIndices()}
     * to {@code true}.
     */
    public static final String INDEX_FROM_PARENT_FIELD_IS_DROPPED =
            "ROOM_PARENT_FIELD_INDEX_IS_DROPPED";

    /**
     * Reported when a {@link Relation} {@link Entity}'s SQLite column type does not match the type
     * in the parent. Room will still do the matching using {@code String} representations.
     */
    public static final String RELATION_TYPE_MISMATCH = "ROOM_RELATION_TYPE_MISMATCH";

    /**
     * Reported when a `room.schemaLocation` argument is not provided into the annotation processor.
     * You can either set {@link Database#exportSchema()} to {@code false} or provide
     * `room.schemaLocation` to the annotation processor. You are strongly adviced to provide it
     * and also commit them into your version control system.
     */
    public static final String MISSING_SCHEMA_LOCATION = "ROOM_MISSING_SCHEMA_LOCATION";

    /**
     * When there is a foreign key from Entity A to Entity B, it is a good idea to index the
     * reference columns in B, otherwise, each modification on Entity A will trigger a full table
     * scan on Entity B.
     * <p>
     * If Room cannot find a proper index in the child entity (Entity B in this case), Room will
     * print this warning.
     */
    public static final String MISSING_INDEX_ON_FOREIGN_KEY_CHILD =
            "ROOM_MISSING_FOREIGN_KEY_CHILD_INDEX";
}
