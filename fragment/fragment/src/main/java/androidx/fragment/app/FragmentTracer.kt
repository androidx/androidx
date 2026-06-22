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

package androidx.fragment.app

import androidx.annotation.MainThread
import androidx.tracing.Tracer

/** Traces fragment operations. */
@MainThread
internal class FragmentTracer(private val fragment: Fragment) {

    /**
     * Runs [block] within a trace section.
     *
     * @param name name of the trace section.
     * @param block operation to run. We use [Runnable] instead of a Kotlin function block because
     *   it is easier to use from Java; Kotlin function blocks force Java callers to explicitly
     *   return [Unit].
     */
    fun trace(name: String, block: Runnable) {
        Tracer.global.trace(
            category = "androidx.fragment",
            name = name,
            metadataBlock = {
                with(fragment) {
                    addMetadataEntry("class", javaClass.simpleName)
                    addMetadataEntry("uuid", mWho)

                    mTag?.let { addMetadataEntry("tag", it) }
                    activity?.let { addMetadataEntry("activity", it.javaClass.simpleName) }

                    val id = mFragmentId
                    if (id != 0) {
                        addMetadataEntry("id", "0x${Integer.toHexString(id)}")
                    }
                }
            },
            block = block::run,
        )
    }
}
