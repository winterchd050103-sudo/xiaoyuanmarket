# 阿里云 ECS 部署指南（Ubuntu 22.04/24.04，2核4G）

> 适用：阿里云学生优惠 ECS。最终效果：浏览器访问 `http://<服务器公网IP>` 即为商城前台，管理后台在 `/admin`。

## 架构

```
浏览器 ──80──> Nginx容器(前端静态 + /api 反代) ──容器网络──> 后端jar容器 ──> MySQL / Redis / RabbitMQ 容器
```

公网只暴露 80（HTTP）和 22（SSH）；MySQL/Redis/RabbitMQ 不映射端口，仅容器内部网络可达。

## 一、本地准备（Windows）

```powershell
# 1. 打包后端（产物：backend/target/market-backend-1.0.0.jar）
D:\dev-env\maven\bin\mvn.cmd -s tools\env\maven-settings.xml -f backend\pom.xml -DskipTests package

# 2. 收集部署包（产物：deploy-package.zip，含 jar/init.sql/docker 目录/前端源码）
powershell -ExecutionPolicy Bypass -File tools\package-deploy.ps1
```

## 二、服务器初始化（SSH 登录后执行）

```bash
# 1. 装 Docker（阿里云镜像源）
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
sudo systemctl enable --now docker

# 2. 配置镜像加速（登录阿里云控制台 → 容器镜像服务 → 镜像加速器，复制你的专属地址）
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<'EOF'
{ "registry-mirrors": ["https://<你的ID>.mirror.aliyuncs.com"] }
EOF
sudo systemctl restart docker

# 3. 加 2G swap（防止构建前端时内存吃紧）
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 三、上传并启动

```bash
# 本地 PowerShell 上传（把 <IP> 换成你的公网 IP）
scp deploy-package.zip root@<IP>:/root/

# 服务器上
cd /root && unzip deploy-package.zip -d market && cd market/docker
cp .env.example .env
vi .env          # ★ 必改：把所有 Change_Me_* 替换为强密码
docker compose up -d --build
```

首次构建约 5-10 分钟（后端秒起因为用已打包 jar；前端需在容器内 npm install + vite build）。查看进度：`docker compose logs -f frontend`。

## 四、阿里云安全组

ECS 控制台 → 实例 → 安全组 → 配置规则，添加入方向：

| 端口 | 授权对象 | 用途 |
|---|---|---|
| 80 | 0.0.0.0/0 | 商城前台 |
| 22 | 你的 IP/0.0.0.0/0 | SSH |

**不要**开放 3306/6379/5672/15672——compose 已不映射这些端口，双保险。

## 五、验证清单

```bash
docker compose ps                    # 5 个容器 Up（mysql 显示 healthy）
curl http://localhost/api/categories # 返回 JSON 分类数据
curl -I http://localhost/api/upload/seed/1.jpg  # 200
```

浏览器：
- `http://<IP>` 商城前台，测试账号 `test / 123456`
- `http://<IP>/admin` 管理后台，`admin / 123456`（**登录后立即改密**）
- 注册一个新账号走一遍 发布→下单→支付→收货→评价

## 常用运维命令

```bash
docker compose logs -f backend     # 后端日志
docker compose restart backend     # 重启后端
docker compose down                # 停止（数据卷保留）
docker compose up -d               # 再次启动
# 上传图片在数据卷 upload-data 中，重建容器不丢失
```

## 常见问题

| 现象 | 处理 |
|---|---|
| frontend 构建卡在 npm install | 已配 npmmirror；仍慢则重试 `docker compose build frontend` |
| backend 反复重启 | `docker compose logs backend` 看密码是否与 .env 一致；MySQL 未 healthy 时属正常，等 healthcheck 通过 |
| 图片上传后 404 | 上传目录在数据卷 upload-data；确认访问路径带 `/api` 前缀 |
| 想用域名 | 需先 ICP 备案；备案后将域名解析到 ECS 并改 nginx.conf 的 server_name |
| 80 端口被运营商拦截提示未备案 | 用纯 IP 访问不受影响；报错时检查是否误开了非 80 端口访问测试 |

## 安全建议（演示项目至少做这几条）

1. `.env` 全部强密码；`git add` 前确认 `.env` 已被 ignore
2. 管理员 admin 首次登录改密
3. JWT_SECRET / PAY_SALT 换成随机串（.env 中已支持注入）
4. 服务器 SSH 用密钥登录，禁用密码登录（可选）
