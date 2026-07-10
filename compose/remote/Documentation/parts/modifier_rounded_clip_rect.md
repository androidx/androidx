### RoundedClipRect Illustration

The `RoundedClipRect` modifier restricts the drawing area of a component to its own bounds with rounded corners.

<div id="roundedClipContainer">
  <canvas id="roundedClipCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('roundedClipCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 300);

    var x=100, y=50, w=300, h=200, r=40;

    // Background content (dimmed)
    ctx.globalAlpha = 0.1;
    ctx.fillStyle = BLUE;
    ctx.fillRect(50, 20, 400, 260);
    ctx.globalAlpha = 1.0;

    // Guide
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 8]);
    ctx.strokeRect(x, y, w, h);
    ctx.setLineDash([]);

    // Clip
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.clip();

    ctx.fillStyle = BLUE;
    ctx.fillRect(50, 20, 400, 260);
    
    ctx.fillStyle = 'white';
    ctx.font = 'bold 24px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('Rounded Clip Area', x + w/2, y + h/2 + 10);
    ctx.restore();

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.fillText('Clip Bounds (left, top, right, bottom)', x, y + h + 30);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>