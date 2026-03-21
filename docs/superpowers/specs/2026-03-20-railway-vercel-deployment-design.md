# Pet Adoption Demo — Vercel + Railway Deployment

## Goal

Deploy the pet adoption site as a live, fully functional demo accessible via URL, suitable for embedding/linking from a personal portfolio website.

- **Frontend (Vercel):** Next.js 15 — free tier
- **Backend (Railway):** Spring Boot 3.4 API + MySQL 8.4 plugin — $5/mo Hobby plan

## Architecture

```
[Browser] ---> [Vercel: Next.js Frontend]
                        |
                        | NEXT_PUBLIC_BACKEND_URL
                        v
               [Railway: Spring Boot API] <---> [Railway: MySQL Plugin]
```

All communication between frontend and backend is via REST over HTTPS. Railway provides a public URL for the API; Vercel provides a public URL for the frontend.

## Changes Required

### 1. Backend — Spring Boot API (Railway)

#### 1a. Server port + Database configuration (`application.yml`)

Railway assigns a dynamic port via the `PORT` env var. The app must bind to it.

Railway's MySQL plugin provides these env vars automatically:
- `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`

Update `application.yml`:

```yaml
server:
  port: ${PORT:8080}

spring:
  datasource:
    url: jdbc:mysql://${MYSQLHOST:localhost}:${MYSQLPORT:3307}/${MYSQLDATABASE:petadoption}
    username: ${MYSQLUSER:root}
    password: ${MYSQLPASSWORD:password}
```

Note: The existing config uses `${MYSQL_HOST:localhost}` (with underscore). Railway provides `MYSQLHOST` (no underscore). This change updates the env var name; anyone using `MYSQL_HOST` in local Docker Compose should update accordingly. The local fallback defaults remain identical.

#### 1b. CORS configuration — full cleanup

**Problem:** There are ~10 controllers with hardcoded `@CrossOrigin` annotations pointing at `35.225.196.242`. When a controller-level `@CrossOrigin` is present, Spring uses that instead of the global `CorsConfig`. This means most API endpoints will reject requests from the Vercel domain.

**Fix (two parts):**

**Part 1 — Remove all per-controller `@CrossOrigin` annotations** from these files:
- `PingEndpoint.java`
- `AdoptionRequestEndpoint.java`
- `PasswordResetController.java`
- `RecommendationEndpoint.java`
- `DebugController.java`
- `AdopterEventController.java`
- `EventController.java`
- `ShelterEventController.java`
- `PetEndpoint.java`
- `NotificationsController.java`

**Part 2 — Update `CorsConfig.java`** to read the frontend URL from env and register a `CorsConfigurationSource` bean so Spring Security's CORS filter and Spring MVC are aligned:

```java
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

#### 1c. Upload path (`application.yml`)

Change from hardcoded Windows path to relative:

```yaml
pet:
  upload:
    base-path: ${UPLOAD_PATH:./pet-uploads}
