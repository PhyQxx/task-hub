package com.taskhub.controller;

import com.taskhub.config.JwtAuthenticationFilter.LoginUser;
import com.taskhub.entity.Member;
import com.taskhub.mapper.MemberMapper;
import com.taskhub.util.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberMapper memberMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthController(MemberMapper memberMapper, JwtUtils jwtUtils, PasswordEncoder passwordEncoder) {
        this.memberMapper = memberMapper;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    /** 登录 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");

        if (phone == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 1, "message", "手机号和密码不能为空"));
        }

        Member member = memberMapper.findByPhone(phone);
        if (member == null) {
            return ResponseEntity.ok(Map.of("code", 1, "message", "用户不存在"));
        }

        if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
            return ResponseEntity.ok(Map.of("code", 1, "message", "密码错误"));
        }

        String token = jwtUtils.generateToken(member.getMemberId(), member.getNickname(), member.getRole());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "登录成功",
                "data", Map.of(
                        "token", token,
                        "memberId", member.getMemberId(),
                        "nickname", member.getNickname(),
                        "role", member.getRole() != null ? member.getRole() : "member"
                )
        ));
    }

    /** 注册 */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");
        String nickname = body.get("nickname");

        if (phone == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 1, "message", "手机号和密码不能为空"));
        }

        if (memberMapper.findByPhone(phone) != null) {
            return ResponseEntity.ok(Map.of("code", 1, "message", "手机号已注册"));
        }

        Member member = new Member();
        member.setMemberId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        member.setPhone(phone);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickname(nickname != null ? nickname : "用户" + phone.substring(phone.length() - 4));
        member.setRole("PD"); // 默认角色
        member.setIsActive(1);
        member.setWeeklyCapacity(40f);

        memberMapper.insert(member);

        String token = jwtUtils.generateToken(member.getMemberId(), member.getNickname(), member.getRole());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "注册成功",
                "data", Map.of(
                        "token", token,
                        "memberId", member.getMemberId(),
                        "nickname", member.getNickname(),
                        "role", member.getRole()
                )
        ));
    }

    /** 当前用户 */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.ok(Map.of("code", 1, "message", "未登录"));
        }

        LoginUser user = (LoginUser) authentication.getPrincipal();
        Member member = memberMapper.findByMemberId(user.memberId());

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("memberId", user.memberId());
        result.put("nickname", user.nickname());
        result.put("role", user.role());
        result.put("avatar", member != null ? (member.getAvatar() != null ? member.getAvatar() : "") : "");
        result.put("email", member != null ? (member.getEmail() != null ? member.getEmail() : "") : "");

        return ResponseEntity.ok(java.util.Map.of(
                "code", 0,
                "data", result
        ));
    }

    /** 登出（前端删除 token 即可，这里仅做记录） */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("code", 0, "message", "登出成功"));
    }
}
