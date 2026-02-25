### MatrixSave/Restore Illustration

`MatrixSave` pushes the current transformation and clip state onto a stack. `MatrixRestore` pops the state, returning the coordinate system to its previous configuration.

<div id="matrixSaveContainer">
  <canvas id="matrixSaveCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('matrixSaveCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
    ctx.clearRect(0, 0, 500, 300);

    // Initial State
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 2;
    ctx.strokeRect(50, 50, 100, 100);
    ctx.fillStyle = GRAY;
    ctx.font = 'bold 16px Arial';
    ctx.fillText('Initial Coordinate System', 50, 45);

    // SAVE 1
    ctx.save();
    ctx.translate(150, 0);
    ctx.rotate(Math.PI/8);
    
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(50, 50, 100, 100);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 3;
    ctx.strokeRect(50, 50, 100, 100);
    ctx.fillStyle = BLUE;
    ctx.fillText('Transformed (State 1)', 50, 45);

    // RESTORE 1
    ctx.restore();

    // Demonstration we are back at initial
    ctx.beginPath();
    ctx.moveTo(50, 200);
    ctx.lineTo(450, 200);
    ctx.strokeStyle = DARK_GRAY;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 5]);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = DARK_GRAY;
    ctx.font = 'bold 20px Arial';
    ctx.fillText('Back to Initial after MatrixRestore', 100, 235);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>