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

import com.example.android.persistence.codelab.step5.Comment;
import com.android.support.room.ColumnInfo;
import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;
import com.android.support.room.TypeConverters;

import java.util.Date;

@Entity(tableName = "comments")
class InternalComment implements Comment {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;
    @ColumnInfo(name = "product_id", index = true)
    private int productId;
    private String text;
    @TypeConverters(DateConverter.class)
    private Date postedAt;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Date getPostedAt() {
        return postedAt;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPostedAt(Date postedAt) {
        this.postedAt = postedAt;
    }

    @Override
    public String toString() {
        return "InternalComment{" + "id=" + id + ", productId=" + productId + ", text='"
                + text + '\'' + ", postedAt=" + postedAt + '}';
    }
}
