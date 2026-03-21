# Vercel + Railway Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the pet adoption demo as a fully functional live site — frontend on Vercel (free), backend + MySQL on Railway ($5/mo).

**Architecture:** Next.js frontend on Vercel talks to Spring Boot API on Railway via `NEXT_PUBLIC_BACKEND_URL`. Railway's MySQL plugin provides the database. All config changes use env vars with local-dev fallbacks so nothing breaks locally.

**Tech Stack:** Next.js 15, Spring Boot 3.4, Java 22, MySQL 8.4, Vercel, Railway

**Spec:** `docs/superpowers/specs/2026-03-20-railway-vercel-deployment-design.md`

---

## File Map

### Backend (Modified)
| File | Responsibility |
|------|---------------|
| `pet-adoption-api/src/main/resources/application.yml` | Server port, DB connection, upload path |
| `pet-adoption-api/src/main/java/petadoption/api/config/CorsConfig.java` | Global CORS + `CorsConfigurationSource` bean |
| `pet-adoption-api/src/main/java/petadoption/api/config/SecurityConfig.java` | Add `/ping` and `/api/pets/import-csv` to permitAll |
| `pet-adoption-api/src/main/java/petadoption/api/endpoint/PingEndpoint.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/endpoint/AdoptionRequestEndpoint.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/endpoint/RecommendationEndpoint.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/endpoint/PetEndpoint.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/notifications/NotificationsController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/controller/DebugController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/controller/EventController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/controller/ShelterEventController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/controller/AdopterEventController.java` | Remove `@CrossOrigin` |
| `pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetService.java` | Fix hardcoded frontend URL in reset email link |

### Frontend (Modified)
| File | Responsibility |
|------|---------------|
| `pet-adoption-frontend/next.config.ts` | Switch `domains` to `remotePatterns` |
| `pet-adoption-frontend/src/pages/signup.jsx` | Fix hardcoded backend URL |
| `pet-adoption-frontend/src/pages/forgot-password.jsx` | Fix hardcoded backend URL |
| `pet-adoption-frontend/src/pages/reset-password.jsx` | Fix hardcoded backend URL |

---

## Task 1: Update `application.yml` — port, DB, and upload path

**Files:**
- Modify: `pet-adoption-api/src/main/resources/application.yml`

- [ ] **Step 1: Update server port to read from Railway's PORT env var**

In `application.yml`, change:
```yaml
server:
  port: 8080
```
to:
```yaml
server:
  port: ${PORT:8080}
```

- [ ] **Step 2: Update datasource to use Railway's MySQL env vars**

Change:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3307/petadoption
    username: root
    password: password
    driverClassName: com.mysql.cj.jdbc.Driver
```
to:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQLHOST:localhost}:${MYSQLPORT:3307}/${MYSQLDATABASE:petadoption}
    username: ${MYSQLUSER:root}
    password: ${MYSQLPASSWORD:password}
    driverClassName: com.mysql.cj.jdbc.Driver
```

Note: `driverClassName` is preserved. The env var name changes from `MYSQL_HOST` to `MYSQLHOST` (no underscore) to match Railway's MySQL plugin env vars.

- [ ] **Step 3: Update upload base path to relative**

Change:
```yaml
pet:
  upload:
    base-path: D:/petAdaptWebsite/s25-pet-adoption-team-2/pet-adoption-api/uploads
```
to:
```yaml
pet:
  upload:
    base-path: ${UPLOAD_PATH:./pet-uploads}
```

- [ ] **Step 4: Commit**

```bash
git add pet-adoption-api/src/main/resources/application.yml
git commit -m "feat: make application.yml deployment-ready with env var placeholders"
```

---

## Task 2: Remove all `@CrossOrigin` annotations from controllers

**Files:**
- Modify: 10 Java controller/endpoint files (see file map above)

For each file, delete the entire `@CrossOrigin(...)` line. If a standalone `import org.springframework.web.bind.annotation.CrossOrigin;` line exists, remove it too. If the import is covered by a wildcard import (`.*`), no import removal is needed. Only `PingEndpoint.java` and `RecommendationEndpoint.java` have standalone CrossOrigin imports.

