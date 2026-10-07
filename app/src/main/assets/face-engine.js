/* Animates contours traced from the user's twelve reference faces. */
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.SextaFace = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';
  const COUNT = 96;
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const copy = model => model.map(shape => shape.map(p => [...p]));
  const collapse = (x, y) => Array.from({ length: COUNT }, () => [x, y]);

  function perimeter(pixels, width) {
    const occupied = new Set(pixels), stride = width + 1, edges = new Map();
    function edge(a, b) { if (!edges.has(a)) edges.set(a, []); edges.get(a).push(b); }
    for (const key of pixels) {
      const x = key % width, y = Math.floor(key / width), a = y * stride + x;
      if (!occupied.has(key - width)) edge(a, a + 1);
      if (x === width - 1 || !occupied.has(key + 1)) edge(a + 1, a + stride + 1);
      if (!occupied.has(key + width)) edge(a + stride + 1, a + stride);
      if (x === 0 || !occupied.has(key - 1)) edge(a + stride, a);
    }
    let longest = [];
    while (edges.size) {
      const start = edges.keys().next().value;
      let cursor = start, loop = [], guard = pixels.length * 5;
      do {
        loop.push([cursor % stride, Math.floor(cursor / stride)]);
        const next = edges.get(cursor);
        if (!next || !next.length) break;
        const destination = next.pop();
        if (!next.length) edges.delete(cursor);
        cursor = destination;
      } while (cursor !== start && --guard > 0);
      if (loop.length > longest.length) longest = loop;
    }
    // Smooth the one-pixel steps of the raster boundary before enlarging it.
    for (let pass = 0; pass < 2; pass++) {
      const previous = longest;
      longest = previous.map((_, i) => {
        let x = 0, y = 0;
        for (let offset = -3; offset <= 3; offset++) {
          const p = previous[(i + offset + previous.length) % previous.length];
          x += p[0]; y += p[1];
        }
        return [x / 7, y / 7];
      });
    }
    return longest;
  }

  function resample(points) {
    if (points.length < 3) throw new Error('Incomplete face contour');
    const distances = [0];
    for (let i = 0; i < points.length; i++) {
      const a = points[i], b = points[(i + 1) % points.length];
      distances.push(distances[i] + Math.hypot(b[0] - a[0], b[1] - a[1]));
    }
    let segment = 0;
    return Array.from({ length: COUNT }, (_, i) => {
      const distance = i / COUNT * distances[distances.length - 1];
      while (segment + 1 < points.length && distances[segment + 1] < distance) segment++;
      const a = points[segment], b = points[(segment + 1) % points.length];
      const t = (distance - distances[segment]) / (distances[segment + 1] - distances[segment]);
      return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t];
    });
  }

  function extractFeatures(imageData) {
    const { width, height, data } = imageData;
    const mask = new Uint8Array(width * height), seen = new Uint8Array(mask.length), components = [];
    for (let i = 0; i < mask.length; i++) mask[i] = Math.min(data[i * 4], data[i * 4 + 1], data[i * 4 + 2]) > 160 ? 1 : 0;
    for (let start = 0; start < mask.length; start++) {
      if (!mask[start] || seen[start]) continue;
      const queue = [start]; seen[start] = 1;
      let sumX = 0, sumY = 0;
      for (let head = 0; head < queue.length; head++) {
        const key = queue[head], x = key % width, y = Math.floor(key / width);
        sumX += x; sumY += y;
        const neighbors = [y > 0 ? key - width : -1, y < height - 1 ? key + width : -1, x > 0 ? key - 1 : -1, x < width - 1 ? key + 1 : -1];
        for (const n of neighbors) if (n >= 0 && mask[n] && !seen[n]) { seen[n] = 1; queue.push(n); }
      }
      if (queue.length > 100) components.push({ pixels: queue, x: sumX / queue.length, y: sumY / queue.length });
    }
    const left = components.filter(c => c.y < 120 && c.x < width / 2).sort((a, b) => b.pixels.length - a.pixels.length);
    const right = components.filter(c => c.y < 120 && c.x >= width / 2).sort((a, b) => b.pixels.length - a.pixels.length);
    const mouth = components.filter(c => c.y >= 120).sort((a, b) => b.pixels.length - a.pixels.length)[0];
    if (!left[0] || !right[0] || !mouth) throw new Error('Face features could not be loaded');
    const contour = c => resample(perimeter(c.pixels, width));
    return [contour(left[0]), contour(right[0]), contour(mouth), left[1] ? contour(left[1]) : collapse(102, 45), right[1] ? contour(right[1]) : collapse(194, 42)];
  }

  function align(reference, candidate) {
    let best = 0, smallest = Infinity;
    for (let shift = 0; shift < COUNT; shift++) {
      let sum = 0;
      for (let i = 0; i < COUNT; i++) {
        const a = reference[i], b = candidate[(i + shift) % COUNT];
        sum += (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2;
      }
      if (sum < smallest) { smallest = sum; best = shift; }
    }
    return candidate.map((_, i) => [...candidate[(i + best) % COUNT]]);
  }

  function interpolate(from, to, t) {
    return from.map((shape, slot) => shape.map((p, i) => [p[0] + (to[slot][i][0] - p[0]) * t, p[1] + (to[slot][i][1] - p[1]) * t]));
  }

  function reaction(index, seconds, enabled = true) {
    const parts = Array.from({ length: 5 }, () => ({ x: 0, y: 0, sx: 1, sy: 1, rotation: 0 }));
    const motion = { x: 0, y: 0, rotation: 0, parts };
    if (!enabled) return motion;
    const s = seconds, sin = Math.sin, pulse = sin(s * 3.1), entrance = Math.exp(-s * 3);
    motion.y = sin(s * 1.8) * .8;
    switch (index) {
      case 1:
        motion.y -= Math.abs(sin(s * 2)) * 1.6;
        parts[2].sx = 1 + .04 * sin(s * 2.2);
        break;
      case 2:
        motion.rotation = -.025; motion.y += 2;
        parts[0].y = parts[1].y = 1.5 * sin(s * 1.2);
        break;
      case 3:
        motion.x = sin(s * 19) * entrance * 1.6;
        parts[0].sy = parts[1].sy = .94 + .05 * sin(s * 2);
        break;
      case 4:
        parts[0].sx = parts[1].sx = 1.1 + .05 * pulse;
        parts[0].sy = parts[1].sy = 1.03 + .04 * pulse;
        parts[2].sy = 1.06 + .1 * pulse;
        motion.y -= 5 * entrance;
        break;
      case 5:
        motion.rotation = .035 + sin(s * .8) * .035;
        motion.y = 1.8 * sin(s * .9);
        parts[0].sy = parts[1].sy = .85 + .15 * sin(s * .9);
        parts[2].sx = .8 + .2 * sin(s * .9);
        break;
      case 6:
        motion.y = -Math.abs(sin(s * 4)) * 3.5;
        parts[0].sy = parts[1].sy = 1 + .05 * sin(s * 4);
        parts[2].sy = 1 + .12 * sin(s * 4);
        break;
      case 7:
        motion.rotation = sin(s * 6) * .035;
        motion.y = -Math.abs(sin(s * 6)) * 2;
        parts[2].sy = 1 + .16 * sin(s * 7);
        parts[2].sx = 1 + .035 * sin(s * 7);
        break;
      case 8:
        motion.rotation = sin(s * 1.4) * .045;
        parts[3].y = -2 - sin(s * 2) * 2;
        parts[0].y = -sin(s * 2);
        break;
      case 9:
        motion.rotation = -.035;
        parts[1].sy = 1 - .45 * Math.max(0, sin(s * 2));
        parts[2].sx = 1 + .045 * sin(s * 2);
        break;
      case 10:
        parts[0].sx = parts[0].sy = parts[1].sx = parts[1].sy = 1 + .08 * Math.max(0, sin(s * 4));
        motion.rotation = sin(s * 1.8) * .025;
        break;
      case 11:
        parts[0].x = parts[1].x = 2 + sin(s * 1.3) * 2;
        parts[0].y = parts[1].y = -2;
        parts[4].y = -1 - sin(s * 1.3) * 2;
        motion.rotation = .025;
        break;
    }
    return motion;
  }

  function draw(ctx, model, width, height, opts = {}) {
    const { index = 0, seconds = 0, enabled = false, blink = 1, lookX = 0, lookY = 0, glow = true } = opts;
    const m = reaction(index, seconds, enabled);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, width, height);
    ctx.setTransform(width / 291, 0, 0, height / 211, 0, 0);
    ctx.translate(145.5 + m.x + lookX, 105.5 + m.y + lookY);
    ctx.rotate(m.rotation);
    ctx.translate(-145.5, -105.5);
    ctx.fillStyle = '#f2f5eb';
    ctx.shadowColor = index === 10 ? '#ffb2d740' : '#e3ffc42e';
    ctx.shadowBlur = glow ? 6 : 0;
    model.forEach((points, slot) => {
      const p = m.parts[slot];
      const cx = points.reduce((a, b) => a + b[0], 0) / COUNT, cy = points.reduce((a, b) => a + b[1], 0) / COUNT;
      ctx.save();
      ctx.translate(cx + p.x, cy + p.y);
      ctx.rotate(p.rotation);
      ctx.scale(p.sx, p.sy * (slot < 2 ? blink : 1));
      ctx.translate(-cx, -cy);
      ctx.beginPath();
      const start = points[0], last = points[points.length - 1];
      ctx.moveTo((start[0] + last[0]) / 2, (start[1] + last[1]) / 2);
      for (let i = 0; i < points.length; i++) {
        const a = points[i], b = points[(i + 1) % points.length];
        ctx.quadraticCurveTo(a[0], a[1], (a[0] + b[0]) / 2, (a[1] + b[1]) / 2);
      }
      ctx.closePath(); ctx.fill(); ctx.restore();
    });
  }

  class Animator {
    constructor(canvas, models) {
      this.canvas = canvas; this.ctx = canvas.getContext('2d'); this.models = models;
      this.current = copy(models[0]); this.from = this.current; this.target = this.current;
      this.index = 0; this.enabled = true; this.reduced = false; this.transitionAt = -1000;
      this.reactionAt = performance.now(); this.blinkAt = -10000; this.nextBlink = performance.now() + 2800;
      this.pointer = { x: 0, y: 0 }; this.look = { x: 0, y: 0 }; this.frameId = null;
      this.loop = this.loop.bind(this);
      this.resize();
      if (typeof ResizeObserver !== 'undefined') this.observer = new ResizeObserver(() => this.resize());
      if (this.observer) this.observer.observe(canvas);
      this.start();
    }
    resize() {
      const rect = this.canvas.getBoundingClientRect(), dpr = Math.min(globalThis.devicePixelRatio || 1, 2);
      this.canvas.width = Math.max(1, Math.round(rect.width * dpr)); this.canvas.height = Math.max(1, Math.round(rect.height * dpr));
      this.paint(performance.now());
    }
    select(index) {
      this.from = copy(this.current);
      this.target = this.models[index].map((points, slot) => align(this.from[slot], points));
      this.index = index; this.transitionAt = this.reactionAt = performance.now();
      if (this.reduced) { this.current = copy(this.target); this.transitionAt -= 1000; }
      this.start();
    }
    replay() { this.reactionAt = performance.now(); this.blink(); }
    settle() {
      // Voice feedback stays visible even if the mobile browser delays animation frames.
      this.current = copy(this.target); this.from = copy(this.target);
      this.transitionAt = performance.now() - 800;
      this.paint(performance.now()); this.start();
    }
    blink() { this.blinkAt = performance.now(); this.nextBlink = this.blinkAt + 3200; this.start(); }
    setMotion(enabled, reduced = this.reduced) {
      this.enabled = enabled; this.reduced = reduced;
      if (!enabled || reduced) this.pointer = { x: 0, y: 0 };
      if (reduced) this.current = copy(this.target);
      this.start();
    }
    setLook(x, y) { if (this.enabled && !this.reduced) this.pointer = { x: clamp(x, -1, 1) * 6, y: clamp(y, -1, 1) * 4 }; }
    paint(now) {
      const progress = clamp((now - this.transitionAt) / 700, 0, 1);
      const t = progress * progress * (3 - 2 * progress);
      this.current = interpolate(this.from, this.target, this.reduced ? 1 : t);
      this.look.x += (this.pointer.x - this.look.x) * .09; this.look.y += (this.pointer.y - this.look.y) * .09;
      let blink = 1;
      if (!this.reduced) {
        if (this.enabled && now > this.nextBlink) {
          this.blinkAt = now; this.nextBlink = now + 3200 + Math.random() * 2200;
        }
        const elapsed = now - this.blinkAt, duration = this.index === 5 ? 480 : 190;
        if (elapsed >= 0 && elapsed < duration) blink = 1 - Math.sin(elapsed / duration * Math.PI) * .94;
      }
      draw(this.ctx, this.current, this.canvas.width, this.canvas.height, {
        index: this.index, seconds: Math.max(0, (now - this.reactionAt) / 1000), enabled: this.enabled && !this.reduced,
        blink, lookX: this.enabled && !this.reduced ? this.look.x : 0, lookY: this.enabled && !this.reduced ? this.look.y : 0
      });
    }
    loop(now) {
      this.frameId = null;
      if (typeof document !== 'undefined' && document.hidden) return;
      this.paint(now);
      if ((this.enabled && !this.reduced) || now - this.transitionAt < 710 || now - this.blinkAt < 490 || Math.abs(this.look.x - this.pointer.x) > .01 || Math.abs(this.look.y - this.pointer.y) > .01) this.start();
    }
    start() { if (this.frameId === null) this.frameId = requestAnimationFrame(this.loop); }
  }

  return { extractFeatures, align, interpolate, draw, reaction, Animator };
});
