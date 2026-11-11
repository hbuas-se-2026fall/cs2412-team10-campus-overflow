# CampusOverflow 统一语言术语表 v1.0

> 本表由团队共同维护，覆盖身份域、问答域、声誉域的核心业务概念。术语需在代码、文档、日常沟通中保持严格一致。

| 所属上下文 | 中文术语 | 英文术语 | 业务解释 | 对应代码标识 (类名/方法名) |
| :--- | :--- | :--- | :--- | :--- |
| **身份与课程域** | 用户 | User | 系统的使用者，包含学生、教师等 | `co-identity-domain`: `User.java` |
| **身份与课程域** | 角色 | Role | 用户在系统中的身份标识（如Student, Teacher） | `co-identity-domain`: `Role.java` |
| **身份与课程域** | 学生 | Student | 提问与回答的主体人群 | `co-identity-domain`: `User.java` (属性 isStudent) |
| **身份与课程域** | 教师 | Teacher | 开设课程、管理课程的主体 | `co-identity-domain`: `User.java` (属性 isTeacher) |
| **身份与课程域** | 注册 | Register | 用户创建新账号的动作 | `co-identity-app`: `UserService.register()` |
| **身份与课程域** | 登录 | Login | 验证用户身份并颁发 Token 的过程 | `co-identity-app`: `AuthService.login()` |
| **身份与课程域** | 课程 | Course | 教师开设的具体教学单元 | `co-identity-domain`: `Course.java` |
| **身份与课程域** | 课程成员 | CourseMember | 加入特定课程的学生或教师 | `co-identity-domain`: `Course.java` (内部实体) |
| **身份与课程域** | 加入课程 | JoinCourse | 学生申请或直接加入某课程的事件 | `co-identity-app`: `CourseService.joinCourse()` |
| **身份与课程域** | 退课 | DropCourse | 学生退出某课程的事件 | `co-identity-app`: `CourseService.dropCourse()` |
| **问答域** | 问题 | Question | 用户在论坛发起的求助或讨论帖 | `co-qa-domain`: `Question.java` |
| **问答域** | 回答 | Answer | 针对问题提交的回复内容 | `co-qa-domain`: `Answer.java` |
| **问答域** | 评论 | Comment | 对问题或回答的补充说明 | `co-qa-domain`: `Comment.java` |
| **问答域** | 投票 | Vote | 用户对问题或回答表达赞同/反对 | `co-qa-domain`: `Vote.java` |
| **问答域** | 赞同 | Upvote | 支持内容的正向投票 | `co-qa-domain`: `Vote.UPVOTE` |
| **问答域** | 反对 | Downvote | 反对内容的负向投票 | `co-qa-domain`: `Vote.DOWNVOTE` |
| **问答域** | 标签 | Tag | 用于分类问题的关键词 | `co-qa-domain`: `Tag.java` |
| **问答域** | 采纳 | Accept | 提问者将某条回答标记为最佳答案 | `co-qa-app`: `QuestionService.acceptAnswer()` |
| **问答域** | 问题状态 | QuestionStatus | 问题的生命周期状态（开放/已解决） | `co-qa-domain`: `QuestionStatus.java` |
| **声誉域** | 声誉账户 | ReputationAccount | 记录用户积分的账户 | `co-reputation-domain`: `ReputationAccount.java` |
| **声誉域** | 可用积分 | AvailablePoints | 用户当前可以使用的积分余额 | `co-reputation-domain`: `ReputationAccount.java` |
| **声誉域** | 冻结积分 | FrozenPoints | 发布悬赏时被暂时扣除的积分 | `co-reputation-domain`: `ReputationAccount.java` |
| **声誉域** | 悬赏 | Bounty | 提问者为吸引高质量回答而设置的积分奖励 | `co-reputation-domain`: `Bounty.java` |
| **声誉域** | 徽章 | Badge | 奖励给达到特定成就用户的荣誉标识 | `co-reputation-domain`: `Badge.java` |
| **声誉域** | 徽章授予 | BadgeAward | 将特定徽章颁发给用户的动作事件 | `co-reputation-domain`: `BadgeAward.java` |
| **声誉域** | 积分交易 | PointTransaction | 记录积分增加或减少的流水 | `co-reputation-domain`: `PointTransaction.java` |
| **通用/共享** | 领域事件 | DomainEvent | 表示过去发生的、具有业务意义的事件 | `co-shared-kernel`: `DomainEvent.java` |
| **通用/共享** | 命令 | Command | 触发领域事件的动作意图 | `co-shared-kernel`: `Command.java` |
| **通用/共享** | 聚合 | Aggregate | 数据修改的一致性边界 | `co-shared-kernel`: `AggregateRoot.java` |
| **通用/共享** | 读模型 | ReadModel | 针对查询优化的数据视图 | `co-shared-kernel`: `ReadModel.java` |
| **外部系统** | 邮件服务 | EmailService | 发送注册验证码、通知的外部服务 | `co-infrastructure`: `EmailServiceImpl.java` |
| **外部系统** | OAuth 提供商 | OAuthProvider | 第三方登录服务（如GitHub登录） | `co-infrastructure`: `OAuthClient.java` |

*(注：以上共 32 条术语，完美达标实验要求的 ≥30 条。等队友发来他们具体的类名，你只需在表格里微调即可)*