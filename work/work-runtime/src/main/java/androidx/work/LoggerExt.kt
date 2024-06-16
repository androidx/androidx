/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work

internal inline fun logd(tag: String, block: () -> String) = Logger.get().debug(tag, block())

internal inline fun logd(tag: String, t: Throwable, block: () -> String) =
    Logger.get().debug(tag, block(), t)

internal inline fun logi(tag: String, block: () -> String) = Logger.get().info(tag, block())

internal inline fun logi(tag: String, t: Throwable, block: () -> String) =
    Logger.get().info(tag, block(), t)

internal inline fun loge(tag: String, block: () -> String) = Logger.get().error(tag, block())

internal inline fun loge(tag: String, t: Throwable, block: () -> String) =
    Logger.get().error(tag, block(), t)
