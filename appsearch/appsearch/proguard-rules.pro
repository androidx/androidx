#  Copyright (C) 2021 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# Keep DocumentClassFactory classes only when DocumentClassFactoryRegistry is used. Since
# DocumentClassFactoryRegistry loads these classes by name, also do not obfuscate names.
-if class androidx.appsearch.app.DocumentClassFactoryRegistry {}
-keep,allowoptimization class ** implements androidx.appsearch.app.DocumentClassFactory {}

-if class androidx.appsearch.app.DocumentClassFactoryRegistry {}
-keep,allowshrinking @androidx.appsearch.annotation.Document class ** {}

# Keep AppSearchDocumentClassMap name for ServiceLoader
-keep,allowshrinking,allowoptimization class androidx.appsearch.app.AppSearchDocumentClassMap {}

# If AppSearchDocumentClassMap#getGlobalMap() is used, keep any implementing subclasses since
# AppSearchDocumentClassMap#getMap() is used to populate the global map.
-if class androidx.appsearch.app.AppSearchDocumentClassMap {
  public static java.util.Map getGlobalMap();
}
-keep,allowoptimization class ** implements androidx.appsearch.app.AppSearchDocumentClassMap {
  public java.util.Map getMap();
}
