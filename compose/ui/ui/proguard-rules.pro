# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# We supply these as stubs and are able to link to them at runtime
# because they are hidden public classes in Android. We don't want
# R8 to complain about them not being there during optimization.
-dontwarn android.view.RenderNode
-dontwarn android.view.DisplayListCanvas
-dontwarn android.view.HardwareCanvas

-keepclassmembers class androidx.compose.ui.platform.ViewLayerContainer {
    protected void dispatchGetDisplayList();
}

-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    android.view.View findViewByAccessibilityIdTraversal(int);
}

# Users can create Modifier.Node instances that implement multiple Modifier.Node interfaces,
# so we cannot tell whether two modifier.node instances are of the same type without using
# reflection to determine the class type. See b/265188224 for more context.
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement
