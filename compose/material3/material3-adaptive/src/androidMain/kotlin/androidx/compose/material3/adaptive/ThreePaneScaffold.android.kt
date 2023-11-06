package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class DefaultThreePaneScaffoldState(
    initialFocusHistory: List<ThreePaneScaffoldRole>,
    initialScaffoldDirective: PaneScaffoldDirective,
    initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies,
) {

    private val focusHistory = mutableStateListOf<ThreePaneScaffoldRole>().apply {
        addAll(initialFocusHistory)
    }

    var scaffoldDirective by mutableStateOf(initialScaffoldDirective)
    var adaptStrategies by mutableStateOf(initialAdaptStrategies)

    val currentFocus: ThreePaneScaffoldRole?
        get() = focusHistory.lastOrNull()

    val scaffoldValue: ThreePaneScaffoldValue get() = calculateScaffoldValue(currentFocus)

    fun navigateTo(pane: ThreePaneScaffoldRole) {
        focusHistory.add(pane)
    }

    fun canNavigateBack(scaffoldValueMustChange: Boolean): Boolean =
        getPreviousFocusIndex(scaffoldValueMustChange) >= 0

    fun navigateBack(popUntilScaffoldValueChange: Boolean): Boolean {
        val previousFocusIndex = getPreviousFocusIndex(popUntilScaffoldValueChange)
        if (previousFocusIndex < 0) {
            focusHistory.clear()
            return false
        }
        val targetSize = previousFocusIndex + 1
        while (focusHistory.size > targetSize) {
            focusHistory.removeLast()
        }
        return true
    }

    private fun getPreviousFocusIndex(withScaffoldValueChange: Boolean): Int {
        if (focusHistory.size <= 1) {
            // No previous focus
            return -1
        }
        if (!withScaffoldValueChange) {
            return focusHistory.lastIndex - 1
        }
        for (previousFocusIndex in focusHistory.lastIndex - 1 downTo 0) {
            val newValue = calculateScaffoldValue(focusHistory[previousFocusIndex])
            if (newValue != scaffoldValue) {
                return previousFocusIndex
            }
        }
        return -1
    }

    private fun calculateScaffoldValue(
        focus: ThreePaneScaffoldRole?
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            scaffoldDirective.maxHorizontalPartitions,
            adaptStrategies,
            focus
        )

    companion object {
        /**
         * To keep focus history saved
         */
        fun saver(
            initialScaffoldDirective: PaneScaffoldDirective,
            initialAdaptStrategies: ThreePaneScaffoldAdaptStrategies
        ): Saver<DefaultThreePaneScaffoldState, *> = listSaver(
            save = {
                it.focusHistory.toList()
            },
            restore = {
                DefaultThreePaneScaffoldState(
                    initialFocusHistory = it,
                    initialScaffoldDirective = initialScaffoldDirective,
                    initialAdaptStrategies = initialAdaptStrategies
                )
            }
        )
    }
}

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun rememberDefaultThreePaneScaffoldState(
    scaffoldDirective: PaneScaffoldDirective,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    initialFocusHistory: List<ThreePaneScaffoldRole>
): DefaultThreePaneScaffoldState =
    rememberSaveable(
        saver = DefaultThreePaneScaffoldState.saver(
            scaffoldDirective,
            adaptStrategies,
        )
    ) {
        DefaultThreePaneScaffoldState(
            initialFocusHistory = initialFocusHistory,
            initialScaffoldDirective = scaffoldDirective,
            initialAdaptStrategies = adaptStrategies
        )
    }.apply {
        this.scaffoldDirective = scaffoldDirective
        this.adaptStrategies = adaptStrategies
    }
