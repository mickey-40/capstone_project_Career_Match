# CareerMatch AI – Firebase Hosting Frontend

This folder contains a simple static site for Task 4 to distribute the Android APK
and link to your Render backend.

## What to do

1. **Install Firebase CLI**
   ```bash
   npm i -g firebase-tools
   firebase login
   ```

2. **Initialize (first time only)**
   ```bash
   # from this folder (frontend_firebase_site)
   firebase init hosting
   # Choose: Use an existing project (or create new)
   # Public directory: public
   # Configure as single-page app? n
   # Set up automatic builds and deploys with GitHub? (optional) y/n
   ```

3. **Add your APK**
   - Place your signed APK inside `public/`, e.g. `public/careermatch-ai-1.0.0.apk`.
   - Update the link in `public/index.html` if your filename differs.

4. **Set your backend URL**
   - In `public/index.html`, replace `https://YOUR-RENDER-SERVICE.onrender.com` with your real Render URL.

5. **Deploy**
   ```bash
   firebase deploy
   ```

6. **Submit the live URL** (e.g., `https://<project-id>.web.app`) in Task 4.

## Optional: GitHub Actions CI/CD
If you selected the GitHub integration during init, you can use the provided workflow
(`.github/workflows/firebase-hosting.yml`) to auto-deploy on push to `main`.
