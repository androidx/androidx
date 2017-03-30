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

package com.example.android.persistence.codelab.step5.internal;

import android.database.Cursor;

import com.example.android.persistence.codelab.step5.Comment;
import com.example.android.persistence.codelab.step5.Product;
import com.example.android.persistence.codelab.step5.ProductReviewService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ProductReviewServiceImpl implements ProductReviewService {

    private static final String[] FIRST = new String[]{"A", "B", "C", "D", "E", "F"};
    private static final String[] SECOND = new String[]{"A", "B", "C", "D", "E", "F"};
    private static final String[] DESCRIPTION = new String[]{"L", "M", "N", "O", "P", "Q"};
    private static final String[] COMMENTS = new String[]{
            "Comment 1", "Comment 2", "Comment 3", "Comment 4", "Comment 5", "Comment 6",
    };

    final ProductsDatabase db;

    ProductReviewServiceImpl(ProductsDatabase db) {
        this.db = db;
    }

    void randDelay() {
        try {
            Thread.sleep((long) (1000 + 1000 * (Math.random() % 3)));
        } catch (InterruptedException ignored) {}
    }

    void initializeDb() {
        ArrayList<InternalProduct> products = new ArrayList<>(FIRST.length * SECOND.length);
        for (int i = 0; i < FIRST.length; i++) {
            for (int j = 0; j < SECOND.length; j++) {
                InternalProduct product = new InternalProduct();
                product.setName(FIRST[i] + " " + SECOND[j]);
                product.setDescription(product.getName() + " is " + DESCRIPTION[j]);
                product.setPrice(((i + 1) * (j + 1) * 37 + 4 * i + j) % 239);
                product.setId(FIRST.length * i + j + 1);
                products.add(product);
            }
        }
        db.dao().insertProducts(products);
        ArrayList<InternalComment> comments = new ArrayList<>();
        for (Product product : products) {
            int commentsNumber = product.getPrice() / 38;
            for (int i = 0; i < commentsNumber; i++) {
                InternalComment comment = new InternalComment();
                comment.setProductId(product.getId());
                comment.setText(COMMENTS[i]);
                comment.setPostedAt(new Date(System.currentTimeMillis()
                        - TimeUnit.DAYS.toMillis(commentsNumber - i) + TimeUnit.HOURS.toMillis(i)));
                comments.add(comment);
            }
        }
        db.dao().insertComments(comments);
    }

    @Override
    public List<Product> fetchProducts() {
        randDelay();
        ArrayList<Product> products = new ArrayList<>();
        products.addAll(db.dao().getProducts());
        return products;
    }

    @Override
    public List<Comment> fetchComments(int productId) {
        randDelay();
        ArrayList<Comment> comments = new ArrayList<>();
        comments.addAll(db.dao().getComments(productId));
        return comments;
    }

    @Override
    public void deleteComment(int commentId) {
        randDelay();
        db.dao().delete(commentId);
    }

    @Override
    public Comment addComment(int productId, String text) {
        randDelay();
        InternalProduct product = db.dao().getProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product with id = " + productId + " doesn't exist");
        }
        InternalComment internalComment = new InternalComment();
        internalComment.setText(text);
        internalComment.setProductId(productId);
        internalComment.setPostedAt(new Date());
        db.beginTransaction();
        db.dao().insertComment(internalComment);
        Cursor cursor = db.getOpenHelper().getReadableDatabase().rawQuery(
                "Select last_insert_rowid() from comments;", null);
        cursor.moveToFirst();
        int id = cursor.getInt(0);
        db.setTransactionSuccessful();
        db.endTransaction();
        internalComment.setId(id);
        return internalComment;
    }
}
