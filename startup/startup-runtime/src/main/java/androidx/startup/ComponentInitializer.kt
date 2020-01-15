/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.startup

import android.content.Context

/**
 * [ComponentInitializer]s can be used to initialize libraries during app startup, without the
 * need to use additional [android.content.ContentProvider]s.
 */
interface ComponentInitializer<out T : Any> {

    /**
     * Initializes and returns an instance of [T] given the application [Context]
     *
     * @param context The application context.
     */
    fun create(context: Context): T

    /**
     * @return A list of dependencies that this [ComponentInitializer] depends on. This is used
     * to determine initialization order of [ComponentInitializer]s.
     * <br/>
     * For e.g. if a [ComponentInitializer] `B` defines another [ComponentInitializer] `A` as
     * its dependency, then `A` gets initialized before `B`.
     */
    fun dependencies(): List<Class<out ComponentInitializer<*>>>
}
