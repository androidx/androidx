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

package androidx.room.integration.autovaluetestapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.integration.autovaluetestapp.dao.ParcelableEntityDao;
import androidx.room.integration.autovaluetestapp.dao.PersonDao;
import androidx.room.integration.autovaluetestapp.dao.PetDao;
import androidx.room.integration.autovaluetestapp.vo.ParcelableEntity;
import androidx.room.integration.autovaluetestapp.vo.Person;
import androidx.room.integration.autovaluetestapp.vo.Pet;

@Database(entities = {Person.class, Pet.Cat.class, Pet.Dog.class, ParcelableEntity.class},
        version = 1, exportSchema = false)
public abstract class TestDatabase extends RoomDatabase {
    public abstract PersonDao getPersonDao();
    public abstract PetDao getPetDao();
    public abstract ParcelableEntityDao getParcelableEntityDao();
}
