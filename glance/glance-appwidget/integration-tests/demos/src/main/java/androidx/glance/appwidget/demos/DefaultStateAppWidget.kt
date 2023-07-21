package androidx.glance.appwidget.demos

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

// Defines a state key for [currentState]
private val CountClicksKey = intPreferencesKey("CountClicks")

// Defines an action key for [actionRunCallback]
private val ClickValueKey = ActionParameters.Key<Int>("ClickValue")

// Showcases a simple widget that uses the default [stateDefinition] of [GlanceAppWidget] to store
// the +/- clicks values.
class DefaultStateAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        // Get the current stored value for the given Key.
        val count = currentState(CountClicksKey) ?: 0

        Row(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .padding(16.dp)
                .background(R.color.default_widget_background),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = GlanceModifier.defaultWeight(),
                text = "-",
                style = TextStyle(textAlign = TextAlign.Center),
                onClick = actionRunCallback<ClickAction>(
                    actionParametersOf(ClickValueKey to -1)
                )
            )
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = "$count",
                style = TextStyle(textAlign = TextAlign.Center)
            )
            Button(
                modifier = GlanceModifier.defaultWeight(),
                text = "+",
                style = TextStyle(textAlign = TextAlign.Center),
                onClick = actionRunCallback<ClickAction>(
                    actionParametersOf(ClickValueKey to 1)
                )
            )
        }
    }
}

class ClickAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Get the current state of the given widget and the value provided in the ActionParameters
        updateAppWidgetState(context, glanceId) { state ->
            state[CountClicksKey] = (state[CountClicksKey] ?: 0) + (parameters[ClickValueKey] ?: 0)
        }
        // Trigger the widget update
        DefaultStateAppWidget().update(context, glanceId)
    }
}

class DefaultStateAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DefaultStateAppWidget()
}