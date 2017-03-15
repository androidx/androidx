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

package android.support.v4.app;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.content.IntentCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import java.util.ArrayList;

/**
 * Extra helper functionality for sharing data between activities.
 *
 * ShareCompat provides functionality to extend the {@link Intent#ACTION_SEND}/
 * {@link Intent#ACTION_SEND_MULTIPLE} protocol and support retrieving more info
 * about the activity that invoked a social sharing action.
 *
 * {@link IntentBuilder} provides helper functions for constructing a sharing
 * intent that always includes data about the calling activity and app.
 * This lets the called activity provide attribution for the app that shared
 * content. Constructing an intent this way can be done in a method-chaining style.
 * To obtain an IntentBuilder with info about your calling activity, use the static
 * method {@link IntentBuilder#from(Activity)}.
 *
 * {@link IntentReader} provides helper functions for parsing the defined extras
 * within an {@link Intent#ACTION_SEND} or {@link Intent#ACTION_SEND_MULTIPLE} intent
 * used to launch an activity. You can also obtain a Drawable for the caller's
 * application icon and the application's localized label (the app's human-readable name).
 * Social apps that enable sharing content are encouraged to use this information
 * to call out the app that the content was shared from.
 */
public final class ShareCompat {
    /**
     * Intent extra that stores the name of the calling package for an ACTION_SEND intent.
     * When an activity is started using startActivityForResult this is redundant info.
     * (It is also provided by {@link Activity#getCallingPackage()}.)
     *
     * Instead of using this constant directly, consider using {@link #getCallingPackage(Activity)}
     * or {@link IntentReader#getCallingPackage()}.
     */
    public static final String EXTRA_CALLING_PACKAGE =
            "android.support.v4.app.EXTRA_CALLING_PACKAGE";

    /**
     * Intent extra that stores the {@link ComponentName} of the calling activity for
     * an ACTION_SEND intent.
     */
    public static final String EXTRA_CALLING_ACTIVITY =
            "android.support.v4.app.EXTRA_CALLING_ACTIVITY";

    private static final String HISTORY_FILENAME_PREFIX = ".sharecompat_";

    private ShareCompat() {}

    /**
     * Retrieve the name of the package that launched calledActivity from a share intent.
     * Apps that provide social sharing functionality can use this to provide attribution
     * for the app that shared the content.
     *
     * <p><em>Note:</em> This data may have been provided voluntarily by the calling
     * application. As such it should not be trusted for accuracy in the context of
     * security or verification.</p>
     *
     * @param calledActivity Current activity that was launched to share content
     * @return Name of the calling package
     */
    public static String getCallingPackage(Activity calledActivity) {
        String result = calledActivity.getCallingPackage();
        if (result == null) {
            result = calledActivity.getIntent().getStringExtra(EXTRA_CALLING_PACKAGE);
        }
        return result;
    }

    /**
     * Retrieve the ComponentName of the activity that launched calledActivity from a share intent.
     * Apps that provide social sharing functionality can use this to provide attribution
     * for the app that shared the content.
     *
     * <p><em>Note:</em> This data may have been provided voluntarily by the calling
     * application. As such it should not be trusted for accuracy in the context of
     * security or verification.</p>
     *
     * @param calledActivity Current activity that was launched to share content
     * @return ComponentName of the calling activity
     */
    public static ComponentName getCallingActivity(Activity calledActivity) {
        ComponentName result = calledActivity.getCallingActivity();
        if (result == null) {
            result = calledActivity.getIntent().getParcelableExtra(EXTRA_CALLING_ACTIVITY);
        }
        return result;
    }

