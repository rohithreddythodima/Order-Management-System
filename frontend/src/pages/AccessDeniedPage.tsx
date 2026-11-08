import { Link } from 'react-router-dom';

export default function AccessDeniedPage() {
  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1>Access denied</h1>
        <p className="muted">You do not have sufficient permissions for this section.</p>
        <Link to="/login" className="primary-button inline-link">Back to login</Link>
      </div>
    </div>
  );
}
