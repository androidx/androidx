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

package android.arch.persistence.room.migration.bundle;

import android.support.annotation.RestrictTo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Data class that holds the information about a database schema export.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaBundle {

    @SerializedName("formatVersion")
    private int mFormatVersion;
    @SerializedName("database")
    private DatabaseBundle mDatabase;

    private static final Gson GSON;
    private static final String CHARSET = "UTF-8";
    public static final int LATEST_FORMAT = 1;
    static {
        GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public SchemaBundle(int formatVersion, DatabaseBundle database) {
        mFormatVersion = formatVersion;
        mDatabase = database;
    }

    @SuppressWarnings("unused")
    public int getFormatVersion() {
        return mFormatVersion;
    }

    public DatabaseBundle getDatabase() {
        return mDatabase;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static SchemaBundle deserialize(InputStream fis)
            throws UnsupportedEncodingException {
        InputStreamReader is = new InputStreamReader(fis, CHARSET);
        try {
            return GSON.fromJson(is, SchemaBundle.class);
        } finally {
            safeClose(is);
            safeClose(fis);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void serialize(SchemaBundle bundle, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw = new OutputStreamWriter(fos, CHARSET);
        try {
            GSON.toJson(bundle, osw);
        } finally {
            safeClose(osw);
            safeClose(fos);
        }
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ignored) {
            }
        }
    }

}