```

Ephemeral on Railway (resets on redeploy) — acceptable for demo purposes.

#### 1d. Healthcheck + Security

Add `/ping` to the Spring Security `permitAll()` list in `SecurityConfig.java` so Railway's healthcheck can hit it unauthenticated:

```java
.requestMatchers("/ping").permitAll()
```

Also add `/api/pets/import-csv` to `permitAll()` so we can seed data without authentication:

```java
.requestMatchers(HttpMethod.GET, "/api/pets/import-csv").permitAll()
```

#### 1e. Railway service configuration

Railway will be configured via the dashboard (not `railway.toml`) to avoid path conflicts:

- **Service > Settings > Root Directory:** `pet-adoption-api`
- **Service > Settings > Custom Dockerfile Path:** `../docker/pet-adoption-api.Dockerfile`
  - If Railway doesn't support `../`, copy the Dockerfile into `pet-adoption-api/Dockerfile` instead
- **Service > Settings > Healthcheck Path:** `/ping`

#### 1f. Schema on fresh database

There is no Flyway in this project (no Flyway dependency in `build.gradle`). The `db/migration/*.sql` files are legacy MySQL dumps, not Flyway migrations. Hibernate `ddl-auto: update` will create all tables from scratch on the fresh Railway MySQL. No changes needed.

### 2. Frontend — Next.js (Vercel)

#### 2a. Fix hardcoded backend URLs

The `useApi.jsx` hook correctly reads `process.env.NEXT_PUBLIC_BACKEND_URL`, but **three pages bypass it** with hardcoded `fetch()` calls:

- `src/pages/signup.jsx` — hardcoded `http://35.225.196.242:8080/api/signup`
- `src/pages/forgot-password.jsx` — hardcoded `http://35.225.196.242:8080/api/forgot-password`
- `src/pages/reset-password.jsx` — hardcoded `http://35.225.196.242:8080/api/reset-password`

**Fix:** Replace the hardcoded URLs with `` `${process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'}/api/...` `` or refactor to use the `useApi` hook.

#### 2b. Environment variable

Set in Vercel dashboard:
- `NEXT_PUBLIC_BACKEND_URL` = `https://<railway-api-url>`

#### 2c. Image domains (`next.config.ts`)

Switch from `domains` to `remotePatterns` for flexibility:

```ts
images: {
  remotePatterns: [
    { protocol: "https", hostname: "**.railway.app" },
    { protocol: "http", hostname: "localhost" },
  ],
},
```

#### 2d. Vercel deployment

Connect the GitHub repo to Vercel. Set:
- **Root Directory:** `pet-adoption-frontend`
- **Framework Preset:** Next.js (auto-detected)
- **Build Command:** `yarn build` (auto-detected)

### 3. Post-Deployment

#### 3a. Seed pet data

After both services are running, hit the CSV import endpoint (now permitted unauthenticated per 1d):

```bash
curl https://<railway-api-url>/api/pets/import-csv
```

This loads 150 sample pets from the bundled CSV. The default shelter account (`shelter@example.com` / `defaultPassword123`) and admin password (`admin123`) are created automatically on startup.

#### 3b. Verify

- Visit the Vercel URL — homepage should load
- Browse pets — should show 150 seeded pets
- Sign up / log in — should work via Railway API
- Adopt a pet — full flow should work

## Environment Variables Summary

### Railway (API service)
| Variable | Value | Source |
|----------|-------|--------|
| `MYSQLHOST` | auto | Railway MySQL plugin |
| `MYSQLPORT` | auto | Railway MySQL plugin |
| `MYSQLDATABASE` | auto | Railway MySQL plugin |
| `MYSQLUSER` | auto | Railway MySQL plugin |
| `MYSQLPASSWORD` | auto | Railway MySQL plugin |
| `FRONTEND_URL` | `https://<vercel-url>` | Manual |
| `PORT` | auto | Railway platform |

### Vercel (Frontend)
| Variable | Value | Source |
|----------|-------|--------|
| `NEXT_PUBLIC_BACKEND_URL` | `https://<railway-api-url>` | Manual |

## Files Modified

### Backend
1. `pet-adoption-api/src/main/resources/application.yml` — port, DB config, upload path
2. `pet-adoption-api/src/main/java/petadoption/api/config/CorsConfig.java` — env-driven CORS + `CorsConfigurationSource` bean
3. `pet-adoption-api/src/main/java/petadoption/api/config/SecurityConfig.java` — add `/ping` and import-csv to permitAll
4. Remove `@CrossOrigin` annotations from 10 controller/endpoint files

### Frontend
5. `pet-adoption-frontend/src/pages/signup.jsx` — fix hardcoded backend URL
6. `pet-adoption-frontend/src/pages/forgot-password.jsx` — fix hardcoded backend URL
7. `pet-adoption-frontend/src/pages/reset-password.jsx` — fix hardcoded backend URL
8. `pet-adoption-frontend/next.config.ts` — image remotePatterns

## Risk Assessment

- **Low risk:** All changes are config-level. No business logic modified.
- **Upload ephemeral:** Uploaded images lost on redeploy — acceptable for demo.
- **Gmail SMTP creds in repo:** Pre-existing issue, not introduced by this work.
- **Cost:** Railway Hobby at $5/mo. Vercel free tier. MySQL plugin included in Railway Hobby.

## Out of Scope

- Custom domain setup (can be added later in Vercel/Railway dashboards)
- CI/CD pipeline changes
- Moving secrets to env vars (pre-existing tech debt)
- Persistent file storage for uploads
