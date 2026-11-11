# 聚合识别清单 (Aggregates)

## 身份与课程域 (Identity & Course Context)

### 聚合 1：User (用户)
*   **根实体 (Aggregate Root)**：`User`
*   **值对象**：`Email`, `PasswordHash`
*   **不变式 (Invariants)**：
    *   **INV-U01**：用户的邮箱或学号必须在全局范围内唯一。
    *   **INV-U02**：密码必须经过哈希加密后才能持久化，绝不能保存明文。
    *   **INV-U03**：用户必须拥有至少一种角色（Student / Teacher）。
*   **一致性边界**：修改密码、更新个人资料等操作仅在 User 聚合内部完成。

### 聚合 2：Course (课程)
*   **根实体 (Aggregate Root)**：`Course`
*   **包含实体**：`CourseMember`（课程成员）
*   **不变式 (Invariants)**：
    *   **INV-C01**：课程必须有一名且仅有一名主讲教师（Teacher）。
    *   **INV-C02**：加入课程的学生总数不能超过课程设定的最大容量（MaxCapacity）。
    *   **INV-C03**：同一学生不能重复加入同一门课程。
*   **一致性边界**：**CourseMember 被放在 Course 聚合内**，因为学生加入课程时，必须由 `Course` 根实体来校验当前人数是否超额（INV-C02）。若将 CourseMember 独立为聚合，将导致跨聚合的强一致性校验变得极其复杂（需要分布式事务或复杂的补偿机制）。

### 聚合 3：Role (角色/权限)
*   **根实体 (Aggregate Root)**：`Role`
*   **值对象**：`Permission`
*   **不变式 (Invariants)**：
    *   **INV-R01**：角色名称全局唯一。
    *   **INV-R02**：只有 Teacher 角色才拥有创建课程的权限。
*   **一致性边界**：角色权限的增删改需保证在同一事务内生效。

---
*(注：问答域和声誉域的聚合由江思达和柯博补充。)*