import os
import subprocess
import re
import glob
import shutil
import pandas as pd
import matplotlib.pyplot as plt

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
class_file = os.path.join(base_dir, "BuildProperGraspStudy.class")
out_dir = os.path.join(base_dir, "proper_grasp_and_sleeve_optimization")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "proper_grasp_summary.csv")

os.makedirs(img_dir, exist_ok=True)

comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("================================================================================")
print("  LAUNCHING PROPER GRASP & FORCE APPLICATION COMSOL BATCH RUNNER")
print("================================================================================")

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
    r">> PROPER_GRASP_ROW:\s*Case=(?P<Case>[\w\-]+),\s*R_cyl=(?P<R_cyl>[\d\.]+),\s*Xc=(?P<Xc>[\d\.\-]+),\s*Yc=(?P<Yc>[\d\.\-]+),\s*P=(?P<P>[\d\.]+),\s*Fn=(?P<Fn>[\d\.]+),\s*Ffric=(?P<Ffric>[\d\.]+),\s*MaxTn=(?P<MaxTn>[\d\.]+),\s*VmMax=(?P<VmMax>[\d\.]+),\s*ux=(?P<ux>[\d\.\-]+),\s*uy=(?P<uy>[\d\.\-]+),\s*uMag=(?P<uMag>[\d\.]+),\s*xTip=(?P<xTip>[\d\.\-]+),\s*yTip=(?P<yTip>[\d\.\-]+),\s*WrapAngle=(?P<WrapAngle>[\d\.]+),\s*Enclosure=(?P<Enclosure>[\d\.]+)%"
)

for line in iter(proc.stdout.readline, ''):
    print(line, end='', flush=True)
    m = pattern.search(line)
    if m:
        d = m.groupdict()
        records.append({
            'Case': d['Case'],
            'R_cyl_mm': float(d['R_cyl']),
            'Xc_mm': float(d['Xc']),
            'Yc_mm': float(d['Yc']),
            'P_kPa': float(d['P']),
            'Fn_N': float(d['Fn']),
            'Ffric_N': float(d['Ffric']),
            'MaxTn_kPa': float(d['MaxTn']),
            'VmMax_kPa': float(d['VmMax']),
            'ux_mm': float(d['ux']),
            'uy_mm': float(d['uy']),
            'uMag_mm': float(d['uMag']),
            'xTip_mm': float(d['xTip']),
            'yTip_mm': float(d['yTip']),
            'WrapAngle_deg': float(d['WrapAngle']),
            'Enclosure_pct': float(d['Enclosure'])
        })

proc.wait()
print(f"\nBatch job finished with exit code {proc.returncode}")

if records:
    df = pd.DataFrame(records)
    df.to_csv(csv_path, index=False)
    print(f"\n[CSV SAVED] {len(df)} records written to: {csv_path}")
    print(df.to_string())

    # Publication Visualization: Wrap Angle & Normal Force
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6), dpi=150)

    colors = {
        'ProperGrasp_R12mm_CagingLoop': '#008000',    # Forest Green
        'ProperGrasp_R16mm_Enclosure': '#1E90FF',     # Dodger Blue
        'ProperGrasp_R20mm_FullSurround': '#8A2BE2'   # Blue Violet
    }
    labels = {
        'ProperGrasp_R12mm_CagingLoop': r'R=12mm at Cradle (Xc=48, Yc=-42) — Tight Caging Loop',
        'ProperGrasp_R16mm_Enclosure': r'R=16mm at Cradle (Xc=46, Yc=-36) — Circumferential Enclosure',
        'ProperGrasp_R20mm_FullSurround': r'R=20mm at Cradle (Xc=44, Yc=-32) — Full Surround Loop'
    }
    markers = {
        'ProperGrasp_R12mm_CagingLoop': ('-o', 2.5),
        'ProperGrasp_R16mm_Enclosure': ('-s', 2.5),
        'ProperGrasp_R20mm_FullSurround': ('-^', 2.5)
    }

    # Plot 1: Circumferential Wrap Angle vs Pressure
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax1.plot(grp['P_kPa'], grp['WrapAngle_deg'], fmt, color=c, lw=lw, ms=7, label=lbl)

    ax1.axhline(360.0, color='black', linestyle=':', lw=2.0, label=r'360° Complete Form-Closure Closure')
    ax1.axhline(270.0, color='gray', linestyle='--', lw=1.2, label=r'270° (3/4th Circumference) Threshold')
    ax1.fill_between([0, 100], 360, 420, color='green', alpha=0.08, label='Interlocking Cinch Zone')

    ax1.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax1.set_ylabel('Circumferential Wrap Angle $\\theta_{\\mathrm{wrap}}$ [deg]', fontsize=11, fontweight='bold')
    ax1.set_title('Circumferential Wrap Progression vs. Pressure', fontsize=12, fontweight='bold')
    ax1.set_xlim(-2, 102)
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=9, loc='lower right')

    # Plot 2: Normal Contact Clamping Force vs Pressure
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax2.plot(grp['P_kPa'], grp['Fn_N'], fmt, color=c, lw=lw, ms=7, label=lbl)

    ax2.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax2.set_ylabel('Normal Clamping Force $F_N$ [N]', fontsize=11, fontweight='bold')
    ax2.set_title('Normal Clamping Force vs. Pressure up to 100 kPa', fontsize=12, fontweight='bold')
    ax2.set_xlim(-2, 102)
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=9, loc='upper left')

    plt.tight_layout()
    chart_path = os.path.join(img_dir, "proper_grasp_mechanics.png")
    fig.savefig(chart_path, bbox_inches='tight')
    print(f"\n[PLOT SAVED] Chart written to: {chart_path}")

    # Move .mph solved files to out_dir
    for f in glob.glob(os.path.join(base_dir, "actuator_proper_*_solved.mph")):
        dest = os.path.join(out_dir, os.path.basename(f))
        shutil.move(f, dest)
        print(f"Moved {f} -> {dest}")

    # Copy images to out_dir/images
    for f in glob.glob(os.path.join(base_dir, "images", "actuator_proper_*.png")):
        dest = os.path.join(img_dir, os.path.basename(f))
        shutil.copy2(f, dest)
        print(f"Copied {f} -> {dest}")

else:
    print("[WARN] No records parsed from batch run.")

