/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.testing;

import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.UseCaseConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions related to operating on androidx.camera.core.impl.Config instances.
 */
public final class Configs {
    /** Return a list of UseCaseConfig from a list of {@link UseCase}. */
    @NonNull
    public static List<UseCaseConfig<?>> useCaseConfigListFromUseCaseList(
            @NonNull List<UseCase> useCases) {
        List<UseCaseConfig<?>> useCaseConfigs = new ArrayList<>();
        for (UseCase useCase : useCases) {
            useCaseConfigs.add(useCase.getUseCaseConfig());
        }

        return useCaseConfigs;
    }

    private Configs() {
    }
}
