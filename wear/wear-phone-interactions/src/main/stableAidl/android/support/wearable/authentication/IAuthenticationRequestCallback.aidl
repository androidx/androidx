package android.support.wearable.authentication;

import android.os.Bundle;

/**
 * Interface for defining the callback to be notified when an aync remote authentication request
 * completes.
 *
 * @hide
 */
interface IAuthenticationRequestCallback {
  // IMPORTANT NOTE: All methods must be given an explicit transcation id that must never change
  // in the future to remain binary backwards compatible
  // Next Id: 2

  /**
   * API version number. This should be incremented every time a new method is added.
   */
  const int API_VERSION = 1;

  /**
   * Called when an aync authentication request is completed.
   *
   * Bundle contents:
   * <ul><li>"responseUrl": the response URL from the remote auth request (Uri)
   * <ul><li>"error": an error code explaining the request result status (int)
   *
   * @since API version 0
   */
  void onResult(in Bundle result) = 0;

  /**
   * Return the version number of this interface which the client can use to determine which
   * methods are available.
   *
   * @since API version 1
   */
  int getApiVersion() = 1;
}
