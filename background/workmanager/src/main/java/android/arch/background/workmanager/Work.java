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

import android.arch.background.workmanager.impl.BaseWork;
import android.arch.background.workmanager.model.InputMerger;
import android.arch.background.workmanager.model.OverwritingInputMerger;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

/**
 * A class to create a logical unit of non-repeating work.
 */

public class Work extends BaseWork {

    Work(Builder builder) {
        super(builder);
    }

    /**
     * Builder for {@link Work} class.
     */
    public static class Builder extends BaseWork.Builder<Work, Work.Builder> {
        public Builder(Class<? extends Worker> workerClass) {
            super(workerClass);
            mWorkSpec.setInputMergerClassName(OverwritingInputMerger.class.getName());
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public Builder withInitialStatus(@WorkStatus int status) {
            mWorkSpec.setStatus(status);
            return this;
        }

        @VisibleForTesting
        @Override
        public Builder withInitialRunAttemptCount(int runAttemptCount) {
            mWorkSpec.setRunAttemptCount(runAttemptCount);
            return this;
        }

        /**
         * Specify whether {@link Work} should run with an initial delay. Default is 0ms.
         *
         * @param duration initial delay before running WorkSpec (in milliseconds)
         * @return The current {@link Builder}.
         */
        public Builder withInitialDelay(long duration) {
            mWorkSpec.setInitialDelay(duration);
            return this;
        }

        /**
         * Specify an {@link InputMerger}.  The default is {@link OverwritingInputMerger}.
         *
         * @param inputMerger The class name of the {@link InputMerger} to use for this {@link Work}
         * @return The current {@link Builder}
         */
        public Builder withInputMerger(Class<? extends InputMerger> inputMerger) {
            mWorkSpec.setInputMergerClassName(inputMerger.getName());
            return this;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        /**
         * Generates the {@link Work} from {@link Builder}.
         *
         * @return new {@link Work}
         */
        @Override
        public Work build() {
            return new Work(this);
        }
    }
}
