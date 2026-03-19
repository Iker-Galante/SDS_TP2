"""
Vicsek model animation using OVITO.
Generates an MP4 video natively using OVITO's render_anim() method.

Usage:
    uv run python animate_video.py <dynamic_file> <output_video.mp4> [--frames N] [--skip S] [--fps 30]
"""
import argparse
import numpy as np
import matplotlib.colors as mcolors

from ovito.data import DataCollection
from ovito.pipeline import Pipeline, StaticSource
from ovito.vis import Viewport, ParticlesVis

# Global state for the modifier
g_frames = []
g_L = 10.0

def parse_dynamic_file(filepath):
    frames = []
    with open(filepath, 'r') as f:
        lines = f.readlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        try:
            t = int(line)
        except ValueError:
            i += 1
            continue
        i += 1
        positions, velocities, is_leader = [], [], []
        while i < len(lines):
            pline = lines[i].strip()
            if not pline:
                i += 1
                continue
            parts = pline.split()
            if len(parts) < 4:
                break
            if len(parts) == 1:
                break
            x, y, vx, vy = float(parts[0]), float(parts[1]), float(parts[2]), float(parts[3])
            leader = int(parts[4]) if len(parts) > 4 else 0
            positions.append([x, y, 0.0])
            velocities.append([vx, vy, 0.0])
            is_leader.append(leader)
            i += 1
        frames.append({
            'time': t,
            'positions': np.array(positions),
            'velocities': np.array(velocities),
            'is_leader': np.array(is_leader)
        })
    return frames

def angle_to_color(angles):
    hue = (angles / (2 * np.pi)) % 1.0
    hsv = np.stack([hue, np.ones_like(hue), np.ones_like(hue)], axis=-1)
    return np.array([mcolors.hsv_to_rgb(h) for h in hsv])

def modify(frame, data):
    """
    OVITO Python modifier. Called for each animation frame from 0 to N-1.
    """
    global g_frames, g_L
    if frame >= len(g_frames):
        return
        
    f = g_frames[frame]
    positions = f['positions']
    velocities = f['velocities']
    is_leader = f['is_leader']
    N = len(positions)

    angles = np.arctan2(velocities[:, 1], velocities[:, 0])
    angles = (angles + 2 * np.pi) % (2 * np.pi)
    colors = angle_to_color(angles)

    leader_mask = is_leader == 1

    cell_matrix = [[g_L, 0, 0, 0], [0, g_L, 0, 0], [0, 0, 2, 0]]
    data.create_cell(cell_matrix, pbc=(True, True, False))

    particles = data.create_particles(count=N)
    particles.create_property("Position", data=positions)

    radii = np.full(N, 0.08)
    radii[leader_mask] = 0.2
    particles.create_property("Radius", data=radii)

    p_colors = colors.copy()
    p_colors[leader_mask] = [1.0, 1.0, 1.0]  # White leader
    particles.create_property("Color", data=p_colors)

    arrow_scale = 15.0
    arrow_vectors = velocities * arrow_scale
    particles.create_property("Force", data=arrow_vectors) # Force works well for arrows

    v_colors = colors.copy()
    v_colors[leader_mask] = [1.0, 0.2, 0.2]  # Red arrow for leader
    particles.create_property("Vector Color", data=v_colors)

def main():
    global g_frames, g_L
    parser = argparse.ArgumentParser()
    parser.add_argument("dynamic_file")
    parser.add_argument("output_video")
    parser.add_argument("--frames", type=int, default=0)
    parser.add_argument("--skip", type=int, default=1)
    parser.add_argument("--fps", type=int, default=30)
    parser.add_argument("--L", type=float, default=10.0)
    args = parser.parse_args()

    g_L = args.L
    print(f"Parsing {args.dynamic_file}...")
    frames_all = parse_dynamic_file(args.dynamic_file)
    
    if args.frames > 0:
        frames_all = frames_all[:args.frames]
    g_frames = frames_all[::args.skip]
    print(f"Rendering {len(g_frames)} frames directly to {args.output_video} via OVITO (no external packages)")

    data = DataCollection()
    pipeline = Pipeline(source=StaticSource(data=data))
    pipeline.modifiers.append(modify)
    pipeline.add_to_scene()

    # Pre-compute first frame so vis settings apply correctly to particles created by modifier
    computed = pipeline.compute(0)
    computed.particles.vis.radius = 0.08
    computed.particles.vis.shape = ParticlesVis.Shape.Circle

    vp = Viewport()
    vp.type = Viewport.Type.Top
    vp.camera_pos = (g_L / 2, g_L / 2, 100)
    vp.camera_dir = (0, 0, -1)
    vp.fov = g_L * 0.55

    import ovito
    ovito.scene.anim.first_frame = 0
    ovito.scene.anim.last_frame = len(g_frames) - 1

    vp.render_anim(filename=args.output_video, size=(800, 800), background=(0.05, 0.05, 0.1), fps=args.fps)
    print("Done!")

if __name__ == "__main__":
    main()
