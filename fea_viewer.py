"""
Interactive viewer for a finite-element run of actuator_fea.py.

    python fea_viewer.py results/run

writes results/run/viewer.html: one self-contained page (works offline) with a
pressure slider, the deformed actuator in 3-D, and the tip deflection and tip
angle curves compared with the beam model of actuator_model.py.
"""
from __future__ import annotations
import base64
import json
import sys
from pathlib import Path
import numpy as np
from plotly.offline import get_plotlyjs

from actuator_model import Geometry, Material, linear_response, hyperelastic_response


def surface(X, conn):
    """Boundary faces of a hex27 mesh.  Returns (faces (n, 9) as 3x3 grids, element of
    each face, mask of faces lying in the symmetry plane z = 0)."""
    c = conn.reshape(-1, 3, 3, 3)
    faces = np.concatenate([c[:, :, :, 0], c[:, :, :, 2], c[:, :, 0, :], c[:, :, 2, :],
                            c[:, 0, :, :], c[:, 2, :, :]]).reshape(-1, 9)
    elem = np.tile(np.arange(len(conn)), 6)
    outer = np.bincount(faces[:, 4])[faces[:, 4]] == 1      # the mid-face node is shared by two elements
    faces, elem = faces[outer], elem[outer]
    sym = (np.abs(X[faces, 2]) < 1e-6 * np.ptp(X[:, 0])).all(axis=1)
    order = np.argsort(sym, kind="stable")                  # exterior faces first
    return faces[order], elem[order], sym[order]


def triangles(faces):
    """Eight triangles per quad9 face, so that curved faces stay curved."""
    q = [(a + 3 * b, a + 1 + 3 * b, a + 4 + 3 * b, a + 3 + 3 * b) for b in range(2) for a in range(2)]
    tri = [faces[:, [n0, n1, n2]] for n0, n1, n2, n3 in q] + [faces[:, [n0, n2, n3]] for n0, n1, n2, n3 in q]
    return np.stack(tri, 1).reshape(-1, 3)                  # face f owns triangles 8f .. 8f+7


def outline(faces, sym):
    """Edges where the cut plane meets the exterior surface, as (start, mid, end) node triples."""
    e = faces[:, [[0, 1, 2], [2, 5, 8], [8, 7, 6], [6, 3, 0]]].reshape(-1, 3)
    s = np.repeat(sym, 4)
    n = e[:, 1].max() + 1
    both = (np.bincount(e[s, 1], minlength=n) > 0) & (np.bincount(e[~s, 1], minlength=n) > 0)
    e = e[np.unique(e[:, 1], return_index=True)[1]]
    return e[both[e[:, 1]]]


def b64(a, dtype):
    return base64.b64encode(np.ascontiguousarray(a, dtype=dtype).tobytes()).decode()


def beam_curves(g, mat, P):
    """Tier 1/2 (linear beam, arc kinematics) and Tier 3 (hyperelastic section) at pressures P [MPa]."""
    lin = linear_response(P, g, mat.E)
    out = dict(P=(1e3 * P).tolist(), t1_theta=np.degrees(lin["theta"]).tolist(), t1_delta=lin["delta_arc"].tolist())
    th, de = [], []
    for p in P:                                             # Tier 3 stops converging at high pressure
        try:
            h = hyperelastic_response(p, g, mat)
        except RuntimeError:
            break
        th.append(float(np.degrees(h["theta"][0])))
        de.append(float(h["delta"][0]))
    out.update(t3_P=(1e3 * P[:len(th)]).tolist(), t3_theta=th, t3_delta=de)
    return out


