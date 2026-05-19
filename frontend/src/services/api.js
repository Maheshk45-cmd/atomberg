import axios from 'axios';

const API = axios.create({ baseURL: '/api' });

API.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

API.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// Auth
export const login = (data) => API.post('/auth/login', data);

// Goal Sheets
export const getMySheet = () => API.get('/goal-sheets/my');
export const getTeamSheets = () => API.get('/goal-sheets/team');
export const getAllSheets = () => API.get('/goal-sheets');
export const addGoal = (sheetId, data) => API.post(`/goal-sheets/${sheetId}/goals`, data);
export const submitSheet = (sheetId) => API.post(`/goal-sheets/${sheetId}/submit`);
export const approveSheet = (sheetId) => API.post(`/goal-sheets/${sheetId}/approve`);
export const returnSheet = (sheetId) => API.post(`/goal-sheets/${sheetId}/return`);
export const getGoalsForSheet = (sheetId) => API.get(`/goal-sheets/${sheetId}/goals`);
export const managerEditGoal = (goalId, data) => API.put(`/goals/${goalId}/manager-edit`, data);
export const adminEditGoal = (goalId, data, reason) =>
  API.put(`/goals/${goalId}/admin-edit`, data, { params: { reason } });

// Achievements
export const logAchievement = (goalId, quarter, data) =>
  API.post(`/goals/${goalId}/achievements/${quarter}`, data);
export const getAchievements = (goalId) => API.get(`/goals/${goalId}/achievements`);

// Check-ins
export const saveCheckin = (sheetId, data) => API.post(`/goal-sheets/${sheetId}/checkins`, data);
export const getCheckins = (sheetId) => API.get(`/goal-sheets/${sheetId}/checkins`);
export const getCheckinDashboard = () => API.get('/checkins/dashboard');

// Shared Goals
export const pushSharedGoal = (data) => API.post('/shared-goals/push', data);

// Reports
export const getAchievementReport = () => API.get('/reports/achievements');
export const getAuditLogs = () => API.get('/reports/audit-logs');
export const getPredictiveScores = (employeeId) => API.get(`/reports/predictive/${employeeId}`);

// Admin — Config
export const getAllUsers = () => API.get('/users');
export const getThrustAreas = () => API.get('/thrust-areas');
export const seedDemoData = () => API.post('/admin/seed-demo');

// Admin — Thrust Area CRUD
export const getAdminThrustAreas = () => API.get('/admin/thrust-areas');
export const createThrustArea = (data) => API.post('/admin/thrust-areas', data);
export const updateThrustArea = (id, data) => API.put(`/admin/thrust-areas/${id}`, data);

// Admin — Cycle CRUD
export const getAdminCycles = () => API.get('/admin/cycles');
export const createCycle = (data) => API.post('/admin/cycles', data);
export const updateCycle = (id, data) => API.put(`/admin/cycles/${id}`, data);

export default API;
