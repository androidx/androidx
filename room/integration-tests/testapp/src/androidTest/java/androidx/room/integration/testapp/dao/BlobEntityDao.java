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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.integration.testapp.vo.BlobEntity;

import java.util.List;

@Dao
public interface BlobEntityDao {

    @Insert
    void insert(BlobEntity item);

    @Query("SELECT * FROM BlobEntity")
    List<BlobEntity> selectAll();

    @Query("SELECT content FROM BlobEntity WHERE id = :id")
    byte[] getContent(long id);

    @Query("UPDATE BlobEntity SET content = :content WHERE id = :id")
    void updateContent(long id, byte[] content);
}
