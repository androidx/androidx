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
 * Repository data object.
 */
@Entity
public class RepositoryData implements Parcelable {
    @PrimaryKey public String id;
    public String name;
    public String full_name;
    public PersonData owner;
    public String description;
    public String created_at;
    public int stargazers_count;
    public String language;
    public int forks_count;
    public int open_issues_count;
    public PersonData organization;
    public int subscribers_count;

    public RepositoryData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeString(name);
        out.writeString(full_name);
        out.writeParcelable(owner, 0);
        out.writeString(description);
        out.writeString(created_at);
        out.writeInt(stargazers_count);
        out.writeString(language);
        out.writeInt(forks_count);
        out.writeInt(open_issues_count);
        out.writeParcelable(organization, 0);
        out.writeInt(subscribers_count);
    }

    public static final Creator<RepositoryData> CREATOR = new Creator<RepositoryData>() {
        public RepositoryData createFromParcel(Parcel in) {
            return new RepositoryData(in);
        }

        public RepositoryData[] newArray(int size) {
            return new RepositoryData[size];
        }
    };

    private RepositoryData(Parcel in) {
        id = in.readString();
        name = in.readString();
        full_name = in.readString();
        owner = in.readParcelable(RepositoryData.class.getClassLoader());
        description = in.readString();
        created_at = in.readString();
        stargazers_count = in.readInt();
        language = in.readString();
        forks_count = in.readInt();
        open_issues_count = in.readInt();
        organization = in.readParcelable(RepositoryData.class.getClassLoader());
        subscribers_count = in.readInt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RepositoryData that = (RepositoryData) o;

        if (stargazers_count != that.stargazers_count) return false;
        if (forks_count != that.forks_count) return false;
        if (open_issues_count != that.open_issues_count) return false;
        if (subscribers_count != that.subscribers_count) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (full_name != null ? !full_name.equals(that.full_name) : that.full_name != null) {
            return false;
        }
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (description != null ? !description.equals(that.description)
                : that.description != null) {
            return false;
        }
        if (created_at != null ? !created_at.equals(that.created_at) : that.created_at != null) {
            return false;
        }
        if (language != null ? !language.equals(that.language) : that.language != null) {
            return false;
        }
        return organization != null ? organization.equals(that.organization)
                : that.organization == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (full_name != null ? full_name.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + stargazers_count;
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + forks_count;
        result = 31 * result + open_issues_count;
        result = 31 * result + (organization != null ? organization.hashCode() : 0);
        result = 31 * result + subscribers_count;
        return result;
    }
}