def build(run_dir):
    run_dir = Path(run_dir)
    info = json.loads((run_dir / "run.json").read_text())
    z = np.load(run_dir / "results.npz")
    X, conn, u, lam = z["X"], z["conn"], z["u"], z["lam_elem"]
    tab = np.genfromtxt(run_dir / "summary.csv", delimiter=",", names=True, dtype=None, encoding="utf-8")
    geo, m = info["geometry"], info["material"]
    g, mat = Geometry(geo["R"], geo["r"], geo["rp"], geo["L"]), Material(m["C10"], m["C20"], m["C30"])

    faces, elem, sym = surface(X, conn)
    nodes, inv = np.unique(faces, return_inverse=True)      # keep only surface nodes
    faces = inv.reshape(faces.shape)
    data = dict(
        nv=len(nodes), ns=len(u), n_ext_tri=int(8 * (~sym).sum()),
        X=b64(X[nodes], np.float32), U=b64(u[:, nodes], np.float32),
        tri=b64(triangles(faces), np.int32), lam=b64(lam[:, elem], np.float32),
        outline=b64(outline(faces, sym), np.int32),
        lam_hi=float(lam.max()),
        P=tab["P_kPa"].tolist(), delta=tab["delta_mm"].tolist(), theta=tab["theta_deg"].tolist(),
        x_tip=tab["x_tip_mm"].tolist(), vol=(tab["V_mm3"] / tab["V_mm3"][0]).tolist(),
        lam_max=tab["lam_max"].tolist(), wall=tab["wall_thin"].tolist(), mode=tab["mode"].tolist(),
        P_limit=info.get("P_limit_kPa"), E=m["E"], mu=m["E"] / 3,
        beam=beam_curves(g, mat, np.linspace(0, tab["P_kPa"].max() / 1e3, 61)))

    law = "neo-Hookean" if m["C20"] == 0 and m["C30"] == 0 else "Yeoh"
    sub = (f"R = {geo['R']:g}, r = {geo['r']:g}, r<sub>p</sub> = {geo['rp']:g}, L = {geo['L']:g} mm"
           f" &middot; {law}, E = {m['E']:.3g} MPa &middot; "
           f"{'hoop-fibre sleeve' if info.get('sleeve') else 'bare (no reinforcement)'}"
           f" &middot; {len(conn)} elements ({info.get('mesh', 'custom')} mesh)")
    rows = "".join(
        f"<tr><td>{r['P_kPa']:.2f}</td><td>{r['delta_mm']:.3f}</td><td>{r['theta_deg']:.3f}</td>"
        f"<td>{r['x_tip_mm']:.2f}</td><td>{r['V_mm3'] / tab['V_mm3'][0]:.3f}</td><td>{r['lam_max']:.3f}</td>"
        f"<td>{100 * r['wall_thin']:.1f}</td><td>{r['mode']}</td></tr>" for r in tab)
    html = (TEMPLATE.replace("__SUB__", sub).replace("__ROWS__", rows)
            .replace("__DATA__", json.dumps(data)).replace("__PLOTLY__", get_plotlyjs()))
    out = run_dir / "viewer.html"
    out.write_text(html, encoding="utf-8")
    return out


