# 后端项目从 0 到 1 搭建指南

> 本文档基于德州扑克在线游戏后端项目的实际部署脚本整理，适合后端新手开发者快速了解一套完整后端项目所需的工具链与部署流程。

![后端项目搭建全流程指南](/images/image_generated_20260416_210653_192_0_229ab3ed-5a6f-41c9-9e36-868f134bfabb.jpg)

## 一、技术栈概览

| 类别 | 工具/技术 | 用途 |
|------|----------|------|
| 编程语言 | Java | 后端业务逻辑 |
| 框架 | Spring Boot | Web 框架、依赖注入、REST API |
| 构建工具 | Maven (mvn) | 依赖管理、打包、测试 |
| 数据库 | MySQL 8.0 | 持久化存储 |
| 缓存 | Redis 7 | 会话缓存、高频读写场景 |
| 容器化 | Docker + Docker Compose | 环境一致性、服务编排 |
| 反向代理 | Nginx | 流量入口、SSL 终止 |
| 认证 | JWT (JSON Web Token) | 无状态身份认证 |
| API 文档 | Swagger (SpringDoc) | 接口文档与在线调试 |
| 健康检查 | Spring Actuator | 服务监控与运维探针 |
| 测试覆盖率 | JaCoCo | 单元测试覆盖率报告 |
| 数据库管理 | Adminer | 轻量级数据库 Web 管理界面 |
| Redis 管理 | Redis Commander | Redis 数据可视化管理 |

## 二、本地开发环境搭建

### 第一步：安装基础工具

**必须安装的工具：**

```
- JDK 17+          # Java 运行环境
- Maven 3.8+       # 构建工具
- MySQL 8.0        # 数据库（本地模式）
- Redis 7+         # 缓存服务（本地模式）
- Docker Desktop   # 容器化运行时（Docker 模式）
```

安装完成后验证：
```bash
java -version
mvn -version
mysql --version
redis-cli --version
docker --version
docker-compose --version
```

![验证工具版本](/images/image_generated_20260416_213237_643_0_0bdacaf1-d9a4-47b5-b161-52565ff706ac.jpg)

### 第二步：初始化数据库

在本地 MySQL 中创建数据库和用户：

```sql
CREATE DATABASE IF NOT EXISTS texas_holdem
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'texas_user'@'localhost'
  IDENTIFIED BY 'texas_password';

GRANT ALL PRIVILEGES ON texas_holdem.* TO 'texas_user'@'localhost';
FLUSH PRIVILEGES;
```

![MySQL Workbench 创建数据库](/images/image_generated_20260416_213435_451_0_4d5b744b-f842-4ec8-9e16-0ccca93cd3d2.jpg)

> 说明：项目启动时，框架（如 Flyway/Liquibase）会自动执行 `src/main/resources/db/migration/` 下的 SQL 脚本完成建表。

### 第三步：启动基础服务

**Linux/macOS：**
```bash
sudo systemctl start mysql
sudo systemctl start redis
```

**Windows：**
- MySQL：在服务管理器中启动 MySQL 服务，或使用 `net start MySQL`
- Redis：运行 `redis-server.exe` 或以服务方式安装启动

### 第四步：构建并启动项目

```bash
# 构建（跳过测试，快速打包）
mvn clean package -DskipTests

# 启动 JAR
java -jar target/texas-holdem-backend-1.0.0.jar
```

![VS Code + Maven 构建项目](/images/image_generated_20260416_213645_820_0_a7df22c5-31b6-4981-bf6a-f08b5886c870.jpg)

启动后访问：
- 应用程序：`http://localhost:8080`
- Swagger 文档：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`

![Swagger API 文档界面](/images/image_generated_20260416_214104_075_0_035d56e5-4291-4fa7-9a84-7748768a6b78.jpg)

## 三、Docker 容器化部署（推荐）

### 为什么用 Docker？

- **环境一致**：开发、测试、生产使用同一套配置，消除"在我机器上能跑"的问题
- **快速启动**：一条命令拉起所有依赖服务（MySQL、Redis、App、Nginx 等）
- **隔离性强**：各服务运行在独立容器，互不干扰

### Docker Compose 服务架构

```
                  ┌──────────┐
  用户请求 ──────▶│  Nginx   │  端口 80/443（反向代理）
                  └────┬─────┘
                       │
                  ┌────▼─────┐
                  │   App    │  端口 8080（Spring Boot）
                  └────┬─────┘
                  ┌────┴──────┬──────────────┐
             ┌────▼────┐ ┌────▼────┐         │
             │  MySQL  │ │  Redis  │         │
             │  :3306  │ │  :6379  │         │
             └─────────┘ └─────────┘         │
                                       ┌─────▼──────────┐
                                       │ Adminer :8081  │  (开发辅助)
                                       │ Redis Cmd:8082 │  (开发辅助)
                                       └────────────────┘
```

### 关键配置说明（docker-compose.yml）

**服务依赖顺序：**
```yaml
app:
  depends_on:
    mysql:
      condition: service_healthy  # 等 MySQL 健康检查通过再启动 App
    redis:
      condition: service_healthy
