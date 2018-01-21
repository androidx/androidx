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

package androidx.app.slice.compat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.widget.TextView;

import androidx.app.slice.core.R;

/**
 * Dialog that grants slice permissions for an app.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SlicePermissionActivity extends Activity implements OnClickListener,
        OnDismissListener {

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
            CharSequence app1 = pm.getApplicationInfo(mCallingPkg, 0).loadLabel(pm);
            CharSequence app2 = pm.getApplicationInfo(mProviderPkg, 0).loadLabel(pm);
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            grantUriPermission(mCallingPkg, mUri.buildUpon().path("").build(),
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            getContentResolver().notifyChange(mUri, null);
        }
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