TEMPLATE = r"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Actuator FE viewer</title>
<style>
:root {
  color-scheme: light;
  --page: #f9f9f7; --surface: #fcfcfb; --ink: #0b0b0b; --ink-2: #52514e; --muted: #898781;
  --grid: #e1e0d9; --axis: #c3c2b7; --border: rgba(11,11,11,0.10);
  --fe: #2a78d6; --beam: #eb6834; --hyper: #1baf7a; --critical: #d03b3b;
}
@media (prefers-color-scheme: dark) {
  :root {
    color-scheme: dark;
    --page: #0d0d0d; --surface: #1a1a19; --ink: #ffffff; --ink-2: #c3c2b7; --muted: #898781;
    --grid: #2c2c2a; --axis: #383835; --border: rgba(255,255,255,0.10);
    --fe: #3987e5; --beam: #d95926; --hyper: #199e70; --critical: #d03b3b;
  }
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--page); color: var(--ink); font: 14px/1.45 system-ui, -apple-system, "Segoe UI", sans-serif; }
main { max-width: 1280px; margin: 0 auto; padding: 20px 16px 40px; }
h1 { font-size: 20px; margin: 0 0 2px; font-weight: 600; }
.sub { color: var(--ink-2); margin: 0 0 16px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; }
.controls { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 20px; padding: 12px 16px; margin-bottom: 12px; }
.controls label { color: var(--ink-2); }
.controls input[type=range] { flex: 1 1 260px; min-width: 200px; accent-color: var(--fe); }
.controls output { font-weight: 600; min-width: 9ch; }
button { font: inherit; color: var(--ink); background: transparent; border: 1px solid var(--axis); border-radius: 6px; padding: 5px 14px; cursor: pointer; }
button:hover { background: var(--grid); }
.tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 12px; }
.tile { padding: 10px 14px; }
.tile .k { color: var(--ink-2); font-size: 12px; }
.tile .v { font-size: 24px; font-weight: 600; }
.tile .u { color: var(--muted); font-size: 13px; font-weight: 400; margin-left: 3px; }
.grid { display: grid; grid-template-columns: minmax(0, 3fr) minmax(0, 2fr); gap: 12px; }
@media (max-width: 900px) { .grid { grid-template-columns: minmax(0, 1fr); } }
.panel { padding: 10px 12px 6px; }
.panel h2 { font-size: 14px; font-weight: 600; margin: 2px 4px 0; }
.panel .hint { color: var(--muted); font-size: 12px; margin: 0 4px; }
#view3d { height: 520px; }
.chart { height: 248px; }
.note { margin: 12px 0 0; padding: 10px 14px; color: var(--ink-2); }
.note b { color: var(--ink); }
details { margin-top: 12px; padding: 10px 14px; }
summary { cursor: pointer; font-weight: 600; }
table { border-collapse: collapse; width: 100%; margin-top: 8px; font-variant-numeric: tabular-nums; }
th, td { text-align: right; padding: 3px 10px; border-bottom: 1px solid var(--grid); white-space: nowrap; }
th { color: var(--ink-2); font-weight: 500; }
.scroll { overflow-x: auto; }
</style>
</head>
<body>
<main>
  <h1>Eccentric-cavity actuator: finite-element pressure sweep</h1>
  <p class="sub">__SUB__</p>

  <div class="card controls">
    <button id="play" type="button">Play</button>
    <label for="step">Pressure</label>
    <input id="step" type="range" min="0" value="0" step="1">
    <output id="plabel"></output>
    <label><input id="full" type="checkbox"> show both halves</label>
  </div>

  <div class="tiles">
    <div class="card tile"><div class="k">Cavity pressure</div><div class="v"><span id="t_p"></span><span class="u">kPa</span></div></div>
    <div class="card tile"><div class="k">Tip deflection &delta;</div><div class="v"><span id="t_d"></span><span class="u">mm</span></div></div>
    <div class="card tile"><div class="k">Tip angle &theta;</div><div class="v"><span id="t_t"></span><span class="u">deg</span></div></div>
    <div class="card tile"><div class="k">Cavity volume</div><div class="v"><span id="t_v"></span><span class="u">&times; initial</span></div></div>
    <div class="card tile"><div class="k">Peak stretch &lambda;<sub>max</sub></div><div class="v"><span id="t_l"></span></div></div>
    <div class="card tile"><div class="k">Thin wall thickness</div><div class="v"><span id="t_w"></span><span class="u">% of initial</span></div></div>
  </div>

  <div class="grid">
    <div class="card panel">
      <h2>Deformed shape</h2>
      <p class="hint">Cut through the bending plane; colour is the largest principal stretch; grey outline is the unloaded shape. Drag to rotate.</p>
      <div id="view3d"></div>
    </div>
    <div>
      <div class="card panel" style="margin-bottom:12px">
        <h2>Tip deflection &delta; [mm] against pressure [kPa]</h2>
        <div id="c_delta" class="chart"></div>
      </div>
      <div class="card panel">
        <h2>Tip angle &theta; [deg] against pressure [kPa]</h2>
        <div id="c_theta" class="chart"></div>
      </div>
    </div>
  </div>

  <div class="card note" id="note"></div>

  <details class="card">
    <summary>Data table (all converged states)</summary>
    <div class="scroll"><table>
      <thead><tr><th>P [kPa]</th><th>&delta; [mm]</th><th>&theta; [deg]</th><th>x<sub>tip</sub> [mm]</th><th>V / V<sub>0</sub></th><th>&lambda;<sub>max</sub></th><th>thin wall [%]</th><th>control</th></tr></thead>
      <tbody>__ROWS__</tbody>
    </table></div>
  </details>
