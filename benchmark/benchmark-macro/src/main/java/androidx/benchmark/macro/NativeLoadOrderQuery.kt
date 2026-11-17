/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.processNameLikePkg

internal class LoadOrderEntry(
    val file: String,
    val buildId: String,
    val relPc: Long,
    val ts: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoadOrderEntry

        if (file != other.file) return false
        if (buildId != other.buildId) return false
        if (relPc != other.relPc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + buildId.hashCode()
        result = 31 * result + relPc.hashCode()
        return result
    }

    override fun toString(): String {
        return "LoadOrderEntry(file='$file', buildId='$buildId', relPc=$relPc, ts=$ts)"
    }
}

internal object NativeLoadOrderQuery {

    internal fun getQuery(
        targetPackageName: String,
        restrictToDataDirectory: Boolean = true,
    ): String {
        val filePattern = if (restrictToDataDirectory) "/data%.so" else "%.so"
        // Materializing callsite and frame metadata into indexed SQLite TEMP tables avoids
        // millions of per-step Perfetto virtual-table cursor lookups during recursive stack
        // traversal, and pre-filtering target `.so` frames + threads upfront prunes irrelevant
        // work before grouping by integer `frame_id`.
        return """
            DROP TABLE IF EXISTS _lo_mappings;
            CREATE TEMP TABLE _lo_mappings (
              id INTEGER PRIMARY KEY,
              file_name TEXT NOT NULL,
              build_id TEXT NOT NULL
            );
            INSERT INTO _lo_mappings
            SELECT
              id,
              name,
              COALESCE(build_id, '')
            FROM stack_profile_mapping
            WHERE name LIKE '$filePattern';

            DROP TABLE IF EXISTS _lo_frames;
            CREATE TEMP TABLE _lo_frames (
              frame_id INTEGER PRIMARY KEY,
              mapping INTEGER NOT NULL,
              rel_pc INTEGER NOT NULL
            );
            INSERT INTO _lo_frames
            SELECT
              f.id,
              f.mapping,
              f.rel_pc
            FROM stack_profile_frame f
            JOIN _lo_mappings m ON f.mapping = m.id;

            DROP TABLE IF EXISTS _lo_threads;
            CREATE TEMP TABLE _lo_threads (
              utid INTEGER PRIMARY KEY
            );
            INSERT INTO _lo_threads
            SELECT t.utid
            FROM process
            JOIN thread t USING (upid)
            WHERE ${processNameLikePkg(targetPackageName)};

            DROP TABLE IF EXISTS _lo_initial_callsites;
            CREATE TEMP TABLE _lo_initial_callsites (
              callsite_id INTEGER PRIMARY KEY,
              ts INTEGER NOT NULL
            );
            INSERT INTO _lo_initial_callsites
            SELECT
              ps.callsite_id,
              MIN(ps.ts)
            FROM perf_sample ps
            JOIN _lo_threads USING (utid)
            WHERE ps.callsite_id IS NOT NULL
              AND EXISTS (SELECT 1 FROM _lo_frames)
            GROUP BY ps.callsite_id;

            DROP TABLE IF EXISTS _lo_callsites;
            CREATE TEMP TABLE _lo_callsites (
              id INTEGER PRIMARY KEY,
              parent_id INTEGER,
              target_frame_id INTEGER
            );
            INSERT INTO _lo_callsites
            SELECT
              c.id,
              c.parent_id,
              f.frame_id
            FROM stack_profile_callsite c
            LEFT JOIN _lo_frames f ON c.frame_id = f.frame_id
            WHERE EXISTS (SELECT 1 FROM _lo_initial_callsites);

            WITH RECURSIVE
              callstack(parent_id, target_frame_id, ts) AS (
                SELECT
                  c.parent_id,
                  c.target_frame_id,
                  ic.ts
                FROM _lo_initial_callsites ic
                JOIN _lo_callsites c ON ic.callsite_id = c.id

                UNION ALL

                SELECT
                  p.parent_id,
                  p.target_frame_id,
                  cs.ts
                FROM callstack cs
                JOIN _lo_callsites p ON cs.parent_id = p.id
              ),
              frame_ts AS (
                SELECT
                  target_frame_id AS frame_id,
                  MIN(ts) AS ts
                FROM callstack
                WHERE target_frame_id IS NOT NULL
                GROUP BY target_frame_id
              )
            SELECT
              MIN(ft.ts) AS timestamp_ns,
              m.file_name AS file_name,
              m.build_id AS build_id,
              f.rel_pc AS rel_pc
            FROM frame_ts ft
            JOIN _lo_frames f USING (frame_id)
            JOIN _lo_mappings m ON f.mapping = m.id
            GROUP BY m.file_name, m.build_id, f.rel_pc
            ORDER BY timestamp_ns ASC;
        """
            .trimIndent()
    }

    internal fun nativeLoadOrderEntries(
        session: TraceProcessor.Session,
        targetPackageName: String,
        // Useful to restrict to things inside the data directory outside of tests.
        restrictToDataDirectory: Boolean = true,
    ): Sequence<LoadOrderEntry> {
        val iterator =
            session.query(
                query =
                    getQuery(
                        targetPackageName = targetPackageName,
                        restrictToDataDirectory = restrictToDataDirectory,
                    )
            )
        return iterator.map { row ->
            LoadOrderEntry(
                file = row.string("file_name"),
                buildId = row.string("build_id"),
                relPc = row.long("rel_pc"),
                ts = row.long("timestamp_ns"),
            )
        }
    }
}
