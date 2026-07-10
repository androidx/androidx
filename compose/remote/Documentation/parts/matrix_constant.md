### MatrixConstant Illustration

The `MatrixConstant` operation defines a static transformation matrix. In RemoteCompose, matrices are typically 3x3 (9 values) for 2D transformations, following the standard row-major indexing:

| | | |
|:---:|:---:|:---:|
| **MSCALE_X** (0) | **MSKEW_X** (1) | **MTRANS_X** (2) |
| **MSKEW_Y** (3) | **MSCALE_Y** (4) | **MTRANS_Y** (5) |
| **MPERSP_0** (6) | **MPERSP_1** (7) | **MPERSP_2** (8) |

<div id="matrixConstantContainer">
  <canvas id="matrixConstantCanvas_v1" width="500" height="350" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('matrixConstantCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', DARK_GRAY = '#444444', GRAY = '#888888';
    ctx.clearRect(0, 0, 500, 350);

    // Matrix Representation
    var startX = 50, startY = 60, cellW = 100, cellH = 40;
    var values = ['sX', 'kX', 'tX', 'kY', 'sY', 'tY', 'p0', 'p1', 'p2'];

    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 1;
    ctx.font = '14px Arial';
    ctx.textAlign = 'center';

    for (var i = 0; i < 3; i++) {
      for (var j = 0; j < 3; j++) {
        var idx = i * 3 + j;
        var x = startX + j * cellW;
        var y = startY + i * cellH;
        
        ctx.strokeRect(x, y, cellW, cellH);
        ctx.fillStyle = DARK_GRAY;
        ctx.fillText(values[idx], x + cellW/2, y + 25);
        ctx.fillStyle = GRAY;
        ctx.font = '10px Arial';
        ctx.fillText('index ' + idx, x + cellW - 25, y + 12);
        ctx.font = '14px Arial';
      }
    }

    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 20px Arial';
    ctx.textAlign = 'left';
    ctx.fillText('3x3 Transformation Matrix Layout', startX, 40);

    // Example explanation
    var exY = 220;
    ctx.font = 'bold 16px Arial';
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'left';
    ctx.fillText('Example: Translation only', startX, exY);
    
    ctx.font = '16px monospace';
    ctx.fillText('[ 1, 0, tx,', startX + 20, exY + 30);
    ctx.fillText('  0, 1, ty,', startX + 20, exY + 55);
    ctx.fillText('  0, 0, 1  ]', startX + 20, exY + 80);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
