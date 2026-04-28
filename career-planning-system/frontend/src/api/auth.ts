import request from './index';

// ==================== 注册 ====================

/**
 * 用户注册
 * @param username 用户名（必填，3-20字符）
 * @param password 密码（必填，6位）
 */
export const register = (username: string, password: string) => {
  return request({
    url: '/auth/register',
    method: 'post',
    data: { username, password }
  });
};

// ==================== 登录 ====================

/**
 * 用户登录
 * @param username 用户名
 * @param password 密码（明文）
 * @returns { token, username, id, name, gender, email }
 */
export const login = (username: string, password: string) => {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  });
};

// ==================== 用户信息 ====================

/**
 * 获取当前登录用户信息
 * @returns { username, gender, email, name }
 */
export const getUserInfo = () => {
  return request({
    url: '/auth/user/info',
    method: 'get'
  });
};

/**
 * 更新用户信息（性别、邮箱）
 * @param gender 性别
 * @param email 邮箱
 */
export const updateUserInfo = (gender?: string, email?: string) => {
  return request({
    url: '/auth/user/info',
    method: 'put',
    data: { gender, email }
  });
};

// ==================== 修改密码 ====================

/**
 * 修改密码
 * @param oldPassword 原密码（明文）
 * @param newPassword 新密码（6位）
 * @param confirmPassword 确认新密码（6位）
 * @returns 修改成功后需跳转登录页重新登录
 */
export const changePassword = (oldPassword: string, newPassword: string, confirmPassword: string) => {
  return request({
    url: '/auth/password',
    method: 'put',
    data: { oldPassword, newPassword, confirmPassword }
  });
};
