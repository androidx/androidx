/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.toolbox;

import com.android.volley.AuthFailureError;
import com.android.volley.VolleyLog;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * An Authenticator that uses {@link AccountManager} to get auth
 * tokens of a specified type for a specified account.
 */
public class AndroidAuthenticator implements Authenticator {

    /**
     * AuthTokenListener is notified when an auth token is received (or an error
     * has occured in trying to obtain the auth token).
     */
    public interface AuthTokenListener {
        public void onAuthTokenReceived(String authToken);
        public void onErrorReceived(AuthFailureError error);
    }

    private final Context mContext;
    private final Account mAccount;
    private final String mDefaultAuthTokenType;

    public AndroidAuthenticator(Context context, Account account) {
        this(context, account, null);
    }

    /**
     * Creates a new authenticator.
     * @param context Context for accessing AccountManager
     * @param account Account to authenticate as
     * @param defaultAuthTokenType Auth token type passed to AccountManager
     */
    public AndroidAuthenticator(Context context, Account account, String defaultAuthTokenType) {
        mContext = context;
        mAccount = account;
        mDefaultAuthTokenType = defaultAuthTokenType;
    }

    /**
     * Returns the Account being used by this authenticator.
     */
    public Account getAccount() {
        return mAccount;
    }

    @Override
    public String getAuthToken() throws AuthFailureError {
        if (mDefaultAuthTokenType == null) {
            throw new UnsupportedOperationException("No default auth type.");
        }
        return getAuthToken(mDefaultAuthTokenType);
    }

    @Override
    public String getAuthToken(String authTokenType) throws AuthFailureError {
        return getAuthToken(authTokenType, false);
    }

    public String getAuthToken(String authTokenType, boolean forceReauth)
            throws AuthFailureError {
        if (forceReauth) {
            invalidateCachedToken(authTokenType);
        }
        final AccountManager accountManager = AccountManager.get(mContext);
        AccountManagerFuture<Bundle> future = accountManager.getAuthToken(mAccount,
                authTokenType, false, null, null);
        Bundle result;
        try {
            result = future.getResult();
        } catch (Exception e) {
            throw new AuthFailureError("Error while retrieving auth token", e);
        }
        String authToken = null;
        if (future.isDone() && !future.isCancelled()) {
            if (result.containsKey(AccountManager.KEY_INTENT)) {
                Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
                throw new AuthFailureError(intent);
            }
            authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
        }
        if (authToken == null) {
            throw new AuthFailureError("Got null auth token for type: " + authTokenType);
        }

        return authToken;
    }

    /**
     * Asynchronously retrieves a default auth token and sends it (or an error)
     * to the listener on the main UI thread.
     *
     * @param listener that will be receive the auth token or error
     * @param handler Handler to post the result to
     */
    public void getAuthTokenAsync(final AuthTokenListener listener,
            final Handler handler) {
        getAuthTokenAsync(listener, handler, mDefaultAuthTokenType);
    }


    /**
     * Asynchronously retrieves a default auth token and sends it (or an error)
     * to the listener on the main UI thread.
     *
     * @param listener that will be receive the auth token or error
     * @param handler Handler to post the result to
     * @param forceReauth Whether to forcibly expire the current auth token first.
     */
    public void getAuthTokenAsync(final AuthTokenListener listener,
            final Handler handler, boolean forceReauth) {
        getAuthTokenAsync(listener, handler, mDefaultAuthTokenType, forceReauth);
    }

    /**
     * Asynchronously retrieves an auth token and sends it (or an error) to the
     * listener on the main UI thread.
     *
     * @param listener that will be receive the auth token or error
     * @param handler Handler to post the result to
     * @param authTokenType
     */
    public void getAuthTokenAsync(final AuthTokenListener listener, final Handler handler,
            final String authTokenType) {
        getAuthTokenAsync(listener, handler, authTokenType, false);
    }

    /**
     * Asynchronously retrieves an auth token and sends it (or an error) to the
     * listener on the main UI thread.
     *
     * @param listener that will be receive the auth token or error
     * @param handler Handler to post the result to
     * @param authTokenType
     * @param forceReauth Whether to forcibly expire the current auth token first.
     */
    public void getAuthTokenAsync(final AuthTokenListener listener, final Handler handler,
            final String authTokenType, final boolean forceReauth) {
        if (listener == null) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                Runnable runnable;
                try {
                    final String authToken = getAuthToken(authTokenType, forceReauth);
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            listener.onAuthTokenReceived(authToken);
                        }
                    };
                } catch (final AuthFailureError afe) {
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            listener.onErrorReceived(afe);
                        }
                    };
                } catch (final SecurityException se) {
                    VolleyLog.e("Caught SecurityException: " + se.getMessage());
                    // don't crash on security exception raised by account manager
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            listener.onErrorReceived(new AuthFailureError(se.getMessage()));
                        }
                    };
                }
                // Post the result back on the main thread
                handler.post(runnable);
            }
        }.start();
    }

    private void invalidateCachedToken(String authTokenType) {
        String cachedAuthToken = AccountManager.get(mContext)
                .peekAuthToken(mAccount, authTokenType);
        if (cachedAuthToken != null) {
            invalidateAuthToken(cachedAuthToken);
        }
    }

    @Override
    public void invalidateAuthToken(String authToken) {
        AccountManager.get(mContext).invalidateAuthToken(mAccount.type, authToken);
    }
}
