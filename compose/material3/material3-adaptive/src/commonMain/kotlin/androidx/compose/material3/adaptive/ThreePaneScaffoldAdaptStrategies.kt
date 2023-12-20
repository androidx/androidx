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

package androidx.compose.material3.adaptive

/**
 * The adaptation specs of [ThreePaneScaffold]. This class denotes how each pane of
 * [ThreePaneScaffold] should be adapted. It should be used as an input parameter of
 * [calculateThreePaneScaffoldValue] to decide the [ThreePaneScaffoldValue].
 *
 * @constructor create an instance of [ThreePaneScaffoldAdaptStrategies]
 * @param primaryPaneAdaptStrategy [AdaptStrategy] of the primary pane of [ThreePaneScaffold]
 * @param secondaryPaneAdaptStrategy [AdaptStrategy] of the secondary pane of [ThreePaneScaffold]
 * @param tertiaryPaneAdaptStrategy [AdaptStrategy] of the tertiary pane of [ThreePaneScaffold]
 */
@ExperimentalMaterial3AdaptiveApi
class ThreePaneScaffoldAdaptStrategies(
    private val primaryPaneAdaptStrategy: AdaptStrategy,
    private val secondaryPaneAdaptStrategy: AdaptStrategy,
    private val tertiaryPaneAdaptStrategy: AdaptStrategy
) {
    operator fun get(role: ThreePaneScaffoldRole): AdaptStrategy {
        return when (role) {
            ThreePaneScaffoldRole.Primary -> primaryPaneAdaptStrategy
            ThreePaneScaffoldRole.Secondary -> secondaryPaneAdaptStrategy
            ThreePaneScaffoldRole.Tertiary -> tertiaryPaneAdaptStrategy
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldAdaptStrategies) return false
        if (primaryPaneAdaptStrategy != other.primaryPaneAdaptStrategy) return false
        if (secondaryPaneAdaptStrategy != other.secondaryPaneAdaptStrategy) return false
        if (tertiaryPaneAdaptStrategy != other.tertiaryPaneAdaptStrategy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primaryPaneAdaptStrategy.hashCode()
        result = 31 * result + secondaryPaneAdaptStrategy.hashCode()
        result = 31 * result + tertiaryPaneAdaptStrategy.hashCode()
        return result
    }
}
