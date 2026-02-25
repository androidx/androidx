!!! ERROR
    This is work in progress and subject to modifications.

RemoteCompose is a standalone content format for Android. It consists
of a list of operations providing rendering, layout, animation, expression evaluation and bindings.

Wire Format overview
======   

The RemoteCompose wire format is a simple flat list of Operations,
serialized in a binary format (see next sections for a detailed description of each operation).

!!! WARNING
    This wire format is intended to be used for fast generation and transport. For display purposes,
    a more runtime-appropriate representation (i.e. a tree of operations and containers) should be created and used instead.

Each list of operations contains as first element a Header (see section [Header]).

            |Operations|
            |:----:|
            | **Header** |
            | *Operation 1* |
            | *Operation 2* |
            | *Operation 3* |
            | ... |
            | *Operation n* |

Encoding
--------

Operations are encoded in binary, using the following types:

    | Type | Size (in bytes) |
    |:----:|:----:|
    | BYTE | 1 |
    | BOOLEAN | 1 |
    | SHORT | 2 |
    | INT | 4 |
    | FLOAT | 4 |
    | LONG | 8 |
    | DOUBLE | 8 |
    | BUFFER | 4 + Size |
    | UTF8 | BUFFER |

A Buffer is simply encoded as the size of the buffer in bytes (encoded as an INT, so 4 bytes)
followed by the byte buffer itself. UTF8 payload is simply encoded as a byte Buffer using encodeToByteArray().



Components & Layout
-------------------

Operations can further be grouped in components, surrounded by ComponentStart (section [ComponentStart])
and ComponentEnd (section [ComponentEnd]).

            |Operations|
            |:----:|
            | ... |
            | **ComponentStart** |
            | *Operation 1* |
            | *Operation 2* |
            | *Operation 3* |
            | ... |
            | *Operation n* |
            | **ComponentEnd** |
            | ... |

More specialized components can be created by using alternative layout operations instead of ComponentStart:

- RootLayoutComponent
- BoxLayout
- RowLayout
- ColumnLayout

Layout managers such as BoxLayout, RowLayout, ColumnLayout provide a way to layout Components.
Such layoutable/resizable components are structured slightly differently:

            |Operations| | |
            |:----: |:----:| :----: |
            | **Header** | ||
            | ... | ... | ... | 
            | **RootLayoutComponent** | | |
            | | **RowLayout** | |
            | ... | ... | ... | 
            | | **RowLayout** | |
            | | *Modifier 1* | |
            | | *Modifier 2* | |
            | ... | ... | ... | 
            | | *Modifier n* | |
            | | | **LayoutComponentContent** |
            | | | *Component 1* |
            | | | *Component 2* |
            | | | ... |
            | | | *Component n* |
            | | | **ComponentEnd** |
            | | **ComponentEnd** | |
            | **ComponentEnd** | | |


In the example above, you can see that RowLayout is comprised of two sections -- a list of Modifier operations followed by a LayoutComponentContent component,
containing the components to be laid out.

