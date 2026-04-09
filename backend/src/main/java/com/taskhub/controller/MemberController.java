package com.taskhub.controller;

import com.taskhub.entity.Member;
import com.taskhub.service.MemberService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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
            "项目经理",    // PM - 项目管理
            "设计师",     // Designer - UI/UX 设计
            "开发者",     // Developer - 后端开发
            "前端开发",   // Frontend - 前端开发
            "测试工程师",  // QA - 测试
            "运维工程师",  // Ops - 运维
            "产品经理",    // Product - 产品
            "其他"        // Other
        ));
    }
}
