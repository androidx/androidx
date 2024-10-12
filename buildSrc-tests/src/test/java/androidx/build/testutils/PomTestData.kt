/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.testutils

/** POM for androidx.collection:collection:1.3.0-alpha04, which is a KMP library anchor artifact. */
const val POM_COLLECTION =
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.collection</groupId>
  <artifactId>collection</artifactId>
  <version>1.3.0-alpha05</version>
  <name>collections</name>
  <description>Standalone efficient collections.</description>
  <url>https://developer.android.com/jetpack/androidx/releases/collection#1.3.0-alpha05</url>
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
      <artifactId>kotlin-stdlib</artifactId>
      <version>1.8.21</version>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>"""

/**
 * POM for androidx.collection:collection-jvm:1.3.0-alpha04, which is a KMP library targeted to JVM.
 */
const val POM_COLLECTION_JVM =
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.collection</groupId>
  <artifactId>collection-jvm</artifactId>
  <version>1.3.0-alpha05</version>
  <name>collections</name>
  <description>Standalone efficient collections.</description>
  <url>https://developer.android.com/jetpack/androidx/releases/collection#1.3.0-alpha05</url>
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
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>androidx.collection</groupId>
        <artifactId>collection-ktx</artifactId>
        <version>1.3.0-alpha01</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib</artifactId>
      <version>1.8.21</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation</artifactId>
      <version>1.3.0</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</project>"""

/** POM for androidx.compose.ui:ui-geometry:1.6.0-alpha01. */
const val POM_COMPOSE_UI_GEOMETRY =
    """<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.compose.ui</groupId>
  <artifactId>ui-geometry</artifactId>
  <version>1.6.0-alpha01</version>
  <packaging>pom</packaging>
  <name>Compose Geometry</name>
  <description>Compose classes related to dimensions without units</description>
  <url>https://developer.android.com/jetpack/androidx/releases/compose-ui#1.6.0-alpha01</url>
  <inceptionYear>2020</inceptionYear>
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
</project>
"""

/** POM for androidx.core:core:1.12.0-alpha05. */
const val POM_CORE_CORE =
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <!-- This module was also published with a richer model, Gradle metadata,  -->
  <!-- which should be used instead. Do not delete the following line which  -->
  <!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
  <!-- that they should prefer consuming it instead. -->
  <!-- do_not_remove: published-with-gradle-metadata -->
  <modelVersion>4.0.0</modelVersion>
  <groupId>androidx.core</groupId>
  <artifactId>core</artifactId>
  <version>1.12.0-alpha05</version>
  <packaging>pom</packaging>
  <name>Core</name>
  <description>Provides backward-compatible implementations of Android platform APIs and features.</description>
  <url>https://developer.android.com/jetpack/androidx/releases/core#1.12.0-alpha05</url>
  <inceptionYear>2015</inceptionYear>
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
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>androidx.core</groupId>
        <artifactId>core-ktx</artifactId>
        <version>1.12.0-alpha05</version>
      </dependency>
      <dependency>
        <groupId>androidx.core</groupId>
        <artifactId>core-testing</artifactId>
        <version>1.12.0-alpha05</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation</artifactId>
      <version>1.6.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.annotation</groupId>
      <artifactId>annotation-experimental</artifactId>
      <version>1.3.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.collection</groupId>
      <artifactId>collection</artifactId>
      <version>1.0.0</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>androidx.concurrent</groupId>
      <artifactId>concurrent-futures</artifactId>
      <version>1.0.0</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>androidx.interpolator</groupId>
      <artifactId>interpolator</artifactId>
      <version>1.0.0</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>androidx.lifecycle</groupId>
      <artifactId>lifecycle-runtime</artifactId>
      <version>2.3.1</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>androidx.versionedparcelable</groupId>
      <artifactId>versionedparcelable</artifactId>
      <version>1.1.1</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib</artifactId>
      <version>1.8.22</version>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>
"""
