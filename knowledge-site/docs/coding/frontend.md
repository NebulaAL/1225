# 前端速查手册

## CSS Flexbox 速查

```css
/* 水平垂直居中 */
.center {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 等间距排列 */
.space {
  display: flex;
  justify-content: space-between;
}

/* 换行排列 */
.wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
```

## 常用 JS 片段

### 防抖
```js
function debounce(fn, delay = 300) {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}
```

### 深拷贝
```js
const deepClone = obj => structuredClone(obj)
```

### 日期格式化
```js
function formatDate(date, fmt = 'YYYY-MM-DD') {
  const d = new Date(date)
  const map = {
    YYYY: d.getFullYear(),
    MM: String(d.getMonth() + 1).padStart(2, '0'),
    DD: String(d.getDate()).padStart(2, '0'),
    HH: String(d.getHours()).padStart(2, '0'),
    mm: String(d.getMinutes()).padStart(2, '0'),
  }
  return fmt.replace(/YYYY|MM|DD|HH|mm/g, m => map[m])
}
```
