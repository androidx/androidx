/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.projected;

import androidx.xr.scenecore.projected.IProjectedNode;
import androidx.xr.scenecore.projected.ISceneResultCallback;
import androidx.xr.scenecore.projected.ISpatialStateChangedCallback;
import androidx.xr.scenecore.projected.ProjectedNodeTransaction;
import java.util.List;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)")
interface IProjectedSceneCoreService {
  IProjectedNode createNode();
  oneway void attachSpatialScene(IProjectedNode sceneNode, ISceneResultCallback resultCallback);
  oneway void setSpatialStateChangedCallback(ISpatialStateChangedCallback callback);
  oneway void clearSpatialStateChangedCallback();
  oneway void applyNodeTransactions(in List<ProjectedNodeTransaction> transactions);
}