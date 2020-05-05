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

package androidx.ui.test

/**
 * Dumps all the semantics nodes information it holds into string.
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 */
fun SemanticsNodeInteraction.dumpToString(): String {
    val result = fetchSemanticsNodes()
    return if (result.selectedNodes.isEmpty()) {
        "There were 0 nodes found!"
    } else {
        result.selectedNodes.toStringInfo()
    }
}

/**
 * Dumps all the semantics nodes information it holds into string.
 *
 * Note that this will fetch the latest snapshot of nodes it sees in the hierarchy for the IDs it
 * collected before. So the output can change over time if the tree changes.
 */
fun SemanticsNodeInteractionCollection.dumpToString(): String {
    val nodes = fetchSemanticsNodes()
    return if (nodes.isEmpty()) {
        "There were 0 nodes found!"
    } else {
        nodes.toStringInfo()
    }
}
