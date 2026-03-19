import os
import glob
import argparse
from PIL import Image

def make_gif(image_folder, output_gif, fps=30):
    pattern = os.path.join(image_folder, "*.png")
    frames = sorted(glob.glob(pattern))

    if not frames:
        print(f"No frames found in {image_folder}")
        return

    print(f"Found {len(frames)} frames. Rendering GIF to {output_gif}...")

    # Load images
    images = [Image.open(f) for f in frames]
    
    # Calculate duration per frame in milliseconds
    duration_ms = int(1000 / fps)

    images[0].save(
        output_gif,
        format="GIF",
        append_images=images[1:],
        save_all=True,
        duration=duration_ms,
        loop=0
    )
    
    print(f"Done! Saved to {output_gif}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("image_folder")
    parser.add_argument("output_gif")
    parser.add_argument("--fps", type=int, default=30)
    args = parser.parse_args()

    make_gif(args.image_folder, args.output_gif, args.fps)
