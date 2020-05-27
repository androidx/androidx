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

import static android.database.DatabaseUtils.getSqlStatementType;

import static androidx.sqlite.inspection.SqliteInspectionExecutors.directExecutor;

import android.annotation.SuppressLint;
import android.app.Application;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteClosable;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.inspection.Connection;
import androidx.inspection.Inspector;
import androidx.inspection.InspectorEnvironment;
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Column;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseClosedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseOpenedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabasePossiblyChangedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorOccurredResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorRecoverability;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Event;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.GetSchemaResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.KeepDatabasesOpenCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.KeepDatabasesOpenResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryParameterValue;
import androidx.sqlite.inspection.SqliteInspectorProtocol.QueryResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Response;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Row;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Table;
import androidx.sqlite.inspection.SqliteInspectorProtocol.TrackDatabasesResponse;

import com.google.protobuf.ByteString;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Inspector to work with SQLite databases
 */
@SuppressWarnings({"TryFinallyCanBeTryWithResources", "SameParameterValue"})
@SuppressLint("SyntheticAccessor")
final class SqliteInspector extends Inspector {
    // TODO: identify all SQLiteDatabase openDatabase methods
    private static final String sOpenDatabaseCommandSignature = "openDatabase"
            + "("
            + "Ljava/io/File;"
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String sCreateInMemoryDatabaseCommandSignature = "createInMemory"
            + "("
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String sAllReferencesReleaseCommandSignature =
            "onAllReferencesReleased()V";

    // SQLiteStatement methods
    private static final List<String> sSqliteStatementExecuteMethodsSignatures = Arrays.asList(
            "execute()V",
            "executeInsert()J",
            "executeUpdateDelete()I");

    private static final int INVALIDATION_MIN_INTERVAL_MS = 1000;

    // Note: this only works on API26+ because of pragma_* functions
    // TODO: replace with a resource file
    // language=SQLite
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
            + "where m.type in ('table', 'view')\n"
            + "order by type, tableName, ti.cid  -- cid = columnId";

    private static final Set<String> sHiddenTables = new HashSet<>(Arrays.asList(
            "android_metadata", "sqlite_sequence"));

    private final DatabaseRegistry mDatabaseRegistry;
    private final InspectorEnvironment mEnvironment;
    private final Executor mIOExecutor;
    private final ScheduledExecutorService mScheduledExecutor;
    /**
     * Utility instance that handles communication with Room's InvalidationTracker instances.
     */
    private final RoomInvalidationRegistry mRoomInvalidationRegistry;

    @NonNull
    private final SqlDelightInvalidation mSqlDelightInvalidation;

    SqliteInspector(@NonNull Connection connection, InspectorEnvironment environment,
            Executor ioExecutor, ScheduledExecutorService scheduledExecutor) {
        super(connection);
        mEnvironment = environment;
        mIOExecutor = ioExecutor;
        mScheduledExecutor = scheduledExecutor;
        mRoomInvalidationRegistry = new RoomInvalidationRegistry(mEnvironment);
        mSqlDelightInvalidation = SqlDelightInvalidation.create(mEnvironment);

        mDatabaseRegistry = new DatabaseRegistry(
                new DatabaseRegistry.Callback() {
                    @Override
                    public void onPostEvent(int databaseId, String path) {
                        dispatchDatabaseOpenedEvent(databaseId, path);
                    }
                },
                new DatabaseRegistry.Callback() {
                    @Override
                    public void onPostEvent(int databaseId, String path) {
                        dispatchDatabaseClosedEvent(databaseId, path);
                    }
                });
    }

