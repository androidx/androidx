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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Database helper.
 */
public class MovieLocalDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MovieLocal.db";

    private static MovieLocalDbHelper sInstance;

    /* Inner class that defines the table contents */
    private static class MovieDataEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_IMDB_ID = "imdbId";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_POSTER = "poster";
        public static final String COLUMN_NAME_RATED = "rated";
        public static final String COLUMN_NAME_RELEASED = "released";
        public static final String COLUMN_NAME_RUNTIME = "runtime";
        public static final String COLUMN_NAME_DIRECTOR = "director";
        public static final String COLUMN_NAME_WRITER = "writer";
        public static final String COLUMN_NAME_ACTORS = "actors";
        public static final String COLUMN_NAME_PLOT = "plot";
        public static final String COLUMN_NAME_IMDB_RATING = "imdbRating";
        public static final String COLUMN_NAME_IMDB_VOTES = "imdbVotes";
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MovieDataEntry.TABLE_NAME + " ("
                    + MovieDataEntry._ID + " INTEGER PRIMARY KEY,"
                    + MovieDataEntry.COLUMN_NAME_IMDB_ID + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_TITLE + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_YEAR + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_POSTER + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_RATED + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_RELEASED + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_RUNTIME + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_DIRECTOR + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_WRITER + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_ACTORS + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_PLOT + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_IMDB_RATING + " TEXT,"
                    + MovieDataEntry.COLUMN_NAME_IMDB_VOTES + " TEXT )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MovieDataEntry.TABLE_NAME;

    /**
     * Gets a helper instance.
     */
    public static synchronized MovieLocalDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MovieLocalDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Creates a new helper instance.
     */
    private MovieLocalDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Inserts full details into the DB.
     */
    public void insert(MovieDataFull entry) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MovieDataEntry.COLUMN_NAME_IMDB_ID, entry.imdbID);
        values.put(MovieDataEntry.COLUMN_NAME_TITLE, entry.Title);
        values.put(MovieDataEntry.COLUMN_NAME_YEAR, entry.Year);
        values.put(MovieDataEntry.COLUMN_NAME_POSTER, entry.Poster);
        values.put(MovieDataEntry.COLUMN_NAME_RATED, entry.Rated);
        values.put(MovieDataEntry.COLUMN_NAME_RELEASED, entry.Released);
        values.put(MovieDataEntry.COLUMN_NAME_RUNTIME, entry.Runtime);
        values.put(MovieDataEntry.COLUMN_NAME_DIRECTOR, entry.Director);
        values.put(MovieDataEntry.COLUMN_NAME_WRITER, entry.Writer);
        values.put(MovieDataEntry.COLUMN_NAME_ACTORS, entry.Actors);
        values.put(MovieDataEntry.COLUMN_NAME_PLOT, entry.Plot);
        values.put(MovieDataEntry.COLUMN_NAME_IMDB_RATING, entry.imdbRating);
        values.put(MovieDataEntry.COLUMN_NAME_IMDB_VOTES, entry.imdbVotes);

        // Insert the new row
        db.insert(MovieDataEntry.TABLE_NAME, null, values);

        db.close();
    }

    /**
     * Gets full details from the DB.
     */
    public MovieDataFull get(String imdbId) {
        SQLiteDatabase db = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                MovieDataEntry.COLUMN_NAME_IMDB_ID,
                MovieDataEntry.COLUMN_NAME_TITLE,
                MovieDataEntry.COLUMN_NAME_YEAR,
                MovieDataEntry.COLUMN_NAME_POSTER,
                MovieDataEntry.COLUMN_NAME_RATED,
                MovieDataEntry.COLUMN_NAME_RELEASED,
                MovieDataEntry.COLUMN_NAME_RUNTIME,
                MovieDataEntry.COLUMN_NAME_DIRECTOR,
                MovieDataEntry.COLUMN_NAME_WRITER,
                MovieDataEntry.COLUMN_NAME_ACTORS,
                MovieDataEntry.COLUMN_NAME_PLOT,
                MovieDataEntry.COLUMN_NAME_IMDB_RATING,
                MovieDataEntry.COLUMN_NAME_IMDB_VOTES
        };

        // Filter results WHERE "imdbId" = 'passed id'
        String selection = MovieDataEntry.COLUMN_NAME_IMDB_ID + " = ?";
        String[] selectionArgs = {imdbId};

        Cursor c = db.query(
                MovieDataEntry.TABLE_NAME,                // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        MovieDataFull result = null;
        if (c.moveToFirst()) {
            result = new MovieDataFull();

            result.imdbID = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_IMDB_ID));
            result.Title = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_TITLE));
            result.Year = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_YEAR));
            result.Poster = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_POSTER));
            result.Rated = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_RATED));
            result.Released = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_RELEASED));
            result.Runtime = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_RUNTIME));
            result.Director = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_DIRECTOR));
            result.Writer = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_WRITER));
            result.Actors = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_ACTORS));
            result.Plot = c.getString(c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_PLOT));
            result.imdbRating = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_IMDB_RATING));
            result.imdbVotes = c.getString(
                    c.getColumnIndexOrThrow(MovieDataEntry.COLUMN_NAME_IMDB_VOTES));
        }

        c.close();
        db.close();

        return result;
    }

    /**
     * Updates full details in the DB.
     */
    public void update(MovieDataFull entry) {
        SQLiteDatabase db = getReadableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(MovieDataEntry.COLUMN_NAME_TITLE, entry.Title);
        values.put(MovieDataEntry.COLUMN_NAME_YEAR, entry.Year);
        values.put(MovieDataEntry.COLUMN_NAME_POSTER, entry.Poster);
        values.put(MovieDataEntry.COLUMN_NAME_RATED, entry.Rated);
        values.put(MovieDataEntry.COLUMN_NAME_RELEASED, entry.Released);
        values.put(MovieDataEntry.COLUMN_NAME_RUNTIME, entry.Runtime);
        values.put(MovieDataEntry.COLUMN_NAME_DIRECTOR, entry.Director);
        values.put(MovieDataEntry.COLUMN_NAME_WRITER, entry.Writer);
        values.put(MovieDataEntry.COLUMN_NAME_ACTORS, entry.Actors);
        values.put(MovieDataEntry.COLUMN_NAME_PLOT, entry.Plot);
        values.put(MovieDataEntry.COLUMN_NAME_IMDB_RATING, entry.imdbRating);
        values.put(MovieDataEntry.COLUMN_NAME_IMDB_VOTES, entry.imdbVotes);

        // Which row to update, based on the title
        String selection = MovieDataEntry.COLUMN_NAME_IMDB_ID + " = ?";
        String[] selectionArgs = {entry.imdbID};

        db.update(
                MovieDataEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);

        db.close();
    }
}
