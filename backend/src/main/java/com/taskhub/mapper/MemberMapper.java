package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    @Select("SELECT * FROM members WHERE phone = #{phone} LIMIT 1")
    Member findByPhone(String phone);

    @Select("SELECT * FROM members WHERE member_id = #{memberId} LIMIT 1")
    Member findByMemberId(String memberId);
}
