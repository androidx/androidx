### DrawTextAnchored Illustration

The `DrawTextAnchored` operation renders text relative to an anchor point `(x, y)` using horizontal (`panX`) and vertical (`panY`) values ranging from -1.0 to 1.0.

#### Center Aligned (`panX: 0, panY: 0`)
<div id="drawTextAnchoredContainer_center">
  <canvas id="drawTextAnchoredCanvas_center" width="500" height="160" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### Top-Left Aligned (`panX: -1, panY: -1`)
<div id="drawTextAnchoredContainer_tl">
  <canvas id="drawTextAnchoredCanvas_tl" width="500" height="160" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### Bottom-Right Aligned (`panX: 1, panY: 1`)
<div id="drawTextAnchoredContainer_br">
  <canvas id="drawTextAnchoredCanvas_br" width="500" height="160" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function setupCanvas(canvasId, panX, panY, anchorLabelYOffset) {
    function draw() {
      var canvas = document.getElementById(canvasId);
      if (!canvas) { setTimeout(draw, 100); return; }
      var ctx = canvas.getContext('2d');
      if (!ctx) return;

      var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444';
      var ax = 250, ay = 80;
      var text = "Anchored Text";
      ctx.font = '32px Arial';
      ctx.textBaseline = 'top'; 
      var metrics = ctx.measureText(text);
      var tw = metrics.width;
      var th = 32;

      ctx.clearRect(0, 0, 500, 160);

      var dx = (-(panX + 1) / 2) * tw;
      var dy = (-(panY + 1) / 2) * th;

      ctx.strokeStyle = '#ddd';
      ctx.lineWidth = 1;
      ctx.strokeRect(ax + dx, ay + dy, tw, th);

      ctx.fillStyle = BLUE;
      ctx.fillText(text, ax + dx, ay + dy);

      ctx.beginPath();
      ctx.arc(ax, ay, 6, 0, 2 * Math.PI);
      ctx.fillStyle = DARK_GRAY;
      ctx.fill();
      
      ctx.font = 'bold 18px Arial';
      ctx.fillStyle = DARK_GRAY;
      ctx.textAlign = 'left';
      var ayOff = anchorLabelYOffset || -15;
      ctx.fillText('Anchor (x, y)', ax + 15, ay + ayOff);
    }
    if (document.readyState === 'complete') { draw(); } 
    else { window.addEventListener('load', draw); setTimeout(draw, 500); }
  }

  setupCanvas('drawTextAnchoredCanvas_center', 0, 0, -35);
  setupCanvas('drawTextAnchoredCanvas_tl', -1, -1, -25);
  setupCanvas('drawTextAnchoredCanvas_br', 1, 1, -15);
})();
</script>
