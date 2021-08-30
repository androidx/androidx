package androidx.core.app.unusedapprestrictions;

/** @hide */
interface IUnusedAppRestrictionsBackportCallback {

 /**
  * This will be called with the results of the
  * IUnusedAppRestrictionsBackportService.isPermissionRevocationEnabledForApp API.
  *
  * @param success false if there was an error while checking if the app is
  * enabled, otherwise true.
  * @param isEnabled true if permission revocation is enabled for the app,
  * otherwise false.
  */
  oneway void onIsPermissionRevocationEnabledForAppResult(
    boolean success, boolean isEnabled
  );
}