- [ ] **Step 1: Remove `@CrossOrigin` from `PingEndpoint.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/endpoint/PingEndpoint.java`
Delete line 10: `@CrossOrigin(origins = "http://35.225.196.242:3001")`
Delete the CrossOrigin import.

- [ ] **Step 2: Remove `@CrossOrigin` from `AdoptionRequestEndpoint.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/endpoint/AdoptionRequestEndpoint.java`
Delete line 18: `@CrossOrigin(origins = "http://35.225.196.242:3001")`
Delete the CrossOrigin import.

- [ ] **Step 3: Remove `@CrossOrigin` from `RecommendationEndpoint.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/endpoint/RecommendationEndpoint.java`
Delete line 15: `@CrossOrigin(origins = "http://35.225.196.242:3001")`
Delete the CrossOrigin import.

- [ ] **Step 4: Remove `@CrossOrigin` from `PetEndpoint.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/endpoint/PetEndpoint.java`
Delete line 20: `@CrossOrigin(origins = "${cors.allowed.origins:http://35.225.196.242:3000}")`
Delete the CrossOrigin import.

- [ ] **Step 5: Remove `@CrossOrigin` from `PasswordResetController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetController.java`
Delete line 12: `@CrossOrigin(origins = "http://35.225.196.242:3001")`
Delete the CrossOrigin import.

- [ ] **Step 6: Remove `@CrossOrigin` from `NotificationsController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/notifications/NotificationsController.java`
Delete line 31: `@CrossOrigin(origins = "${cors.allowed.origins:http://35.225.196.242:3000}")`
Delete the CrossOrigin import.

- [ ] **Step 7: Remove `@CrossOrigin` from `DebugController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/controller/DebugController.java`
Delete line 10: `@CrossOrigin(origins = {"http://35.225.196.242:3000", "http://localhost:3000"})`
Delete the CrossOrigin import.

- [ ] **Step 8: Remove `@CrossOrigin` from `EventController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/controller/EventController.java`
Delete line 14: `@CrossOrigin(origins = "http://35.225.196.242:3000")`
Delete the CrossOrigin import.

- [ ] **Step 9: Remove `@CrossOrigin` from `ShelterEventController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/controller/ShelterEventController.java`
Delete line 28: `@CrossOrigin(origins = "http://35.225.196.242:3000")`
Delete the CrossOrigin import.

- [ ] **Step 10: Remove `@CrossOrigin` from `AdopterEventController.java`**

File: `pet-adoption-api/src/main/java/petadoption/api/controller/AdopterEventController.java`
Delete line 14: `@CrossOrigin(origins = "http://35.225.196.242:3000")`
Delete the CrossOrigin import.

- [ ] **Step 11: Commit**

```bash
git add pet-adoption-api/src/main/java/petadoption/api/endpoint/*.java \
        pet-adoption-api/src/main/java/petadoption/api/controller/*.java \
        pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetController.java \
        pet-adoption-api/src/main/java/petadoption/api/notifications/NotificationsController.java
git commit -m "refactor: remove per-controller @CrossOrigin annotations, use global CORS config"
```

---

## Task 3: Update `CorsConfig.java` with env-driven CORS + `CorsConfigurationSource` bean

**Files:**
- Modify: `pet-adoption-api/src/main/java/petadoption/api/config/CorsConfig.java`

- [ ] **Step 1: Rewrite CorsConfig.java**

Replace the full contents of `CorsConfig.java` with:

```java
package petadoption.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendUrl, "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .allowCredentials(true);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl, "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add pet-adoption-api/src/main/java/petadoption/api/config/CorsConfig.java
git commit -m "feat: add CorsConfigurationSource bean and env-driven FRONTEND_URL"
```

---

## Task 4: Update `SecurityConfig.java` — permit healthcheck and CSV import

**Files:**
- Modify: `pet-adoption-api/src/main/java/petadoption/api/config/SecurityConfig.java`

