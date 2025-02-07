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

import org.jspecify.annotations.NonNull;

import java.util.UUID;

@Entity
public class Robot {

    @PrimaryKey
    // This project is tested against a version of the room compiler that doesn't recognize JSpecify
    // for primary keys
    @SuppressWarnings("JSpecifyNullness")
    @androidx.annotation.NonNull
    public final UUID mId;
    public final @NonNull UUID mHiveId;

    public Robot(@NonNull UUID id, @NonNull UUID hiveId) {
        mId = id;
        mHiveId = hiveId;
    }
}
