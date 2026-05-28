package com.taskhub.util;

import com.taskhub.config.JwtAuthenticationFilter;
import com.taskhub.mapper.ProjectMemberMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 项目权限校验工具类。
 * 所有需要校验项目成员身份的 Controller 方法统一调用这里。
 */
public final class ProjectAuth {

    private ProjectAuth() {}

    /**
     * 判断当前登录用户是否是指定项目的成员。
     * ADMIN 角色拥有所有项目的访问权限。
     */
    public static boolean isMember(ProjectMemberMapper pmMapper, String projectId) {
        String memberId = getCurrentMemberId();
        String role = getCurrentRole();
        if ("ADMIN".equalsIgnoreCase(role)) return true;
        return pmMapper.existsMember(projectId, memberId);
    }

    /**
     * 判断当前登录用户是否是指定项目的成员，否则抛出 IllegalArgumentException。
     */
    public static void requireMember(ProjectMemberMapper pmMapper, String projectId) {
        if (!isMember(pmMapper, projectId)) {
            throw new IllegalArgumentException("你不是该项目成员，无权访问");
        }
    }

    /**
     * 判断当前登录用户是否是指定项目的管理员（owner 或 admin 角色）。
     */
    public static boolean isAdmin(ProjectMemberMapper pmMapper, String projectId) {
        String role = getCurrentRole();
        if ("ADMIN".equalsIgnoreCase(role)) return true;
        // owner 视为管理员
        String memberId = getCurrentMemberId();
        return pmMapper.existsMember(projectId, memberId);
    }

    /**
     * 判断当前登录用户是否是指定项目的管理员，否则抛出异常。
     */
    public static void requireAdmin(ProjectMemberMapper pmMapper, String projectId) {
        if (!isAdmin(pmMapper, projectId)) {
            throw new IllegalArgumentException("你需要是项目管理员才能执行此操作");
        }
    }

    private static String getCurrentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.LoginUser u) {
            return u.memberId();
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    private static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.LoginUser u) {
            return u.role();
        }
        return null;
    }
}
