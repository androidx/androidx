/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager.constraints.receivers;

import android.content.Context;

/**
 * A singleton class that holds all the {@link BaseConstraintsReceiver}s and can track when to
 * register and unregister them.
 */

public class ConstraintsReceivers {

    private static ConstraintsReceivers sInstance;

    /**
     * Gets the singleton instance of {@link ConstraintsReceivers}.
     *
     * @param context The initializing context (we only use the application context)
     * @return The singleton instance of {@link ConstraintsReceivers}.
     */
    public static synchronized ConstraintsReceivers getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConstraintsReceivers(context);
        }
        return sInstance;
    }

    private BatteryReceiver mBatteryReceiver;

    private ConstraintsReceivers(Context context) {
        Context appContext = context.getApplicationContext();
        mBatteryReceiver = new BatteryReceiver(appContext);
    }

    /**
     * @return The receiver used to track battery state.
     */
    public BaseConstraintsReceiver getBatteryReceiver() {
        return mBatteryReceiver;
    }
}
