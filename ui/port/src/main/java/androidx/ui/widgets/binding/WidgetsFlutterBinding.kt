package androidx.ui.widgets.binding

// / A concrete binding for applications based on the Widgets framework.
// / This is the glue that binds the framework to the Flutter engine.
class WidgetsFlutterBinding /* extends BindingBase with GestureBinding, ServicesBinding, SchedulerBinding, PaintingBinding, RendererBinding, WidgetsBinding */ {

    companion object {

        // / Returns an instance of the [WidgetsBinding], creating and
        // / initializing it if necessary. If one is created, it will be a
        // / [WidgetsFlutterBinding]. If one was previously initialized, then
        // / it will at least implement [WidgetsBinding].
        // /
        // / You only need to call this method if you need the binding to be
        // / initialized before calling [runApp].
        // /
        // / In the `flutter_test` framework, [testWidgets] initializes the
        // / binding instance to a [TestWidgetsFlutterBinding], not a
        // / [WidgetsFlutterBinding].
        val ensureInitialized: WidgetsBinding by lazy { WidgetsBinding() }
    }
}