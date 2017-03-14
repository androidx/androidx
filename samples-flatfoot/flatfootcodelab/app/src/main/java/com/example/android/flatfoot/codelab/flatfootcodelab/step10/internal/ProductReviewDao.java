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

import com.android.support.room.Dao;
import com.android.support.room.Insert;
import com.android.support.room.Query;

import java.util.List;

@Dao
abstract class ProductReviewDao {
    @Insert
    abstract void insertProducts(List<InternalProduct> products);

    @Insert
    abstract void insertComments(List<InternalComment> comments);

    @Insert
    abstract void insertComment(InternalComment comment);

    @Query("Select * from comments where product_id = :productId")
    abstract List<InternalComment> getComments(int productId);

    @Query("Select * from products")
    abstract List<InternalProduct> getProducts();

    @Query("Select * from products where id = :productId")
    abstract InternalProduct getProduct(int productId);

    @Query("Delete from comments where id = :commentId")
    abstract void delete(int commentId);
}
