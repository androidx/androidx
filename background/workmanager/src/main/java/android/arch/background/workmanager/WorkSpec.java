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

package android.arch.background.workmanager;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Stores information about a logical chain of work.
 */
@Entity
class WorkSpec {

    @ColumnInfo(name = "id")
    @PrimaryKey
    String mId;

    // TODO(xbhatnag)
    @ColumnInfo(name = "repeat_duration")
    long mRepeatDuration;

    // TODO(xbhatnag)
    @ColumnInfo(name = "flex_duration")
    long mFlexDuration;

    WorkSpec(String id) {
        mId = id;
    }
}
