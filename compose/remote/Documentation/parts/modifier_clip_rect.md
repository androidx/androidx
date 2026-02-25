### ClipRect Illustration

The `ClipRect` modifier restricts the drawing area of a component to its own rectangular bounds.

<div id="clipRectContainer">
  <canvas id="clipRectCanvas_v1" width="500" height="330" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('clipRectCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 330);

    var clipX=80, clipY=80, clipW=340, clipH=170;

    // Draw what's outside the clip (dimmed Blue)
    ctx.globalAlpha = 0.2;
    ctx.fillStyle = BLUE;
    ctx.beginPath();
    ctx.arc(115, 115, 100, 0, 2 * Math.PI);
    ctx.fill();
    ctx.globalAlpha = 1.0;

    // Clip Boundary
    ctx.strokeStyle = GRAY;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 8]);
    ctx.strokeRect(clipX, clipY, clipW, clipH);
    ctx.setLineDash([]);
    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Clip Bounds', clipX, clipY - 15);

    // Apply clip and draw content
    ctx.save();
    ctx.beginPath();
    ctx.rect(clipX, clipY, clipW, clipH);
    ctx.clip();
    
    ctx.fillStyle = BLUE;
    ctx.beginPath();
    ctx.arc(115, 115, 100, 0, 2 * Math.PI);
    ctx.fill();
    
    ctx.fillStyle = BLUE;
    ctx.globalAlpha = 0.6;
    ctx.fillRect(300, 130, 170, 170);
    
    ctx.restore();
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
