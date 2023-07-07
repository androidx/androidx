/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.sqlite.inspection.DatabaseExtensions.isInMemoryDatabase;
import static androidx.sqlite.inspection.DatabaseExtensions.pathForDatabase;

import android.database.sqlite.SQLiteDatabase;
import android.util.ArraySet;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The class keeps track of databases under inspection, and can keep database connections open if
 * such option is enabled.
 * <p>Signals expected to be provided to the class:
 * <ul>
 * <li>{@link #notifyDatabaseOpened} - should be called when the inspection code detects a
 * database open operation.
 * <li>{@link #notifyAllDatabaseReferencesReleased} - should be called when the inspection code
 * detects that the last database connection reference has been released (effectively a connection
 * closed event).
 * <li>{@link #notifyKeepOpenToggle} - should be called when the inspection code detects a
 * request to change the keep-database-connection-open setting (enabled|disabled).
 * </ul></p>
 * <p>Callbacks exposed by the class:
 * <ul>
 * <li>Detected a database that is now open, and previously was either closed or not tracked.
 * <li>Detected a database that is now closed, and previously was reported as open.
 * </ul></p>
 */
@SuppressWarnings({"DanglingJavadoc", "SyntheticAccessor"})
class DatabaseRegistry {
    private static final int NOT_TRACKED = -1;

    // Called when tracking state changes (notTracked|closed)->open
    private final Callback mOnOpenedCallback;
    // Called when tracking state changes open->closed
    private final Callback mOnClosedCallback;

    // True if keep-database-connection-open functionality is enabled.
    private boolean mKeepDatabasesOpen = false;

    private final Object mLock = new Object();

    // Starting from '1' to distinguish from '0' which could stand for an unset parameter.
    @GuardedBy("mLock") private int mNextId = 1;

    // TODO: decide if use weak-references to database objects

    /**
     * Database connection id -> a list of database references pointing to the same database. The
     * collection is meant to only contain open connections (eventually consistent after all
     * callbacks queued behind {@link #mLock} are processed).
     */
    @GuardedBy("mLock") private final Map<Integer, Set<SQLiteDatabase>> mDatabases =
            new HashMap<>();

    // Database connection id -> extra database reference used to facilitate the
    // keep-database-connection-open functionality.
    @GuardedBy("mLock") private final Map<Integer, KeepOpenReference> mKeepOpenReferences =
            new HashMap<>();

    // Database path -> database connection id - allowing to report a consistent id for all
    // references pointing to the same path.
    @GuardedBy("mLock") private final Map<String, Integer> mPathToId = new HashMap<>();

    /**
     * @param onOpenedCallback called when tracking state changes (notTracked|closed)->open
     * @param onClosedCallback called when tracking state changes open->closed
     */
    DatabaseRegistry(Callback onOpenedCallback, Callback onClosedCallback) {
        mOnOpenedCallback = onOpenedCallback;
        mOnClosedCallback = onClosedCallback;
    }

    /**
     * Should be called when the inspection code detects a database being open operation.
     * <p> Note that the method should be called before any code has a chance to close the
     * database, so e.g. in an {@link androidx.inspection.ArtTooling.ExitHook#onExit}
     * before the return value is released.
     * Thread-safe.
     */
    void notifyDatabaseOpened(@NonNull SQLiteDatabase database) {
        handleDatabaseSignal(database);
    }

    void notifyReleaseReference(SQLiteDatabase database) {
        synchronized (mLock) {
            /** Prevent all other methods from releasing a reference if a
             *  {@link KeepOpenReference} is present */
            for (KeepOpenReference reference : mKeepOpenReferences.values()) {
                if (reference.mDatabase == database) {
                    /** The below will always succeed as {@link mKeepOpenReferences} only
                     * contains active references:
                     * - we only insert active references into {@link mKeepOpenReferences}
                     * - {@link KeepOpenReference#releaseAllReferences} is the only place where we
                     * allow references to be released
                     * - {@link KeepOpenReference#releaseAllReferences} is private an can only be
                     * called from this class; and before it is called, it must be removed from
                     * from {@link mKeepOpenReferences}
                     */
                    reference.acquireReference();
                }
            }
        }
    }

    /**
     * Should be called when the inspection code detects that the last database connection
     * reference has been released (effectively a connection closed event).
     * Thread-safe.
     */
    void notifyAllDatabaseReferencesReleased(@NonNull SQLiteDatabase database) {
        handleDatabaseSignal(database);
    }

    /**
     * Should be called when the inspection code detects a request to change the
     * keep-database-connection-open setting (enabled|disabled).
     * Thread-safe.
     */
    void notifyKeepOpenToggle(boolean setEnabled) {
        synchronized (mLock) {
            if (mKeepDatabasesOpen == setEnabled) {
                return; // no change
            }

            if (setEnabled) { // allowClose -> keepOpen
                mKeepDatabasesOpen = true;

                for (int id : mDatabases.keySet()) {
                    secureKeepOpenReference(id);
                }
            } else { // keepOpen -> allowClose
                mKeepDatabasesOpen = false;

                Iterator<Map.Entry<Integer, KeepOpenReference>> iterator =
                        mKeepOpenReferences.entrySet().iterator();
                while (iterator.hasNext()) {
                    KeepOpenReference reference = iterator.next().getValue();
                    iterator.remove(); // first remove so it doesn't get in its own way
                    reference.releaseAllReferences(); // then release its references
                }
            }
        }
    }

    /**
     * Should be called at the start of inspection to pre-populate the list of databases with
     * ones on disk.
     */
    void notifyOnDiskDatabase(@NonNull String path) {
        synchronized (mLock) {
            Integer currentId = mPathToId.get(path);
            if (currentId == null) {
                int id = mNextId++;
                mPathToId.put(path, id);
                mOnClosedCallback.onPostEvent(id, path);
            }
        }
    }

    /** Thread-safe */
    private void handleDatabaseSignal(@NonNull SQLiteDatabase database) {
        Integer notifyOpenedId = null;
        Integer notifyClosedId = null;

        synchronized (mLock) {
            int id = getIdForDatabase(database);

            // TODO: revisit the text below since now we're synchronized on the same lock (mLock)
            //  as releaseReference() calls -- which most likely allows for simplifying invariants
            // Guaranteed up to date:
            // - either called in a secure context (e.g. before the newly created connection is
            // returned from the creation; or with an already acquiredReference on it),
            // - or called after the last reference was released which cannot be undone.
            final boolean isOpen = database.isOpen();

            if (id == NOT_TRACKED) { // handling a transition: not tracked -> tracked
                id = mNextId++;
                registerReference(id, database);
                if (isOpen) {
                    notifyOpenedId = id;
                } else {
                    notifyClosedId = id;
                }
            } else if (isOpen) { // handling a transition: tracked(closed) -> tracked(open)
                // There are two scenarios here:
                // - hasReferences is up to date and there is an open reference already, so we
                // don't need to announce a new one
                // - hasReferences is stale, and references in it are queued up to be
                // announced as closing, in this case the outside world thinks that the
                // connection is open (close ones not processed yet), so we don't need to
                // announce anything; later, when processing the queued up closed events nothing
                // will be announced as the currently processed database will keep at least one open
                // connection.
                if (!hasReferences(id)) {
                    notifyOpenedId = id;
                }
                registerReference(id, database);
            } else { // handling a transition: tracked(open) -> tracked(closed)
                // There are two scenarios here:
                // - hasReferences is up to date and we can use it
                // - hasReferences is stale, and references in it are queued up to be
                // announced as closed; in this case there is no harm not announcing a closed
                // event now as the subsequent calls will do it if appropriate
                final boolean hasReferencesPre = hasReferences(id);
                unregisterReference(id, database);
                final boolean hasReferencesPost = hasReferences(id);
                if (hasReferencesPre && !hasReferencesPost) {
                    notifyClosedId = id;
                }
            }

            secureKeepOpenReference(id);

            // notify of changes if any
            if (notifyOpenedId != null) {
                mOnOpenedCallback.onPostEvent(notifyOpenedId, pathForDatabase(database));
            } else if (notifyClosedId != null) {
                mOnClosedCallback.onPostEvent(notifyClosedId, pathForDatabase(database));
            }
        }
    }

    /**
     * Returns a currently active database reference if one is available. Null otherwise.
     * Consumer of this method must release the reference when done using it.
     * Thread-safe
     */
    @Nullable
    SQLiteDatabase getConnection(int databaseId) {
        synchronized (mLock) {
            return getConnectionImpl(databaseId);
        }
    }

    @GuardedBy("mLock")
    private SQLiteDatabase getConnectionImpl(int databaseId) {
        KeepOpenReference keepOpenReference = mKeepOpenReferences.get(databaseId);
        if (keepOpenReference != null) {
            return keepOpenReference.mDatabase;
        }

        final Set<SQLiteDatabase> references = mDatabases.get(databaseId);
        if (references == null) return null;

        // tries to find an open reference preferring write-enabled over read-only
        SQLiteDatabase readOnlyReference = null;
        for (SQLiteDatabase reference : references) {
            if (reference.isOpen()) {
                if (!reference.isReadOnly()) return reference; // write-enabled was found: return it
                readOnlyReference = reference; // remember the read-only reference but keep looking
            }
        }
        return readOnlyReference; // or null if we did not find an open reference
    }

    @GuardedBy("mLock")
    private void registerReference(int id, @NonNull SQLiteDatabase database) {
        Set<SQLiteDatabase> references = mDatabases.get(id);
        if (references == null) {
            references = new ArraySet<>(1);
            mDatabases.put(id, references);
            if (!isInMemoryDatabase(database)) {
                mPathToId.put(pathForDatabase(database), id);
            }
        }
        // mDatabases only tracks open instances
        if (database.isOpen()) {
            references.add(database);
        }
    }

    @GuardedBy("mLock")
    private void unregisterReference(int id, @NonNull SQLiteDatabase database) {
        Set<SQLiteDatabase> references = mDatabases.get(id);
        if (references == null) {
            return;
        }
        references.remove(database);
    }

    @GuardedBy("mLock")
    private void secureKeepOpenReference(int id) {
        if (!mKeepDatabasesOpen || mKeepOpenReferences.containsKey(id)) {
            // Keep-open is disabled or we already have a keep-open-reference for that id.
            return;
        }

        // Try secure a keep-open reference
        SQLiteDatabase reference = getConnectionImpl(id);
        if (reference != null) {
            mKeepOpenReferences.put(id, new KeepOpenReference(reference));
        }
    }

    @GuardedBy("mLock")
    private int getIdForDatabase(SQLiteDatabase database) {
        String databasePath = pathForDatabase(database);

        Integer previousId = mPathToId.get(databasePath);
        if (previousId != null) {
            return previousId;
        }

        if (isInMemoryDatabase(database)) {
            for (Map.Entry<Integer, Set<SQLiteDatabase>> entry : mDatabases.entrySet()) {
                for (SQLiteDatabase entryDb : entry.getValue()) {
                    if (entryDb == database) {
                        return entry.getKey();
                    }
                }
            }
        }

        return NOT_TRACKED;
    }

    @GuardedBy("mLock")
    private boolean hasReferences(int databaseId) {
        final Set<SQLiteDatabase> references = mDatabases.get(databaseId);
        return references != null && !references.isEmpty();
    }

    interface Callback {
        void onPostEvent(int databaseId, String path);
    }

    private static final class KeepOpenReference {
        private final SQLiteDatabase mDatabase;

        private final Object mLock = new Object();
        @GuardedBy("mLock") private int mAcquiredReferenceCount = 0;

        private KeepOpenReference(SQLiteDatabase database) {
            mDatabase = database;
        }

        private void acquireReference() {
            synchronized (mLock) {
                if (DatabaseExtensions.tryAcquireReference(mDatabase)) {
                    mAcquiredReferenceCount++;
                }
            }
        }

        /**
         * This should only be called after removing the object from
         * {@link DatabaseRegistry#mKeepOpenReferences}. Otherwise, the object will get in its
         * own way or releasing its references.
         */
        private void releaseAllReferences() {
            synchronized (mLock) {
                for (; mAcquiredReferenceCount > 0; mAcquiredReferenceCount--) {
                    mDatabase.releaseReference();
                }
            }
        }
    }
}