- [ ] **Step 1: Add `/ping` and `/api/pets/import-csv` to permitAll**

In `SecurityConfig.java`, in the `filterChain` method, add these two lines after the existing `.requestMatchers("/api/login", ...)` line:

```java
.requestMatchers("/ping").permitAll()
.requestMatchers(HttpMethod.GET, "/api/pets/import-csv").permitAll()
```

The full `.authorizeHttpRequests()` block should read:
```java
.authorizeHttpRequests()
    .requestMatchers("/api/login", "/api/signup", "/api/forgot-password", "/api/reset-password").permitAll()
    .requestMatchers("/ping").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/pets/import-csv").permitAll()
    .requestMatchers(HttpMethod.GET, "/images/**", "/uploads/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/pets").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/shelter/events").permitAll()
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/shelter/events/debug").permitAll()
    .requestMatchers("/api/admin/**").authenticated()
    .anyRequest().authenticated()
```

- [ ] **Step 2: Commit**

```bash
git add pet-adoption-api/src/main/java/petadoption/api/config/SecurityConfig.java
git commit -m "feat: permit /ping healthcheck and /api/pets/import-csv for deployment"
```

---

## Task 5: Fix hardcoded frontend URL in `PasswordResetService.java`

**Files:**
- Modify: `pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetService.java`

The password reset email contains a hardcoded link to `http://35.225.196.242:3000`. After deployment, this must point to the Vercel frontend URL.

- [ ] **Step 1: Add `@Value` injection for `FRONTEND_URL`**

At the top of the class (after the existing `@Autowired` fields), add:

```java
@Value("${FRONTEND_URL:http://localhost:3000}")
private String frontendUrl;
```

Add the import at the top of the file:
```java
import org.springframework.beans.factory.annotation.Value;
```

- [ ] **Step 2: Replace the hardcoded reset link**

On line 49, change:
```java
String resetLink = "http://35.225.196.242:3000/reset-password?token=" + token;
```
to:
```java
String resetLink = frontendUrl + "/reset-password?token=" + token;
```

- [ ] **Step 3: Commit**

```bash
git add pet-adoption-api/src/main/java/petadoption/api/passwordReset/PasswordResetService.java
git commit -m "fix: use FRONTEND_URL env var for password reset email link"
```

---

## Task 6: Fix hardcoded backend URLs in 3 frontend pages

**Files:**
- Modify: `pet-adoption-frontend/src/pages/signup.jsx:120-121`
- Modify: `pet-adoption-frontend/src/pages/forgot-password.jsx:29`
- Modify: `pet-adoption-frontend/src/pages/reset-password.jsx:39`

- [ ] **Step 1: Fix `signup.jsx`**

