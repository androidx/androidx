/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.room.ColumnInfo;
import androidx.room.Relation;

import java.util.List;

public class UserIdAndPetIds {
    @ColumnInfo(name = "mId")
    private final int mUserId;
    @Relation(entity = Pet.class, parentColumn = "mId", entityColumn = "mUserId",
            projection = "mPetId")
    private final List<Integer> mIds;

    public UserIdAndPetIds(int userId, List<Integer> ids) {
        this.mUserId = userId;
        this.mIds = ids;
    }

    public int getUserId() {
        return mUserId;
    }

    public List<Integer> getIds() {
        return mIds;
    }
}
