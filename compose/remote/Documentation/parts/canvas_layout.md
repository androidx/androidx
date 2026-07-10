### Canvas Layout Algorithm

Canvas Layout is a container optimized for custom drawing operations while still supporting child components.

#### Visual Illustration
<div id="canvasLayoutContainer">
  <canvas id="canvasLayoutCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('canvasLayoutCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', DARK_BLUE = '#002366', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    var cx = 50, cy = 50, cw = 400, ch = 200;

    // Canvas Bounds
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.strokeRect(cx, cy, cw, ch);
    ctx.fillStyle = '#fdfdfd';
    ctx.fillRect(cx, cy, cw, ch);

    // Custom Drawing (relative to canvas top-left)
    ctx.save();
    ctx.translate(cx, cy);

    // Drawing 1: Grid
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 1;
    for (var i = 0; i < cw; i += 40) { ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, ch); ctx.stroke(); }
    for (var j = 0; j < ch; j += 40) { ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(cw, j); ctx.stroke(); }

    // Drawing 2: Shapes
    ctx.fillStyle = BLUE_TRANS;
    ctx.beginPath();
    ctx.arc(100, 100, 60, 0, Math.PI*2);
    ctx.fill();
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    // Drawing 3: Child Component (e.g., a button or text block)
    var childX = 220, childY = 80, childW = 120, childH = 40;
    ctx.fillStyle = DARK_BLUE;
    ctx.fillRect(childX, childY, childW, childH);
    ctx.fillStyle = 'white';
    ctx.font = 'bold 14px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Child Component', childX + childW/2, childY + 25);
    
    // Annotation for child positioning
    ctx.strokeStyle = DARK_GRAY;
    ctx.setLineDash([2, 2]);
    ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(childX, childY); ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = DARK_GRAY;
    ctx.font = '12px Arial';
    ctx.fillText('LayoutCompute(x, y)', childX/2, childY/2);

    ctx.restore();

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.textAlign = 'left';
    ctx.fillText('Canvas Layout', cx, cy - 15);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>

#### Layout Logic

1.  **Drawing Encapsulation**:
    -   Unlike other layouts, CanvasLayout primarily serves as a scope for `Draw` operations (DrawRect, DrawCircle, etc.).
    -   All drawing instructions contained within the Canvas are executed relative to the Canvas's top-left corner.

2.  **Child Positioning**:
    -   Children of a CanvasLayout are by default positioned at `(0, 0)` and sized to fill the entire Canvas area if standard `LayoutComponentContent` is used.
    -   However, if `LayoutCompute` modifiers are used, children can be positioned anywhere on top of the custom drawing.
