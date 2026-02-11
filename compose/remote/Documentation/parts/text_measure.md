### TextMeasure Illustration

The `TextMeasure` operation calculates dimensions of a text string, such as its width, height, or relative positions like the baseline.

<div id="textMeasureContainer">
  <canvas id="textMeasureCanvas_v1" width="600" height="220" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('textMeasureCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 600, 220);

    var x=60, y=110;
    var text = "Typography";
    ctx.font = '80px serif';
    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';
    
    // Draw Text
    ctx.fillStyle = BLUE;
    ctx.fillText(text, x, y);

    // Metrics lines
    var metrics = ctx.measureText(text);
    var w = metrics.width;
    var ascent = 60; // approx
    var descent = 20; // approx

    // Baseline
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(x - 20, y); ctx.lineTo(x + w + 20, y); ctx.stroke();
    
    // Ascent
    ctx.strokeStyle = GRAY;
    ctx.setLineDash([5, 5]);
    ctx.beginPath(); ctx.moveTo(x - 20, y - ascent); ctx.lineTo(x + w + 20, y - ascent); ctx.stroke();
    
    // Descent
    ctx.beginPath(); ctx.moveTo(x - 20, y + descent); ctx.lineTo(x + w + 20, y + descent); ctx.stroke();
    ctx.setLineDash([]);

    // Labels
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Baseline', x + w + 25, y + 5);
    
    ctx.fillStyle = GRAY;
    ctx.fillText('Ascent', x + w + 25, y - ascent + 5);
    ctx.fillText('Descent', x + w + 25, y + descent + 5);
    
    // Width dimension
    var dimY = y + 50;
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x, dimY); ctx.lineTo(x + w, dimY);
    ctx.moveTo(x, dimY - 5); ctx.lineTo(x, dimY + 5);
    ctx.moveTo(x + w, dimY - 5); ctx.lineTo(x + w, dimY + 5);
    ctx.stroke();

    ctx.textAlign = 'center';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Width: ' + Math.round(w) + 'px', x + w/2, dimY + 25);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
