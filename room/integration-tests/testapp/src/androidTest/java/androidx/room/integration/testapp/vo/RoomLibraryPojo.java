/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.testapp.vo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.RoomWarnings;

/**
 * A Room POJO is just wraps a POJO provided by some library.
 */
@Entity(tableName = "library_items", ignoredColumns = "mJsonObj")
@SuppressWarnings(RoomWarnings.MISMATCHED_GETTER)
public class RoomLibraryPojo extends LibraryPojo {

    @PrimaryKey
    private long mId;
    private String mName;
    private Long mPrice;

    public RoomLibraryPojo(long id, String name, Long price) {
        setId(id);
        setName(name);
        setPrice(price);
    }

}
