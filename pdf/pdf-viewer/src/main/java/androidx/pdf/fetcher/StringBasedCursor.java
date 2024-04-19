/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.fetcher;

import android.database.AbstractCursor;

import androidx.annotation.RestrictTo;

/**
 * Cursor implementation that only leaves getString to be implemented - all the other methods
 * delegate to getString and then parse the result.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class StringBasedCursor extends AbstractCursor {
    @Override
    public short getShort(int column) {
        return Short.parseShort(getString(column));
    }

    @Override
    public int getInt(int column) {
        return Integer.parseInt(getString(column));
    }

    @Override
    public long getLong(int column) {
        return Long.parseLong(getString(column));
    }

    @Override
    public float getFloat(int column) {
        return Float.parseFloat(getString(column));
    }

    @Override
    public double getDouble(int column) {
        return Double.parseDouble(getString(column));
    }

    @Override
    public boolean isNull(int column) {
        return getString(column) == null;
    }
}
