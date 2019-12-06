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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.inspection.Connection;
import androidx.inspection.Inspector;
import androidx.inspection.InspectorEnvironment;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Column;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseOpenedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Response;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Table;
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesResponse;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inspector to work with SQLite databases
 */
final class SqliteInspector extends Inspector {
    // TODO: identify all SQLiteDatabase openDatabase methods
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting static final String sOpenDatabaseCommandSignature = "openDatabase"
            + "("
            + "Ljava/io/File;"
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String sQueryTableNames =
            "SELECT name FROM sqlite_master WHERE type='table'";

    private static final String sQueryTableInfo = "PRAGMA table_info(%s)";

    // TODO: decide if to expose the 'android_metadata' table
    private static final Set<String> sHiddenTables = new HashSet<>(Collections.singletonList(
            "android_metadata"));

    private final DatabaseRegistry mDatabaseRegistry = new DatabaseRegistry();
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
                handleTrackDatabases(callback);
            } else if (command.hasGetSchema()) {
                handleGetSchema(command.getGetSchema(), callback);
            } else {
                // TODO: handle unrecognised command
            }
        } catch (InvalidProtocolBufferException exception) {
            // TODO: decide on error handling strategy
        }
    }

    private void handleTrackDatabases(CommandCallback callback) {
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

    private void handleGetSchema(GetSchemaCommand command, CommandCallback callback) {
        final int databaseId = command.getDatabaseId();
        SQLiteDatabase database = mDatabaseRegistry.getDatabase(databaseId);

        callback.reply((database != null
                        ? querySchema(database)
                        : createErrorOccurredResponse("No database with id=" + databaseId, null)
                ).toByteArray()
        );
    }

    private @NonNull Response querySchema(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery(sQueryTableNames, null);
        List<String> tableNames = new ArrayList<>();
        while (cursor.moveToNext()) {
            String table = cursor.getString(0);
            if (!sHiddenTables.contains(table)) {
                tableNames.add(table);
            }
        }
        cursor.close();

        GetSchemaResponse.Builder schemaBuilder = GetSchemaResponse.newBuilder();
        for (String table : tableNames) {
            Table.Builder tableBuilder = Table.newBuilder();
            tableBuilder.setName(table);
            String query = String.format(sQueryTableInfo, table);
            Cursor tableInfo = database.rawQuery(query, null);
            int nameIndex = tableInfo.getColumnIndex("name");
            int typeIndex = tableInfo.getColumnIndex("type");
            while (tableInfo.moveToNext()) {
                Column column =
                        Column.newBuilder()
                                .setName(tableInfo.getString(nameIndex))
                                .setType(tableInfo.getString(typeIndex))
                                .build();
                tableBuilder.addColumns(column);
            }
            schemaBuilder.addTables(tableBuilder.build());
            tableInfo.close();
        }
        return Response.newBuilder().setGetSchema(schemaBuilder.build()).build();
    }

    @SuppressWarnings("WeakerAccess")
        // avoiding a synthetic accessor
    void onDatabaseAdded(SQLiteDatabase database) {
        Event response;
        try {
            int id = mDatabaseRegistry.addDatabase(database);
            String name = database.getPath();
            response = createDatabaseOpenedEvent(id, name);
        } catch (IllegalArgumentException exception) {
            response = createErrorOccurredEvent(exception);
        }

        getConnection().sendEvent(response.toByteArray());
    }

    private Event createDatabaseOpenedEvent(int id, String name) {
        return Event.newBuilder().setDatabaseOpened(
                DatabaseOpenedEvent.newBuilder().setDatabaseId(id).setName(name).build())
                .build();
    }

    private Event createErrorOccurredEvent(IllegalArgumentException exception) {
        return Event.newBuilder().setErrorOccurred(
                ErrorOccurredEvent.newBuilder()
                        .setMessage(exception.getMessage())
                        .setStackTrace(stackTraceFromException(exception))
                        .build())
                .build();
    }

    private Response createErrorOccurredResponse(@NonNull String message,
            @SuppressWarnings("SameParameterValue") @Nullable String stackTrace) {
        return Response.newBuilder().setErrorOccurred(
                ErrorOccurredResponse.newBuilder()
                        .setMessage(message)
                        .setStackTrace(stackTrace)
                        .build())
                .build();
    }

    @NonNull
    private String stackTraceFromException(IllegalArgumentException exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    static class DatabaseRegistry {
        private final Object mLock = new Object();

        // starting from '1' to distinguish from '0' which could stand for an unset parameter
        @GuardedBy("mLock") private int mNextId = 1;

        @GuardedBy("mLock") private final Map<Integer, SQLiteDatabase> mDatabases = new HashMap<>();

        /**
         * Thread safe
         *
         * @return id used to track the database
         * @throws IllegalArgumentException if database is already in the registry
         */
        int addDatabase(@NonNull SQLiteDatabase database) {
            synchronized (mLock) {
                // TODO: decide if compare by path or object-reference; for now using reference
                // TODO: decide if the same database object here twice an Exception
                // TODO: decide if to track database close events and update here
                // TODO: decide if use weak-references to database objects
                // TODO: consider database.acquireReference() approach

                // check if already tracked
                for (Map.Entry<Integer, SQLiteDatabase> entry : mDatabases.entrySet()) {
                    if (entry.getValue() == database) {
                        throw new IllegalArgumentException("Database is already tracked.");
                    }
                }

                // make a new entry
                int id = mNextId++;
                mDatabases.put(id, database);
                return id;
            }
        }

        @Nullable SQLiteDatabase getDatabase(int databaseId) {
            synchronized (mLock) {
                return mDatabases.get(databaseId);
            }
        }
    }
}
