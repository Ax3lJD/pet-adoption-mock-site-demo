import React, { useState } from "react";
import Head from "next/head";
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

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");
    setSuccess("");

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/forgot-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Failed to process request');
      }

      setSuccess('If an account exists with this email, you will receive password reset instructions.');
      
    } catch (err) {
      console.error('Error:', err);
      setError(err.message || 'An unexpected error occurred');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Head>
        <title>Forgot Password - Pet Adoption</title>
      </Head>

      <main>
        <Stack sx={{ paddingTop: 4, paddingX: 2 }} alignItems="center" gap={2}>
          <Card sx={{ width: { xs: '100%', sm: 600 } }} elevation={4}>
            <CardContent>
              <Typography variant="h4" align="center" gutterBottom>
                Reset Your Password
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
                    label="Email Address"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
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
                    {isLoading ? 'Sending...' : 'Send Reset Link'}
                  </Button>
                </Stack>
              </form>
              
              <Box textAlign="center" mt={3}>
                <Link href="/login" passHref>
                  <Button variant="text" color="primary">
                    Back to Login
                  </Button>
                </Link>
              </Box>
            </CardContent>
          </Card>
        </Stack>
      </main>
    </>
  );
} 