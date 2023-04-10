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

import com.android.SdkConstants.AAPT_ATTR_PREFIX
import com.android.SdkConstants.AAPT_PREFIX

/**
 * Derived from https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-master-dev:android/src/com/android/tools/idea/rendering/parsers/AaptAttrAttributeSnapshot.java
 *
 * Aapt attributes are attributes that instead of containing a reference, contain the inlined value
 * of the reference. This snapshot will generate a dynamic reference that will be used by the
 * resource resolution to be able to retrieve the inlined value.
 */
class AaptAttrSnapshot(
  override val namespace: String,
  override val prefix: String,
  override val name: String,
  val id: String,
  val bundledTag: TagSnapshot
) : AttributeSnapshot(namespace, prefix, name, "${AAPT_ATTR_PREFIX}$AAPT_PREFIX$id")
