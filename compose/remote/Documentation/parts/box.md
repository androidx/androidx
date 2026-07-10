### Box Layout Algorithm

The Box Layout manager positions child components independently within its own bounds. It is the most basic container, allowing components to be stacked or positioned relative to the Box's edges.

#### Visual Illustration (Alignment)
<div id="boxLayoutContainer">
  <canvas id="boxLayoutCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('boxLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    var W=400, H=200, startX=50, startY=50;

    // Box Bounds
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(startX, startY, W, H);
    ctx.setLineDash([]);

    function drawItem(offsetX, offsetY, label, color) {
      var iw=100, ih=60;
      ctx.fillStyle = color;
      ctx.fillRect(startX + offsetX, startY + offsetY, iw, ih);
      ctx.strokeStyle = BLUE;
      ctx.lineWidth = 2;
      ctx.strokeRect(startX + offsetX, startY + offsetY, iw, ih);
      ctx.fillStyle = 'white';
      ctx.font = 'bold 14px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, startX + offsetX + iw/2, startY + offsetY + ih/2 + 5);
    }

    // Stacked/Aligned Items
    drawItem(0, 0, 'Top-Start', 'rgba(0, 71, 171, 0.4)');
    drawItem(W-100, H-60, 'Bottom-End', 'rgba(0, 71, 171, 0.7)');
    drawItem((W-100)/2, (H-60)/2, 'Center', BLUE);

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('Independent Child Alignment & Stacking', startX, startY - 20);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>

#### Measurement Phase

The measurement process is handled in `computeWrapSize` and `computeSize`.

1.  **Independent Measurement**:
    -   Each child component is measured independently with the available width and height of the Box.
    -   If a child has a dynamic `LayoutCompute` modifier, its size is calculated based on the provided expressions before final measurement.

2.  **Intrinsic Size (Wrap Content)**:
    -   If the Box is set to wrap its content, its `width` will be the maximum width among all its visible children.
    -   Similarly, its `height` will be the maximum height among all its visible children.

#### Layout Phase

In the `internalLayoutMeasure` method, children are positioned based on the Box's alignment rules.

1.  **Alignment**:
    -   **Horizontal Positioning**: Children can be aligned to the `START`, `CENTER`, or `END` of the Box.
    -   **Vertical Positioning**: Children can be aligned to the `TOP`, `CENTER`, or `BOTTOM` of the Box.
    -   Each child is positioned using these global rules relative to the Box's internal area (after padding).

2.  **Stacking**:
    -   Components are painted in their hierarchy order. The first child in the list is drawn first, and subsequent children are drawn on top if they overlap.