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

package androidx.room.integration.testapp.vo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.integration.testapp.TestDatabase;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@TypeConverters({TestDatabase.Converters.class})
public class User {

    @PrimaryKey
    private int mId;

    private String mName;

    private String mLastName;

    private int mAge;

    private boolean mAdmin;

    private float mWeight;

    private Date mBirthday;

    @ColumnInfo(name = "custommm", collate = ColumnInfo.NOCASE)
    private String mCustomField;

    // bit flags
    private Set<Day> mWorkDays = new HashSet<>();

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String lastName) {
        this.mLastName = lastName;
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int age) {
        this.mAge = age;
    }

    public boolean isAdmin() {
        return mAdmin;
    }

    public void setAdmin(boolean admin) {
        mAdmin = admin;
    }

    public float getWeight() {
        return mWeight;
    }

    public void setWeight(float weight) {
        mWeight = weight;
    }

    public Date getBirthday() {
        return mBirthday;
    }

    public void setBirthday(Date birthday) {
        mBirthday = birthday;
    }

    public String getCustomField() {
        return mCustomField;
    }

    public void setCustomField(String customField) {
        mCustomField = customField;
    }

    public Set<Day> getWorkDays() {
        return mWorkDays;
    }

    public void setWorkDays(
            Set<Day> workDays) {
        mWorkDays = workDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (mId != user.mId) return false;
        if (mAge != user.mAge) return false;
        if (mAdmin != user.mAdmin) return false;
        if (Float.compare(user.mWeight, mWeight) != 0) return false;
        if (mName != null ? !mName.equals(user.mName) : user.mName != null) return false;
        if (mLastName != null ? !mLastName.equals(user.mLastName) : user.mLastName != null) {
            return false;
        }
        if (mBirthday != null ? !mBirthday.equals(user.mBirthday) : user.mBirthday != null) {
            return false;
        }
        if (mCustomField != null ? !mCustomField.equals(user.mCustomField)
                : user.mCustomField != null) {
            return false;
        }
        return mWorkDays != null ? mWorkDays.equals(user.mWorkDays) : user.mWorkDays == null;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mLastName != null ? mLastName.hashCode() : 0);
        result = 31 * result + mAge;
        result = 31 * result + (mAdmin ? 1 : 0);
        result = 31 * result + (mWeight != +0.0f ? Float.floatToIntBits(mWeight) : 0);
        result = 31 * result + (mBirthday != null ? mBirthday.hashCode() : 0);
        result = 31 * result + (mCustomField != null ? mCustomField.hashCode() : 0);
        result = 31 * result + (mWorkDays != null ? mWorkDays.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{"
                + "mId=" + mId
                + ", mName='" + mName + '\''
                + ", mLastName='" + mLastName + '\''
                + ", mAge=" + mAge
                + ", mAdmin=" + mAdmin
                + ", mWeight=" + mWeight
                + ", mBirthday=" + mBirthday
                + ", mCustomField='" + mCustomField + '\''
                + ", mWorkDays=" + mWorkDays
                + '}';
    }
}
