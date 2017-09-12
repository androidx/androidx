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

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * The database entity for work.
 */
@Entity
public class WorkItem {

    public static final int STATUS_ENQUEUED = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_SUCCEEDED = 2;
    public static final int STATUS_FAILED = 3;

    @PrimaryKey private int mId;
    private int mStatus; // TODO: IntDef
    private String mWorkerClassName;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        this.mStatus = status;
    }

    public String getWorkerClassName() {
        return mWorkerClassName;
    }

    public void setWorkerClassName(String workerClassName) {
        this.mWorkerClassName = workerClassName;
    }
}
