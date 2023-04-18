/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl;
/* @hide */
interface IExerciseApiService {
  int getApiVersion() = 0;
  void prepareExercise(in androidx.health.services.client.impl.request.PrepareExerciseRequest prepareExerciseRequest, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 14;
  void startExercise(in androidx.health.services.client.impl.request.StartExerciseRequest startExerciseRequest, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 1;
  void pauseExercise(in String packageName, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 2;
  void resumeExercise(in String packageName, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 3;
  void endExercise(in String packageName, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 4;
  void markLap(in String packageName, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 5;
  void getCurrentExerciseInfo(in String packageName, androidx.health.services.client.impl.internal.IExerciseInfoCallback exerciseInfoCallback) = 6;
  void setUpdateListener(in String packageName, in androidx.health.services.client.impl.IExerciseUpdateListener listener, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 7;
  void clearUpdateListener(in String packageName, in androidx.health.services.client.impl.IExerciseUpdateListener listener, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 8;
  void addGoalToActiveExercise(in androidx.health.services.client.impl.request.ExerciseGoalRequest request, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 9;
  void removeGoalFromActiveExercise(in androidx.health.services.client.impl.request.ExerciseGoalRequest request, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 13;
  void overrideAutoPauseAndResumeForActiveExercise(in androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest request, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 10;
  void overrideBatchingModesForActiveExercise(in androidx.health.services.client.impl.request.BatchingModeConfigRequest request, androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 17;
  androidx.health.services.client.impl.response.ExerciseCapabilitiesResponse getCapabilities(in androidx.health.services.client.impl.request.CapabilitiesRequest request) = 11;
  void flushExercise(in androidx.health.services.client.impl.request.FlushRequest request, in androidx.health.services.client.impl.internal.IStatusCallback statusCallback) = 12;
  void updateExerciseTypeConfigForActiveExercise(in androidx.health.services.client.impl.request.UpdateExerciseTypeConfigRequest updateExerciseTypeConfigRequest, androidx.health.services.client.impl.internal.IStatusCallback statuscallback) = 16;
  const int API_VERSION = 4;
}
