### Particles Illustration

The `ParticlesCreate` operation defines a particle emitter that generates short-lived visual elements with dynamic velocity, spread, and life.

<div id="particlesContainer">
  <canvas id="particlesCanvas_v1" width="500" height="300" style="border:1px solid #ccc; background: #000; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var particles = [];
  function draw() {
    var canvas = document.getElementById('particlesCanvas_v1');
    if (!canvas) { setTimeout(draw, 100); return; }
    var ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, 500, 300);

    var ex = 250, ey = 150;

    if (particles.length < 100) {
      particles.push({
        x: ex, y: ey,
        vx: (Math.random() - 0.5) * 4,
        vy: (Math.random() - 0.5) * 4,
        life: 1.0,
        size: 2 + Math.random() * 4
      });
    }

    for (var i = particles.length - 1; i >= 0; i--) {
      var p = particles[i];
      p.x += p.vx;
      p.y += p.vy;
      p.life -= 0.01;

      if (p.life <= 0) {
        particles.splice(i, 1);
        continue;
      }

      ctx.fillStyle = 'rgba(0, 71, 171, ' + p.life + ')';
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI*2);
      ctx.fill();
    }

    ctx.fillStyle = '#888';
    ctx.font = 'bold 20px Arial';
    ctx.fillText('Dynamic Particle Emitter', 140, 40);
    
    requestAnimationFrame(draw);
  }
  draw();
})();
</script>