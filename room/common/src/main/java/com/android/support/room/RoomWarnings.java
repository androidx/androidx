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

package com.android.support.room;

/**
 * The list of warnings that are produced by Room that you can disable via {@code SurpressWarnings}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RoomWarnings {
    /**
     * The warning dispatched by Room when the return value of a @Query method does not exactly
     * match the fields in the query result.
     * <p>
     * You can use this value inside a {@link SuppressWarnings} annotation to disable such warnings
     * for a method.
     */
    // if you change this, don't forget to change com.android.support.room.vo.Warning
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
     * Reported when an {@link Entity} field that is annotated with {@link Decompose} has a
     * sub field which is annotated with {@link PrimaryKey} but the {@link PrimaryKey} is dropped
     * during the decomposition.
     */
    public static final String PRIMARY_KEY_FROM_DECOMPOSED_IS_DROPPED =
            "ROOM_DECOMPOSED_PRIMARY_KEY_IS_DROPPED";
}
