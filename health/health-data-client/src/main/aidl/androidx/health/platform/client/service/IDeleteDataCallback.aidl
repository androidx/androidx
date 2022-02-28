package androidx.health.platform.client.service;

import androidx.health.platform.client.error.ErrorStatus;

oneway interface IDeleteDataCallback {
  void onSuccess() = 0;
  void onError(in ErrorStatus status) = 1;
}