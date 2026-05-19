import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, Card, Table, Modal, Form, Input, Select, InputNumber, message, Tag, Space, Alert, Switch, DatePicker } from 'antd';
import { useSelector, useDispatch } from 'react-redux';
import { logout } from '../store/authSlice';
import {
  getAllSheets, getAchievementReport, getAuditLogs, pushSharedGoal,
  getThrustAreas, getAllUsers, seedDemoData, getCheckinDashboard,
  getAdminThrustAreas, createThrustArea, updateThrustArea,
  getAdminCycles, createCycle, updateCycle
} from '../services/api';
import dayjs from 'dayjs';

const { Sider, Content, Header } = Layout;
const { TextArea } = Input;

export default function AdminDashboard() {
  const { name } = useSelector(s => s.auth);
  const dispatch = useDispatch();
  const [activeTab, setActiveTab] = useState('reports');
  const [reportData, setReportData] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [seeding, setSeeding] = useState(false);
  const [sharedGoalModal, setSharedGoalModal] = useState(false);
  const [form] = Form.useForm();
  const [thrustAreas, setThrustAreas] = useState([]);
  const [users, setUsers] = useState([]);
  const [completionData, setCompletionData] = useState([]);

  // System Config state
  const [configThrustAreas, setConfigThrustAreas] = useState([]);
  const [cycles, setCycles] = useState([]);
  const [taModal, setTaModal] = useState({ open: false, record: null });
  const [cycleModal, setCycleModal] = useState({ open: false, record: null });
  const [taForm] = Form.useForm();
  const [cycleForm] = Form.useForm();
  const [configLoading, setConfigLoading] = useState(false);

  useEffect(() => {
    loadData();
    getThrustAreas().then(res => setThrustAreas(res.data)).catch(() => {});
    getAllUsers().then(res => setUsers(res.data.filter(u => u.role === 'EMPLOYEE'))).catch(() => {});
  }, [activeTab]);

  useEffect(() => {
    if (activeTab === 'config') {
      loadConfigData();
    }
  }, [activeTab]);

  const loadData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'reports') {
        const res = await getAchievementReport();
        setReportData(res.data);
      } else if (activeTab === 'audit') {
        const res = await getAuditLogs();
        setAuditLogs(res.data);
      } else if (activeTab === 'completion') {
        const res = await getCheckinDashboard();
        setCompletionData(res.data.completionData || []);
      }
    } catch (e) {
      message.error('Failed to load data');
    } finally { setLoading(false); }
  };

  const loadConfigData = async () => {
    setConfigLoading(true);
    try {
      const [taRes, cycleRes] = await Promise.all([getAdminThrustAreas(), getAdminCycles()]);
      setConfigThrustAreas(taRes.data);
      setCycles(cycleRes.data);
    } catch (e) {
      message.error('Failed to load configuration data');
    } finally { setConfigLoading(false); }
  };

  const handlePushSharedGoal = async (vals) => {
    try {
      await pushSharedGoal(vals);
      message.success('Shared goal pushed to selected employees successfully!');
      setSharedGoalModal(false);
      form.resetFields();
    } catch (e) { message.error(e.response?.data?.message || 'Failed to push goal'); }
  };

  const handleSeed = async () => {
    setSeeding(true);
    try {
      await seedDemoData();
      message.success('Demo data seeded successfully! You can now log in as priya@atomquest.com or manager@atomquest.com');
      loadData();
    } catch (e) { message.error('Seed failed'); }
    finally { setSeeding(false); }
  };

  // ── Thrust Area Handlers ──────────────────────────────────────────────────
  const openTaModal = (record = null) => {
    setTaModal({ open: true, record });
    if (record) {
      taForm.setFieldsValue({ name: record.name, description: record.description, isActive: record.active });
    } else {
      taForm.resetFields();
      taForm.setFieldsValue({ isActive: true });
    }
  };

  const handleSaveTa = async (vals) => {
    try {
      if (taModal.record) {
        await updateThrustArea(taModal.record.id, vals);
        message.success('Thrust Area updated!');
      } else {
        await createThrustArea(vals);
        message.success('Thrust Area created!');
      }
      setTaModal({ open: false, record: null });
      taForm.resetFields();
      loadConfigData();
    } catch (e) { message.error(e.response?.data?.message || 'Operation failed'); }
  };

  // ── Cycle Handlers ────────────────────────────────────────────────────────
  const openCycleModal = (record = null) => {
    setCycleModal({ open: true, record });
    if (record) {
      cycleForm.setFieldsValue({
        year: record.year,
        startDate: record.startDate ? dayjs(record.startDate) : null,
        endDate: record.endDate ? dayjs(record.endDate) : null,
        isActive: record.active
      });
    } else {
      cycleForm.resetFields();
      cycleForm.setFieldsValue({ isActive: false });
    }
  };

  const handleSaveCycle = async (vals) => {
    try {
      const payload = {
        ...vals,
        startDate: vals.startDate ? vals.startDate.format('YYYY-MM-DD') : null,
        endDate: vals.endDate ? vals.endDate.format('YYYY-MM-DD') : null,
      };
      if (cycleModal.record) {
        await updateCycle(cycleModal.record.id, payload);
        message.success('Cycle updated!');
      } else {
        await createCycle(payload);
        message.success('Cycle created!');
      }
      setCycleModal({ open: false, record: null });
      cycleForm.resetFields();
      loadConfigData();
    } catch (e) { message.error(e.response?.data?.message || 'Operation failed'); }
  };

  // Dynamic columns for report table
  const reportCols = reportData.length > 0 ? Object.keys(reportData[0]).map(k => ({
    title: k, dataIndex: k, key: k,
    render: v => typeof v === 'number' ? v.toFixed(2) : (v || '—')
  })) : [];

  const auditCols = [
    { title: 'Timestamp', dataIndex: 'changedAt', key: 'date', render: d => new Date(d).toLocaleString() },
    { title: 'Admin', dataIndex: ['changedBy', 'name'], key: 'admin' },
    { title: 'Field Changed', dataIndex: 'fieldName', key: 'field' },
    { title: 'Old Value', dataIndex: 'oldValue', key: 'old' },
    { title: 'New Value', dataIndex: 'newValue', key: 'new', render: v => <span style={{ color: 'var(--accent-amber)', fontWeight: 'bold' }}>{v}</span> },
    { title: 'Reason', dataIndex: 'reason', key: 'reason' },
  ];

  const completionCols = [
    { title: 'Employee', dataIndex: 'employeeName', key: 'emp' },
    ...['Q1', 'Q2', 'Q3', 'Q4'].map(q => ({
      title: q + ' Check-in', key: q,
      render: (_, r) => r.quarters[q] ? <Tag color="success">Completed</Tag> : <Tag color="warning">Pending</Tag>
    }))
  ];

  const thrustAreaCols = [
    { title: 'Name', dataIndex: 'name', key: 'name', render: (t) => <strong>{t}</strong> },
    { title: 'Description', dataIndex: 'description', key: 'desc', render: d => d || '—' },
    {
      title: 'Status', dataIndex: 'active', key: 'status',
      render: v => v ? <Tag color="success">Active</Tag> : <Tag color="default">Inactive</Tag>
    },
    {
      title: 'Action', key: 'act',
      render: (_, r) => <Button size="small" onClick={() => openTaModal(r)}>Edit</Button>
    }
  ];

  const cycleCols = [
    { title: 'Year', dataIndex: 'year', key: 'year' },
    { title: 'Start Date', dataIndex: 'startDate', key: 'start' },
    { title: 'End Date', dataIndex: 'endDate', key: 'end' },
    {
      title: 'Status', dataIndex: 'active', key: 'status',
      render: v => v ? <Tag color="blue">Active</Tag> : <Tag color="default">Inactive</Tag>
    },
    {
      title: 'Action', key: 'act',
      render: (_, r) => <Button size="small" onClick={() => openCycleModal(r)}>Edit</Button>
    }
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} style={{ background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)' }}>
        <div className="logo">
          <h2>🎯 Goal Portal</h2>
          <span>Admin HR Control</span>
        </div>
        <Menu mode="inline" selectedKeys={[activeTab]} onClick={({ key }) => setActiveTab(key)}
          items={[
            { key: 'reports', icon: '📊', label: 'Achievement Reports' },
            { key: 'completion', icon: '✅', label: 'Completion Tracking' },
            { key: 'shared', icon: '🔗', label: 'Push Shared Goals' },
            { key: 'audit', icon: '🛡️', label: 'Audit Trail Logs' },
            { key: 'config', icon: '⚙️', label: 'System Configuration' },
            { key: 'demo', icon: '⚡', label: 'One-Click Demo' },
          ]}
        />
        <div style={{ position: 'absolute', bottom: 0, width: '100%', padding: 16, borderTop: '1px solid var(--border)' }}>
          <div style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{name}</div>
          <Button block onClick={() => dispatch(logout())} size="small" style={{ marginTop: 8 }}>Sign Out</Button>
        </div>
      </Sider>

      <Layout>
        <Header style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)', padding: '0 24px' }}>
          <span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 16 }}>
            HR &amp; System Administration
          </span>
        </Header>
        <Content style={{ padding: 24, overflowY: 'auto' }}>

          {activeTab === 'reports' && (
            <Card title="End of Year Achievement Report" extra={<Button type="primary" onClick={() => window.open('/api/reports/export', '_blank')}>Export to Excel</Button>}>
              <Table dataSource={reportData} columns={reportCols} scroll={{ x: 'max-content' }} loading={loading} rowKey={(r, i) => i} />
            </Card>
          )}

          {activeTab === 'completion' && (
            <Card title="Quarterly Check-in Completion Status">
              <Table dataSource={completionData} columns={completionCols} rowKey="employeeId" loading={loading} />
            </Card>
          )}

          {activeTab === 'shared' && (
            <Card title="Push Shared Goal">
              <Alert message="Create a company-wide or departmental goal and push it to multiple employees at once. This goal will be locked for the employee (read-only title/target)." type="info" showIcon style={{ marginBottom: 20 }} />
              <Button type="primary" onClick={() => setSharedGoalModal(true)}>+ Create Shared Goal</Button>
            </Card>
          )}

          {activeTab === 'audit' && (
            <Card title="System Audit Trail (Append-Only)">
              <Table dataSource={auditLogs} columns={auditCols} rowKey="id" loading={loading} />
            </Card>
          )}

          {activeTab === 'config' && (
            <Space direction="vertical" style={{ width: '100%' }} size="large">
              {/* Thrust Areas */}
              <Card
                title="🏷️ Thrust Areas"
                extra={<Button type="primary" onClick={() => openTaModal()}>+ Add Thrust Area</Button>}
              >
                <Table
                  dataSource={configThrustAreas}
                  columns={thrustAreaCols}
                  rowKey="id"
                  loading={configLoading}
                  pagination={false}
                />
              </Card>

              {/* Cycles */}
              <Card
                title="📅 Performance Cycles"
                extra={<Button type="primary" onClick={() => openCycleModal()}>+ Add Cycle</Button>}
              >
                <Table
                  dataSource={cycles}
                  columns={cycleCols}
                  rowKey="id"
                  loading={configLoading}
                  pagination={false}
                />
              </Card>
            </Space>
          )}

          {activeTab === 'demo' && (
            <Card title="Judge Demo Mode">
              <div style={{ textAlign: 'center', padding: '40px 0' }}>
                <h3 style={{ marginBottom: 16 }}>Load Realistic Hackathon Demo Data</h3>
                <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>This will wipe the current database and seed it with 1 Admin, 1 Manager, 3 Employees, and mixed goal sheets across different statuses (Draft, Submitted, Approved) + Achievement data.</p>
                <Button type="primary" size="large" onClick={handleSeed} loading={seeding} style={{ background: 'var(--accent-purple)' }}>
                  🚀 Seed Demo Data Now
                </Button>
              </div>
            </Card>
          )}

        </Content>
      </Layout>

      {/* Shared Goal Modal */}
      <Modal title="Push Shared Goal to Employees" open={sharedGoalModal} onCancel={() => setSharedGoalModal(false)} footer={null} width={600}>
        <Form form={form} layout="vertical" onFinish={handlePushSharedGoal}>
          <Form.Item name="thrustAreaId" label="Thrust Area" rules={[{ required: true }]}>
            <Select>{thrustAreas.map(ta => <Select.Option key={ta.id} value={ta.id}>{ta.name}</Select.Option>)}</Select>
          </Form.Item>
          <Form.Item name="title" label="Goal Title" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={2} />
          </Form.Item>
          <Space size="large">
            <Form.Item name="uom" label="UoM" rules={[{ required: true }]} style={{ width: 150 }}>
              <Select options={['NUMERIC', 'PERCENTAGE', 'TIMELINE', 'ZERO_BASED'].map(o => ({ value: o, label: o }))} />
            </Form.Item>
            <Form.Item name="targetValue" label="Target Value">
              <InputNumber style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="minWeightage" label="Min Weightage %" rules={[{ required: true }]}>
              <InputNumber style={{ width: 150 }} min={10} max={100} />
            </Form.Item>
          </Space>
          <Form.Item name="recipientIds" label="Select Employees to Push To" rules={[{ required: true, message: 'Select at least one employee' }]}>
            <Select mode="multiple" placeholder="Select employees">
              {users.map(u => <Select.Option key={u.id} value={u.id}>{u.name} ({u.department})</Select.Option>)}
            </Select>
          </Form.Item>
          <Button type="primary" htmlType="submit" block>Push Goal to Employees</Button>
        </Form>
      </Modal>

      {/* Thrust Area Add/Edit Modal */}
      <Modal
        title={taModal.record ? 'Edit Thrust Area' : 'Add Thrust Area'}
        open={taModal.open}
        onCancel={() => { setTaModal({ open: false, record: null }); taForm.resetFields(); }}
        footer={null}
      >
        <Form form={taForm} layout="vertical" onFinish={handleSaveTa}>
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
            <Input placeholder="e.g. Customer Excellence" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={3} placeholder="Describe this strategic thrust area..." />
          </Form.Item>
          <Form.Item name="isActive" label="Active" valuePropName="checked">
            <Switch checkedChildren="Active" unCheckedChildren="Inactive" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            {taModal.record ? 'Update Thrust Area' : 'Create Thrust Area'}
          </Button>
        </Form>
      </Modal>

      {/* Cycle Add/Edit Modal */}
      <Modal
        title={cycleModal.record ? 'Edit Cycle' : 'Add Performance Cycle'}
        open={cycleModal.open}
        onCancel={() => { setCycleModal({ open: false, record: null }); cycleForm.resetFields(); }}
        footer={null}
      >
        <Form form={cycleForm} layout="vertical" onFinish={handleSaveCycle}>
          <Form.Item name="year" label="Year" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} placeholder="e.g. 2026" min={2020} max={2099} />
          </Form.Item>
          <Form.Item name="startDate" label="Start Date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="endDate" label="End Date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="isActive" label="Set as Active Cycle" valuePropName="checked">
            <Switch checkedChildren="Active" unCheckedChildren="Inactive" />
          </Form.Item>
          <Alert
            message="Only one cycle should be Active at a time. Setting this to Active will not automatically deactivate others — please deactivate the previous cycle manually."
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
          />
          <Button type="primary" htmlType="submit" block>
            {cycleModal.record ? 'Update Cycle' : 'Create Cycle'}
          </Button>
        </Form>
      </Modal>

    </Layout>
  );
}
