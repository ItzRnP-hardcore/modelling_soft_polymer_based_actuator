import os
import subprocess
import re
import pandas as pd
import matplotlib.pyplot as plt

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
class_file = os.path.join(base_dir, "BuildFullSurroundMaxForceStudy.class")
out_dir = os.path.join(base_dir, "maximum_force_full_surround")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "full_surround_max_force_summary.csv")

os.makedirs(img_dir, exist_ok=True)

comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("================================================================================")
print("  LAUNCHING FULL-SURROUND (360 deg) & MAXIMUM FORCE COMSOL BATCH RUNNER")
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

# Pre-populate records with Case 0 (Std140mm_R10mm_HighPressure) which already solved & saved
records = [
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 0.0, 'Fn_N': 0.00000, 'Ffric_N': 0.00000, 'MaxTn_kPa': 0.00, 'VmMax_kPa': 0.00, 'ux_mm': 0.000, 'uy_mm': 0.000, 'uMag_mm': 0.000, 'xTip_mm': 140.0, 'yTip_mm': 0.0, 'WrapAngle_deg': 0.0, 'Enclosure_pct': 0.0},
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 20.0, 'Fn_N': 0.09880, 'Ffric_N': 0.04940, 'MaxTn_kPa': 1.75, 'VmMax_kPa': 63.12, 'ux_mm': -4.361, 'uy_mm': 0.036, 'uMag_mm': 4.361, 'xTip_mm': 39.0, 'yTip_mm': -97.3, 'WrapAngle_deg': 122.0, 'Enclosure_pct': 33.9},
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 40.0, 'Fn_N': 0.23852, 'Ffric_N': 0.11926, 'MaxTn_kPa': 2.45, 'VmMax_kPa': 92.63, 'ux_mm': -8.428, 'uy_mm': 5.572, 'uMag_mm': 10.103, 'xTip_mm': 13.7, 'yTip_mm': -71.5, 'WrapAngle_deg': 167.5, 'Enclosure_pct': 46.5},
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 60.0, 'Fn_N': 0.31848, 'Ffric_N': 0.15924, 'MaxTn_kPa': 2.63, 'VmMax_kPa': 124.66, 'ux_mm': -6.089, 'uy_mm': 11.537, 'uMag_mm': 13.045, 'xTip_mm': 13.8, 'yTip_mm': -49.9, 'WrapAngle_deg': 195.3, 'Enclosure_pct': 54.3},
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 80.0, 'Fn_N': 0.37934, 'Ffric_N': 0.18967, 'MaxTn_kPa': 2.77, 'VmMax_kPa': 279.72, 'ux_mm': -1.796, 'uy_mm': 15.437, 'uMag_mm': 15.541, 'xTip_mm': 20.7, 'yTip_mm': -36.8, 'WrapAngle_deg': 214.3, 'Enclosure_pct': 59.5},
    {'Case': 'Std140mm_R10mm_HighPressure', 'L_mm': 140.0, 'R_cyl_mm': 10.0, 'Xc_mm': 50.0, 'Yc_mm': -55.0, 'P_kPa': 100.0, 'Fn_N': 0.46902, 'Ffric_N': 0.23451, 'MaxTn_kPa': 2.97, 'VmMax_kPa': 559.63, 'ux_mm': 3.219, 'uy_mm': 16.715, 'uMag_mm': 17.023, 'xTip_mm': 29.6, 'yTip_mm': -29.7, 'WrapAngle_deg': 229.6, 'Enclosure_pct': 63.8}
]
pattern = re.compile(
    r">> FULL_SURROUND_ROW:\s*Case=(?P<Case>[\w\-]+),\s*L=(?P<L>[\d\.]+),\s*R_cyl=(?P<R_cyl>[\d\.]+),\s*Xc=(?P<Xc>[\d\.\-]+),\s*Yc=(?P<Yc>[\d\.\-]+),\s*P=(?P<P>[\d\.]+),\s*Fn=(?P<Fn>[\d\.]+),\s*Ffric=(?P<Ffric>[\d\.]+),\s*MaxTn=(?P<MaxTn>[\d\.]+),\s*VmMax=(?P<VmMax>[\d\.]+),\s*ux=(?P<ux>[\d\.\-]+),\s*uy=(?P<uy>[\d\.\-]+),\s*uMag=(?P<uMag>[\d\.]+),\s*xTip=(?P<xTip>[\d\.\-]+),\s*yTip=(?P<yTip>[\d\.\-]+),\s*WrapAngle=(?P<WrapAngle>[\d\.]+),\s*Enclosure=(?P<Enclosure>[\d\.]+)%"
)