    @Override
    public void onReceiveCommand(@NonNull byte[] data, @NonNull CommandCallback callback) {
        try {
            Command command = Command.parseFrom(data);
            switch (command.getOneOfCase()) {
                case TRACK_DATABASES:
                handleTrackDatabases(callback);
                    break;
                case GET_SCHEMA:
                handleGetSchema(command.getGetSchema(), callback);
                    break;
                case QUERY:
                handleQuery(command.getQuery(), callback);
                    break;
                case KEEP_DATABASES_OPEN:
                    handleKeepDatabasesOpen(command.getKeepDatabasesOpen(), callback);
                    break;
                default:
                    callback.reply(
                        createErrorOccurredResponse(
                                "Unrecognised command type: " + command.getOneOfCase().name(),
                                null,
                                true
                        ).toByteArray());
            }
        } catch (Exception exception) {
            callback.reply(
                    createErrorOccurredResponse(
                            "Unhandled Exception while processing the command: "
                                    + exception.getMessage(),
                            stackTraceFromException(exception),
                            null
                    ).toByteArray()
            );
        }
    }

    private void handleTrackDatabases(CommandCallback callback) {
        callback.reply(Response.newBuilder()
                .setTrackDatabases(TrackDatabasesResponse.getDefaultInstance())
                .build().toByteArray()
        );

        registerReleaseReferenceHooks();
        registerDatabaseOpenedHooks();

        EntryExitMatchingHookRegistry hookRegistry = new EntryExitMatchingHookRegistry(
                mEnvironment);

        registerInvalidationHooks(hookRegistry);
        registerDatabaseClosedHooks(hookRegistry);

        // Check for database instances in memory
        for (SQLiteDatabase instance : mEnvironment.findInstances(SQLiteDatabase.class)) {
            /** the race condition here will be handled by mDatabaseRegistry */
            if (instance.isOpen()) {
                onDatabaseOpened(instance);
            } else {
                onDatabaseClosed(instance);
            }
        }

        // Check for database instances on disk
        for (Application instance : mEnvironment.findInstances(Application.class)) {
            for (String name : instance.databaseList()) {
                File path = instance.getDatabasePath(name);
                if (path.exists() && !isHelperSqliteFile(path)) {
                    mDatabaseRegistry.notifyOnDiskDatabase(path.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Tracking potential database closed events via {@link #sAllReferencesReleaseCommandSignature}
     */
    private void registerDatabaseClosedHooks(EntryExitMatchingHookRegistry hookRegistry) {
        hookRegistry.registerHook(SQLiteDatabase.class, sAllReferencesReleaseCommandSignature,
                new EntryExitMatchingHookRegistry.OnExitCallback() {
                    @Override
                    public void onExit(EntryExitMatchingHookRegistry.Frame exitFrame) {
                        final Object thisObject = exitFrame.mThisObject;
                        if (thisObject instanceof SQLiteDatabase) {
                            onDatabaseClosed((SQLiteDatabase) thisObject);
                        }
                    }
                });
    }

    private void registerDatabaseOpenedHooks() {
        for (String method : Arrays.asList(sOpenDatabaseCommandSignature,
                sCreateInMemoryDatabaseCommandSignature)) {
            mEnvironment.registerExitHook(
                    SQLiteDatabase.class,
                    method,
                    new InspectorEnvironment.ExitHook<SQLiteDatabase>() {
                        @SuppressLint("SyntheticAccessor")
                        @Override
                        public SQLiteDatabase onExit(SQLiteDatabase database) {
                            try {
                                onDatabaseOpened(database);
                            } catch (Exception exception) {
                                getConnection().sendEvent(createErrorOccurredEvent(
                                        "Unhandled Exception while processing an onDatabaseAdded "
                                                + "event: "
                                                + exception.getMessage(),
                                        stackTraceFromException(exception), null)
                                        .toByteArray());
                            }
                            return database;
                        }
                    });
        }
    }

    private void registerReleaseReferenceHooks() {
        mEnvironment.registerEntryHook(
                SQLiteClosable.class,
                "releaseReference()V",
                new InspectorEnvironment.EntryHook() {
                    @Override
                    public void onEntry(@Nullable Object thisObject,
                            @NonNull List<Object> args) {
                        if (thisObject instanceof SQLiteDatabase) {
                            mDatabaseRegistry.notifyReleaseReference((SQLiteDatabase) thisObject);
                        }
                    }
                });
    }

    private void registerInvalidationHooks(EntryExitMatchingHookRegistry hookRegistry) {
        /**
         * Schedules a task using {@link mScheduledExecutor} and executes it on {@link mIOExecutor}.
         **/
        final RequestCollapsingThrottler.DeferredExecutor deferredExecutor =
                new RequestCollapsingThrottler.DeferredExecutor() {
                    @Override
                    @SuppressWarnings("FutureReturnValueIgnored") // TODO: handle errors from Future
                    public void schedule(final Runnable command, final long delayMs) {
                        mScheduledExecutor.schedule(new Runnable() {
                            @Override
                            public void run() {
                                mIOExecutor.execute(command);
                            }
                        }, delayMs, TimeUnit.MILLISECONDS);
                    }
                };
        final RequestCollapsingThrottler throttler = new RequestCollapsingThrottler(
                INVALIDATION_MIN_INTERVAL_MS,
                new Runnable() {
                    @Override
                    public void run() {
                        dispatchDatabasePossiblyChangedEvent();
                    }
                }, deferredExecutor);

        registerInvalidationHooksSqliteStatement(throttler);
        registerInvalidationHooksSQLiteCursor(throttler, hookRegistry);
    }

    /**
     * Invalidation hooks triggered by:
     * <ul>
     *     <li>{@link SQLiteStatement#execute}</li>
     *     <li>{@link SQLiteStatement#executeInsert}</li>
     *     <li>{@link SQLiteStatement#executeUpdateDelete}</li>
     * </ul>
     */
    private void registerInvalidationHooksSqliteStatement(
            final RequestCollapsingThrottler throttler) {
        for (String method : sSqliteStatementExecuteMethodsSignatures) {
            mEnvironment.registerExitHook(SQLiteStatement.class, method,
                    new InspectorEnvironment.ExitHook<Object>() {
                        @Override
                        public Object onExit(Object result) {
                            throttler.submitRequest();
                            return result;
                        }
                    });
        }
    }

    /**
     * Invalidation hooks triggered by {@link SQLiteCursor#getCount} and
     * {@link SQLiteCursor#onMove} both of which lead to cursor's query being executed.
     * <p>
     * In order to access cursor's query, we also use {@link SQLiteDatabase#rawQueryWithFactory}
     * which takes a query String and constructs a cursor based on it.
     */
    private void registerInvalidationHooksSQLiteCursor(final RequestCollapsingThrottler throttler,
            EntryExitMatchingHookRegistry hookRegistry) {

        // TODO: add active pruning via Cursor#close listener
        final Map<SQLiteCursor, Void> trackedCursors = Collections.synchronizedMap(
                new WeakHashMap<SQLiteCursor, Void>());

        final String rawQueryMethodSignature = "rawQueryWithFactory("
                + "Landroid/database/sqlite/SQLiteDatabase$CursorFactory;"
                + "Ljava/lang/String;"
                + "[Ljava/lang/String;"
                + "Ljava/lang/String;"
                + "Landroid/os/CancellationSignal;"
                + ")Landroid/database/Cursor;";
        hookRegistry.registerHook(SQLiteDatabase.class,
                rawQueryMethodSignature, new EntryExitMatchingHookRegistry.OnExitCallback() {
                    @Override
                    public void onExit(EntryExitMatchingHookRegistry.Frame exitFrame) {
                        SQLiteCursor cursor = cursorParam(exitFrame.mResult);
                        String query = stringParam(exitFrame.mArgs.get(1));

                        // Only track cursors that might modify the database.
                        // TODO: handle PRAGMA select queries, e.g. PRAGMA_TABLE_INFO
                        if (cursor != null && query != null && getSqlStatementType(query)
                                != DatabaseUtils.STATEMENT_SELECT) {
                            trackedCursors.put(cursor, null);
                        }
                    }
                });

        for (final String method : Arrays.asList("getCount()I", "onMove(II)Z")) {
            hookRegistry.registerHook(SQLiteCursor.class, method,
                    new EntryExitMatchingHookRegistry.OnExitCallback() {
                        @Override
                        public void onExit(EntryExitMatchingHookRegistry.Frame exitFrame) {
                            SQLiteCursor cursor = (SQLiteCursor) exitFrame.mThisObject;
                            if (trackedCursors.containsKey(cursor)) {
                                throttler.submitRequest();
                            }
                        }
                    });
        }
    }

    // Gets a SQLiteCursor from a passed-in Object (if possible)
    private @Nullable SQLiteCursor cursorParam(Object cursor) {
        if (cursor instanceof SQLiteCursor) {
            return (SQLiteCursor) cursor;
        }

        if (cursor instanceof CursorWrapper) {
            CursorWrapper wrapper = (CursorWrapper) cursor;
            return cursorParam(wrapper.getWrappedCursor());
        }

        // TODO: add support for more cursor types
        Log.w(SqliteInspector.class.getName(), String.format(
                "Unsupported Cursor type: %s. Invalidation might not work correctly.", cursor));
        return null;
    }

    // Gets a String from a passed-in Object (if possible)
    private @Nullable String stringParam(Object string) {
        return string instanceof String ? (String) string : null;
    }

    private void dispatchDatabaseOpenedEvent(int databaseId, String path) {
        getConnection().sendEvent(Event.newBuilder().setDatabaseOpened(
                DatabaseOpenedEvent.newBuilder().setDatabaseId(databaseId).setPath(path)
        ).build().toByteArray());
    }

    private void dispatchDatabaseClosedEvent(int databaseId, String path) {
        getConnection().sendEvent(Event.newBuilder().setDatabaseClosed(
                DatabaseClosedEvent.newBuilder().setDatabaseId(databaseId).setPath(path)
        ).build().toByteArray());
    }

    private void dispatchDatabasePossiblyChangedEvent() {
        getConnection().sendEvent(Event.newBuilder().setDatabasePossiblyChanged(
                DatabasePossiblyChangedEvent.getDefaultInstance()
        ).build().toByteArray());
    }

    private void handleGetSchema(GetSchemaCommand command, CommandCallback callback) {
        SQLiteDatabase reference = acquireReference(command.getDatabaseId(), callback);
        if (reference == null) return;

        callback.reply(querySchema(reference).toByteArray());
    }

    private void handleQuery(final QueryCommand command, final CommandCallback callback) {
        final SQLiteDatabase reference = acquireReference(command.getDatabaseId(), callback);
        if (reference == null) return;

        final CancellationSignal cancellationSignal = new CancellationSignal();
        final Future<?> future = SqliteInspectionExecutors.submit(mIOExecutor, new Runnable() {
            @Override
            public void run() {
                String[] params = parseQueryParameterValues(command);
                Cursor cursor = null;
                try {
                    cursor = rawQuery(reference, command.getQuery(), params,
                            cancellationSignal);
                    List<String> columnNames = Arrays.asList(cursor.getColumnNames());
                    callback.reply(Response.newBuilder()
                            .setQuery(QueryResponse.newBuilder()
                                    .addAllRows(convert(cursor))
                                    .addAllColumnNames(columnNames)
                                    .build())
                            .build()
                            .toByteArray()
                    );
                    triggerInvalidation(command.getQuery());
                } catch (SQLiteException | IllegalArgumentException exception) {
                    callback.reply(createErrorOccurredResponse(exception, true).toByteArray());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

            }
        });
        callback.addCancellationListener(directExecutor(), new Runnable() {
            @Override
            public void run() {
                cancellationSignal.cancel();
                future.cancel(true);
            }
        });
    }

    private void triggerInvalidation(String query) {
        if (getSqlStatementType(query) != DatabaseUtils.STATEMENT_SELECT) {
            mSqlDelightInvalidation.triggerInvalidations();
            mRoomInvalidationRegistry.triggerInvalidations();
        }
    }

    private void handleKeepDatabasesOpen(KeepDatabasesOpenCommand keepDatabasesOpen,
            CommandCallback callback) {
        // Acknowledge the command
        callback.reply(Response.newBuilder().setKeepDatabasesOpen(
                KeepDatabasesOpenResponse.getDefaultInstance()
        ).build().toByteArray());

        mDatabaseRegistry.notifyKeepOpenToggle(keepDatabasesOpen.getSetEnabled());
    }

    @SuppressLint("Recycle") // For: "The cursor should be freed up after use with #close"
    private static Cursor rawQuery(@NonNull SQLiteDatabase database, @NonNull String queryText,
            @NonNull final String[] params, @Nullable CancellationSignal cancellationSignal) {
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

        return database.rawQueryWithFactory(cursorFactory, queryText, null, null,
                cancellationSignal);
    }

    @NonNull
    private static String[] parseQueryParameterValues(QueryCommand command) {
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
     * Consumer of this method must release the reference when done using it.
     *
     * @return null if no database found for the provided id. A database reference otherwise.
     */
    @Nullable
    private SQLiteDatabase acquireReference(int databaseId, CommandCallback callback) {
        SQLiteDatabase database = mDatabaseRegistry.acquireReference(databaseId);
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
                builder.setLongValue(cursor.getLong(index));
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                builder.setDoubleValue(cursor.getDouble(index));
                break;
        }

        return builder.build();
    }

    private void replyNoDatabaseWithId(CommandCallback callback, int databaseId) {
        String message = String.format("Unable to perform an operation on database (id=%s)."
                + " The database may have already been closed.", databaseId);
        callback.reply(createErrorOccurredResponse(message, null, true).toByteArray());
    }

    private @NonNull Response querySchema(SQLiteDatabase database) {
        Cursor cursor = null;
        try {
            cursor = rawQuery(database, sQueryTableInfo, new String[0], null);
            GetSchemaResponse.Builder schemaBuilder = GetSchemaResponse.newBuilder();

            int objectTypeIx = cursor.getColumnIndex("type"); // view or table
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
                    tableBuilder.setIsView("view".equalsIgnoreCase(cursor.getString(objectTypeIx)));
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
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // avoiding a synthetic accessor
    void onDatabaseOpened(SQLiteDatabase database) {
        mRoomInvalidationRegistry.invalidateCache();
        mDatabaseRegistry.notifyDatabaseOpened(database);
    }

    @SuppressWarnings("WeakerAccess") // avoiding a synthetic accessor
    void onDatabaseClosed(SQLiteDatabase database) {
        mDatabaseRegistry.notifyAllDatabaseReferencesReleased(database);
    }

    private Event createErrorOccurredEvent(@Nullable String message, @Nullable String stackTrace,
            Boolean isRecoverable) {
        return Event.newBuilder().setErrorOccurred(
                ErrorOccurredEvent.newBuilder()
                        .setContent(
                                createErrorContentMessage(message,
                                        stackTrace,
                                        isRecoverable))
                        .build())
                .build();
    }

    private static ErrorContent createErrorContentMessage(@Nullable String message,
            @Nullable String stackTrace, Boolean isRecoverable) {
        ErrorContent.Builder builder = ErrorContent.newBuilder();
        if (message != null) {
            builder.setMessage(message);
        }
        if (stackTrace != null) {
            builder.setStackTrace(stackTrace);
        }
        ErrorRecoverability.Builder recoverability = ErrorRecoverability.newBuilder();
        if (isRecoverable != null) { // leave unset otherwise, which translates to 'unknown'
            recoverability.setIsRecoverable(isRecoverable);
        }
        builder.setRecoverability(recoverability.build());
        return builder.build();
    }

    private static Response createErrorOccurredResponse(@NonNull Exception exception,
            Boolean isRecoverable) {
        return createErrorOccurredResponse(exception.getMessage(),
                stackTraceFromException(exception), isRecoverable);
    }

    private static Response createErrorOccurredResponse(@Nullable String message,
            @Nullable String stackTrace, Boolean isRecoverable) {
        return Response.newBuilder()
                .setErrorOccurred(
                        ErrorOccurredResponse.newBuilder()
                                .setContent(createErrorContentMessage(message, stackTrace,
                                        isRecoverable)))
                .build();
    }

    @NonNull
    private static String stackTraceFromException(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static boolean isHelperSqliteFile(File file) {
        String path = file.getPath();
        return path.endsWith("-journal") || path.endsWith("-shm") || path.endsWith("-wal");
    }
}
