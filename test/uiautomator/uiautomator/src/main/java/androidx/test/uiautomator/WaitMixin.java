/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.os.SystemClock;

/**
 * Mixin which provides functionality to wait for conditions that depend on a given object.
 */
class WaitMixin<T> {

    private static final long DEFAULT_POLL_INTERVAL = 100;
    private T mObject;

    public WaitMixin(T instance) {
        mObject = instance;
    }

    public <U> U wait(Condition<? super T, U> condition, long timeout) {
        return wait(condition, timeout, DEFAULT_POLL_INTERVAL);
    }

    public <U> U wait(Condition<? super T, U> condition, long timeout, long interval) {
        long startTime = SystemClock.uptimeMillis();

        U result = condition.apply(mObject);
        for (long elapsedTime = 0; result == null || result.equals(false);
                elapsedTime = SystemClock.uptimeMillis() - startTime) {

            if (elapsedTime >= timeout) {
                break;
            }

            SystemClock.sleep(interval);
            result = condition.apply(mObject);
        }
        return result;
    }
}
