# NovaHost

NovaHost is an MVP static website hosting API. It accepts ZIP files containing HTML, CSS, JavaScript, and assets, then serves the published site at `/s/<site-name>/`.

## Deploy to Render

1. Open Render and create a **Web Service** from this repository.
2. Choose Node, the Singapore region, and the Free plan for testing.
3. Build command: `npm install`
4. Start command: `npm start`
5. Add environment variable `ADMIN_API_KEY` with a long, random secret. Never commit the key to GitHub.
6. Deploy and test `https://YOUR-SERVICE.onrender.com/health`.

The included `render.yaml` can also be used as a Blueprint.

## API

All management endpoints require `Authorization: Bearer YOUR_ADMIN_API_KEY`.

- `GET /health` — public health check.
- `GET /api/sites` — list published sites.
- `POST /api/sites` — multipart upload; fields: `name` and `file` (ZIP).
- `DELETE /api/sites/:slug` — delete a published site.
- Public website: `GET /s/:slug/`.

A ZIP must have `index.html` at its root. Upload limit is 20 MB; extracted limit is 100 MB and 500 entries.

Example:
```bash
curl -H "Authorization: Bearer $ADMIN_API_KEY" \
  -F "name=my-first-site" -F "file=@website.zip" \
  https://YOUR-SERVICE.onrender.com/api/sites
```

## Important storage limitation

This starter uses the service filesystem. Render Free services have ephemeral filesystems: uploaded websites can be lost when the service restarts or redeploys. Do not use this as a public production hosting provider until persistent storage or external object storage (such as S3-compatible storage) has been configured. A persistent Render disk requires a paid service plan. This MVP only hosts static HTML/CSS/JS; it does not run PHP, databases, or arbitrary server code.

## Security

This starter is intended for private testing by one owner. Do not embed the admin API key in a publicly distributed Android APK: keys inside APKs can be extracted. A public multi-user service needs accounts, per-user authentication, quotas, abuse controls, malware scanning, durable object storage, backups, and operational monitoring.

## Android app (AIDE Pro)

The Android project source is in `android-aide/`. In AIDE Pro, use Open Project and select the `android-aide` folder after cloning or downloading this repository. Build and run the app on your Android device. It includes a pixel-style dark dashboard, server URL/API key settings, ZIP picker, publish, refresh, open-site, and delete-site actions.

In the app, keep the API URL as `https://novahost-api.onrender.com` and enter the private `ADMIN_API_KEY` configured in Render. Do not share the key or publish an APK containing it; this first app is for owner-only testing. Android stores the entered key in app preferences, which is not a secure credential vault.
