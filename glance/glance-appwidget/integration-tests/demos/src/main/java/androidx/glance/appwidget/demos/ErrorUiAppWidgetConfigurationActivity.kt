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

package androidx.glance.appwidget.demos

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class OnErrorBehavior { Default, Custom, Ignore }
class ErrorUiAppWidgetConfigurationActivity : ComponentActivity() {

    private var repo: ErrorUiAppWidgetConfigurationRepo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId: Int = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return;
        }

        val glanceId: GlanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        val repo = ErrorUiAppWidgetConfigurationRepo(context = this, glanceId = glanceId)
        this.repo = repo

        setContent {
            ConfigurationUi(repo, onSaveAndFinish = { saveAndFinish(appWidgetId) })
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveAndFinish(appWidgetId: Int) {
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()

        GlobalScope.launch {
            val context: Context = this@ErrorUiAppWidgetConfigurationActivity
            val glanceId: GlanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            ErrorUiAppWidget().update(context = context, id = glanceId)
        }
    }

    override fun onStart() {
        super.onStart()
        this.repo = repo
    }

    override fun onStop() {
        super.onStop()
        repo?.unregisterObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        repo = null
    }
}

class ErrorUiAppWidgetConfigurationRepo(val context: Context, glanceId: GlanceId) {
    private val prefsFile = "androidx.glance.appwidget.demos.ErrorUiAppWidgetConfigurationRepo"
    private val prefsKey = "onErrorBehavior-$glanceId"
    private val sharedPreferences = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun update(behavior: OnErrorBehavior) {
        // todo
        sharedPreferences.edit {
            putInt(prefsKey, behavior.ordinal)
        }
    }

    fun getOnErrorBehavior(): OnErrorBehavior = getOnErrorBehavior(sharedPreferences)

    private fun getOnErrorBehavior(prefs: SharedPreferences): OnErrorBehavior {
        val ordinal = prefs.getInt(prefsKey, OnErrorBehavior.Default.ordinal)
        return OnErrorBehavior.values()[ordinal]
    }

    fun observeOnErrorBehavior(onChange: (OnErrorBehavior) -> Unit) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key != prefsKey) {
                    return@OnSharedPreferenceChangeListener
                } else {
                    onChange(getOnErrorBehavior(prefs))
                }
            }

        this.listener = listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterObserver() {
        listener?.let { sharedPreferences.unregisterOnSharedPreferenceChangeListener(it) }
    }
}

@Composable
private fun ConfigurationUi(
    repo: ErrorUiAppWidgetConfigurationRepo,
    onSaveAndFinish: () -> Unit
) {
    val selected: MutableState<OnErrorBehavior> =
        remember { mutableStateOf(repo.getOnErrorBehavior()) }
    LaunchedEffect(repo) {
        repo.observeOnErrorBehavior { newState -> selected.value = newState }
    }

    Box(Modifier.padding(16.dp)) {
        Column(Modifier.padding(24.dp)) {
            Text(text = "OnError behavior")
            Spacer(Modifier.size(16.dp))
            LabeledRadioButton(
                "Default Behavior",
                myBehavior = OnErrorBehavior.Default,
                selectedBehavior = selected.value,
                onClick = repo::update
            )
            LabeledRadioButton(
                "Custom Error UI",
                myBehavior = OnErrorBehavior.Custom,
                selectedBehavior = selected.value,
                onClick = repo::update
            )
            LabeledRadioButton(
                "Ignore Error, No UI Change",
                myBehavior = OnErrorBehavior.Ignore,
                selectedBehavior = selected.value,
                onClick = repo::update
            )

            Spacer(Modifier.size(32.dp))
            Button(onClick = onSaveAndFinish) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun LabeledRadioButton(
    text: String,
    myBehavior: OnErrorBehavior,
    selectedBehavior: OnErrorBehavior,
    onClick: (OnErrorBehavior) -> Unit
) {
    Row {
        RadioButton(selected = selectedBehavior == myBehavior, onClick = { onClick(myBehavior) })
        Spacer(Modifier.size(8.dp))
        Text(text)
    }
}
