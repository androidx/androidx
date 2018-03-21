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


public class NameAndLastName {
    private String mName;
    private String mLastName;

    public NameAndLastName(String name, String lastName) {
        mName = name;
        mLastName = lastName;
    }

    public String getName() {
        return mName;
    }

    public String getLastName() {
        return mLastName;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameAndLastName that = (NameAndLastName) o;

        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;
        return mLastName != null ? mLastName.equals(that.mLastName) : that.mLastName == null;
    }

    @Override
    public int hashCode() {
        int result = mName != null ? mName.hashCode() : 0;
        result = 31 * result + (mLastName != null ? mLastName.hashCode() : 0);
        return result;
    }
}
