# 1. 未来功能/增强点 (Future Features/Enhancements)

对于小项目，直接实例化DAO是可以的。如果项目变大，可以考虑依赖注入。

# 2. 潜在的技术债/需要重构的地方 (Potential Technical Debt/Refactoring Areas)

目前大部分代码缺乏测少的测试，实际效果难以保证。

# 3. 待讨论的问题/不确定的决策 (Discussion Points/Uncertain Decisions)

1. ~~书籍books，作者authors，借阅loans的删除策略
这三项当中，任意一项的删除都会彼此影响。
删除Book的时候，如果有Loan借阅记录，该如何处理?
删除Author的时候，如果有Book记录，该如何处理?~~



# 4.临时的笔记/调研点 (Temporary Notes/Research Points)

我想看狼与香辛料。

# 5. 已知的小Bug或待修复的问题 (Minor Known Bugs/Issues to Fix)

目前完成的Servlet在开发的过程中忘了添加对Session的检查，之后需要在用户每次操作检查是否有对应的权限。



