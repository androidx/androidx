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

package androidx.room.integration.autovaluetestapp.vo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

public abstract class Pet {

    @CopyAnnotations
    @PrimaryKey(autoGenerate = true)
    public abstract long getPetId();
    @CopyAnnotations
    @PrimaryKey(autoGenerate = true)
    public abstract long getOwnerId();

    @AutoValue
    @Entity
    public abstract static class Cat extends Pet {
        @CopyAnnotations
        @ColumnInfo(name = "kittyName")
        public abstract String getName();

        public static Cat create(long petId, long ownerId, String name) {
            return new AutoValue_Pet_Cat(petId, ownerId, name);
        }
    }

    @AutoValue
    @Entity
    public abstract static class Dog extends Pet {
        @CopyAnnotations
        @ColumnInfo(name = "doggoName")
        public abstract String getName();

        public static Dog create(long petId, long ownerId, String name) {
            return new AutoValue_Pet_Dog(petId, ownerId, name);
        }
    }
}
