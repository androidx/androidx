# Focus Scroll with Item Decorations

## 1. Goal
Ensure focus scrolling in `RecyclerView` includes item decorations when the
`fixFocusScrollingOffsets` platform flag is active (API >= 37.1).

### Goals
1. Automatically include item decoration offsets during focus scrolls on API >= 37.1.
2. Keep the target rectangle child-relative (LayoutManager API rule).
3. Keep coordinates correct for nested views.
4. Avoid mutating the input `Rect` parameter in-place.

### Non-Goals
* Change the platform's `fixFocusScrollingOffsets` implementation.
* Change focus scrolling behavior on API < 37.1.

---

## 2. Design

### The Problem
On API >= 37.1, focus gain triggers two scroll requests:
1. `ViewGroup#requestChildFocus`: `RecyclerView` intercepts this,
   adds item decoration offsets, and scrolls.
2. `View#handleFocusGainInternal`: The platform calls `requestChildRectangleOnScreen`
   with the view's un-decorated drawing bounds. This second call overrides
   the first, scrolling the decorations off-screen.

### Solution
We intercept `requestChildRectangleOnScreen` on API >= 37.1 and expand
the rectangle to include decoration offsets.

```java
@Override
public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
    if (SdkFullVersionCompat.isAtLeastCinnamonBunMinor1()) {
        mTempRectFocusScroll.set(rect);
        adjustRectForDecorationInsets(child, mTempRectFocusScroll);
        return mLayout.requestChildRectangleOnScreen(
                this, child, mTempRectFocusScroll, immediate);
    }
    return mLayout.requestChildRectangleOnScreen(this, child, rect, immediate);
}
```

#### Targeted Expansion
To avoid expanding smaller items like text cursors, we only expand
sides of the `Rect` that align with the child view's edges
(`left <= 0`, `right >= width`, `top <= 0`, `bottom >= height`).

#### Key Decisions
1. **No In-place Mutation**: The input `rect` is mutable. We copy it to
   `mTempRectFocusScroll` to prevent side-effects or regressions in callers
   and custom LayoutManagers.
2. **API Gating**: We only apply this on API >= 37.1 to avoid behavior
   changes on older versions.
3. **Child-relative Coordinates**: Keeping the rectangle child-relative
   ensures nested views scroll correctly.

---

## 3. Rejected Alternatives

### 1. Expand unconditionally
* **Why Rejected**: Changes default behavior on older platforms, violating
  backward compatibility.

### 2. Mutate the Rect parameter in-place
* **Why Rejected**: Modifying the caller's `Rect` reference causes layout
  regressions and side-effects.

### 3. Translate coordinates to parent-relative
* **Why Rejected**: Violates LayoutManager API contract (rect must be
  child-relative) and breaks nested scroll calculations.

---

## 4. Code Links

* [RecyclerView.java](../src/main/java/androidx/recyclerview/widget/RecyclerView.java)
* [RecyclerViewLayoutTest.java][test_file]

[test_file]: ../src/androidTest/java/androidx/recyclerview/widget/RecyclerViewLayoutTest.java
