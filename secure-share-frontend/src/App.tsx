import { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from "./auth/Login";
import Register from "./auth/Register";
import Home from "./pages/Home";
import ShareAccess from "./pages/ShareAccess"

const App = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(
    !!localStorage.getItem('token')
  );

  const handleAuthSuccess = (token: string) => {
    localStorage.setItem('token', token);
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
  };

  return (
    <Router>
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? (
              <Navigate to="/" />
            ) : (
              <Login onAuthSuccess={handleAuthSuccess} />
            )
          }
        />
        <Route
          path="/register"
          element={
            isAuthenticated ? (
              <Navigate to="/" />
            ) : (
              <Register onAuthSuccess={handleAuthSuccess} />
            )
          }
        />
        <Route
          path="/"
          element={
            isAuthenticated ? (
              <Home onLogout={handleLogout} token={localStorage.getItem('token')!} />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
        {/* Add this new route for share access */}
        <Route
          path="/share/access/:token"
          element={<ShareAccess />}
        />
      </Routes>
    </Router>
  );
};

export default App;