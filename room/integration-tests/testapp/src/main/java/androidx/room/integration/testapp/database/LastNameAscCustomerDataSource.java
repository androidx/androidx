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
package androidx.room.integration.testapp.database;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.ItemKeyedDataSource;
import androidx.room.InvalidationTracker;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sample Room keyed data source.
 */
public class LastNameAscCustomerDataSource extends ItemKeyedDataSource<String, Customer> {
    private final CustomerDao mCustomerDao;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;
    private SampleDatabase mDb;

    public static Factory<String, Customer> factory(final SampleDatabase db) {
        return new Factory<String, Customer>() {
            @Override
            public DataSource<String, Customer> create() {
                return new LastNameAscCustomerDataSource(db);
            }
        };
    }

    /**
     * Create a DataSource from the customer table of the given database
     */
    private LastNameAscCustomerDataSource(SampleDatabase db) {
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
    public void loadInitial(@NonNull LoadInitialParams<String> params,
            @NonNull LoadInitialCallback<Customer> callback) {
        String customerName = params.requestedInitialKey;
        List<Customer> list;
        if (customerName != null) {
            // initial keyed load - load before 'customerName',
            // and load after last item in before list
            int pageSize = params.requestedLoadSize / 2;
            String key = customerName;
            list = mCustomerDao.customerNameLoadBefore(key, pageSize);
            Collections.reverse(list);
            if (!list.isEmpty()) {
                key = getKey(list.get(list.size() - 1));
            }
            list.addAll(mCustomerDao.customerNameLoadAfter(key, pageSize));
        } else {
            list = mCustomerDao.customerNameInitial(params.requestedLoadSize);
        }

        if (params.placeholdersEnabled && !list.isEmpty()) {
            String firstKey = getKey(list.get(0));
            String lastKey = getKey(list.get(list.size() - 1));

            // only bother counting if placeholders are desired
            final int position = mCustomerDao.customerNameCountBefore(firstKey);
            final int count = position + list.size() + mCustomerDao.customerNameCountAfter(lastKey);
            callback.onResult(list, position, count);
        } else {
            callback.onResult(list);
        }
    }

    @Override
    public void loadAfter(@NonNull LoadParams<String> params,
            @NonNull LoadCallback<Customer> callback) {
        callback.onResult(mCustomerDao.customerNameLoadAfter(params.key, params.requestedLoadSize));
    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params,
            @NonNull LoadCallback<Customer> callback) {
        List<Customer> list = mCustomerDao.customerNameLoadBefore(
                params.key, params.requestedLoadSize);
        Collections.reverse(list);
        callback.onResult(list);
    }
}

