# ---------------------------------------------------------------------------
# Stage 1 - build the static bundle.
#
# package*.json is copied first so `npm ci` is cached independently of source
# changes. `npm ci` (not `npm install`) installs exactly what the lockfile
# pins, which is what makes the build reproducible.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite inlines VITE_* variables at build time, so the API base URL has to be
# known here rather than at container start. It defaults to empty, meaning the
# SPA calls same-origin paths like /api/projects and nginx proxies them to the
# backend - no CORS involved. Override with --build-arg to point a build at an
# externally hosted API instead.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ---------------------------------------------------------------------------
# Stage 2 - serve the bundle with nginx.
#
# Only the built dist/ is copied across; node_modules and the toolchain stay
# behind in the build stage.
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# The nginx image runs envsubst over /etc/nginx/templates/*.template at startup.
# The filter restricts substitution to BACKEND_ORIGIN so that nginx's own
# runtime variables ($host, $uri, ...) are left untouched.
ENV BACKEND_ORIGIN="http://backend:8080"
ENV NGINX_ENVSUBST_FILTER="BACKEND_ORIGIN"

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
