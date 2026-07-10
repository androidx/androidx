### Flow Layout Algorithm

The Flow Layout manager positions child components horizontally and automatically wraps them to a new "row" when the available width is exhausted.

#### Visual Illustration
<div id="flowLayoutContainer">
  <canvas id="flowLayoutCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var widthPhase = 0;
  function draw() {
    var canvas = document.getElementById('flowLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    // Dynamic width for animation
    var baseWidth = 460;
    var minWidth = 150;
    var currentFlowWidth = minWidth + (Math.sin(widthPhase) + 1) / 2 * (baseWidth - minWidth);
    widthPhase += 0.02;

    var startX = 20, startY = 60;
    var itemW = 60, itemH = 40, spacing = 10;
    var items = 8;

    // Draw Container Boundary
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(startX, startY, currentFlowWidth, 200);
    ctx.setLineDash([]);
    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = GRAY;
    ctx.fillText('Available Width: ' + Math.round(currentFlowWidth) + 'px', startX, startY - 15);

    // Flow Logic
    var tx = 0, ty = 0;
    for (var i = 0; i < items; i++) {
      if (tx + itemW > currentFlowWidth) {
        tx = 0;
        ty += itemH + spacing;
      }

      ctx.fillStyle = BLUE_TRANS;
      ctx.fillRect(startX + tx, startY + ty, itemW, itemH);
      ctx.strokeStyle = BLUE;
      ctx.lineWidth = 2;
      ctx.strokeRect(startX + tx, startY + ty, itemW, itemH);
      
      ctx.fillStyle = BLUE;
      ctx.font = 'bold 14px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(i + 1, startX + tx + itemW/2, startY + ty + itemH/2 + 5);
      
      tx += itemW + spacing;
    }

    requestAnimationFrame(draw);
  }
  draw();
})();
</script>

#### Measurement Phase

1.  **Segmentation**:
    -   The algorithm first iterates through children to determine which ones fit on the current line.
    -   If a child (including its minimum width if weighted) exceeds the remaining width in the current line, a new line is started.
    -   This results in a list of "rows," where each row is a collection of components.

2.  **Wrap Sizing**:
    -   The total `width` is the maximum width of any individual row.
    -   The total `height` is the sum of the heights of all rows.

#### Layout Phase

1.  **Row Processing**:
    -   Each segment (row) identified in the measurement phase is treated like an individual Row Layout.
    -   Horizontal alignment (`START`, `CENTER`, `END`, etc.) is applied to components within each row.
    -   Vertical alignment (`TOP`, `CENTER`, `BOTTOM`) determines how the entire block of rows is positioned within the Flow Layout's total height.

2.  **Spacing**:
    -   Horizontal spacing between components in a row is controlled by `spacedBy`.