import axios from "axios";

const api = axios.create({
  baseURL: "/api",
});

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem("kb_token");
  if (token) {
    cfg.headers.Authorization = `Bearer ${token}`;
  }
  return cfg;
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && !window.location.pathname.includes("/login")) {
      localStorage.removeItem("kb_token");
      localStorage.removeItem("kb_user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
