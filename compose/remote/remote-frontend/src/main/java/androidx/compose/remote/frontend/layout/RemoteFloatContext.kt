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
package androidx.compose.remote.frontend.layout

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteFloatExpression
import androidx.compose.remote.frontend.state.remoteFloat

class RemoteFloatContext(val state: RemoteComposeCreationState) {
    fun componentWidth(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentWidthValue()
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    fun componentHeight(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentHeightValue()
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    fun componentCenterX(): RemoteFloat {
        val doc = state.document
        val componentWidthValue = doc.addComponentWidthValue()
        val value = doc.floatExpression(componentWidthValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }

    fun componentCenterY(): RemoteFloat {
        val doc = state.document
        val componentHeightValue = doc.addComponentHeightValue()
        val value = doc.floatExpression(componentHeightValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(true, { _ -> floatArrayOf(value) })
    }
}

fun remoteComponentWidth(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentWidth() }
}

fun remoteComponentHeight(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentHeight() }
}

fun remoteComponentCenterX(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentCenterX() }
}

fun remoteComponentCenterY(state: RemoteComposeCreationState): RemoteFloat {
    return remoteFloat(state) { componentCenterY() }
}
