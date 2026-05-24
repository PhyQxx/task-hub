package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.entity.ProjectMember;
import com.taskhub.mapper.ProjectMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceTest {

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @InjectMocks
    private ProjectPermissionService permissionService;

    private ProjectMember ownerMember;
    private ProjectMember regularMember;
    private ProjectMember viewerMember;

    @BeforeEach
    void setUp() {
        ownerMember = new ProjectMember();
        ownerMember.setProjectId("proj-1");
        ownerMember.setMemberId("user-owner");
        ownerMember.setRole("owner");

        regularMember = new ProjectMember();
        regularMember.setProjectId("proj-1");
        regularMember.setMemberId("user-member");
        regularMember.setRole("member");

        viewerMember = new ProjectMember();
        viewerMember.setProjectId("proj-1");
        viewerMember.setMemberId("user-viewer");
        viewerMember.setRole("viewer");
    }

    @Nested
    @DisplayName("getRole")
    class GetRoleTests {
        @Test
        @DisplayName("returns role when member exists")
        void returnsRoleForMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(ownerMember);
            assertThat(permissionService.getRole("proj-1", "user-owner")).isEqualTo("owner");
        }

        @Test
        @DisplayName("returns null when not a member")
        void returnsNullForNonMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            assertThat(permissionService.getRole("proj-1", "stranger")).isNull();
        }
    }

    @Nested
    @DisplayName("isMember")
    class IsMemberTests {
        @Test
        @DisplayName("returns true for owner")
        void trueForOwner() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(ownerMember);
            assertThat(permissionService.isMember("proj-1", "user-owner")).isTrue();
        }

        @Test
        @DisplayName("returns true for viewer")
        void trueForViewer() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(viewerMember);
            assertThat(permissionService.isMember("proj-1", "user-viewer")).isTrue();
        }

        @Test
        @DisplayName("returns false for non-member")
        void falseForNonMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            assertThat(permissionService.isMember("proj-1", "stranger")).isFalse();
        }
    }

    @Nested
    @DisplayName("isOwnerOrAdmin")
    class IsOwnerOrAdminTests {
        @Test
        @DisplayName("returns true for global admin")
        void trueForGlobalAdmin() {
            assertThat(permissionService.isOwnerOrAdmin("proj-1", "anyone", true)).isTrue();
        }

        @Test
        @DisplayName("returns true for project owner")
        void trueForProjectOwner() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(ownerMember);
            assertThat(permissionService.isOwnerOrAdmin("proj-1", "user-owner", false)).isTrue();
        }

        @Test
        @DisplayName("returns false for regular member")
        void falseForRegularMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(regularMember);
            assertThat(permissionService.isOwnerOrAdmin("proj-1", "user-member", false)).isFalse();
        }

        @Test
        @DisplayName("returns false for viewer")
        void falseForViewer() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(viewerMember);
            assertThat(permissionService.isOwnerOrAdmin("proj-1", "user-viewer", false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canEdit")
    class CanEditTests {
        @Test
        @DisplayName("returns true for global admin")
        void trueForGlobalAdmin() {
            assertThat(permissionService.canEdit("proj-1", "anyone", true)).isTrue();
        }

        @Test
        @DisplayName("returns true for owner")
        void trueForOwner() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(ownerMember);
            assertThat(permissionService.canEdit("proj-1", "user-owner", false)).isTrue();
        }

        @Test
        @DisplayName("returns true for member")
        void trueForMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(regularMember);
            assertThat(permissionService.canEdit("proj-1", "user-member", false)).isTrue();
        }

        @Test
        @DisplayName("returns false for viewer")
        void falseForViewer() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(viewerMember);
            assertThat(permissionService.canEdit("proj-1", "user-viewer", false)).isFalse();
        }
    }

    @Nested
    @DisplayName("canView")
    class CanViewTests {
        @Test
        @DisplayName("returns true for any member")
        void trueForMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(viewerMember);
            assertThat(permissionService.canView("proj-1", "user-viewer")).isTrue();
        }

        @Test
        @DisplayName("returns false for non-member")
        void falseForNonMember() {
            when(projectMemberMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            assertThat(permissionService.canView("proj-1", "stranger")).isFalse();
        }
    }
}
