package androidx.core.app.unusedapprestrictions;

import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;

/** @hide */
interface IUnusedAppRestrictionsBackportService {

  /**
   * Checks whether permission revocation is enabled for the calling application.
   *
   * <p>This API is only intended to work for the backported version of
   * permission revocation running on Android M-Q and will not work for Android
   * R+ versions of permission revocation. Only the Verifier on the device can implement this,
   * as that is the component responsible for auto-revoking permissions on M-Q devices.
   *
   * @param callback An IUnusedAppRestrictionsBackportCallback object that will
   * be called with the results of this API
   */
  oneway void isPermissionRevocationEnabledForApp(in IUnusedAppRestrictionsBackportCallback callback);
}