package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.dto.ChangePasswordRequest;
import com.career.planning.dto.LoginRequest;
import com.career.planning.dto.RegisterRequest;
import com.career.planning.dto.UpdateUserInfoRequest;
import com.career.planning.entity.StudentInfo;
import com.career.planning.service.StudentInfoService;
import com.career.planning.util.JwtBlacklistUtil;
import com.career.planning.util.JwtUtil;
import com.career.planning.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供用户注册、登录、用户信息查询、用户信息更新、修改密码等接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private StudentInfoService studentInfoService;

    @Autowired
    private JwtBlacklistUtil jwtBlacklistUtil;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 注册 ====================

    /**
     * POST /api/auth/register
     * 用户注册接口
     *
     * @param username 用户名（必填，唯一）
     * @param password 密码（必填，长度严格6位）
     * @return 成功返回用户信息，失败返回错误信息
     */
    @PostMapping("/register")
    public CommonResult<Map<String, Object>> register(@RequestBody RegisterRequest request) {

        String username = request.getUsername();
        String password = request.getPassword();

        // 参数非空校验
        if (!StringUtils.hasText(username)) {
            return CommonResult.validateFailed("用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            return CommonResult.validateFailed("密码不能为空");
        }

        // 用户名长度校验（3-20字符）
        if (username.length() < 3 || username.length() > 20) {
            return CommonResult.validateFailed("用户名长度必须在3-20个字符之间");
        }

        // 密码长度严格6位校验
        if (password.length() != 6) {
            return CommonResult.validateFailed("密码长度必须为6位");
        }

        // 构造用户对象
        StudentInfo studentInfo = new StudentInfo();
        studentInfo.setUsername(username);
        studentInfo.setPassword(password);
        studentInfo.setName(username); // 默认姓名=用户名

        try {
            StudentInfo registered = studentInfoService.register(studentInfo);
            // 注册成功不返回 Token，需要重新登录
            Map<String, Object> result = new HashMap<>();
            result.put("username", registered.getUsername());
            result.put("id", registered.getId());
            return CommonResult.success(result, "注册成功，请登录");
        } catch (RuntimeException e) {
            return CommonResult.validateFailed(e.getMessage());
        }
    }

    // ==================== 登录 ====================

    /**
     * POST /api/auth/login
     * 用户登录接口
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 成功返回 Token + 用户信息，失败返回错误信息
     */
    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody LoginRequest request) {

        String username = request.getUsername();
        String password = request.getPassword();

        // 参数校验
        if (!StringUtils.hasText(username)) {
            return CommonResult.validateFailed("用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            return CommonResult.validateFailed("密码不能为空");
        }

        // 查询用户
        StudentInfo student = studentInfoService.login(username, password);
        if (student == null) {
            return CommonResult.validateFailed("用户名或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(username);

        // 构建返回数据（不含密码）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", student.getUsername());
        data.put("id", student.getId());
        data.put("gender", student.getGender());
        data.put("email", student.getEmail());
        putStudentProfileForClient(data, student);

        log.info("用户 {} 登录成功", username);
        return CommonResult.success(data, "登录成功");
    }

    // ==================== 用户信息查询 ====================

    /**
     * GET /api/auth/user/info
     * 获取当前登录用户信息
     * 需要 Header: Authorization: Bearer {token}
     *
     * @param authorization Bearer Token
     * @return 用户信息（username, gender, email）
     */
    @GetMapping("/user/info")
    public CommonResult<Map<String, Object>> getUserInfo(
            @RequestHeader("Authorization") String authorization) {

        String username = doAuthenticate(authorization);
        if (username == null) {
            return CommonResult.unauthorized("Token 无效或已失效");
        }

        StudentInfo student = studentInfoService.getByUsername(username);
        if (student == null) {
            return CommonResult.unauthorized("用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", student.getUsername());
        data.put("gender", student.getGender());
        data.put("email", student.getEmail());
        putStudentProfileForClient(data, student);

        return CommonResult.success(data);
    }

    /**
     * 登录 / 拉取用户信息时一并返回可编辑的档案字段，便于前端持久展示与再次保存。
     */
    private void putStudentProfileForClient(Map<String, Object> data, StudentInfo s) {
        data.put("name", s.getName());
        data.put("graduationSchool", s.getGraduationSchool());
        data.put("major", s.getMajor());
        data.put("education", s.getEducation());
        data.put("graduationYear", s.getGraduationYear());
        data.put("careerIntention", s.getCareerIntention());
        data.put("personalityTraits", s.getPersonalityTraits());
        data.put("workExperienceYears", s.getWorkExperienceYears());
        data.put("resumeFilePath", s.getResumeFilePath());
    }

    // ==================== 用户信息更新 ====================

    /**
     * PUT /api/auth/user/info
     * 更新当前登录用户信息（gender, email）
     * 需要 Header: Authorization: Bearer {token}
     *
     * @param authorization Bearer Token
     * @return 更新结果
     */
    @PutMapping("/user/info")
    public CommonResult<String> updateUserInfo(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpdateUserInfoRequest request) {

        String username = doAuthenticate(authorization);
        if (username == null) {
            return CommonResult.unauthorized("Token 无效或已失效");
        }

        StudentInfo student = studentInfoService.getByUsername(username);
        if (student == null) {
            return CommonResult.unauthorized("用户不存在");
        }

        String gender = request != null ? request.getGender() : null;
        String email = request != null ? request.getEmail() : null;

        // 更新字段
        if (gender != null) {
            student.setGender(gender);
        }
        if (email != null) {
            // 校验邮箱格式
            if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
                return CommonResult.validateFailed("邮箱格式不正确");
            }
            student.setEmail(email);
        }

        studentInfoService.updateById(student);
        log.info("用户 {} 更新信息成功: gender={}, email={}", username, gender, email);
        return CommonResult.success("信息更新成功");
    }

    // ==================== 修改密码 ====================

    /**
     * PUT /api/auth/password
     * 修改当前登录用户密码
     * 需要 Header: Authorization: Bearer {token}
     *
     * @param authorization     Bearer Token
     * @param request           包含 oldPassword / newPassword / confirmPassword
     * @return 修改成功后返回强制登出提示
     */
    @PutMapping("/password")
    public CommonResult<String> changePassword(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChangePasswordRequest request) {

        String token = extractToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return CommonResult.unauthorized("Token 无效或已过期");
        }
        if (jwtBlacklistUtil.isBlacklisted(token)) {
            return CommonResult.unauthorized("Token 已失效，请重新登录");
        }
        String username = jwtUtil.parseUsername(token);
        if (username == null) {
            return CommonResult.unauthorized("无效的 Token");
        }

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 原密码非空校验
        if (!StringUtils.hasText(oldPassword)) {
            return CommonResult.validateFailed("原密码不能为空");
        }

        // 新密码长度严格6位校验
        if (newPassword == null || newPassword.length() != 6) {
            return CommonResult.validateFailed("新密码长度必须为6位");
        }

        // 确认密码一致性校验
        if (!newPassword.equals(confirmPassword)) {
            return CommonResult.validateFailed("两次输入的新密码不一致");
        }

        // 原密码校验
        StudentInfo student = studentInfoService.login(username, oldPassword);
        if (student == null) {
            return CommonResult.validateFailed("原密码错误");
        }

        // 新密码加密后更新
        student.setPassword(PasswordEncoderUtil.encode(newPassword));
        studentInfoService.updateById(student);

        // 将当前 Token 加入黑名单（强制登出）
        jwtBlacklistUtil.addToBlacklist(token);

        log.info("用户 {} 修改密码成功，Token 已加入黑名单", username);
        return CommonResult.success("密码修改成功，请使用新密码重新登录");
    }

    // ==================== 工具方法 ====================

    /**
     * 从 Authorization Header 中提取 Token（格式："Bearer {token}"）
     */
    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    /**
     * 统一 Token 校验：提取、验证有效性、黑名单检查、解析 username。
     * 成功返回 username，失败返回 null（由调用方统一返回 401）。
     */
    private String doAuthenticate(String authorization) {
        String token = extractToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        if (jwtBlacklistUtil.isBlacklisted(token)) {
            return null;
        }
        return jwtUtil.parseUsername(token);
    }
}