    /**
     * Configure a {@link MenuItem} to act as a sharing action.
     *
     * <p>This method will configure a ShareActionProvider to provide a more robust UI
     * for selecting the target of the share. History will be tracked for each calling
     * activity in a file named with the prefix ".sharecompat_" in the application's
     * private data directory. If the application wishes to set this MenuItem to show
     * as an action in the Action Bar it should use {@link MenuItem#setShowAsAction(int)} to request
     * that behavior in addition to calling this method.</p>
     *
     * <p>During the calling activity's lifecycle, if data within the share intent must
     * change the app should change that state in one of several ways:</p>
     * <ul>
     * <li>Call {@link ActivityCompat#invalidateOptionsMenu(Activity)}. If the app uses the
     * Action Bar its menu will be recreated and rebuilt.
     * If not, the activity will receive a call to {@link Activity#onPrepareOptionsMenu(Menu)}
     * the next time the user presses the menu key to open the options menu panel. The activity
     * can then call configureMenuItem again with a new or altered IntentBuilder to reconfigure
     * the share menu item.</li>
     * <li>Keep a reference to the MenuItem object for the share item once it has been created
     * and call configureMenuItem to update the associated sharing intent as needed.</li>
     * </ul>
     *
     * @param item MenuItem to configure for sharing
     * @param shareIntent IntentBuilder with data about the content to share
     */
    public static void configureMenuItem(MenuItem item, IntentBuilder shareIntent) {
        ActionProvider itemProvider = item.getActionProvider();
        ShareActionProvider provider;
        if (!(itemProvider instanceof ShareActionProvider)) {
            provider = new ShareActionProvider(shareIntent.getActivity());
        } else {
            provider = (ShareActionProvider) itemProvider;
        }
        provider.setShareHistoryFileName(HISTORY_FILENAME_PREFIX
                + shareIntent.getActivity().getClass().getName());
        provider.setShareIntent(shareIntent.getIntent());
        item.setActionProvider(provider);

        if (SDK_INT < 16) {
            if (!item.hasSubMenu()) {
                item.setIntent(shareIntent.createChooserIntent());
            }
        }
    }

    /**
     * Configure a menu item to act as a sharing action.
     *
     * @param menu Menu containing the item to use for sharing
     * @param menuItemId ID of the share item within menu
     * @param shareIntent IntentBuilder with data about the content to share
     * @see #configureMenuItem(MenuItem, IntentBuilder)
     */
    public static void configureMenuItem(Menu menu, int menuItemId, IntentBuilder shareIntent) {
        MenuItem item = menu.findItem(menuItemId);
        if (item == null) {
            throw new IllegalArgumentException("Could not find menu item with id " + menuItemId
                    + " in the supplied menu");
        }
        configureMenuItem(item, shareIntent);
    }

    /**
     * IntentBuilder is a helper for constructing {@link Intent#ACTION_SEND} and
     * {@link Intent#ACTION_SEND_MULTIPLE} sharing intents and starting activities
     * to share content. The ComponentName and package name of the calling activity
     * will be included.
     */
    public static class IntentBuilder {
        private Activity mActivity;
        private Intent mIntent;
        private CharSequence mChooserTitle;
        private ArrayList<String> mToAddresses;
        private ArrayList<String> mCcAddresses;
        private ArrayList<String> mBccAddresses;

        private ArrayList<Uri> mStreams;

        /**
         * Create a new IntentBuilder for launching a sharing action from launchingActivity.
         *
         * @param launchingActivity Activity that the share will be launched from
         * @return a new IntentBuilder instance
         */
        public static IntentBuilder from(Activity launchingActivity) {
            return new IntentBuilder(launchingActivity);
        }

