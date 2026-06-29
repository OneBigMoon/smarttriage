FROM node:6.17.1-slim AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install --no-audit --no-fund --no-package-lock

COPY . .

RUN npm run build


FROM node:6.17.1-slim

WORKDIR /app
COPY --from=builder /app/dist/smarttriage .

ENV NODE_ENV=development \
    PORT=7016

EXPOSE 7016

CMD ["node", "server.js"]
