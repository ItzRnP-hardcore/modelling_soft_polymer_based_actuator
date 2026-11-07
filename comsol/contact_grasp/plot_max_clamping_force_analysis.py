import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
from PIL import Image

base_dir = r"c:\Users\rudra\Documents\BTP\comsol\contact_grasp"
out_dir = os.path.join(base_dir, "loop_caging_optimization")
img_dir = os.path.join(out_dir, "images")
csv_path = os.path.join(out_dir, "max_clamping_force_summary.csv")
brain_dir = r"C:\Users\rudra\.gemini\antigravity\brain\6c8dd65a-72ab-4ed6-b0bf-0b6bda358562"

def plot_curves():
    if not os.path.exists(csv_path):
        print(f"CSV not found: {csv_path}")
        return

    df = pd.read_csv(csv_path)
    print("=== MAX CLAMPING FORCE SUMMARY ===")
    print(df.to_string())

    fig, axes = plt.subplots(2, 2, figsize=(14, 10))

    colors = {
        'Caging_L220_Pos3_FineCont_Xc32_Yc-52': 'tab:blue',
        'Caging_L220_Tuned_Xc31_Yc-51': 'tab:orange',
        'Caging_L250_FullWrap_Xc32_Yc-52': 'tab:green'
    }

    labels = {
        'Caging_L220_Pos3_FineCont_Xc32_Yc-52': 'L=220mm (Pos3, Xc=32, Yc=-52)',
        'Caging_L220_Tuned_Xc31_Yc-51': 'L=220mm Tuned (Xc=31, Yc=-51)',
        'Caging_L250_FullWrap_Xc32_Yc-52': 'L=250mm Full Wrap (376.6 deg)'
    }

    # 1. Normal Contact Clamping Force Fn
    ax1 = axes[0, 0]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        lbl = labels.get(name, name)
        ax1.plot(group['P_kPa'], group['Fn_N']*1000.0, 'o-', color=c, linewidth=2.5, markersize=6, label=lbl)
    ax1.set_title('Normal Clamping Force $F_n$ vs Pressure', fontsize=12, fontweight='bold')
    ax1.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11)
    ax1.set_ylabel('Normal Clamping Force $F_n$ [mN]', fontsize=11)
    ax1.grid(True, linestyle=':', alpha=0.6)
    ax1.legend(fontsize=9)

    # 2. Peak Contact Traction MaxTn
    ax2 = axes[0, 1]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        lbl = labels.get(name, name)
        ax2.plot(group['P_kPa'], group['MaxTn_kPa'], 's--', color=c, linewidth=2.0, markersize=5, label=lbl)
    ax2.set_title('Peak Contact Traction $T_{n,max}$ vs Pressure', fontsize=12, fontweight='bold')
    ax2.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11)
    ax2.set_ylabel('Peak Contact Traction $T_n$ [kPa]', fontsize=11)
    ax2.grid(True, linestyle=':', alpha=0.6)
    ax2.legend(fontsize=9)

    # 3. Peak Von Mises Stress
    ax3 = axes[1, 0]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        lbl = labels.get(name, name)
        ax3.plot(group['P_kPa'], group['VmMax_kPa'], '^-', color=c, linewidth=2.0, markersize=5, label=lbl)
    ax3.set_title('Max Von Mises Stress $\sigma_{vM,max}$ vs Pressure', fontsize=12, fontweight='bold')
    ax3.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11)
    ax3.set_ylabel('Peak Von Mises Stress [kPa]', fontsize=11)
    ax3.grid(True, linestyle=':', alpha=0.6)
    ax3.legend(fontsize=9)

    # 4. Floating Cylinder Displacement
    ax4 = axes[1, 1]
    for name, group in df.groupby('Case'):
        c = colors.get(name, 'black')
        lbl = labels.get(name, name)
        ax4.plot(group['P_kPa'], group['uMag_mm'], 'd-', color=c, linewidth=2.0, markersize=5, label=lbl)
    ax4.set_title('Floating Cylinder Displacement $|u_{cyl}|$ vs Pressure', fontsize=12, fontweight='bold')
    ax4.set_xlabel('Actuation Pressure $P$ [kPa]', fontsize=11)
    ax4.set_ylabel('Displacement Magnitude [mm]', fontsize=11)
    ax4.grid(True, linestyle=':', alpha=0.6)
    ax4.legend(fontsize=9)

    plt.suptitle('COMSOL 6.4: Max Clamping Force & Enclosure Optimization\nFixed Small Cylinder Radius R = 8.0 mm', fontsize=14, fontweight='bold')
    plt.tight_layout()

    fig_out = os.path.join(out_dir, "max_clamping_force_curves.png")
    plt.savefig(fig_out, dpi=200)
    print(f"Saved curve figure: {fig_out}")

    import shutil
    shutil.copy2(fig_out, os.path.join(brain_dir, "max_clamping_force_curves.png"))

def build_composite_panel():
    cases = [
        ("Caging_L220_Pos3_FineCont_Xc32_Yc-52", "L220 Pos 3 (Xc=32, Yc=-52)\nP=94 kPa | Fn=9.4 mN"),
        ("Caging_L220_Tuned_Xc31_Yc-51", "L220 Tuned (Xc=31, Yc=-51)\nP=94.5 kPa | Fn=11.5 mN"),
        ("Caging_L250_FullWrap_Xc32_Yc-52", "L250 Full Wrap (376.6 deg)\nP=71.1 kPa | Fn=69.8 mN (MAX FORCE!)")
    ]
    
    images = []
    titles = []

    for c, title in cases:
        pattern = os.path.join(img_dir, f"actuator_{c}_disp_P*.png")
        matches = glob.glob(pattern)
        if matches:
            matches.sort(key=os.path.getmtime)
            images.append(matches[-1])
            titles.append(title)
        else:
            print(f"Missing image for {c}")

    if len(images) < 3:
        print("Not all images found.")
        return

    loaded = [Image.open(p) for p in images]
    w, h = loaded[0].size

    fig, axes = plt.subplots(1, 3, figsize=(18, 6.5))
    for ax, im, title in zip(axes, loaded, titles):
        ax.imshow(im)
        ax.axis('off')
        ax.set_title(title, fontsize=12, fontweight='bold', pad=10)

    plt.suptitle("COMSOL 6.4: Circumferential Caging & Max Clamping Force Comparison\nFixed Cylinder Radius R = 8.0 mm | Monolithic Hyperelastic Actuator",
                 fontsize=14, fontweight='bold')
    plt.tight_layout()

    panel_out = os.path.join(out_dir, "max_clamping_force_verified_panel.png")
    plt.savefig(panel_out, dpi=200, bbox_inches='tight')
    print(f"Saved composite panel to {panel_out}")

    import shutil
    shutil.copy2(panel_out, os.path.join(brain_dir, "max_clamping_force_verified_panel.png"))

if __name__ == '__main__':
    plot_curves()
    build_composite_panel()
