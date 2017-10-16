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

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;

public class Address {
    @ColumnInfo(name = "street")
    private String mStreet;
    @ColumnInfo(name = "state")
    private String mState;
    @ColumnInfo(name = "post_code")
    private int mPostCode;
    @Embedded
    private Coordinates mCoordinates;

    public String getStreet() {
        return mStreet;
    }

    public void setStreet(String street) {
        mStreet = street;
    }

    public String getState() {
        return mState;
    }

    public void setState(String state) {
        mState = state;
    }

    public int getPostCode() {
        return mPostCode;
    }

    public void setPostCode(int postCode) {
        mPostCode = postCode;
    }

    public Coordinates getCoordinates() {
        return mCoordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        mCoordinates = coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (mPostCode != address.mPostCode) return false;
        if (mStreet != null ? !mStreet.equals(address.mStreet) : address.mStreet != null) {
            return false;
        }
        if (mState != null ? !mState.equals(address.mState) : address.mState != null) {
            return false;
        }
        return mCoordinates != null ? mCoordinates.equals(address.mCoordinates)
                : address.mCoordinates == null;
    }

    @Override
    public int hashCode() {
        int result = mStreet != null ? mStreet.hashCode() : 0;
        result = 31 * result + (mState != null ? mState.hashCode() : 0);
        result = 31 * result + mPostCode;
        result = 31 * result + (mCoordinates != null ? mCoordinates.hashCode() : 0);
        return result;
    }
}