</main>

<script>__PLOTLY__</script>
<script>
const D = __DATA__;
const bytes = s => Uint8Array.from(atob(s), c => c.charCodeAt(0)).buffer;
const X = new Float32Array(bytes(D.X)), U = new Float32Array(bytes(D.U)), LAM = new Float32Array(bytes(D.lam));
const TRI = new Int32Array(bytes(D.tri));
const OUTLINE = new Int32Array(bytes(D.outline));
const nv = D.nv, ns = D.ns, nTri = TRI.length / 3, nFace = nTri / 8;
const css = k => getComputedStyle(document.documentElement).getPropertyValue(k).trim();
const fmt = (v, d) => Number(v).toFixed(d);
let step = 0, full = false, timer = null;

// model axes (x along the actuator, y in the bending plane, z out of it) -> plot axes (x, z, y)
function coords(s, mirror) {
  const n = mirror ? 2 * nv : nv, x = new Array(n), y = new Array(n), z = new Array(n);
  for (let i = 0; i < nv; i++) {
    const o = 3 * i, q = 3 * (s * nv + i);                 // s < 0: unloaded shape
    x[i] = X[o] + (s < 0 ? 0 : U[q]); z[i] = X[o + 1] + (s < 0 ? 0 : U[q + 1]); y[i] = X[o + 2] + (s < 0 ? 0 : U[q + 2]);
    if (mirror) { x[nv + i] = x[i]; z[nv + i] = z[i]; y[nv + i] = -y[i]; }
  }
  return {x, y, z};
}
function cutOutline(c) {                    // drawn just in front of the cut plane so it is not hidden by it
  const x = [], y = [], z = [];
  for (let e = 0; e < OUTLINE.length; e += 3) {
    for (let k = 0; k < 3; k++) { const n = OUTLINE[e + k]; x.push(c.x[n]); y.push(-0.15); z.push(c.z[n]); }
    x.push(null); y.push(null); z.push(null);
  }
  return {x, y, z};
}
function topology() {                       // half: all faces; full: exterior faces of both halves
  const n = full ? D.n_ext_tri : nTri, m = full ? 2 * n : n, i = new Array(m), j = new Array(m), k = new Array(m);
  for (let t = 0; t < n; t++) {
    i[t] = TRI[3 * t]; j[t] = TRI[3 * t + 1]; k[t] = TRI[3 * t + 2];
    if (full) { i[n + t] = i[t] + nv; j[n + t] = j[t] + nv; k[n + t] = k[t] + nv; }
  }
  return {i, j, k};
}
function intensity(s) {
  const n = full ? D.n_ext_tri : nTri, v = new Array(full ? 2 * n : n);
  for (let t = 0; t < n; t++) { v[t] = LAM[s * nFace + (t >> 3)]; if (full) v[n + t] = v[t]; }
  return v;
}

