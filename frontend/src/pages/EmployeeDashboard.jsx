import React, { useState, useEffect, useCallback } from 'react';
import {
  Layout, Menu, Button, Card, Table, Tag, Modal, Form, Input,
  Select, InputNumber, message, Statistic, Progress, Tabs, Spin,
  Badge, Tooltip, Row, Col, Divider, Alert, Space
} from 'antd';
import { useSelector, useDispatch } from 'react-redux';
import { logout } from '../store/authSlice';
import {
  getMySheet, addGoal, submitSheet, getGoalsForSheet,
  logAchievement, getAchievements, getThrustAreas
} from '../services/api';

const { Sider, Content, Header } = Layout;
const { TextArea } = Input;
const QUARTERS = ['Q1', 'Q2', 'Q3', 'Q4'];
const UOM_OPTS = [
  { value: 'NUMERIC', label: 'Numeric' },
  { value: 'PERCENTAGE', label: 'Percentage (%)' },
  { value: 'TIMELINE', label: 'Timeline (Date)' },
  { value: 'ZERO_BASED', label: 'Zero-based (Done/Not Done)' },
];
const STATUS_OPTS = ['NOT_STARTED', 'ON_TRACK', 'COMPLETED'];

export default function EmployeeDashboard() {
  const { name, userId, token } = useSelector(s => s.auth);
  const dispatch = useDispatch();
  const [sheet, setSheet] = useState(null);
  const [goals, setGoals] = useState([]);
  const [thrustAreas, setThrustAreas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addGoalOpen, setAddGoalOpen] = useState(false);
  const [achModal, setAchModal] = useState({ open: false, goal: null });
  const [achData, setAchData] = useState({});
  const [aiCheck, setAiCheck] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [form] = Form.useForm();
  const [achForm] = Form.useForm();
  const [activeTab, setActiveTab] = useState('goals');
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const [sheetRes, taRes] = await Promise.all([getMySheet(), getThrustAreas()]);
      setSheet(sheetRes.data);
      setThrustAreas(taRes.data);
      const goalsRes = await getGoalsForSheet(sheetRes.data.id);
      setGoals(goalsRes.data);
    } catch (e) {
      message.error(e.response?.data?.message || 'Failed to load data');
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const totalWeightage = goals.reduce((s, g) => s + (g.weightage || 0), 0);

  const handleAddGoal = async (vals) => {
    try {
      await addGoal(sheet.id, vals);
      message.success('Goal added successfully!');
      setAddGoalOpen(false);
      form.resetFields();
      setAiCheck(null);
      const res = await getGoalsForSheet(sheet.id);
      setGoals(res.data);
    } catch (e) { message.error(e.response?.data?.message || 'Failed to add goal'); }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const res = await submitSheet(sheet.id);
      setSheet(res.data);
      message.success('Goals submitted for manager review!');
    } catch (e) { message.error(e.response?.data?.message || 'Submission failed'); }
    finally { setSubmitting(false); }
  };

  const handleLogAchievement = async (vals) => {
    try {
      await logAchievement(achModal.goal.id, vals.quarter, {
        actualValue: vals.actualValue,
        actualDate: vals.actualDate,
        status: vals.status,
      });
      message.success('Achievement logged & score computed!');
      setAchModal({ open: false, goal: null });
      achForm.resetFields();
    } catch (e) { message.error(e.response?.data?.message || 'Failed to log achievement'); }
  };

  // AI mock checker (debounce on title change)
  const checkGoalQuality = useCallback((title) => {
    if (!title || title.length < 10) { setAiCheck(null); return; }
    setAiLoading(true);
    setTimeout(() => {
      const checks = {
        specific: title.length > 15,
        measurable: /\d/.test(title),
        timeBound: /q[1-4]|month|week|year|date/i.test(title),
      };
      const score = Object.values(checks).filter(Boolean).length;
      setAiCheck({ checks, score, suggestion: score < 2 ? `Try: "${title} — increase by X% by Q2 2026"` : null });
      setAiLoading(false);
    }, 600);
  }, []);

  const getStatusColor = (status) => {
    const map = { DRAFT: 'default', SUBMITTED: 'warning', APPROVED: 'success', RETURNED: 'error' };
    return map[status] || 'default';
  };

  const goalColumns = [
    { title: 'Title', dataIndex: 'title', key: 'title', render: (t, r) => (
      <div>
        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{t}</div>
        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.thrustArea?.name}</div>
      </div>
    )},
    { title: 'UoM', dataIndex: 'uom', key: 'uom', render: t => <Tag color="blue">{t}</Tag> },
    { title: 'Target', dataIndex: 'targetValue', key: 'target', render: v => v || '—' },
    { title: 'Weight', dataIndex: 'weightage', key: 'weight', render: v => <strong>{v}%</strong> },
    {
      title: 'Shared', dataIndex: 'isShared', key: 'shared',
      render: v => v ? <Tag color="purple">Shared</Tag> : <Tag color="default">Personal</Tag>
    },
    {
      title: 'Log Achievement', key: 'ach',
      render: (_, r) => sheet?.status === 'APPROVED' ? (
        <Button size="small" type="primary" onClick={() => setAchModal({ open: true, goal: r })} id={`log-ach-${r.id}`}>
          Log Achievement
        </Button>
      ) : <Tag>Locked until approved</Tag>
    },
  ];

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
      <Spin size="large" />
    </div>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} style={{ background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)' }}>
        <div className="logo">
          <h2>🎯 Goal Portal</h2>
          <span>Employee Dashboard</span>
        </div>
        <Menu mode="inline" selectedKeys={[activeTab]} onClick={({ key }) => setActiveTab(key)}
          items={[
            { key: 'goals', icon: '🎯', label: 'My Goals' },
            { key: 'achievements', icon: '📊', label: 'Achievements' },
          ]}
        />
        <div style={{ position: 'absolute', bottom: 0, width: '100%', padding: 16, borderTop: '1px solid var(--border)' }}>
          <div style={{ marginBottom: 12 }}>
            <div style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: 14 }}>{name}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>Employee</div>
          </div>
          <Button block onClick={() => dispatch(logout())} size="small" id="logout-btn">Sign Out</Button>
        </div>
      </Sider>

      <Layout>
        <Header style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)', padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 16 }}>
              {activeTab === 'goals' ? 'My Goals' : 'Achievement Tracker'}
            </span>
            {sheet && (
              <span className={`status-badge status-${sheet.status?.toLowerCase()}`}>{sheet.status}</span>
            )}
          </div>
          <div className="ws-badge"><div className="ws-dot" />Live</div>
        </Header>

        <Content style={{ padding: 24, overflowY: 'auto' }}>
          {/* Stats Row */}
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={6}>
              <div className="stat-card blue">
                <div className="stat-label">Total Goals</div>
                <div className="stat-value">{goals.length}<span style={{ fontSize: 16 }}>/8</span></div>
                <div className="stat-icon">🎯</div>
              </div>
            </Col>
            <Col span={6}>
              <div className={`stat-card ${Math.abs(totalWeightage - 100) < 0.01 ? 'green' : 'amber'}`}>
                <div className="stat-label">Total Weightage</div>
                <div className="stat-value">{totalWeightage}%</div>
                <div className="stat-icon">⚖️</div>
              </div>
            </Col>
            <Col span={6}>
              <div className="stat-card purple">
                <div className="stat-label">Sheet Status</div>
                <div className="stat-value" style={{ fontSize: 18 }}>{sheet?.status || 'NEW'}</div>
                <div className="stat-icon">📋</div>
              </div>
            </Col>
            <Col span={6}>
              <div className="stat-card amber">
                <div className="stat-label">Shared Goals</div>
                <div className="stat-value">{goals.filter(g => g.isShared).length}</div>
                <div className="stat-icon">🔗</div>
              </div>
            </Col>
          </Row>

          {/* Weightage warning */}
          {goals.length > 0 && (
            <div className={`weightage-total ${Math.abs(totalWeightage - 100) < 0.01 ? 'weightage-ok' : 'weightage-warn'}`}>
              Total Weightage: {totalWeightage}% {Math.abs(totalWeightage - 100) < 0.01 ? '✅ Ready to submit' : `— Need ${100 - totalWeightage}% more to submit`}
            </div>
          )}

          {/* Action Bar */}
          {sheet?.status === 'DRAFT' || sheet?.status === 'RETURNED' ? (
            <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
              <Button type="primary" id="add-goal-btn" disabled={goals.length >= 8}
                onClick={() => setAddGoalOpen(true)}>
                {goals.length >= 8 ? '✋ Max 8 Goals Reached' : '+ Add Goal'}
              </Button>
              <Button id="submit-sheet-btn" loading={submitting}
                disabled={goals.length === 0 || Math.abs(totalWeightage - 100) > 0.01}
                onClick={handleSubmit}
                style={{ background: 'var(--accent-green)', borderColor: 'var(--accent-green)', color: '#fff' }}>
                Submit for Approval
              </Button>
            </div>
          ) : null}

          {activeTab === 'goals' && (
            <Card title="My Goal Sheet" style={{ marginBottom: 20 }}>
              {goals.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-icon">🎯</div>
                  <h3>No goals yet</h3>
                  <p>Add your first goal to get started. You need at least 1 goal with total weightage = 100%</p>
                </div>
              ) : (
                <Table dataSource={goals} columns={goalColumns} rowKey="id" pagination={false} />
              )}
            </Card>
          )}

          {activeTab === 'achievements' && (
            <Card title="Achievement Log">
              {sheet?.status !== 'APPROVED' ? (
                <Alert message="Goals must be approved before you can log achievements" type="info" showIcon style={{ background: 'rgba(59,130,246,0.1)', border: '1px solid rgba(59,130,246,0.3)' }} />
              ) : (
                <Table dataSource={goals} rowKey="id" pagination={false}
                  columns={[
                    { title: 'Goal', dataIndex: 'title', key: 'title' },
                    { title: 'Target', dataIndex: 'targetValue', key: 'target' },
                    { title: 'Weightage', dataIndex: 'weightage', key: 'w', render: v => `${v}%` },
                    ...QUARTERS.map(q => ({ title: q, key: q, render: () => <Button size="small" onClick={() => setAchModal({ open: true, goal: goals[0] })}>Log</Button> }))
                  ]}
                />
              )}
            </Card>
          )}
        </Content>
      </Layout>

      {/* Add Goal Modal */}
      <Modal title="Add New Goal" open={addGoalOpen} onCancel={() => { setAddGoalOpen(false); setAiCheck(null); form.resetFields(); }}
        footer={null} width={600}>
        <Form form={form} layout="vertical" onFinish={handleAddGoal} id="add-goal-form">
          <Form.Item name="thrustAreaId" label="Thrust Area" rules={[{ required: true }]}>
            <Select placeholder="Select thrust area" id="goal-thrust-area">
              {thrustAreas.map(ta => <Select.Option key={ta.id} value={ta.id}>{ta.name}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="title" label="Goal Title" rules={[{ required: true }]}>
            <Input id="goal-title" placeholder="e.g. Improve CSAT score to 90% by Q4"
              onChange={e => checkGoalQuality(e.target.value)} />
          </Form.Item>
          {aiLoading && <Spin size="small" />}
          {aiCheck && (
            <div className="ai-box">
              <div className="ai-label">🤖 AI SMART Check</div>
              <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
                {[['Specific', aiCheck.checks.specific], ['Measurable', aiCheck.checks.measurable], ['Time-bound', aiCheck.checks.timeBound]].map(([k, v]) => (
                  <Tag key={k} color={v ? 'success' : 'error'}>{v ? '✓' : '✗'} {k}</Tag>
                ))}
              </div>
              {aiCheck.suggestion && <p style={{ color: 'var(--accent-amber)', fontSize: 12 }}>💡 Suggestion: {aiCheck.suggestion}</p>}
            </div>
          )}
          <Form.Item name="description" label="Description">
            <TextArea rows={3} id="goal-desc" placeholder="Describe what success looks like..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="uom" label="Unit of Measurement" rules={[{ required: true, message: 'Please select Unit of Measurement' }]}>
                <Select id="goal-uom" placeholder="Select UoM" options={UOM_OPTS} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="targetValue" label="Target Value">
                <InputNumber id="goal-target" style={{ width: '100%' }} placeholder="e.g. 100" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="weightage" label={`Weightage % (Running total: ${totalWeightage}%)`}
            rules={[
              { required: true },
              { type: 'number', min: 10, message: 'Minimum weightage per goal is 10%' },
              { type: 'number', max: 100, message: 'Max 100%' },
            ]}>
            <InputNumber id="goal-weightage" style={{ width: '100%' }} min={10} max={100} placeholder="Min 10%" />
          </Form.Item>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
            <Button onClick={() => { setAddGoalOpen(false); form.resetFields(); setAiCheck(null); }}>Cancel</Button>
            <Button type="primary" htmlType="submit" id="goal-submit">Add Goal</Button>
          </div>
        </Form>
      </Modal>

      {/* Achievement Modal */}
      <Modal title={`Log Achievement — ${achModal.goal?.title}`} open={achModal.open}
        onCancel={() => { setAchModal({ open: false, goal: null }); achForm.resetFields(); }}
        footer={null}>
        <Form form={achForm} layout="vertical" onFinish={handleLogAchievement}>
          <Form.Item name="quarter" label="Quarter" rules={[{ required: true }]}>
            <Select id="ach-quarter">
              {QUARTERS.map(q => <Select.Option key={q} value={q}>{q}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="actualValue" label="Actual Value" rules={[{ required: true }]}>
            <InputNumber id="ach-actual" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="Status" rules={[{ required: true }]}>
            <Select id="ach-status">
              {STATUS_OPTS.map(s => <Select.Option key={s} value={s}>{s.replace('_', ' ')}</Select.Option>)}
            </Select>
          </Form.Item>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
            <Button onClick={() => { setAchModal({ open: false, goal: null }); achForm.resetFields(); }}>Cancel</Button>
            <Button type="primary" htmlType="submit" id="ach-submit">Log & Compute Score</Button>
          </div>
        </Form>
      </Modal>
    </Layout>
  );
}
