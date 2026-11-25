# CampusOverflow

CampusOverflow 是面向高校课程的校园问答平台。本仓库用于软件工程课程实验，包含后端、前端、领域建模文档以及 Lab 1/Lab 2 的 Scrum 交付物。

> 工程代码位于 `campus-overflow-student/`，团队与实验文档位于根目录 `docs/`。

## 团队

| 角色 | 成员 | GitHub |
| --- | --- | --- |
| PO（产品负责人） | 唐志烽 | [luck970](https://github.com/luck970) |
| SM（Scrum Master） | 左焱之| [Flagg32](https://github.com/Flagg32) |
| Developer | 江思达 | [houhai6](https://github.com/houhai6) |
| Developer | 何欣怡 | [hehe309](https://github.com/hehe309) |
| Developer | 柯博 | [Ke-1226](https://github.com/Ke-1226) |

项目看板：[GitHub Projects #3](https://github.com/users/luck970/projects/3/views/1)

## 实验交付物

### Lab 1：团队组建与协作工具链

- [团队章程 v1.0](docs/team/charter-v1.0.md)
- 成员自我介绍：[唐志烽](docs/team/hello-luck970.md)、[Flagg32](docs/team/hello-flagg32.md)、[江思达](docs/team/hello-jiangsida.md)、[何欣怡](docs/team/hello-hexinyi.md)、[柯博](docs/team/hello-kebo.md)
- 角色承诺书：[江思达](docs/team/role-commitment-jiangsida.md)、[柯博](docs/team/role-commitment-kebo.md)；[PO](docs/team/role-commitment-luck970.md)、[SM](docs/team/role-commitment-flagg32.md)、[何欣怡](docs/team/role-commitment-hexinyi.md)为待本人确认草稿

### Lab 2：需求建模工作坊

- [用户故事地图](docs/backlog/user-story-map.md)（[PNG](docs/backlog/user-story-map.png)）
- [Product Backlog](docs/backlog/product-backlog.md)：8 个 Sprint 1 故事，27 SP
- [Sprint 1 Backlog](docs/backlog/sprint-1-backlog.md)：26 个任务及责任人
- [Sprint 1 Planning 纪要](docs/backlog/sprint-1-planning-纪要.md)
- [Gherkin 验收标准](docs/backlog/stories/)：8 个文件，31 个场景
- [用户访谈记录](docs/backlog/interviews/README.md)：**当前为待填写模板，完成真实访谈前不能作为验收证据**

## 当前待补

| 项目 | 状态 |
| --- | --- |
| Lab 1 成员 hello 文档 | 5/5 |
| Lab 1 角色承诺书 | 2/5 已签，3 份待本人确认 |
| Lab 2 真实用户访谈 | 0/3，现有文件均为空白模板 |
| Sprint 1 看板任务卡 | 26 张任务清单及 CSV 已准备，需核对是否全部录入 Projects |
| Sprint 1 看板截图 | 待保存为 `docs/backlog/sprint-1-board.png` |
| Project 外部可见性 | 待确认教师和队员可访问 |

## 本地运行

后端：

```powershell
cd campus-overflow-student/backend
mvn -DskipTests install
mvn spring-boot:run -pl co-bootstrap
```

前端：

```powershell
cd campus-overflow-student/frontend
npm install
npm run dev
```

默认地址：前端 `http://localhost:5173`，后端 `http://localhost:8080`，Swagger `http://localhost:8080/swagger-ui.html`。完整后端需要 MySQL 与 Redis。
