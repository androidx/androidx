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
@file:JvmName("WindowInfoTrackerRx")

package androidx.window.rxjava2.layout

import android.app.Activity
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable

/**
 * Return an [Observable] stream of [WindowLayoutInfo].
 * @see WindowInfoTracker.windowLayoutInfo
 */
public fun WindowInfoTracker.windowLayoutInfoObservable(
    activity: Activity
): Observable<WindowLayoutInfo> {
    return windowLayoutInfo(activity).asObservable()
}

/**
 * Return a [Flowable] stream of [WindowLayoutInfo].
 * @see WindowInfoTracker.windowLayoutInfo
 */
public fun WindowInfoTracker.windowLayoutInfoFlowable(
    activity: Activity
): Flowable<WindowLayoutInfo> {
    return windowLayoutInfo(activity).asFlowable()
}
