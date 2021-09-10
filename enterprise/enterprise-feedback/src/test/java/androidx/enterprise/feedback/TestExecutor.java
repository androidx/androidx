/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.enterprise.feedback;

import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

/**
 * {@link Executor} which executes all {@link Runnable} instances inline and records that they have
 * run.
 */
@DoNotInstrument
public class TestExecutor implements Executor {

    private Runnable mLastExecuted;

    @Override
    public void execute(Runnable command) {
        command.run();
        mLastExecuted = command;
    }

    public Runnable lastExecuted() {
        return mLastExecuted;
    }
}
