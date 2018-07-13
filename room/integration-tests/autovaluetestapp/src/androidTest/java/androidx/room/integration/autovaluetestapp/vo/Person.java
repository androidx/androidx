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

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity
public abstract class Person {
    @CopyAnnotations
    @PrimaryKey(autoGenerate = true)
    public abstract long getId();
    public abstract String getFirstName();
    public abstract String getLastName();
    @CopyAnnotations
    @Ignore
    public abstract boolean isCoolPerson();

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public static Person create(long id, String firstName, String lastName) {
        return builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .coolPerson(false)
                .build();
    }

    @Ignore
    public static Person create(String firstName, String lastName) {
        return create(0 /* auto-generate id */, firstName, lastName);
    }

    public static Builder builder() {
        return new AutoValue_Person.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder id(long id);
        abstract Builder firstName(String firstName);
        abstract Builder lastName(String lastName);
        abstract Builder coolPerson(boolean coolPerson);
        abstract Person build();
    }
}
