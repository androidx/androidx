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

    internal fun getQuery(targetPackageName: String): String {
        return """
            WITH RECURSIVE callstack AS (
              SELECT
                ps.id AS sample_id,
                ps.ts,
                t.name AS thread_name,
                c.id AS callsite_id,
                c.parent_id,
                c.frame_id,
                0 AS depth
              FROM perf_sample ps
              JOIN thread t USING (utid)
              JOIN process USING (upid)
              JOIN stack_profile_callsite c ON ps.callsite_id = c.id
              WHERE ${processNameLikePkg(targetPackageName)}

              UNION ALL

              SELECT
                cs.sample_id,
                cs.ts,
                cs.thread_name,
                p.id AS callsite_id,
                p.parent_id,
                p.frame_id,
                cs.depth + 1 AS depth
              FROM callstack cs
              JOIN stack_profile_callsite p ON cs.parent_id = p.id
            )
            SELECT
              cs.sample_id,
              cs.ts AS timestamp_ns,
              cs.thread_name,
              cs.depth,
              COALESCE(m.name, '[anonymous / unmapped]') AS file_name,
              COALESCE(m.build_id, '') AS build_id,
              f.rel_pc AS rel_pc,
              COALESCE(f.name, '') AS function_name
            FROM callstack cs
            JOIN stack_profile_frame f ON cs.frame_id = f.id
            JOIN stack_profile_mapping m ON f.mapping = m.id
            ORDER BY cs.ts ASC;
        """
            .trimIndent()
    }

    internal fun nativeLoadOrderEntries(
        session: TraceProcessor.Session,
        targetPackageName: String,
        // Useful to restrict to things inside the data directory outside of tests.
        restrictToDataDirectory: Boolean = true,
    ): Sequence<LoadOrderEntry> {
        val iterator = session.query(query = getQuery(targetPackageName))
        val set = mutableSetOf<LoadOrderEntry>()
        val entries = iterator.mapNotNull { row ->
            val file = row.string("file_name")
            val buildId = row.string("build_id")
            val relPc = row.long("rel_pc")
            val symbol = row.string("function_name")
            val ts = row.long("timestamp_ns")
            when {
                file.isBlank() || !file.endsWith(".so") -> null
                restrictToDataDirectory && !file.startsWith("/data") -> null
                else -> {
                    val entry =
                        LoadOrderEntry(
                            file = file,
                            buildId = buildId,
                            relPc = relPc,
                            name = symbol,
                            ts = ts,
                        )
                    if (entry !in set) {
                        set += entry
                        entry
                    } else {
                        null
                    }
                }
            }
        }
        return entries
    }
}