        private IntentBuilder(Activity launchingActivity) {
            mActivity = launchingActivity;
            mIntent = new Intent().setAction(Intent.ACTION_SEND);
            mIntent.putExtra(EXTRA_CALLING_PACKAGE, launchingActivity.getPackageName());
            mIntent.putExtra(EXTRA_CALLING_ACTIVITY, launchingActivity.getComponentName());
            mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        /**
         * Retrieve the Intent as configured so far by the IntentBuilder. This Intent
         * is suitable for use in a ShareActionProvider or chooser dialog.
         *
         * <p>To create an intent that will launch the activity chooser so that the user
         * may select a target for the share, see {@link #createChooserIntent()}.
         *
         * @return The current Intent being configured by this builder
         */
        public Intent getIntent() {
            if (mToAddresses != null) {
                combineArrayExtra(Intent.EXTRA_EMAIL, mToAddresses);
                mToAddresses = null;
            }
            if (mCcAddresses != null) {
                combineArrayExtra(Intent.EXTRA_CC, mCcAddresses);
                mCcAddresses = null;
            }
            if (mBccAddresses != null) {
                combineArrayExtra(Intent.EXTRA_BCC, mBccAddresses);
                mBccAddresses = null;
            }

            // Check if we need to change the action.
            boolean needsSendMultiple = mStreams != null && mStreams.size() > 1;
            boolean isSendMultiple = mIntent.getAction().equals(Intent.ACTION_SEND_MULTIPLE);

            if (!needsSendMultiple && isSendMultiple) {
                // Change back to a single send action; place the first stream into the
                // intent for single sharing.
                mIntent.setAction(Intent.ACTION_SEND);
                if (mStreams != null && !mStreams.isEmpty()) {
                    mIntent.putExtra(Intent.EXTRA_STREAM, mStreams.get(0));
                } else {
                    mIntent.removeExtra(Intent.EXTRA_STREAM);
                }
                mStreams = null;
            }

            if (needsSendMultiple && !isSendMultiple) {
                // Change to a multiple send action; place the relevant ArrayList into the
                // intent for multiple sharing.
                mIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                if (mStreams != null && !mStreams.isEmpty()) {
                    mIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mStreams);
                } else {
                    mIntent.removeExtra(Intent.EXTRA_STREAM);
                }
            }

            return mIntent;
        }

        Activity getActivity() {
            return mActivity;
        }

        private void combineArrayExtra(String extra, ArrayList<String> add) {
            String[] currentAddresses = mIntent.getStringArrayExtra(extra);
            int currentLength = currentAddresses != null ? currentAddresses.length : 0;
            String[] finalAddresses = new String[currentLength + add.size()];
            add.toArray(finalAddresses);
            if (currentAddresses != null) {
                System.arraycopy(currentAddresses, 0, finalAddresses, add.size(), currentLength);
            }
            mIntent.putExtra(extra, finalAddresses);
        }

        private void combineArrayExtra(String extra, String[] add) {
            // Add any items still pending
            Intent intent = getIntent();
            String[] old = intent.getStringArrayExtra(extra);
            int oldLength = old != null ? old.length : 0;
            String[] result = new String[oldLength + add.length];
            if (old != null) System.arraycopy(old, 0, result, 0, oldLength);
            System.arraycopy(add, 0, result, oldLength, add.length);
            intent.putExtra(extra, result);
        }

        /**
         * Create an Intent that will launch the standard Android activity chooser,
         * allowing the user to pick what activity/app on the system should handle
         * the share.
         *
         * @return A chooser Intent for the currently configured sharing action
         */
        public Intent createChooserIntent() {
            return Intent.createChooser(getIntent(), mChooserTitle);
        }

        /**
         * Start a chooser activity for the current share intent.
         *
         * <p>Note that under most circumstances you should use
         * {@link ShareCompat#configureMenuItem(MenuItem, IntentBuilder)
         *  ShareCompat.configureMenuItem()} to add a Share item to the menu while
         * presenting a detail view of the content to be shared instead
         * of invoking this directly.</p>
         */
        public void startChooser() {
            mActivity.startActivity(createChooserIntent());
        }

        /**
         * Set the title that will be used for the activity chooser for this share.
         *
         * @param title Title string
         * @return This IntentBuilder for method chaining
         */
        public IntentBuilder setChooserTitle(CharSequence title) {
            mChooserTitle = title;
            return this;
        }

        /**
         * Set the title that will be used for the activity chooser for this share.
         *
         * @param resId Resource ID of the title string to use
         * @return This IntentBuilder for method chaining
         */
        public IntentBuilder setChooserTitle(@StringRes int resId) {
            return setChooserTitle(mActivity.getText(resId));
        }

        /**
         * Set the type of data being shared
         *
         * @param mimeType mimetype of the shared data
         * @return This IntentBuilder for method chaining
         * @see Intent#setType(String)
         */
        public IntentBuilder setType(String mimeType) {
            mIntent.setType(mimeType);
            return this;
        }

        /**
         * Set the literal text data to be sent as part of the share.
         * This may be a styled CharSequence.
         *
         * @param text Text to share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_TEXT
         */
        public IntentBuilder setText(CharSequence text) {
            mIntent.putExtra(Intent.EXTRA_TEXT, text);
            return this;
        }

