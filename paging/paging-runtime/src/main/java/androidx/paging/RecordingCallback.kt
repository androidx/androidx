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

package androidx.paging

@Suppress("DEPRECATION")
internal class RecordingCallback : PagedList.Callback() {
    private val list = mutableListOf<Int>()
    override fun onChanged(position: Int, count: Int) {
        list.add(Changed)
        list.add(position)
        list.add(count)
    }

    override fun onInserted(position: Int, count: Int) {
        list.add(Inserted)
        list.add(position)
        list.add(count)
    }

    override fun onRemoved(position: Int, count: Int) {
        list.add(Removed)
        list.add(position)
        list.add(count)
    }

    fun dispatchRecordingTo(other: PagedList.Callback) {
        for (i in 0 until list.size step 3) {
            when (list[i]) {
                Changed -> other.onChanged(list[i + 1], list[i + 2])
                Inserted -> other.onInserted(list[i + 1], list[i + 2])
                Removed -> other.onRemoved(list[i + 1], list[i + 2])
                else -> throw IllegalStateException("Unexpected recording value")
            }
        }
        list.clear()
    }

    companion object {
        private const val Changed = 0
        private const val Inserted = 1
        private const val Removed = 2
    }
}