package androidx.ui.ui.pointer

// /// A sequence of reports about the state of pointers.
class PointerDataPacket(
    // /// Data about the individual pointers in this packet.
    // ///
    // /// This list might contain multiple pieces of data about the same pointer.
    val data: MutableList<PointerData> = mutableListOf()
)