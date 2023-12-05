package androidx.compose.material3.adaptive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical supporting-pane layout.
 *
 * @param supportingPane the supporting pane of the scaffold.
 *        See [SupportingPaneScaffoldRole.Supporting].
 * @param modifier [Modifier] of the scaffold layout.
 * @param scaffoldState the state of the scaffold, which provides the current scaffold directive
 *        and scaffold value.
 * @param extraPane the extra pane of the scaffold. See [SupportingPaneScaffoldRole.Extra].
 * @param mainPane the main pane of the scaffold. See [SupportingPaneScaffoldRole.Main].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun SupportingPaneScaffold(
    supportingPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: ThreePaneScaffoldState = calculateSupportingPaneScaffoldState(),
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
    mainPane: @Composable ThreePaneScaffoldScope.() -> Unit
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = scaffoldState.scaffoldDirective,
        scaffoldValue = scaffoldState.scaffoldValue,
        paneOrder = ThreePaneScaffoldDefaults.SupportingPaneLayoutPaneOrder,
        secondaryPane = supportingPane,
        tertiaryPane = extraPane,
        primaryPane = mainPane
    )
}

/**
 * This function calculates [ThreePaneScaffoldValue] based on the given [PaneScaffoldDirective],
 * [ThreePaneScaffoldAdaptStrategies], and the current pane destination of a
 * [SupportingPaneScaffold].
 *
 * @param scaffoldDirective the layout directives that the associated [SupportingPaneScaffold]
 *        needs to follow. The default value will be the calculation result from
 *        [calculateStandardPaneScaffoldDirective] with the current window configuration, and
 *        will be automatically updated when the window configuration changes.
 * @param adaptStrategies the [ThreePaneScaffoldAdaptStrategies] should be used by scaffold panes.
 * @param currentPaneDestination the current pane destination, which will be guaranteed to have
 *        highest priority when deciding pane visibility.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateSupportingPaneScaffoldState(
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        SupportingPaneScaffoldDefaults.adaptStrategies(),
    currentPaneDestination: ThreePaneScaffoldRole = SupportingPaneScaffoldRole.Main
): ThreePaneScaffoldState = ThreePaneScaffoldStateImpl(
    scaffoldDirective,
    calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        adaptStrategies,
        currentPaneDestination
    )
)

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
 * The set of the available pane roles of [SupportingPaneScaffold]. Basically those values are
 * aliases of [ThreePaneScaffoldRole]. We suggest you to use the values defined here instead of
 * the raw [ThreePaneScaffoldRole] under the context of [SupportingPaneScaffold] for better
 * code clarity.
 */
@ExperimentalMaterial3AdaptiveApi
object SupportingPaneScaffoldRole {
    /**
     * The main pane of [SupportingPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Primary].
     */
    val Main = ThreePaneScaffoldRole.Primary

    /**
     * The supporting pane of [SupportingPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Secondary].
     */
    val Supporting = ThreePaneScaffoldRole.Secondary

    /**
     * The extra pane of [SupportingPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
