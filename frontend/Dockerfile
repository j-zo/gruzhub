FROM node:20

WORKDIR /frontend

COPY package*.json ./
RUN npm install
RUN npm install pm2 -g

COPY . .

RUN npm run build

CMD ["npm", "run", "start"]