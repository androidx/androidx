/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.wear.widget.util;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.test.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule which holds a wake lock for the duration of the test.
 */
public class WakeLockRule implements TestRule {
    @SuppressWarnings("deprecation")
    private static final int WAKELOCK_FLAGS =
            PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;

    @Override
    public Statement apply(final Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WakeLock wakeLock = createWakeLock();
                wakeLock.acquire();
                try {
                    statement.evaluate();
                } finally {
                    wakeLock.release();
                }
            }
        };
    }

    private WakeLock createWakeLock() {
        Context context = InstrumentationRegistry.getTargetContext();
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return power.newWakeLock(WAKELOCK_FLAGS, context.getPackageName());
    }
}