        /**
         * Set an HTML string to be sent as part of the share.
         * If {@link Intent#EXTRA_TEXT EXTRA_TEXT} has not already been supplied,
         * a styled version of the supplied HTML text will be added as EXTRA_TEXT as
         * parsed by {@link android.text.Html#fromHtml(String) Html.fromHtml}.
         *
         * @param htmlText A string containing HTML markup as a richer version of the text
         *                 provided by EXTRA_TEXT.
         * @return This IntentBuilder for method chaining
         * @see #setText(CharSequence)
         */
        public IntentBuilder setHtmlText(String htmlText) {
            mIntent.putExtra(IntentCompat.EXTRA_HTML_TEXT, htmlText);
            if (!mIntent.hasExtra(Intent.EXTRA_TEXT)) {
                // Supply a default if EXTRA_TEXT isn't set
                setText(Html.fromHtml(htmlText));
            }
            return this;
        }

        /**
         * Set a stream URI to the data that should be shared.
         *
         * <p>This replaces all currently set stream URIs and will produce a single-stream
         * ACTION_SEND intent.</p>
         *
         * @param streamUri URI of the stream to share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_STREAM
         */
        public IntentBuilder setStream(Uri streamUri) {
            if (!mIntent.getAction().equals(Intent.ACTION_SEND)) {
                mIntent.setAction(Intent.ACTION_SEND);
            }
            mStreams = null;
            mIntent.putExtra(Intent.EXTRA_STREAM, streamUri);
            return this;
        }

        /**
         * Add a stream URI to the data that should be shared. If this is not the first
         * stream URI added the final intent constructed will become an ACTION_SEND_MULTIPLE
         * intent. Not all apps will handle both ACTION_SEND and ACTION_SEND_MULTIPLE.
         *
         * @param streamUri URI of the stream to share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_STREAM
         * @see Intent#ACTION_SEND
         * @see Intent#ACTION_SEND_MULTIPLE
         */
        public IntentBuilder addStream(Uri streamUri) {
            Uri currentStream = mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (mStreams == null && currentStream == null) {
                return setStream(streamUri);
            }
            if (mStreams == null) {
                mStreams = new ArrayList<Uri>();
            }
            if (currentStream != null) {
                mIntent.removeExtra(Intent.EXTRA_STREAM);
                mStreams.add(currentStream);
            }
            mStreams.add(streamUri);
            return this;
        }

        /**
         * Set an array of email addresses as recipients of this share.
         * This replaces all current "to" recipients that have been set so far.
         *
         * @param addresses Email addresses to send to
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_EMAIL
         */
        public IntentBuilder setEmailTo(String[] addresses) {
            if (mToAddresses != null) {
                mToAddresses = null;
            }
            mIntent.putExtra(Intent.EXTRA_EMAIL, addresses);
            return this;
        }

        /**
         * Add an email address to be used in the "to" field of the final Intent.
         *
         * @param address Email address to send to
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_EMAIL
         */
        public IntentBuilder addEmailTo(String address) {
            if (mToAddresses == null) {
                mToAddresses = new ArrayList<String>();
            }
            mToAddresses.add(address);
            return this;
        }

        /**
         * Add an array of email addresses to be used in the "to" field of the final Intent.
         *
         * @param addresses Email addresses to send to
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_EMAIL
         */
        public IntentBuilder addEmailTo(String[] addresses) {
            combineArrayExtra(Intent.EXTRA_EMAIL, addresses);
            return this;
        }

        /**
         * Set an array of email addresses to CC on this share.
         * This replaces all current "CC" recipients that have been set so far.
         *
         * @param addresses Email addresses to CC on the share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_CC
         */
        public IntentBuilder setEmailCc(String[] addresses) {
            mIntent.putExtra(Intent.EXTRA_CC, addresses);
            return this;
        }

        /**
         * Add an email address to be used in the "cc" field of the final Intent.
         *
         * @param address Email address to CC
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_CC
         */
        public IntentBuilder addEmailCc(String address) {
            if (mCcAddresses == null) {
                mCcAddresses = new ArrayList<String>();
            }
            mCcAddresses.add(address);
            return this;
        }

