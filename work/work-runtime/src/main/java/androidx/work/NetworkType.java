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

import androidx.annotation.RequiresApi;

/**
 * An enumeration of various network types that can be used as {@link Constraints} for work.
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
    METERED,

    /**
     * A temporarily unmetered Network. This capability will be set for networks that are
     * generally metered, but are currently unmetered.
     *
     * Note: This capability can be changed at any time. When it is removed,
     * {@link ListenableWorker}s are responsible for stopping any data transfer that should not
     * occur on a metered network.
     */
    @RequiresApi(30)
    TEMPORARILY_UNMETERED
}
