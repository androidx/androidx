### Offset Modifier Illustration

The `Offset` modifier shifts the position of a component by a specific amount in X and Y without affecting its layout in the parent.

<div id="offsetModifierContainer">
  <canvas id="offsetModifierCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('offsetModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    var ox=100, oy=50;

    ctx.clearRect(0, 0, 500, 250);

    // Original (dashed)
    ctx.setLineDash([8, 8]);
    ctx.strokeStyle = '#bbb';
    ctx.lineWidth = 2;
    ctx.strokeRect(65, 65, 135, 100);
    ctx.setLineDash([]);
    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = '#888';
    ctx.fillText('Original', 65, 55);

    // Offset
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(65 + ox, 65 + oy, 135, 100);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 3;
    ctx.strokeRect(65 + ox, 65 + oy, 135, 100);

    // Arrow
    ctx.beginPath();
    ctx.moveTo(130, 115);
    ctx.lineTo(130 + ox, 115 + oy);
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 2;
    ctx.stroke();
    
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 22px Arial';
    ctx.fillText('x: ' + ox + ', y: ' + oy, 130 + ox/2 - 40, 115 + oy/2 - 10);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
