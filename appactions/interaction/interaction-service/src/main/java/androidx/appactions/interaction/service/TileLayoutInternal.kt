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

package androidx.appactions.interaction.service

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.protobuf.ByteString
import androidx.appactions.interaction.service.proto.AppInteractionServiceProto
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders

/**
 * Holder for TileLayout response.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
data class TileLayoutInternal(
    val layout: LayoutElementBuilders.Layout,
    val resources: ResourceBuilders.Resources
) {

    fun toProto(): AppInteractionServiceProto.TileLayout {
        return AppInteractionServiceProto.TileLayout.newBuilder()
            .setLayout(ByteString.copyFrom(layout.toByteArray()))
            .setResources(ByteString.copyFrom(resources.toByteArray()))
            .build()
    }
}
