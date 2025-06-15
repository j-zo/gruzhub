import type { Metadata } from "next";
import "./globals.css";
import "./mantine.css";
import { MantineProvider, ColorSchemeScript } from "@mantine/core";

export const metadata: Metadata = {
  title: "ГрузХаб: личный кабинет",
  description:
    "ГрузХаб: поиск грузовых автосервисов с минимальным простоем машины",
  icons: {
    icon: "/favicon.ico",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru" translate="no">
      <head>
        <link rel="icon" href="/favicon.ico" />
        <ColorSchemeScript />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap"
          rel="stylesheet"
        ></link>
        <meta name="robots" content="noindex"></meta>
      </head>

      <body>
        <MantineProvider
          theme={{
            primaryColor: "blue",
            colors: {
              blue: [
                "#dbeafe",
                "#bfdbfe",
                "#93c5fd",
                "#60a5fa",
                "#3b82f6",
                "#2563eb",
                "#1d4ed8",
                "#1e40af",
                "#1e3a8a",
                "#172554",
              ],
            },
          }}
        >
          {children}
        </MantineProvider>
      </body>
    </html>
  );
}
