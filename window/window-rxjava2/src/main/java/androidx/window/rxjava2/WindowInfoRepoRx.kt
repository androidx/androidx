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
@file:JvmName("WindowInfoRepoRx")

package androidx.window.rxjava2

import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import androidx.window.WindowMetrics
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.asObservable

/**
 * Return an [Observable] stream of [WindowMetrics].
 * @see WindowInfoRepo.currentWindowMetrics
 */
fun WindowInfoRepo.currentWindowMetricsObservable(): Observable<WindowMetrics> {
    return currentWindowMetrics.asObservable()
}

/**
 * Return a [Flowable] stream of [WindowMetrics].
 * @see WindowInfoRepo.currentWindowMetrics
 */
fun WindowInfoRepo.currentWindowMetricsFlowable(): Flowable<WindowMetrics> {
    return currentWindowMetrics.asFlowable()
}

/**
 * Return an [Observable] stream of [WindowLayoutInfo].
 * @see WindowInfoRepo.windowLayoutInfo
 */
public fun WindowInfoRepo.windowLayoutInfoObservable(): Observable<WindowLayoutInfo> {
    return windowLayoutInfo.asObservable()
}

/**
 * Return a [Flowable] stream of [WindowLayoutInfo].
 * @see WindowInfoRepo.windowLayoutInfo
 */
public fun WindowInfoRepo.windowLayoutInfoFlowable(): Flowable<WindowLayoutInfo> {
    return windowLayoutInfo.asFlowable()
}