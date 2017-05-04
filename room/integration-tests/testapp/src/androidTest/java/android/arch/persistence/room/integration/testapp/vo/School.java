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

package android.arch.persistence.room.integration.testapp.vo;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class School {
    @PrimaryKey
    private int mId;
    private String mName;
    @Embedded(prefix = "address_")
    public Address address;

    @Embedded(prefix = "manager_")
    private User mManager;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public User getManager() {
        return mManager;
    }

    public void setManager(User manager) {
        mManager = manager;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        School school = (School) o;

        if (mId != school.mId) {
            return false;
        }
        if (mName != null ? !mName.equals(school.mName) : school.mName != null) {
            return false;
        }
        if (address != null ? !address.equals(school.address) : school.address != null) {
            return false;
        }
        return mManager != null ? mManager.equals(school.mManager) : school.mManager == null;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (mManager != null ? mManager.hashCode() : 0);
        return result;
    }
}