function draw3d() {
  const ink2 = css('--ink-2'), c = coords(step, full), ref = coords(-1, full), t = topology();
  const lines = cutOutline(c), ghost = cutOutline(ref);
  // fixed box over all states so that the motion is visible
  let lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
  for (let s = 0; s < ns; s++) { const a = coords(s, true); [a.x, a.y, a.z].forEach((v, d) => { for (const q of v) { if (q < lo[d]) lo[d] = q; if (q > hi[d]) hi[d] = q; } }); }
  const axis = d => ({range: [lo[d] - 2, hi[d] + 2], showbackground: false, gridcolor: css('--grid'), zerolinecolor: css('--axis'),
                      color: ink2, title: {text: ['x [mm]', 'z [mm]', 'y [mm]'][d], font: {size: 11}}, tickfont: {size: 10}});
  Plotly.react('view3d', [
    {type: 'mesh3d', x: c.x, y: c.y, z: c.z, i: t.i, j: t.j, k: t.k, intensity: intensity(step), intensitymode: 'cell',
     cmin: 1, cmax: D.lam_hi, colorscale: [[0, '#cde2fb'], [0.25, '#86b6ef'], [0.5, '#3987e5'], [0.75, '#1c5cab'], [1, '#0d366b']],
     colorbar: {title: {text: 'stretch', font: {size: 11}}, thickness: 10, len: 0.6, tickfont: {size: 10, color: ink2}, outlinewidth: 0},
     flatshading: false, lighting: {ambient: 0.8, diffuse: 0.5, specular: 0.05, roughness: 0.9}, hoverinfo: 'skip'},
    {type: 'scatter3d', mode: 'lines', x: lines.x, y: lines.y, z: lines.z, line: {color: css('--ink'), width: 2}, hoverinfo: 'skip', visible: !full},
    {type: 'scatter3d', mode: 'lines', x: ghost.x, y: ghost.y, z: ghost.z, line: {color: css('--muted'), width: 2}, hoverinfo: 'skip'}
  ], {
    margin: {l: 0, r: 0, t: 0, b: 0}, showlegend: false, paper_bgcolor: 'rgba(0,0,0,0)', font: {color: ink2, family: 'system-ui, sans-serif'},
    uirevision: 'keep',
    scene: {aspectmode: 'data', xaxis: axis(0), yaxis: axis(1), zaxis: axis(2), camera: {projection: {type: 'orthographic'}, eye: {x: 0.3, y: -3.5, z: 0.65}, up: {x: 0, y: 0, z: 1}}}
  }, {displaylogo: false, responsive: true});
}
function update3d() {
  const c = coords(step, full), lines = cutOutline(c);
  Plotly.restyle('view3d', {x: [c.x], y: [c.y], z: [c.z], intensity: [intensity(step)]}, [0]);
  Plotly.restyle('view3d', {x: [lines.x], y: [lines.y], z: [lines.z]}, [1]);
}

function drawChart(id, key, beamKey, unit, digits) {
  const ink2 = css('--ink-2'), surf = css('--surface');
  const traces = [
    {name: 'Finite element', x: D.P, y: D[key], mode: 'lines+markers', line: {color: css('--fe'), width: 2}, marker: {size: 6},
     hovertemplate: '%{y:.' + digits + 'f} ' + unit + '<extra>FE</extra>'},
    {name: 'Beam, Tier 1-2', x: D.beam.P, y: D.beam['t1_' + beamKey], mode: 'lines', line: {color: css('--beam'), width: 2},
     hovertemplate: '%{y:.' + digits + 'f} ' + unit + '<extra>Tier 1-2</extra>'},
    {name: 'Beam, Tier 3', x: D.beam.t3_P, y: D.beam['t3_' + beamKey], mode: 'lines', line: {color: css('--hyper'), width: 2},
     hovertemplate: '%{y:.' + digits + 'f} ' + unit + '<extra>Tier 3</extra>'},
    {name: 'Current state', x: [D.P[step]], y: [D[key][step]], mode: 'markers', showlegend: false, hoverinfo: 'skip',
     marker: {size: 13, color: css('--fe'), line: {color: surf, width: 2}}}
  ];
  const yMax = 1.15 * Math.max(...D[key]);
  const layout = {
    margin: {l: 44, r: 12, t: 26, b: 34}, paper_bgcolor: 'rgba(0,0,0,0)', plot_bgcolor: 'rgba(0,0,0,0)',
    font: {color: ink2, size: 11, family: 'system-ui, sans-serif'}, hovermode: 'x unified',
    legend: {orientation: 'h', x: 0, y: 1.14, font: {size: 11}},
    xaxis: {gridcolor: css('--grid'), linecolor: css('--axis'), zeroline: false, rangemode: 'tozero'},
    yaxis: {gridcolor: css('--grid'), linecolor: css('--axis'), zerolinecolor: css('--axis'), range: [0, yMax]},
    shapes: [], annotations: []
  };
  if (D.P_limit != null) {
    layout.shapes.push({type: 'line', x0: D.P_limit, x1: D.P_limit, yref: 'paper', y0: 0, y1: 1, line: {color: css('--critical'), width: 1}});
    layout.annotations.push({x: D.P_limit, yref: 'paper', y: 0.02, text: 'ballooning limit ', xanchor: 'right', yanchor: 'bottom',
                             showarrow: false, font: {size: 10, color: ink2}});
  }
  Plotly.react(id, traces, layout, {displayModeBar: false, responsive: true});
}

