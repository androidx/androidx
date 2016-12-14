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
 * Full movie data object.
 */
public class MovieDataFull implements Parcelable {
    public String Title;
    public String Year;
    public String imdbID;
    public String Type;
    public String Poster;
    public String Rated;
    public String Released;
    public String Runtime;
    public String Genre;
    public String Director;
    public String Writer;
    public String Actors;
    public String Plot;
    public String Language;
    public String Country;
    public String Awards;
    public String Metascore;
    public String imdbRating;
    public String imdbVotes;
    public String Response;

    public MovieDataFull() {
    }

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
        out.writeString(Rated);
        out.writeString(Released);
        out.writeString(Runtime);
        out.writeString(Genre);
        out.writeString(Director);
        out.writeString(Writer);
        out.writeString(Actors);
        out.writeString(Plot);
        out.writeString(Language);
        out.writeString(Country);
        out.writeString(Awards);
        out.writeString(Metascore);
        out.writeString(imdbRating);
        out.writeString(imdbVotes);
        out.writeString(Response);
    }

    public static final Parcelable.Creator<MovieDataFull> CREATOR =
            new Parcelable.Creator<MovieDataFull>() {
            public MovieDataFull createFromParcel(Parcel in) {
                return new MovieDataFull(in);
            }

            public MovieDataFull[] newArray(int size) {
                return new MovieDataFull[size];
            }
    };

    private MovieDataFull(Parcel in) {
        Title = in.readString();
        Year = in.readString();
        imdbID = in.readString();
        Type = in.readString();
        Poster = in.readString();
        Rated = in.readString();
        Released = in.readString();
        Runtime = in.readString();
        Genre = in.readString();
        Director = in.readString();
        Writer = in.readString();
        Actors = in.readString();
        Plot = in.readString();
        Language = in.readString();
        Country = in.readString();
        Awards = in.readString();
        Metascore = in.readString();
        imdbRating = in.readString();
        imdbVotes = in.readString();
        Response = in.readString();
    }
}
