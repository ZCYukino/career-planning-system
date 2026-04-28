package com.career.planning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.common.BusinessException;
import com.career.planning.entity.StudentInfo;
import com.career.planning.mapper.StudentInfoMapper;
import com.career.planning.service.StudentInfoService;
import com.career.planning.util.PasswordEncoderUtil;
import org.springframework.stereotype.Service;

@Service
public class StudentInfoServiceImpl extends ServiceImpl<StudentInfoMapper, StudentInfo> implements StudentInfoService {

    /**
     * 用户登录（密码 BCrypt 加密验证）
     */
    @Override
    public StudentInfo login(String username, String password) {
        QueryWrapper<StudentInfo> query = new QueryWrapper<>();
        query.eq("username", username);
        StudentInfo student = getOne(query);
        
        if (student == null) {
            return null;
        }
        
        String storedPassword = student.getPassword();
        if (storedPassword == null) {
            return null;
        }
        
        if (PasswordEncoderUtil.matches(password, storedPassword)) {
            return student;
        }
        
        return null;
    }

    /**
     * 用户注册（密码使用 BCrypt 加密存储）
     */
    @Override
    public StudentInfo register(StudentInfo studentInfo) {
        // 校验用户名唯一性
        QueryWrapper<StudentInfo> query = new QueryWrapper<>();
        query.eq("username", studentInfo.getUsername());
        if (count(query) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 校验邮箱唯一性（仅当非空时）
        if (studentInfo.getEmail() != null && !studentInfo.getEmail().isEmpty()) {
            QueryWrapper<StudentInfo> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", studentInfo.getEmail());
            if (count(emailQuery) > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        // BCrypt 加密密码
        studentInfo.setPassword(PasswordEncoderUtil.encode(studentInfo.getPassword()));

        save(studentInfo);
        return studentInfo;
    }

    /**
     * 根据用户名查询用户
     */
    public StudentInfo getByUsername(String username) {
        return getOne(new QueryWrapper<StudentInfo>().eq("username", username));
    }
}
