package com.taskhub.ws;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GanttWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/ws/subscribe/{projectId}")
    @SendTo("/topic/project/{projectId}")
    public Map<String, Object> subscribe(@DestinationVariable String projectId) {
        log.info("Client subscribed to project: {}", projectId);
        return Map.of("type", "SUBSCRIBED", "projectId", projectId);
    }

    public void broadcastTaskUpdate(String projectId, String taskId, Map<String, Object> fields, String actor) {
        messagingTemplate.convertAndSend("/topic/project/" + projectId, Map.of(
            "type", "TASK_UPDATED",
            "payload", Map.of("taskId", taskId, "fields", fields, "actor", actor, "ts", System.currentTimeMillis())
        ));
    }

    public void broadcastTaskCreated(String projectId, Object task, String actor) {
        messagingTemplate.convertAndSend("/topic/project/" + projectId, Map.of(
            "type", "TASK_CREATED",
            "payload", Map.of("task", task, "actor", actor, "ts", System.currentTimeMillis())
        ));
    }

    public void broadcastTaskDeleted(String projectId, String taskId, String actor) {
        messagingTemplate.convertAndSend("/topic/project/" + projectId, Map.of(
            "type", "TASK_DELETED",
            "payload", Map.of("taskId", taskId, "actor", actor, "ts", System.currentTimeMillis())
        ));
    }

    public void broadcastMemberLoadAlert(String memberId, int loadPct) {
        messagingTemplate.convertAndSend("/topic/members/" + memberId, Map.of(
            "type", "MEMBER_LOAD_ALERT",
            "payload", Map.of("memberId", memberId, "loadPct", loadPct, "ts", System.currentTimeMillis())
        ));
    }
}
