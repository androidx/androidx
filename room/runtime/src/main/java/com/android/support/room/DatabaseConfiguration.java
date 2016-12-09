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

import android.content.Context;

import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;

/**
 * Configuration class for {@link RoomDatabase}.
 */
public class DatabaseConfiguration {
    /**
     * The factory to use to access the database.
     */
    public final SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory;
    /**
     * The context to you.
     */
    public final Context context;
    /**
     * The name of the database or null if it is in memory.
     */
    public final String name;
    /**
     * The version of the database.
     */
    public final int version;

    private DatabaseConfiguration(Context context, String name, int version,
                                  SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory) {
        this.sqliteOpenHelperFactory = sqliteOpenHelperFactory;
        this.context = context;
        this.name = name;
        this.version = version;
    }

    /**
     * Builder for the RoomDatabase configuration.
     */
    public static class Builder {
        SupportSQLiteOpenHelper.Factory mFactory;
        Context mContext;
        String mName;
        int mVersion = 1;

        /**
         * Sets the database factory. If not set, it defaults to
         * {@link FrameworkSQLiteOpenHelperFactory}.
         *
         * @param factory The factory to use to access the database.
         *
         * @return this
         */
        public Builder withOpenHelperFactory(SupportSQLiteOpenHelper.Factory factory) {
            mFactory = factory;
            return this;
        }

        /**
         * @param name The name of the database.
         * @return this
         */
        public Builder withName(String name) {
            mName = name;
            return this;
        }

        /**
         * Version of the database, defaults to 1.
         * @param version The database version to use
         * @return this
         */
        public Builder withVersion(int version) {
            mVersion = version;
            return this;
        }

        /**
         * Creates a Configuration builder with the given context.
         * Most of the time, this should be an application context.
         *
         * @param context The context to use for the database.
         */
        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Creates the {@link RoomDatabase.Configuration} object for the database.
         *
         * @return Configuration that can be used to create the RoomDatabase.
         */
        public DatabaseConfiguration build() {
            if (mFactory == null) {
                mFactory = new FrameworkSQLiteOpenHelperFactory();
            }
            return new DatabaseConfiguration(mContext, mName, mVersion, mFactory);
        }
    }
}
