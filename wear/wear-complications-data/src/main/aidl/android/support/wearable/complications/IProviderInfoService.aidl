package android.support.wearable.complications;

import android.content.ComponentName;
import android.support.wearable.complications.ComplicationProviderInfo;

/**
 * Interface for a service that allows provider info to be retrieved from the system.
 *
 * @hide
 */
interface IProviderInfoService {
    /**
     * Returns provider info for each of the provided ids, in an array in the same order as the ids.
     * For each id, if no provider is configured then null will be added at that index of the
     * result.
     */
    ComplicationProviderInfo[] getProviderInfos(
        in ComponentName watchFaceComponent, in int[] ids) = 0;
}
