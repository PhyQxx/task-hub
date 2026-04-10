package com.taskhub.controller;

import com.taskhub.entity.Member;
import com.taskhub.entity.Task;
import com.taskhub.service.MemberService;
import com.taskhub.service.TaskService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final TaskService taskService;

    @GetMapping
    public ApiResponse<List<Member>> list() {
        return ApiResponse.success(memberService.list());
    }

    @GetMapping("/{memberId}")
    public ApiResponse<Member> get(@PathVariable String memberId) {
        Member member = memberService.getById(memberId);
        if (member == null) return ApiResponse.error(404, "Member not found");
        return ApiResponse.success(member);
    }

    @GetMapping("/role/{role}")
    public ApiResponse<List<Member>> getByRole(@PathVariable String role) {
        return ApiResponse.success(memberService.getByRole(role));
    }

    // 获取所有可选角色
    @GetMapping("/roles")
    public ApiResponse<List<String>> getRoles() {
        return ApiResponse.success(List.of(
            "admin",   // 管理员 - 全部权限
            "user"     // 用户 - 查看 + 基本操作
        ));
    }

    // 获取成员的任务列表
    @GetMapping("/{memberId}/tasks")
    public ApiResponse<List<Task>> memberTasks(@PathVariable String memberId) {
        return ApiResponse.success(taskService.listByMemberId(memberId));
    }
}
