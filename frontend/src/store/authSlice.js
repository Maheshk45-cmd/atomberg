import { createSlice } from '@reduxjs/toolkit';

const stored = localStorage.getItem('user');
const initialState = stored ? JSON.parse(stored) : { token: null, role: null, name: null, email: null, userId: null };

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action) {
      const { token, role, name, email, userId } = action.payload;
      state.token = token;
      state.role = role;
      state.name = name;
      state.email = email;
      state.userId = userId;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify({ token, role, name, email, userId }));
    },
    logout(state) {
      state.token = null;
      state.role = null;
      state.name = null;
      state.email = null;
      state.userId = null;
      localStorage.clear();
    }
  }
});

export const { setCredentials, logout } = authSlice.actions;
export default authSlice.reducer;
