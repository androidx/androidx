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
package com.android.sample.githubbrowser;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contributor data object.
 */
public class ContributorData implements Parcelable {
    public String login;
    public String id;
    public String avatar_url;
    public String repos_url;
    public String type;
    public int contributions;

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
}
