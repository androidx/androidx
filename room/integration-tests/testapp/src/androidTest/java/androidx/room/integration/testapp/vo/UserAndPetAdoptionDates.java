/*
 * Copyright 2017 The Android Open Source Project
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

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.Date;
import java.util.List;

public class UserAndPetAdoptionDates {
    @Embedded
    public final User user;
    @Relation(entity = Pet.class,
            parentColumn = "mId",
            entityColumn = "mUserId",
            projection = "mAdoptionDate")
    public List<Date> petAdoptionDates;

    public UserAndPetAdoptionDates(User user) {
        this.user = user;
    }

    @Ignore
    public UserAndPetAdoptionDates(User user, List<Date> petAdoptionDates) {
        this.user = user;
        this.petAdoptionDates = petAdoptionDates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAndPetAdoptionDates that = (UserAndPetAdoptionDates) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        return petAdoptionDates != null ? petAdoptionDates.equals(that.petAdoptionDates)
                : that.petAdoptionDates == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (petAdoptionDates != null ? petAdoptionDates.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserAndPetAdoptionDates{"
                + "user=" + user
                + ", petAdoptionDates=" + petAdoptionDates
                + '}';
    }
}
