import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Card, Table, Modal, Form, Input, Tag, message, Statistic, Row, Col, Typography, Tooltip, InputNumber, notification, Alert, Select } from 'antd';
import { useSelector, useDispatch } from 'react-redux';
import { logout } from '../store/authSlice';
import { getTeamSheets, approveSheet, returnSheet, managerEditGoal, getGoalsForSheet, saveCheckin, getCheckins, getCheckinDashboard } from '../services/api';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const { Sider, Content, Header } = Layout;
const { TextArea } = Input;
const { Text, Title } = Typography;

export default function ManagerDashboard() {
  const { name, userId } = useSelector(s => s.auth);
  const dispatch = useDispatch();
  const [activeTab, setActiveTab] = useState('review');
  const [teamSheets, setTeamSheets] = useState([]);
  const [checkinData, setCheckinData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewSheetModal, setViewSheetModal] = useState({ open: false, sheet: null, goals: [] });
  const [editGoalModal, setEditGoalModal] = useState({ open: false, goal: null });
  const [checkinModal, setCheckinModal] = useState({ open: false, sheet: null });
  const [editForm] = Form.useForm();
  const [checkinForm] = Form.useForm();
  const [aiGenerating, setAiGenerating] = useState(false);

  useEffect(() => {
    loadData();

    // WebSocket for real-time notifications
    const socket = new SockJS('/api/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = () => {}; // Disable debug logging
    stompClient.connect({}, () => {
      stompClient.subscribe(`/topic/manager/${userId}`, (msg) => {
        const data = JSON.parse(msg.body);
        if (data.type === 'GOAL_SUBMITTED') {
          notification.success({
            message: 'Goals Submitted! 🚀',
            description: `${data.employeeName} just submitted their goal sheet for approval.`,
            placement: 'topRight',
            duration: 5,
            style: { background: 'var(--bg-card)', border: '1px solid var(--accent-blue)', borderRadius: '8px' }
          });
          loadData(); // Refresh list
        }
      });
    });

    return () => {
      if (stompClient) stompClient.disconnect();
    };
  }, [userId]);

  const loadData = async () => {
    try {
      const [sheetsRes, checkinRes] = await Promise.all([
        getTeamSheets(),
        getCheckinDashboard()
      ]);
      setTeamSheets(sheetsRes.data);
      setCheckinData(checkinRes.data.completionData || []);
    } catch (e) {
      message.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const openSheet = async (sheet) => {
    try {
      const res = await getGoalsForSheet(sheet.id);
      setViewSheetModal({ open: true, sheet, goals: res.data });
    } catch (e) {
      message.error('Failed to load goals');
    }
  };

  const handleApprove = async (sheetId) => {
    try {
      await approveSheet(sheetId);
      message.success('Goal sheet approved and locked!');
      setViewSheetModal({ ...viewSheetModal, open: false });
      loadData();
    } catch (e) { message.error(e.response?.data?.message || 'Approval failed'); }
  };

  const handleReturn = async (sheetId) => {
    try {
      await returnSheet(sheetId);
      message.warning('Goal sheet returned to employee');
      setViewSheetModal({ ...viewSheetModal, open: false });
      loadData();
    } catch (e) { message.error('Return failed'); }
  };

  const handleEditSubmit = async (vals) => {
    try {
      await managerEditGoal(editGoalModal.goal.id, vals);
      message.success('Target updated successfully');
      setEditGoalModal({ open: false, goal: null });
      openSheet(viewSheetModal.sheet); // refresh goals
    } catch (e) { message.error(e.response?.data?.message || 'Edit failed'); }
  };

  const handleSaveCheckin = async (vals) => {
    try {
      await saveCheckin(checkinModal.sheet.id, vals);
      message.success('Check-in comment saved!');
      setCheckinModal({ open: false, sheet: null });
      checkinForm.resetFields();
      loadData();
    } catch (e) { message.error('Failed to save comment'); }
  };

  const generateAiComment = () => {
    const rawNote = checkinForm.getFieldValue('comment');
    if (!rawNote || rawNote.length < 5) return message.warning('Type a few rough notes first!');
    setAiGenerating(true);
    setTimeout(() => {
      // Mock AI Generation
      const generated = `[AI Enhanced]: Based on your note "${rawNote}": The employee demonstrated strong execution this quarter. While targets were mostly met, focus should be shifted towards improving customer satisfaction metrics in the upcoming quarter. Highly commendable effort on the core deliverables.`;
      checkinForm.setFieldsValue({ comment: generated });
      setAiGenerating(false);
    }, 1000);
  };

  const columns = [
    { title: 'Employee', dataIndex: ['employee', 'name'], key: 'emp' },
    { title: 'Status', dataIndex: 'status', key: 'status', render: s => (
      <span className={`status-badge status-${s.toLowerCase()}`}>{s}</span>
    )},
    { title: 'Submitted At', dataIndex: 'submittedAt', key: 'date', render: d => d ? new Date(d).toLocaleDateString() : '—' },
    { title: 'Action', key: 'action', render: (_, r) => (
      <Button type="primary" size="small" onClick={() => openSheet(r)}>Review Goals</Button>
    )}
  ];

  const dashboardCols = [
    { title: 'Employee', dataIndex: 'employeeName', key: 'emp' },
    ...['Q1', 'Q2', 'Q3', 'Q4'].map(q => ({
      title: q + ' Check-in', key: q,
      render: (_, r) => r.quarters[q] ? <Tag color="success">Completed</Tag> : <Tag color="warning">Pending</Tag>
    })),
    { title: 'Action', key: 'act', render: (_, r) => (
      <Button size="small" onClick={() => setCheckinModal({ open: true, sheet: { id: r.sheetId, employee: { name: r.employeeName } } })}>
        Add Comment
      </Button>
    )}
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} style={{ background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)' }}>
        <div className="logo">
          <h2>🎯 Goal Portal</h2>
          <span>Manager Dashboard</span>
        </div>
        <Menu mode="inline" selectedKeys={[activeTab]} onClick={({ key }) => setActiveTab(key)}
          items={[
            { key: 'review', icon: '📝', label: 'Goal Approvals' },
            { key: 'checkin', icon: '💬', label: 'Quarterly Check-ins' },
          ]}
        />
        <div style={{ position: 'absolute', bottom: 0, width: '100%', padding: 16, borderTop: '1px solid var(--border)' }}>
          <div style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{name}</div>
          <Button block onClick={() => dispatch(logout())} size="small" style={{ marginTop: 8 }}>Sign Out</Button>
        </div>
      </Sider>

      <Layout>
        <Header style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)', padding: '0 24px', display: 'flex', alignItems: 'center' }}>
          <span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 16 }}>
            {activeTab === 'review' ? 'Pending Goal Approvals' : 'Check-in Completion Dashboard'}
          </span>
        </Header>
        <Content style={{ padding: 24, overflowY: 'auto' }}>
          {activeTab === 'review' && (
            <Card title="Team Goal Sheets">
              <Table dataSource={teamSheets} columns={columns} rowKey="id" loading={loading} />
            </Card>
          )}

          {activeTab === 'checkin' && (
            <Card title="Quarterly Check-in Status">
              <Table dataSource={checkinData} columns={dashboardCols} rowKey="employeeId" loading={loading} />
            </Card>
          )}
        </Content>
      </Layout>

      {/* Review Sheet Modal */}
      <Modal title={`Review Goals — ${viewSheetModal.sheet?.employee?.name}`} open={viewSheetModal.open}
        onCancel={() => setViewSheetModal({ open: false, sheet: null, goals: [] })} width={900}
        footer={viewSheetModal.sheet?.status === 'SUBMITTED' ? [
          <Button key="ret" danger onClick={() => handleReturn(viewSheetModal.sheet.id)}>Return to Employee</Button>,
          <Button key="app" type="primary" style={{ background: 'var(--accent-green)' }} onClick={() => handleApprove(viewSheetModal.sheet.id)}>Approve & Lock</Button>
        ] : null}>
        <Table dataSource={viewSheetModal.goals} rowKey="id" pagination={false}
          columns={[
            { title: 'Title', dataIndex: 'title', key: 'title', render: (t, r) => (
              <div>
                <div style={{ fontWeight: 600 }}>{t}</div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.uom}</div>
              </div>
            )},
            { title: 'Target', dataIndex: 'targetValue', key: 'target' },
            { title: 'Weight', dataIndex: 'weightage', key: 'w', render: w => `${w}%` },
            { title: 'Shared', dataIndex: 'isShared', key: 'shared', render: s => s ? <Tag color="purple">Yes</Tag> : 'No' },
            { title: 'Action', key: 'act', render: (_, r) => (
              viewSheetModal.sheet?.status === 'SUBMITTED' && !r.isShared ?
                <Button size="small" onClick={() => {
                  setEditGoalModal({ open: true, goal: r });
                  editForm.setFieldsValue({ targetValue: r.targetValue, weightage: r.weightage });
                }}>Edit</Button> : null
            )}
          ]}
        />
      </Modal>

      {/* Manager Edit Goal Modal */}
      <Modal title="Edit Goal Before Approval" open={editGoalModal.open}
        onCancel={() => setEditGoalModal({ open: false, goal: null })} footer={null}>
        <Alert message="Changes made here are inline and will be approved automatically once you approve the sheet." type="info" showIcon style={{ marginBottom: 16 }} />
        <Form form={editForm} layout="vertical" onFinish={handleEditSubmit}>
          <Form.Item name="targetValue" label="Target Value" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="weightage" label="Weightage %" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={10} max={100} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>Save Changes</Button>
        </Form>
      </Modal>

      {/* Checkin Comment Modal */}
      <Modal title={`Add Check-in Comment — ${checkinModal.sheet?.employee?.name}`} open={checkinModal.open}
        onCancel={() => setCheckinModal({ open: false, sheet: null })} footer={null}>
        <Form form={checkinForm} layout="vertical" onFinish={handleSaveCheckin}>
          <Form.Item name="quarter" label="Select Quarter" rules={[{ required: true }]}>
            <Select options={['Q1', 'Q2', 'Q3', 'Q4'].map(q => ({ value: q, label: q }))} />
          </Form.Item>
          <Form.Item name="comment" label="Discussion Notes" rules={[{ required: true }]}>
            <TextArea rows={4} placeholder="Type rough notes here..." />
          </Form.Item>
          <div className="ai-box" style={{ marginBottom: 16 }}>
            <div className="ai-label">🤖 AI Magic Refine</div>
            <p>Turn your rough notes into a professional performance comment.</p>
            <Button size="small" type="dashed" loading={aiGenerating} onClick={generateAiComment} style={{ marginTop: 8 }}>
              ✨ Enhance with AI
            </Button>
          </div>
          <Button type="primary" htmlType="submit" block>Save Check-in Comment</Button>
        </Form>
      </Modal>
    </Layout>
  );
}
