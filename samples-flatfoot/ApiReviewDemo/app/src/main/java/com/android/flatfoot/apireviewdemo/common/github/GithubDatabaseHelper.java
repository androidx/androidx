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
package com.android.flatfoot.apireviewdemo.common.github;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.android.flatfoot.apireviewdemo.DemoApplication;

/**
 * Database helper.
 */
public class GithubDatabaseHelper {
    private static GithubDatabase sInstance;

    /**
     * Gets a database instance.
     */
    public static synchronized GithubDatabase getDatabase() {
        if (sInstance == null) {
            Context context = DemoApplication.context();
            sInstance = Room.databaseBuilder(context, GithubDatabase.class, "github.db").build();
        }
        return sInstance;
    }

}
