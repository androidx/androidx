package app.cash.paparazzi.internal.parsers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.xmlpull.v1.XmlPullParserException

class InMemoryParserTest {
  @Test
  fun parse() {
    val root = parseResourceTree("plus_sign.xml")
    val parser = RealInMemoryParser(root)

    assertThat(parser.name).isNull() // START_DOCUMENT
    assertThat(parser.depth).isEqualTo(0)

    parser.next() // START_TAG = "vector"

    assertThat(parser.name).isEqualTo(VECTOR_TAG_NAME)
    assertThat(parser.depth).isEqualTo(1)
    assertThat(parser.attributeCount).isEqualTo(4)

    assertThat(parser.getAttributeName(0)).isEqualTo("height")
    assertThat(parser.getAttributeName(1)).isEqualTo("viewportHeight")
    assertThat(parser.getAttributeName(2)).isEqualTo("viewportWidth")
    assertThat(parser.getAttributeName(3)).isEqualTo("width")

    assertThat(parser.getAttributeNamespace(0)).isEqualTo(ANDROID_NAMESPACE)
    assertThat(parser.getAttributeNamespace(1)).isEqualTo(ANDROID_NAMESPACE)
    assertThat(parser.getAttributeNamespace(2)).isEqualTo(ANDROID_NAMESPACE)
    assertThat(parser.getAttributeNamespace(3)).isEqualTo(ANDROID_NAMESPACE)

    assertThat(parser.getAttributePrefix(0)).isEqualTo(ANDROID_PREFIX)
    assertThat(parser.getAttributePrefix(1)).isEqualTo(ANDROID_PREFIX)
    assertThat(parser.getAttributePrefix(2)).isEqualTo(ANDROID_PREFIX)
    assertThat(parser.getAttributePrefix(3)).isEqualTo(ANDROID_PREFIX)

    assertThat(parser.getAttributeValue(0)).isEqualTo("24dp")
    assertThat(parser.getAttributeValue(1)).isEqualTo("40")
    assertThat(parser.getAttributeValue(2)).isEqualTo("40")
    assertThat(parser.getAttributeValue(3)).isEqualTo("24dp")

    parser.next() // START_TAG = "path"

    assertThat(parser.name).isEqualTo(PATH_TAG_NAME)
    assertThat(parser.depth).isEqualTo(2)
    assertThat(parser.attributeCount).isEqualTo(2)

    assertThat(parser.getAttributeName(0)).isEqualTo(FILL_COLOR_ATTR_NAME)
    assertThat(parser.getAttributeName(1)).isEqualTo(PATH_DATA_ATTR_NAME)

    assertThat(parser.getAttributeNamespace(0)).isEqualTo(ANDROID_NAMESPACE)
    assertThat(parser.getAttributeNamespace(1)).isEqualTo(ANDROID_NAMESPACE)

    assertThat(parser.getAttributePrefix(0)).isEqualTo(ANDROID_PREFIX)
    assertThat(parser.getAttributePrefix(1)).isEqualTo(ANDROID_PREFIX)

    assertThat(parser.getAttributeValue(0)).isEqualTo("#999999")
    assertThat(parser.getAttributeValue(1)).isNotNull // pathData

    parser.next() // END_TAG = "path"

    assertThat(parser.name).isEqualTo(PATH_TAG_NAME)
    assertThat(parser.depth).isEqualTo(2)

    parser.next() // END_TAG = "vector"

    assertThat(parser.name).isEqualTo(VECTOR_TAG_NAME)
    assertThat(parser.depth).isEqualTo(1)

    parser.next() // END_DOCUMENT
    assertThat(parser.name).isNull() // START_DOCUMENT
    assertThat(parser.depth).isEqualTo(0)

    try {
      parser.next()
    } catch (expected: XmlPullParserException) {
    }
  }

  private fun parseResourceTree(resourceId: String): TagSnapshot {
    val resourceInputStream = javaClass.classLoader.getResourceAsStream(resourceId)!!
    return ResourceParser(resourceInputStream).createTagSnapshot()
  }

  class RealInMemoryParser(private val root: TagSnapshot) : InMemoryParser() {
    override fun rootTag(): TagSnapshot = root
  }

  companion object {
    const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    const val ANDROID_PREFIX = "android"

    const val VECTOR_TAG_NAME = "vector"
    const val PATH_TAG_NAME = "path"
    const val PATH_DATA_ATTR_NAME = "pathData"
    const val FILL_COLOR_ATTR_NAME = "fillColor"
  }
}
