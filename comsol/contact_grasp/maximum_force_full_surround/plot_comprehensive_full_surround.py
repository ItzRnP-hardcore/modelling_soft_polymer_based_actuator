import os
import pandas as pd
import matplotlib.pyplot as plt

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
max_force_dir = os.path.join(base_dir, "maximum_force_full_surround")
force_opt_dir = os.path.join(base_dir, "force_optimization")
img_dir = os.path.join(max_force_dir, "images")
os.makedirs(img_dir, exist_ok=True)

# 1. Load Datasets
df_std140 = pd.read_csv(os.path.join(max_force_dir, "full_surround_max_force_summary.csv"))
df_fullwrap = pd.read_csv(os.path.join(force_opt_dir, "full_wrap_grasp_summary.csv"))
df_scale = pd.read_csv(os.path.join(force_opt_dir, "scale_high_pressure_sweep.csv"))

# Merge / standardize
# Case A: Standard 140mm Actuator (R=10mm) up to 100 kPa
sub_std = df_std140[df_std140['Case'] == 'Std140mm_R10mm_HighPressure'].copy()

# Case B: Full-Wrap 220mm Actuator (R=20mm) up to 100 kPa
sub_wrap = df_fullwrap.copy()

# Case C: Scale 1.00x Actuator (R=22mm) up to 100 kPa
sub_s100_r22 = df_scale[(df_scale['Scale'] == 1.0) & (df_scale['R_mm'] == 22.0)].copy()

# Case D: Scale 1.50x Actuator (R=22mm) up to 100 kPa
sub_s150_r22 = df_scale[(df_scale['Scale'] == 1.5) & (df_scale['R_mm'] == 22.0)].copy()

# Case E: Scale 1.00x Actuator (R=15mm) up to 100 kPa
sub_s100_r15 = df_scale[(df_scale['Scale'] == 1.0) & (df_scale['R_mm'] == 15.0)].copy()

# 2. Master Plotting: 4-Panel Executive Dashboard
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(16, 12), dpi=180)

# Colors & Line Styles
c_std = '#DC143C'      # Crimson (Standard 140mm)
c_wrap = '#008000'     # Forest Green (Full-Wrap 220mm)
c_s100_22 = '#1E90FF'  # Dodger Blue (Scale 1.00x R=22mm)
c_s150_22 = '#8A2BE2'  # Blue Violet (Scale 1.50x R=22mm)
c_s100_15 = '#FF8C00'  # Dark Orange (Scale 1.00x R=15mm)

# Plot 1: Circumferential Wrap Angle vs Pressure
ax1.plot(sub_std['P_kPa'], sub_std['WrapAngle_deg'], '--x', color=c_std, lw=2.5, ms=8,
         label='Std 140mm Actuator ($R=10\\,$mm) — Length Deficit ($229.6^\\circ$ max)')
ax1.plot(sub_wrap['P_kPa'], sub_wrap['WrapAngle_deg'], '-o', color=c_wrap, lw=3.0, ms=8,
         label='Full-Wrap 220mm Actuator ($R=20\\,$mm) — Full Enclosure ($381.0^\\circ$ max)')

ax1.axhline(360.0, color='black', linestyle=':', lw=2.0, label='$360^\\circ$ Full Circumferential Enclosure')
ax1.axhline(270.0, color='gray', linestyle='--', lw=1.5, label='$270^\\circ$ (3/4th Circumference) Threshold')
ax1.fill_between([0, 100], 360, 420, color='green', alpha=0.08, label='Full Surround / Overlapping Cinch Zone')

ax1.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=12, fontweight='bold')
ax1.set_ylabel('Circumferential Wrap Angle $\\theta_{\\mathrm{wrap}}$ [$^\\circ$]', fontsize=12, fontweight='bold')
ax1.set_title('Circumferential Wrap Progression vs. Actuation Pressure', fontsize=13, fontweight='bold')
ax1.set_xlim(-2, 102)
ax1.set_ylim(0, 420)
ax1.grid(True, alpha=0.3)
ax1.legend(fontsize=9.5, loc='lower right')

# Plot 2: Normal Contact Clamping Force vs Pressure up to 100 kPa
ax2.plot(sub_s150_r22['P_kPa'], sub_s150_r22['Fn_N'], '-^', color=c_s150_22, lw=3.0, ms=8,
         label=r'Scale 1.50x Actuator ($R=22\,$mm) — **Peak Force: 2.010 N**')
ax2.plot(sub_s100_r22['P_kPa'], sub_s100_r22['Fn_N'], '-s', color=c_s100_22, lw=2.5, ms=8,
         label=r'Scale 1.00x Actuator ($R=22\,$mm) — Peak Force: 1.591 N')
ax2.plot(sub_s100_r15['P_kPa'], sub_s100_r15['Fn_N'], '-d', color=c_s100_15, lw=2.2, ms=7,
         label=r'Scale 1.00x Actuator ($R=15\,$mm) — Peak Force: 0.807 N')
