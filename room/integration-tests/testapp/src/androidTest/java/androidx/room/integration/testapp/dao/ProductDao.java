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

package androidx.room.integration.testapp.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.integration.testapp.vo.Product;

@Dao
public interface ProductDao {

    @Insert
    long insert(@NonNull Product product);

    /**
     * Insert a new product with the given name.
     */
    @Query("INSERT INTO products (name) VALUES (:name)")
    void insert(String name);

    /**
     * Insert a new product with the given name and return the id of the new row.
     */
    @Query("INSERT INTO products (name) VALUES (:name)")
    long insertForLong(String name);

    @Query("INSERT INTO products (name) SELECT 'Product X' WHERE MAX(0, :ints)")
    long insertVarArgs(int... ints);

    @Query("SELECT COUNT(*) FROM products")
    int countProducts();

    @Query("SELECT * FROM products WHERE id = :id")
    Product getProductById(long id);
}
