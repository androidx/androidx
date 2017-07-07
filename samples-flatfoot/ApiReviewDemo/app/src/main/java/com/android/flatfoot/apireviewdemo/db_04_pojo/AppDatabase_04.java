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

package com.android.flatfoot.apireviewdemo.db_04_pojo;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.flatfoot.apireviewdemo.db_03_entity.Pet;

@Database(entities = {User.class, Pet.class}, version = 4)
public abstract class AppDatabase_04 extends RoomDatabase {
    public abstract UserPetDao userPetDao();
}
