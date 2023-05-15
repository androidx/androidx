/*
 * Copyright (C) 2015 The Android Open Source Project
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
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package android.support.customtabs;
/* @hide */
interface ICustomTabsService {
  boolean warmup(long flags) = 1;
  boolean newSession(in android.support.customtabs.ICustomTabsCallback callback) = 2;
  boolean newSessionWithExtras(in android.support.customtabs.ICustomTabsCallback callback, in android.os.Bundle extras) = 9;
  boolean mayLaunchUrl(in android.support.customtabs.ICustomTabsCallback callback, in android.net.Uri url, in android.os.Bundle extras, in List<android.os.Bundle> otherLikelyBundles) = 3;
  android.os.Bundle extraCommand(String commandName, in android.os.Bundle args) = 4;
  boolean updateVisuals(in android.support.customtabs.ICustomTabsCallback callback, in android.os.Bundle bundle) = 5;
  boolean requestPostMessageChannel(in android.support.customtabs.ICustomTabsCallback callback, in android.net.Uri postMessageOrigin) = 6;
  boolean requestPostMessageChannelWithExtras(in android.support.customtabs.ICustomTabsCallback callback, in android.net.Uri postMessageOrigin, in android.os.Bundle extras) = 10;
  int postMessage(in android.support.customtabs.ICustomTabsCallback callback, String message, in android.os.Bundle extras) = 7;
  boolean validateRelationship(in android.support.customtabs.ICustomTabsCallback callback, int relation, in android.net.Uri origin, in android.os.Bundle extras) = 8;
  boolean receiveFile(in android.support.customtabs.ICustomTabsCallback callback, in android.net.Uri uri, int purpose, in android.os.Bundle extras) = 11;
  boolean isEngagementSignalsApiAvailable(in android.support.customtabs.ICustomTabsCallback customTabsCallback, in android.os.Bundle extras) = 12;
  boolean setEngagementSignalsCallback(in android.support.customtabs.ICustomTabsCallback customTabsCallback, in IBinder callback, in android.os.Bundle extras) = 13;
  int getGreatestScrollPercentage(in android.support.customtabs.ICustomTabsCallback customTabsCallback, in android.os.Bundle extras) = 14;
}
