/*
 * Copyright (C) 2014 The Android Open Source Project
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

package app.cash.paparazzi.internal.parsers

import com.android.SdkConstants.ATTR_IGNORE
import com.android.SdkConstants.EXPANDABLE_LIST_VIEW
import com.android.SdkConstants.GRID_VIEW
import com.android.SdkConstants.LIST_VIEW
import com.android.SdkConstants.SPINNER
import com.android.SdkConstants.TOOLS_URI
import com.android.ide.common.rendering.api.ILayoutPullParser
import com.android.ide.common.rendering.api.ResourceNamespace
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOError
import java.io.InputStream
import java.nio.charset.Charset
import okio.buffer
import okio.source
import org.xmlpull.v1.XmlPullParserException

/**
 * A layout parser that holds an in-memory tree of a given resource for subsequent traversal
 * during the inflation process
 */
internal class LayoutPullParser : InMemoryParser, AaptAttrParser, ILayoutPullParser {
  private constructor(inputStream: InputStream) : super() {
    try {
      val buffer = inputStream.source().buffer()

      setFeature(FEATURE_PROCESS_NAMESPACES, true)
      setInput(buffer.peek().inputStream(), null)

      // IntelliJ uses XmlFile/PsiFile to parse tag snapshots,
      // leaving XmlPullParser for Android to parse resources as usual
      // Here, we use the same XmlPullParser approach for both, which means
      // we need reinitialize the document stream between the two passes.
      val resourceParser = ResourceParser(buffer.inputStream())
      root = resourceParser.createTagSnapshot()

      // Obtain a list of all the aapt declared attributes
      declaredAaptAttrs = findDeclaredAaptAttrs(root)
    } catch (e: XmlPullParserException) {
      throw IOError(e)
    }
  }

  private constructor(aaptResource: TagSnapshot) : super() {
    root = aaptResource
    declaredAaptAttrs = emptyMap()
  }

  private val root: TagSnapshot
  private val declaredAaptAttrs: Map<String, TagSnapshot>

  private var layoutNamespace = ResourceNamespace.RES_AUTO

  override fun rootTag() = root

  @Suppress("SENSELESS_COMPARISON")
  override fun getViewCookie(): Any? {
    // TODO: Implement this properly.
    val name = super.getName() ?: return null

    // Store tools attributes if this looks like a layout we'll need adapter view
    // bindings for in the LayoutlibCallback.
    if (LIST_VIEW == name || EXPANDABLE_LIST_VIEW == name || GRID_VIEW == name || SPINNER == name) {
      var map: MutableMap<String, String>? = null
      val count = attributeCount
      for (i in 0 until count) {
        val namespace = getAttributeNamespace(i)
        if (namespace != null && namespace == TOOLS_URI) {
          val attribute = getAttributeName(i)!!
          if (attribute == ATTR_IGNORE) {
            continue
          }
          if (map == null) {
            map = HashMap(4)
          }
          map[attribute] = getAttributeValue(i)!!
        }
      }

      return map
    }

    return null
  }

  override fun getLayoutNamespace(): ResourceNamespace = layoutNamespace

  override fun getAaptDeclaredAttrs(): Map<String, TagSnapshot> = declaredAaptAttrs

  fun setLayoutNamespace(layoutNamespace: ResourceNamespace) {
    this.layoutNamespace = layoutNamespace
  }

  private fun findDeclaredAaptAttrs(tag: TagSnapshot): Map<String, TagSnapshot> {
    if (!tag.hasDeclaredAaptAttrs) {
      // Nor tag or any of the children has any aapt:attr declarations, we can stop here.
      return emptyMap()
    }

    return buildMap {
      tag.attributes
        .filterIsInstance<AaptAttrSnapshot>()
        .forEach { attr ->
          val bundledTag = attr.bundledTag
          put(attr.id, bundledTag)
          for (child in bundledTag.children) {
            putAll(findDeclaredAaptAttrs(child))
          }
        }
      for (child in tag.children) {
        putAll(findDeclaredAaptAttrs(child))
      }
    }
  }

  companion object {
    @Throws(FileNotFoundException::class)
    fun createFromFile(layoutFile: File) = LayoutPullParser(FileInputStream(layoutFile))

    /**
     * @param layoutPath Must start with '/' and be relative to test resources.
     */
    fun createFromPath(layoutPath: String): LayoutPullParser {
      @Suppress("NAME_SHADOWING") var layoutPath = layoutPath
      if (layoutPath.startsWith("/")) {
        layoutPath = layoutPath.substring(1)
      }

      return LayoutPullParser(
        LayoutPullParser::class.java.classLoader!!.getResourceAsStream(layoutPath)
      )
    }

    fun createFromString(contents: String): LayoutPullParser {
      return LayoutPullParser(
        ByteArrayInputStream(contents.toByteArray(Charset.forName("UTF-8")))
      )
    }

    fun createFromAaptResource(aaptResource: TagSnapshot): LayoutPullParser {
      return LayoutPullParser(aaptResource)
    }
  }
}
