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
public class RoomWarnings {
    /**
     * The warning dispatched by Room when the return value of a @Query method does not exactly
     * match the fields in the query result.
     * <p>
     * You can use this value inside a {@link SuppressWarnings} annotation to disable such warnings
     * for a method.
     */
    // if you change this, don't forget to change SurpressWarningsVisitor
    public static final String CURSOR_MISMATCH = "ROOM_CURSOR_MISMATCH";
}
