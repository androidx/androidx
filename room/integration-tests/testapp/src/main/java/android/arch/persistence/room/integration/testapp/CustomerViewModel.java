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
import android.arch.persistence.room.Room;
import android.arch.persistence.room.integration.testapp.database.Customer;
import android.arch.persistence.room.integration.testapp.database.SampleDatabase;
import android.arch.util.paging.PagedList;

import java.util.UUID;

/**
 * Sample database-backed view model of Customers
 */
public class CustomerViewModel extends AndroidViewModel {
    private SampleDatabase mDatabase;
    private LiveData<PagedList<Customer>> mLiveCustomerList;
    private static int sCustomerId = 0;

    public CustomerViewModel(Application application) {
        super(application);
        createDb();
        mLiveCustomerList = mDatabase.getCustomerDao().loadPagedAgeOrder().create(0, 10);
    }

    private void createDb() {
        mDatabase = Room.inMemoryDatabaseBuilder(this.getApplication(),
                SampleDatabase.class).build();
    }

    public void setDatabase(SampleDatabase database) {
        mDatabase = database;
    }

    void insertCustomer() {
        AppToolkitTaskExecutor.getInstance().executeOnDiskIO(new Runnable() {
            @Override
            public void run() {
                Customer customer = new Customer();
                customer.setId(sCustomerId++);
                customer.setName(UUID.randomUUID().toString());
                customer.setLastName(UUID.randomUUID().toString());
                mDatabase.getCustomerDao().insert(customer);
            }
        });
    }

    LiveData<PagedList<Customer>> getLivePagedList() {
        return mLiveCustomerList;
    }
}
