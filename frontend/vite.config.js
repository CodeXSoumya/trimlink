import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react-swc";
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, ".", "");
    var devApiTarget = env.VITE_DEV_API_TARGET || "http://localhost:8080";
    return {
        plugins: [react()],
        server: {
            port: 5173,
            host: "0.0.0.0",
            proxy: {
                "/api": {
                    target: devApiTarget,
                    changeOrigin: true
                }
            }
        },
        preview: {
            port: 4173,
            host: "0.0.0.0",
            proxy: {
                "/api": {
                    target: devApiTarget,
                    changeOrigin: true
                }
            }
        }
    };
});
