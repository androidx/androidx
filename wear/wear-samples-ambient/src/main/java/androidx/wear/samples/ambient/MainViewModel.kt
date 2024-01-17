/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.samples.ambient

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

internal enum class Status {
    ACTIVE, AMBIENT
}

internal class MainViewModel : ViewModel() {
    /** The last time the activity was resumed. */
    private var resumeTime: MutableLiveData<Long> = MutableLiveData<Long>(0)

    /** The current status of the activity, either active or ambient. */
    private var status: MutableLiveData<Status> = MutableLiveData<Status>(Status.ACTIVE)

    /** The number of updates received within the current status. */
    private var updates: MutableLiveData<Int> = MutableLiveData<Int>(0)

    /** The timestamp of the last update. */
    private var updateTimestamp: MutableLiveData<Long> = MutableLiveData<Long>(0)

    fun observeStartTime(owner: LifecycleOwner, observer: Observer<Long>) =
        resumeTime.observe(owner, observer)

    fun observeStatus(owner: LifecycleOwner, observer: Observer<Status>) =
        status.observe(owner, observer)

    fun observeUpdates(owner: LifecycleOwner, observer: Observer<Int>) =
        updates.observe(owner, observer)

    fun observeUpdateTimestamp(owner: LifecycleOwner, observer: Observer<Long>) =
        updateTimestamp.observe(owner, observer)

    /** Returns the amount of time between the last resume and the last update. */
    fun getTimer(): Long = updateTimestamp.value!! - resumeTime.value!!

    /** Publishes a new update. */
    fun publishUpdate() {
        updateWith(updates.value!! + 1)
    }

    /** Updates the status. */
    fun setStatus(status: Status) {
        if (this.status.value == status) {
            return
        }
        this.status.value = status
        resetUpdates()
    }

    /** Updates the result time. */
    fun updateResumeTime() {
        resumeTime.value = System.currentTimeMillis()
        resetUpdates()
    }

    /** Resets the number of updates. */
    private fun resetUpdates() {
        updateWith(0)
    }

    private fun updateWith(value: Int) {
        updates.value = value
        updateTimestamp.value = System.currentTimeMillis()
    }
}
