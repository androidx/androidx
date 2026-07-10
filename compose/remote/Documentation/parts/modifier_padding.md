### Padding Modifier Illustration

The `Padding` modifier adds space around a component's content.

<div id="paddingModifierContainer">
  <canvas id="paddingModifierCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('paddingModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.1)';
    var L=50, T=65, R=80, B=35;
    var W=500, H=330;

    ctx.clearRect(0, 0, W, H);

    // Component Bounds
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 2;
    ctx.setLineDash([4, 4]);
    ctx.strokeRect(15, 15, W-30, H-30);
    ctx.setLineDash([]);

    // Padding Area
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(15, 15, W-30, H-30);
    
    // Content Area
    ctx.fillStyle = BLUE;
    ctx.fillRect(15+L, 15+T, W-30-L-R, H-30-T-B);
    
    // Labels
    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = GRAY;
    ctx.textAlign = 'center';
    ctx.fillText('Top: ' + T, W/2, 15+T/2 + 8);
    ctx.fillText('Bottom: ' + B, W/2, H-15-B/2 + 8);
    ctx.textAlign = 'left';
    ctx.fillText('Left: ' + L, 15+8, H/2);
    ctx.textAlign = 'right';
    ctx.fillText('Right: ' + R, W-15-8, H/2);

    ctx.fillStyle = 'white';
    ctx.textAlign = 'center';
    ctx.fillText('Content', W/2, H/2);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
