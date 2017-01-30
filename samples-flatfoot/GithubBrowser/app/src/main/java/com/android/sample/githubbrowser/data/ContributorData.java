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
package com.android.sample.githubbrowser.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;

/**
 * Contributor data object.
 */
@Entity
public class ContributorData implements Parcelable {
    @PrimaryKey
    public String login;
    public String id;
    public String avatar_url;
    public String repos_url;
    public String type;
    public int contributions;

    public ContributorData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(login);
        out.writeString(id);
        out.writeString(avatar_url);
        out.writeString(repos_url);
        out.writeString(type);
        out.writeInt(contributions);
    }

    public static final Creator<ContributorData> CREATOR = new Creator<ContributorData>() {
        public ContributorData createFromParcel(Parcel in) {
            return new ContributorData(in);
        }

        public ContributorData[] newArray(int size) {
            return new ContributorData[size];
        }
    };

    private ContributorData(Parcel in) {
        login = in.readString();
        id = in.readString();
        avatar_url = in.readString();
        repos_url = in.readString();
        type = in.readString();
        contributions = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContributorData that = (ContributorData) o;

        if (contributions != that.contributions) return false;
        if (login != null ? !login.equals(that.login) : that.login != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (avatar_url != null ? !avatar_url.equals(that.avatar_url) : that.avatar_url != null) {
            return false;
        }
        if (repos_url != null ? !repos_url.equals(that.repos_url) : that.repos_url != null) {
            return false;
        }
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = login != null ? login.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (avatar_url != null ? avatar_url.hashCode() : 0);
        result = 31 * result + (repos_url != null ? repos_url.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + contributions;
        return result;
    }
}
