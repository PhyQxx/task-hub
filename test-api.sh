#!/bin/bash

# 任务舱 API 测试脚本
# 测试 7 个 Bug 修复情况

BASE_URL="http://localhost:8096"
PASS=0
FAIL=0

echo "========================================"
echo "任务舱 API 测试 - 7 个 Bug 验证"
echo "========================================"
echo ""

# 测试工具函数
test_api() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_code="$5"
    
    echo -n "[$name] "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint")
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_code" ]; then
        echo "✅ PASS (HTTP $http_code)"
        ((PASS++))
    else
        echo "❌ FAIL (HTTP $http_code, expected $expected_code)"
        echo "   Response: $body"
        ((FAIL++))
    fi
}

# Bug 1: 全部项目概览 - 获取所有任务
echo "===== Bug 1: 全部项目概览 ====="
test_api "获取所有任务 /tasks/all" "GET" "/api/tasks/all" "" "200"
test_api "获取所有任务(空项目ID)" "GET" "/api/tasks/project/" "" "200"
echo ""

# Bug 2: 甘特图数据
echo "===== Bug 2: 甘特图数据 ====="
test_api "甘特图数据" "GET" "/api/gantt/project/PRJ-20260408-001" "" "200"
echo ""

# Bug 3: 任务编辑（中文测试）
echo "===== Bug 3: 任务编辑 ====="
test_api "创建任务(中文标题)" "POST" "/api/tasks" '{"projectId":"PRJ-20260408-001","title":"测试任务中文标题","priority":"MEDIUM","status":"TODO"}' "200"
echo ""

# Bug 4: 新建任务（已修复，测试创建）
echo "===== Bug 4: 新建任务 ====="
test_api "创建任务" "POST" "/api/tasks" '{"projectId":"PRJ-20260408-001","title":"新建任务测试","priority":"HIGH","status":"TODO"}' "200"
echo ""

# Bug 5: 泳道数据（成员数据）
echo "===== Bug 5: 成员列表 ====="
test_api "成员列表" "GET" "/api/members" "" "200"
echo ""

# Bug 6: 工作日志
echo "===== Bug 6: 工作日志 ====="
test_api "工作日志列表" "GET" "/api/work-logs?date=2026-04-09" "" "200"
test_api "创建工作日志" "POST" "/api/work-logs" '{"taskId":"TT-20260409-001","userId":"测试用户","logDate":"2026-04-09","todayDone":"完成测试","tomorrowPlan":"继续测试"}' "200"
echo ""

# Bug 7: 角色列表
echo "===== Bug 7: 角色列表 ====="
test_api "获取所有角色" "GET" "/api/members/roles" "" "200"
echo ""

# 额外测试：项目 CRUD
echo "===== 项目 CRUD ====="
test_api "项目列表" "GET" "/api/projects" "" "200"
test_api "创建项目" "POST" "/api/projects" '{"name":"测试项目API","description":"API测试","ownerId":"1"}' "200"
echo ""

# 输出结果
echo "========================================"
echo "测试结果: ✅ $PASS | ❌ $FAIL"
echo "========================================"

if [ $FAIL -gt 0 ]; then
    exit 1
else
    exit 0
fi
