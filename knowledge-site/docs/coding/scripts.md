# 实用脚本合集

## 批量下载图片

```python
import requests
import os

def download_images(urls, folder="downloads"):
    os.makedirs(folder, exist_ok=True)
    for i, url in enumerate(urls):
        resp = requests.get(url)
        ext = url.split('.')[-1].split('?')[0]
        path = os.path.join(folder, f"img_{i+1:03d}.{ext}")
        with open(path, 'wb') as f:
            f.write(resp.content)
        print(f"已下载: {path}")
```

## JSON 格式化查看

```python
import json
import sys

def pretty_json(file_path):
    with open(file_path) as f:
        data = json.load(f)
    print(json.dumps(data, indent=2, ensure_ascii=False))

# 命令行用法: python script.py data.json
if __name__ == "__main__":
    pretty_json(sys.argv[1])
```

## 端口占用查询（Mac/Linux）

```bash
# 查看某端口被谁占用
lsof -i :8080

# 杀掉占用端口的进程
kill -9 $(lsof -t -i :8080)
```

## Git 常用别名

```bash
# 添加到 ~/.gitconfig
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.lg "log --oneline --graph --decorate -20"
```
