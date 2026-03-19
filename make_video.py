import os
import glob
import imageio
import argparse

def make_video(image_folder, output_video, fps=30):
    # Find all PNGs
    pattern = os.path.join(image_folder, "*.png")
    frames = sorted(glob.glob(pattern))

    if not frames:
        print(f"No frames found in {image_folder}")
        return

    print(f"Found {len(frames)} frames. Rendering video to {output_video}...")

    # Write video
    writer = imageio.get_writer(output_video, fps=fps, macro_block_size=None)
    for i, frame_path in enumerate(frames):
        im = imageio.imread(frame_path)
        writer.append_data(im)
        if (i+1) % 50 == 0:
            print(f"  Processed {i+1}/{len(frames)} frames")

    writer.close()
    print("Done!")

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("image_folder")
    parser.add_argument("output_video")
    parser.add_argument("--fps", type=int, default=30)
    args = parser.parse_args()

    make_video(args.image_folder, args.output_video, args.fps)
