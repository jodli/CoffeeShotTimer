// Interactive shot chart on the landing page.
// The recommendation mirrors CalculateGrindAdjustmentUseCase in the app;
// the shot outcomes are a made-up grinder model so the demo has somewhere to go.
(function () {
  const root = document.getElementById("demo");
  if (!root) return;

  const SVG_NS = "http://www.w3.org/2000/svg";
  const shotsLayer = root.querySelector("#shots");
  const trailsLayer = root.querySelector("#trails");
  const readout = root.querySelector("#readout");
  const verdict = root.querySelector("#verdict");
  const verdictMain = root.querySelector("#verdict-main");
  const verdictWhy = root.querySelector("#verdict-why");
  const resetBtn = root.querySelector("#reset");
  const buttons = Array.from(root.querySelectorAll("[data-taste]"));
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // same thresholds as the app
  const OPTIMAL_MIN = 25;
  const OPTIMAL_MAX = 30;
  const STEP = 0.1;
  const SCALE_MIN = 1.0;
  const SCALE_MAX = 5.0;
  const MAX_SHOTS = 7;

  // demo grinder: 0.1 finer adds about 2.5 s
  const START = { grind: 3.0, time: 19, dose: 18, ratio: 2.11 };
  const RATIOS = [2.11, 1.94, 2.28, 2.06, 2.35, 1.98, 2.2];
  const JITTER = [0, 0.4, -0.6, 0.5, -0.3, 0.2, -0.4];

  // plot mapping, matches the static axes in index.html
  const x = (t) => 64 + ((Math.min(45, Math.max(10, t)) - 10) / 35) * 636;
  const y = (r) => 440 - ((Math.min(3, Math.max(1.5, r)) - 1.5) / 1.5) * 416;

  let shots = [];
  let done = false;

  function simulate(index, grind) {
    const t = START.time + (START.grind - grind) * 25 + JITTER[index % JITTER.length];
    return {
      grind,
      time: Math.max(8, Math.round(t)),
      dose: START.dose,
      ratio: RATIOS[index % RATIOS.length],
    };
  }

  function deviation(time) {
    if (time < OPTIMAL_MIN) return time - OPTIMAL_MIN;
    if (time > OPTIMAL_MAX) return time - OPTIMAL_MAX;
    return 0;
  }

  function stepsFor(dev) {
    const d = Math.abs(dev);
    if (d <= 3) return 1;
    if (d <= 6) return 2;
    return 3;
  }

  // taste first, timing second, same order as the app
  function recommend(shot, taste) {
    const dev = deviation(shot.time);
    let direction = "none";
    if (taste === "bitter") direction = "coarser";
    else if (taste === "sour") direction = "finer";
    else if (shot.time < OPTIMAL_MIN) direction = "finer";
    else if (shot.time > OPTIMAL_MAX) direction = "coarser";

    if (direction === "none") return { direction, steps: 0, grind: shot.grind, dev };

    const wanted = stepsFor(dev);
    const raw = direction === "finer" ? shot.grind - wanted * STEP : shot.grind + wanted * STEP;
    const grind = Math.round(Math.min(SCALE_MAX, Math.max(SCALE_MIN, raw)) * 10) / 10;
    const steps = Math.round(Math.abs(grind - shot.grind) / STEP);
    return { direction: steps === 0 ? "none" : direction, steps, grind, dev };
  }

  const fmt = (n) => n.toFixed(1);
  const out = (s) => Math.round(s.dose * s.ratio);

  function describe(shot, taste, rec) {
    const tasteText = {
      sour: "Under-extracted (Sour)",
      bitter: "Over-extracted (Bitter)",
      perfect: "Good extraction",
    }[taste];
    let timing;
    if (rec.dev < 0) timing = `Shot ran ${-rec.dev}s too fast`;
    else if (rec.dev > 0) timing = `Shot ran ${rec.dev}s too slow`;
    else timing = `Good timing (${shot.time}s)`;
    const action = {
      finer: "Try grinding finer",
      coarser: "Try grinding coarser",
      none: "Keep current setting",
    }[rec.direction];
    return `${tasteText} - ${timing}. ${action}.`;
  }

  function headline(rec) {
    if (rec.direction === "none") return "Keep current setting";
    const unit = rec.steps === 1 ? "1 step" : `${rec.steps} steps`;
    return `${unit} ${rec.direction} → ${fmt(rec.grind)}`;
  }

  function el(name, attrs) {
    const node = document.createElementNS(SVG_NS, name);
    for (const k in attrs) node.setAttribute(k, attrs[k]);
    return node;
  }

  function drawShot(shot, index, opts) {
    const g = el("g", { class: "shot" + (opts.arrive ? " arrive" : "") });
    g.appendChild(el("circle", { cx: x(shot.time), cy: y(shot.ratio), r: 14 }));
    const label = el("text", { x: x(shot.time), y: y(shot.ratio) });
    label.textContent = String(index + 1);
    g.appendChild(label);
    shotsLayer.appendChild(g);
    return g;
  }

  function drawTrail(a, b, animate) {
    const path = el("path", {
      class: "trail",
      d: `M${x(a.time)} ${y(a.ratio)} L${x(b.time)} ${y(b.ratio)}`,
    });
    trailsLayer.appendChild(path);
    if (animate) {
      const len = path.getTotalLength();
      path.style.setProperty("--len", len);
      path.classList.add("drawing");
      path.addEventListener("animationend", () => path.classList.remove("drawing"), { once: true });
    }
  }

  // faint sour-sour-perfect path so visitors who never tap still see the loop
  function drawGhost() {
    const pts = [shots[0]];
    for (let i = 1; i < 3; i++) {
      const prev = pts[i - 1];
      pts.push(simulate(i, recommend(prev, "sour").grind));
    }
    const g = el("g", { class: "ghost" });
    g.appendChild(el("path", {
      d: pts.map((p, i) => `${i ? "L" : "M"}${x(p.time)} ${y(p.ratio)}`).join(" "),
    }));
    pts.slice(1).forEach((p, i) => {
      g.appendChild(el("circle", { cx: x(p.time), cy: y(p.ratio), r: 14 }));
      const t = el("text", { x: x(p.time), y: y(p.ratio) });
      t.textContent = String(i + 2);
      g.appendChild(t);
    });
    trailsLayer.appendChild(g);
  }

  function setCurrent() {
    const nodes = shotsLayer.querySelectorAll(".shot");
    nodes.forEach((n, i) => n.classList.toggle("current", i === nodes.length - 1 && !done));
  }

  function renderReadout(shot, index) {
    readout.innerHTML =
      `<span class="label">Shot ${index + 1}</span>` +
      `Grind ${fmt(shot.grind)} · ${shot.dose} g → ${out(shot)} g (1:${shot.ratio.toFixed(1)}) · ${shot.time} s`;
  }

  function setButtons(enabled) {
    buttons.forEach((b) => (b.disabled = !enabled));
  }

  function choose(taste) {
    if (done) return;
    const index = shots.length - 1;
    const shot = shots[index];
    const rec = recommend(shot, taste);
    trailsLayer.querySelectorAll(".ghost").forEach((n) => n.remove());

    verdict.removeAttribute("data-empty");
    verdictWhy.hidden = false;
    resetBtn.hidden = false;
    verdictWhy.textContent = describe(shot, taste, rec);

    const inTarget = rec.dev === 0 && taste === "perfect";
    if (inTarget) {
      done = true;
      verdict.classList.add("is-dialed");
      verdictMain.textContent = `Dialed in at ${fmt(shot.grind)}`;
      verdictWhy.textContent += " The app saves this with the bean for next time.";
      shotsLayer.lastElementChild.classList.add("dialed");
      setCurrent();
      setButtons(false);
      return;
    }

    verdict.classList.remove("is-dialed");
    verdictMain.textContent = `Next shot: ${headline(rec)}`;

    if (shots.length >= MAX_SHOTS) {
      done = true;
      verdictWhy.textContent += " That’s enough shots for one demo.";
      setCurrent();
      setButtons(false);
      return;
    }

    const next = simulate(shots.length, rec.grind);
    shots.push(next);
    const animate = !reduceMotion;
    drawTrail(shot, next, animate);
    const node = drawShot(next, shots.length - 1, { arrive: false });
    if (animate) {
      node.style.opacity = "0";
      window.setTimeout(() => {
        node.style.opacity = "";
        node.classList.add("arrive");
      }, 520);
    }
    setCurrent();
    renderReadout(next, shots.length - 1);
  }

  function reset() {
    shots = [simulate(0, START.grind)];
    shots[0].time = START.time;
    done = false;
    shotsLayer.replaceChildren();
    trailsLayer.replaceChildren();
    drawGhost();
    drawShot(shots[0], 0, { arrive: false });
    setCurrent();
    renderReadout(shots[0], 0);
    verdict.setAttribute("data-empty", "");
    verdict.classList.remove("is-dialed");
    verdictMain.textContent = "How did it taste? Pick one to see the next grind.";
    verdictWhy.hidden = true;
    verdictWhy.textContent = "";
    resetBtn.hidden = true;
    setButtons(true);
  }

  buttons.forEach((b) => b.addEventListener("click", () => choose(b.dataset.taste)));
  resetBtn.addEventListener("click", () => {
    reset();
    buttons[0].focus();
  });

  reset();
})();
