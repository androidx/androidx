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

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

/**
 * Factory class for AutoClosingRoomOpenHelper
 */
final class AutoClosingRoomOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {
    @NonNull
    private final SupportSQLiteOpenHelper.Factory mDelegate;

    @NonNull
    private final AutoCloser mAutoCloser;

    AutoClosingRoomOpenHelperFactory(
            @NonNull SupportSQLiteOpenHelper.Factory factory,
            @NonNull AutoCloser autoCloser) {
        mDelegate = factory;
        mAutoCloser = autoCloser;
    }

    /**
     * @return AutoClosingRoomOpenHelper instances.
     */
    @Override
    @NonNull
    public AutoClosingRoomOpenHelper create(
            @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
        return new AutoClosingRoomOpenHelper(mDelegate.create(configuration), mAutoCloser);
    }
}
