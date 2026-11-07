import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
out_dir = os.path.join(base_dir, "loop_caging_optimization")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "full_enclosure_optimization_summary.csv")
brain_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562"

def plot_curves():
    if not os.path.exists(csv_path):
        print(f"CSV not found: {csv_path}")
        return

    df = pd.read_csv(csv_path)
    print("Optimization Summary:")
    print(df.to_string())

    fig, axes = plt.subplots(2, 2, figsize=(14, 10))

    colors = {'Opt_L220_TunedPos_Xc30_Yc-54': 'tab:blue',
              'Opt_L260_DeepWrap_Xc32_Yc-52': 'tab:orange',
              'Opt_L280_FullSurround_Xc30_Yc-54': 'tab:green'}

    # 1. Normal Contact Clamping Force Fn
    ax1 = axes[0, 0]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        label = f"{name} (L={group['L_act_mm'].iloc[0]:.0f}mm)"
        ax1.plot(group['P_kPa'], group['Fn_N'], 'o-', color=c, linewidth=2.5, markersize=6, label=label)
    ax1.set_title('Normal Clamping Force $F_n$ vs Pressure', fontsize=12, fontweight='bold')
    ax1.set_xlabel('Pressure $P$ [kPa]', fontsize=11)
    ax1.set_ylabel('Clamping Force $F_n$ [N]', fontsize=11)
    ax1.grid(True, linestyle=':', alpha=0.6)
    ax1.legend(fontsize=9)

    # 2. Peak Contact Traction MaxTn
    ax2 = axes[0, 1]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        label = f"{name} (L={group['L_act_mm'].iloc[0]:.0f}mm)"
        ax2.plot(group['P_kPa'], group['MaxTn_kPa'], 's--', color=c, linewidth=2.0, markersize=5, label=label)
    ax2.set_title('Peak Contact Pressure $T_{n,max}$ vs Pressure', fontsize=12, fontweight='bold')
    ax2.set_xlabel('Pressure $P$ [kPa]', fontsize=11)
    ax2.set_ylabel('Max Contact Traction $T_n$ [kPa]', fontsize=11)
    ax2.grid(True, linestyle=':', alpha=0.6)
    ax2.legend(fontsize=9)

    # 3. Peak Von Mises Stress
    ax3 = axes[1, 0]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        label = f"{name} (L={group['L_act_mm'].iloc[0]:.0f}mm)"
        ax3.plot(group['P_kPa'], group['VmMax_kPa'], '^-', color=c, linewidth=2.0, markersize=5, label=label)
    ax3.set_title('Max Von Mises Stress $\sigma_{vM,max}$ vs Pressure', fontsize=12, fontweight='bold')
    ax3.set_xlabel('Pressure $P$ [kPa]', fontsize=11)
    ax3.set_ylabel('Von Mises Stress [kPa]', fontsize=11)
    ax3.grid(True, linestyle=':', alpha=0.6)
    ax3.legend(fontsize=9)

    # 4. Floating Cylinder Displacement
    ax4 = axes[1, 1]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        label = f"{name} (L={group['L_act_mm'].iloc[0]:.0f}mm)"
        ax4.plot(group['P_kPa'], group['uMag_mm'], 'd-', color=c, linewidth=2.0, markersize=5, label=label)
    ax4.set_title('Cylinder Floating Displacement $|u_{cyl}|$ vs Pressure', fontsize=12, fontweight='bold')
    ax4.set_xlabel('Pressure $P$ [kPa]', fontsize=11)
    ax4.set_ylabel('Displacement Magnitude [mm]', fontsize=11)
    ax4.grid(True, linestyle=':', alpha=0.6)
    ax4.legend(fontsize=9)

    plt.suptitle('Full Enclosure Optimization: Scaling Length & Taper for Maximal Caging Grasp', fontsize=14, fontweight='bold')
    plt.tight_layout()

    fig_out = os.path.join(out_dir, "full_enclosure_optimization_curves.png")
    plt.savefig(fig_out, dpi=200)
    print(f"Saved curve figure: {fig_out}")

    import shutil
    shutil.copy2(fig_out, os.path.join(brain_dir, "full_enclosure_optimization_curves.png"))

def build_composite_panel():
    cases = [
        "Opt_L220_TunedPos_Xc30_Yc-54",
        "Opt_L260_DeepWrap_Xc32_Yc-52",
        "Opt_L280_FullSurround_Xc30_Yc-54"
    ]
    
    images = []
    titles = [
        "Case 1: L=220mm (Tuned Position Xc=30, Yc=-54)",
        "Case 2: L=260mm (Deep Wrap Xc=32, Yc=-52)",
        "Case 3: L=280mm (Complete Surround Xc=30, Yc=-54)"
    ]

    for c in cases:
        pattern = os.path.join(img_dir, f"actuator_{c}_disp_P*.png")
        matches = glob.glob(pattern)
        if matches:
            # Pick highest pressure render
            matches.sort(key=os.path.getmtime)
            images.append(matches[-1])
        else:
            images.append(None)

    if any(img is None for img in images):
        print("Not all renders are ready yet.")
        return

    loaded = [Image.open(p) for p in images]
    w, h = loaded[0].size

    composite = Image.new('RGB', (w * 3, h + 80), (255, 255, 255))
    for i, im in enumerate(loaded):
        composite.paste(im, (i * w, 80))

    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(18, 7))
    ax.imshow(composite)
    ax.axis('off')

    plt.title("COMSOL 6.4: Full Enclosure Optimization Grasp Comparison\nFixed Small Cylinder Radius R = 8.0 mm | True Caging & Multi-Directional Wrap",
              fontsize=14, fontweight='bold', pad=20)

    panel_out = os.path.join(out_dir, "full_enclosure_optimization_panel.png")
    plt.savefig(panel_out, dpi=200, bbox_inches='tight')
    print(f"Saved composite panel to {panel_out}")

    import shutil
    shutil.copy2(panel_out, os.path.join(brain_dir, "full_enclosure_optimization_panel.png"))

if __name__ == '__main__':
    plot_curves()
    build_composite_panel()

