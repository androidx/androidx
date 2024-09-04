# Module root

androidx.glance glance-appwidget-multiprocess

# Package androidx.glance.appwidget.multiprocess

This module introduces support for running GlanceAppWidgets from multiple processes,
with the WorkManager multiprocess library ("androidx.work:work-multiprocess").

Developers can define a widget using the `RemoteGlanceAppWidget` class, which allows them
to specify the component name of the RemoteWorkerService it will run in, as well as the
component names of action receivers and RemoteViewsService. These components must then
be set to the same process as the `GlanceAppWidgetReceiver` in the manifest.

This way, developers can choose to provide different widgets from different processes.
