/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MavenUploadHelperTest {
    @Test
    fun testSortPomDependencies() {
        /* ktlint-disable max-line-length */
        val pom = """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.collection</groupId>
  <artifactId>collection-jvm</artifactId>
  <version>1.3.0-alpha01</version>
  <name>Android Support Library collections</name>
  <description>Standalone efficient collections.</description>
  <url>https://developer.android.com/jetpack/androidx/releases/collection#1.3.0-alpha01</url>
  <inceptionYear>2018</inceptionYear>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <name>The Android Open Source Project</name>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:https://android.googlesource.com/platform/frameworks/support</connection>
    <url>https://cs.android.com/androidx/platform/frameworks/support</url>
  </scm>
  <dependencies>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib-common</artifactId>
      <version>1.6.10</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib</artifactId>
      <version>1.6.10</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation</artifactId>
      <version>1.3.0</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</project>
        """

        val expected = """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.collection</groupId>
  <artifactId>collection-jvm</artifactId>
  <version>1.3.0-alpha01</version>
  <name>Android Support Library collections</name>
  <description>Standalone efficient collections.</description>
  <url>https://developer.android.com/jetpack/androidx/releases/collection#1.3.0-alpha01</url>
  <inceptionYear>2018</inceptionYear>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <name>The Android Open Source Project</name>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:https://android.googlesource.com/platform/frameworks/support</connection>
    <url>https://cs.android.com/androidx/platform/frameworks/support</url>
  </scm>
  <dependencies>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation</artifactId>
      <version>1.3.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib-common</artifactId>
      <version>1.6.10</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib</artifactId>
      <version>1.6.10</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</project>
        """
        /* ktlint-enable max-line-length */

        val actual = sortPomDependencies(pom)
        assertEquals(expected, actual)
    }

    @Test
    fun testSortGradleMetadataDependencies() {
        /* ktlint-disable max-line-length */
        val metadata = """
{
  "formatVersion": "1.1",
  "component": {
    "url": "../../collection/1.3.0-alpha01/collection-1.3.0-alpha01.module",
    "group": "androidx.collection",
    "module": "collection",
    "version": "1.3.0-alpha01",
    "attributes": {
      "org.gradle.status": "release"
    }
  },
  "createdBy": {
    "gradle": {
      "version": "7.4"
    }
  },
  "variants": [
    {
      "name": "jvmApiElements-published",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.libraryelements": "jar",
        "org.gradle.usage": "java-api",
        "org.jetbrains.kotlin.platform.type": "jvm"
      },
      "dependencies": [
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib",
          "version": {
            "requires": "1.6.10"
          }
        },
        {
          "group": "androidx.annotation",
          "module": "annotation",
          "version": {
            "requires": "1.3.0"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib-common",
          "version": {
            "requires": "1.6.10"
          }
        }
      ],
      "files": [
        {
          "name": "collection-jvm-1.3.0-alpha01.jar",
          "url": "collection-jvm-1.3.0-alpha01.jar",
          "size": 42271,
          "sha512": "b01746682f5499426492ed56cfa10e863b181f0a94e1c97de935a1d68bc1a8da9b60bbc670a71642e4c4ebde0cedbed42f08f6b305bbfa7270b3b1fa76059fa6",
          "sha256": "647d39d1ef35b45ff9b4c4b2afd7b0280431223142ededb4ee2d3ff73eb2657a",
          "sha1": "11cbbdeaa0540d0cef16567781a99cdf7b34b242",
          "md5": "309042f77be5772d725180056e5e97e9"
        }
      ]
    },
    {
      "name": "jvmRuntimeElements-published",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.libraryelements": "jar",
        "org.gradle.usage": "java-runtime",
        "org.jetbrains.kotlin.platform.type": "jvm"
      },
      "dependencies": [
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib",
          "version": {
            "requires": "1.6.10"
          }
        },
        {
          "group": "androidx.annotation",
          "module": "annotation",
          "version": {
            "requires": "1.3.0"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib-common",
          "version": {
            "requires": "1.6.10"
          }
        }
      ],
      "files": [
        {
          "name": "collection-jvm-1.3.0-alpha01.jar",
          "url": "collection-jvm-1.3.0-alpha01.jar",
          "size": 42271,
          "sha512": "b01746682f5499426492ed56cfa10e863b181f0a94e1c97de935a1d68bc1a8da9b60bbc670a71642e4c4ebde0cedbed42f08f6b305bbfa7270b3b1fa76059fa6",
          "sha256": "647d39d1ef35b45ff9b4c4b2afd7b0280431223142ededb4ee2d3ff73eb2657a",
          "sha1": "11cbbdeaa0540d0cef16567781a99cdf7b34b242",
          "md5": "309042f77be5772d725180056e5e97e9"
        }
      ]
    }
  ]
}
        """

        val expected = """
{
  "formatVersion": "1.1",
  "component": {
    "url": "../../collection/1.3.0-alpha01/collection-1.3.0-alpha01.module",
    "group": "androidx.collection",
    "module": "collection",
    "version": "1.3.0-alpha01",
    "attributes": {
      "org.gradle.status": "release"
    }
  },
  "createdBy": {
    "gradle": {
      "version": "7.4"
    }
  },
  "variants": [
    {
      "name": "jvmApiElements-published",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.libraryelements": "jar",
        "org.gradle.usage": "java-api",
        "org.jetbrains.kotlin.platform.type": "jvm"
      },
      "dependencies": [
        {
          "group": "androidx.annotation",
          "module": "annotation",
          "version": {
            "requires": "1.3.0"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib",
          "version": {
            "requires": "1.6.10"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib-common",
          "version": {
            "requires": "1.6.10"
          }
        }
      ],
      "files": [
        {
          "name": "collection-jvm-1.3.0-alpha01.jar",
          "url": "collection-jvm-1.3.0-alpha01.jar",
          "size": 42271,
          "sha512": "b01746682f5499426492ed56cfa10e863b181f0a94e1c97de935a1d68bc1a8da9b60bbc670a71642e4c4ebde0cedbed42f08f6b305bbfa7270b3b1fa76059fa6",
          "sha256": "647d39d1ef35b45ff9b4c4b2afd7b0280431223142ededb4ee2d3ff73eb2657a",
          "sha1": "11cbbdeaa0540d0cef16567781a99cdf7b34b242",
          "md5": "309042f77be5772d725180056e5e97e9"
        }
      ]
    },
    {
      "name": "jvmRuntimeElements-published",
      "attributes": {
        "org.gradle.category": "library",
        "org.gradle.libraryelements": "jar",
        "org.gradle.usage": "java-runtime",
        "org.jetbrains.kotlin.platform.type": "jvm"
      },
      "dependencies": [
        {
          "group": "androidx.annotation",
          "module": "annotation",
          "version": {
            "requires": "1.3.0"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib",
          "version": {
            "requires": "1.6.10"
          }
        },
        {
          "group": "org.jetbrains.kotlin",
          "module": "kotlin-stdlib-common",
          "version": {
            "requires": "1.6.10"
          }
        }
      ],
      "files": [
        {
          "name": "collection-jvm-1.3.0-alpha01.jar",
          "url": "collection-jvm-1.3.0-alpha01.jar",
          "size": 42271,
          "sha512": "b01746682f5499426492ed56cfa10e863b181f0a94e1c97de935a1d68bc1a8da9b60bbc670a71642e4c4ebde0cedbed42f08f6b305bbfa7270b3b1fa76059fa6",
          "sha256": "647d39d1ef35b45ff9b4c4b2afd7b0280431223142ededb4ee2d3ff73eb2657a",
          "sha1": "11cbbdeaa0540d0cef16567781a99cdf7b34b242",
          "md5": "309042f77be5772d725180056e5e97e9"
        }
      ]
    }
  ]
}
        """
        /* ktlint-enable max-line-length */

        val actual = sortGradleMetadataDependencies(metadata)
        assertEquals(expected, actual)
    }
}
