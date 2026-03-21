import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,

  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**.railway.app" },
      { protocol: "http", hostname: "localhost" },
    ],
  },
};

export default nextConfig;
