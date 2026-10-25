# Build stage
FROM node:20-alpine AS builder

# Accept build arguments
ARG VERSION=dev
ARG RELEASE_TAG=
ARG BUILD_DATE

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application with version info
ENV VITE_APP_VERSION=${VERSION}
ENV VITE_RELEASE_TAG=${RELEASE_TAG}
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY proxy-params.conf /etc/nginx/proxy-params.conf

# Expose port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
