### Collapsible Row Layout Algorithm

Collapsible Row extends Row Layout with the ability to automatically hide children that do not fit within the available horizontal space.

#### Basic Overflow Animation
<div id="collapsibleRowContainer">
  <canvas id="collapsibleRowCanvas_v1" width="500" height="180" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

#### Priority-Based Hiding Animation
In this example, **Item 2** has a higher priority value (lower importance), so it is hidden **first**. When it disappears, the layout collapses and moves Item 3 into its place.

<div id="collapsibleRowPriorityContainer">
  <canvas id="collapsibleRowPriorityCanvas_v1" width="500" height="180" style="border:1px solid #ccc; background: #fff; display: block; margin: 10px 0;"></canvas>
</div>

<script>
(function() {
  var BLUE = '#0047AB', GRAY = '#888888', DARK_GRAY = '#444444', BLUE_TRANS = 'rgba(0, 71, 171, 0.2)';
  var itemW = 120, itemH = 60, spacing = 10, startX = 25, startY = 70;

  function drawItem(ctx, idx, x, y, fits, label) {
    if (!fits) return; // In a real layout, GONE items aren't drawn at all
    ctx.fillStyle = BLUE_TRANS;
    ctx.fillRect(x, y, itemW, itemH);
    ctx.strokeStyle = BLUE;
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, itemW, itemH);
    ctx.fillStyle = BLUE;
    ctx.textAlign = 'center';
    ctx.font = 'bold 14px Arial';
    ctx.fillText(label, x + itemW/2, y + itemH/2 + 5);
  }

  var phase1 = 0, phase2 = 0;
  function animate() {
    var c1 = document.getElementById('collapsibleRowCanvas_v1');
    var c2 = document.getElementById('collapsibleRowPriorityCanvas_v1');
    
    if (c1) {
      var ctx = c1.getContext('2d');
      ctx.clearRect(0, 0, 500, 180);
      var w = 100 + (Math.sin(phase1) + 1) / 2 * 350;
      phase1 += 0.015;
      ctx.strokeStyle = GRAY; ctx.setLineDash([5, 5]); ctx.strokeRect(startX, startY, w, itemH); ctx.setLineDash([]);
      ctx.fillStyle = GRAY; ctx.font = 'bold 14px Arial'; ctx.textAlign='left'; ctx.fillText('Width: ' + Math.round(w) + 'px', startX, startY - 10);
      
      var currentX = startX;
      for (var i=0; i<3; i++) {
        var fits = ( (currentX + itemW - startX) <= w );
        if (fits) {
          drawItem(ctx, i+1, currentX, startY, true, 'ITEM ' + (i+1));
          currentX += itemW + spacing;
        }
      }
    }

    if (c2) {
      var ctx = c2.getContext('2d');
      ctx.clearRect(0, 0, 500, 180);
      var w = 100 + (Math.sin(phase2) + 1) / 2 * 350;
      phase2 += 0.015;
      ctx.strokeStyle = GRAY; ctx.setLineDash([5, 5]); ctx.strokeRect(startX, startY, w, itemH); ctx.setLineDash([]);
      ctx.fillStyle = GRAY; ctx.font = 'bold 14px Arial'; ctx.textAlign='left'; ctx.fillText('Width: ' + Math.round(w) + 'px', startX, startY - 10);
      
      var items = [
        {id: 1, p: 10},
        {id: 2, p: 100},
        {id: 3, p: 20}
      ];
      
      var visibility = {};
      var accumulated = 0;
      // Process in priority order (lowest P first)
      var priorityProcess = [...items].sort((a,b) => a.p - b.p);
      for (var itm of priorityProcess) {
         if (accumulated + itemW <= w) {
            visibility[itm.id] = true;
            accumulated += itemW + spacing;
         } else {
            visibility[itm.id] = false;
         }
      }

      var currentX = startX;
      for (var itm of items) {
        if (visibility[itm.id]) {
          drawItem(ctx, itm.id, currentX, startY, true, 'ITEM ' + itm.id + ' (P:' + itm.p + ')');
          currentX += itemW + spacing;
        }
      }
    }
    requestAnimationFrame(animate);
  }
  animate();
})();
</script>

#### Measurement Phase

1.  **Priority Sorting**:
    -   If children have `CollapsiblePriority` modifiers, they are sorted by priority (highest values are kept first).
    -   If no priority is set, they are considered in last resort.
    -   If priorities are equal, the original hierarchy order is used.

2.  **Overflow Detection**:
    -   Children are measured one by one.
    -   The layout keeps track of the accumulated width.
    -   As soon as a child's width would cause the total to exceed the available width, that child and all subsequent children (in priority order) are marked as `GONE`.

#### Layout Phase

-   The remaining `VISIBLE` children are laid out exactly like a standard Row Layout, using the specified horizontal and vertical positioning.
