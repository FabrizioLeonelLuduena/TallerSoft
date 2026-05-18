# Multi-stage build for Angular frontend with nginx
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build Angular application for production
RUN npm run build -- --configuration production

# Production stage with nginx
FROM nginx:alpine

# Remove default nginx config
RUN rm -rf /etc/nginx/conf.d/*

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built application from builder
COPY --from=builder /app/dist/frontend /usr/share/nginx/html

# Create non-root user
RUN addgroup -g 1001 tallersoft && \
    adduser -D -u 1001 -G tallersoft tallersoft && \
    chown -R tallersoft:tallersoft /usr/share/nginx/html

USER tallersoft

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
