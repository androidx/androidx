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
 * Marks a method in a {@link Dao} annotated class as an update method.
 * <p>
 * The implementation of the method will update its parameters in the database if they already
 * exists (checked by primary keys). If they don't already exists, this option will not change the
 * database.
 * <p>
 * All of the parameters of the Update method must either be classes annotated with {@link Entity}
 * or collections/array of it.
 *
 * @see Insert
 * @see Delete
 */
public @interface Update {
    /**
     * What to do if a conflict happens.
     * @see <a href="https://sqlite.org/lang_conflict.html">SQLite conflict documentation</a>
     *
     * @return How to handle conflicts. Defaults to {@link OnConflictStrategy#ABORT}.
     */
    @OnConflictStrategy
    int onConflict() default OnConflictStrategy.ABORT;
}
