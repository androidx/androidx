/*
 * Copyright 2018 The Android Open Source Project
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

package android.arch.background.workmanager.impl.model;

import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_BLOCKED;
import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_CANCELLED;
import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_ENQUEUED;
import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_RUNNING;
import static android.arch.background.workmanager.BaseWork.WorkStatus.STATUS_SUCCEEDED;

import android.arch.background.workmanager.BaseWork;
import android.arch.persistence.room.TypeConverter;

/**
 * TypeConverters for enums.
 */

public class EnumTypeConverters {

    public static final int ID_STATUS_ENQUEUED = 0;
    public static final int ID_STATUS_RUNNING = 1;
    public static final int ID_STATUS_SUCCEEDED = 2;
    public static final int ID_STATUS_FAILED = 3;
    public static final int ID_STATUS_BLOCKED = 4;
    public static final int ID_STATUS_CANCELLED = 5;

    /**
     * TypeConverter for a WorkStatus to an int.
     *
     * @param workStatus The input WorkStatus
     * @return The associated int constant
     */
    @TypeConverter
    public static int workStatusToInt(BaseWork.WorkStatus workStatus) {
        switch (workStatus) {
            case STATUS_ENQUEUED:
                return ID_STATUS_ENQUEUED;

            case STATUS_RUNNING:
                return ID_STATUS_RUNNING;

            case STATUS_SUCCEEDED:
                return ID_STATUS_SUCCEEDED;

            case STATUS_FAILED:
                return ID_STATUS_FAILED;

            case STATUS_BLOCKED:
                return ID_STATUS_BLOCKED;

            case STATUS_CANCELLED:
                return ID_STATUS_CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + workStatus + " to int");
        }
    }

    /**
     * TypeConverter for an int to a WorkStatus.
     *
     * @param value The input integer
     * @return The associated WorkStatus enum value
     */
    @TypeConverter
    public static BaseWork.WorkStatus intToWorkStatus(int value) {
        switch (value) {
            case ID_STATUS_ENQUEUED:
                return STATUS_ENQUEUED;

            case ID_STATUS_RUNNING:
                return STATUS_RUNNING;

            case ID_STATUS_SUCCEEDED:
                return STATUS_SUCCEEDED;

            case ID_STATUS_FAILED:
                return STATUS_FAILED;

            case ID_STATUS_BLOCKED:
                return STATUS_BLOCKED;

            case ID_STATUS_CANCELLED:
                return STATUS_CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to WorkStatus");
        }
    }

    private EnumTypeConverters() {
    }
}
