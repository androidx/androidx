# Module root

androidx.customview customview-poolingcontainer

# Package androidx.customview.poolingcontainer

This library exists for two primary consumers:
 1. Implementers of containers that manage child views outside of the View hierarchy, such as `RecyclerView`
 2. Views that have expensive-to-recreate resources that will not be cleaned up when the View is garbage-collected, such as `ComposeView`.

Containers should set the `isPoolingContainer` property on themself to `true`, and obey the contract described on that property.

Views that want to observe events from pooling containers should use the `addPoolingContainerListener` method to add a listener to themselves.
