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

package androidx.room.integration.testapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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
     * @return number of customers
     */
    @Query("SELECT COUNT(*) FROM customer")
    int countCustomers();

    /**
     * @return All customers
     */
    @Query("SELECT * FROM customer")
    LiveData<List<Customer>> all();

    /**
     * @return True if customer is found
     */
    @Query("SELECT 1 FROM customer WHERE mId = :id AND mName = :name AND mLastName = :lastName")
    boolean contains(int id, String name, String lastName);
}
