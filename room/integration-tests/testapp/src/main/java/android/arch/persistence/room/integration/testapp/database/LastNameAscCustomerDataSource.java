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
package android.arch.persistence.room.integration.testapp.database;

import android.arch.paging.KeyedDataSource;
import android.arch.persistence.room.InvalidationTracker;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Sample Room keyed data source.
 */
public class LastNameAscCustomerDataSource extends KeyedDataSource<String, Customer> {
    private final CustomerDao mCustomerDao;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;
    private SampleDatabase mDb;

    /**
     * Create a DataSource from the customer table of the given database
     */
    public LastNameAscCustomerDataSource(SampleDatabase db) {
        mDb = db;
        mCustomerDao = db.getCustomerDao();
        mObserver = new InvalidationTracker.Observer("customer") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                invalidate();
            }
        };
        db.getInvalidationTracker().addWeakObserver(mObserver);
    }

    @Override
    public boolean isInvalid() {
        mDb.getInvalidationTracker().refreshVersionsSync();

        return super.isInvalid();
    }

    @NonNull
    public static String getKeyStatic(@NonNull Customer customer) {
        return customer.getLastName();
    }

    @NonNull
    @Override
    public String getKey(@NonNull Customer customer) {
        return getKeyStatic(customer);
    }

    @Override
    public int countItemsBefore(@NonNull String customerName) {
        return mCustomerDao.customerNameCountBefore(customerName);
    }

    @Override
    public int countItemsAfter(@NonNull String customerName) {
        return mCustomerDao.customerNameCountAfter(customerName);
    }

    @Nullable
    @Override
    public List<Customer> loadInitial(int pageSize) {
        return mCustomerDao.customerNameInitial(pageSize);
    }

    @Nullable
    @Override
    public List<Customer> loadBefore(@NonNull String customerName, int pageSize) {
        return mCustomerDao.customerNameLoadBefore(customerName, pageSize);
    }

    @Nullable
    @Override
    public List<Customer> loadAfter(@Nullable String customerName, int pageSize) {
        return mCustomerDao.customerNameLoadAfter(customerName, pageSize);
    }
}
