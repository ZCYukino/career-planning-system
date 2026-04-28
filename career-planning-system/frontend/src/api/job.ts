import request from './index';

export const getTopJobs = () => {
  return request({
    url: '/job/analysis/summary',
    method: 'get'
  });
};

export const getJobById = (id: number) => {
  return request({
    url: `/job/info/${id}`,
    method: 'get'
  });
};

export const getAggregatedProfile = (jobName: string) => {
  return request({
    url: '/job/analysis/profile',
    method: 'get',
    params: { jobName }
  });
};

export const clearJobCache = () => {
  return request({
    url: '/job/analysis/cache/clear',
    method: 'post'
  });
};

export const getCareerPaths = (jobName: string) => {
  return request({
    url: '/career-path/paths',
    method: 'get',
    params: { jobName }
  });
};

