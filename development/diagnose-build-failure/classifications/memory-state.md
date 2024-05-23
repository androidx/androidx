If the build passed when disabling the Gradle Daemon it suggests that there is some state in the Gradle daemon that is causing a failure

This suggests that there is some state saved in the Gradle Daemon that is causing a failure.

Some ideas:

  A) Next time you could get a heap dump via jmap

     Try `jmap ${pid}`: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jmap.html

  B) You could look for more examples of this error on build servers using ab-damage-estimator: https://dashboards.corp.google.com/_d7c29bbb_d22c_4d60_833b_98f096f089e7?f=branch:in:aosp-androidx-main&f=day:pd:90
