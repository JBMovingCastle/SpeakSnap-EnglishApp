import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        "speaksnap": {
          bg: "#0F0F1A",
          surface: "#16162A",
          card: "#1E1E38",
          border: "#252540",
          "border-light": "#313160",
          primary: "#4F46E5",
          accent: "#7C3AED",
          secondary: "#C4B5FD",
          "secondary-md": "#A78BFA",
          "secondary-sm": "#818CF8",
          success: "#22C55E",
          warning: "#F59E0B",
          error: "#EF4444",
          text: "#E2E8F0",
          muted: "#888888",
          dim: "#666666",
        },
      },
    },
  },
  plugins: [],
};
export default config;
