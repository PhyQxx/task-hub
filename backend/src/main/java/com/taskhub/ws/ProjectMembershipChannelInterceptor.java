package com.taskhub.ws;

import com.taskhub.config.JwtAuthenticationFilter;
import com.taskhub.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 校验 WebSocket STOMP 订阅请求：用户必须登录，且是目标项目组成员。
 * 仅拦截 /topic/project/{projectId} 的 SUBSCRIBE 命令。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectMembershipChannelInterceptor implements ChannelInterceptor {

    private final ProjectMemberMapper projectMemberMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) return message;

        // 仅校验 /topic/project/{projectId} 格式
        if (!destination.startsWith("/topic/project/")) {
            return message;
        }

        String projectId = destination.substring("/topic/project/".length());
        if (projectId.isEmpty()) {
            log.warn("WebSocket subscribe to empty projectId, rejecting");
            throw new IllegalArgumentException("projectId cannot be empty");
        }

        // 提取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtAuthenticationFilter.LoginUser loginUser)) {
            log.warn("Unauthenticated WebSocket subscribe attempt to project {}", projectId);
            throw new IllegalStateException("Not authenticated");
        }

        // 校验项目成员身份
        if (!projectMemberMapper.existsMember(projectId, loginUser.memberId())) {
            log.warn("User {} attempted to subscribe to project {} without membership", loginUser.memberId(), projectId);
            throw new IllegalArgumentException("你不是该项目成员，无权订阅");
        }

        log.debug("User {} subscribed to project {}", loginUser.memberId(), projectId);
        return message;
    }
}
