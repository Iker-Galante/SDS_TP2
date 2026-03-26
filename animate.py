"""
Vicsek model animation using OVITO.
Reads dynamic output files and renders frames with velocity arrows colored by angle.
Leader particle is highlighted with a distinct marker.
Produces individual PNGs and optionally stitches them into an .mp4 video.

Usage:
    uv run python animate.py <dynamic_file> [--frames N] [--output_dir dir] [--fps 30] [--no-video]
"""
import os
os.environ["OVITO_THREAD_COUNT"] = "1"
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
import multiprocessing
from multiprocessing.pool import AsyncResult
import sys
from concurrent.futures import as_completed
from typing import List
import numpy as np
import matplotlib.colors as mcolors

from ovito.data import DataCollection, Particles, SimulationCell
from ovito.pipeline import Pipeline, StaticSource
from ovito.vis import Viewport, ParticlesVis


def parse_dynamic_file(filepath):
    """
    Parse dynamic file format:
    t
    x y vx vy isLeader
    ...
    Returns list of frames, each frame is dict with 'time', 'positions', 'velocities', 'is_leader'.
    """
    frames = []
    with open(filepath, 'r') as f:
        lines = f.readlines()

    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue

        # Time line (single number)
        t = int(line)
        i += 1

        positions = []
        velocities = []
        is_leader = []

        # Read particle data until next time line or EOF
        while i < len(lines):
            pline = lines[i].strip()
            if not pline:
                i += 1
                continue
            parts = pline.split()
            if len(parts) < 4:
                break  # next time step
            # Check if this is the next time header (single integer)
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
    """Convert angles to HSV colors (hue = angle mapped to [0,1])."""
    hue = (angles / (2 * np.pi)) % 1.0
    saturation = np.ones_like(hue)
    value = np.ones_like(hue)
    hsv = np.stack([hue, saturation, value], axis=-1)
    rgb = np.array([mcolors.hsv_to_rgb(h) for h in hsv])
    return rgb


def render_frame(frame, L, output_path, frame_idx):
    """Render a single frame using OVITO."""
    positions = frame['positions']
    velocities = frame['velocities']
    is_leader = frame['is_leader']
    N = len(positions)

    # Compute velocity angles for coloring
    angles = np.arctan2(velocities[:, 1], velocities[:, 0])
    angles = (angles + 2 * np.pi) % (2 * np.pi)
    colors = angle_to_color(angles)

    # Leader gets a special white color and larger size
    leader_mask = is_leader == 1

    # Create OVITO data collection
    data = DataCollection()

    # Simulation cell
    cell_matrix = [[L, 0, 0, 0], [0, L, 0, 0], [0, 0, 1, 0]]
    cell = data.create_cell(cell_matrix, pbc=(True, True, False))

    # Create particles
    particles = data.create_particles(count=N)
    particles.create_property("Position", data=positions)

    # Particle radii — leader is bigger
    radii = np.full(N, 0.08)
    radii[leader_mask] = 0.2
    particles.create_property("Radius", data=radii)

    # Particle colors — leader is white
    particle_colors = colors.copy()
    particle_colors[leader_mask] = [1.0, 1.0, 1.0]
    particles.create_property("Color", data=particle_colors)

    # Create arrows (velocity vectors) as a vector property
    # Scale velocity arrows for visibility
    arrow_scale = 15.0  # scale factor for arrow length
    arrow_vectors = velocities * arrow_scale
    particles.create_property("Force", data=arrow_vectors)  # Use Force for arrows

    # Arrow colors
    arrow_colors = colors.copy()
    arrow_colors[leader_mask] = [1.0, 0.2, 0.2]  # red arrows for leader
    particles.create_property("Vector Color", data=arrow_colors)

    pipeline = Pipeline(source=StaticSource(data=data))
    pipeline.add_to_scene()

    # Configure particle vis
    particles.vis.radius = 0.08
    particles.vis.shape = ParticlesVis.Shape.Circle

    # Configure viewport
    vp = Viewport()
    vp.type = Viewport.Type.Top
    vp.camera_pos = (L / 2, L / 2, 100)
    vp.camera_dir = (0, 0, -1)
    vp.fov = L * 0.55

    vp.render_image(filename=output_path, size=(800, 800), background=(0.05, 0.05, 0.1))

    pipeline.remove_from_scene()