        /**
         * Add an array of email addresses to be used in the "cc" field of the final Intent.
         *
         * @param addresses Email addresses to CC
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_CC
         */
        public IntentBuilder addEmailCc(String[] addresses) {
            combineArrayExtra(Intent.EXTRA_CC, addresses);
            return this;
        }

        /**
         * Set an array of email addresses to BCC on this share.
         * This replaces all current "BCC" recipients that have been set so far.
         *
         * @param addresses Email addresses to BCC on the share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_BCC
         */
        public IntentBuilder setEmailBcc(String[] addresses) {
            mIntent.putExtra(Intent.EXTRA_BCC, addresses);
            return this;
        }

        /**
         * Add an email address to be used in the "bcc" field of the final Intent.
         *
         * @param address Email address to BCC
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_BCC
         */
        public IntentBuilder addEmailBcc(String address) {
            if (mBccAddresses == null) {
                mBccAddresses = new ArrayList<String>();
            }
            mBccAddresses.add(address);
            return this;
        }

        /**
         * Add an array of email addresses to be used in the "bcc" field of the final Intent.
         *
         * @param addresses Email addresses to BCC
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_BCC
         */
        public IntentBuilder addEmailBcc(String[] addresses) {
            combineArrayExtra(Intent.EXTRA_BCC, addresses);
            return this;
        }

        /**
         * Set a subject heading for this share; useful for sharing via email.
         *
         * @param subject Subject heading for this share
         * @return This IntentBuilder for method chaining
         * @see Intent#EXTRA_SUBJECT
         */
        public IntentBuilder setSubject(String subject) {
            mIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }
    }

    /**
     * IntentReader is a helper for reading the data contained within a sharing (ACTION_SEND)
     * Intent. It provides methods to parse standard elements included with a share
     * in addition to extra metadata about the app that shared the content.
     *
     * <p>Social sharing apps are encouraged to provide attribution for the app that shared
     * the content. IntentReader offers access to the application label, calling activity info,
     * and application icon of the app that shared the content. This data may have been provided
     * voluntarily by the calling app and should always be displayed to the user before submission
     * for manual verification. The user should be offered the option to omit this information
     * from shared posts if desired.</p>
     *
     * <p>Activities that intend to receive sharing intents should configure an intent-filter
     * to accept {@link Intent#ACTION_SEND} intents ("android.intent.action.SEND") and optionally
     * accept {@link Intent#ACTION_SEND_MULTIPLE} ("android.intent.action.SEND_MULTIPLE") if
     * the activity is equipped to handle multiple data streams.</p>
     */
    public static class IntentReader {
        private static final String TAG = "IntentReader";

        private Activity mActivity;
        private Intent mIntent;
        private String mCallingPackage;
        private ComponentName mCallingActivity;

        private ArrayList<Uri> mStreams;

        /**
         * Get an IntentReader for parsing and interpreting the sharing intent
         * used to start the given activity.
         *
         * @param activity Activity that was started to share content
         * @return IntentReader for parsing sharing data
         */
        public static IntentReader from(Activity activity) {
            return new IntentReader(activity);
        }

        private IntentReader(Activity activity) {
            mActivity = activity;
            mIntent = activity.getIntent();
            mCallingPackage = ShareCompat.getCallingPackage(activity);
            mCallingActivity = ShareCompat.getCallingActivity(activity);
        }

        /**
         * Returns true if the activity this reader was obtained for was
         * started with an {@link Intent#ACTION_SEND} or {@link Intent#ACTION_SEND_MULTIPLE}
         * sharing Intent.
         *
         * @return true if the activity was started with an ACTION_SEND
         *         or ACTION_SEND_MULTIPLE Intent
         */
        public boolean isShareIntent() {
            final String action = mIntent.getAction();
            return Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action);
        }

        /**
         * Returns true if the activity this reader was obtained for was started with an
         * {@link Intent#ACTION_SEND} intent and contains a single shared item.
         * The shared content should be obtained using either the {@link #getText()}
         * or {@link #getStream()} methods depending on the type of content shared.
         *
         * @return true if the activity was started with an ACTION_SEND intent
         */
        public boolean isSingleShare() {
            return Intent.ACTION_SEND.equals(mIntent.getAction());
        }

