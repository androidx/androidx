/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.service;

import androidx.health.platform.client.permission.Permission;
import androidx.health.platform.client.request.AggregateDataRequest;
import androidx.health.platform.client.request.DeleteDataRequest;
import androidx.health.platform.client.request.DeleteDataRangeRequest;
import androidx.health.platform.client.request.UpsertDataRequest;
import androidx.health.platform.client.request.ReadDataRequest;
import androidx.health.platform.client.request.ReadDataRangeRequest;
import androidx.health.platform.client.request.RequestContext;
import androidx.health.platform.client.service.IGetGrantedPermissionsCallback;
import androidx.health.platform.client.service.IDeleteDataCallback;
import androidx.health.platform.client.service.IDeleteDataRangeCallback;
import androidx.health.platform.client.service.IReadDataRangeCallback;
import androidx.health.platform.client.service.IUpdateDataCallback;
import androidx.health.platform.client.service.IInsertDataCallback;
import androidx.health.platform.client.service.IReadDataCallback;
import androidx.health.platform.client.service.IRevokeAllPermissionsCallback;
import androidx.health.platform.client.service.IAggregateDataCallback;

interface IHealthDataService {
  /**
   * API version of the AIDL interface. Should be incremented every time a new
   * method is added.
   */
  const int CURRENT_API_VERSION = 1;

  const int MIN_API_VERSION = 1;

  // Next Id: 16

  /**
   * Returns version of this AIDL interface.
   *
   * <p> Can be used by client to detect version of the API on the service
   * side. Should always return CURRENT_API_VERSION.
   */
  int getApiVersion() = 0;

  void getGrantedPermissions(in RequestContext context, in List<Permission> permissions, in IGetGrantedPermissionsCallback callback) = 3;

  void revokeAllPermissions(in RequestContext context, in IRevokeAllPermissionsCallback callback) = 8;

  void insertData(in RequestContext context, in UpsertDataRequest request, in IInsertDataCallback callback) = 9;

  void deleteData(in RequestContext context, in DeleteDataRequest request, in IDeleteDataCallback callback) = 10;

  void deleteDataRange(in RequestContext context, in DeleteDataRangeRequest request, in IDeleteDataRangeCallback callback) = 13;

  void readData(in RequestContext context, in ReadDataRequest request, in IReadDataCallback callback) = 11;

  void readDataRange(in RequestContext context, in ReadDataRangeRequest request, in IReadDataRangeCallback callback) = 15;

  void updateData(in RequestContext context, in UpsertDataRequest request, in IUpdateDataCallback callback) = 12;

  void aggregate(in RequestContext context, in AggregateDataRequest request, in IAggregateDataCallback callback) = 14;
}
