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

package android.arch.persistence.room.migration;

import android.arch.persistence.db.SupportSQLiteDatabase;

/**
 * Base class for a database migration.
 * <p>
 * Each migration can move between 2 versions that are defined by {@link #startVersion} and
 * {@link #endVersion}.
 * <p>
 * Usually, you would need 1 Migration class for each version change but you can also provide
 * Migrations that can handle multiple version changes.
 * <p>
 * Room expects the migrated table to have the exact same structure as if it is created from
 * scratch. This means the order of columns in the table must also be modified during the migration
 * if necessary.
 */
public abstract class Migration {
    public final int startVersion;
    public final int endVersion;

    /**
     * Creates a new migration between {@code startVersion} and {@code endVersion}.
     *
     * @param startVersion The start version of the database.
     * @param endVersion The end version of the database after this migration is applied.
     */
    public Migration(int startVersion, int endVersion) {
        this.startVersion = startVersion;
        this.endVersion = endVersion;
    }

    /**
     * Should run the necessary migrations.
     * <p>
     * This class cannot access any generated Dao in this method.
     *
     * @param database The database instance
     */
    public abstract void migrate(SupportSQLiteDatabase database);
}
