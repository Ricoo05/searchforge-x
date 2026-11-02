import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
// Test-only backend origin: cloned data, never the active user cluster at 8090.
export default defineConfig(({mode})=>({plugins:[react(),...(mode==='latency'?[{
  name:'test-network-latency',configureServer(server){
    server.middlewares.use((request,_response,next)=>{
      if(['/search','/ingest','/diagnostics'].includes(request.url??''))setTimeout(next,1500);else next();
    });
  },
}]:[])],server:{host:'127.0.0.1',port:5274,strictPort:true,proxy:{'/search':'http://127.0.0.1:8290','/diagnostics':'http://127.0.0.1:8290','/ingest':'http://127.0.0.1:8290','/health':'http://127.0.0.1:8290'}}}));