        /**
         * Returns true if the activity this reader was obtained for was started with an
         * {@link Intent#ACTION_SEND_MULTIPLE} intent. The Intent may contain more than
         * one stream item.
         *
         * @return true if the activity was started with an ACTION_SEND_MULTIPLE intent
         */
        public boolean isMultipleShare() {
            return Intent.ACTION_SEND_MULTIPLE.equals(mIntent.getAction());
        }

        /**
         * Get the mimetype of the data shared to this activity.
         *
         * @return mimetype of the shared data
         * @see Intent#getType()
         */
        public String getType() {
            return mIntent.getType();
        }

        /**
         * Get the literal text shared with the target activity.
         *
         * @return Literal shared text or null if none was supplied
         * @see Intent#EXTRA_TEXT
         */
        public CharSequence getText() {
            return mIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        }

        /**
         * Get the styled HTML text shared with the target activity.
         * If no HTML text was supplied but {@link Intent#EXTRA_TEXT} contained
         * styled text, it will be converted to HTML if possible and returned.
         * If the text provided by {@link Intent#EXTRA_TEXT} was not styled text,
         * it will be escaped by {@link android.text.Html#escapeHtml(CharSequence)}
         * and returned. If no text was provided at all, this method will return null.
         *
         * @return Styled text provided by the sender as HTML.
         */
        public String getHtmlText() {
            String result = mIntent.getStringExtra(IntentCompat.EXTRA_HTML_TEXT);
            if (result == null) {
                CharSequence text = getText();
                if (text instanceof Spanned) {
                    result = Html.toHtml((Spanned) text);
                } else if (text != null) {
                    if (SDK_INT >= 16) {
                        result = Html.escapeHtml(text);
                    } else {
                        StringBuilder out = new StringBuilder();
                        withinStyle(out, text, 0, text.length());
                        result = out.toString();
                    }
                }
            }
            return result;
        }

        private static void withinStyle(StringBuilder out, CharSequence text,
                int start, int end) {
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);

