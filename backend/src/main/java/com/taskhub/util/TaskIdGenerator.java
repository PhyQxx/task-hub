package com.taskhub.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TaskIdGenerator {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "TT:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String nextTaskId() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String key = KEY_PREFIX + dateStr;
        Long seq = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(86400));
        return "TT-" + dateStr + "-" + String.format("%03d", seq);
    }

    public String nextProjectId() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String key = "PRJ:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(86400));
        return "PRJ-" + dateStr + "-" + String.format("%03d", seq);
    }

    public String nextMilestoneId() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String key = "MS:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(86400));
        return "MS-" + dateStr + "-" + String.format("%03d", seq);
    }

    public String nextWorkLogId() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String key = "WL:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(86400));
        return "WL-" + dateStr + "-" + String.format("%03d", seq);
    }
}
