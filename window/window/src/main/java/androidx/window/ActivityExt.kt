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
package androidx.window

import android.app.Activity
import android.os.IBinder
import android.os.Looper

/**
 * A utility method [Activity] to return an optional [IBinder] window token from an [Activity].
 */
internal fun getActivityWindowToken(activity: Activity?): IBinder? {
    return activity?.window?.attributes?.token
}

internal inline fun <reified T> Activity.getTag(id: Int): T? {
    return window.decorView.getTag(id) as? T
}

/**
 * Checks to see if an object of type [T] is associated with the tag [id]. If it is available
 * then it is returned. Otherwise set an object crated using the [producer] and return that value.
 * @return object associated with the tag.
 */
internal inline fun <reified T> Activity.getOrCreateTag(id: Int, producer: () -> T): T {
    return (window.decorView.getTag(id) as? T) ?: run {
        assert(Looper.getMainLooper() == Looper.myLooper())
        val value = producer()
        window.decorView.setTag(id, value)
        value
    }
}

/**
 * Provide an instance of [WindowInfoRepo] that is associated to the given [Activity]
 */
public fun Activity.windowInfoRepository(): WindowInfoRepo {
    return WindowInfoRepo.create(this)
}