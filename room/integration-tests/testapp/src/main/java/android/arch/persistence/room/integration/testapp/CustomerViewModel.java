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

package android.arch.persistence.room.integration.testapp;

import android.app.Application;
import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListProvider;
import android.arch.paging.PagedList;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.integration.testapp.database.Customer;
import android.arch.persistence.room.integration.testapp.database.LastNameAscCustomerDataSource;
import android.arch.persistence.room.integration.testapp.database.SampleDatabase;
import android.support.annotation.WorkerThread;

import java.util.UUID;

/**
 * Sample database-backed view model of Customers
 */
public class CustomerViewModel extends AndroidViewModel {
    private SampleDatabase mDatabase;
    private LiveData<PagedList<Customer>> mLiveCustomerList;

    public CustomerViewModel(Application application) {
        super(application);
        createDb();
    }

    private void createDb() {
        mDatabase = Room.databaseBuilder(this.getApplication(),
                SampleDatabase.class, "customerDatabase").build();

        AppToolkitTaskExecutor.getInstance().executeOnDiskIO(new Runnable() {
            @Override
            public void run() {
                // fill with some simple data
                int customerCount = mDatabase.getCustomerDao().countCustomers();
                if (customerCount == 0) {
                    Customer[] initialCustomers = new Customer[10];
                    for (int i = 0; i < 10; i++) {
                        initialCustomers[i] = createCustomer();
                    }
                    mDatabase.getCustomerDao().insertAll(initialCustomers);
                }

            }
        });
    }

    @WorkerThread
    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setName(UUID.randomUUID().toString());
        customer.setLastName(UUID.randomUUID().toString());
        return customer;
    }

    void insertCustomer() {
        AppToolkitTaskExecutor.getInstance().executeOnDiskIO(new Runnable() {
            @Override
            public void run() {
                mDatabase.getCustomerDao().insert(createCustomer());
            }
        });
    }

    LiveData<PagedList<Customer>> getLivePagedList(int position) {
        if (mLiveCustomerList == null) {
            mLiveCustomerList = mDatabase.getCustomerDao()
                    .loadPagedAgeOrder().create(position,
                            new PagedList.Config.Builder()
                                    .setPageSize(10)
                                    .setEnablePlaceholders(false)
                                    .build());
        }
        return mLiveCustomerList;
    }

    LiveData<PagedList<Customer>> getLivePagedList(String key) {
        if (mLiveCustomerList == null) {
            mLiveCustomerList = new LivePagedListProvider<String, Customer>() {
                @Override
                protected DataSource<String, Customer> createDataSource() {
                    return new LastNameAscCustomerDataSource(mDatabase);
                }
            }.create(key,
                    new PagedList.Config.Builder()
                            .setPageSize(10)
                            .setEnablePlaceholders(false)
                            .build());
        }
        return mLiveCustomerList;
    }
}
