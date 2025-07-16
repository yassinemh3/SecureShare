import React, { useState, ChangeEvent, FormEvent } from "react";
import axiosInstance from "../api/axios";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Toaster } from "@/components/ui/sonner"
import { toast } from 'sonner';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface AuthResponse {
  access_token: string;
  refresh_token?: string;
}

interface RegisterForm {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
}

interface AuthProps {
  onAuthSuccess: (token: string) => void;
}

const Register: React.FC<AuthProps> = ({ onAuthSuccess }) => {
  const [form, setForm] = useState<RegisterForm>({
    firstname: "",
    lastname: "",
    email: "",
    password: "",
  });

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const res = await axiosInstance.post<AuthResponse>("/auth/register", form);
      onAuthSuccess(res.data.access_token);
    } catch (err) {
      console.error(err);
      toast("Registration failed");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100 px-4">
    <Toaster position="top-center" richColors />
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center text-2xl">Create an Account</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleRegister} className="space-y-4">
            <div>
              <Label htmlFor="firstname">First Name</Label>
              <Input
                type="text"
                name="firstname"
                id="firstname"
                placeholder="John"
                value={form.firstname}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <Label htmlFor="lastname">Last Name</Label>
              <Input
                type="text"
                name="lastname"
                id="lastname"
                placeholder="Doe"
                value={form.lastname}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <Label htmlFor="email">Email</Label>
              <Input
                type="email"
                name="email"
                id="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <Label htmlFor="password">Password</Label>
              <Input
                type="password"
                name="password"
                id="password"
                placeholder="••••••••"
                value={form.password}
                onChange={handleChange}
                required
              />
            </div>

            <Button type="submit" className="w-full">
              Register
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default Register;
