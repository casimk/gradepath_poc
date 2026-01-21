# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY web-app/package*.json ./

# Copy the local @gradepath/telemetry package
COPY packages/@gradepath/telemetry ./packages/@gradepath/telemetry

# Install dependencies
RUN npm ci

# Copy source code
COPY web-app/ ./

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY web-app/nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
