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

package androidx.inspection;

import androidx.annotation.NonNull;

/**
 * A factory that is responsible for creation of an inspector for your library.
 * <p>
 * Implementations of this class should be registered
 * as "META-INF/services/androidx.inspection.inspection.InspectorFactory"
 * and should have a default constructor.
 *
 * @param <T> inspector created by this factory
 */
public abstract class InspectorFactory<T extends Inspector> {
    private final String mInspectorId;

    /**
     * Identifier of an inspector that created by this factory.
     *
     * @param inspectorId an id of an inspector that is served by this factory.
     */
    public InspectorFactory(@NonNull String inspectorId) {
        mInspectorId = inspectorId;
    }

    /**
     * @return an id of an inspector that is served by this factory.
     */
    @NonNull
    public final String getInspectorId() {
        return mInspectorId;
    }

    /**
     * Creates a new inspector with the provided connection.
     *
     * @param connection a connection to send events.
     * @param environment an environment that provides tooling utilities.
     * @return a new instance of an inspector.
     */
    @NonNull
    public abstract T createInspector(@NonNull Connection connection,
            @NonNull InspectorEnvironment environment);
}
