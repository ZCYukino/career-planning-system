import request from './index';

// 创建报告生成任务（异步）
export const createReportTask = (studentId: number, targetJobName: string) => {
  return request({
    url: '/report/create',
    method: 'post',
    params: { studentId, targetJobName }
  });
};

// 轮询报告生成状态
export const getReportStatus = (reportId: number) => {
  return request({
    url: `/report/status/${reportId}`,
    method: 'get'
  });
};

export const getLatestReport = (studentId: number) => {
  return request({
    url: '/report/latest',
    method: 'get',
    params: { studentId }
  });
};

export const updateReport = (payload: { reportId: number; studentId: number; reportContent: string }) => {
  return request({
    url: '/report/update',
    method: 'put',
    data: payload
  });
};
