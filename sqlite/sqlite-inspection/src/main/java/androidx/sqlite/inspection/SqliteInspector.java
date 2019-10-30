/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.sqlite.inspection;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.inspection.Connection;
import androidx.inspection.Inspector;
import androidx.inspection.InspectorEnvironment;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command;
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesResponse;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inspector to work with SQLite databases
 */
final class SqliteInspector extends Inspector {
    // TODO: identify all SQLiteDatabase openDatabase methods
    @VisibleForTesting static String sOpenDatabaseCommandSignature = "openDatabase"
            + "("
            + "Ljava/io/File;"
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private final AtomicInteger mNextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, SQLiteDatabase> mDatabases = new ConcurrentHashMap<>();

    private final InspectorEnvironment mEnvironment;

    SqliteInspector(@NonNull Connection connection, InspectorEnvironment environment) {
        super(connection);
        mEnvironment = environment;
    }

    @Override
    public void onReceiveCommand(@NonNull byte[] data, @NonNull CommandCallback callback) {
        try {
            Command command = Command.parseFrom(data);
            if (command.hasTrackDatabases()) {
                onTrackDatabases(callback);
            } else {
                // TODO: handle unrecognised command
            }
        } catch (InvalidProtocolBufferException exception) {
            // TODO: decide on error handling strategy
        }
    }

    private void onTrackDatabases(CommandCallback callback) {
        callback.reply(SqliteInspectorProtocol.Response.newBuilder().setTrackDatabases(
                TrackDatabasesResponse.getDefaultInstance()).build().toByteArray());

        mEnvironment.registerExitHook(
                SQLiteDatabase.class,
                sOpenDatabaseCommandSignature,
                new InspectorEnvironment.ExitHook<SQLiteDatabase>() {
                    @Override
                    public SQLiteDatabase onExit(SQLiteDatabase database) {
                        onDatabaseAdded(database);
                        return database;
                    }
                });

        List<SQLiteDatabase> instances = mEnvironment.findInstances(SQLiteDatabase.class);
        for (SQLiteDatabase instance : instances) {
            onDatabaseAdded(instance);
        }
    }

    private void onDatabaseAdded(SQLiteDatabase database) {
        // TODO: implement onDatabaseAdded
    }
}