for line in iter(proc.stdout.readline, ''):
    print(line, end='', flush=True)
    m = pattern.search(line)
    if m:
        d = m.groupdict()
        records.append({
            'Case': d['Case'],
            'L_mm': float(d['L']),
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

    # Publication Visualization 1: Wrap Angle & Normal Force
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 11), dpi=150)

    colors = {
        'Std140mm_R10mm_HighPressure': '#DC143C',          # Crimson
        'FullSurround200mm_R10mm_CinchLoop': '#008000',    # Deep Green
        'FullSurround200mm_R14mm_Enclosure': '#1E90FF',    # Dodger Blue
        'FullSurround200mm_R18mm_LargePayload': '#FF8C00'  # Dark Orange
    }
    labels = {
        'Std140mm_R10mm_HighPressure': 'Std 140mm (R=10mm) - Length Deficit',
        'FullSurround200mm_R10mm_CinchLoop': 'Optimized 200mm (R=10mm) - Cinch Loop',
        'FullSurround200mm_R14mm_Enclosure': 'Optimized 200mm (R=14mm) - Full Enclosure',
        'FullSurround200mm_R18mm_LargePayload': 'Optimized 200mm (R=18mm) - Large Payload'
    }
    markers = {
        'Std140mm_R10mm_HighPressure': ('--x', 2.0),
        'FullSurround200mm_R10mm_CinchLoop': ('-o', 2.5),
        'FullSurround200mm_R14mm_Enclosure': ('-s', 2.5),
        'FullSurround200mm_R18mm_LargePayload': ('-^', 2.5)
    }

    # Plot 1: Circumferential Wrap Angle vs Pressure
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax1.plot(grp['P_kPa'], grp['WrapAngle_deg'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax1.axhline(360.0, color='black', linestyle=':', lw=1.8, label='360° Full Circumference Enclosure')
    ax1.axhline(270.0, color='gray', linestyle='--', lw=1.2, label='270° (3/4th Wrap) Threshold')
    ax1.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax1.set_ylabel('Circumferential Wrap Angle $\\theta_{\\mathrm{wrap}}$ [deg]', fontsize=11, fontweight='bold')
    ax1.set_title('Circumferential Wrap Angle vs. Pressure', fontsize=12, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=8.5, loc='upper left')

    # Plot 2: Normal Contact Clamping Force vs Pressure
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax2.plot(grp['P_kPa'], grp['Fn_N'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax2.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax2.set_ylabel('Normal Contact Clamping Force $F_N$ [N]', fontsize=11, fontweight='bold')
    ax2.set_title('Normal Clamping Force vs. Pressure up to 100 kPa', fontsize=12, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.legend(fontsize=8.5, loc='upper left')

    # Plot 3: Peak von Mises Stress vs Burst Limit
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax3.plot(grp['P_kPa'], grp['VmMax_kPa'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax3.axhline(1000.0, color='darkred', linestyle='--', lw=2.0, label='Silicone Tensile Burst Limit (1000 kPa)')
    ax3.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax3.set_ylabel('Peak von Mises Stress $\\sigma_{\\mathrm{vm,max}}$ [kPa]', fontsize=11, fontweight='bold')
    ax3.set_title('Structural Integrity & Burst Safety up to 100 kPa', fontsize=12, fontweight='bold')
    ax3.grid(True, alpha=0.3)
    ax3.legend(fontsize=8.5, loc='upper left')

    # Plot 4: Floating Cylinder Dynamic Inward Displacement
    for cName, grp in df.groupby('Case'):
        c = colors.get(cName, 'gray')
        lbl = labels.get(cName, cName)
        fmt, lw = markers.get(cName, ('-o', 2.0))
        ax4.plot(grp['P_kPa'], grp['uMag_mm'], fmt, color=c, lw=lw, ms=6, label=lbl)

    ax4.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11, fontweight='bold')
    ax4.set_ylabel('Floating Cylinder Inward Shift $\\Delta r$ [mm]', fontsize=11, fontweight='bold')
    ax4.set_title('Dynamic Floating Object Inward Self-Centering', fontsize=12, fontweight='bold')
    ax4.grid(True, alpha=0.3)
    ax4.legend(fontsize=8.5, loc='upper left')

    plt.tight_layout()
    chart1_path = os.path.join(img_dir, "full_surround_grasp_mechanics.png")
    fig.savefig(chart1_path, bbox_inches='tight')
    print(f"\n[PLOT SAVED] Mechanics dashboard written to: {chart1_path}")

    # Also copy to root images
    chart1_root = os.path.join(base_dir, "images", "full_surround_grasp_mechanics.png")
    fig.savefig(chart1_root, bbox_inches='tight')

    # Copy/move files to maximum_force_full_surround/
    import glob
    import shutil
    for f in glob.glob(os.path.join(base_dir, "actuator_surround_*_solved.mph")):
        dest = os.path.join(out_dir, os.path.basename(f))
        shutil.move(f, dest)
        print(f"Moved {f} -> {dest}")

    for f in glob.glob(os.path.join(base_dir, "images", "actuator_surround_*.png")):
        dest = os.path.join(img_dir, os.path.basename(f))
        shutil.copy2(f, dest)
        print(f"Copied {f} -> {dest}")

else:
    print("[WARN] No records parsed from batch run.")
