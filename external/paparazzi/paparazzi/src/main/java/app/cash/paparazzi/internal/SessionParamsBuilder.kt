/*
 * Copyright (C) 2017 The Android Open Source Project
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

package app.cash.paparazzi.internal

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.internal.parsers.LayoutPullParser
import com.android.SdkConstants
import com.android.ide.common.rendering.api.AssetRepository
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.ide.common.rendering.api.SessionParams
import com.android.ide.common.rendering.api.SessionParams.Key
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.android.ide.common.resources.ResourceResolver
import com.android.ide.common.resources.ResourceValueMap
import com.android.layoutlib.bridge.Bridge
import com.android.resources.LayoutDirection
import com.android.resources.ResourceType

/** Creates [SessionParams] objects. */
internal data class SessionParamsBuilder(
  private val layoutlibCallback: PaparazziCallback,
  private val logger: PaparazziLogger,
  @Suppress("DEPRECATION")
  private val frameworkResources: com.android.ide.common.resources.deprecated.ResourceRepository,
  private val assetRepository: AssetRepository,
  @Suppress("DEPRECATION")
  private val projectResources: com.android.ide.common.resources.deprecated.ResourceRepository,
  private val deviceConfig: DeviceConfig = DeviceConfig.NEXUS_5,
  private val renderingMode: RenderingMode = RenderingMode.NORMAL,
  private val targetSdk: Int = 22,
  private val flags: Map<Key<*>, Any> = mapOf(),
  private val themeName: String? = null,
  private val isProjectTheme: Boolean = false,
  private val layoutPullParser: LayoutPullParser? = null,
  private val projectKey: Any? = null,
  private val minSdk: Int = 0,
  private val decor: Boolean = true
) {
  fun withTheme(
    themeName: String,
    isProjectTheme: Boolean
  ): SessionParamsBuilder {
    return copy(themeName = themeName, isProjectTheme = isProjectTheme)
  }

  fun withTheme(themeName: String): SessionParamsBuilder {
    return when {
      themeName.startsWith(SdkConstants.PREFIX_ANDROID) -> {
        withTheme(themeName.substring(SdkConstants.PREFIX_ANDROID.length), false)
      }
      else -> withTheme(themeName, true)
    }
  }

  fun plusFlag(
    flag: SessionParams.Key<*>,
    value: Any
  ) = copy(flags = flags + (flag to value))

  fun build(): SessionParams {
    require(themeName != null)

    val folderConfiguration = deviceConfig.folderConfiguration

    @Suppress("DEPRECATION")
    val resourceResolver = ResourceResolver.create(
      mapOf<ResourceNamespace, Map<ResourceType, ResourceValueMap>>(
        ResourceNamespace.ANDROID to frameworkResources.getConfiguredResources(
          folderConfiguration
        ),
        ResourceNamespace.TODO() to projectResources.getConfiguredResources(
          folderConfiguration
        )
      ),
      ResourceReference(
        ResourceNamespace.fromBoolean(!isProjectTheme),
        ResourceType.STYLE,
        themeName
      )
    )

    val result = SessionParams(
      layoutPullParser, renderingMode, projectKey /* for caching */,
      deviceConfig.hardwareConfig, resourceResolver, layoutlibCallback, minSdk, targetSdk, logger
    )
    result.fontScale = deviceConfig.fontScale

    val localeQualifier = folderConfiguration.localeQualifier
    val layoutDirectionQualifier = folderConfiguration.layoutDirectionQualifier
    // https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:android/src/com/android/tools/idea/rendering/RenderTask.java;l=645
    if (
      LayoutDirection.RTL == layoutDirectionQualifier.value &&
      !Bridge.isLocaleRtl(localeQualifier.tag)
    ) {
      result.locale = "ur"
    } else {
      result.locale = localeQualifier.tag
    }
    result.setRtlSupport(true)

    for ((key, value) in flags) {
      @Suppress("UNCHECKED_CAST")
      result.setFlag(key as Key<Any>, value)
    }
    result.setAssetRepository(assetRepository)

    if (!decor) {
      result.setForceNoDecor()
    }

    return result
  }
}
