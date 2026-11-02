import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({plugins:[react()],server:{proxy:{'/search':'http://127.0.0.1:8090','/diagnostics':'http://127.0.0.1:8090','/ingest':'http://127.0.0.1:8090','/health':'http://127.0.0.1:8090'}}});
