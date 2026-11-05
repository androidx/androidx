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

package androidx.navigation3.dynamicfeatures.samples

import androidx.activity.compose.LocalActivity
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.dynamicfeatures.DynamicFeatureModuleEntryProvider
import androidx.navigation3.dynamicfeatures.rememberDynamicFeatureNavEntryDecorator
import androidx.navigation3.dynamicfeatures.rememberSplitInstallManager
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.serialization.Serializable

// Shared keys defined in a common module visible to both the base app and dynamic feature modules.
@Serializable internal data object HomeKey : NavKey

@Serializable internal data object CheckoutKey : NavKey

@Serializable internal data object PromoKey : NavKey

@Suppress("unused", "FunctionName")
@Sampled
@Composable
public fun DynamicFeatureNavDisplaySample() {
    val backStack = rememberNavBackStack(HomeKey)
    val splitInstallManager = rememberSplitInstallManager()
    val dynamicFeatureDecorator =
        rememberDynamicFeatureNavEntryDecorator<NavKey>(
            splitInstallManager = splitInstallManager,
            defaultLoadingContent = { sessionState ->
                when (sessionState.status()) {
                    SplitInstallSessionStatus.DOWNLOADING -> {
                        val progress =
                            if (sessionState.totalBytesToDownload() > 0) {
                                sessionState.bytesDownloaded().toFloat() /
                                    sessionState.totalBytesToDownload()
                            } else {
                                0f
                            }
                        CircularProgressIndicator(progress = { progress })
                    }
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        val activity = LocalActivity.current
                        LaunchedEffect(sessionState.sessionId()) {
                            if (activity != null) {
                                splitInstallManager.startConfirmationDialogForResult(
                                    sessionState,
                                    activity,
                                    100,
                                )
                            }
                        }
                    }
                    SplitInstallSessionStatus.FAILED -> {
                        Column {
                            Text("Installation failed (error ${sessionState.errorCode()})")
                            Button(onClick = { backStack.removeLastOrNull() }) { Text("Go Back") }
                        }
                    }
                    else -> CircularProgressIndicator()
                }
            },
        )

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                dynamicFeatureDecorator,
            ),
        entryProvider =
            dynamicFeatureDecorator.entryProvider {
                entry<HomeKey> {
                    Column {
                        Button(onClick = { backStack.add(CheckoutKey) }) { Text("Go to Checkout") }
                        Button(onClick = { backStack.add(PromoKey) }) { Text("View Promo") }
                    }
                }
                dynamicFeatureModule(
                    initialKey = CheckoutKey,
                    moduleName = "checkout",
                    entryProviderClassName = "com.example.checkout.CheckoutEntryProvider",
                )
                dynamicFeatureModule(
                    initialKey = PromoKey,
                    moduleName = "promo",
                    entryProviderClassName = "com.example.promo.PromoEntryProvider",
                    loadingContent = { Text("Loading promo...") },
                )
            },
    )
}

// Private key defined inside the `:checkout` dynamic feature module.
@Serializable internal data class OrderConfirmationKey(val orderId: String) : NavKey

@Suppress("unused", "FunctionName")
@Sampled
public fun DynamicFeatureModuleEntryProviderSample() {
    // Defined in the `:checkout` dynamic feature module at
    // `com.example.checkout.CheckoutEntryProvider`.
    class CheckoutEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
        override fun EntryProviderScope<NavKey>.buildEntries() {
            entry<CheckoutKey> {
                Text("Checkout Screen")
            }
            entry<OrderConfirmationKey> { key ->
                Text("Order Confirmed: ${key.orderId}")
            }
        }
    }

    // Defined in the `:promo` dynamic feature module at
    // `com.example.promo.PromoEntryProvider`.
    class PromoEntryProvider : DynamicFeatureModuleEntryProvider<NavKey> {
        override fun EntryProviderScope<NavKey>.buildEntries() {
            entry<PromoKey> {
                Text("Promo Screen")
            }
        }
    }
}
