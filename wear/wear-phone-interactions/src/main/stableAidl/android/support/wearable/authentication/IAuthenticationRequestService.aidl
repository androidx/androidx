package android.support.wearable.authentication;

import android.os.Bundle;

import android.support.wearable.authentication.IAuthenticationRequestCallback;

/**
 * Interface of a service that supports an async remote authentication.
 *
 * @hide
 */
interface IAuthenticationRequestService {
  // IMPORTANT NOTE: all methods must be given an explicit transaction id that must never change
  // in the future to remain binary backwards compatible.
  // Next Id: 2

  /**
   * API version number. This should be incremented every time a new method is added.
   */
  const int API_VERSION = 1;

  /**
   * Open the request Url, and send the respond result back to callback when authentication
   * completed.
   *
   * Bundle contents:
   * <ul><li>"requestUrl": the URL of the OAuth request (Uri)
   * <ul><li>"packageName": the package name of the requester (String)
   *
   * @since API version 0
   */
  void openUrl(
    in Bundle request,
    in IAuthenticationRequestCallback authenticationRequestCallback) = 0;

  /**
   * Return the version number of this interface which the client can use to determine which
   * methods are available
   */
  int getApiVersion() = 1;
}
