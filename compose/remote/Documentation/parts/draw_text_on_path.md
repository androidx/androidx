### DrawTextOnPath Illustration

The `DrawTextOnPath` operation renders text along a specified path. The text follows the curve, maintaining its orientation relative to the path's tangent.

<div id="drawTextOnPathContainer">
  <canvas id="drawTextOnPathCanvas_v1" width="500" height="250" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  function draw() {
    var canvas = document.getElementById('drawTextOnPathCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444';
    ctx.clearRect(0, 0, 500, 250);

    var p0 = {x: 60,  y: 180};
    var p1 = {x: 120, y: 60};
    var p2 = {x: 380, y: 300};
    var p3 = {x: 440, y: 180};

    // Reference Path
    ctx.beginPath();
    ctx.moveTo(p0.x, p0.y);
    ctx.bezierCurveTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
    ctx.strokeStyle = '#eee';
    ctx.lineWidth = 2;
    ctx.setLineDash([12, 12]);
    ctx.stroke();
    ctx.setLineDash([]);

    var text = "RemoteCompose Text Following Curve";
    ctx.font = '24px Arial';
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'center';

    function getBezierPoint(t) {
      var cx = 3 * (p1.x - p0.x);
      var bx = 3 * (p2.x - p1.x) - cx;
      var ax = p3.x - p0.x - cx - bx;
      var cy = 3 * (p1.y - p0.y);
      var by = 3 * (p2.y - p1.y) - cy;
      var ay = p3.y - p0.y - cy - by;
      var x = (ax * Math.pow(t, 3)) + (bx * Math.pow(t, 2)) + (cx * t) + p0.x;
      var y = (ay * Math.pow(t, 3)) + (by * Math.pow(t, 2)) + (cy * t) + p0.y;
      return {x: x, y: y};
    }

    function getBezierTangent(t) {
      var cx = 3 * (p1.x - p0.x);
      var bx = 3 * (p2.x - p1.x) - cx;
      var ax = p3.x - p0.x - cx - bx;
      var cy = 3 * (p1.y - p0.y);
      var by = 3 * (p2.y - p1.y) - cy;
      var ay = p3.y - p0.y - cy - by;
      var dx = (3 * ax * Math.pow(t, 2)) + (2 * bx * t) + cx;
      var dy = (3 * ay * Math.pow(t, 2)) + (2 * by * t) + cy;
      return Math.atan2(dy, dx);
    }

    var chars = text.split("");
    var step = 0.8 / chars.length;
    for (var i = 0; i < chars.length; i++) {
      var t = 0.1 + (i * step);
      var pt = getBezierPoint(t);
      var angle = getBezierTangent(t);

      ctx.save();
      ctx.translate(pt.x, pt.y);
      ctx.rotate(angle);
      ctx.fillText(chars[i], 0, -8);
      ctx.restore();
    }

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = DARK_GRAY;
    ctx.fillText('Path Tangent Alignment', 120, 30);
  }
  if (document.readyState === 'complete') { draw(); } 
  else { window.addEventListener('load', draw); setTimeout(draw, 500); }
})();
</script>
