import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { APP_VERSION } from './utils/appInfo'
import { installStaleChunkReload } from './utils/staleChunks'

console.info(`KMA Library frontend started (version: ${APP_VERSION})`)
installStaleChunkReload()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
