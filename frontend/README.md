# TaskFlow Frontend

React + TypeScript + Vite single-page app for the TaskFlow task/project tracker. See the [repository root README](../README.md) for the full project overview, architecture, and setup instructions covering both the frontend and backend.

## Quick start

```bash
npm install
npm run dev      # starts the dev server on http://localhost:5173
npm run build    # type-checks with tsc and produces a production build in dist/
```

Configure the backend API location via `VITE_API_BASE_URL` (see `.env.example`). It defaults to `http://localhost:8080` if unset.
