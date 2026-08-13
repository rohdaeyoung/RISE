import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
//
// vite-plugin-singlefile은 웹 배포에서 뺐다. 이미지까지 index.html 하나에 인라인하면
// 첫 화면이 14MB가 되어, 브라우저가 그걸 다 받을 때까지 흰 화면이 유지된다.
// 나눠서 내보내면 처음엔 100KB 정도만 받고 캐릭터·의상 이미지는 필요할 때 받는다.
//
// 서버 없이 파일 하나로 데모해야 할 일이 생기면(발표용 USB 등) 아래 두 줄을 되살리면 된다.
// 패키지는 지우지 않았다.
//   import { viteSingleFile } from 'vite-plugin-singlefile'
//   plugins: [react(), tailwindcss(), viteSingleFile()],
export default defineConfig({
  plugins: [react(), tailwindcss()],
})
