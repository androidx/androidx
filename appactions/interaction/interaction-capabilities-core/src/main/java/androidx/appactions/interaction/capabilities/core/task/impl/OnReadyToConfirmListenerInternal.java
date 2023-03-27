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

package androidx.appactions.interaction.capabilities.core.task.impl;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.MissingRequiredArgException;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Map;

/**
 * Generic onReadyToConfirm listener for a task capability. This is the entry point to specific
 * onReadyToCOnfirm listeners. For example, Search/Update sub-BIIs factories may invoke specific
 * onReadyToConfirm listeners for that BII.
 *
 * @param <ConfirmationT>
 */
public interface OnReadyToConfirmListenerInternal<ConfirmationT> {

    /** onReadyToConfirm callback for a task capability. */
    @NonNull
    ListenableFuture<ConfirmationOutput<ConfirmationT>> onReadyToConfirm(
            @NonNull Map<String, List<ParamValue>> args)
            throws StructConversionException, MissingRequiredArgException;
}
