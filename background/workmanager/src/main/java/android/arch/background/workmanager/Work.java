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

package android.arch.background.workmanager;

import android.arch.background.workmanager.impl.WorkImpl;
import android.support.annotation.NonNull;

/**
 * A class to create a logical unit of non-repeating work.
 */

public abstract class Work implements BaseWork {

    /**
     * Creates a {@link Work} that runs once.
     *
     * @param workerClass The {@link Worker} class to run with this job
     * @return A {@link Builder} used to construct the {@link Work}
     */
    public static Builder newBuilder(Class<? extends Worker> workerClass) {
        return new WorkImpl.Builder(workerClass);
    }

    /**
     * Builder for {@link Work} class.
     */
    public abstract static class Builder implements BaseWork.Builder<Work, Builder> {

        /**
         * Creates a {@link Work} that runs once.
         *
         * @param workerClass The {@link Worker} class to run with this job
         */
        public Builder(Class<? extends Worker> workerClass) {
        }

        /**
         * Specify whether {@link Work} should run with an initial delay. Default is 0ms.
         *
         * @param duration initial delay before running WorkSpec (in milliseconds)
         * @return The current {@link Builder}
         */
        public abstract Builder withInitialDelay(long duration);

        /**
         * Specify an {@link InputMerger}.  The default is {@link OverwritingInputMerger}.
         *
         * @param inputMerger The class name of the {@link InputMerger} to use for this {@link Work}
         * @return The current {@link Builder}
         */
        public abstract Builder withInputMerger(@NonNull Class<? extends InputMerger> inputMerger);
    }
}
