/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.arch.persistence.room.integration.testapp;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.integration.testapp.dao.BlobEntityDao;
import android.arch.persistence.room.integration.testapp.dao.PetCoupleDao;
import android.arch.persistence.room.integration.testapp.dao.PetDao;
import android.arch.persistence.room.integration.testapp.dao.ProductDao;
import android.arch.persistence.room.integration.testapp.dao.SchoolDao;
import android.arch.persistence.room.integration.testapp.dao.SpecificDogDao;
import android.arch.persistence.room.integration.testapp.dao.ToyDao;
import android.arch.persistence.room.integration.testapp.dao.UserDao;
import android.arch.persistence.room.integration.testapp.dao.UserPetDao;
import android.arch.persistence.room.integration.testapp.dao.WithClauseDao;
import android.arch.persistence.room.integration.testapp.vo.BlobEntity;
import android.arch.persistence.room.integration.testapp.vo.Pet;
import android.arch.persistence.room.integration.testapp.vo.PetCouple;
import android.arch.persistence.room.integration.testapp.vo.Product;
import android.arch.persistence.room.integration.testapp.vo.School;
import android.arch.persistence.room.integration.testapp.vo.Toy;
import android.arch.persistence.room.integration.testapp.vo.User;

import java.util.Date;

@Database(entities = {User.class, Pet.class, School.class, PetCouple.class, Toy.class,
        BlobEntity.class, Product.class},
        version = 1, exportSchema = false)
@TypeConverters(TestDatabase.Converters.class)
public abstract class TestDatabase extends RoomDatabase {
    public abstract UserDao getUserDao();
    public abstract PetDao getPetDao();
    public abstract UserPetDao getUserPetDao();
    public abstract SchoolDao getSchoolDao();
    public abstract PetCoupleDao getPetCoupleDao();
    public abstract ToyDao getToyDao();
    public abstract BlobEntityDao getBlobEntityDao();
    public abstract ProductDao getProductDao();
    public abstract SpecificDogDao getSpecificDogDao();
    public abstract WithClauseDao getWithClauseDao();

    @SuppressWarnings("unused")
    public static class Converters {
        @TypeConverter
        public Date fromTimestamp(Long value) {
            return value == null ? null : new Date(value);
        }

        @TypeConverter
        public Long dateToTimestamp(Date date) {
            if (date == null) {
                return null;
            } else {
                return date.getTime();
            }
        }
    }
}
