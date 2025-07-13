import React, { useState, ChangeEvent, FormEvent } from 'react';
import axiosInstance from '../api/axios';
import { Link } from 'react-router-dom';
interface AuthProps {
  onAuthSuccess: (token: string) => void;
}

interface AuthResponse {
  access_token: string;
  refresh_token?: string;
}

interface LoginForm {
  email: string;
  password: string;
}

const Login: React.FC<AuthProps> = ({ onAuthSuccess }) => {
  const [form, setForm] = useState<LoginForm>({
    email: '',
    password: '',
  });

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

    const handleLogin = async (e: FormEvent) => {
      e.preventDefault();
      try {
        const res = await axiosInstance.post<AuthResponse>(
          '/auth/authenticate',
          form
        );
        console.log('Received token:', res.data.access_token); // Debug log
        onAuthSuccess(res.data.access_token);
      } catch (err) {
        console.error(err);
        alert('Login failed');
      }
    };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <form
        onSubmit={handleLogin}
        className="bg-white shadow-lg rounded-lg p-8 w-full max-w-md"
      >
        <h2 className="text-2xl font-bold mb-6 text-center text-gray-800">
          Login to SecureFile
        </h2>
        <div className="mb-4">
          <label
            htmlFor="email"
            className="block text-gray-700 font-semibold mb-2"
          >
            Email
          </label>
          <input
            type="email"
            name="email"
            id="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={handleChange}
            required
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="mb-6">
          <label
            htmlFor="password"
            className="block text-gray-700 font-semibold mb-2"
          >
            Password
          </label>
          <input
            type="password"
            name="password"
            id="password"
            placeholder="••••••••"
            value={form.password}
            onChange={handleChange}
            required
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <button
          type="submit"
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition-colors font-semibold"
        >
          Login
        </button>
        <div className="mt-4 text-center">
          <span className="text-gray-600">Don't have an account?</span>{' '}
          <Link to="/register" className="text-blue-600 hover:underline font-semibold">
            Register
          </Link>
        </div>
      </form>
    </div>
  );
};

export default Login;