On line 120-121, change:
```js
const response = await fetch(
  "http://35.225.196.242:8080/api/signup",
```
to:
```js
const response = await fetch(
  `${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/signup`,
```

- [ ] **Step 2: Fix `forgot-password.jsx`**

On line 29, change:
```js
const response = await fetch('http://35.225.196.242:8080/api/forgot-password', {
```
to:
```js
const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/forgot-password`, {
```

- [ ] **Step 3: Fix `reset-password.jsx`**

On line 39, change:
```js
const response = await fetch('http://35.225.196.242:8080/api/reset-password', {
```
to:
```js
const response = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/reset-password`, {
```

- [ ] **Step 4: Commit**

```bash
git add pet-adoption-frontend/src/pages/signup.jsx \
        pet-adoption-frontend/src/pages/forgot-password.jsx \
        pet-adoption-frontend/src/pages/reset-password.jsx
git commit -m "fix: replace hardcoded backend URLs with NEXT_PUBLIC_BACKEND_URL env var"
```

---

## Task 7: Update `next.config.ts` — switch to `remotePatterns`

**Files:**
- Modify: `pet-adoption-frontend/next.config.ts`

- [ ] **Step 1: Replace `domains` with `remotePatterns`**

Replace the full file contents with:

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,

  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**.railway.app" },
      { protocol: "http", hostname: "localhost" },
    ],
  },
};

export default nextConfig;
```

- [ ] **Step 2: Commit**

```bash
git add pet-adoption-frontend/next.config.ts
git commit -m "feat: switch next.config image domains to remotePatterns for Railway"
```

---

## Task 8: Deploy backend to Railway

This task is done manually in the Railway dashboard + CLI.

- [ ] **Step 1: Push all commits to GitHub**

```bash
git push origin main
```

- [ ] **Step 2: Create Railway project**

1. Go to railway.app and log in
2. Click "New Project"
3. Select "Deploy from GitHub Repo"
4. Select the `pet-adoption-mock-site-demo` repository

- [ ] **Step 3: Add MySQL plugin**

1. In the Railway project, click "New" > "Database" > "MySQL"
2. Railway auto-provisions the database and injects `MYSQLHOST`, `MYSQLPORT`, etc. into the linked service

- [ ] **Step 4: Configure the API service**

1. Click the API service > Settings
2. Set **Root Directory** to `pet-adoption-api`
3. Set **Custom Dockerfile Path** to `../docker/pet-adoption-api.Dockerfile`
   - If Railway rejects the `../` path, create `pet-adoption-api/Dockerfile` with this content:
     ```dockerfile
     FROM gradle:8.9.0-jdk22 AS build
     WORKDIR /build
     COPY . .
     RUN ./gradlew build --no-daemon -p .

     FROM openjdk:22
     WORKDIR /app
     COPY --from=build /build/build/libs/pet-adoption-api-1.0.0-SNAPSHOT.jar app.jar
     ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
     ```
4. Set **Healthcheck Path** to `/ping`
5. Under **Networking**, generate a public domain (e.g., `pet-adoption-api-xxx.railway.app`)

- [ ] **Step 5: Set environment variables**

In the API service > Variables:
- `FRONTEND_URL` = (set after Vercel deploy in Task 9)
- All MySQL variables are auto-injected by the plugin — verify they exist

- [ ] **Step 6: Deploy and verify healthcheck**

1. Trigger a deploy
2. Watch build logs for successful Gradle build + Spring Boot startup
3. Visit `https://<railway-url>/ping` — should return a response
4. Check Railway logs for "CORS configuration applied" and "Started PetAdoptionApplication"

---

## Task 9: Deploy frontend to Vercel

- [ ] **Step 1: Connect repo to Vercel**

1. Go to vercel.com and log in
2. Click "Add New" > "Project"
3. Import the `pet-adoption-mock-site-demo` GitHub repo
4. Set **Root Directory** to `pet-adoption-frontend`
5. Framework Preset: Next.js (auto-detected)

- [ ] **Step 2: Set environment variable**

In Vercel project settings > Environment Variables:
- `NEXT_PUBLIC_BACKEND_URL` = `https://<railway-api-url>` (the Railway public domain from Task 8 Step 4)

- [ ] **Step 3: Deploy**

Click "Deploy". Vercel will run `yarn build` and deploy.

- [ ] **Step 4: Copy Vercel URL back to Railway**

1. Note the Vercel deployment URL (e.g., `https://pet-adoption-xxx.vercel.app`)
2. Go to Railway > API service > Variables
3. Set `FRONTEND_URL` = `https://pet-adoption-xxx.vercel.app`
4. Railway will auto-redeploy with the new CORS origin

---

## Task 10: Seed data and verify

- [ ] **Step 1: Seed the 150 sample pets**

```bash
curl https://<railway-api-url>/api/pets/import-csv
```

Should return a list of 150 imported pets.

- [ ] **Step 2: End-to-end verification**

Visit the Vercel URL and test:
1. Homepage loads with pet listings
2. Browse/search pets works
3. Sign up with a new account
4. Log in with the new account
5. View pet details
6. Submit an adoption request
7. Check events page

- [ ] **Step 3: Test default accounts**

Log in with the pre-seeded shelter account:
- Email: `shelter@example.com`
- Password: `defaultPassword123`

Verify shelter dashboard features work (add events, manage pets).
