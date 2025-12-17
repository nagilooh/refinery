import csv
import re
from pathlib import Path

# ---------------- Configuration ----------------
INPUT_DIR = Path("gen")          # folder with .problem files
OUTPUT_CSV = Path("measurement.csv")

TIMEOUT = 30
RUNS = 30

# Map filename prefix -> name column
NAME_MAP = {
    "fase-trainbenchmark": "TB1",
    "fase-trainbenchmark-simple": "TB2",
    "simplified-railway": "TB3",
    "yakindu": "SC2",
}
# ------------------------------------------------

pattern = re.compile(r"(.+)-(\d+)\.problem")

rows = []

for file_path in INPUT_DIR.iterdir():
    if not file_path.is_file():
        continue

    match = pattern.match(file_path.name)
    if not match:
        continue

    prefix, size_str = match.groups()
    size = int(size_str)

    if prefix not in NAME_MAP:
        # raise ValueError(f"No name mapping defined for prefix '{prefix}'")
        continue

    name = NAME_MAP[prefix]
    # output_filename = f"{prefix}-{size}.problem"

    for generate_up in ("false", "true"):
        rows.append([
            name,
            size,
            file_path.name,
            TIMEOUT,
            generate_up,
            RUNS
        ])

# Sort rows by name and size
rows.sort(key=lambda r: (r[0], r[1]))

# Write CSV
with OUTPUT_CSV.open("w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    writer.writerow(["name", "size", "filename", "timeout", "generate-up", "runs"])
    writer.writerows(rows)

print(f"CSV written to {OUTPUT_CSV}")
