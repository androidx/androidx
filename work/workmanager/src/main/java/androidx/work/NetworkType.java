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
 * The type of network required by a unit of work.
 */
public enum NetworkType {

    /**
     * A network is not required for this work.
     */
    NOT_REQUIRED,

    /**
     * Any working network connection is required for this work.
     */
    CONNECTED,

    /**
     * An unmetered network connection is required for this work.
     */
    UNMETERED,

    /**
     * A non-roaming network connection is required for this work.
     */
    NOT_ROAMING,

    /**
     * A metered network connection is required for this work.
     */
    METERED
}
