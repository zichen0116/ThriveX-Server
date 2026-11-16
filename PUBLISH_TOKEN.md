# 发布令牌接入说明

本文档面向自动化写作/发布工具（例如 Muse）。目标是提供一个**长期有效、可单独撤销、权限最小化**的发布令牌，用于把文章以草稿形式写入 ThriveX。

## 1. 安全模型

- 服务端只保存令牌的 **SHA-256 摘要**（`PUBLISH_TOKEN_SHA256`），不保存令牌明文。
- 原始令牌只应保存在密码保险库中，通过请求头传递：

  ```http
  Authorization: Bearer <PUBLISH_TOKEN>
  ```

- 发布令牌不是网页登录 JWT：
  - 不会因为管理员在浏览器登录而被顶掉；
  - 不会随登录 token 的 3 天有效期过期；
  - 不写入 `user_token` 表。
- 发布令牌只能访问下列最小权限接口，其他管理接口一律 `403`。

## 2. 权限范围

| 能力 | 方法与路径 | 说明 |
| --- | --- | --- |
| 建草稿 | `POST /api/article` | 服务端强制 `isDraft=true`、`isDel=false` |
| 改草稿 | `PATCH /api/article` | 目标文章必须是草稿；不能改成已发布 |
| 删草稿 | `DELETE /api/article/{id}/{is_del}` | 目标文章必须是草稿；`0` 硬删除，`1` 软删除 |
| 上传图片 | `POST /api/file` | 表单字段 `dir`、`files`；返回博客对象存储 URL |
| 读文章 | `GET /api/article`、`GET /api/article/{id}` | 用于确认草稿是否写入成功 |
| 读分类 | `GET /api/cate`、`GET /api/cate/{id}` | 本来就是公开接口 |
| 读标签 | `GET /api/tag`、`GET /api/tag/{id}` | 本来就是公开接口 |
| 令牌自检 | `GET /api/publish/check` | 返回 `tokenType` 与可用权限 |

未列出的接口（用户、系统配置、评论、文件删除、批量删除、恢复、导入/导出等）均不可访问。

发布令牌**不能直接把草稿发布上线**。审稿后在管理后台点击发布，或后续按需调整服务端策略。

没有 IP 白名单、没有验证码、没有浏览器同源限制；CORS 已允许脚本直接调用。

## 3. 生成令牌与配置

### 3.1 生成

在可信机器上执行（原始令牌只进入密码保险库）：

```bash
TOKEN=$(openssl rand -hex 32)
printf '%s' "$TOKEN" | sha256sum | awk '{print $1}'
```

- `$TOKEN`：交给自动化工具/存入密码保险库，不要写入仓库或聊天记录。
- 第二条命令输出 64 位十六进制摘要，填入服务端配置。

### 3.2 服务端配置

在部署目录的 `.env` 中增加：

```bash
PUBLISH_TOKEN_SHA256=<64位小写十六进制摘要>
```

`docker-compose.yml` / `docker-compose.prod.yml` 已把该变量传入 `thrivex-server` 容器。

改完后重建后端容器：

```bash
docker compose up -d --force-recreate thrivex-server
docker logs thrivex-server | grep 发布令牌
```

看到下面的日志表示启用成功：

```text
发布令牌已启用：仅允许草稿文章创建/编辑/删除、图片上传与分类标签读取
```

如果日志显示“发布令牌未启用”，检查 `.env` 是否在 compose 所在目录、变量名是否为 `PUBLISH_TOKEN_SHA256`。

## 4. 接口示例

以下示例假设：

```bash
export PUBLISH_TOKEN='从密码保险库取出的原始令牌'
export BLOG_API='https://admin.lozic7.com/api'
```

### 4.1 自检

```bash
curl -s "$BLOG_API/publish/check" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"
```

期望：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "tokenType": "publish",
    "scopes": [
      "article:create-draft",
      "article:update-draft",
      "article:delete-draft",
      "file:upload",
      "cate:read",
      "tag:read"
    ]
  }
}
```

### 4.2 读分类与标签

```bash
curl -s "$BLOG_API/cate?pageNum=1&pageSize=100" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"

curl -s "$BLOG_API/tag?pageNum=1&pageSize=100" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"
```

列表数据在 `data.result`，总数在 `data.total`。

### 4.3 上传图片

```bash
curl -s -X POST "$BLOG_API/file" \
  -H "Authorization: Bearer $PUBLISH_TOKEN" \
  -F "dir=article" \
  -F "files=@cover.png"
