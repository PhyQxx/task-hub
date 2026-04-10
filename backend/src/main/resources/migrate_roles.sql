-- 角色权限迁移：将现有成员角色统一为 admin/user 两级
-- 执行前请备份 members 表

-- 将 PM/PD 角色提升为管理员
UPDATE members SET role = 'admin' WHERE role IN ('PD', 'PM');

-- 其余角色降为普通用户
UPDATE members SET role = 'user' WHERE role NOT IN ('admin') AND role IS NOT NULL;
