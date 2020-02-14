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
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.inspection.Connection;
import androidx.inspection.Inspector;
import androidx.inspection.InspectorEnvironment;
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Column;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseOpenedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryParameterValue;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Response;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Row;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Table;
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesResponse;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String sOpenDatabaseCommandSignature = "openDatabase"
            + "("
            + "Ljava/io/File;"
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    // Note: this only works on API26+ because of pragma_* functions
    // TODO: replace with a resource file
    private static final String sQueryTableInfo = "select\n"
            + "  m.type as type,\n"
            + "  m.name as tableName,\n"
            + "  ti.name as columnName,\n"
            + "  ti.type as columnType,\n"
            + "  [notnull],\n"
            + "  pk,\n"
            + "  ifnull([unique], 0) as [unique]\n"
            + "from sqlite_master AS m, pragma_table_info(m.name) as ti\n"
            + "left outer join\n"
            + "  (\n"
            + "    select tableName, name as columnName, ti.[unique]\n"
            + "    from\n"
            + "      (\n"
            + "        select m.name as tableName, il.name as indexName, il.[unique]\n"
            + "        from\n"
            + "          sqlite_master AS m,\n"
            + "          pragma_index_list(m.name) AS il,\n"
            + "          pragma_index_info(il.name) as ii\n"
            + "        where il.[unique] = 1\n"
            + "        group by il.name\n"
            + "        having count(*) = 1  -- countOfColumnsInIndex=1\n"
            + "      )\n"
            + "        as ti,  -- tableName|indexName|unique : unique=1 and "
            + "countOfColumnsInIndex=1\n"
            + "      pragma_index_info(ti.indexName)\n"
            + "  )\n"
            + "    as tci  -- tableName|columnName|unique : unique=1 and countOfColumnsInIndex=1\n"
            + "  on tci.tableName = m.name and tci.columnName = ti.name\n"
            + "where m.type in ('table')\n"
            + "order by type, tableName, ti.cid  -- cid = columnId";

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
            } else if (command.hasQuery()) {
                handleQuery(command.getQuery(), callback);
            } else {
                // TODO: handle unrecognised command
            }
        } catch (InvalidProtocolBufferException exception) {
            // TODO: decide on error handling strategy
        }
    }

    private void handleTrackDatabases(CommandCallback callback) {
        callback.reply(Response.newBuilder()
                .setTrackDatabases(TrackDatabasesResponse.getDefaultInstance())
                .build().toByteArray()
        );

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
        SQLiteDatabase database = handleDatabaseId(command.getDatabaseId(), callback);
        if (database == null) return;

        callback.reply(querySchema(database).toByteArray());
    }

    private void handleQuery(QueryCommand command, CommandCallback callback) {
        SQLiteDatabase database = handleDatabaseId(command.getDatabaseId(), callback);
        if (database == null) return;

        String[] params = parseQueryParameterValues(command);
        Cursor cursor = rawQuery(database, command.getQuery(), params, null);
        try {
            List<String> columnNames = Arrays.asList(cursor.getColumnNames());
            callback.reply(Response.newBuilder()
                    .setQuery(QueryResponse.newBuilder()
                            .addAllRows(convert(cursor))
                            .addAllColumnNames(columnNames)
                            .build())
                    .build()
                    .toByteArray()
            );
        } finally {
            cursor.close();
        }
    }

    private Cursor rawQuery(SQLiteDatabase database, String queryText, final String[] params,
            CancellationSignal cancellationSignal) {
        SQLiteDatabase.CursorFactory cursorFactory = new SQLiteDatabase.CursorFactory() {
            @Override
            public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
                    String editTable, SQLiteQuery query) {
                for (int i = 0; i < params.length; i++) {
                    String value = params[i];
                    int index = i + 1;
                    if (value == null) {
                        query.bindNull(index);
                    } else {
                        query.bindString(index, value);
                    }
                }
                return new SQLiteCursor(driver, editTable, query);
            }
        };

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                ? database.rawQueryWithFactory(cursorFactory, queryText, null, null,
                cancellationSignal)
                : database.rawQueryWithFactory(cursorFactory, queryText, null, null);
    }

    @NonNull
    private String[] parseQueryParameterValues(QueryCommand command) {
        String[] params = new String[command.getQueryParameterValuesCount()];
        for (int i = 0; i < command.getQueryParameterValuesCount(); i++) {
            QueryParameterValue param = command.getQueryParameterValues(i);
            switch (param.getOneOfCase()) {
                case STRING_VALUE:
                    params[i] = param.getStringValue();
                    break;
                case ONEOF_NOT_SET:
                    params[i] = null;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported parameter type. OneOfCase=" + param.getOneOfCase());
            }
        }
        return params;
    }

    /**
     * Tries to find a database for an id. If no such database is found, it replies with an
     * {@link ErrorOccurredResponse} via the {@code callback} provided.
     *
     * @return null if no database found for the provided id. A database reference otherwise.
     */
    @Nullable
    private SQLiteDatabase handleDatabaseId(int databaseId, CommandCallback callback) {
        SQLiteDatabase database = mDatabaseRegistry.getDatabase(databaseId);
        if (database == null) {
            replyNoDatabaseWithId(callback, databaseId);
            return null;
        }
        return database;
    }

    private static List<Row> convert(Cursor cursor) {
        List<Row> result = new ArrayList<>();
        int columnCount = cursor.getColumnCount();
        while (cursor.moveToNext()) {
            Row.Builder rowBuilder = Row.newBuilder();
            for (int i = 0; i < columnCount; i++) {
                CellValue value = readValue(cursor, i);
                rowBuilder.addValues(value);
            }
            result.add(rowBuilder.build());
        }
        return result;
    }

    private static CellValue readValue(Cursor cursor, int index) {
        CellValue.Builder builder = CellValue.newBuilder();

        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_NULL:
                // no field to set
                break;
            case Cursor.FIELD_TYPE_BLOB:
                builder.setBlobValue(ByteString.copyFrom(cursor.getBlob(index)));
                break;
            case Cursor.FIELD_TYPE_STRING:
                builder.setStringValue(cursor.getString(index));
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                builder.setIntValue(cursor.getInt(index));
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                builder.setFloatValue(cursor.getFloat(index));
                break;
        }

        return builder.build();
    }

    private void replyNoDatabaseWithId(CommandCallback callback, int databaseId) {
        callback.reply(createErrorOccurredResponse("No database with id=" + databaseId,
                null).toByteArray());
    }

    private @NonNull Response querySchema(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery(sQueryTableInfo, null);
        try {
            GetSchemaResponse.Builder schemaBuilder = GetSchemaResponse.newBuilder();

            int tableNameIx = cursor.getColumnIndex("tableName");
            int columnNameIx = cursor.getColumnIndex("columnName");
            int typeIx = cursor.getColumnIndex("columnType");
            int pkIx = cursor.getColumnIndex("pk");
            int notNullIx = cursor.getColumnIndex("notnull");
            int uniqueIx = cursor.getColumnIndex("unique");

            Table.Builder tableBuilder = null;
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(tableNameIx);

                // ignore certain tables
                if (sHiddenTables.contains(tableName)) {
                    continue;
                }

                // check if getting data for a new table or appending columns to the current one
                if (tableBuilder == null || !tableBuilder.getName().equals(tableName)) {
                    if (tableBuilder != null) {
                        schemaBuilder.addTables(tableBuilder.build());
                    }
                    tableBuilder = Table.newBuilder();
                    tableBuilder.setName(tableName);
                }

                // append column information to the current table info
                tableBuilder.addColumns(Column.newBuilder()
                        .setName(cursor.getString(columnNameIx))
                        .setType(cursor.getString(typeIx))
                        .setPrimaryKey(cursor.getInt(pkIx))
                        .setIsNotNull(cursor.getInt(notNullIx) > 0)
                        .setIsUnique(cursor.getInt(uniqueIx) > 0)
                        .build()
                );
            }
            if (tableBuilder != null) {
                schemaBuilder.addTables(tableBuilder.build());
            }

            return Response.newBuilder().setGetSchema(schemaBuilder.build()).build();
        } finally {
            cursor.close();
        }
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

    private Event createErrorOccurredEvent(@NonNull Exception exception) {
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
    private String stackTraceFromException(Exception exception) {
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
