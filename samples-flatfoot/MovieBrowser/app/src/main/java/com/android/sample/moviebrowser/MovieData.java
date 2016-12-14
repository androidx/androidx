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
package com.android.sample.moviebrowser;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Short movie data object.
 */
public class MovieData implements Parcelable {
    public String Title;
    public String Year;
    public String imdbID;
    public String Type;
    public String Poster;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(Title);
        out.writeString(Year);
        out.writeString(imdbID);
        out.writeString(Type);
        out.writeString(Poster);
    }

    public static final Parcelable.Creator<MovieData> CREATOR =
            new Parcelable.Creator<MovieData>() {
        public MovieData createFromParcel(Parcel in) {
            return new MovieData(in);
        }

        public MovieData[] newArray(int size) {
            return new MovieData[size];
        }
    };

    private MovieData(Parcel in) {
        Title = in.readString();
        Year = in.readString();
        imdbID = in.readString();
        Type = in.readString();
        Poster = in.readString();
    }
}
