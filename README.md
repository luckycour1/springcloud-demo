# SpringCloud Demo — 使用说明文档

本文档汇总了本项目的服务端口、Swagger 路径、认证流程、构建和 Docker 操作及常见排查命令，便于快速上手和调试。

---

## 一、项目概览

- 项目根目录：`springcloud-demo`
- 模块：`common`、`gateway-service`、`auth-service`、`user-service`、`order-service`
- Java 版本：17
- Spring Boot：3.x

---

## 二、服务与端口

| 服务 | 容器名 / 说明 | 端口（宿主） |
|------|---------------|-------------:|
| gateway-service | 网关 | 8080 |
| auth-service | 认证服务 | 8083 |
| user-service | 用户服务 | 8081 |
| order-service | 订单服务 | 8082 |

中间件（infra）示例：

| 服务 | 容器名 | 端口 |
|------|--------|------:|
| Nacos | nacos | 8848 / 9848 |
| Zookeeper | zookeeper | 2181 |
| Kafka | kafka | 9092 (容器内 29092 用于 broker 内网) |
| Redis | redis | 6379 |
| Prometheus | prometheus | 9090 |
| Grafana | grafana | 3000 |
| Loki | loki | 3100 |
| Promtail | promtail | 无对外端口（采集日志） |

---

## 三、Swagger / OpenAPI

- OpenAPI JSON（每个服务）： `http://localhost:{port}/v3/api-docs`
- Swagger UI（每个服务）： `http://localhost:{port}/swagger-ui/index.html`

示例：
- Gateway: `http://localhost:8080/swagger-ui/index.html`
- Auth: `http://localhost:8083/swagger-ui/index.html`
- User: `http://localhost:8081/swagger-ui/index.html`
- Order: `http://localhost:8082/swagger-ui/index.html`

> 注意：gateway 也可聚合各服务文档（取决于路由和 swagger 配置）。

---

## 四、认证（示例）

### 登录获取 Token

请求（WSL / bash）：

```bash
curl -X POST "http://localhost:8080/auth/login" -d "username=admin&password=password"
```

PowerShell：

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/auth/login" -Method POST -ContentType "application/x-www-form-urlencoded" -Body "username=admin&password=password"
```

返回示例（JSON）：

```json
{ "code": 200, "msg": "success", "data": "<JWT_TOKEN>" }
```

使用方式：在后续请求 Header 中添加

```
Authorization: Bearer <JWT_TOKEN>
```

### 免鉴权白名单（Gateway）
```
/auth/login
/auth/register
/actuator/**
/v3/api-docs/**
/swagger-ui/**
/webjars/**
```

---

## 五、常用 API 调用示例

- 通过网关访问用户信息（推荐）：

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/users/1
```

- 通过网关创建订单（会触发 Kafka 消息）：

```bash
curl -X POST "http://localhost:8080/orders/create?userId=1&product=iPhone" -H "Authorization: Bearer <TOKEN>"
```

- 绕过网关直接访问服务（测试用）：

```bash
curl http://localhost:8081/users/1
curl -X POST "http://localhost:8082/orders/create?userId=1&product=iPhone"
```

---

## 六、构建与本地运行

Maven 打包（在项目根运行）：

```bash
mvn -U -DskipTests clean package
```

单模块运行（例：order-service）：

```bash
cd order-service
mvn spring-boot:run
```

运行打包 Jar：

```bash
java -jar target/order-service-1.0.0.jar
```

---

## 七、Docker / Compose 操作

文件：`infra.yml`（中间件）和 `services.yml`（业务服务）已在仓库根。

首选流程（推荐）：

1. 启动中间件（只需执行一次，复用现有中间件）

```bash
# 使用 docker compose v2 语法
docker compose -f infra.yml up -d
```

2. 启动业务服务（可随时重建/启动）

```bash
docker compose -f services.yml up -d --build
```

3. 单独重建某个业务服务（例如 gateway）

```bash
docker compose -f services.yml up -d --build gateway-service
```

4. 停止业务服务（中间件保持运行）

```bash
docker compose -f services.yml down
```

5. 停止中间件

```bash
docker compose -f infra.yml down
```

注意事项：
- 若遇到容器名冲突（例如 `/loki` 已存在），先手动删除旧容器：

```bash
docker rm -f loki nacos kafka zookeeper redis promtail grafana prometheus || true
```

- 启动业务服务时若提示 `network ... declared as external, but could not be found`，先创建网络或先启动 `infra.yml`：

```bash
docker network create spring-cloud-net
# 或先运行 infra.yml
docker compose -f infra.yml up -d
```

- Windows 路径在 WSL 下要使用 `/mnt/e/...` 形式映射宿主目录到容器。

---

## 八、Kafka 调试（发送/查看消息）

1) 通过业务链路触发（推荐）：

```bash
# 登录获取 token -> 通过网关创建订单（order-service 会向 topic 发送消息）
curl -X POST "http://localhost:8080/orders/create?userId=1&product=iPhone" -H "Authorization: Bearer <TOKEN>"
```

2) 在 kafka 容器内使用控制台工具（容器名 `kafka`）：

```bash
# 进入 kafka 容器（示例）
docker exec -it kafka bash
# 列出 topics
kafka-topics --bootstrap-server localhost:9092 --list
# 使用 console-consumer 查看 topic
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-created --from-beginning
```

3) 使用 kcat（kafkacat）带 header 测试（可选）：

```bash
# 本机或 WSL 安装 kcat
echo 'Order TEST' | kcat -P -b localhost:9092 -t order-created -H "Authorization:Bearer <TOKEN>"
# 消费端查看
kcat -C -b localhost:9092 -t order-created
```

---

## 九、排查与日志

查看容器状态：

```bash
docker ps -a
```

查看业务日志（示例）：

```bash
docker compose -f services.yml logs -f gateway-service
docker compose -f services.yml logs -f auth-service
docker compose -f services.yml logs -f user-service
docker compose -f services.yml logs -f order-service
```

快速查找异常堆栈（查找 `Caused by`）：

```bash
docker compose -f services.yml logs order-service --tail=500 | grep -i "Caused by"
```

常见问题：
- `no main manifest attribute, in app.jar`：说明 Jar 不是可执行包，需在 `pom.xml` 中配置 Spring Boot 的 `repackage` 执行并重新打包。
- `JWT signature does not match`：检查各服务 `JwtUtil` 的 SECRET_KEY 是否一致（签名必须相同）。
- `登录已经过期`：检查 token 是否过期或网关是否使用最新的校验逻辑（已移除内存 TokenCache 依赖）。

---

## 十、常用命令总结（复制用）

```bash
# 打包
mvn -U -DskipTests clean package

# 启动中间件
docker compose -f infra.yml up -d

# 启动业务
docker compose -f services.yml up -d --build

# 查看日志
docker compose -f services.yml logs -f order-service

# 发送测试请求
curl -X POST "http://localhost:8080/orders/create?userId=1&product=iPhone" -H "Authorization: Bearer <TOKEN>"
```

---

如果你希望我把这份文档写入仓库的其他位置（例如 `docs/README.md`）或增加 CI/CD、健康检查和本地调试脚本（`run.sh` / `run.ps1`），告诉我具体位置和需求，我会继续补充。
