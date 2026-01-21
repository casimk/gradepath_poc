import { useAuth } from '../auth/AuthContext';

export function DashboardPage() {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="dashboard-container">
      {/* Navigation */}
      <nav className="dashboard-nav">
        <div className="dashboard-nav-content">
          <div className="dashboard-logo">
            <svg viewBox="0 0 60 60" fill="none" xmlns="http://www.w3.org/2000/svg" className="dashboard-logo-icon">
              <path d="M30 5L55 20V40L30 55L5 40V20L30 5Z" fill="currentColor"/>
              <path d="M30 15L45 25V35L30 45L15 35V25L30 15Z" fill="#FAF8F5"/>
              <circle cx="30" cy="30" r="8" fill="#C4A962"/>
            </svg>
            <span className="dashboard-logo-text">GradePath</span>
          </div>

          <div className="dashboard-nav-links">
            <a href="#" className="dashboard-nav-link dashboard-nav-link-active">Dashboard</a>
            <a href="#" className="dashboard-nav-link">Courses</a>
            <a href="#" className="dashboard-nav-link">Progress</a>
            <a href="#" className="dashboard-nav-link">Analytics</a>
          </div>

          <div className="dashboard-user-menu">
            <div className="dashboard-user-info">
              <span className="dashboard-user-name">{user?.displayName || user?.username || user?.email}</span>
              <span className="dashboard-user-email">{user?.email}</span>
            </div>
            <div className="dashboard-user-avatar">
              {(user?.displayName || user?.username || user?.email)?.charAt(0).toUpperCase()}
            </div>
            <button onClick={handleLogout} className="dashboard-logout-button" aria-label="Logout">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </button>
          </div>
        </div>
      </nav>

      {/* Main content */}
      <main className="dashboard-main">
        {/* Welcome hero */}
        <section className="dashboard-hero">
          <div className="dashboard-hero-content">
            <h1 className="dashboard-hero-title">
              Welcome back, {user?.displayName || user?.username || 'Student'}!
            </h1>
            <p className="dashboard-hero-subtitle">
              Continue your learning journey. You're making great progress this week.
            </p>
          </div>
          <div className="dashboard-hero-streak">
            <div className="dashboard-streak-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div className="dashboard-streak-content">
              <span className="dashboard-streak-number">7</span>
              <span className="dashboard-streak-label">Day Streak!</span>
            </div>
          </div>
        </section>

        {/* Stats grid */}
        <section className="dashboard-stats">
          <div className="dashboard-stat-card dashboard-stat-primary">
            <div className="dashboard-stat-header">
              <div className="dashboard-stat-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <span className="dashboard-stat-label">Completed</span>
            </div>
            <div className="dashboard-stat-value">24</div>
            <div className="dashboard-stat-change dashboard-stat-change-positive">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
              +12% this week
            </div>
          </div>

          <div className="dashboard-stat-card dashboard-stat-secondary">
            <div className="dashboard-stat-header">
              <div className="dashboard-stat-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <span className="dashboard-stat-label">Study Hours</span>
            </div>
            <div className="dashboard-stat-value">18.5h</div>
            <div className="dashboard-stat-change dashboard-stat-change-positive">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
              +3.2h this week
            </div>
          </div>

          <div className="dashboard-stat-card dashboard-stat-tertiary">
            <div className="dashboard-stat-header">
              <div className="dashboard-stat-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </div>
              <span className="dashboard-stat-label">Accuracy</span>
            </div>
            <div className="dashboard-stat-value">87%</div>
            <div className="dashboard-stat-change dashboard-stat-change-neutral">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              Same as last week
            </div>
          </div>

          <div className="dashboard-stat-card dashboard-stat-quaternary">
            <div className="dashboard-stat-header">
              <div className="dashboard-stat-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                  <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                </svg>
              </div>
              <span className="dashboard-stat-label">Courses</span>
            </div>
            <div className="dashboard-stat-value">6</div>
            <div className="dashboard-stat-change dashboard-stat-change-positive">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
                <polyline points="17 6 23 6 23 12"/>
              </svg>
              2 new this month
            </div>
          </div>
        </section>

        {/* Two column layout */}
        <section className="dashboard-grid">
          {/* Active courses */}
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <h2 className="dashboard-card-title">Active Courses</h2>
              <a href="#" className="dashboard-card-link">View all</a>
            </div>
            <div className="dashboard-courses">
              <div className="dashboard-course">
                <div className="dashboard-course-icon dashboard-course-icon-blue">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                  </svg>
                </div>
                <div className="dashboard-course-info">
                  <h3 className="dashboard-course-title">Advanced Mathematics</h3>
                  <p className="dashboard-course-subtitle">Calculus II • Module 4</p>
                  <div className="dashboard-course-progress">
                    <div className="dashboard-progress-bar">
                      <div className="dashboard-progress-fill dashboard-progress-blue" style={{ width: '72%' }} />
                    </div>
                    <span className="dashboard-progress-text">72%</span>
                  </div>
                </div>
              </div>

              <div className="dashboard-course">
                <div className="dashboard-course-icon dashboard-course-icon-green">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="16 18 22 12 16 6"/>
                    <polyline points="8 6 2 12 8 18"/>
                  </svg>
                </div>
                <div className="dashboard-course-info">
                  <h3 className="dashboard-course-title">Computer Science</h3>
                  <p className="dashboard-course-subtitle">Data Structures • Module 2</p>
                  <div className="dashboard-course-progress">
                    <div className="dashboard-progress-bar">
                      <div className="dashboard-progress-fill dashboard-progress-green" style={{ width: '45%' }} />
                    </div>
                    <span className="dashboard-progress-text">45%</span>
                  </div>
                </div>
              </div>

              <div className="dashboard-course">
                <div className="dashboard-course-icon dashboard-course-icon-purple">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"/>
                </svg>
                </div>
                <div className="dashboard-course-info">
                  <h3 className="dashboard-course-title">Physics</h3>
                  <p className="dashboard-course-subtitle">Mechanics • Module 3</p>
                  <div className="dashboard-course-progress">
                    <div className="dashboard-progress-bar">
                      <div className="dashboard-progress-fill dashboard-progress-purple" style={{ width: '88%' }} />
                    </div>
                    <span className="dashboard-progress-text">88%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Activity feed */}
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <h2 className="dashboard-card-title">Recent Activity</h2>
              <a href="#" className="dashboard-card-link">View all</a>
            </div>
            <div className="dashboard-activity">
              <div className="dashboard-activity-item">
                <div className="dashboard-activity-icon dashboard-activity-complete">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
                <div className="dashboard-activity-content">
                  <p className="dashboard-activity-text">Completed <strong>Integration by Parts</strong> lesson</p>
                  <span className="dashboard-activity-time">2 hours ago</span>
                </div>
              </div>

              <div className="dashboard-activity-item">
                <div className="dashboard-activity-icon dashboard-activity-quiz">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <circle cx="12" cy="14" r="3"/>
                  </svg>
                </div>
                <div className="dashboard-activity-content">
                  <p className="dashboard-activity-text">Scored <strong>92%</strong> on Calculus quiz</p>
                  <span className="dashboard-activity-time">5 hours ago</span>
                </div>
              </div>

              <div className="dashboard-activity-item">
                <div className="dashboard-activity-icon dashboard-activity-start">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polygon points="5 3 19 12 5 21 5 3"/>
                  </svg>
                </div>
                <div className="dashboard-activity-content">
                  <p className="dashboard-activity-text">Started <strong>Binary Trees</strong> module</p>
                  <span className="dashboard-activity-time">Yesterday</span>
                </div>
              </div>

              <div className="dashboard-activity-item">
                <div className="dashboard-activity-icon dashboard-activity-achievement">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="8" r="7"/>
                    <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/>
                  </svg>
                </div>
                <div className="dashboard-activity-content">
                  <p className="dashboard-activity-text">Earned <strong>Quick Learner</strong> badge</p>
                  <span className="dashboard-activity-time">2 days ago</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Weekly progress chart placeholder */}
        <section className="dashboard-card">
          <div className="dashboard-card-header">
            <h2 className="dashboard-card-title">Weekly Progress</h2>
            <div className="dashboard-card-actions">
              <button className="dashboard-tab-button dashboard-tab-active">This Week</button>
              <button className="dashboard-tab-button">Last Week</button>
              <button className="dashboard-tab-button">This Month</button>
            </div>
          </div>
          <div className="dashboard-chart">
            <div className="dashboard-chart-bars">
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '60%' }} />
                <span className="dashboard-chart-label">Mon</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '80%' }} />
                <span className="dashboard-chart-label">Tue</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '45%' }} />
                <span className="dashboard-chart-label">Wed</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '90%' }} />
                <span className="dashboard-chart-label">Thu</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '70%' }} />
                <span className="dashboard-chart-label">Fri</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '55%' }} />
                <span className="dashboard-chart-label">Sat</span>
              </div>
              <div className="dashboard-chart-bar-wrapper">
                <div className="dashboard-chart-bar" style={{ height: '40%' }} />
                <span className="dashboard-chart-label">Sun</span>
              </div>
            </div>
          </div>
        </section>
      </main>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap');

        .dashboard-container {
          min-height: 100vh;
          background: #FAF8F5;
          font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
        }

        /* Navigation */
        .dashboard-nav {
          background: #FFFFFF;
          border-bottom: 1px solid #E8E4DD;
          position: sticky;
          top: 0;
          z-index: 100;
        }

        .dashboard-nav-content {
          max-width: 1400px;
          margin: 0 auto;
          padding: 0 2rem;
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 64px;
        }

        .dashboard-logo {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .dashboard-logo-icon {
          width: 36px;
          height: 36px;
          color: #1A365D;
        }

        .dashboard-logo-text {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 1.25rem;
          font-weight: 600;
          color: #1A365D;
        }

        .dashboard-nav-links {
          display: flex;
          gap: 0.5rem;
        }

        .dashboard-nav-link {
          padding: 0.5rem 1rem;
          font-size: 0.875rem;
          font-weight: 500;
          color: #5A6B7C;
          text-decoration: none;
          border-radius: 6px;
          transition: all 0.2s ease;
        }

        .dashboard-nav-link:hover {
          background: #F5F3F0;
          color: #1A365D;
        }

        .dashboard-nav-link-active {
          background: #1A365D;
          color: #FAF8F5;
        }

        .dashboard-user-menu {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .dashboard-user-info {
          text-align: right;
        }

        .dashboard-user-name {
          display: block;
          font-size: 0.875rem;
          font-weight: 600;
          color: #1A365D;
        }

        .dashboard-user-email {
          display: block;
          font-size: 0.75rem;
          color: #8B95A5;
        }

        .dashboard-user-avatar {
          width: 40px;
          height: 40px;
          background: linear-gradient(135deg, #1A365D 0%, #C4A962 100%);
          color: #FAF8F5;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 600;
          font-size: 0.875rem;
        }

        .dashboard-logout-button {
          width: 40px;
          height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: transparent;
          border: 1px solid #E8E4DD;
          border-radius: 8px;
          color: #5A6B7C;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .dashboard-logout-button:hover {
          border-color: #1A365D;
          color: #1A365D;
        }

        .dashboard-logout-button svg {
          width: 18px;
          height: 18px;
        }

        /* Main content */
        .dashboard-main {
          max-width: 1400px;
          margin: 0 auto;
          padding: 2rem;
        }

        /* Hero section */
        .dashboard-hero {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 2rem;
          padding: 2rem;
          background: linear-gradient(135deg, #1A365D 0%, #0F2744 100%);
          border-radius: 12px;
          position: relative;
          overflow: hidden;
        }

        .dashboard-hero::before {
          content: '';
          position: absolute;
          inset: 0;
          background: url("data:image/svg+xml,%3Csvg viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
          opacity: 0.03;
          pointer-events: none;
        }

        .dashboard-hero-content {
          position: relative;
          flex: 1;
        }

        .dashboard-hero-title {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 2rem;
          font-weight: 600;
          color: #FAF8F5;
          margin-bottom: 0.5rem;
        }

        .dashboard-hero-subtitle {
          font-size: 0.9375rem;
          color: rgba(250, 248, 245, 0.75);
          line-height: 1.6;
        }

        .dashboard-hero-streak {
          position: relative;
          display: flex;
          align-items: center;
          gap: 1rem;
          padding: 1.25rem 1.5rem;
          background: rgba(250, 248, 245, 0.1);
          border: 1px solid rgba(250, 248, 245, 0.2);
          border-radius: 12px;
          backdrop-filter: blur(10px);
        }

        .dashboard-streak-icon {
          width: 40px;
          height: 40px;
          background: #C4A962;
          color: #1A365D;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .dashboard-streak-icon svg {
          width: 20px;
          height: 20px;
        }

        .dashboard-streak-content {
          display: flex;
          flex-direction: column;
        }

        .dashboard-streak-number {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 1.5rem;
          font-weight: 600;
          color: #C4A962;
          line-height: 1;
        }

        .dashboard-streak-label {
          font-size: 0.75rem;
          color: rgba(250, 248, 245, 0.75);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }

        /* Stats grid */
        .dashboard-stats {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 1rem;
          margin-bottom: 2rem;
        }

        .dashboard-stat-card {
          background: #FFFFFF;
          border: 1px solid #E8E4DD;
          border-radius: 12px;
          padding: 1.5rem;
          transition: all 0.2s ease;
        }

        .dashboard-stat-card:hover {
          box-shadow: 0 4px 12px rgba(26, 54, 93, 0.1);
          transform: translateY(-2px);
        }

        .dashboard-stat-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 1rem;
        }

        .dashboard-stat-icon {
          width: 40px;
          height: 40px;
          background: #F5F3F0;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #1A365D;
        }

        .dashboard-stat-icon svg {
          width: 18px;
          height: 18px;
        }

        .dashboard-stat-label {
          font-size: 0.8125rem;
          font-weight: 500;
          color: #8B95A5;
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }

        .dashboard-stat-value {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 2rem;
          font-weight: 600;
          color: #1A365D;
          margin-bottom: 0.5rem;
        }

        .dashboard-stat-change {
          display: flex;
          align-items: center;
          gap: 0.25rem;
          font-size: 0.75rem;
          font-weight: 500;
        }

        .dashboard-stat-change svg {
          width: 14px;
          height: 14px;
        }

        .dashboard-stat-change-positive {
          color: #059669;
        }

        .dashboard-stat-change-neutral {
          color: #8B95A5;
        }

        .dashboard-stat-negative {
          color: #DC2626;
        }

        .dashboard-stat-primary .dashboard-stat-icon {
          background: #EFF6FF;
          color: #1A365D;
        }

        .dashboard-stat-secondary .dashboard-stat-icon {
          background: #F0F9FF;
          color: #0369A1;
        }

        .dashboard-stat-tertiary .dashboard-stat-icon {
          background: #FEF3C7;
          color: #92400E;
        }

        .dashboard-stat-quaternary .dashboard-stat-icon {
          background: #F5F3F0;
          color: #1A365D;
        }

        /* Grid layout */
        .dashboard-grid {
          display: grid;
          grid-template-columns: 1.5fr 1fr;
          gap: 1rem;
          margin-bottom: 2rem;
        }

        /* Cards */
        .dashboard-card {
          background: #FFFFFF;
          border: 1px solid #E8E4DD;
          border-radius: 12px;
          padding: 1.5rem;
        }

        .dashboard-card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 1.5rem;
        }

        .dashboard-card-title {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 1.25rem;
          font-weight: 600;
          color: #1A365D;
        }

        .dashboard-card-link {
          font-size: 0.8125rem;
          font-weight: 500;
          color: #1A365D;
          text-decoration: none;
          transition: color 0.2s ease;
        }

        .dashboard-card-link:hover {
          color: #C4A962;
        }

        .dashboard-card-actions {
          display: flex;
          gap: 0.25rem;
        }

        .dashboard-tab-button {
          padding: 0.375rem 0.75rem;
          font-size: 0.75rem;
          font-weight: 500;
          font-family: inherit;
          background: transparent;
          border: 1px solid #E8E4DD;
          border-radius: 6px;
          color: #5A6B7C;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .dashboard-tab-button:hover {
          border-color: #1A365D;
          color: #1A365D;
        }

        .dashboard-tab-active {
          background: #1A365D;
          border-color: #1A365D;
          color: #FAF8F5;
        }

        /* Courses */
        .dashboard-courses {
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }

        .dashboard-course {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding: 1rem;
          background: #FAF8F5;
          border-radius: 8px;
          transition: all 0.2s ease;
        }

        .dashboard-course:hover {
          background: #F5F3F0;
        }

        .dashboard-course-icon {
          width: 48px;
          height: 48px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .dashboard-course-icon svg {
          width: 20px;
          height: 20px;
        }

        .dashboard-course-icon-blue {
          background: #EFF6FF;
          color: #1A365D;
        }

        .dashboard-course-icon-green {
          background: #ECFDF5;
          color: #059669;
        }

        .dashboard-course-icon-purple {
          background: #F5F3FF;
          color: #7C3AED;
        }

        .dashboard-course-info {
          flex: 1;
        }

        .dashboard-course-title {
          font-size: 0.9375rem;
          font-weight: 600;
          color: #1A365D;
          margin-bottom: 0.25rem;
        }

        .dashboard-course-subtitle {
          font-size: 0.75rem;
          color: #8B95A5;
          margin-bottom: 0.75rem;
        }

        .dashboard-course-progress {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .dashboard-progress-bar {
          flex: 1;
          height: 6px;
          background: #E8E4DD;
          border-radius: 3px;
          overflow: hidden;
        }

        .dashboard-progress-fill {
          height: 100%;
          border-radius: 3px;
          transition: width 0.3s ease;
        }

        .dashboard-progress-blue {
          background: #1A365D;
        }

        .dashboard-progress-green {
          background: #059669;
        }

        .dashboard-progress-purple {
          background: #7C3AED;
        }

        .dashboard-progress-text {
          font-size: 0.75rem;
          font-weight: 600;
          color: #1A365D;
          min-width: 2.5rem;
          text-align: right;
        }

        /* Activity feed */
        .dashboard-activity {
          display: flex;
          flex-direction: column;
        }

        .dashboard-activity-item {
          display: flex;
          align-items: flex-start;
          gap: 0.75rem;
          padding: 0.75rem 0;
          border-bottom: 1px solid #F5F3F0;
        }

        .dashboard-activity-item:last-child {
          border-bottom: none;
        }

        .dashboard-activity-icon {
          width: 32px;
          height: 32px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .dashboard-activity-icon svg {
          width: 14px;
          height: 14px;
        }

        .dashboard-activity-complete {
          background: #ECFDF5;
          color: #059669;
        }

        .dashboard-activity-quiz {
          background: #EFF6FF;
          color: #1A365D;
        }

        .dashboard-activity-start {
          background: #FEF3C7;
          color: #92400E;
        }

        .dashboard-activity-achievement {
          background: #F5F3FF;
          color: #7C3AED;
        }

        .dashboard-activity-content {
          flex: 1;
        }

        .dashboard-activity-text {
          font-size: 0.875rem;
          color: #1A365D;
          margin-bottom: 0.25rem;
        }

        .dashboard-activity-text strong {
          font-weight: 600;
        }

        .dashboard-activity-time {
          font-size: 0.75rem;
          color: #8B95A5;
        }

        /* Chart */
        .dashboard-chart {
          padding: 1rem 0;
        }

        .dashboard-chart-bars {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          height: 200px;
          gap: 1rem;
        }

        .dashboard-chart-bar-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 0.5rem;
          height: 100%;
        }

        .dashboard-chart-bar {
          width: 100%;
          max-width: 40px;
          background: linear-gradient(180deg, #1A365D 0%, #0F2744 100%);
          border-radius: 6px 6px 0 0;
          transition: all 0.2s ease;
        }

        .dashboard-chart-bar:hover {
          background: linear-gradient(180deg, #C4A962 0%, #1A365D 100%);
          transform: scaleY(1.05);
          transform-origin: bottom;
        }

        .dashboard-chart-label {
          font-size: 0.75rem;
          font-weight: 500;
          color: #8B95A5;
        }

        /* Responsive */
        @media (max-width: 1200px) {
          .dashboard-stats {
            grid-template-columns: repeat(2, 1fr);
          }

          .dashboard-grid {
            grid-template-columns: 1fr;
          }
        }

        @media (max-width: 768px) {
          .dashboard-nav-content {
            padding: 0 1rem;
          }

          .dashboard-nav-links {
            display: none;
          }

          .dashboard-main {
            padding: 1rem;
          }

          .dashboard-hero {
            flex-direction: column;
            text-align: center;
            gap: 1rem;
          }

          .dashboard-stats {
            grid-template-columns: 1fr;
          }

          .dashboard-chart-bars {
            height: 150px;
          }

          .dashboard-user-info {
            display: none;
          }
        }
      `}</style>
    </div>
  );
}
