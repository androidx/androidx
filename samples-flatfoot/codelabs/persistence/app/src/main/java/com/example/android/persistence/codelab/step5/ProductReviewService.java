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
package com.example.android.persistence.codelab.step5;

import java.util.List;

public interface ProductReviewService {
    /**
     * Returns list of all available products
     */
    List<Product> fetchProducts();

    /**
     * Returns list of all available comments for the given productId
     * @param productId id of a product
     * @return list of comments
     */
    List<Comment> fetchComments(int productId);

    /**
     * Deletes a comment by id.
     * @param commentId id of a comment to remove
     */
    void deleteComment(int commentId);

    /**
     * Adds a comment to a product with the given {@code productId} and the given text
     * @param productId id of a commented product
     * @param text text of a comment
     * @return a created comment
     */
    Comment addComment(int productId, String text);
}
