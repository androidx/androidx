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

package android.arch.persistence.room.integration.testapp.database;

import android.arch.paging.LivePagedListProvider;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Simple Customer DAO for Room Customer list sample.
 */
@Dao
public interface CustomerDao {

    /**
     * Insert a customer
     * @param customer Customer.
     */
    @Insert
    void insert(Customer customer);

    /**
     * Insert multiple customers.
     * @param customers Customers.
     */
    @Insert
    void insertAll(Customer[] customers);

    /**
     * @return LivePagedListProvider of customers, ordered by last name. Call
     * {@link LivePagedListProvider#create(Object, android.arch.paging.PagedList.Config)} to
     * get a LiveData of PagedLists.
     */
    @Query("SELECT * FROM customer ORDER BY mLastName ASC")
    LivePagedListProvider<Integer, Customer> loadPagedAgeOrder();

    /**
     * @return number of customers
     */
    @Query("SELECT COUNT(*) FROM customer")
    int countCustomers();

    // Keyed

    @Query("SELECT * from customer ORDER BY mLastName ASC LIMIT :limit")
    List<Customer> customerNameInitial(int limit);

    @Query("SELECT * from customer WHERE mLastName < :key ORDER BY mLastName DESC LIMIT :limit")
    List<Customer> customerNameLoadAfter(String key, int limit);

    @Query("SELECT COUNT(*) from customer WHERE mLastName < :key ORDER BY mLastName DESC")
    int customerNameCountAfter(String key);

    @Query("SELECT * from customer WHERE mLastName > :key ORDER BY mLastName ASC LIMIT :limit")
    List<Customer> customerNameLoadBefore(String key, int limit);

    @Query("SELECT COUNT(*) from customer WHERE mLastName > :key ORDER BY mLastName ASC")
    int customerNameCountBefore(String key);
}
