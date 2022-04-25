/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.changes

import android.os.Parcelable
import androidx.health.platform.client.impl.OnChangesListenerProxy
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.ChangeProto

/** Returned via [OnChangesListenerProxy]. */
class ChangesEvent(override val proto: ChangeProto.ChangesEvent) :
    ProtoParcelable<ChangeProto.ChangesEvent>() {
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ChangesEvent> = newCreator {
            val proto = ChangeProto.ChangesEvent.parseFrom(it)
            ChangesEvent(proto)
        }
    }
}