                if (c == '<') {
                    out.append("&lt;");
                } else if (c == '>') {
                    out.append("&gt;");
                } else if (c == '&') {
                    out.append("&amp;");
                } else if (c > 0x7E || c < ' ') {
                    out.append("&#" + ((int) c) + ";");
                } else if (c == ' ') {
                    while (i + 1 < end && text.charAt(i + 1) == ' ') {
                        out.append("&nbsp;");
                        i++;
                    }

                    out.append(' ');
                } else {
                    out.append(c);
                }
            }
        }

        /**
         * Get a URI referring to a data stream shared with the target activity.
         *
         * <p>This call will fail if the share intent contains multiple stream items.
         * If {@link #isMultipleShare()} returns true the application should use
         * {@link #getStream(int)} and {@link #getStreamCount()} to retrieve the
         * included stream items.</p>
         *
         * @return A URI referring to a data stream to be shared or null if one was not supplied
         * @see Intent#EXTRA_STREAM
         */
        public Uri getStream() {
            return mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        /**
         * Get the URI of a stream item shared with the target activity.
         * Index should be in the range [0-getStreamCount()).
         *
         * @param index Index of text item to retrieve
         * @return Requested stream item URI
         * @see Intent#EXTRA_STREAM
         * @see Intent#ACTION_SEND_MULTIPLE
         */
        public Uri getStream(int index) {
            if (mStreams == null && isMultipleShare()) {
                mStreams = mIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
            if (mStreams != null) {
                return mStreams.get(index);
            }
            if (index == 0) {
                return mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
            throw new IndexOutOfBoundsException("Stream items available: " + getStreamCount()
                    + " index requested: " + index);
        }

        /**
         * Return the number of stream items shared. The return value will be 0 or 1 if
         * this was an {@link Intent#ACTION_SEND} intent, or 0 or more if it was an
         * {@link Intent#ACTION_SEND_MULTIPLE} intent.
         *
         * @return Count of text items contained within the Intent
         */
        public int getStreamCount() {
            if (mStreams == null && isMultipleShare()) {
                mStreams = mIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
            if (mStreams != null) {
                return mStreams.size();
            }
            return mIntent.hasExtra(Intent.EXTRA_STREAM) ? 1 : 0;
        }

        /**
         * Get an array of Strings, each an email address to share to.
         *
         * @return An array of email addresses or null if none were supplied.
         * @see Intent#EXTRA_EMAIL
         */
        public String[] getEmailTo() {
            return mIntent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        }

        /**
         * Get an array of Strings, each an email address to CC on this share.
         *
         * @return An array of email addresses or null if none were supplied.
         * @see Intent#EXTRA_CC
         */
        public String[] getEmailCc() {
            return mIntent.getStringArrayExtra(Intent.EXTRA_CC);
        }

        /**
         * Get an array of Strings, each an email address to BCC on this share.
         *
         * @return An array of email addresses or null if none were supplied.
         * @see Intent#EXTRA_BCC
         */
        public String[] getEmailBcc() {
            return mIntent.getStringArrayExtra(Intent.EXTRA_BCC);
        }

        /**
         * Get a subject heading for this share; useful when sharing via email.
         *
         * @return The subject heading for this share or null if one was not supplied.
         * @see Intent#EXTRA_SUBJECT
         */
        public String getSubject() {
            return mIntent.getStringExtra(Intent.EXTRA_SUBJECT);
        }

        /**
         * Get the name of the package that invoked this sharing intent. If the activity
         * was not started for a result, IntentBuilder will read this from extra metadata placed
         * in the Intent by ShareBuilder.
         *
         * <p><em>Note:</em> This data may have been provided voluntarily by the calling
         * application. As such it should not be trusted for accuracy in the context of
         * security or verification.</p>
         *
         * @return Name of the package that started this activity or null if unknown
         * @see Activity#getCallingPackage()
         * @see ShareCompat#EXTRA_CALLING_PACKAGE
         */
        public String getCallingPackage() {
            return mCallingPackage;
        }

        /**
         * Get the {@link ComponentName} of the Activity that invoked this sharing intent.
         * If the target sharing activity was not started for a result, IntentBuilder will read
         * this from extra metadata placed in the intent by ShareBuilder.
         *
         * <p><em>Note:</em> This data may have been provided voluntarily by the calling
         * application. As such it should not be trusted for accuracy in the context of
         * security or verification.</p>
         *
         * @return ComponentName of the calling Activity or null if unknown
         * @see Activity#getCallingActivity()
         * @see ShareCompat#EXTRA_CALLING_ACTIVITY
         */
        public ComponentName getCallingActivity() {
            return mCallingActivity;
        }

        /**
         * Get the icon of the calling activity as a Drawable if data about
         * the calling activity is available.
         *
         * <p><em>Note:</em> This data may have been provided voluntarily by the calling
         * application. As such it should not be trusted for accuracy in the context of
         * security or verification.</p>
         *
         * @return The calling Activity's icon or null if unknown
         */
        public Drawable getCallingActivityIcon() {
            if (mCallingActivity == null) return null;

            PackageManager pm = mActivity.getPackageManager();
            try {
                return pm.getActivityIcon(mCallingActivity);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Could not retrieve icon for calling activity", e);
            }
            return null;
        }

        /**
         * Get the icon of the calling application as a Drawable if data
         * about the calling package is available.
         *
         * <p><em>Note:</em> This data may have been provided voluntarily by the calling
         * application. As such it should not be trusted for accuracy in the context of
         * security or verification.</p>
         *
         * @return The calling application's icon or null if unknown
         */
        public Drawable getCallingApplicationIcon() {
            if (mCallingPackage == null) return null;

            PackageManager pm = mActivity.getPackageManager();
            try {
                return pm.getApplicationIcon(mCallingPackage);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Could not retrieve icon for calling application", e);
            }
            return null;
        }

        /**
         * Get the human-readable label (title) of the calling application if
         * data about the calling package is available.
         *
         * <p><em>Note:</em> This data may have been provided voluntarily by the calling
         * application. As such it should not be trusted for accuracy in the context of
         * security or verification.</p>
         *
         * @return The calling application's label or null if unknown
         */
        public CharSequence getCallingApplicationLabel() {
            if (mCallingPackage == null) return null;

            PackageManager pm = mActivity.getPackageManager();
            try {
                return pm.getApplicationLabel(pm.getApplicationInfo(mCallingPackage, 0));
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Could not retrieve label for calling application", e);
            }
            return null;
        }
    }
}
