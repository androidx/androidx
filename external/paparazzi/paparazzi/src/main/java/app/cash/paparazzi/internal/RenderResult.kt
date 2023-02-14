/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.android.ide.common.rendering.api.RenderSession
import com.android.ide.common.rendering.api.Result
import com.android.ide.common.rendering.api.ViewInfo
import java.awt.image.BufferedImage

internal data class RenderResult(
  val result: Result,
  val systemViews: List<ViewInfo>,
  val rootViews: List<ViewInfo>,
  val image: BufferedImage
)

internal fun RenderSession.toResult(): RenderResult {
  return RenderResult(result, systemRootViews.toList(), rootViews.toList(), image)
}
