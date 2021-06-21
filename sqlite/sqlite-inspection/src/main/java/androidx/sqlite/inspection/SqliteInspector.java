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

import static androidx.sqlite.inspection.DatabaseExtensions.isAttemptAtUsingClosedDatabase;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_DB_CLOSED_DURING_OPERATION;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_ISSUE_WITH_LOCKING_DATABASE;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_ISSUE_WITH_PROCESSING_NEW_DATABASE_CONNECTION;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_ISSUE_WITH_PROCESSING_QUERY;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_NO_OPEN_DATABASE_WITH_REQUESTED_ID;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_UNKNOWN;
import static androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode.ERROR_UNRECOGNISED_COMMAND;

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
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.inspection.ArtTooling;
import androidx.inspection.ArtTooling.EntryHook;
import androidx.inspection.ArtTooling.ExitHook;
import androidx.inspection.Connection;
import androidx.inspection.Inspector;
import androidx.inspection.InspectorEnvironment;
import androidx.sqlite.inspection.SqliteInspectorProtocol.AcquireDatabaseLockCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.AcquireDatabaseLockResponse;
import androidx.sqlite.inspection.SqliteInspectorProtocol.CellValue;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Column;
import androidx.sqlite.inspection.SqliteInspectorProtocol.Command;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseClosedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabaseOpenedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.DatabasePossiblyChangedEvent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ErrorContent.ErrorCode;
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
import androidx.sqlite.inspection.SqliteInspectorProtocol.ReleaseDatabaseLockCommand;
import androidx.sqlite.inspection.SqliteInspectorProtocol.ReleaseDatabaseLockResponse;
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

/**
 * Inspector to work with SQLite databases
 */
@SuppressWarnings({"TryFinallyCanBeTryWithResources", "SameParameterValue"})
@SuppressLint("SyntheticAccessor")
final class SqliteInspector extends Inspector {
    private static final String OPEN_DATABASE_COMMAND_SIGNATURE_API_11 = "openDatabase"
            + "("
            + "Ljava/lang/String;"
            + "Landroid/database/sqlite/SQLiteDatabase$CursorFactory;"
            + "I"
            + "Landroid/database/DatabaseErrorHandler;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String OPEN_DATABASE_COMMAND_SIGNATURE_API_27 = "openDatabase"
            + "("
            + "Ljava/io/File;"
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String CREATE_IN_MEMORY_DATABASE_COMMAND_SIGNATURE_API_27 =
            "createInMemory"
            + "("
            + "Landroid/database/sqlite/SQLiteDatabase$OpenParams;"
            + ")"
            + "Landroid/database/sqlite/SQLiteDatabase;";

    private static final String ALL_REFERENCES_RELEASE_COMMAND_SIGNATURE =
            "onAllReferencesReleased()V";

