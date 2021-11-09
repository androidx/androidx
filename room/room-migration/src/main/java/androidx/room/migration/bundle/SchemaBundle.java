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

package androidx.room.migration.bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SchemaBundle implements SchemaEquality<SchemaBundle> {

    @SerializedName("formatVersion")
    private int mFormatVersion;
    @SerializedName("database")
    private DatabaseBundle mDatabase;

    private static final Gson GSON;
    private static final String CHARSET = "UTF-8";
    public static final int LATEST_FORMAT = 1;

    static {
        GSON = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(new EntityTypeAdapterFactory())
                .create();
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
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static SchemaBundle deserialize(InputStream fis)
            throws UnsupportedEncodingException {
        InputStreamReader is = new InputStreamReader(fis, CHARSET);
        try {
            SchemaBundle result = GSON.fromJson(is, SchemaBundle.class);
            if (result == null || result.getDatabase() == null) {
                throw new IllegalStateException("Invalid schema file");
            }
            return result;
        } finally {
            safeClose(is);
            safeClose(fis);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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

    @Override
    public boolean isSchemaEqual(SchemaBundle other) {
        return SchemaEqualityUtil.checkSchemaEquality(mDatabase, other.mDatabase)
                && mFormatVersion == other.mFormatVersion;
    }

    private static class EntityTypeAdapterFactory implements TypeAdapterFactory {
        EntityTypeAdapterFactory() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!EntityBundle.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
            TypeAdapter<EntityBundle> entityBundleAdapter = gson.getDelegateAdapter(this,
                    TypeToken.get(EntityBundle.class));
            TypeAdapter<FtsEntityBundle> ftsEntityBundleAdapter = gson.getDelegateAdapter(this,
                    TypeToken.get(FtsEntityBundle.class));
            return (TypeAdapter<T>) new EntityTypeAdapter(
                    jsonElementAdapter, entityBundleAdapter, ftsEntityBundleAdapter);
        }

        private static class EntityTypeAdapter extends TypeAdapter<EntityBundle> {

            private final TypeAdapter<JsonElement> mJsonElementAdapter;
            private final TypeAdapter<EntityBundle> mEntityBundleAdapter;
            private final TypeAdapter<FtsEntityBundle> mFtsEntityBundleAdapter;

            EntityTypeAdapter(
                    TypeAdapter<JsonElement> jsonElementAdapter,
                    TypeAdapter<EntityBundle> entityBundleAdapter,
                    TypeAdapter<FtsEntityBundle> ftsEntityBundleAdapter) {
                this.mJsonElementAdapter = jsonElementAdapter;
                this.mEntityBundleAdapter = entityBundleAdapter;
                this.mFtsEntityBundleAdapter = ftsEntityBundleAdapter;
            }

            @Override
            public void write(JsonWriter out, EntityBundle value) throws IOException {
                if (value instanceof FtsEntityBundle) {
                    mFtsEntityBundleAdapter.write(out, (FtsEntityBundle) value);
                } else {
                    mEntityBundleAdapter.write(out, value);
                }
            }

            @Override
            public EntityBundle read(JsonReader in) throws IOException {
                JsonObject jsonObject = mJsonElementAdapter.read(in).getAsJsonObject();
                if (jsonObject.has("ftsVersion")) {
                    return mFtsEntityBundleAdapter.fromJsonTree(jsonObject);
                } else {
                    return mEntityBundleAdapter.fromJsonTree(jsonObject);
                }
            }
        }
    }
}
