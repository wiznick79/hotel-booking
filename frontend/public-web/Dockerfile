FROM node:22-alpine AS build

WORKDIR /app

COPY frontend/public-web/package.json frontend/public-web/package-lock.json ./

RUN npm ci

COPY frontend/public-web/ ./

RUN npm run build

FROM nginx:1.31-alpine

COPY frontend/public-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
