"""
Vicsek model analysis and plotting.
Reads polarization time series and summary CSV files to produce:
  (b) va(t) temporal evolution for selected eta values
  (c) va vs eta curve with error bars
  (d) Comparative plot of all three scenarios

Usage:
    uv run python plot_polarization.py <output_dir> [--scenarios none fixed circular]
"""
import argparse
import os
import glob
import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl


def parse_polarization_file(filepath):
    """Parse polarization file: t\tva per line."""
    data = np.loadtxt(filepath)
    if data.ndim == 1:
        data = data.reshape(1, -1)
    return data[:, 0].astype(int), data[:, 1]


def parse_summary_csv(filepath):
    """Parse summary CSV: eta,va_mean,va_std."""
    data = np.loadtxt(filepath, delimiter=',', skiprows=1)
    return data[:, 0], data[:, 1], data[:, 2]


def setup_style():
    """Configure matplotlib for publication-quality plots."""
    plt.rcParams.update({
        'figure.figsize': (10, 7),
        'font.size': 14,
        'font.family': 'serif',
        'axes.labelsize': 16,
        'axes.titlesize': 18,
        'legend.fontsize': 12,
        'xtick.labelsize': 13,
        'ytick.labelsize': 13,
        'figure.dpi': 150,
        'savefig.dpi': 200,
        'savefig.bbox': 'tight',
        'axes.grid': True,
        'grid.alpha': 0.3,
    })


def plot_temporal_evolution(output_dir, scenario, density_tag="rho4"):
    """
    Plot va(t) for a few characteristic eta values (b).
    Shows how steady state is identified.
    """
    # Find polarization files for this scenario, seed 0
    pattern = os.path.join(output_dir, f"polarization_{scenario}_eta*_s0.txt")
    files = sorted(glob.glob(pattern))

    if not files:
        # Try without seed suffix (single run)
        pattern = os.path.join(output_dir, f"polarization_{scenario}_eta*.txt")
        files = sorted(glob.glob(pattern))

    if not files:
        print(f"No polarization files found for scenario={scenario}")
        return

    # Select a few characteristic eta values
    selected_etas = [0.0, 1.0, 2.0, 3.5, 5.0]

    fig, ax = plt.subplots()

    cmap = plt.cm.viridis
    colors = cmap(np.linspace(0.1, 0.9, len(selected_etas)))

    plotted = 0
    for fpath in files:
        # Extract eta from filename
        fname = os.path.basename(fpath)
        try:
            eta_str = fname.split("_eta")[1].split("_")[0].replace(".txt", "")
            eta_val = float(eta_str)
        except (IndexError, ValueError):
            continue

        if not any(abs(eta_val - se) < 0.01 for se in selected_etas):
            continue

        t, va = parse_polarization_file(fpath)
        ax.plot(t, va, label=f'η = {eta_val:.1f}', color=colors[plotted % len(colors)],
                linewidth=1.5, alpha=0.85)
        plotted += 1

    if plotted == 0:
        print(f"No matching polarization files for selected etas in {scenario}")
        return

    # Add steady state indicator
    ax.axvline(x=1000, color='red', linestyle='--', alpha=0.6, label='Steady state start (t=1000)')

    scenario_labels = {'none': 'Sin líder', 'fixed': 'Líder dirección fija', 'circular': 'Líder circular'}
    ax.set_xlabel('Tiempo (t)')
    ax.set_ylabel('Polarización $v_a$')
    ax.set_title(f'Evolución temporal de $v_a$ — {scenario_labels.get(scenario, scenario)}')
    ax.legend(loc='best')
    ax.set_ylim(-0.05, 1.05)

    out_path = os.path.join(output_dir, f"temporal_{scenario}_{density_tag}.png")
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Saved: {out_path}")


