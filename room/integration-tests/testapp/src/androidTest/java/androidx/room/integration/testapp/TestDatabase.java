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

package androidx.room.integration.testapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.integration.testapp.dao.BlobEntityDao;
import androidx.room.integration.testapp.dao.FunnyNamedDao;
import androidx.room.integration.testapp.dao.PetCoupleDao;
import androidx.room.integration.testapp.dao.PetDao;
import androidx.room.integration.testapp.dao.ProductDao;
import androidx.room.integration.testapp.dao.RawDao;
import androidx.room.integration.testapp.dao.SchoolDao;
import androidx.room.integration.testapp.dao.SpecificDogDao;
import androidx.room.integration.testapp.dao.ToyDao;
import androidx.room.integration.testapp.dao.UserDao;
import androidx.room.integration.testapp.dao.UserPetDao;
import androidx.room.integration.testapp.dao.WithClauseDao;
import androidx.room.integration.testapp.vo.BlobEntity;
import androidx.room.integration.testapp.vo.Day;
import androidx.room.integration.testapp.vo.FunnyNamedEntity;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetCouple;
import androidx.room.integration.testapp.vo.Product;
import androidx.room.integration.testapp.vo.School;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Database(entities = {User.class, Pet.class, School.class, PetCouple.class, Toy.class,
        BlobEntity.class, Product.class, FunnyNamedEntity.class},
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
    public abstract FunnyNamedDao getFunnyNamedDao();
    public abstract RawDao getRawDao();

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

        @TypeConverter
        public Set<Day> decomposeDays(int flags) {
            Set<Day> result = new HashSet<>();
            for (Day day : Day.values()) {
                if ((flags & (1 << day.ordinal())) != 0) {
                    result.add(day);
                }
            }
            return result;
        }

        @TypeConverter
        public int composeDays(Set<Day> days) {
            int result = 0;
            for (Day day : days) {
                result |= 1 << day.ordinal();
            }
            return result;
        }
    }
}
