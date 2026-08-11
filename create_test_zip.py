import zipfile
import shutil
from pathlib import Path

output_dir = Path('output')
tasks_new = output_dir / 'tasks_new.xlsx'
tasks_excel = output_dir / 'tasks.xlsx'

# Try replacing tasks.xlsx if possible
if tasks_new.exists():
    try:
        shutil.copy2(tasks_new, tasks_excel)
        print("Copied tasks_new.xlsx -> tasks.xlsx")
    except Exception as e:
        print(f"Could not copy over tasks.xlsx: {e}. Will use tasks_new.xlsx directly.")

excel_to_use = tasks_excel if tasks_excel.exists() else tasks_new

zip_path = output_dir / 'images.zip'
print(f"Creating {zip_path} from {excel_to_use} and output/images/ ...")

with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED) as zf:
    # Add Excel file as tasks.xlsx in root of zip
    zf.write(excel_to_use, arcname='tasks.xlsx')
    
    images_dir = output_dir / 'images'
    if images_dir.exists():
        count = 0
        for img in images_dir.iterdir():
            if img.is_file():
                zf.write(img, arcname=f"images/{img.name}")
                count += 1
        print(f"Packed {count} image files into images/ in zip")

size_mb = zip_path.stat().st_size / (1024 * 1024)
print(f"Successfully created {zip_path} ({size_mb:.2f} MB)")
