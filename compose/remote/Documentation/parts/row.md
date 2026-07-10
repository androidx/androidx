### Row Layout Algorithm

The Row Layout manager positions child components horizontally one after another. It supports intrinsic sizing, weight-based distribution, and various horizontal and vertical alignment strategies.

#### Visual Illustration (Weights)
<div id="rowLayoutContainer">
  <canvas id="rowLayoutCanvas_v1" width="500" height="200" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('rowLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 200);

    var startX = 25, startY = 60, rowH = 80;
    var minW = 150, maxW = 450;
    var totalW = minW + (Math.sin(phase) + 1) / 2 * (maxW - minW);
    phase += 0.02;

    // Row Bounds
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(startX, startY, totalW, rowH);
    ctx.setLineDash([]);

    // Items
    // Item 1: Fixed
    var w1 = 80;
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(startX, startY, w1, rowH);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.strokeRect(startX, startY, w1, rowH);
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 14px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Fixed (80px)', startX + w1/2, startY + rowH/2 + 5);

    // Remaining width distribution
    var remainingW = Math.max(0, totalW - w1);

    // Item 2: Weight 1
    var w2 = remainingW * (1/3);
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(startX + w1, startY, w2, rowH);
    ctx.strokeRect(startX + w1, startY, w2, rowH);
    ctx.fillStyle = BLUE;
    if (w2 > 50) {
      var p2 = Math.round(w2 / totalW * 100);
      ctx.fillText('Weight 1 (' + p2 + '%)', startX + w1 + w2/2, startY + rowH/2 + 5);
    }

    // Item 3: Weight 2
    var w3 = remainingW * (2/3);
    ctx.fillStyle = BLUE;
    ctx.fillRect(startX + w1 + w2, startY, w3, rowH);
    ctx.strokeRect(startX + w1 + w2, startY, w3, rowH);
    ctx.fillStyle = 'white';
    if (w3 > 50) {
      var p3 = Math.round(w3 / totalW * 100);
      ctx.fillText('Weight 2 (' + p3 + '%)', startX + w1 + w2 + w3/2, startY + rowH/2 + 5);
    }

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('Horizontal Stacking with Weights', startX, startY - 20);
    
    requestAnimationFrame(draw);
  }
  draw();
})();
</script>

#### Measurement Phase

The measurement process is handled in `computeWrapSize` and `computeSize`.

1.  **Weight Distribution**:
    -   The layout first identifies children with horizontal weights (`WidthModifier.weight`).
    -   Children **without** weights are measured first with the available width. The accumulated width of these children is subtracted from the total available space.
    -   The remaining space is then distributed among the weighted children proportional to their weight value.
    -   Each weighted child is measured with its calculated fixed width.

2.  **Intrinsic Size**:
    -   The `width` of the Row is the sum of all children's measured widths plus the `spacedBy` gaps between them.
    -   The `height` of the Row is the maximum height among all measured children.

#### Layout Phase

Once children are measured, the `internalLayoutMeasure` method determines their final `(x, y)` coordinates.

1.  **Horizontal Positioning**:
    -   **START**: Children are packed at the beginning of the row.
    -   **CENTER**: The entire block of children is centered horizontally.
    -   **END**: Children are packed at the end of the row.
    -   **SPACE_BETWEEN**: The first child is at the start, the last at the end, and the remaining space is distributed evenly between children.
    -   **SPACE_EVENLY**: Space is distributed such that the gap between any two items and the edges is equal.
    -   **SPACE_AROUND**: Space is distributed such that the gap between items is equal, and the gap at the ends is half of the internal gap.

2.  **Vertical Positioning**:
    -   Applied individually to each child within the calculated Row height.
    -   **TOP**: Child is at the top.
    -   **CENTER**: Child is vertically centered.
    -   **BOTTOM**: Child is at the bottom.
    -   **Baseline Alignment**: If children have `AlignBy` modifiers, they are aligned based on their specified baseline or anchor.

3.  **Spacing**:
    -   The `spacedBy` value is added between each child in all positioning modes.