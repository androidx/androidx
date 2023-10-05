package androidx.compose.material3.adaptive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastMap

/**
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical supporting-pane layout.
 *
 * @param scaffoldState the state of the scaffold, which will decide the current layout directive
 *        and scaffold layout value, and perform navigation within the scaffold.
 * @param supportingPane the supporting pane of the scaffold.
 *        See [SupportingPaneScaffoldRole.Supporting].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold. See [SupportingPaneScaffoldRole.Extra].
 * @param mainPane the main pane of the scaffold. See [SupportingPaneScaffoldRole.Main].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun SupportingPaneScaffold(
    scaffoldState: SupportingPaneScaffoldState,
    supportingPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit)? = null,
    mainPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = scaffoldState.scaffoldDirective,
        scaffoldValue = scaffoldState.layoutValue,
        arrangement = ThreePaneScaffoldDefaults.SupportingPaneLayoutArrangement,
        secondaryPane = supportingPane,
        tertiaryPane = extraPane,
        primaryPane = mainPane
    )
}

/**
 * Provides default values of [SupportingPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
object SupportingPaneScaffoldDefaults {
    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [SupportingPaneScaffold].
     *
     * @param mainPaneAdaptStrategy the adapt strategy of the main pane
     * @param supportingPaneAdaptStrategy the adapt strategy of the supporting pane
     * @param extraPaneAdaptStrategy the adapt strategy of the extra pane
     */
    fun adaptStrategies(
        mainPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        supportingPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            mainPaneAdaptStrategy,
            supportingPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )
}

/**
 * The state of [SupportingPaneScaffold]. It provides the layout directive and value state that will
 * be updated directly. It also provides functions to perform navigation.
 *
 * Use [rememberSupportingPaneScaffoldState] to get a remembered default instance of this interface,
 * which works independently from any navigation frameworks. Developers can also integrate with
 * other navigation frameworks by implementing this interface.
 *
 * @property scaffoldDirective the current layout directives that the associated
 *           [SupportingPaneScaffold] needs to follow. It's supposed to be automatically updated
 *           when the window configuration changes.
 * @property layoutValue the current layout value of the associated [SupportingPaneScaffold], which
 *           represents unique layout states of the scaffold.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface SupportingPaneScaffoldState {
    val scaffoldDirective: PaneScaffoldDirective
    val layoutValue: ThreePaneScaffoldValue

    /**
     * Navigates to a new focus.
     */
    fun navigateTo(pane: SupportingPaneScaffoldRole)

    /**
     * Returns `true` if there is a previous focus to navigate back to.
     *
     * @param layoutValueMustChange `true` if the navigation operation should only be performed when
     *        there are actual layout value changes.
     */
    fun canNavigateBack(layoutValueMustChange: Boolean = true): Boolean

    /**
     * Navigates to the previous focus.
     *
     * @param popUntilLayoutValueChange `true` if the backstack should be popped until the layout
     *        value changes.
     */
    fun navigateBack(popUntilLayoutValueChange: Boolean = true): Boolean
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class DefaultSupportingPaneScaffoldState(
    val internalState: DefaultThreePaneScaffoldState
) : SupportingPaneScaffoldState {
    override val scaffoldDirective get() = internalState.scaffoldDirective
    override val layoutValue get() = internalState.layoutValue

    override fun navigateTo(pane: SupportingPaneScaffoldRole) {
        internalState.navigateTo(pane.threePaneScaffoldRole)
    }

    override fun canNavigateBack(layoutValueMustChange: Boolean): Boolean =
        internalState.canNavigateBack(layoutValueMustChange)

    override fun navigateBack(popUntilLayoutValueChange: Boolean): Boolean =
        internalState.navigateBack(popUntilLayoutValueChange)
}

/**
 * Returns a remembered default implementation of [SupportingPaneScaffoldState], which will
 * be updated automatically when the input values change. The default state is supposed to be
 * used independently from any navigation frameworks and it will address the navigation purely
 * inside the [SupportingPaneScaffold].
 *
 * @param scaffoldDirective the current layout directives to follow. The default value will be
 *        Calculated with [calculateStandardPaneScaffoldDirective] using [WindowAdaptiveInfo]
 *        retrieved from the current context.
 * @param adaptStrategies adaptation strategies of each pane.
 * @param initialFocusHistory the initial focus history of the scaffold, by default it will be just
 *        the list pane.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun rememberSupportingPaneScaffoldState(
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(calculateWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        SupportingPaneScaffoldDefaults.adaptStrategies(),
    initialFocusHistory: List<SupportingPaneScaffoldRole> = listOf(SupportingPaneScaffoldRole.Main)
): SupportingPaneScaffoldState {
    val internalState = rememberDefaultThreePaneScaffoldState(
        scaffoldDirective,
        adaptStrategies,
        initialFocusHistory.fastMap { it.threePaneScaffoldRole }
    )
    return remember(internalState) {
        DefaultSupportingPaneScaffoldState(internalState)
    }
}

/**
 * The set of the available pane roles of [SupportingPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
enum class SupportingPaneScaffoldRole(internal val threePaneScaffoldRole: ThreePaneScaffoldRole) {
    /**
     * The main pane of [SupportingPaneScaffold]. It is mapped to [ThreePaneScaffoldRole.Primary].
     */
    Main(ThreePaneScaffoldRole.Primary),
    /**
     * The supporting pane of [SupportingPaneScaffold]. It is mapped to
     * [ThreePaneScaffoldRole.Secondary].
     */
    Supporting(ThreePaneScaffoldRole.Secondary),
    /**
     * The extra pane of [SupportingPaneScaffold]. It is mapped to [ThreePaneScaffoldRole.Tertiary].
     */
    Extra(ThreePaneScaffoldRole.Tertiary)
}
