/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * Marks a method in a {@link Dao} annotated class as an insert method.
 * <p>
 * The implementation of the method will insert its parameters into the database.
 * <p>
 * All of the parameters of the Insert method must either be classes annotated with {@link Entity}
 * or collections/array of it.
 */
public @interface Insert {
    int REPLACE = 1;
    int ROLLBACK = 2;
    int ABORT = 3;
    int FAIL = 4;
    int IGNORE = 5;

    @Retention(SOURCE)
    @IntDef({REPLACE, ROLLBACK, ABORT, FAIL, IGNORE})
    @interface OnConflict {}

    /**
     * What to do if a conflict happens.
     * See: https://sqlite.org/lang_conflict.html
     *
     * @return How to handle conflicts.
     */
    @OnConflict
    int onConflict() default ABORT;
}
