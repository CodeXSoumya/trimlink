import axios, { InternalAxiosRequestConfig } from "axios";

const STORAGE_KEY = "trimlink-client-id";

function getClientId() {
  const existing = localStorage.getItem(STORAGE_KEY);
  if (existing) {
    return existing;
  }

  const generated = `web_${crypto.randomUUID()}`;
  localStorage.setItem(STORAGE_KEY, generated);
  return generated;
}

const runtimeMeta = import.meta as unknown as {
  env?: {
    VITE_API_BASE_URL?: string;
  };
};

export const apiBaseUrl = runtimeMeta.env?.VITE_API_BASE_URL ?? "/api/v1";

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  config.headers.set("X-Client-ID", getClientId());
  return config;
});
