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

package android.support.v4.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Helper to register for and send broadcasts of Intents to local objects
 * within your process.  This has a number of advantages over sending
 * global broadcasts with {@link android.content.Context#sendBroadcast}:
 * <ul>
 * <li> You know that the data you are broadcasting won't leave your app, so
 * don't need to worry about leaking private data.
 * <li> It is not possible for other applications to send these broadcasts to
 * your app, so you don't need to worry about having security holes they can
 * exploit.
 * <li> It is more efficient than sending a global broadcast through the
 * system.
 * </ul>
 */
public final class LocalBroadcastManager {
    private static final class ReceiverRecord {
        final IntentFilter filter;
        final BroadcastReceiver receiver;
        boolean broadcasting;
        boolean dead;

        ReceiverRecord(IntentFilter _filter, BroadcastReceiver _receiver) {
            filter = _filter;
            receiver = _receiver;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("Receiver{");
            builder.append(receiver);
            builder.append(" filter=");
            builder.append(filter);
            if (dead) {
                builder.append(" DEAD");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    private static final class BroadcastRecord {
        final Intent intent;
        final ArrayList<ReceiverRecord> receivers;

        BroadcastRecord(Intent _intent, ArrayList<ReceiverRecord> _receivers) {
            intent = _intent;
            receivers = _receivers;
        }
    }

    private static final String TAG = "LocalBroadcastManager";
    private static final boolean DEBUG = false;

    private final Context mAppContext;

    private final HashMap<BroadcastReceiver, ArrayList<ReceiverRecord>> mReceivers
            = new HashMap<>();
    private final HashMap<String, ArrayList<ReceiverRecord>> mActions = new HashMap<>();

    private final ArrayList<BroadcastRecord> mPendingBroadcasts = new ArrayList<>();

    static final int MSG_EXEC_PENDING_BROADCASTS = 1;

    private final Handler mHandler;

    private static final Object mLock = new Object();
    private static LocalBroadcastManager mInstance;

    public static LocalBroadcastManager getInstance(Context context) {
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new LocalBroadcastManager(context.getApplicationContext());
            }
            return mInstance;
        }
    }

    private LocalBroadcastManager(Context context) {
        mAppContext = context;
        mHandler = new Handler(context.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_EXEC_PENDING_BROADCASTS:
                        executePendingBroadcasts();
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };
    }

    /**
     * Register a receive for any local broadcasts that match the given IntentFilter.
     *
     * @param receiver The BroadcastReceiver to handle the broadcast.
     * @param filter Selects the Intent broadcasts to be received.
     *
     * @see #unregisterReceiver
     */
    public void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        synchronized (mReceivers) {
            ReceiverRecord entry = new ReceiverRecord(filter, receiver);
            ArrayList<ReceiverRecord> filters = mReceivers.get(receiver);
            if (filters == null) {
                filters = new ArrayList<>(1);
                mReceivers.put(receiver, filters);
            }
            filters.add(entry);
            for (int i=0; i<filter.countActions(); i++) {
                String action = filter.getAction(i);
                ArrayList<ReceiverRecord> entries = mActions.get(action);
                if (entries == null) {
                    entries = new ArrayList<ReceiverRecord>(1);
                    mActions.put(action, entries);
                }
                entries.add(entry);
            }
        }
    }

    /**
     * Unregister a previously registered BroadcastReceiver.  <em>All</em>
     * filters that have been registered for this BroadcastReceiver will be
     * removed.
     *
     * @param receiver The BroadcastReceiver to unregister.
     *
     * @see #registerReceiver
     */
    public void unregisterReceiver(BroadcastReceiver receiver) {
        synchronized (mReceivers) {
            final ArrayList<ReceiverRecord> filters = mReceivers.remove(receiver);
            if (filters == null) {
                return;
            }
            for (int i=filters.size()-1; i>=0; i--) {
                final ReceiverRecord filter = filters.get(i);
                filter.dead = true;
                for (int j=0; j<filter.filter.countActions(); j++) {
                    final String action = filter.filter.getAction(j);
                    final ArrayList<ReceiverRecord> receivers = mActions.get(action);
                    if (receivers != null) {
                        for (int k=receivers.size()-1; k>=0; k--) {
                            final ReceiverRecord rec = receivers.get(k);
                            if (rec.receiver == receiver) {
                                rec.dead = true;
                                receivers.remove(k);
                            }
                        }
                        if (receivers.size() <= 0) {
                            mActions.remove(action);
                        }
                    }
                }
            }
        }
    }

    /**
     * Broadcast the given intent to all interested BroadcastReceivers.  This
     * call is asynchronous; it returns immediately, and you will continue
     * executing while the receivers are run.
     *
     * @param intent The Intent to broadcast; all receivers matching this
     *     Intent will receive the broadcast.
     *
     * @see #registerReceiver
     *
     * @return Returns true if the intent has been scheduled for delivery to one or more
     * broadcast receivers.  (Note tha delivery may not ultimately take place if one of those
     * receivers is unregistered before it is dispatched.)
     */
    public boolean sendBroadcast(Intent intent) {
        synchronized (mReceivers) {
            final String action = intent.getAction();
            final String type = intent.resolveTypeIfNeeded(
                    mAppContext.getContentResolver());
            final Uri data = intent.getData();
            final String scheme = intent.getScheme();
            final Set<String> categories = intent.getCategories();

            final boolean debug = DEBUG ||
                    ((intent.getFlags() & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0);
            if (debug) Log.v(
                    TAG, "Resolving type " + type + " scheme " + scheme
                    + " of intent " + intent);

            ArrayList<ReceiverRecord> entries = mActions.get(intent.getAction());
            if (entries != null) {
                if (debug) Log.v(TAG, "Action list: " + entries);

                ArrayList<ReceiverRecord> receivers = null;
                for (int i=0; i<entries.size(); i++) {
                    ReceiverRecord receiver = entries.get(i);
                    if (debug) Log.v(TAG, "Matching against filter " + receiver.filter);

                    if (receiver.broadcasting) {
                        if (debug) {
                            Log.v(TAG, "  Filter's target already added");
                        }
                        continue;
                    }

                    int match = receiver.filter.match(action, type, scheme, data,
                            categories, "LocalBroadcastManager");
                    if (match >= 0) {
                        if (debug) Log.v(TAG, "  Filter matched!  match=0x" +
                                Integer.toHexString(match));
                        if (receivers == null) {
                            receivers = new ArrayList<ReceiverRecord>();
                        }
                        receivers.add(receiver);
                        receiver.broadcasting = true;
                    } else {
                        if (debug) {
                            String reason;
                            switch (match) {
                                case IntentFilter.NO_MATCH_ACTION: reason = "action"; break;
                                case IntentFilter.NO_MATCH_CATEGORY: reason = "category"; break;
                                case IntentFilter.NO_MATCH_DATA: reason = "data"; break;
                                case IntentFilter.NO_MATCH_TYPE: reason = "type"; break;
                                default: reason = "unknown reason"; break;
                            }
                            Log.v(TAG, "  Filter did not match: " + reason);
                        }
                    }
                }

                if (receivers != null) {
                    for (int i=0; i<receivers.size(); i++) {
                        receivers.get(i).broadcasting = false;
                    }
                    mPendingBroadcasts.add(new BroadcastRecord(intent, receivers));
                    if (!mHandler.hasMessages(MSG_EXEC_PENDING_BROADCASTS)) {
                        mHandler.sendEmptyMessage(MSG_EXEC_PENDING_BROADCASTS);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Like {@link #sendBroadcast(Intent)}, but if there are any receivers for
     * the Intent this function will block and immediately dispatch them before
     * returning.
     */
    public void sendBroadcastSync(Intent intent) {
        if (sendBroadcast(intent)) {
            executePendingBroadcasts();
        }
    }

    private void executePendingBroadcasts() {
        while (true) {
            final BroadcastRecord[] brs;
            synchronized (mReceivers) {
                final int N = mPendingBroadcasts.size();
                if (N <= 0) {
                    return;
                }
                brs = new BroadcastRecord[N];
                mPendingBroadcasts.toArray(brs);
                mPendingBroadcasts.clear();
            }
            for (int i=0; i<brs.length; i++) {
                final BroadcastRecord br = brs[i];
                final int nbr = br.receivers.size();
                for (int j=0; j<nbr; j++) {
                    final ReceiverRecord rec = br.receivers.get(j);
                    if (!rec.dead) {
                        rec.receiver.onReceive(mAppContext, br.intent);
                    }
                }
            }
        }
    }
}
