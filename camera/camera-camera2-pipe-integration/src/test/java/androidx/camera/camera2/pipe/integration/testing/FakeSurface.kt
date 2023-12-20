/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture

@RequiresApi(21)
class FakeSurface(
    val surface: Surface? = null
) : DeferrableSurface() {
    override fun provideSurface(): ListenableFuture<Surface> {
        return Futures.immediateFuture(surface)
    }
}
