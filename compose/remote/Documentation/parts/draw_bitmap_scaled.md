### DrawBitmapScaled Illustration

The `DrawBitmapScaled` operation handles rendering images within a destination area using different scaling algorithms. To illustrate this, a "House" image with a 1:2 aspect ratio (tall) is used.

<div id="drawBitmapScaledContainer">
  <canvas id="drawBitmapScaledCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawBitmapScaledCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    function drawHouse(ctx, w, h) {
      ctx.beginPath();
      ctx.rect(0, h * 0.4, w, h * 0.6);
      ctx.moveTo(0, h * 0.4);
      ctx.lineTo(w / 2, 0);
      ctx.lineTo(w, h * 0.4);
      ctx.rect(w * 0.35, h * 0.7, w * 0.3, h * 0.3);
      ctx.fillStyle = BLUE_TRANS;
      ctx.fill();
      ctx.lineWidth = Math.max(2, w/20);
      ctx.strokeStyle = BLUE;
      ctx.stroke();
    }

    function drawScaleExample(x, label, mode) {
      var destW = 120, destH = 180;
      var imgW = 100, imgH = 200;

      ctx.save();
      ctx.translate(x, 40);
      
      ctx.strokeStyle = '#eee';
      ctx.setLineDash([5, 5]);
      ctx.lineWidth = 2;
      ctx.strokeRect(0, 0, destW, destH);
      ctx.setLineDash([]);

      ctx.save();
      ctx.beginPath(); ctx.rect(0, 0, destW, destH); ctx.clip();

      if (mode === 'FIT') {
        var s = Math.min(destW / imgW, destH / imgH);
        ctx.translate((destW - imgW * s) / 2, (destH - imgH * s) / 2);
        drawHouse(ctx, imgW * s, imgH * s);
      } else if (mode === 'CROP') {
        var s = Math.max(destW / imgW, destH / imgH);
        ctx.translate((destW - imgW * s) / 2, (destH - imgH * s) / 2);
        drawHouse(ctx, imgW * s, imgH * s);
      } else if (mode === 'FILL') {
        drawHouse(ctx, destW, destH);
      }
      ctx.restore();

      ctx.fillStyle = DARK_GRAY;
      ctx.font = 'bold 18px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(label, destW / 2, destH + 35);
      ctx.restore();
    }

    drawScaleExample(40, 'SCALE_FIT', 'FIT');
    drawScaleExample(190, 'SCALE_CROP', 'CROP');
    drawScaleExample(340, 'SCALE_FILL', 'FILL');
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
