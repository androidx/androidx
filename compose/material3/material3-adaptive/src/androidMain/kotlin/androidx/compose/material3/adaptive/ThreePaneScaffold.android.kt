package androidx.compose.material3.adaptive

import androidx.compose.runtime.Stable

/**
 * The state of [ListDetailPaneScaffold]. It provides the layout directive and value state that will
 * be updated directly. It also provides functions to perform navigation.
 *
 * @property scaffoldDirective the current layout directives that the associated
 *           [ListDetailPaneScaffold] needs to follow. It's supposed to be automatically updated
 *           when the window configuration changes.
 * @property scaffoldValue the current layout value of the associated [ListDetailPaneScaffold],
 *           which represents unique layout states of the scaffold.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
interface ThreePaneScaffoldState {
    val scaffoldDirective: PaneScaffoldDirective
    val scaffoldValue: ThreePaneScaffoldValue
}

@ExperimentalMaterial3AdaptiveApi
internal class ThreePaneScaffoldStateImpl(
    override val scaffoldDirective: PaneScaffoldDirective,
    override val scaffoldValue: ThreePaneScaffoldValue
) : ThreePaneScaffoldState
