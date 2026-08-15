package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.dto.user.*;
import csulzc.My_Personal_Blogger.api.dto.user.LoginResponseDTO;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.security.RequestSourceResolver;
import csulzc.My_Personal_Blogger.security.SecurityContextUtil;
import csulzc.My_Personal_Blogger.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final RequestSourceResolver requestSourceResolver;

    private final SecurityContextUtil securityContextUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Result<UserDetailDTO>> register(
            @Valid @RequestBody UserRegisterRequest request,
            HttpServletRequest httpRequest) {
        User.UserRole role = resolveRegisterRole(request, httpRequest);
        UserDetailDTO user = userService.register(request, role);
        return ResponseEntity.ok(Result.success(user, "注册成功"));
    }

    private User.UserRole resolveRegisterRole(UserRegisterRequest request, HttpServletRequest httpRequest) {
        // 前端请求：强制为 USER 权限，忽略请求体中的角色
        if (requestSourceResolver.isFromFrontend(httpRequest)) {
            return User.UserRole.USER;
        }
        // Apifox 等 API 客户端：允许 USER 或 ADMIN
        User.UserRole role = request.getRole() != null ? request.getRole() : User.UserRole.USER;
        if (role == User.UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("注册接口不允许创建超级管理员");
        }
        return role;
    }

    /**
     * 用户登录(返回Token)
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponseDTO>> login(
            @Valid @RequestBody UserLoginRequest request) {
        LoginResponseDTO response = userService.loginWithToken(request);
        return ResponseEntity.ok(Result.success(response, "登录成功"));
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<LoginResponseDTO>> refreshToken(
            @RequestParam String refreshToken) {
        LoginResponseDTO response = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(Result.success(response, "Token刷新成功"));
    }

    /**
     * 获取用户详情（通过ID）
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Result<UserDetailDTO>> getUserDetail(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        UserDetailDTO user = userService.getUserDetail(userId);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 获取当前登录用户个人信息（仅凭Token，无需传入用户ID）
     */
    @GetMapping("/me")
    public ResponseEntity<Result<UserDetailDTO>> getMyInfo() {
        Long currentUserId = securityContextUtil.getCurrentUserId();
        UserDetailDTO user = userService.getUserDetail(currentUserId);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 获取用户详情（通过用户名）
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Result<UserDetailDTO>> getUserDetailByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        UserDetailDTO user = userService.getUserDetailByUsername(username);
        return ResponseEntity.ok(Result.success(user));
    }

    /**
     * 获取用户公开资料
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<Result<UserProfileDTO>> getUserProfile(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(Result.success(profile));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Result<UserDetailDTO>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        UserDetailDTO user = userService.updateUser(userId, request);
        return ResponseEntity.ok(Result.success(user, "更新成功"));
    }

    /**
     * 修改当前登录用户个人信息（仅凭Token，无需传入用户ID）
     */
    @PutMapping("/me")
    public ResponseEntity<Result<UserDetailDTO>> updateMyInfo(
            @Valid @RequestBody UserUpdateRequest request) {
        Long currentUserId = securityContextUtil.getCurrentUserId();
        UserDetailDTO user = userService.updateUser(currentUserId, request);
        return ResponseEntity.ok(Result.success(user, "更新成功"));
    }

    /**
     * 修改密码
     */
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Result<Void>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordChangeRequest request) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(Result.success(null, "密码修改成功"));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Result<Void>> changeMyPassword(
            @Valid @RequestBody UserPasswordChangeRequest request) {
        Long currentUserId = securityContextUtil.getCurrentUserId();
        userService.changePassword(currentUserId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(Result.success(null, "密码修改成功"));
    }

    /**
     * 获取用户活动统计
     */
    @GetMapping("/{userId}/activity")
    public ResponseEntity<Result<UserActivityDTO>> getUserActivity(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        UserActivityDTO activity = userService.getUserActivity(userId);
        return ResponseEntity.ok(Result.success(activity));
    }

    /**
     * 获取所有用户（分页）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序字段
     */
    @GetMapping
    public ResponseEntity<Result<PageResponseDTO<UserProfileDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<UserProfileDTO> users = userService.getAllUsers(page, size, sortBy);
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * 搜索用户
     * @param keyword 关键词
     * @param page 页码（从0开始）
     * @param size 每页大小
     */
    @GetMapping("/search")
    public ResponseEntity<Result<PageResponseDTO<UserProfileDTO>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<UserProfileDTO> users = userService.searchUsers(keyword, page, size);
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * 启用用户（管理员功能）
     */
    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> activateUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.activateUser(userId);
        return ResponseEntity.ok(Result.success(null, "用户已启用"));
    }

    /**
     * 禁用用户（管理员功能）
     */
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> deactivateUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.deactivateUser(userId);
        return ResponseEntity.ok(Result.success(null, "用户已禁用"));
    }

    /**
     * 锁定用户（管理员功能）
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> lockUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.lockUser(userId);
        return ResponseEntity.ok(Result.success(null, "用户已锁定"));
    }

    /**
     * 解锁用户（管理员功能）
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> unlockUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.unlockUser(userId);
        return ResponseEntity.ok(Result.success(null, "用户已解锁"));
    }

    /**
     * 删除用户（管理员功能）
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> deleteUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean softDelete) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        userService.deleteUser(userId, softDelete);
        return ResponseEntity.ok(Result.success(null, softDelete ? "用户已禁用" : "用户已删除"));
    }

    /**
     * 获取活跃用户数
     */
    @GetMapping("/stats/active")
    public ResponseEntity<Result<Long>> countActiveUsers() {
        long count = userService.countActiveUsers();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取总用户数
     */
    @GetMapping("/stats/total")
    public ResponseEntity<Result<Long>> getTotalUserCount() {
        long count = userService.getTotalUserCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取最近活跃用户列表
     */
    @GetMapping("/stats/recently-active")
    public ResponseEntity<Result<List<UserActivityDTO>>> getRecentlyActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 50) {
            throw new IllegalArgumentException("限制数量必须在1-50之间");
        }
        List<UserActivityDTO> users = userService.getRecentlyActiveUsers(limit);
        return ResponseEntity.ok(Result.success(users));
    }
}
