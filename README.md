# fasaxi-emma
fasaxi 艾玛帝国

## 运行说明

- 仓库不会把 `application*.yml` 打进最终 jar，运行时请通过环境变量或外部配置目录提供配置。
- 管理接口默认关闭；如需使用 `/api/debug/**`、`/api/build/**`、`/api/clear/**`、`/api/obtainCollection`，需要显式配置 `admin-api.enabled=true`、`admin-api.token`，并在允许的 profile 下通过 `X-Admin-Token` 请求头访问。
- CORS 默认关闭；只有显式配置 `cors.allowed-origin-patterns` 后才会放开跨域访问。
- 建议使用本地忽略的配置文件或部署平台密钥管理注入数据库、Redis、OpenList、TMDB、豆瓣等敏感参数。
