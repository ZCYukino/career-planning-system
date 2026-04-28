import request from './index';

export const updateStudentInfo = (id: number, data: any) => {
  return request({
    url: `/student/${id}`,
    method: 'put',
    data
  });
};

export const uploadResume = (formData: FormData): Promise<string> => {
  return request({
    url: '/student/upload/resume',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }) as Promise<string>;
};

// 创建个人画像生成任务（异步）
export const createAbilityProfileTask = (id: number) => {
  return request({
    url: `/student/${id}/profile/create`,
    method: 'post'
  });
};

// 轮询个人画像生成状态
export const getAbilityProfileStatus = (studentId: number) => {
  return request({
    url: `/student/${studentId}/profile/status`,
    method: 'get'
  });
};

export const getLatestAbilityProfile = (id: number) => {
  return request({
    url: `/student/${id}/profile/latest`,
    method: 'get'
  });
};
