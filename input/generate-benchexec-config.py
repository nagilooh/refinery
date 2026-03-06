import math
import xml.etree.ElementTree as ET
import re
from pathlib import Path

# ---------------- Configuration ----------------
INPUT_DIR = Path('benchmark-set/generate')          # folder with .problem files
OUTPUT_XML = Path('benchmark-set/benchmark-refinery-test2.xml')

TIMEOUT = 30
RUNS = 5
WARMUPTIME = 5

NUM_GENERATED = 5           # number of new files to generate per input file
START_NODE = 10             # starting value for the first node number
NODE_STEP = 10              # increment for each generated file

# # Map filename prefix -> name column
# NAME_MAP = {
#     'fase-trainbenchmark': 'TB1',
#     'fase-trainbenchmark-simple': 'TB2',
#     'simplified-railway': 'TB3',
#     'simplified-statechart': 'SC1',
#     'yakindu': 'SC2',
# }
# ------------------------------------------------


# Write XML
root = ET.Element('benchmark', attrib={
    'tool': 'refinery', 
    'displayName': 'refinery', 
    'timelimit': f'{str(math.ceil(TIMEOUT * RUNS / 60 + 2))} min', 
    'memlimit': '8 GB',
    'cpuCores': '1'})

ET.SubElement(root, 'option').text = 'measure'
ET.SubElement(root, 'option', attrib={'name': '-timeout'}).text = f'{TIMEOUT}'
ET.SubElement(root, 'option', attrib={'name': '-runs'}).text = f'{RUNS}'
ET.SubElement(root, 'option', attrib={'name': '-warmuptime'}).text = f'{WARMUPTIME}'
ET.SubElement(root, 'option', attrib={'name': '-output'}).text = 'output/${benchmark_date}-${rundefinition_name}-${inputfile_name}'

for generate_up in ('false', 'true'):
    for i in range(NUM_GENERATED):
        size_lower = START_NODE + i * NODE_STEP
        size_upper = math.ceil(size_lower * 1.2)

        rundefinition = ET.SubElement(root, 'rundefinition', attrib={'name': f'size_{size_lower}-genup_{generate_up}'})
        ET.SubElement(rundefinition, 'option', attrib={'name': '-scope'}).text = f'node={size_lower}..{size_upper}'
        ET.SubElement(rundefinition, 'option', attrib={'name': '-generate-up'}).text = f'{generate_up}'

# for file_path in INPUT_DIR.iterdir():
#     print(f'Processing {file_path}...')
#     if not file_path.is_file():
#         continue
#     ET.SubElement(root, 'option', attrib={'name': '-scope'}).text = f'node={size_lower}..{size_upper}'

task = ET.SubElement(root, 'tasks', attrib={'name': 'generation'})
ET.SubElement(task, 'includesfile').text = '../benchmark-input/ModelGeneration.set'

tree = ET.ElementTree(root)
tree.write(OUTPUT_XML, encoding='utf-8', xml_declaration=True)

print(f'XML written to {OUTPUT_XML}')
