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

package com.android.support.room.integration.testapp.vo;

import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey
    private int mId;
    private String mName;
    private String mLastName;
    private int mAge;
    private boolean mAdmin;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (mId != user.mId) return false;
        if (mAge != user.mAge) return false;
        if (mAdmin != user.mAdmin) return false;
        if (mName != null ? !mName.equals(user.mName) : user.mName != null) return false;
        return mLastName != null ? mLastName.equals(user.mLastName) : user.mLastName == null;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mLastName != null ? mLastName.hashCode() : 0);
        result = 31 * result + mAge;
        result = 31 * result + (mAdmin ? 1 : 0);
        return result;
    }
}
