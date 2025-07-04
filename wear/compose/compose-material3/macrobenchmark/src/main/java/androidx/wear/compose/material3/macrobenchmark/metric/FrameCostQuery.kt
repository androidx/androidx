/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.metric

import androidx.benchmark.traceprocessor.TraceProcessor
import org.intellij.lang.annotations.Language

/** A copy from aosp/3328563 */
@Suppress("INVISIBLE_MEMBER", "OPT_IN_USAGE_ERROR")
internal object FrameCostQuery {
    @Language("sql")
    private fun getFullQuery(packageName: String) =
        """
        SELECT
          a.name as id,
          -- an adjusted "frame time" in milliseconds that is just the "work" done on ui thread
          -- and render thread, none of the waiting/scheduling.
          CAST(doFrame.dur - post.dur + drawFrame.dur - dequeue.dur AS FLOAT)  / 1000000 as frameCost
        FROM actual_frame_timeline_slice a
        INNER JOIN process_track t on a.track_id = t.id
        INNER JOIN process USING(upid)

        -- the slice of 'Choreographer#doFrame N' for that frame on ui thread
        INNER JOIN slice doFrame on doFrame.name = CONCAT('Choreographer#doFrame ', a.name)
        -- the slice of 'DrawFrames N' for that frame on render thread
        INNER JOIN slice drawFrame on drawFrame.name = CONCAT('DrawFrames ', a.name)
        -- the slice of 'dequeueBuffer' inside of `DrawFrames` on render thread
        INNER JOIN slice dequeue on dequeue.name = 'dequeueBuffer' and slice_is_ancestor(drawFrame.id, dequeue.id)
        -- the slice of 'postAndWait' inside of doFrame on ui thread
        INNER JOIN slice post on post.name = 'postAndWait' and slice_is_ancestor(doFrame.id, post.id)

        WHERE
            ${androidx.benchmark.traceprocessor.processNameLikePkg(packageName)}
        AND
            -- remove "incomplete" frames
            a.dur > 0
    """
            .trimIndent()

    internal fun getFrameCost(session: TraceProcessor.Session, packageName: String): List<Double> {
        val queryResultIterator = session.query(query = getFullQuery(packageName))
        return queryResultIterator.map { it.double("frameCost") }.toList()
    }
}
