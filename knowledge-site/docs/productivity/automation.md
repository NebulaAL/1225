# 自动化工作流

## 思路：重复 3 次以上的事，就该自动化

### 1. 文件整理自动化

```python
import os
import shutil

# 按扩展名自动分类文件
def organize_files(folder):
    rules = {
        'images': ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
        'documents': ['.pdf', '.doc', '.docx', '.xlsx', '.pptx'],
        'videos': ['.mp4', '.mov', '.avi'],
        'archives': ['.zip', '.rar', '.7z', '.tar.gz'],
    }

    for filename in os.listdir(folder):
        filepath = os.path.join(folder, filename)
        if os.path.isdir(filepath):
            continue
        ext = os.path.splitext(filename)[1].lower()
        for category, exts in rules.items():
            if ext in exts:
                dest = os.path.join(folder, category)
                os.makedirs(dest, exist_ok=True)
                shutil.move(filepath, os.path.join(dest, filename))
                print(f"{filename} → {category}/")
                break

# 用法：整理下载文件夹
organize_files(os.path.expanduser("~/Downloads"))
```

### 2. 定时任务（Mac crontab）

```bash
# 每天早上 9 点运行脚本
crontab -e
# 添加：
0 9 * * * /usr/bin/python3 /path/to/script.py
```

### 3. 快捷指令（iOS/Mac）

- 打开"快捷指令"App
- 可以做到：一键发送固定消息、批量处理照片、自动生成日报模板
- 支持与 Python 脚本联动
