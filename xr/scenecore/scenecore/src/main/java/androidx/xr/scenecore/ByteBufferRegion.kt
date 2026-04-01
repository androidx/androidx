/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import java.nio.ByteBuffer

/**
 * A container holding a reference to a [ByteBuffer], along with an offset and size.
 *
 * This is used to define a specific slice or region within a buffer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ByteBufferRegion(
    public val buffer: ByteBuffer,
    public val offset: Int,
    public val size: Int,
)
