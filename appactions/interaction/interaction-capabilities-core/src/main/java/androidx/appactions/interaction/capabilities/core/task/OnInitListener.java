/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.task;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Listener for when a task is initialized.
 *
 * @param <TaskUpdaterT>
 */
public interface OnInitListener<TaskUpdaterT> {

    /**
     * Called when a task is initiated. This method should perform initialization if necessary, and
     * return a future that represents when initialization is finished.
     *
     * @param arg depending on the BII, the SDK may pass a utility object to this method, such as
     *            TaskUpdater.
     */
    @NonNull
    ListenableFuture<Void> onInit(TaskUpdaterT arg);
}
