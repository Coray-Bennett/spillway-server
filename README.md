# Spillway

*Spillway* is a video upload and streaming application that you can host yourself,
    for whatever media collections you always want access to. Videos are uploaded to the service
    and converted to HLS files and playlists, and 

Run backend using `docker-compose`:
```bash
cd spillway
mvn clean package
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