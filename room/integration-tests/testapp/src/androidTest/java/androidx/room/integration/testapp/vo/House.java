/*
 * Copyright (C) 2018 The Android Open Source Project
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
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class House {

    @PrimaryKey
    private final long mHouseId;

    private final long mOwnerId;

    @Embedded
    private final Address mAddress;

    public House(long houseId, long ownerId, Address address) {
        mHouseId = houseId;
        mOwnerId = ownerId;
        mAddress = address;
    }

    public long getHouseId() {
        return mHouseId;
    }

    public long getOwnerId() {
        return mOwnerId;
    }

    public Address getAddress() {
        return mAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        House house = (House) o;

        if (mHouseId != house.mHouseId) return false;
        if (mOwnerId != house.mOwnerId) return false;
        return mAddress != null ? mAddress.equals(house.mAddress) : house.mAddress == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (mHouseId ^ (mHouseId >>> 32));
        result = 31 * result + (int) (mOwnerId ^ (mOwnerId >>> 32));
        result = 31 * result + (mAddress != null ? mAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "House{"
                + "mHouseId=" + mHouseId
                + ", mOwnerId=" + mOwnerId
                + ", mAddress=" + mAddress
                + '}';
    }
}
