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
package com.android.sample.moviebrowser;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Repository data object.
 */
public class RepositoryData implements Parcelable {
    public String id;
    public String name;
    public String full_name;
    public ContributorData owner;
    public String description;
    public String created_at;
    public int stargazers_count;
    public String language;
    public int forks_count;
    public int open_issues_count;
    public ContributorData organization;
    public int subscribers_count;

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

}
