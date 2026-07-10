### Column Layout Algorithm

The Column Layout manager positions child components vertically one after another. It is the vertical counterpart to the Row Layout.

#### Visual Illustration (Weights)
<div id="columnLayoutContainer">
  <canvas id="columnLayoutCanvas_v1" width="500" height="400" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var phase = 0;
  function draw() {
    var canvas = document.getElementById('columnLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 400);

    var startX = 150, startY = 50, colW = 200;
    var minH = 100, maxH = 320;
    var totalH = minH + (Math.sin(phase) + 1) / 2 * (maxH - minH);
    phase += 0.02;

    // Column Bounds
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(startX, startY, colW, totalH);
    ctx.setLineDash([]);

    // Items
    // Item 1: Fixed
    var h1 = 60;
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(startX, startY, colW, h1);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.strokeRect(startX, startY, colW, h1);
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 14px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Fixed (60px)', startX + colW/2, startY + h1/2 + 5);

    // Remaining height distribution
    var remainingH = Math.max(0, totalH - h1);
    
    // Item 2: Weight 1
    var h2 = remainingH * (1/3);
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(startX, startY + h1, colW, h2);
    ctx.strokeRect(startX, startY + h1, colW, h2);
    ctx.fillStyle = BLUE;
    if (h2 > 25) {
      var p2 = Math.round(h2 / totalH * 100);
      ctx.fillText('Weight 1 (' + p2 + '%)', startX + colW/2, startY + h1 + h2/2 + 5);
    }

    // Item 3: Weight 2
    var h3 = remainingH * (2/3);
    ctx.fillStyle = BLUE;
    ctx.fillRect(startX, startY + h1 + h2, colW, h3);
    ctx.strokeRect(startX, startY + h1 + h2, colW, h3);
    ctx.fillStyle = 'white';
    if (h3 > 25) {
      var p3 = Math.round(h3 / totalH * 100);
      ctx.fillText('Weight 2 (' + p3 + '%)', startX + colW/2, startY + h1 + h2 + h3/2 + 5);
    }

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('Vertical Stacking with Weights', 140, 30);
    
    requestAnimationFrame(draw);
  }
  draw();
})();
</script>

#### Measurement Phase

1.  **Weight Distribution**:
    -   The layout identifies children with vertical weights (`HeightModifier.weight`).
    -   Children **without** weights are measured first. Their combined height is subtracted from the total available height.
    -   The remaining vertical space is distributed among weighted children proportional to their weights.

2.  **Intrinsic Size**:
    -   The `height` of the Column is the sum of all children's measured heights plus the `spacedBy` vertical gaps.
    -   The `width` of the Column is the maximum width among all measured children.

#### Layout Phase

1.  **Vertical Positioning**:
    -   **TOP**: Children are packed at the top.
    -   **CENTER**: The block of children is centered vertically.
    -   **BOTTOM**: Children are packed at the bottom.
    -   **SPACE_BETWEEN**, **SPACE_EVENLY**, **SPACE_AROUND**: Similar to Row Layout, but applied vertically to distribute children across the available height.

2.  **Horizontal Positioning**:
    -   Applied individually to each child within the Column's width.
    -   **START**, **CENTER**, **END**: Aligns children horizontally within the column.

3.  **Spacing**:
    -   The `spacedBy` value is added between each child vertically.