```

期望返回：

```json
{
  "code": 200,
  "message": "文件上传成功：",
  "data": { "urls": ["https://<对象存储域名>/..."] }
}
```

支持 `jpg`、`jpeg`、`png`、`webp`，单文件最大 10MB。正文图和封面都建议先上传到这里，再把返回 URL 写入文章，避免微信图床防盗链。

### 4.4 新建草稿

```bash
curl -s -X POST "$BLOG_API/article" \
  -H "Authorization: Bearer $PUBLISH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "文章标题",
    "description": "文章摘要，最多 200 字",
    "content": "Markdown 正文",
    "cover": "https://<对象存储域名>/...",
    "cateIds": [1],
    "tagIds": [3],
    "config": {
      "status": 1,
      "password": "",
      "isDraft": true,
      "isEncrypt": false
    }
  }'
```

字段说明：

- `title`：必填，最多 255 字。
- `content`：必填，Markdown/HTML 正文。
- `description`：可选，最多 200 字。
- `cover`：可选，最多 300 字，建议使用上传接口返回的 URL。
- `cateIds`：必填且不能为空。
- `tagIds`：可选。
- `config.status`：必填，`1` 正常、`2` 首页隐藏、`3` 全站隐藏。
- `config.isDraft`：发布令牌会被强制为 `true`，不传或传 `false` 都一样。
- `config.isEncrypt` / `password`：按需。
- `createTime`：可不传，服务端自动填当前时间。

成功响应中的 `data` 是新建文章 ID：

```json
{ "code": 200, "message": "ok", "data": 123 }
```

### 4.5 确认草稿

```bash
curl -s "$BLOG_API/article?isDraft=true&pageNum=1&pageSize=20" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"

curl -s "$BLOG_API/article/{id}" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"
```

发布令牌按管理员视角读取，因此能看到 `status=3` 的全站隐藏草稿。

### 4.6 修改草稿

```bash
curl -s -X PATCH "$BLOG_API/article" \
  -H "Authorization: Bearer $PUBLISH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 123,
    "title": "修改后的标题",
    "description": "修改后的摘要",
    "content": "修改后的正文",
    "cover": "https://<对象存储域名>/...",
    "cateIds": [1],
    "tagIds": [3],
    "config": {
      "status": 1,
      "password": "",
      "isDraft": false,
      "isEncrypt": false
    }
  }'
```

注意：即使请求里写 `isDraft=false`，服务端也会保持草稿状态；发布令牌不能借此上线。

### 4.7 删除草稿

```bash
# 硬删除：彻底移除文章及关联数据
curl -s -X DELETE "$BLOG_API/article/{id}/0" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"

# 软删除：进入回收站
curl -s -X DELETE "$BLOG_API/article/{id}/1" \
  -H "Authorization: Bearer $PUBLISH_TOKEN"
```

只能删除草稿；删除已发布文章会返回 `code=403`。

## 5. 响应约定

| HTTP | `code` | 含义 |
| --- | --- | --- |
| 401 | 401 | 没有 token、token 错误或已失效 |
| 403 | 403 | 发布令牌访问了白名单之外的接口 |
| 200 | 403 | 发布令牌试图修改/删除非草稿 |
| 200 | 400/404 | 参数错误、文章不存在等业务错误 |
| 200 | 200 | 成功 |

**判断成功请以响应体里的 `code == 200` 为准**，不要只看 HTTP 状态码；业务异常仍可能返回 HTTP 200。

## 6. 轮换与撤销

### 轮换（不中断）

1. 生成新 token 和新哈希。
2. 在 `PUBLISH_TOKEN_SHA256` 中用英文逗号追加新哈希：
   ```bash
   PUBLISH_TOKEN_SHA256=<旧哈希>,<新哈希>
   ```
3. 重建后端容器。
4. 把自动化工具切换到新 token。
5. 删除旧哈希，再次重建容器。

### 撤销

- 从 `PUBLISH_TOKEN_SHA256` 删除对应哈希并重建容器。
- 清空 `PUBLISH_TOKEN_SHA256` 可禁用整个发布令牌通道。
- 令牌泄露时立即轮换，并检查服务器日志中的发布令牌指纹。

## 7. 本地验证参考

```bash
cd ThriveX-Server
mvn -DskipTests package

export DB_INFO='127.0.0.1:3306/ThriveX'
export DB_USERNAME='<user>'
export DB_PASSWORD='<password>'
export PUBLISH_TOKEN_SHA256="$(printf '%s' '<test-token>' | sha256sum | awk '{print $1}')"
export PORT=9003

java -jar blog/target/blog-4.0.jar
```

然后依次验证：自检 → 建草稿 → 列表确认 → 改草稿 → 越权接口 403 → 删草稿。

## 8. 注意事项

- 生产环境必须使用 HTTPS。
- 原始令牌不要写进 `.env`、Git、日志、聊天记录或文章内容。
- 服务端日志只打印令牌摘要前 8 位（指纹），不会打印明文。
- 发布令牌没有过期时间，安全性依赖保密和及时轮换；建议 32 字节以上随机值。
- 如果后续需要“发布令牌直接发文章”，只需调整 `ArticleController` 中强制 `isDraft=true` 的两处逻辑，但这会扩大令牌权限，默认不建议。
