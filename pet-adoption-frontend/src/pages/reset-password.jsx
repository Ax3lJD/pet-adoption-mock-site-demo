import React, { useState } from "react";
import Head from "next/head";
import { useRouter } from "next/router";
import Link from "next/link";
import {
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
  Box,
  Alert,
  CircularProgress,
} from "@mui/material";

export default function ResetPassword() {
  const router = useRouter();
  const { token } = router.query;
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");
    setSuccess("");

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      setIsLoading(false);
      return;
    }

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/reset-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          token,
          newPassword: password 
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Failed to reset password');
      }

      setSuccess('Password has been reset successfully. You can now login with your new password.');
      setTimeout(() => {
        router.push('/login');
      }, 3000);
      
    } catch (err) {
      console.error('Error:', err);
      setError(err.message || 'An unexpected error occurred');
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <Box sx={{ padding: 4, textAlign: 'center' }}>
        <Alert severity="error">Invalid or expired reset link</Alert>
        <Box mt={2}>
          <Link href="/forgot-password" passHref>
            <Button variant="contained" color="primary">
              Request New Reset Link
            </Button>
          </Link>
        </Box>
      </Box>
    );
  }

  return (
    <>
      <Head>
        <title>Reset Password - Pet Adoption</title>
      </Head>

      <main>
        <Stack sx={{ paddingTop: 4, paddingX: 2 }} alignItems="center" gap={2}>
          <Card sx={{ width: { xs: '100%', sm: 600 } }} elevation={4}>
            <CardContent>
              <Typography variant="h4" align="center" gutterBottom>
                Set New Password
              </Typography>
              
              {error && (
                <Alert severity="error" sx={{ mb: 3 }}>
                  {error}
                </Alert>
              )}
              
              {success && (
                <Alert severity="success" sx={{ mb: 3 }}>
                  {success}
                </Alert>
              )}
              
              <form onSubmit={handleSubmit}>
                <Stack spacing={3}>
                  <TextField
                    label="New Password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    fullWidth
                    required
                    disabled={isLoading}
                  />
                  
                  <TextField
                    label="Confirm New Password"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    fullWidth
                    required
                    disabled={isLoading}
                  />
                  
                  <Button 
                    type="submit"
                    variant="contained" 
                    color="primary"
                    size="large"
                    fullWidth
                    disabled={isLoading}
                    startIcon={isLoading ? <CircularProgress size={20} /> : null}
                  >
                    {isLoading ? 'Resetting...' : 'Reset Password'}
                  </Button>
                </Stack>
              </form>
            </CardContent>
          </Card>
        </Stack>
      </main>
    </>
  );
} 