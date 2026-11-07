import os
import subprocess
import re
import glob
import shutil
import pandas as pd
import matplotlib.pyplot as plt

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
class_file = os.path.join(base_dir, "BuildFullEnclosureOptimization.class")
out_dir = os.path.join(base_dir, "loop_caging_optimization")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "full_enclosure_optimization_summary.csv")
brain_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562"

os.makedirs(img_dir, exist_ok=True)

comsol_compile = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolcompile.exe"
comsol_batch = r"C:\Program Files\COMSOL\COMSOL64\Multiphysics\bin\win64\comsolbatch.exe"

print("================================================================================")
print("  STEP 1: COMPILING BuildFullEnclosureOptimization.java")
print("================================================================================")
src_java = os.path.join(base_dir, "BuildFullEnclosureOptimization.java")
res_c = subprocess.run([comsol_compile, src_java], capture_output=True, text=True, encoding='latin-1', errors='replace')
if res_c.returncode != 0:
    print("Compilation failed:")
    print(res_c.stderr)
    exit(1)
print("Compilation successful -> BuildFullEnclosureOptimization.class generated.")

print("================================================================================")
print("  STEP 2: EXECUTING COMSOL BATCH RUNNER")
print("================================================================================")
cmd = [comsol_batch, "-inputfile", class_file]
print(f"Executing: {' '.join(cmd)}")

proc = subprocess.Popen(
    cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    encoding='latin-1',
    errors='replace',
    bufsize=1,
    universal_newlines=True
)

rows = []
row_pattern = re.compile(
    r">> OPT_ROW:\s+Case=(?P<Case>[^,]+),\s+L=(?P<L>[^,]+),\s+R_cyl=(?P<R_cyl>[^,]+),\s+Xc=(?P<Xc>[^,]+),\s+Yc=(?P<Yc>[^,]+),\s+P=(?P<P>[^,]+),\s+Fn=(?P<Fn>[^,]+),\s+Ffric=(?P<Ffric>[^,]+),\s+MaxTn=(?P<MaxTn>[^,]+),\s+VmMax=(?P<VmMax>[^,]+),\s+ux=(?P<ux>[^,]+),\s+uy=(?P<uy>[^,]+),\s+uMag=(?P<uMag>[^,]+),\s+xTip=(?P<xTip>[^,]+),\s+yTip=(?P<yTip>[^\s]+)"
)

for line in iter(proc.stdout.readline, ''):
    line_str = line.strip()
    if line_str:
        if any(k in line_str for k in ["SOLVING CASE", "OPT_ROW", "Exported render", "Saving model", "ERROR", "Solve phase", "Total converged", "Notice", "Exception"]):
            print(line_str)
        m = row_pattern.search(line_str)
        if m:
            d = m.groupdict()
            rows.append({
                'Case': d['Case'],
                'L_act_mm': float(d['L']),
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
                'yTip_mm': float(d['yTip'])
            })

proc.stdout.close()
return_code = proc.wait()
print(f"\nBatch process exited with code {return_code}")

if rows:
    df = pd.DataFrame(rows)
    df.to_csv(csv_path, index=False)
    print(f"\nSaved summary CSV to {csv_path}")
    print(df.to_string())
    shutil.copy2(csv_path, os.path.join(brain_dir, "full_enclosure_optimization_summary.csv"))

print("================================================================================")
print("  STEP 3: ORGANIZING GENERATED RENDERS AND ARTIFACTS")
print("================================================================================")
base_img_dir = os.path.join(base_dir, "images")
for f in glob.glob(os.path.join(base_img_dir, "*Opt_L*.png")):
    dest = os.path.join(img_dir, os.path.basename(f))
    shutil.copy2(f, dest)
    print(f"Copied render: {dest}")
    brain_dest = os.path.join(brain_dir, os.path.basename(f))
    shutil.copy2(f, brain_dest)

# Move solved .mph files
for f in glob.glob(os.path.join(base_dir, "*Opt_L*_solved.mph")):
    dest = os.path.join(out_dir, os.path.basename(f))
    shutil.move(f, dest)
    print(f"Moved solved model: {dest}")

print("\nProcessing complete!")
