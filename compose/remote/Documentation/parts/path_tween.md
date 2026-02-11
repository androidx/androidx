### PathTween Illustration

The `PathTween` operation interpolates between two source paths (Path 1 and Path 2) based on a `tween` factor (0.0 to 1.0).

<div id="pathTweenContainer">
  <canvas id="pathTweenCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('pathTweenCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#BBBBBB', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.4)';
    ctx.clearRect(0, 0, 500, 300);

    var tween = 0.5;

    function getPoints(t) {
      // Circle-ish
      var p1 = [ {x:100,y:150}, {x:150,y:50}, {x:250,y:50}, {x:300,y:150} ];
      // Line-ish
      var p2 = [ {x:100,y:250}, {x:150,y:250}, {x:250,y:250}, {x:300,y:250} ];
      
      return p1.map((p, i) => ({
        x: p.x + (p2[i].x - p.x) * t,
        y: p.y + (p2[i].y - p.y) * t
      }));
    }

    function drawPath(pts, color, width, dash) {
      ctx.beginPath();
      ctx.moveTo(pts[0].x, pts[0].y);
      ctx.bezierCurveTo(pts[1].x, pts[1].y, pts[2].x, pts[2].y, pts[3].x, pts[3].y);
      ctx.strokeStyle = color;
      ctx.lineWidth = width;
      if (dash) ctx.setLineDash(dash);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // Source Paths
    drawPath(getPoints(0), GRAY, 2, [5, 5]);
    drawPath(getPoints(1), GRAY, 2, [5, 5]);
    
    // Tweened Path
    drawPath(getPoints(tween), BLUE, 6);

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = GRAY;
    ctx.fillText('Path 1 (tween: 0.0)', 100, 40);
    ctx.fillText('Path 2 (tween: 1.0)', 100, 280);
    ctx.fillStyle = BLUE;
    ctx.fillText('Interpolated (tween: 0.5)', 120, 140);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
