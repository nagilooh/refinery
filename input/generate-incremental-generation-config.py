import math
import xml.etree.ElementTree as ET
import re
from pathlib import Path

# ---------------- Configuration ----------------
INPUT_DIR = Path('benchmark-set/generate')          # folder with .problem files
OUTPUT_XML = Path('benchmark-set/incremental-generation.xml')


START_SIZE = 20           # number of new files to generate per input file
STEP_SIZE = 5             # starting value for the first node number
END_SIZE = 100              # increment for each generated file

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
    'tool': 'refinery-incremental', 
    'displayName': 'refinery', 
    'timelimit': f'120 min', 
    'memlimit': '15 GB',
    'cpuCores': '2'})

# ET.SubElement(root, 'option').text = 'generate'
ET.SubElement(root, 'option', attrib={'name': '--start-size'}).text = f'{START_SIZE}'
ET.SubElement(root, 'option', attrib={'name': '--step-size'}).text = f'{STEP_SIZE}'
ET.SubElement(root, 'option', attrib={'name': '--end-size'}).text = f'{END_SIZE}'

for i in range(1, 101):
    rundefinition = ET.SubElement(root, 'rundefinition', attrib={'name': f'seed_{i}'})
    ET.SubElement(rundefinition, 'option', attrib={'name': '--seed'}).text = f'{i}'

# for file_path in INPUT_DIR.iterdir():
#     print(f'Processing {file_path}...')
#     if not file_path.is_file():
#         continue
#     ET.SubElement(root, 'option', attrib={'name': '-scope'}).text = f'node={size_lower}..{size_upper}'

task = ET.SubElement(root, 'tasks', attrib={'name': 'generation'})
ET.SubElement(task, 'includesfile').text = '../benchmark-input/IncrementalModelGeneration.set'

ET.SubElement(root, 'resultfiles').text = '**/*.*'

tree = ET.ElementTree(root)
tree.write(OUTPUT_XML, encoding='utf-8', xml_declaration=True)

print(f'XML written to {OUTPUT_XML}')
