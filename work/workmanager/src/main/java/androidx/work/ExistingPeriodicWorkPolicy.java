/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

/**
 * An enum that determines what to do with existing {@link PeriodicWorkRequest}s with the same
 * unique name in case of a collision.
 */
public enum ExistingPeriodicWorkPolicy {

    /**
     * If there is existing pending work with the same unique name, cancel and delete it.  Then,
     * insert the newly-specified work.
     */
    REPLACE,

    /**
     * If there is existing pending work with the same unique name, do nothing.  Otherwise, insert
     * the newly-specified work.
     */
    KEEP
}
