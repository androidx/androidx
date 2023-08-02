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

import com.android.ide.common.rendering.api.AssetRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

internal class PaparazziAssetRepository(private val assetPath: String) : AssetRepository() {
  @Throws(FileNotFoundException::class)
  private fun open(path: String): InputStream? {
    val asset = File(path)
    return when {
      asset.isFile -> FileInputStream(asset)
      else -> null
    }
  }

  override fun isSupported(): Boolean = true

  @Throws(IOException::class)
  override fun openAsset(
    path: String,
    mode: Int
  ): InputStream? = open("$assetPath/$path")

  @Throws(IOException::class)
  override fun openNonAsset(
    cookie: Int,
    path: String,
    mode: Int
  ): InputStream? = open(path)
}