def frames_to_video(frame_dir, output_path, fps=30):
    """Stitch rendered PNG frames into an .mp4 video using imageio-ffmpeg."""
    import imageio
    import glob

    pattern = os.path.join(frame_dir, "frame_*.png")
    frame_files = sorted(glob.glob(pattern))

    if not frame_files:
        print(f"No frames found in {frame_dir}, skipping video generation.")
        return

    print(f"Stitching {len(frame_files)} frames into {output_path} at {fps} fps...")

    writer = imageio.get_writer(output_path, fps=fps, codec='libx264',
                                pixelformat='yuv420p')
    for idx, fpath in enumerate(frame_files):
        img = imageio.v3.imread(fpath)
        writer.append_data(img)
        printText = f"Stitched {idx+1:{"0"+str(len(str(len(frame_files))))}} frames ({(idx+1)*100/len(frame_files):5.2f}%)"
        print(f"\r\033[7m{printText[0:int(len(printText)*(idx+1)/len(frame_files))]}\033[0m{printText[int(len(printText)*(idx+1)/len(frame_files)):]}", end='')
    writer.close()

    print(f"\nVideo saved: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Vicsek model animation with OVITO")
    parser.add_argument("dynamic_file", help="Path to dynamic output file")
    parser.add_argument("--frames", type=int, default=0, help="Number of frames to render (0=all)")
    parser.add_argument("--output_dir", default="animation_frames", help="Output directory for frames")
    parser.add_argument("--skip", type=int, default=1, help="Render every Nth frame")
    parser.add_argument("--L", type=float, default=10.0, help="Box side length")
    parser.add_argument("--fps", type=int, default=60, help="Frames per second for video")
    parser.add_argument("--no-video", action="store_true", help="Skip .mp4 generation (frames only)")
    args = parser.parse_args()

    print(f"Parsing {args.dynamic_file}...")
    frames = parse_dynamic_file(args.dynamic_file)
    print(f"Found {len(frames)} frames")

    os.makedirs(args.output_dir, exist_ok=True)

    total = len(frames) if args.frames == 0 else min(args.frames, len(frames))
    jobs: List[AsyncResult] = []
    multiprocessing.set_start_method('spawn')
    with multiprocessing.Pool(processes=multiprocessing.cpu_count()) as pool:
        for idx in range(0, total, args.skip):
            frame = frames[idx]
            out_path = os.path.join(args.output_dir, f"frame_{idx:05d}.png")
            jobs.append(pool.apply_async(render_frame, args=(frame, args.L, out_path, idx)))

        rendered = 0
        initTimestamp = datetime.now()
        while len(jobs):
            for job in jobs:
                if job.ready():
                    jobs.remove(job)
                    rendered += 1
                    printText = f"Rendered {rendered:{"0"+str(len(str(total)))}} frames ({rendered*100/total:5.2f}%), elapsed: {str(datetime.now() - initTimestamp).split('.')[0]}, remaining: {str((datetime.now() - initTimestamp) / (rendered/total) * (total - rendered) / total).split('.')[0]}"
                    print(f"\r\033[7m{printText[0:int(len(printText)*rendered/total)]}\033[0m{printText[int(len(printText)*rendered/total):]}", end='')

    print(f"\nDone. {total} frames saved to {args.output_dir}")

    # Generate .mp4 video
    if not args.no_video:
        # Derive video filename from the dynamic file name
        base = os.path.splitext(os.path.basename(args.dynamic_file))[0]
        video_path = os.path.join(args.output_dir, f"{base}.mp4")
        frames_to_video(args.output_dir, video_path, fps=args.fps)


if __name__ == "__main__":
    main()
