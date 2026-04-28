package com.career.planning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.career.planning.entity.StudentAbilityProfile;

public interface StudentAbilityProfileService extends IService<StudentAbilityProfile> {
    void saveProfileFromJson(Long studentId, String profileJson);

    String getLatestProfileJson(Long studentId);
}
