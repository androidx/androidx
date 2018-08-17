# Remote Callbacks

Remote callbacks provide a wrapper that makes it easier for developers to
provide a PendingIntent. Generally those writing widgets, notifications, and
more recently slices, have the fun of writing code that looks like this
relatively frequently.

```java
public class MyReceiver extends BroadcastReceiver {
  final String ACTION_MY_CALLBACK_ACTION = "...";
  final String EXTRA_PARAM_1 = "...";
  final String EXTRA_PARAM_2 = "...";

  public PendingIntent getPendingIntent(Context context, int value1, int value2) {
    Intent intent = new Intent(context, MyReceiver.class);
    intent.setaction(ACTION_MY_CALLBACK_ACTION);
    intent.putExtra(EXTRA_PARAM_1, value1);
    intent.putExtra(EXTRA_PARAM_2, value2);
    intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
    return PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public void onReceive(Context context, Intent intent) {
    if (ACTION_MY_CALLBACK_ACTION.equals(intent.getAction())) {
      int param1 = intent.getIntExtra(EXTRA_PARAM_1, 0);
      int param2 = intent.getintExtra(EXTRA_PARAM_2, 0);
      doMyAction(param1, param2);
    }
  }

  public void doMyAction(int value1, int value2) {
    ...
  }
}
```

The goal of Remote Callbacks is to remove as much of that fun as possible
and let you get right down to business. Which looks like this much abbreviated
version.

```java
public class MyReceiver extends BroadcastReceiverWithCallbacks<MyReceiver> {
  public PendingIntent getPendingIntent(Context context, int value1, int value2) {
    return createRemoteCallback(context).doMyAction(value1, value2)
        .toPendingIntent();
  }

  @RemoteCallable
  public RemoteCallback doMyAction(int value1, int value2) {
    ...
    return RemoteCallback.LOCAL;
  }
}
```

See CallbackReceiver and its linked documentation for more API details.
