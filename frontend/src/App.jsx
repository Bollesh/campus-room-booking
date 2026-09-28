import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './home';
import Register from './register';
import Login from './login';
import StudentDashboard from './dashboards/studentDash';
import ProfessorDashboard from './dashboards/profDash';
import FloorManagerDashboard from './dashboards/floorMgrDash';
import SecurityDashboard from './dashboards/securityDash';
import StudentCouncilDashboard from './dashboards/scDash';
import StudentView from './dashboards/studentView';
import StudentBook from './dashboards/studentBook';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/student-dashboard" element={<StudentDashboard />} />
        <Route path="/professor-dashboard" element={<ProfessorDashboard />} />
        <Route path="/floor-manager-dashboard" element={<FloorManagerDashboard />} />
        <Route path="/security-dashboard" element={<SecurityDashboard />} />
        <Route path="/student-council-dashboard" element={<StudentCouncilDashboard />} />
        <Route path="/student-view" element={<StudentView />} />
        <Route path="/student-book" element={<StudentBook />} />
      </Routes>
    </Router>
  );
}

export default App;