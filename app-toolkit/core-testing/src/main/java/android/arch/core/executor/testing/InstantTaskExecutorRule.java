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

package android.arch.core.executor.testing;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.core.executor.TaskExecutor;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A JUnit Test Rule that swaps the background executor used by the Architecture Components with a
 * different one which executes each task synchronously.
 * <p>
 * You can use this rule for your host side tests that use Architecture Components.
 */
public class InstantTaskExecutorRule extends TestWatcher {
    @Override
    protected void starting(Description description) {
        super.starting(description);
        AppToolkitTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        AppToolkitTaskExecutor.getInstance().setDelegate(null);
    }
}
