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

package androidx.work.impl.utils.taskexecutor;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A {@link org.junit.Rule} that swaps the background executor used by WorkManager with a
 * different one which executes each task synchronously.
 * Adapted from android arch core's InstantTaskExecutorRule.
 */

public class InstantTaskExecutorRule extends TestWatcher {
    @Override
    protected void starting(Description description) {
        super.starting(description);
        WorkManagerTaskExecutor.getInstance().setTaskExecutor(new TaskExecutor() {
            @Override
            public void postToMainThread(Runnable r) {
                r.run();
            }

            @Override
            public void executeOnBackgroundThread(Runnable r) {
                r.run();
            }
        });
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        WorkManagerTaskExecutor.getInstance().setTaskExecutor(null);
    }
}
