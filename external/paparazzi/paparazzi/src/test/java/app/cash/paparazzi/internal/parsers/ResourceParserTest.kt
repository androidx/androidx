/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.internal.parsers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ResourceParserTest {
  @Test
  fun parseResource() {
    val root = parseResourceTree("plus_sign.xml")
    assertThat(root.namespace).isEmpty()
    assertThat(root.prefix).isNull()
    assertThat(root.name).isEqualTo(VECTOR_TAG_NAME)
    assertThat(root.hasDeclaredAaptAttrs).isEqualTo(false)
    assertThat(root.next).isNull()
    assertThat(root.attributes).containsExactly(
      AttributeSnapshot(ANDROID_NAMESPACE, ANDROID_PREFIX, "height", "24dp"),
      AttributeSnapshot(ANDROID_NAMESPACE, ANDROID_PREFIX, "viewportHeight", "40"),
      AttributeSnapshot(ANDROID_NAMESPACE, ANDROID_PREFIX, "viewportWidth", "40"),
      AttributeSnapshot(ANDROID_NAMESPACE, ANDROID_PREFIX, "width", "24dp")
    )

    val pathElement = root.children.single()
    with(pathElement) {
      assertThat(namespace).isEmpty()
      assertThat(prefix).isNull()
      assertThat(name).isEqualTo(PATH_TAG_NAME)
      assertThat(hasDeclaredAaptAttrs).isEqualTo(false)
      assertThat(next).isNull()
    }

    val pathAttributes = pathElement.attributes
    assertThat(pathAttributes).hasSize(2)
    assertThat(pathAttributes[0]).isEqualTo(
      AttributeSnapshot(ANDROID_NAMESPACE, ANDROID_PREFIX, FILL_COLOR_ATTR_NAME, "#999999")
    )

    with(pathAttributes[1]) {
      assertThat(namespace).isEqualTo(ANDROID_NAMESPACE)
      assertThat(prefix).isEqualTo(ANDROID_PREFIX)
      assertThat(name).isEqualTo(PATH_DATA_ATTR_NAME)
      assertThat(value).isNotEmpty // don't care about pathData precision
    }
  }

  @Test
  fun parseAaptAttrTags() {
    // Since #parseResource covers the basics, this test can be more targeted

    val root = parseResourceTree("card_chip.xml")
    assertThat(root.name).isEqualTo(VECTOR_TAG_NAME)
    assertThat(root.hasDeclaredAaptAttrs).isEqualTo(true)
    assertThat(root.next).isNull()

    assertThat(root.children).hasSize(2)

    val outerPathElement = root.children[0]
    assertThat(outerPathElement.hasDeclaredAaptAttrs).isEqualTo(false)

    val groupElement = root.children[1]
    assertThat(groupElement.hasDeclaredAaptAttrs).isEqualTo(true)

    assertThat(outerPathElement.next).isEqualTo(groupElement)

    val clipPathElement = groupElement.children[0]
    assertThat(clipPathElement.hasDeclaredAaptAttrs).isEqualTo(false)

    val innerPathElement1 = groupElement.children[1]
    assertThat(innerPathElement1.hasDeclaredAaptAttrs).isEqualTo(true)
    with(innerPathElement1.attributes[0]) {
      assertThat(name).isEqualTo(PATH_DATA_ATTR_NAME)
      assertThat(this).isNotInstanceOf(AaptAttrSnapshot::class.java)
    }
    with(innerPathElement1.attributes[1] as AaptAttrSnapshot) {
      assertThat(name).isEqualTo(FILL_COLOR_ATTR_NAME)
      assertThat(id).isEqualTo("1")
      assertThat(value).isEqualTo("@aapt:_aapt/aapt1")
      assertThat(bundledTag.name).isEqualTo(GRADIENT_TAG_NAME) // 🎉
    }

    val innerPathElement2 = groupElement.children[2]
    assertThat(innerPathElement2.hasDeclaredAaptAttrs).isEqualTo(true)
    with(innerPathElement2.attributes[0]) {
      assertThat(name).isEqualTo(PATH_DATA_ATTR_NAME)
      assertThat(this).isNotInstanceOf(AaptAttrSnapshot::class.java)
    }
    with(innerPathElement2.attributes[1] as AaptAttrSnapshot) {
      assertThat(name).isEqualTo(FILL_COLOR_ATTR_NAME)
      assertThat(id).isEqualTo("2")
      assertThat(value).isEqualTo("@aapt:_aapt/aapt2")
      assertThat(bundledTag.name).isEqualTo(GRADIENT_TAG_NAME) // 🎉
    }

    val innerPathElement3 = groupElement.children[3]
    assertThat(innerPathElement3.hasDeclaredAaptAttrs).isEqualTo(true)
    with(innerPathElement3.attributes[0]) {
      assertThat(name).isEqualTo(PATH_DATA_ATTR_NAME)
      assertThat(this).isNotInstanceOf(AaptAttrSnapshot::class.java)
    }
    with(innerPathElement3.attributes[1]) {
      assertThat(name).isEqualTo(FILL_TYPE_ATTR_NAME)
      assertThat(this).isNotInstanceOf(AaptAttrSnapshot::class.java)
    }
    with(innerPathElement3.attributes[2] as AaptAttrSnapshot) {
      assertThat(name).isEqualTo(FILL_COLOR_ATTR_NAME)
      assertThat(id).isEqualTo("3")
      assertThat(value).isEqualTo("@aapt:_aapt/aapt3")
      assertThat(bundledTag.name).isEqualTo(GRADIENT_TAG_NAME) // 🎉
    }
  }

  private fun parseResourceTree(resourceId: String): TagSnapshot {
    val resourceInputStream = javaClass.classLoader.getResourceAsStream(resourceId)!!
    return ResourceParser(resourceInputStream).createTagSnapshot()
  }

  companion object {
    const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    const val ANDROID_PREFIX = "android"

    const val VECTOR_TAG_NAME = "vector"
    const val PATH_TAG_NAME = "path"
    const val PATH_DATA_ATTR_NAME = "pathData"
    const val FILL_COLOR_ATTR_NAME = "fillColor"
    const val FILL_TYPE_ATTR_NAME = "fillType"
    const val GRADIENT_TAG_NAME = "gradient"
  }
}
