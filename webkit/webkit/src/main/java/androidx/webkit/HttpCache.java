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

package androidx.webkit;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;

import org.chromium.support_lib_boundary.HttpCacheBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * HttpCache manages the configuration of the HTTP cache for a {@link Profile}.
 * <p>
 * Use {@link Profile#getHttpCache()} to obtain the instance for a specific profile.
 */
public final class HttpCache {
    private final @NonNull HttpCacheBoundaryInterface mHttpCacheImpl;
    private final @NonNull String mProfileName;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HttpCache(
            @NonNull String profileName,
            @NonNull HttpCacheBoundaryInterface httpCacheImpl) {
        mHttpCacheImpl = httpCacheImpl;
        mProfileName = profileName;
    }

    /**
     * Returns the default HTTP cache quota in bytes.
     *
     * This is the cache quota (in bytes) that would be allocated to the associated profile if
     * using WebView's automatic cache quota sizing.
     * <p>
     * The default quota may not be a stable size across sessions.
     * <p>
     * If you wish to use automatic quota management, call {@link #useDefaultQuota()} instead of
     * supplying the output of this method to {@link #setQuotaBytes(long)}.
     */
    @UiThread
    @IntRange(from = 0)
    public long getDefaultQuotaBytes() {
        return mHttpCacheImpl.getDefaultQuotaBytes();
    }

    /**
     * Returns whether the cache quota is being automatically managed by WebView.
     *
     * @see #useDefaultQuota()
     */
    @UiThread
    public boolean isUsingDefaultQuota() {
        return mHttpCacheImpl.isUsingDefaultQuota();
    }

    /**
     * Use WebView's default automatic cache quota sizing.
     * <p>
     * The default quota may not be a stable size across sessions.
     * <p>
     * This will take immediate effect and forget any previously configured manual quota.
     * <p>
     * If a call to this method decreases the cache quota, it may trigger background cache
     * evictions, which could have performance impacts. This method does not wait for evictions to
     * complete.
     * <p>
     * You should only use this API on a Profile that your code owns. Setting or unsetting a quota
     * on a Profile (particularly the Default Profile) that is shared by multiple libraries or
     * activities may result in degradation of performance or overuse of disk space.
     */
    @UiThread
    public void useDefaultQuota() {
        mHttpCacheImpl.useDefaultQuota();
    }

    /**
     * Returns the current cache quota in bytes.
     * <p>
     * This is the cache quota that is actually in effect for the associated profile, which may
     * either be an automatic default value or a value previously set via
     * {@link #setQuotaBytes(long)} (with possible adjustments).
     */
    @UiThread
    @IntRange(from = 0)
    public long getQuotaBytes() {
        return mHttpCacheImpl.getQuotaBytes();
    }

    /**
     * Sets the HTTP cache quota in bytes.
     * <p>
     * This overrides the automatic default.
     * <p>
     * A value greater than or equal to zero must be given. WebView may further enforce
     * minimum/maximum constraints on the quota size, and values outside of this range will be
     * automatically clamped. {@link #getQuotaBytes()} will reflect such quota adjustments.
     * <p>
     * Passing a value of zero will neither disable nor fully clear the cache, but will only reduce
     * its quota to the smallest possible value.
     * <p>
     * A cache quota will take immediate effect, persist across restarts, and will not
     * automatically expire.
     * <p>
     * If a call to this method decreases the cache quota, it may trigger background cache
     * evictions, which could have performance impacts. This method does not wait for evictions to
     * complete.
     * <p>
     * You should only use this API on a Profile that your code owns. Setting or unsetting a quota
     * on a Profile (particularly the Default Profile) that is shared by multiple libraries or
     * activities may result in degradation of performance or overuse of disk space.
     * <p>
     * If you wish to use automatic quota management, call {@link #useDefaultQuota()} instead of
     * supplying the output of {@link #getDefaultQuotaBytes()} method to this method.
     */
    @UiThread
    public void setQuotaBytes(@IntRange(from = 0) long quotaInBytes) {
        if (quotaInBytes < 0) {
            throw new IllegalArgumentException("Quota must be non-negative");
        }
        mHttpCacheImpl.setQuotaBytes(quotaInBytes);
    }

    /**
     * Compares this HttpCache with the specified object for equality.
     * <p>
     * Two {@link HttpCache} instances are considered equal if they are associated with the same
     * {@link Profile} (i.e., they have the same profile name).
     *
     * @param obj the object to compare with.
     * @return {@code true} if the objects are equal; {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HttpCache)) {
            return false;
        }
        HttpCache other = (HttpCache) obj;
        return this.mProfileName.equals(other.mProfileName);
    }

    /**
     * Returns a hash code value for the object, consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return mProfileName.hashCode();
    }

}
