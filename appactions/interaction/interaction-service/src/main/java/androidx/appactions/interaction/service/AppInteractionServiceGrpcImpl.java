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

package androidx.appactions.interaction.service;

import androidx.appactions.interaction.service.proto.AppActionsServiceGrpc.AppActionsServiceImplBase;

/**
 * Implementation of {@link AppActionsServiceImplBase} generated from the GRPC proto file. This
 * class delegates the requests to the appropriate capability session.
 */
final class AppInteractionServiceGrpcImpl extends AppActionsServiceImplBase {

    // TODO(b/268069897): Migrate ActionsServiceGrpcImpl.
    @SuppressWarnings("unused")
    private final AppInteractionService mAppInteractionService;

    AppInteractionServiceGrpcImpl(AppInteractionService mAppInteractionService) {
        this.mAppInteractionService = mAppInteractionService;
    }
}
