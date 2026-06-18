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

package androidx.core.locationbutton.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.jvm.JvmInline

private const val TAG = "LocationButton"

internal val MinPadding: Dp = 4.dp
internal val MaxPadding: Dp = 8.dp

/**
 * Represents the text that can be displayed on the [LocationButton].
 *
 * These values correspond to the text strings supported by
 * [android.app.permissionui.LocationButtonSession].
 */
@JvmInline
public value class LocationButtonTextType private constructor(public val value: Int) {
    public companion object {
        /** The button displays no text. */
        public val None: LocationButtonTextType = LocationButtonTextType(0)

        /** The button displays the text as "Precise location". */
        public val PreciseLocation: LocationButtonTextType = LocationButtonTextType(1)

        /** The button displays the text as "Use precise location". */
        public val UsePreciseLocation: LocationButtonTextType = LocationButtonTextType(2)

        /** The button displays the text as "Share precise location". */
        public val SharePreciseLocation: LocationButtonTextType = LocationButtonTextType(3)

        /** The button displays the text as "Near my precise location". */
        public val NearMyPreciseLocation: LocationButtonTextType = LocationButtonTextType(4)

        /** The button displays the text as "Near your precise location". */
        public val NearYourPreciseLocation: LocationButtonTextType = LocationButtonTextType(5)
    }
}

/**
 * Displays a location button either rendered by the system or rendered locally (as a fallback).
 *
 * Uses remote rendering on [Build.VERSION_CODES.CINNAMON_BUN] and later. Falls back to a local
 * Compose implementation on platforms before [Build.VERSION_CODES.CINNAMON_BUN] or if remote
 * rendering fails.
 *
 * @param modifier Optional [Modifier] for the button layout.
 * @param backgroundColor Optional background color.
 * @param strokeColor Optional stroke color.
 * @param strokeWidth Optional stroke width.
 * @param cornerRadius Optional corner radius.
 * @param pressedCornerRadius Optional corner radius when pressed.
 * @param iconTint Optional icon tint.
 * @param textType The predefined [LocationButtonTextType] to display.
 * @param textColor Optional text color.
 * @param clickablePadding Optional padding between the clickable boundary and the visual button.
 * @param compositionOrder Optional Z-order for the remote surface. This only applies on
 *   [Build.VERSION_CODES.CINNAMON_BUN] and later. On older platforms, where the library falls back
 *   to a local button, this parameter is a safe no-op.
 * @param onRequestPermissions Optional callback when clicked on platforms before
 *   [Build.VERSION_CODES.CINNAMON_BUN]. If not provided, the button will automatically request
 *   location permissions using standard platform dialogs.
 * @param onError Optional callback invoked if the remote rendering session fails. Clients can use
 *   this to trigger a retry or display a custom fallback UI.
 * @param onPermissionResult Called with the permission result. Invoked on older platforms after the
 *   permission request completes (either default or custom), and on
 *   [Build.VERSION_CODES.CINNAMON_BUN] and later after the system-managed secure flow completes.
 */
@Composable
public fun LocationButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocationButtonDefaults.backgroundColor,
    strokeColor: Color = LocationButtonDefaults.strokeColor,
    strokeWidth: Dp = LocationButtonDefaults.strokeWidth,
    cornerRadius: Dp = LocationButtonDefaults.cornerRadius,
    pressedCornerRadius: Dp = LocationButtonDefaults.pressedCornerRadius,
    iconTint: Color = LocationButtonDefaults.iconTint,
    textType: LocationButtonTextType = LocationButtonDefaults.textType,
    textColor: Color = LocationButtonDefaults.textColor,
    clickablePadding: PaddingValues = LocationButtonDefaults.clickablePadding,
    compositionOrder: Int = LocationButtonDefaults.defaultCompositionOrder,
    onRequestPermissions: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    onPermissionResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        RemoteLocationButton(
            modifier = modifier,
            backgroundColor = backgroundColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            iconTint = iconTint,
            textType = textType.value,
            textColor = textColor,
            pressedCornerRadius = pressedCornerRadius,
            clickablePadding = clickablePadding,
            compositionOrder = compositionOrder,
            onPermissionResult = onPermissionResult,
            onError = { throwable -> onError?.invoke(throwable) },
        )
    } else {
        val requestPermissionsLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val isAccessFineLocationGranted =
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                onPermissionResult(isAccessFineLocationGranted)
            }

        val clickHandler =
            onRequestPermissions
                ?: {
                    val hasFineLocation =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED

                    if (hasFineLocation) {
                        onPermissionResult(true)
                    } else {
                        requestPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                }

        LocalLocationButton(
            onClick = clickHandler,
            modifier = modifier,
            backgroundColor = backgroundColor,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            iconTint = iconTint,
            textType = textType.value,
            textColor = textColor,
            pressedCornerRadius = pressedCornerRadius,
            clickablePadding = clickablePadding,
        )
    }
}
