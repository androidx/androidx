/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.samples

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Demo app for Glance Adaptive widgets.
 *
 * Discovers all widget receivers declared in this application's manifest and pins the one that is
 * tapped.
 */
class AdaptiveDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DemoScreen() } }
    }
}

@Composable
private fun DemoScreen() {
    val context = LocalContext.current
    val providers =
        remember(context) {
            AppWidgetManager.getInstance(context).installedProviders.filter {
                it.provider.packageName == context.packageName
            }
        }

    LazyColumn(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        item {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        item {
            Text(
                stringResource(R.string.pin_widget_instructions),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        items(providers, key = { it.provider.flattenToString() }) { providerInfo ->
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            WidgetRow(providerInfo) { pin(context, it.provider) }
        }
    }
}

@Composable
private fun WidgetRow(
    providerInfo: AppWidgetProviderInfo,
    onClick: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    // AppWidgetProviderInfo.loadLabel(PackageManager) returns a String (unlike
    // PackageItemInfo.loadLabel, which returns a CharSequence).
    val label: String =
        remember(providerInfo) {
            providerInfo.loadLabel(context.packageManager) ?: providerInfo.provider.shortClassName
        }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            Modifier.fillMaxWidth().clickable { onClick(providerInfo) }.padding(vertical = 16.dp),
    )
}

private fun pin(context: Context, provider: ComponentName) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        Toast.makeText(context, R.string.pin_widget_unsupported, Toast.LENGTH_SHORT).show()
        return
    }

    appWidgetManager.requestPinAppWidget(provider, null, null)
}
