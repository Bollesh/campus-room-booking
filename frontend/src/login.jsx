import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from './components/header';
import './styles/loginstyles.css';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loginError, setLoginError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem('jwt', data.jwt);
        localStorage.setItem('role', data.role);
        localStorage.setItem('email', email); // Store email in localStorage
    
        const storedEmail = localStorage.getItem('email');


        // Clear error on successful login
        setLoginError('');

        switch (data.role) {
          case 'ROLE_STUDENT':
            navigate('/student-dashboard');
            break;
          case 'ROLE_PROFESSOR':
            navigate('/professor-dashboard');
            break;
          case 'ROLE_FLOOR_MANAGER':
            navigate('/floor-manager-dashboard');
            break;
          case 'ROLE_SECURITY':
            navigate('/security-dashboard');
            break;
          case 'ROLE_STUDENT_COUNCIL':
            navigate('/student-council-dashboard');
            break;
          default:
            setLoginError('Unknown role: ' + data.role);
        }
      } else {
        // Show backend error or default error
        setLoginError(data.message || 'Invalid email or password');
      }
    } catch (error) {
      setLoginError('Server error. Please try again later.');
      console.error('Error during login:', error);
    }
  };

  return (
    <>
      <Header />
      <div className='login'>
        <form onSubmit={handleLogin}>
          <h2>Login</h2>

          {loginError && (
            <div className="error-message">{loginError}</div>
          )}

          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit">Login</button>
        </form>
      </div>
    </>
  );
}

export default Login;
