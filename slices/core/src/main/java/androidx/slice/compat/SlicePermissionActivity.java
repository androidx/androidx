/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.compat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.BidiFormatter;
import androidx.slice.core.R;

/**
 * Dialog that grants slice permissions for an app.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SlicePermissionActivity extends Activity implements OnClickListener,
        OnDismissListener {

    private static final float MAX_LABEL_SIZE_PX = 500f;

    private static final String TAG = "SlicePermissionActivity";

    private Uri mUri;
    private String mCallingPkg;
    private String mProviderPkg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = getIntent().getParcelableExtra(SliceProviderCompat.EXTRA_BIND_URI);
        mCallingPkg = getIntent().getStringExtra(SliceProviderCompat.EXTRA_PKG);
        mProviderPkg = getIntent().getStringExtra(SliceProviderCompat.EXTRA_PROVIDER_PKG);

        try {
            PackageManager pm = getPackageManager();
            CharSequence app1 = BidiFormatter.getInstance().unicodeWrap(
                    loadSafeLabel(pm, pm.getApplicationInfo(mCallingPkg, 0))
                    .toString());
            CharSequence app2 = BidiFormatter.getInstance().unicodeWrap(
                    loadSafeLabel(pm, pm.getApplicationInfo(mProviderPkg, 0))
                    .toString());
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.abc_slice_permission_title, app1, app2))
                    .setView(R.layout.abc_slice_permission_request)
                    .setNegativeButton(R.string.abc_slice_permission_deny, this)
                    .setPositiveButton(R.string.abc_slice_permission_allow, this)
                    .setOnDismissListener(this)
                    .show();
            TextView t1 = dialog.getWindow().getDecorView().findViewById(R.id.text1);
            t1.setText(getString(R.string.abc_slice_permission_text_1, app2));
            TextView t2 = dialog.getWindow().getDecorView().findViewById(R.id.text2);
            t2.setText(getString(R.string.abc_slice_permission_text_2, app2));
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package", e);
            finish();
        }
    }

    // Based on loadSafeLabel in PackageitemInfo
    private CharSequence loadSafeLabel(PackageManager pm, ApplicationInfo appInfo) {
        // loadLabel() always returns non-null
        String label = appInfo.loadLabel(pm).toString();
        // strip HTML tags to avoid <br> and other tags overwriting original message
        String labelStr = Html.fromHtml(label).toString();

        // If the label contains new line characters it may push the UI
        // down to hide a part of it. Labels shouldn't have new line
        // characters, so just truncate at the first time one is seen.
        final int labelLength = labelStr.length();
        int offset = 0;
        while (offset < labelLength) {
            final int codePoint = labelStr.codePointAt(offset);
            final int type = Character.getType(codePoint);
            if (type == Character.LINE_SEPARATOR
                    || type == Character.CONTROL
                    || type == Character.PARAGRAPH_SEPARATOR) {
                labelStr = labelStr.substring(0, offset);
                break;
            }
            // replace all non-break space to " " in order to be trimmed
            if (type == Character.SPACE_SEPARATOR) {
                labelStr = labelStr.substring(0, offset) + " " + labelStr.substring(offset
                        + Character.charCount(codePoint));
            }
            offset += Character.charCount(codePoint);
        }

        labelStr = labelStr.trim();
        if (labelStr.isEmpty()) {
            return appInfo.packageName;
        }
        TextPaint paint = new TextPaint();
        paint.setTextSize(42);

        return TextUtils.ellipsize(labelStr, paint, MAX_LABEL_SIZE_PX, TextUtils.TruncateAt.END);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            SliceProviderCompat.grantSlicePermission(this, getPackageName(), mCallingPkg,
                    mUri.buildUpon().path("").build());
        }
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
