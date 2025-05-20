# Spillway

*Spillway* is a video upload and streaming application that you can host yourself,
    for whatever media collections you always want access to. This repository provides
    a containerized Spring Boot backend and a Vue frontend client. 

Run backend using `docker-compose`:
```bash
cd spillway
docker-compose up --build
```

Run frontend client locally:
```bash
cd spillway-frontend
npm run dev
```

If/when deploying for production, backend server settings can be changed 
    for DB and video endpoints in `spillway/.../application.properties`.
    Frontend .env variables currently live in `spillway-frontend/.env.example`.
