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
    val name: String,
    val ts: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoadOrderEntry

        if (file != other.file) return false
        if (buildId != other.buildId) return false
        if (relPc != other.relPc) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + buildId.hashCode()
        result = 31 * result + relPc.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "LoadOrderEntry(file='$file', buildId='$buildId', relPc=$relPc, name='$name', ts=$ts)"
    }
}

internal object NativeLoadOrderQuery {

    internal fun getQuery(
        targetPackageName: String,
        restrictToDataDirectory: Boolean = true,
    ): String {
        val filePattern = if (restrictToDataDirectory) "/data%.so" else "%.so"
        return """
            WITH RECURSIVE
              initial_callsites AS (
                SELECT
                  ps.callsite_id,
                  MIN(ps.ts) AS ts
                FROM perf_sample ps
                JOIN thread t USING (utid)
                JOIN process USING (upid)
                WHERE ${processNameLikePkg(targetPackageName)}
                AND ps.callsite_id IS NOT NULL
                GROUP BY ps.callsite_id
              ),
              callstack AS (
                SELECT
                  c.id AS callsite_id,
                  c.parent_id,
                  c.frame_id,
                  ic.ts
                FROM initial_callsites ic
                JOIN stack_profile_callsite c ON ic.callsite_id = c.id

                UNION ALL

                SELECT
                  p.id AS callsite_id,
                  p.parent_id,
                  p.frame_id,
                  cs.ts
                FROM callstack cs
                JOIN stack_profile_callsite p ON cs.parent_id = p.id
              ),
              frames AS (
                SELECT
                  frame_id,
                  MIN(ts) AS ts
                FROM callstack
                GROUP BY frame_id
              )
            SELECT
              MIN(frames.ts) AS timestamp_ns,
              m.name AS file_name,
              COALESCE(m.build_id, '') AS build_id,
              f.rel_pc AS rel_pc,
              COALESCE(f.name, '') AS function_name
            FROM frames
            JOIN stack_profile_frame f ON frames.frame_id = f.id
            JOIN stack_profile_mapping m ON f.mapping = m.id
            WHERE m.name LIKE '$filePattern'
            GROUP BY file_name, build_id, rel_pc, function_name
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
                name = row.string("function_name"),
                ts = row.long("timestamp_ns"),
            )
        }
    }
}