```

**健康检查机制：**
- MySQL：`mysqladmin ping`，重试 10 次，每次超时 20s
- Redis：`redis-cli ping`，重试 5 次
- App：HTTP 探测 `/actuator/health`，启动宽限期 60s

**数据持久化（Volumes）：**
```yaml
volumes:
  mysql_data:   # MySQL 数据目录，容器重启后数据不丢失
  redis_data:   # Redis AOF 持久化数据
```

**应用环境变量注入：**
```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker          # 激活 docker 配置文件
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/...  # 容器内网地址
  JWT_SECRET: your-secret-key             # ⚠️ 生产环境务必替换
  JWT_EXPIRATION: 86400000                # Token 有效期：24小时（毫秒）
  JWT_REFRESH_EXPIRATION: 604800000       # Refresh Token：7天
```

> 容器间通信使用**服务名**（如 `mysql`、`redis`）而非 `localhost`，这是 Docker 网络的关键概念。

### 启动命令

```bash
# 构建镜像并后台启动所有服务
docker-compose build
docker-compose up -d

# 查看服务状态
docker-compose ps
```

![Docker Desktop 运行中的容器](/images/image_generated_20260416_213925_847_0_345bd879-f338-4b3a-a68f-29851bc1a069.jpg)

```bash
# 查看应用日志（实时）
docker-compose logs -f app

# 停止所有服务
docker-compose down

# 停止并删除数据卷（⚠️ 会清除数据库数据）
docker-compose down -v
```

## 四、Maven 常用命令

Maven 是 Java 项目的标准构建工具，以下是项目中实际使用的命令：

| 命令 | 说明 |
|------|------|
| `mvn clean package` | 清理并打包（含测试） |
| `mvn clean package -DskipTests` | 清理并打包（跳过测试） |
| `mvn test` | 仅运行单元测试 |
| `mvn jacoco:report` | 生成测试覆盖率报告 |
| `mvn clean` | 清理 target/ 构建目录 |

测试报告位置：`target/site/jacoco/index.html`

## 五、端口规划

启动前请确认以下端口未被占用：

| 端口 | 服务 | 说明 |
|------|------|------|
| 3306 | MySQL | 数据库 |
| 6379 | Redis | 缓存 |
| 8080 | Spring Boot App | 主应用 |
| 8081 | Adminer | 数据库管理界面（开发用） |
| 8082 | Redis Commander | Redis 可视化（开发用） |
| 80/443 | Nginx | HTTP/HTTPS 入口 |

**检查端口占用：**
```bash
# Linux/macOS
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

## 六、两种模式对比

| 维度 | 本地开发模式 | Docker 容器模式 |
|------|-------------|----------------|
| 启动速度 | 快（服务已在本地） | 略慢（需拉取镜像/构建） |
| 环境依赖 | 需手动安装 MySQL、Redis | 只需安装 Docker |
| 适合场景 | 日常代码调试 | 集成测试、类生产验证 |
| 团队协作 | 环境可能不一致 | 环境完全一致 |
| 数据隔离 | 与本地数据共用 | 完全隔离的容器数据卷 |

**建议：**
- 日常开发 → 使用本地模式，调试方便
- 提交代码前/联调测试 → 使用 Docker 模式验证


## 七、生产环境注意事项

基于脚本中的配置，以下内容在生产环境 **必须修改**：

1. **JWT 密钥**：`JWT_SECRET` 必须替换为高强度随机字符串，不能使用示例值
2. **数据库密码**：`MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD` 使用强密码
3. **HTTPS**：配置 Nginx SSL 证书，将 HTTP 流量重定向到 HTTPS
4. **移除开发工具**：生产环境去掉 Adminer 和 Redis Commander 服务

![Adminer 数据库管理界面](/images/image_generated_20260416_214110_373_0_e5a3f19f-b481-400b-85f3-d5f0e85cc120.jpg)

5. **日志管理**：`./logs` 目录挂载到宿主机，配置日志轮转
6. **健康检查**：利用 `/actuator/health` 接入负载均衡器的健康探测


## 八、快速排障指南

| 问题 | 排查方向 |
|------|---------|
| 应用启动失败 | 检查 MySQL/Redis 是否健康；查看 `docker-compose logs app` |
| 数据库连接失败 | 确认 URL 中的主机名（本地用 `localhost`，容器内用服务名 `mysql`） |
| 端口被占用 | 用 `lsof`/`netstat` 找占用进程并停止，或修改 compose 映射端口 |
| JWT 认证失败 | 检查 `JWT_SECRET` 是否一致；确认 Token 是否过期 |
| 健康检查超时 | 增大 `start_period`；检查数据库迁移脚本是否报错 |


## 九、推荐学习路径（新手）

```
1. Java 基础  →  2. Spring Boot  →  3. MySQL + MyBatis/JPA
       ↓
4. Redis 缓存设计  →  5. JWT 认证鉴权  →  6. RESTful API 设计
       ↓
7. Maven 构建  →  8. Docker 容器化  →  9. Nginx 反向代理
       ↓
10. 单元测试（JUnit + JaCoCo）  →  11. CI/CD 流水线
```
