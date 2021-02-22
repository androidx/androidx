/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.sqlite.db.SupportSQLiteProgram;

import java.util.ArrayList;
import java.util.List;

/**
 * A program implementing an {@link SupportSQLiteProgram} API to record bind arguments.
 */
final class QueryInterceptorProgram implements SupportSQLiteProgram {
    private List<Object> mBindArgsCache = new ArrayList<>();

    @Override
    public void bindNull(int index) {
        saveArgsToCache(index, null);
    }

    @Override
    public void bindLong(int index, long value) {
        saveArgsToCache(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        saveArgsToCache(index, value);
    }

    @Override
    public void bindString(int index, String value) {
        saveArgsToCache(index, value);
    }

    @Override
    public void bindBlob(int index, byte[] value) {
        saveArgsToCache(index, value);
    }

    @Override
    public void clearBindings() {
        mBindArgsCache.clear();
    }

    @Override
    public void close() { }

    private void saveArgsToCache(int bindIndex, Object value) {
        // The index into bind methods are 1...n
        int index = bindIndex - 1;
        if (index >= mBindArgsCache.size()) {
            for (int i = mBindArgsCache.size(); i <= index; i++) {
                mBindArgsCache.add(null);
            }
        }
        mBindArgsCache.set(index, value);
    }

    /**
     * Returns the list of arguments associated with the query.
     *
     * @return argument list.
     */
    List<Object> getBindArgs() {
        return mBindArgsCache;
    }
}
