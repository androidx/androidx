/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation.testapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.navigation.Navigation;

/**
 * Fragment used to show how to deep link to a destination
 */
public class AndroidFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.android_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tv = view.findViewById(R.id.text);
        tv.setText(getArguments().getString("myarg"));

        Button b = view.findViewById(R.id.send_notification);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editArgs = view.findViewById(R.id.edit_args);
                Bundle args = new Bundle();
                args.putString("myarg", editArgs.getText().toString());
                PendingIntent deeplink = Navigation.findNavController(v).createDeepLink()
                        .setDestination(R.id.android)
                        .setArguments(args)
                        .createPendingIntent();
                NotificationManager notificationManager = (NotificationManager)
                        requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationManager.createNotificationChannel(new NotificationChannel(
                            "deeplink", "Deep Links", NotificationManager.IMPORTANCE_HIGH));
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        requireContext(), "deeplink")
                        .setContentTitle("Navigation")
                        .setContentText("Deep link to Android")
                        .setSmallIcon(R.drawable.ic_android)
                        .setContentIntent(deeplink)
                        .setAutoCancel(true);
                notificationManager.notify(0, builder.build());
            }
        });
    }
}
