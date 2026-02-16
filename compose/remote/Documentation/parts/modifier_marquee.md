### Marquee Illustration

The `Marquee` modifier creates a scrolling animation for text that is too long to fit within its container.

<div id="marqueeModifierContainer">
  <canvas id="marqueeModifierCanvas_v1" width="500" height="150" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var offset = 0;
  function draw() {
    var canvas = document.getElementById('marqueeModifierCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.1)';
    ctx.clearRect(0, 0, 500, 150);

    var vx=100, vy=50, vw=300, vh=50;
    var text = "This is a long marquee text that scrolls continuously...";
    
    ctx.font = '24px Arial';
    var tw = ctx.measureText(text).width;

    // Viewport
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.strokeRect(vx, vy, vw, vh);
    
    // Scrolling Content
    ctx.save();
    ctx.beginPath();
    ctx.rect(vx, vy, vw, vh);
    ctx.clip();
    
    ctx.fillStyle = BLUE;
    ctx.fillText(text, vx + 20 - offset, vy + 35);
    // Draw repeat if needed
    if (offset > 20) {
       ctx.fillText(text, vx + 20 - offset + tw + 50, vy + 35);
    }
    ctx.restore();

    // Labels
    ctx.fillStyle = GRAY;
    ctx.font = 'bold 18px Arial';
    ctx.fillText('Viewport (Static)', vx, vy - 15);
    ctx.fillText('Velocity Direction', 180, vy + vh + 35);
    
    // Arrow
    ctx.beginPath();
    ctx.moveTo(150, 130);
    ctx.lineTo(100, 130);
    ctx.strokeStyle = BLUE;
    ctx.stroke();

    offset = (offset + 1) % (tw + 50);
    requestAnimationFrame(draw);
  }
  draw();
})();
</script>
