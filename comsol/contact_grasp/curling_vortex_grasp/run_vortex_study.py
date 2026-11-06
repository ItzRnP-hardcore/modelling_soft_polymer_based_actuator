import os
import subprocess
import re
import pandas as pd
import matplotlib.pyplot as plt

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp\curling_vortex_grasp"
class_file = os.path.join(base_dir, "BuildCurlingVortexGraspStudy.class")
csv_path = os.path.join(base_dir, "curling_vortex_grasp_summary.csv")
img_dir = os.path.join(base_dir, "images")
os.makedirs(img_dir, exist_ok=True)

comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("=======================================================")
print("  LAUNCHING CURLING VORTEX GRASP BATCH RUNNER")
print("=======================================================")

cmd = [comsol_batch, "-inputfile", class_file]
print(f"Executing: {' '.join(cmd)}")

proc = subprocess.Popen(
    cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    cwd=base_dir
)

records = []
pattern = re.compile(
    r">> VORTEX_ROW:\s*Case=(?P<Case>[\w\-]+),\s*R=(?P<R>[\d\.]+),\s*Xc=(?P<Xc>[\d\.\-]+),\s*Yc=(?P<Yc>[\d\.\-]+),\s*P=(?P<P>[\d\.]+),\s*ux=(?P<ux>[\d\.\-]+),\s*uy=(?P<uy>[\d\.\-]+),\s*uMag=(?P<uMag>[\d\.]+),\s*Fn=(?P<Fn>[\d\.]+),\s*MaxTn=(?P<MaxTn>[\d\.]+),\s*Ffric=(?P<Ffric>[\d\.]+)"
)

for line in iter(proc.stdout.readline, ''):
    print(line, end='', flush=True)
    m = pattern.search(line)
    if m:
        d = m.groupdict()
        records.append({
            'Case': d['Case'],
            'R_mm': float(d['R']),
            'Xc_mm': float(d['Xc']),
            'Yc_mm': float(d['Yc']),
            'P_kPa': float(d['P']),
            'ux_mm': float(d['ux']),
            'uy_mm': float(d['uy']),
            'uMag_mm': float(d['uMag']),
            'Fn_N': float(d['Fn']),
            'MaxTn_kPa': float(d['MaxTn']),
            'Ffric_N': float(d['Ffric'])
        })

proc.wait()
print(f"\nBatch job finished with exit code {proc.returncode}")

if records:
    df = pd.DataFrame(records)
    df.to_csv(csv_path, index=False)
    print(f"\n[CSV SAVED] {len(df)} records written to: {csv_path}")
    print(df.to_string())

    # Generate Publication Comparison Plot
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5), dpi=150)

    colors = {
        'R10mm_Vortex_Xc50_Yc-55': '#008000',    # Deep green
        'R8mm_Vortex_Xc52_Yc-58': '#FF8C00',     # Dark orange
        'R12mm_Vortex_Xc48_Yc-52': '#1E90FF',    # Dodger blue
        'R12mm_Baseline_Xc42_Yc-25': '#DC143C'   # Crimson (Baseline)
    }
    labels = {
        'R10mm_Vortex_Xc50_Yc-55': 'R=10mm @ Curl Vortex (Xc=50, Yc=-55)',
        'R8mm_Vortex_Xc52_Yc-58': 'R=8mm @ Tight Vortex (Xc=52, Yc=-58)',
        'R12mm_Vortex_Xc48_Yc-52': 'R=12mm @ Curl Vortex (Xc=48, Yc=-52)',
        'R12mm_Baseline_Xc42_Yc-25': 'R=12mm @ Baseline Underbelly (Xc=42, Yc=-25)'
    }
    styles = {
        'R10mm_Vortex_Xc50_Yc-55': ('-o', 2.5),
        'R8mm_Vortex_Xc52_Yc-58': ('-s', 2.5),
        'R12mm_Vortex_Xc48_Yc-52': ('-^', 2.5),
        'R12mm_Baseline_Xc42_Yc-25': ('--x', 2.0)
    }

    # Left plot: Normal contact force vs pressure
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = styles.get(cName, ('-o', 2.0))
        ax1.plot(grp['P_kPa'], grp['Fn_N'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax1.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax1.set_ylabel('Normal Contact Clamping Force $F_N$ [N]', fontsize=11, fontweight='bold')
    ax1.set_title('Normal Grasp Force: Curling Vortex vs. Proximal Baseline', fontsize=12, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=9, loc='upper left')

    # Right plot: Frictional capacity & self-centering cylinder shift
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = styles.get(cName, ('-o', 2.0))
        ax2.plot(grp['P_kPa'], grp['uMag_mm'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax2.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax2.set_ylabel('Floating Cylinder Inward Shift $\\Delta r$ [mm]', fontsize=11, fontweight='bold')
    ax2.set_title('Dynamic Floating Object Enclosure & Self-Centering', fontsize=12, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=9, loc='upper left')

    plt.tight_layout()
    chart_path = os.path.join(img_dir, "curling_vortex_grasp_comparison.png")
    fig.savefig(chart_path, bbox_inches='tight')
    print(f"\n[PLOT SAVED] Chart written to: {chart_path}")
else:
    print("[WARN] No records parsed from batch run.")

