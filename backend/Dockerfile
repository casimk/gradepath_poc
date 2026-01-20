# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Production stage
FROM node:18-alpine AS production

WORKDIR /app

COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production

COPY --from=builder /app/dist ./dist

ENV NODE_ENV production

CMD ["node", "dist/main"]
