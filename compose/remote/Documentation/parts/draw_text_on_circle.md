### DrawTextOnCircle Illustration

The `DrawTextOnCircle` operation renders curved text along the circumference of a circle.

<div id="drawTextOnCircleContainer">
  <canvas id="drawTextOnCircleCanvas_v1" width="500" height="500" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawTextOnCircleCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 500);

    var cx = 250, cy = 250, r = 160;
    var text = "RemoteCompose Curved Text Illustration";
    
    // Circle Guide
    ctx.strokeStyle = '#eee';
    ctx.setLineDash([12, 12]);
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.stroke();
    ctx.setLineDash([]);

    ctx.font = '32px Arial';
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'center';
    
    var characters = text.split("");
    var anglePerChar = (Math.PI * 1.5) / characters.length;
    var startAngle = -Math.PI * 0.75;

    for (var i = 0; i < characters.length; i++) {
      ctx.save();
      ctx.translate(cx, cy);
      var currentAngle = startAngle + i * anglePerChar;
      ctx.rotate(currentAngle);
      ctx.fillText(characters[i], 0, -r);
      ctx.restore();
    }

    // Center Anchor
    ctx.beginPath();
    ctx.arc(cx, cy, 6, 0, 2 * Math.PI);
    ctx.fillStyle = DARK_GRAY;
    ctx.fill();
    
    ctx.font = 'bold 22px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Center (cx, cy)', cx + 15, cy - 15);
    
    // Radius indicator
    ctx.strokeStyle = BLUE;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + r, cy);
    ctx.stroke();
    ctx.fillStyle = BLUE;
    ctx.fillText('Radius', cx + r/2, cy + 25);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>