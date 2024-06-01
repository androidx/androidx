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

package androidx.work.impl.utils

import androidx.core.util.Consumer
import androidx.work.WorkerExceptionInfo
import androidx.work.loge

/**
 * Runs worker exception handler and catches any [Throwable] thrown.
 *
 * @param info The info about the exception
 * @param tag Tag used for logging [Throwable] thrown from the handler
 * @receiver The worker exception handler
 */
fun Consumer<WorkerExceptionInfo>.safeAccept(info: WorkerExceptionInfo, tag: String) {
    try {
        accept(info)
    } catch (throwable: Throwable) {
        loge(tag, throwable) { "Exception handler threw an exception" }
    }
}
