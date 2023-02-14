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
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Listener for when a task reaches a valid argument state, and can confirm fulfillment.
 *
 * @param <ArgumentT>
 * @param <ConfirmationT>
 */
public interface OnReadyToConfirmListener<ArgumentT, ConfirmationT> {

    /** Called when a task is ready to confirm, with the final Argument instance. */
    @NonNull
    ListenableFuture<ConfirmationOutput<ConfirmationT>> onReadyToConfirm(ArgumentT finalArgument);
}
