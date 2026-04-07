package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.entity.Member;
import com.taskhub.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    public Member getById(String memberId) {
        return memberMapper.selectOne(new QueryWrapper<Member>().eq("member_id", memberId));
    }

    public List<Member> list() {
        return memberMapper.selectList(new QueryWrapper<Member>().eq("is_active", 1));
    }

    public Member create(Member member) {
        memberMapper.insert(member);
        return member;
    }

    public void update(Member member) {
        memberMapper.updateById(member);
    }

    public void delete(String memberId) {
        memberMapper.delete(new QueryWrapper<Member>().eq("member_id", memberId));
    }

    public List<Member> getByRole(String role) {
        return memberMapper.selectList(new QueryWrapper<Member>().eq("role", role).eq("is_active", 1));
    }
}
