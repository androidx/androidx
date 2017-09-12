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

package android.arch.background.workmanager;

import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link Blueprint}s.
 */
@Dao
public interface BlueprintDao {

    /**
     * @param id The identifier
     * @return The {@link Blueprint} associated with that identifier
     */
    @Query("SELECT * FROM blueprint WHERE id=:id")
    Blueprint getBlueprint(int id);

    /**
     * Attempts to insert a Blueprint into the database.
     *
     * @param blueprints The {@link Blueprint}s to insert
     */
    @Insert(onConflict = FAIL)
    void insertBlueprints(List<Blueprint> blueprints);

    /**
     * Updates the status of a {@link Blueprint}.
     *
     * @param id     The identifier for the {@link Blueprint}
     * @param status The new status
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE blueprint SET status=:status WHERE id=:id")
    int setBlueprintStatus(int id, int status);
}