def plot_va_vs_eta(output_dir, scenario, density_tag="rho4"):
    """
    Plot va vs eta with error bars (c).
    """
    summary_file = os.path.join(output_dir, f"summary_{scenario}_{density_tag}.csv")

    if not os.path.exists(summary_file):
        print(f"Summary file not found: {summary_file}")
        return None

    eta, va_mean, va_std = parse_summary_csv(summary_file)

    fig, ax = plt.subplots()
    ax.errorbar(eta, va_mean, yerr=va_std, fmt='o-', capsize=4, markersize=6,
                linewidth=2, color='#2196F3', ecolor='#90CAF9', markeredgecolor='#1565C0')
    ax.set_xlabel('Ruido η')
    ax.set_ylabel('Polarización $\\langle v_a \\rangle$')

    scenario_labels = {'none': 'Sin líder', 'fixed': 'Líder dirección fija', 'circular': 'Líder circular'}
    ax.set_title(f'$v_a$ vs η — {scenario_labels.get(scenario, scenario)}')
    ax.set_ylim(-0.05, 1.05)
    ax.set_xlim(-0.2, 5.2)

    out_path = os.path.join(output_dir, f"va_vs_eta_{scenario}_{density_tag}.png")
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Saved: {out_path}")

    return eta, va_mean, va_std


def plot_comparative(output_dir, scenarios, density_tag="rho4"):
    """
    Plot comparative va vs eta for all scenarios overlaid (d).
    """
    fig, ax = plt.subplots()

    scenario_labels = {
        'none': 'Sin líder (A)',
        'fixed': 'Líder dirección fija (B)',
        'circular': 'Líder circular (C)'
    }
    colors_map = {
        'none': '#2196F3',
        'fixed': '#FF9800',
        'circular': '#4CAF50'
    }
    markers_map = {
        'none': 'o',
        'fixed': 's',
        'circular': '^'
    }

    for scenario in scenarios:
        summary_file = os.path.join(output_dir, f"summary_{scenario}_{density_tag}.csv")
        if not os.path.exists(summary_file):
            print(f"Skip {scenario}: {summary_file} not found")
            continue

        eta, va_mean, va_std = parse_summary_csv(summary_file)

        color = colors_map.get(scenario, '#555')
        marker = markers_map.get(scenario, 'o')
        label = scenario_labels.get(scenario, scenario)

        ax.errorbar(eta, va_mean, yerr=va_std, fmt=f'{marker}-', capsize=4, markersize=6,
                    linewidth=2, color=color, label=label, alpha=0.85,
                    markeredgecolor=color)

    ax.set_xlabel('Ruido η')
    ax.set_ylabel('Polarización $\\langle v_a \\rangle$')
    density_val = density_tag.replace("rho", "ρ = ")
    ax.set_title(f'Comparación de escenarios — {density_val}')
    ax.legend(loc='best')
    ax.set_ylim(-0.05, 1.05)
    ax.set_xlim(-0.2, 5.2)

    out_path = os.path.join(output_dir, f"comparative_{density_tag}.png")
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Saved: {out_path}")


def main():
    parser = argparse.ArgumentParser(description="Vicsek model analysis plots")
    parser.add_argument("output_dir", help="Directory with simulation output files")
    parser.add_argument("--scenarios", nargs='+', default=['none', 'fixed', 'circular'],
                        help="Scenarios to plot")
    parser.add_argument("--density", type=float, default=4.0, help="Density value for labels")
    args = parser.parse_args()

    setup_style()

    density_tag = f"rho{int(args.density)}"

    for scenario in args.scenarios:
        print(f"\n=== Scenario: {scenario} ===")
        # (b) Temporal evolution
        plot_temporal_evolution(args.output_dir, scenario, density_tag)
        # (c) va vs eta
        plot_va_vs_eta(args.output_dir, scenario, density_tag)

    # (d) Comparative
    print(f"\n=== Comparative ===")
    plot_comparative(args.output_dir, args.scenarios, density_tag)


if __name__ == "__main__":
    main()
