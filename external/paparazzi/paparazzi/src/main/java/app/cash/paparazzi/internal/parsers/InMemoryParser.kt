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

import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParserException

/**
 * Derived from https://cs.android.com/android-studio/platform/tools/adt/idea/+/858f81bb7c350bc7a05daad36edefd21f74c8cef:android/src/com/android/tools/idea/rendering/parsers/LayoutPullParser.java;bpv=0;bpt=0
 *
 * A parser implementation that walks an in-memory XML DOM tree.
 */
abstract class InMemoryParser : KXmlParser() {
  abstract fun rootTag(): TagSnapshot

  private val nodeStack = mutableListOf<TagSnapshot>()
  private var parsingState = START_DOCUMENT

  override fun getAttributeCount(): Int {
    val tag = getCurrentNode() ?: return 0
    return tag.attributes.size
  }

  override fun getAttributeName(i: Int): String? {
    val attribute = getAttribute(i) ?: return null
    return attribute.name
  }

  override fun getAttributeNamespace(i: Int): String {
    val attribute = getAttribute(i) ?: return ""
    return attribute.namespace
  }

  override fun getAttributePrefix(i: Int): String? {
    val attribute = getAttribute(i) ?: return null
    return attribute.prefix
  }

  override fun getAttributeValue(i: Int): String? {
    val attribute = getAttribute(i) ?: return null
    return attribute.value
  }

  override fun getAttributeValue(
    namespace: String?,
    name: String?
  ): String? {
    val tag = getCurrentNode() ?: return null
    return tag.attributes.find { it.name == name }?.value
  }

  override fun getDepth(): Int = nodeStack.size

  override fun getName(): String? {
    if (parsingState == START_TAG || parsingState == END_TAG) {
      // Should only be called when START_TAG
      val currentNode = getCurrentNode()!!
      return currentNode.name
    }
    return null
  }

  @Throws(XmlPullParserException::class)
  override fun next(): Int {
    when (parsingState) {
      END_DOCUMENT -> throw XmlPullParserException("Nothing after the end")
      START_DOCUMENT -> onNextFromStartDocument()
      START_TAG -> onNextFromStartTag()
      END_TAG -> onNextFromEndTag()
    }
    return parsingState
  }

  private fun getCurrentNode(): TagSnapshot? = nodeStack.lastOrNull()

  private fun getAttribute(i: Int): AttributeSnapshot? {
    if (parsingState != START_TAG) {
      throw IndexOutOfBoundsException()
    }
    val tag = getCurrentNode() ?: return null
    return tag.attributes[i]
  }

  private fun push(node: TagSnapshot) {
    nodeStack.add(node)
  }

  private fun pop(): TagSnapshot = nodeStack.removeLast()

  private fun onNextFromStartDocument() {
    val rootTag = rootTag()
    @Suppress("SENSELESS_COMPARISON")
    parsingState = if (rootTag != null) {
      push(rootTag)
      START_TAG
    } else {
      END_DOCUMENT
    }
  }

  private fun onNextFromStartTag() {
    // get the current node, and look for text or children (children first)
    // Should only be called when START_TAG
    val node = getCurrentNode()!!
    val children = node.children
    parsingState = if (children.isNotEmpty()) {
      // move to the new child, and don't change the state.
      push(children[0])

      // in case the current state is CURRENT_DOC, we set the proper state.
      START_TAG
    } else {
      if (parsingState == START_DOCUMENT) {
        // this handles the case where there's no node.
        END_DOCUMENT
      } else {
        END_TAG
      }
    }
  }

  private fun onNextFromEndTag() {
    // look for a sibling. if no sibling, go back to the parent
    // Should only be called when END_TAG
    var node = getCurrentNode()!!
    val sibling = node.next
    if (sibling != null) {
      node = sibling
      // to go to the sibling, we need to remove the current node,
      pop()
      // and add its sibling.
      push(node)
      parsingState = START_TAG
    } else {
      // move back to the parent
      pop()

      // we have only one element left (myRoot), then we're done with the document.
      parsingState = if (nodeStack.isEmpty()) {
        END_DOCUMENT
      } else {
        END_TAG
      }
    }
  }
}