    // SQLiteStatement methods
    private static final List<String> SQLITE_STATEMENT_EXECUTE_METHODS_SIGNATURES = Arrays.asList(
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
    private final DatabaseLockRegistry mDatabaseLockRegistry;
    private final InspectorEnvironment mEnvironment;
    private final Executor mIOExecutor;

    /**
     * Utility instance that handles communication with Room's InvalidationTracker instances.
     */
    private final RoomInvalidationRegistry mRoomInvalidationRegistry;

    @NonNull
    private final SqlDelightInvalidation mSqlDelightInvalidation;

    SqliteInspector(@NonNull Connection connection, @NonNull InspectorEnvironment environment) {
        super(connection);
        mEnvironment = environment;
        mIOExecutor = environment.executors().io();
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

        mDatabaseLockRegistry = new DatabaseLockRegistry();
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
                case ACQUIRE_DATABASE_LOCK:
                    handleAcquireDatabaseLock(command.getAcquireDatabaseLock(), callback);
                    break;
                case RELEASE_DATABASE_LOCK:
                    handleReleaseDatabaseLock(command.getReleaseDatabaseLock(), callback);
                    break;
                default:
                    callback.reply(
                        createErrorOccurredResponse(
                                "Unrecognised command type: " + command.getOneOfCase().name(),
                                null,
                                true,
                                ERROR_UNRECOGNISED_COMMAND).toByteArray());
            }
        } catch (Exception exception) {
            callback.reply(
                    createErrorOccurredResponse(
                            "Unhandled Exception while processing the command: "
                                    + exception.getMessage(),
                            stackTraceFromException(exception),
                            null,
                            ERROR_UNKNOWN).toByteArray()
            );
        }
    }

    @Override
    public void onDispose() {
        super.onDispose();
        // TODO(161081452): release database locks and keep-open references
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
        for (SQLiteDatabase instance :
                mEnvironment.artTooling().findInstances(SQLiteDatabase.class)) {
            /** the race condition here will be handled by mDatabaseRegistry */
            if (instance.isOpen()) {
                onDatabaseOpened(instance);
            } else {
                onDatabaseClosed(instance);
            }
        }

        // Check for database instances on disk
        for (Application instance : mEnvironment.artTooling().findInstances(Application.class)) {
            for (String name : instance.databaseList()) {
                File path = instance.getDatabasePath(name);
                if (path.exists() && !isHelperSqliteFile(path)) {
                    mDatabaseRegistry.notifyOnDiskDatabase(path.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Secures a lock (transaction) on the database. Note that while the lock is in place, no
     * changes to the database are possible:
     * - the lock prevents other threads from modifying the database,
     * - lock thread, on releasing the lock, rolls-back all changes (transaction is rolled-back).
     */
    @SuppressWarnings("FutureReturnValueIgnored") // code inside the future is exception-proofed
    private void handleAcquireDatabaseLock(
            AcquireDatabaseLockCommand command,
            final CommandCallback callback) {
        final int databaseId = command.getDatabaseId();
        final DatabaseConnection connection = acquireConnection(databaseId, callback);
        if (connection == null) return;

        // Timeout is covered by mDatabaseLockRegistry
        SqliteInspectionExecutors.submit(mIOExecutor, new Runnable() {
            @Override
            public void run() {
                int lockId;
                try {
                    lockId = mDatabaseLockRegistry.acquireLock(databaseId, connection.mDatabase);
                } catch (Exception e) {
                    processLockingException(callback, e, true);
                    return;
                }

                callback.reply(Response.newBuilder().setAcquireDatabaseLock(
                        AcquireDatabaseLockResponse.newBuilder().setLockId(lockId)
                ).build().toByteArray());
            }
        });
    }

    @SuppressWarnings("FutureReturnValueIgnored") // code inside the future is exception-proofed
    private void handleReleaseDatabaseLock(
            final ReleaseDatabaseLockCommand command,
            final CommandCallback callback) {
        // Timeout is covered by mDatabaseLockRegistry
        SqliteInspectionExecutors.submit(mIOExecutor, new Runnable() {
            @Override
            public void run() {
                try {
                    mDatabaseLockRegistry.releaseLock(command.getLockId());
                } catch (Exception e) {
                    processLockingException(callback, e, false);
                    return;
                }
                callback.reply(Response.newBuilder().setReleaseDatabaseLock(
                        ReleaseDatabaseLockResponse.getDefaultInstance()
                ).build().toByteArray());
            }
        });
    }

    /**
     * @param isLockingStage provide true for acquiring a lock; false for releasing a lock
     */
    private void processLockingException(CommandCallback callback, Exception exception,
            boolean isLockingStage) {
        ErrorCode errorCode = ((exception instanceof IllegalStateException)
                && isAttemptAtUsingClosedDatabase((IllegalStateException) exception))
                ? ERROR_DB_CLOSED_DURING_OPERATION
                : ERROR_ISSUE_WITH_LOCKING_DATABASE;

        String message = isLockingStage
                ? "Issue while trying to lock the database for the export operation: "
                : "Issue while trying to unlock the database after the export operation: ";

        Boolean isRecoverable = isLockingStage
                ? true // failure to lock the db should be recoverable
                : null; // not sure if we can recover from a failure to unlock the db, so UNKNOWN

        callback.reply(createErrorOccurredResponse(message, isRecoverable, exception,
                errorCode).toByteArray());
    }

    /**
     * Tracking potential database closed events via
     * {@link #ALL_REFERENCES_RELEASE_COMMAND_SIGNATURE}
     */
    private void registerDatabaseClosedHooks(EntryExitMatchingHookRegistry hookRegistry) {
        hookRegistry.registerHook(SQLiteDatabase.class, ALL_REFERENCES_RELEASE_COMMAND_SIGNATURE,
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
        List<String> methods = (Build.VERSION.SDK_INT < 27)
                ? Arrays.asList(OPEN_DATABASE_COMMAND_SIGNATURE_API_11)
                : Arrays.asList(OPEN_DATABASE_COMMAND_SIGNATURE_API_27,
                        OPEN_DATABASE_COMMAND_SIGNATURE_API_11,
                        CREATE_IN_MEMORY_DATABASE_COMMAND_SIGNATURE_API_27);

        ExitHook<SQLiteDatabase> hook =
                new ExitHook<SQLiteDatabase>() {
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
                                    stackTraceFromException(exception), null,
                                    ERROR_ISSUE_WITH_PROCESSING_NEW_DATABASE_CONNECTION)
                                    .toByteArray());
                        }
                        return database;
                    }
                };
        for (String method : methods) {
            mEnvironment.artTooling().registerExitHook(SQLiteDatabase.class, method, hook);
        }
    }

    private void registerReleaseReferenceHooks() {
        mEnvironment.artTooling().registerEntryHook(
                SQLiteClosable.class,
                "releaseReference()V",
                new EntryHook() {
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
                        mEnvironment.executors().handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mIOExecutor.execute(command);
                            }
                        }, delayMs);
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
        registerInvalidationHooksTransaction(throttler);
        registerInvalidationHooksSQLiteCursor(throttler, hookRegistry);
    }

    /**
     * Triggering invalidation on {@link SQLiteDatabase#endTransaction} allows us to avoid
     * showing incorrect stale values that could originate from a mid-transaction query.
     *
     * TODO: track if transaction committed or rolled back by observing if
     * {@link SQLiteDatabase#setTransactionSuccessful} was called
     */
    private void registerInvalidationHooksTransaction(final RequestCollapsingThrottler throttler) {
        mEnvironment.artTooling().registerExitHook(SQLiteDatabase.class, "endTransaction()V",
                new ExitHook<Object>() {
                    @Override
                    public Object onExit(Object result) {
                        throttler.submitRequest();
                        return result;
                    }
                });
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
        for (String method : SQLITE_STATEMENT_EXECUTE_METHODS_SIGNATURES) {
            mEnvironment.artTooling().registerExitHook(SQLiteStatement.class, method,
                    new ExitHook<Object>() {
                        @Override
                        public Object onExit(Object result) {
                            throttler.submitRequest();
                            return result;
                        }
                    });
        }
    }

    /**
     * Invalidation hooks triggered by {@link SQLiteCursor#close()}
     * which means that the cursor's query was executed.
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


        mEnvironment.artTooling().registerEntryHook(SQLiteCursor.class, "close()V",
                new ArtTooling.EntryHook() {
                    @Override
                    public void onEntry(@Nullable Object thisObject, @NonNull List<Object> args) {
                        if (trackedCursors.containsKey(thisObject)) {
                            throttler.submitRequest();
                        }
                    }
                });
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

    @SuppressWarnings("FutureReturnValueIgnored") // code inside the future is exception-proofed
    private void handleGetSchema(GetSchemaCommand command, final CommandCallback callback) {
        final DatabaseConnection connection = acquireConnection(command.getDatabaseId(), callback);
        if (connection == null) return;

        // TODO: consider a timeout
        SqliteInspectionExecutors.submit(connection.mExecutor, new Runnable() {
            @Override
            public void run() {
                callback.reply(querySchema(connection.mDatabase).toByteArray());
            }
        });
    }

    private void handleQuery(final QueryCommand command, final CommandCallback callback) {
        final DatabaseConnection connection = acquireConnection(command.getDatabaseId(), callback);
        if (connection == null) return;

        final CancellationSignal cancellationSignal = new CancellationSignal();
        final Executor executor = connection.mExecutor;
        // TODO: consider a timeout
        final Future<?> future = SqliteInspectionExecutors.submit(executor, new Runnable() {
            @Override
            public void run() {
                String[] params = parseQueryParameterValues(command);
                Cursor cursor = null;
                try {
                    cursor = rawQuery(connection.mDatabase, command.getQuery(), params,
                            cancellationSignal);

                    long responseSizeLimitHint = command.getResponseSizeLimitHint();
                    // treating unset field as unbounded
                    if (responseSizeLimitHint <= 0) responseSizeLimitHint = Long.MAX_VALUE;

                    List<String> columnNames = Arrays.asList(cursor.getColumnNames());
                    callback.reply(Response.newBuilder()
                            .setQuery(QueryResponse.newBuilder()
                                    .addAllRows(convert(cursor, responseSizeLimitHint))
                                    .addAllColumnNames(columnNames)
                                    .build())
                            .build()
                            .toByteArray()
                    );
                    triggerInvalidation(command.getQuery());
                } catch (SQLiteException | IllegalArgumentException e) {
                    callback.reply(createErrorOccurredResponse(e, true,
                            ERROR_ISSUE_WITH_PROCESSING_QUERY).toByteArray());
                } catch (IllegalStateException e) {
                    if (isAttemptAtUsingClosedDatabase(e)) {
                        callback.reply(createErrorOccurredResponse(e, true,
                                ERROR_DB_CLOSED_DURING_OPERATION).toByteArray());
                    } else {
                        callback.reply(createErrorOccurredResponse(e, null,
                                ERROR_UNKNOWN).toByteArray());
                    }
                } catch (Exception e) {
                    callback.reply(createErrorOccurredResponse(e, null,
                            ERROR_UNKNOWN).toByteArray());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
        callback.addCancellationListener(mEnvironment.executors().primary(), new Runnable() {
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
     * TODO: remove race condition (affects WAL=off)
     * - lock request is received and in the process of being secured
     * - query request is received and since no lock in place, receives an IO Executor
     * - lock request completes and holds a lock on the database
     * - query cannot run because there is a lock in place
     *
     * The race condition can be mitigated by clients by securing a lock synchronously with no
     * other queries in place.
     *
     * @return null if no database found for the provided id. A database reference otherwise.
     */
    @Nullable
    private DatabaseConnection acquireConnection(int databaseId, CommandCallback callback) {
        DatabaseConnection connection = mDatabaseLockRegistry.getConnection(databaseId);
        if (connection != null) {
            // With WAL enabled, we prefer to use the IO executor. With WAL off we don't have a
            // choice and must use the executor that has a lock (transaction) on the database.
            return connection.mDatabase.isWriteAheadLoggingEnabled()
                    ? new DatabaseConnection(connection.mDatabase, mIOExecutor)
                    : connection;
        }

        SQLiteDatabase database = mDatabaseRegistry.getConnection(databaseId);
        if (database == null) {
            replyNoDatabaseWithId(callback, databaseId);
            return null;
        }

        // Given no lock, IO executor is appropriate.
        return new DatabaseConnection(database, mIOExecutor);
    }

    /**
     * @param responseSizeLimitHint expressed in bytes
     */
    private static List<Row> convert(Cursor cursor, long responseSizeLimitHint) {
        long responseSize = 0;
        List<Row> result = new ArrayList<>();
        int columnCount = cursor.getColumnCount();
        while (cursor.moveToNext() && responseSize < responseSizeLimitHint) {
            Row.Builder rowBuilder = Row.newBuilder();
            for (int i = 0; i < columnCount; i++) {
                CellValue value = readValue(cursor, i);
                rowBuilder.addValues(value);
            }
            Row row = rowBuilder.build();
            // Optimistically adding a row before checking the limit. Eliminates the case when a
            // misconfigured client (limit too low) is unable to fetch any results. Row size in
            // SQLite Android is limited to (~2MB), so the worst case scenario is very manageable.
            result.add(row);
            responseSize += row.getSerializedSize();
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
        callback.reply(createErrorOccurredResponse(message, null, true,
                ERROR_NO_OPEN_DATABASE_WITH_REQUESTED_ID).toByteArray());
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
        } catch (IllegalStateException e) {
            if (isAttemptAtUsingClosedDatabase(e)) {
                return createErrorOccurredResponse(e, true,
                        ERROR_DB_CLOSED_DURING_OPERATION);
            } else {
                return createErrorOccurredResponse(e, null,
                        ERROR_UNKNOWN);
            }
        } catch (Exception e) {
            return createErrorOccurredResponse(e, null,
                    ERROR_UNKNOWN);
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
            Boolean isRecoverable, ErrorCode errorCode) {
        return Event.newBuilder().setErrorOccurred(
                ErrorOccurredEvent.newBuilder()
                        .setContent(
                                createErrorContentMessage(message,
                                        stackTrace,
                                        isRecoverable,
                                        errorCode))
                        .build())
                .build();
    }

    private static ErrorContent createErrorContentMessage(@Nullable String message,
            @Nullable String stackTrace, Boolean isRecoverable, ErrorCode errorCode) {
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
        builder.setErrorCode(errorCode);
        return builder.build();
    }

    private static Response createErrorOccurredResponse(@NonNull Exception exception,
            Boolean isRecoverable, ErrorCode errorCode) {
        return createErrorOccurredResponse("", isRecoverable, exception, errorCode);
    }

    private static Response createErrorOccurredResponse(@NonNull String messagePrefix,
            Boolean isRecoverable, @NonNull Exception exception, ErrorCode errorCode) {
        String message = exception.getMessage();
        if (message == null) message = exception.toString();
        return createErrorOccurredResponse(messagePrefix + message,
                stackTraceFromException(exception), isRecoverable, errorCode);
    }

    private static Response createErrorOccurredResponse(@Nullable String message,
            @Nullable String stackTrace, Boolean isRecoverable, ErrorCode errorCode) {
        return Response.newBuilder()
                .setErrorOccurred(
                        ErrorOccurredResponse.newBuilder()
                                .setContent(createErrorContentMessage(message, stackTrace,
                                        isRecoverable, errorCode)))
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

    /**
     * Provides a reference to the database and an executor to access the database.
     *
     * Executor is relevant in the context of locking, where a locked database with WAL disabled
     * needs to run queries on the thread that locked it.
     */
    static final class DatabaseConnection {
        @NonNull final SQLiteDatabase mDatabase;
        @NonNull final Executor mExecutor;

        DatabaseConnection(@NonNull SQLiteDatabase database, @NonNull Executor executor) {
            mDatabase = database;
            mExecutor = executor;
        }
    }
}