ax2.plot(sub_std['P_kPa'], sub_std['Fn_N'], '--x', color=c_std, lw=2.2, ms=7,
         label=r'Std 140mm Actuator ($R=10\,$mm) — Peak Force: 0.469 N')
ax2.plot(sub_wrap['P_kPa'], sub_wrap['Fn_N'], '-o', color=c_wrap, lw=2.2, ms=7,
         label=r'Full-Wrap 220mm Actuator ($R=20\,$mm) — Peak Force: 0.246 N (Cinch Enclosure)')

ax2.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=12, fontweight='bold')
ax2.set_ylabel('Normal Clamping Force $F_N$ [N]', fontsize=12, fontweight='bold')
ax2.set_title('Normal Clamping Force Scaling Law up to 100 kPa', fontsize=13, fontweight='bold')
ax2.set_xlim(-2, 102)
ax2.grid(True, alpha=0.3)
ax2.legend(fontsize=9.5, loc='upper left')

# Plot 3: Structural Integrity & Burst Safety Margin
ax3.plot(sub_wrap['P_kPa'], sub_wrap['VmMax_kPa'], '-o', color=c_wrap, lw=2.5, ms=8,
         label=r'Full-Wrap 220mm ($\sigma_{\mathrm{vm,max}} = 583.9\,$kPa, $SF=1.71\times$)')
ax3.plot(sub_std['P_kPa'], sub_std['VmMax_kPa'], '--x', color=c_std, lw=2.2, ms=7,
         label=r'Std 140mm ($\sigma_{\mathrm{vm,max}} = 559.6\,$kPa, $SF=1.79\times$)')
ax3.plot(sub_s150_r22['P_kPa'], sub_s150_r22['VmMax_kPa'], '-^', color=c_s150_22, lw=2.5, ms=8,
         label=r'Scale 1.50x R22mm ($\sigma_{\mathrm{vm,max}} = 394.9\,$kPa, $SF=2.53\times$)')
ax3.plot(sub_s100_r22['P_kPa'], sub_s100_r22['VmMax_kPa'], '-s', color=c_s100_22, lw=2.2, ms=7,
         label=r'Scale 1.00x R22mm ($\sigma_{\mathrm{vm,max}} = 147.2\,$kPa, $SF=6.79\times$)')

ax3.axhline(1000.0, color='darkred', linestyle='--', lw=2.2, label='Silicone Tensile Rupture Limit (1000 kPa)')
ax3.fill_between([0, 100], 0, 1000, color='lightblue', alpha=0.08, label='Safe Operating Regime (No Bursting)')

ax3.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=12, fontweight='bold')
ax3.set_ylabel('Peak von Mises Stress $\\sigma_{\\mathrm{vm,max}}$ [kPa]', fontsize=12, fontweight='bold')
ax3.set_title('Actuator Structural Safety & Burst Limits up to 100 kPa', fontsize=13, fontweight='bold')
ax3.set_xlim(-2, 102)
ax3.set_ylim(0, 1100)
ax3.grid(True, alpha=0.3)
ax3.legend(fontsize=9.0, loc='upper left')

# Plot 4: Dynamic Floating Cylinder Self-Centering Inward Shift
ax4.plot(sub_std['P_kPa'], sub_std['uMag_mm'], '--x', color=c_std, lw=2.5, ms=8,
         label=r'Std 140mm ($R=10\,$mm): $\Delta r = 17.02\,$mm inward draw')
ax4.plot(sub_s100_r22['P_kPa'], sub_s100_r22['uMag_mm'], '-s', color=c_s100_22, lw=2.5, ms=8,
         label=r'Scale 1.00x ($R=22\,$mm): $\Delta r = 14.75\,$mm inward draw')
ax4.plot(sub_s150_r22['P_kPa'], sub_s150_r22['uMag_mm'], '-^', color=c_s150_22, lw=2.5, ms=8,
         label=r'Scale 1.50x ($R=22\,$mm): $\Delta r = 14.06\,$mm inward draw')
ax4.plot(sub_wrap['P_kPa'], sub_wrap['uMag_mm'], '-o', color=c_wrap, lw=2.2, ms=7,
         label=r'Full-Wrap 220mm ($R=20\,$mm): $\Delta r = 9.86\,$mm self-centering')

ax4.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=12, fontweight='bold')
ax4.set_ylabel('Floating Cylinder Inward Displacement $\\Delta r$ [mm]', fontsize=12, fontweight='bold')
ax4.set_title('Dynamic Floating Object Inward Self-Centering Trajectories', fontsize=13, fontweight='bold')
ax4.set_xlim(-2, 102)
ax4.grid(True, alpha=0.3)
ax4.legend(fontsize=9.5, loc='upper left')

plt.tight_layout()
out_plot = os.path.join(img_dir, "master_full_surround_and_max_force.png")
fig.savefig(out_plot, bbox_inches='tight')
print(f"[SAVED] Master dashboard written to: {out_plot}")

# Copy to root images
root_plot = os.path.join(base_dir, "images", "master_full_surround_and_max_force.png")
fig.savefig(root_plot, bbox_inches='tight')
print(f"[SAVED] Root plot written to: {root_plot}")
