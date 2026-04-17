# Python 自动化脚本

## 文件批量重命名

```python
import os

def batch_rename(folder, prefix="file"):
    for i, name in enumerate(sorted(os.listdir(folder))):
        ext = os.path.splitext(name)[1]
        new_name = f"{prefix}_{i+1:03d}{ext}"
        os.rename(
            os.path.join(folder, name),
            os.path.join(folder, new_name)
        )
        print(f"{name} → {new_name}")

# 用法
batch_rename("/path/to/folder", prefix="photo")
```

## Excel 批量处理

```python
import openpyxl

def merge_excels(files, output="merged.xlsx"):
    wb_out = openpyxl.Workbook()
    ws_out = wb_out.active
    header_written = False

    for f in files:
        wb = openpyxl.load_workbook(f)
        ws = wb.active
        for i, row in enumerate(ws.iter_rows(values_only=True)):
            if i == 0 and header_written:
                continue
            ws_out.append(row)
            header_written = True

    wb_out.save(output)
    print(f"合并完成 → {output}")
```

## 图片批量压缩

```python
from PIL import Image
import os

def compress_images(folder, quality=80):
    for name in os.listdir(folder):
        if name.lower().endswith(('.jpg', '.jpeg', '.png')):
            path = os.path.join(folder, name)
            img = Image.open(path)
            img.save(path, optimize=True, quality=quality)
            print(f"已压缩: {name}")
```

::: info
使用前需安装依赖：`pip install openpyxl Pillow`
:::
