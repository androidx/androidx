/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteFloatExpression
import androidx.compose.remote.frontend.state.remoteFloat

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteFloatContext(public val state: RemoteComposeCreationState) {
    public fun componentWidth(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentWidthValue()
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    public fun componentHeight(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentHeightValue()
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    public fun componentCenterX(): RemoteFloat {
        val doc = state.document
        val componentWidthValue = doc.addComponentWidthValue()
        val value = doc.floatExpression(componentWidthValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    public fun componentCenterY(): RemoteFloat {
        val doc = state.document
        val componentHeightValue = doc.addComponentHeightValue()
        val value = doc.floatExpression(componentHeightValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }
}

public fun remoteComponentWidth(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentWidth() }
}

public fun remoteComponentHeight(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentHeight() }
}

public fun remoteComponentCenterX(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentCenterX() }
}

public fun remoteComponentCenterY(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentCenterY() }
}
