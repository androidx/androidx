/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.tracing.trace
import java.io.ByteArrayInputStream

@Composable
public fun rememberRemoteDocument(
    creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    onCreate: ((CoreDocument) -> Unit)? = null,
    clock: RemoteClock = RemoteClock.SYSTEM,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    val layoutDirection = LocalLayoutDirection.current
    val doc: MutableState<CoreDocument?> = remember { mutableStateOf(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val document =
            captureSingleRemoteDocument(
                creationDisplayInfo = createCreationDisplayInfo(context),
                layoutDirection = layoutDirection,
                context = context,
                content = content,
                profile = profile,
                clock = clock,
            )
        val coreDocument =
            CoreDocument(clock).apply {
                trace("CreateRemoteDocument:parsing") {
                    initFromBuffer(
                        RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
                    )
                }
            }
        if (onCreate != null) {
            onCreate(coreDocument)
        }
        doc.value = coreDocument
    }
    return doc
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteDocument(
    size: Size,
    onCreate: ((CoreDocument) -> Unit)? = null,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    return rememberRemoteDocument(
        creationDisplayInfo =
            RemoteCreationDisplayInfo(
                size.width.toInt(),
                size.height.toInt(),
                LocalConfiguration.current.densityDpi,
                LocalConfiguration.current.fontScale,
            ),
        onCreate = onCreate,
        content = content,
    )
}

@Composable
public fun displaySize(): Size {
    // Note: Usage of LocalConfiguration.current.screenWidthDp was replaced due to
    // `ConfigurationScreenWidthHeight` lint rule
    // TODO: Consider to remove this function at all
    return LocalWindowInfo.current.containerSize.toSize()
}

/** Convert an Android layout direction to a compose [layout direction][LayoutDirection]. */
internal fun toLayoutDirection(androidLayoutDirection: Int): LayoutDirection {
    return when (androidLayoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        else -> throw IllegalArgumentException("Unknown layout direction: $androidLayoutDirection")
    }
}
