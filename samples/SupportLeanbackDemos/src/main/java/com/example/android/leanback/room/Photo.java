/*
 * Copyright 2020 The Android Open Source Project
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
package com.example.android.leanback.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Photo
 */
@Entity
public class Photo {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int mId;
    @ColumnInfo(name = "title")
    private String mTitle;
    @ColumnInfo(name = "imgResourceId")
    private int mImgResourceId;
    Photo(int id, String title, int imgResourceId) {
        this.mTitle = title;
        this.mImgResourceId = imgResourceId;
        this.mId = id;
    }
    public int getImgResourceId() {
        return mImgResourceId;
    }
    public String getTitle() {
        return mTitle;
    }
    public int getId() {
        return mId;
    }
}
