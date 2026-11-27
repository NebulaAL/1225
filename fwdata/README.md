# FW Data 采集管理后台

用 Java 17、Spring Boot、MySQL 8.4 和 Vue 3 管理来自不同渠道的填报。每个数据集定义一张表单，同一份表单可以分发到多个来源。例如，给门店海报和公众号各生成一个链接，两边填写的答案会进入同一份提交列表，并保留各自的来源。

## 角色和使用流程

| 身份 | 可以做什么 |
| --- | --- |
| 管理员 | 登录后台，创建管理员和普通用户，删除普通用户，配置数据集和来源，查看全部提交与答案 |
| 后台普通用户 | 登录后台，按数据集和来源筛选全部提交，逐题查看实际选项与文本 |
| 前台填报人 | 打开指定来源的填报链接，无需账号即可填写并获得提交编号 |

1. 管理员在 **数据集 → 新建数据集** 填写名称、题目和选项。支持必填或选填的单选、多选及文本题。
2. 每个数据集自带一个默认来源。在 **收集来源** 中输入渠道名称并生成链接，把对应链接交给该渠道的填报人。
3. 管理员在 **账号管理** 创建查阅账号。后台成员在 **提交记录 → 查看答案** 可以看到每次提交选了什么，以及何时、从哪个来源提交。

## MySQL 表设计

| 表 | 一行代表什么 | 关键字段和关系 |
| --- | --- | --- |
| `accounts` | 一个后台账号 | `id` 主键，`username` 唯一，`password_hash` 保存加盐哈希，`role` 取 `ADMIN` 或 `USER` |
| `datasets` | 一套采集表单 | `name`、`description`、`created_by`、`created_at`，创建人关联后台管理员 |
| `dataset_fields` | 数据集中的一道题 | `dataset_id`、`label`、`kind`、`required`、`position_no` |
| `field_options` | 一道选择题的一个候选项 | `field_id`、`label`、`position_no` |
| `collection_sources` | 一个数据集的一个收集渠道 | `dataset_id`、`name`、随机唯一的 `public_key`，同一数据集内来源名称唯一 |
| `submissions` | 前台的一次完整提交 | `dataset_id`、`source_id`、`submitted_at`。同一来源内的 `request_key` 唯一，用来识别重复发送 |
| `submission_answers` | 某次提交对一道题的回答 | 主键为 `(submission_id, field_id)`，文本题答案存入 `text_value` |
| `answer_choices` | 某道题实际选中的一个选项 | 主键为 `(submission_id, field_id, option_id)`。多选两个选项就写两行 |

例如，填报人在“关注的功能”选了“查询”和“统计”，会写入一条提交、一条该题答案和两条选中项。数据库的组合外键保证提交来源属于当前数据集，选项属于当前题目。提交列表可按数据集或来源使用索引分页读取。

数据集创建后保留原始题目与选项。题目变更时可创建新数据集，历史记录继续显示填报时的名称。删除后台普通用户会撤销其访问权，采集记录由来源和提交编号标识，可以继续查阅。

Flyway 在应用启动时自动建表。另有一张内部表 `bootstrap_lock`，用于多个实例同时启动时只创建一次初始管理员。[完整建表 SQL](backend/src/main/resources/db/migration/V1__collection_workspace.sql)

例如，需要比较某道题在各个渠道的选择情况时，可以按题目 ID 查询每个选项被选中的提交数。

```sql
SELECT c.name AS source_name, o.label AS selected_option, COUNT(*) AS submission_count
FROM answer_choices a
JOIN submissions s ON s.id = a.submission_id
JOIN collection_sources c ON c.id = s.source_id
JOIN field_options o ON o.id = a.option_id
WHERE a.field_id = ?
GROUP BY c.id, c.name, o.id, o.label
ORDER BY c.id, submission_count DESC;
```

## 启动

在安装了 Docker Compose 的机器上，从仓库根目录执行以下命令。

```sh
cd fwdata
cp -n .env.example .env
```

在本地 `.env` 填写 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD` 和 `ADMIN_PASSWORD`。首次管理员账号默认为 `admin`，可通过 `ADMIN_USERNAME` 指定。随后启动应用。

```sh
docker compose up --build -d
```

打开 [本机登录页](http://localhost:8080)。Java 同时提供 Vue 页面和 API。MySQL 使用命名卷 `mysql-data` 保存数据，执行 `docker compose down` 后再次启动可继续使用原数据。首次管理员配置只在空库生效。

默认端口仅对本机开放。需要向其他设备分发填报链接时，将自己的 HTTPS 域名反向代理至应用，并用该域名登录后台复制链接。生成的链接沿用当前站点地址。

## 本地开发

后端使用 JDK 17 和 Maven 3.9，前端使用 Node.js 22.12 或更高版本。先创建一个使用 `utf8mb4` 的 MySQL 数据库，并为应用账号授予该数据库的建表和读写权限。

在 `fwdata/` 中导出数据库与首次管理员配置。以下默认连接地址使用本机的 `fwdata` 数据库。

```sh
set -a
. ./.env
set +a
export DB_URL='jdbc:mysql://127.0.0.1:3306/fwdata?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
export DB_USER=fwdata
mvn -f backend/pom.xml spring-boot:run
```

在另一个终端启动 Vue。Vite 将 `/api` 转发至本机 8080 端口，可通过 `API_TARGET` 改为其他后端地址。

```sh
cd frontend
npm ci
npm run dev
```

要生成包含前端的可执行 JAR，在 `fwdata/` 中运行以下命令。集成测试使用独立的 H2 内存库，覆盖角色权限、来源归属、选项还原、提交重试及删除用户后的会话失效。

```sh
npm --prefix frontend ci
npm --prefix frontend run build
mvn -f backend/pom.xml clean package
java -jar backend/target/fwdata-0.1.0.jar
```
