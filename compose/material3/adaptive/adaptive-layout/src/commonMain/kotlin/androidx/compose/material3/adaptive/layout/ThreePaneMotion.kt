/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.internal.getValue
import androidx.compose.material3.adaptive.layout.internal.rememberRef
import androidx.compose.material3.adaptive.layout.internal.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize

/**
 * The class that provides motion settings for three pane scaffolds like [ListDetailPaneScaffold]
 * and [SupportingPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneMotion
internal constructor(
    private val primaryPaneMotion: PaneMotion,
    private val secondaryPaneMotion: PaneMotion,
    private val tertiaryPaneMotion: PaneMotion,
) {
    /**
     * Gets the specified [PaneMotion] of a given pane role.
     *
     * @param role the specified role of the pane, see [ListDetailPaneScaffoldRole] and
     *   [SupportingPaneScaffoldRole].
     */
    operator fun get(role: ThreePaneScaffoldRole): PaneMotion =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primaryPaneMotion
            ThreePaneScaffoldRole.Secondary -> secondaryPaneMotion
            ThreePaneScaffoldRole.Tertiary -> tertiaryPaneMotion
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneMotion) return false
        if (primaryPaneMotion != other.primaryPaneMotion) return false
        if (secondaryPaneMotion != other.secondaryPaneMotion) return false
        if (tertiaryPaneMotion != other.tertiaryPaneMotion) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primaryPaneMotion.hashCode()
        result = 31 * result + secondaryPaneMotion.hashCode()
        result = 31 * result + tertiaryPaneMotion.hashCode()
        return result
    }

    override fun toString(): String {
        return "ThreePaneMotion(" +
            "primaryPaneMotion=$primaryPaneMotion, " +
            "secondaryPaneMotion=$secondaryPaneMotion, " +
            "tertiaryPaneMotion=$tertiaryPaneMotion)"
    }

    companion object {
        /** A default [ThreePaneMotion] instance that specifies no motions. */
        val NoMotion =
            ThreePaneMotion(PaneMotion.NoMotion, PaneMotion.NoMotion, PaneMotion.NoMotion)
    }
}

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ThreePaneScaffoldState.calculateThreePaneMotion(
    ltrPaneOrder: ThreePaneScaffoldHorizontalOrder
): ThreePaneMotion {
    var result by rememberRef(ThreePaneMotion.NoMotion)
    if (currentState != targetState) {
        // Only update motions when the state changes to prevent unnecessary recomposition at the
        // end of state transitions.
        val paneMotions = calculatePaneMotion(currentState, targetState, ltrPaneOrder)
        result =
            ThreePaneMotion(
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Primary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Secondary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Tertiary)],
            )
    }
    return result
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
internal class ThreePaneScaffoldMotionDataProvider :
    PaneScaffoldMotionDataProvider<ThreePaneScaffoldRole> {
    private lateinit var ltrOrder: ThreePaneScaffoldHorizontalOrder

    private val primary = PaneMotionData()
    private val secondary = PaneMotionData()
    private val tertiary = PaneMotionData()

    internal var scaffoldState: ThreePaneScaffoldState? = null

    internal val predictiveBackScaleState =
        PredictiveBackScaleState(
            scaffoldSize = { scaffoldSize },
            isPredictiveBackInProgress = { scaffoldState?.isPredictiveBackInProgress ?: false },
        )

    override var scaffoldSize: IntSize = IntSize.Zero

    override val count: Int = 3

    override fun getRoleAt(index: Int): ThreePaneScaffoldRole = ltrOrder[index]

    override fun get(role: ThreePaneScaffoldRole): PaneMotionData =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primary
            ThreePaneScaffoldRole.Secondary -> secondary
            ThreePaneScaffoldRole.Tertiary -> tertiary
        }

    override fun get(index: Int): PaneMotionData = get(getRoleAt(index))

    internal fun update(
        threePaneMotion: ThreePaneMotion,
        ltrOrder: ThreePaneScaffoldHorizontalOrder,
    ) {
        this.ltrOrder = ltrOrder
        forEach { role, it ->
            it.motion = threePaneMotion[role]
            it.isOriginSizeAndPositionSet = false
        }
    }
}
