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

import android.arch.background.workmanager.model.WorkSpec;

/**
 * Generates test WorkSpecs.
 */

public class WorkSpecs {

    /**
     * Creates a {@link WorkSpec} from a {@link Worker} class for testing.  Used to overcome the
     * fact that Work.getWorkSpec is not public (nor should it be).
     *
     * @param clazz The {@link Worker} class
     * @return A {@link WorkSpec}
     */
    public static WorkSpec getWorkSpec(Class<? extends Worker> clazz) {
        Work work = new Work.Builder(clazz).build();
        return work.getWorkSpec();
    }

    private WorkSpecs() {
        // Do nothing.
    }
}
