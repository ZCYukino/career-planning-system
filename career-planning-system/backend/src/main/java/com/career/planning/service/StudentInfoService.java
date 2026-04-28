package com.career.planning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.career.planning.entity.StudentInfo;

public interface StudentInfoService extends IService<StudentInfo> {
    StudentInfo login(String username, String password);
    StudentInfo register(StudentInfo studentInfo);
    StudentInfo getByUsername(String username);
}
