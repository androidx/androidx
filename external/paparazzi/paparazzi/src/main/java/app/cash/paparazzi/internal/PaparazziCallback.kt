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

package app.cash.paparazzi.internal

import app.cash.paparazzi.internal.parsers.LayoutPullParser
import app.cash.paparazzi.internal.parsers.TagSnapshot
import com.android.ide.common.rendering.api.ActionBarCallback
import com.android.ide.common.rendering.api.AdapterBinding
import com.android.ide.common.rendering.api.ILayoutPullParser
import com.android.ide.common.rendering.api.LayoutlibCallback
import com.android.ide.common.rendering.api.ResourceNamespace.RES_AUTO
import com.android.ide.common.rendering.api.ResourceReference
import com.android.ide.common.rendering.api.ResourceValue
import com.android.ide.common.rendering.api.SessionParams.Key
import com.android.layoutlib.bridge.android.RenderParamsFlags
import com.android.resources.ResourceType
import com.android.resources.ResourceType.STYLE
import com.google.common.io.ByteStreams
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Modifier
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

internal class PaparazziCallback(
  private val logger: PaparazziLogger,
  private val resourcePackageNames: List<String>
) : LayoutlibCallback() {
  private val projectResources = mutableMapOf<Int, ResourceReference>()
  private val resources = mutableMapOf<ResourceReference, Int>()
  private val actionBarCallback = ActionBarCallback()
  private val aaptDeclaredResources = mutableMapOf<String, TagSnapshot>()

  private var adaptiveIconMaskPath: String? = null
  private val loadedClasses = mutableMapOf<String, Class<*>>()

  @Throws(ClassNotFoundException::class)
  fun initResources() {
    for (rPackageName in resourcePackageNames) {
      val rClass = Class.forName("$rPackageName.R")
      for (resourceClass in rClass.declaredClasses) {
        val resourceType = ResourceType.fromClassName(resourceClass.simpleName) ?: continue

        for (field in resourceClass.declaredFields) {
          if (!Modifier.isStatic(field.modifiers)) continue

          // May not be final in library projects.
          val type = field.type
          try {
            if (type == Int::class.javaPrimitiveType) {
              val value = field.get(null) as Int
              val reference = ResourceReference(RES_AUTO, resourceType, field.name)
              projectResources[value] = reference
              resources[reference] = value
            } else if (type.isArray && type.componentType == Int::class.javaPrimitiveType) {
              // Ignore.
            } else {
              logger.error(null, "Unknown field type in R class: $type")
            }
          } catch (e: IllegalAccessException) {
            logger.error(e, "Malformed R class: %1\$s", "$rPackageName.R")
          }
        }
      }
    }
  }

  @Throws(Exception::class)
  override fun loadView(
    name: String,
    constructorSignature: Array<Class<*>>,
    constructorArgs: Array<Any>
  ): Any? {
    val viewClass = Class.forName(name)
    val viewConstructor = viewClass.getConstructor(*constructorSignature)
    viewConstructor.isAccessible = true
    return viewConstructor.newInstance(*constructorArgs)
  }

  override fun resolveResourceId(id: Int): ResourceReference? = projectResources[id]

  override fun getOrGenerateResourceId(resource: ResourceReference): Int {
    // Workaround: We load our resource map from fields in R.class, which are named using Java
    // class conventions.  Therefore, we need to similarly transform style naming conventions
    // that contain periods (e.g., Widget.AppCompat.TextView) to avoid false lookup misses.
    // Long-term: Perhaps parse and load resource names from file system directly?
    val resourceKey =
      if (resource.resourceType == STYLE) resource.transformStyleResource() else resource
    return resources[resourceKey] ?: 0
  }

  override fun getParser(layoutResource: ResourceValue): ILayoutPullParser? {
    try {
      val value = layoutResource.value ?: return null
      if (aaptDeclaredResources.isNotEmpty() && layoutResource.resourceType == ResourceType.AAPT) {
        val aaptResource = aaptDeclaredResources.getValue(value)
        return LayoutPullParser.createFromAaptResource(aaptResource)
      }

      return LayoutPullParser.createFromFile(File(layoutResource.value))
        .also {
          // For parser of elements included in this parser, publish any aapt declared values
          aaptDeclaredResources.putAll(it.getAaptDeclaredAttrs())
        }
    } catch (e: FileNotFoundException) {
      return null
    }
  }

  override fun getAdapterItemValue(
    adapterView: ResourceReference,
    adapterCookie: Any,
    itemRef: ResourceReference,
    fullPosition: Int,
    positionPerType: Int,
    fullParentPosition: Int,
    parentPositionPerType: Int,
    viewRef: ResourceReference,
    viewAttribute: ViewAttribute,
    defaultValue: Any
  ): Any? = null

  override fun getAdapterBinding(
    adapterViewRef: ResourceReference,
    adapterCookie: Any,
    viewObject: Any
  ): AdapterBinding? = null

  override fun getActionBarCallback(): ActionBarCallback = actionBarCallback

  override fun createXmlParserForPsiFile(fileName: String): XmlPullParser? =
    createXmlParserForFile(fileName)

  override fun createXmlParserForFile(fileName: String): XmlPullParser? {
    try {
      FileInputStream(fileName).use { fileStream ->
        // Read data fully to memory to be able to close the file stream.
        val byteOutputStream = ByteArrayOutputStream()
        ByteStreams.copy(fileStream, byteOutputStream)
        val parser = KXmlParser()
        parser.setInput(ByteArrayInputStream(byteOutputStream.toByteArray()), null)
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        return parser
      }
    } catch (e: IOException) {
      return null
    } catch (e: XmlPullParserException) {
      return null
    }
  }

  override fun createXmlParser(): XmlPullParser = KXmlParser()

  @Suppress("UNCHECKED_CAST")
  override fun <T> getFlag(key: Key<T>?): T? {
    return when (key) {
      RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH -> adaptiveIconMaskPath as T?
      else -> null
    }
  }

  fun setAdaptiveIconMaskPath(adaptiveIconMaskPath: String) {
    this.adaptiveIconMaskPath = adaptiveIconMaskPath
  }

  override fun findClass(name: String): Class<*> {
    val clazz = loadedClasses[name]
    logger.verbose("loadClassA($name)")

    try {
      if (clazz != null) {
        return clazz
      }
      val clazz2 = Class.forName(name)
      logger.verbose("loadClassB($name)")
      loadedClasses[name] = clazz2
      return clazz2
    } catch (e: LinkageError) {
      throw ClassNotFoundException("error loading class $name", e)
    } catch (e: ExceptionInInitializerError) {
      throw ClassNotFoundException("error loading class $name", e)
    } catch (e: ClassNotFoundException) {
      throw ClassNotFoundException("error loading class $name", e)
    }
  }

  private fun ResourceReference.transformStyleResource() =
    ResourceReference.style(namespace, name.replace('.', '_'))
}
