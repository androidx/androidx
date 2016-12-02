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

import android.database.Cursor;

/**
 * Converts a Cursor into an instance of type T.
 * @param <T> The type of the output class.
 *
 * @hide
 */
@SuppressWarnings("unused")
public interface CursorConverter<T> {
    /**
     * Converts the cursor into an instance of type T.
     * <p>
     * This method should NOT advance / move the cursor.
     *
     * @param cursor The cursor
     * @return An instance of type T.
     */
    T convert(Cursor cursor);
}