function setStep(s) {
  step = s;
  document.getElementById('step').value = s;
  document.getElementById('plabel').textContent = fmt(D.P[s], 1) + ' kPa';
  document.getElementById('t_p').textContent = fmt(D.P[s], 1);
  document.getElementById('t_d').textContent = fmt(D.delta[s], 2);
  document.getElementById('t_t').textContent = fmt(D.theta[s], 2);
  document.getElementById('t_v').textContent = fmt(D.vol[s], 2);
  document.getElementById('t_l').textContent = fmt(D.lam_max[s], 2);
  document.getElementById('t_w').textContent = fmt(100 * D.wall[s], 0);
  update3d();
  Plotly.restyle('c_delta', {x: [[D.P[s]]], y: [[D.delta[s]]]}, [3]);
  Plotly.restyle('c_theta', {x: [[D.P[s]]], y: [[D.theta[s]]]}, [3]);
}
function drawAll() {
  draw3d();
  drawChart('c_delta', 'delta', 'delta', 'mm', 2);
  drawChart('c_theta', 'theta', 'theta', 'deg', 2);
  setStep(step);
}

document.getElementById('step').max = ns - 1;
document.getElementById('step').addEventListener('input', e => setStep(+e.target.value));
document.getElementById('full').addEventListener('change', e => { full = e.target.checked; draw3d(); });
document.getElementById('play').addEventListener('click', e => {
  if (timer) { clearInterval(timer); timer = null; e.target.textContent = 'Play'; return; }
  e.target.textContent = 'Pause';
  if (step === ns - 1) setStep(0);
  timer = setInterval(() => {
    if (step >= ns - 1) { clearInterval(timer); timer = null; e.target.textContent = 'Play'; return; }
    setStep(step + 1);
  }, 350);
});
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', drawAll);
window.addEventListener('resize', () => ['view3d', 'c_delta', 'c_theta'].forEach(id => Plotly.Plots.resize(id)));

const mu = 1e3 * D.mu;
document.getElementById('note').innerHTML = D.P_limit != null
  ? '<b>Ballooning limit: ' + fmt(D.P_limit, 1) + ' kPa</b> (P / &mu; = ' + fmt(D.P_limit / mu, 3) + '). Above this pressure the model has no stable equilibrium: '
    + 'the thin wall inflates without bound. States beyond the peak were traced by prescribing the cavity volume instead of the pressure.'
  : 'No ballooning limit was reached in this pressure range (highest pressure ' + fmt(Math.max(...D.P), 1) + ' kPa, P / &mu; = ' + fmt(Math.max(...D.P) / mu, 3) + ').';
drawAll();
</script>
</body>
</html>
"""


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    print(f"wrote {build(sys.argv[1])}")
