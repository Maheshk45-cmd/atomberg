import React, { useState } from 'react';
import { Form, Input, Button, message, Card } from 'antd';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { setCredentials } from '../store/authSlice';
import { login } from '../services/api';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const res = await login(values);
      dispatch(setCredentials(res.data));
      message.success(`Welcome back, ${res.data.name}!`);
      const role = res.data.role;
      if (role === 'EMPLOYEE') navigate('/employee');
      else if (role === 'MANAGER') navigate('/manager');
      else navigate('/admin');
    } catch (err) {
      message.error(err.response?.data?.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-primary)', padding: 24 }}>
      {/* Background glow */}
      <div style={{ position: 'fixed', top: '20%', left: '30%', width: 400, height: 400, background: 'radial-gradient(circle, rgba(59,130,246,0.08) 0%, transparent 70%)', pointerEvents: 'none' }} />
      <div style={{ position: 'fixed', bottom: '20%', right: '30%', width: 300, height: 300, background: 'radial-gradient(circle, rgba(139,92,246,0.08) 0%, transparent 70%)', pointerEvents: 'none' }} />

      <div style={{ width: '100%', maxWidth: 420 }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 40 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 64, height: 64, borderRadius: 16, background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)', marginBottom: 20, boxShadow: '0 0 30px rgba(59,130,246,0.4)' }}>
            <span style={{ fontSize: 28 }}>🎯</span>
          </div>
          <h1 style={{ fontSize: 28, fontWeight: 800, background: 'linear-gradient(135deg, #f0f4ff, #3b82f6)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', marginBottom: 6 }}>
            Goal Portal
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>AtomQuest Hackathon 1.0 • Team Antigravity</p>
        </div>

        <Card style={{ borderRadius: 16, border: '1px solid var(--border)', background: 'var(--bg-card)', boxShadow: '0 20px 60px rgba(0,0,0,0.5)' }}>
          <div style={{ marginBottom: 28 }}>
            <h2 style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 20, marginBottom: 6 }}>Sign In</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Access your performance dashboard</p>
          </div>

          <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
            <Form.Item name="email" label="Email Address" rules={[{ required: true, type: 'email' }]}>
              <Input placeholder="you@company.com" size="large" id="login-email" />
            </Form.Item>
            <Form.Item name="password" label="Password" rules={[{ required: true }]}>
              <Input.Password placeholder="Enter password" size="large" id="login-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large" id="login-submit" style={{ marginTop: 8, height: 48, fontSize: 15 }}>
              Sign In
            </Button>
          </Form>

          {/* Demo credentials */}
          <div style={{ marginTop: 24, padding: '16px', background: 'rgba(59,130,246,0.06)', borderRadius: 10, border: '1px solid rgba(59,130,246,0.15)' }}>
            <p style={{ color: 'var(--accent-blue)', fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 }}>Demo Credentials</p>
            {[
              { role: 'Employee', email: 'priya@atomquest.com', pass: 'Employee@123' },
              { role: 'Manager', email: 'manager@atomquest.com', pass: 'Manager@123' },
              { role: 'Admin', email: 'admin@atomquest.com', pass: 'Admin@123' },
            ].map(c => (
              <div key={c.role} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span style={{ color: 'var(--text-secondary)', fontSize: 12, fontWeight: 600 }}>{c.role}:</span>
                <span style={{ color: 'var(--text-muted)', fontSize: 11 }}>{c.email}</span>
              </div>
            ))}
            <p style={{ color: 'var(--text-muted)', fontSize: 11, marginTop: 6 }}>Password: <code style={{ color: 'var(--accent-green)' }}>Employee@123 / Manager@123 / Admin@123</code></p>
          </div>
        </Card>
      </div>
    </div>
  );
}
