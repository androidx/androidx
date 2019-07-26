package androidx.ads.identifier.provider;

/**
 * The Advertising ID service used to communicate between an Advertising ID Provider and the
 * developer library.
 *
 * <p>The Advertising ID is a resettable identifier used for ads purpose.
 * @hide
 */
interface IAdvertisingIdService {
    String getId() = 0;
    boolean isLimitAdTrackingEnabled() = 1;
}
