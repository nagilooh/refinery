import os
import math
from pathlib import Path

# ---------------- Configuration ----------------
INPUT_DIR = Path("input")   # folder containing original files
OUTPUT_DIR = Path("gen")    # folder for generated files

NUM_GENERATED = 10           # number of new files to generate per input file
START_NODE = 10             # starting value for the first node number
NODE_STEP = 10              # increment for each generated file
# ------------------------------------------------

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


def generate_scope_line(filename: str, first_node: int) -> str:
    if filename == "fase-trainbenchmark.problem":
        return generate_tb1_scope_line(first_node)
    elif filename == "fase-trainbenchmark-simple.problem":
        return generate_tb2_scope_line(first_node)
    elif filename == "simplified-railway.problem":
        return generate_tb3_scope_line(first_node)
    elif filename == "yakindu.problem":
        return generate_sc2_scope_line(first_node)


def generate_tb1_scope_line(first_node: int) -> str:
    """
    Generate the scope line according to the rules.
    """
    second_node = math.ceil(first_node * 1.2)     # 20% larger
    segment_switch = math.ceil(first_node * 0.1)  # 10% of first value

    return (
        f"scope node = {first_node}..{second_node}, "
        f"RailwayContainer = 1, "
        f"Segment = {segment_switch}..*, "
        f"Switch = {segment_switch}..*."
    )


def generate_tb2_scope_line(first_node: int) -> str:
    """
    Generate the scope line according to the rules.
    """
    second_node = math.ceil(first_node * 1.2)     # 20% larger
    region = math.ceil(first_node * 0.05)  # 5% of first value
    sensor = math.ceil(first_node * 0.05)  # 5% of first value
    segment_switch = math.ceil(first_node * 0.1)  # 10% of first value

    return (
        f"scope node = {first_node}..{second_node}, "
        f"Region = {region}..*, "
        f"Sensor = {sensor}..*, "
        f"Segment = {segment_switch}..*, "
        f"Switch = {segment_switch}..*."
    )


def generate_tb3_scope_line(first_node: int) -> str:
    """
    Generate the scope line according to the rules.
    """
    second_node = math.ceil(first_node * 1.2)     # 20% larger
    segment_switch = math.ceil(first_node * 0.1)  # 10% of first value

    return (
        f"scope node = {first_node}..{second_node}, "
        f"Segment = {segment_switch}..*, "
        f"Switch = {segment_switch}..*."
    )


def generate_sc2_scope_line(first_node: int) -> str:
    """
    Generate the scope line according to the rules.
    """
    second_node = math.ceil(first_node * 1.2)     # 20% larger
    region = math.ceil(first_node * 0.1)  # 10% of first value
    choice = math.ceil(first_node * 0.05)  # 5% of first value

    return (
        f"scope node = {first_node}..{second_node}, "
        f"Region = {region}..*, "
        f"Choice = {choice}..*, "
        f"Statechart += 0."
    )


for file_path in INPUT_DIR.iterdir():
    if file_path.name not in ["fase-trainbenchmark.problem", "fase-trainbenchmark-simple.problem", "simplified-railway.problem", "yakindu.problem"]:
        continue
    if not file_path.is_file():
        continue

    with file_path.open("r", encoding="utf-8") as f:
        original_content = f.read()

    for i in range(NUM_GENERATED):
        first_node = START_NODE + i * NODE_STEP

        extra_line = generate_scope_line(file_path.name, first_node)

        new_content = original_content + "\n" + extra_line

        new_filename = f"{file_path.stem}-{first_node}{file_path.suffix}"
        output_path = OUTPUT_DIR / new_filename

        with output_path.open("w", encoding="utf-8") as out:
            out.write(new_content)

        print(f"Generated: {output_path}")
