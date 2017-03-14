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

package com.example.android.flatfoot.codelab.flatfootcodelab.step10.internal;

import android.content.Context;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10.ProductReviewServiceFactory;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteOpenHelper.Callback;
import com.android.support.db.SupportSQLiteOpenHelper.Configuration;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.android.support.room.Room;
import com.android.support.room.RoomDatabase;

/**
 * Database helper.
 */
class DatabaseHelper {

    static void initialize(Context context) {
        final boolean[] initialised = new boolean[1];
        FrameworkSQLiteOpenHelperFactory factory = new FrameworkSQLiteOpenHelperFactory() {
            @Override
            public SupportSQLiteOpenHelper create(final Configuration configuration) {
                Callback callback = new Callback() {
                    @Override
                    public void onCreate(SupportSQLiteDatabase db) {
                        configuration.callback.onCreate(db);
                        initialised[0] = true;
                    }

                    @Override
                    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                            int newVersion) {
                        configuration.callback.onUpgrade(db, oldVersion, newVersion);
                    }
                };
                Configuration newConfig = Configuration.builder(configuration.context)
                        .name(configuration.name)
                        .errorHandler(configuration.errorHandler)
                        .factory(configuration.factory)
                        .version(configuration.version)
                        .callback(callback)
                        .build();
                return super.create(newConfig);
            }
        };
        RoomDatabase.Builder<ProductsDatabase> databaseBuilder = Room.databaseBuilder(context,
                ProductsDatabase.class, "_internal_product_database.db");
        ProductsDatabase database = databaseBuilder.openHelperFactory(factory).build();
        ProductReviewServiceImpl service = new ProductReviewServiceImpl(database);
        service.db.beginTransaction();
        service.db.endTransaction();
        if (initialised[0]) {
            service.initializeDb();
        }
        ProductReviewServiceFactory.setDefault(service);
    }
}
