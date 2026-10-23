# mod-licenses Build Guide

This guide explains how to build **mod-licenses** (the FOLIO Licenses backend module, a Grails application) on any machine: Linux, macOS (Intel or Apple Silicon), or Windows, on x86_64 or ARM64.

Unless stated otherwise, paths are relative to the repository root (the folder that contains this file). The Grails project itself lives in `service/`. Replace placeholders such as `<GRADLE_INSTALL_DIR>` with locations of your choice.

---

## 1. What you are building

| Item | Value |
|------|-------|
| Framework | Grails **6.2.3**, GORM 8.0.3, Spring Boot 2.7, Groovy 3.0.23 (`service/gradle.properties`) |
| Build tool | Gradle **7.6.3** (`service/gradle/wrapper/gradle-wrapper.properties`) |
| Required JDK | **17** (the official runtime image is Java 17; Gradle 7.6.3 cannot run on JDK 20+) |
| Web server | Embedded Undertow |
| Database | **PostgreSQL** (required at runtime, not at build time) |
| Output | `service/build/libs/mod-licenses-<version>.jar` (executable Spring Boot jar) |
| Version | `appVersion` in `service/gradle.properties` (e.g. `6.4.0-SNAPSHOT`); in Jenkins, `BUILD_NUMBER` is appended to snapshot versions |

mod-licenses is a FOLIO module: in a real deployment it runs behind **Okapi** (the FOLIO API gateway), which routes requests, provides tenant and permission headers, and enables tenants. The module can be built and started on its own (section 7), but most business endpoints only work behind Okapi.

---

## 2. Things to know before building

### 2.1 The Gradle wrapper jar is missing

`service/gradle/wrapper/gradle-wrapper.jar` is not in the repository, so `service/gradlew` and `gradlew.bat` fail (`Could not find or load main class org.gradle.wrapper.GradleWrapperMain`). The same is true for `grailsw`. Install **Gradle 7.6.3** and run `gradle` directly (section 4). No Grails CLI is needed.

Optional: with Gradle 7.6.3 installed, `gradle wrapper --gradle-version 7.6.3` (run inside `service/`) regenerates the wrapper.

### 2.2 Dependency repositories

Besides Maven Central and the Gradle Plugin Portal, the build downloads from these repositories (declared in `service/build.gradle`). They must be reachable from the build machine (whitelist them on a proxy or firewall):

- `https://repo.grails.org/grails/core` and `https://repo.grails.org/grails/plugins`
- `https://maven.k-int.com/repository/releases` (K-Int libraries: `web-toolkit-ce`, `grails-okapi`, `access-control-grails`)
- `https://maven.indexdata.com/`
- `https://jitpack.io`

---

## 3. Install JDK 17

Any OpenJDK 17 build works (Eclipse Temurin, Azul Zulu, Amazon Corretto, Microsoft, Liberica, etc.), with native x86_64 and ARM64 builds including Apple Silicon.

| Platform | Command |
|----------|---------|
| SDKMAN (Linux, macOS, WSL) | `sdk install java 17.<x>-tem` (pick a version from `sdk list java`) |
| Debian / Ubuntu | `sudo apt install openjdk-17-jdk` |
| Fedora / RHEL | `sudo dnf install java-17-openjdk-devel` |
| Arch | `sudo pacman -S jdk17-openjdk` |
| macOS (Homebrew) | `brew install --cask temurin@17` |
| Windows (winget) | `winget install EclipseAdoptium.Temurin.17.JDK` |
| Windows (Chocolatey) | `choco install temurin17` |
| Manual | https://adoptium.net/temurin/releases/?version=17 |

Verify:

```bash
java -version    # must print version "17.x"
```

---

## 4. Install Gradle 7.6.3

| Option | Command |
|--------|---------|
| SDKMAN (Linux, macOS, WSL) | `sdk install gradle 7.6.3` |
| Manual (all platforms) | Download https://services.gradle.org/distributions/gradle-7.6.3-bin.zip and extract it to `<GRADLE_INSTALL_DIR>` |

Package managers install Gradle 8/9, which is not compatible with this Grails 6 build. Use 7.6.3.

**Linux / macOS / WSL:**

```bash
export JAVA_HOME=<path-to-jdk17>
export PATH="$JAVA_HOME/bin:<GRADLE_INSTALL_DIR>/gradle-7.6.3/bin:$PATH"
```

**Windows PowerShell:**

```powershell
$env:JAVA_HOME = "<path-to-jdk17>"
$env:Path = "$env:JAVA_HOME\bin;<GRADLE_INSTALL_DIR>\gradle-7.6.3\bin;$env:Path"
```

**Windows cmd.exe:**

```bat
set JAVA_HOME=<path-to-jdk17>
set PATH=%JAVA_HOME%\bin;<GRADLE_INSTALL_DIR>\gradle-7.6.3\bin;%PATH%
```

Verify:

```bash
gradle -version    # Gradle 7.6.3, JVM: 17.x
```

---

## 5. Build

All Gradle commands run from `service/`:

```bash
cd service
gradle --no-daemon assemble
```

The first build downloads all dependencies (cached in `~/.gradle`, `%USERPROFILE%\.gradle` on Windows) and takes a few minutes. Outputs in `service/build/libs/`:

| File | Description |
|------|-------------|
| `mod-licenses-<version>.jar` | Executable Spring Boot jar (about 160 MB). This is the artifact to run or package in Docker. |
| `mod-licenses-<version>-plain.jar` | Plain jar with only the module's own classes |

The build also generates the FOLIO descriptors (`ModuleDescriptor.json`, `DeploymentDescriptor.json`) from `service/src/main/okapi/*-template.json` into `service/build/resources/main/okapi/`.

Other useful commands (from `service/`):

```bash
gradle --no-daemon compileGroovy    # compile only
gradle --no-daemon clean            # delete service/build
```

`gradle build` also runs the unit and integration tests. The integration tests need a PostgreSQL database `okapi_modules_test` (and MinIO for file storage tests); see "Integration Tests" in the README and `tools/testing/docker-compose.yml`. They are not needed to produce the jar.

---

## 6. Build with Docker (no local JDK or Gradle)

Works on any OS and CPU that runs Docker (`eclipse-temurin:17-jdk` is multi-arch). From the repository root:

```bash
docker run --rm -v "$PWD":/src -w /src/service eclipse-temurin:17-jdk bash -c '
  set -e
  apt-get update -qq && apt-get install -y -qq unzip curl >/dev/null
  curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-7.6.3-bin.zip
  unzip -q /tmp/gradle.zip -d /opt
  /opt/gradle-7.6.3/bin/gradle --no-daemon assemble
'
```

On Windows PowerShell replace `"$PWD":/src` with `"${PWD}:/src"`. On Linux, files created by the container may be owned by `root`; remove them with `sudo` if needed.

### 6.1 Build the runtime Docker image

After the jar exists in `service/build/libs/`, the `Dockerfile` in the repository root packages it on top of the FOLIO base image `folioci/alpine-jre-openjdk17`:

```bash
docker build -t mod-licenses:local .
```

- The jar is copied to `/usr/verticles/mod-licenses.war` and started by the base image's `run-java.sh`; the container listens on port 8080.
- Keep only one version of the boot jar in `service/build/libs/` (run `gradle clean assemble` after a version change), because the `COPY` uses a wildcard.
- The base image is published for **amd64 only**. On ARM64 hosts (Apple Silicon, ARM servers) build and run with `--platform linux/amd64` (emulated), for example `docker build --platform linux/amd64 -t mod-licenses:local .`.

There is also a Gradle task that writes its own Dockerfile and builds an image named `folioci/mod-licenses:<version>` (requires access to a Docker daemon): `gradle --no-daemon buildImage` (from `service/`).

---

## 7. Run

### 7.1 Start PostgreSQL

The module connects to `jdbc:postgresql://<db.host>:<db.port>/<db.database>` with user `folio_admin` / `folio_admin` by default. `tools/testing/init.sql` creates those users and the `okapi_modules` database. Quick start with Docker (any OS/CPU):

```bash
docker run -d --name mod-licenses-pg -p 5432:5432 \
  -e POSTGRES_PASSWORD=pa55w0rd \
  -v "$PWD/tools/testing/init.sql":/docker-entrypoint-initdb.d/init.sql:ro \
  postgres:17
```

(Or `docker compose -f tools/testing/docker-compose.yml up -d postgres`, which also maps port 54321.)

### 7.2 Start the module

```bash
java -jar service/build/libs/mod-licenses-<version>.jar
```

Connection settings can be overridden with environment variables (Spring relaxed binding) or `--key=value` arguments:

| Setting | Env variable | Default |
|---------|--------------|---------|
| `db.host` | `DB_HOST` | `localhost` |
| `db.port` | `DB_PORT` | `5432` |
| `db.database` | `DB_DATABASE` | `okapi_modules` |
| `db.username` | `DB_USERNAME` | `folio_admin` |
| `db.password` | `DB_PASSWORD` | `folio_admin` |
| `db.maxpoolsize` | `DB_MAXPOOLSIZE` | `10` |
| `server.port` | `SERVER_PORT` | `8080` |

Check that it is up:

```bash
curl http://localhost:8080/admin/health
# {"status":"UP"}
```

### 7.3 Enable a tenant (normally done by Okapi)

Okapi enables a tenant by calling the module's tenant API, which creates the `<tenant>_mod_licenses` schema, runs the Liquibase migrations and loads reference data. You can do the same by hand:

```bash
curl -X POST http://localhost:8080/_/tenant \
  -H "X-Okapi-Tenant: diku" -H "Content-Type: application/json" \
  -d '{"module_to":"mod-licenses-<version>","parameters":[{"key":"loadReference","value":"true"}]}'
```

A `201` response and a new `diku_mod_licenses` schema in PostgreSQL confirm the module works.

Business endpoints such as `/licenses/licenses` additionally call back into Okapi (they need the `X-Okapi-Url`, `X-Okapi-Token`, etc. headers that Okapi adds). Called directly, without Okapi, they return HTTP 500 with `IllegalArgumentException: URI is not absolute` in the log. This is expected; to use the API, register and deploy the module in a FOLIO/Okapi environment using the generated descriptors (section 5).

### 7.4 Alternative configurations

`service/grails-app/conf` contains environment files for other local setups, selected with `-Dgrails.env=<name>`: `vagrant-db` (FOLIO Vagrant box) and `rancher-desktop-db` (PostgreSQL on port 30101, user `postgres`, server port 8081). See the comments at the top of each file.

---

## 8. Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | Wrapper jar missing (section 2.1). Use Gradle 7.6.3 directly. |
| `Could not resolve com.k_int...` / `Could not GET 'https://maven.k-int.com/...'` | The K-Int (or Index Data / JitPack) repository is blocked (section 2.2). |
| `Unsupported class file major version 6x` or Gradle fails to start | Gradle 7.6.3 is running on JDK 20 or newer. Use JDK 17. |
| Errors with Gradle 8 or 9 | Use Gradle 7.6.3. |
| `Connection to localhost:5432 refused` at start-up | PostgreSQL is not running or uses another host/port (section 7). |
| `password authentication failed for user "folio_admin"` | The database users were not created; run `tools/testing/init.sql` or set `DB_USERNAME` / `DB_PASSWORD`. |
| HTTP 500 `URI is not absolute` on API calls | The module is called without Okapi (section 7.3). |
| Tenant upgrade hangs / Liquibase lock | See "Locks and failure to upgrade" in the README. |
| `docker build` fails with `exec format error` or `no matching manifest` on ARM64 | The base image is amd64 only; add `--platform linux/amd64` (section 6.1). |
| `docker build` fails with "COPY ... must be a directory" | More than one boot jar in `service/build/libs/`; run `gradle clean assemble`. |
| Out of memory while compiling | Increase `org.gradle.jvmargs` in `service/gradle.properties` (e.g. `-Xmx2g`). |

---

## 9. Quick reference

```bash
# Requirements: JDK 17 + Gradle 7.6.3 on PATH (sections 3 and 4)
java -version              # 17.x
gradle -version            # 7.6.3

# Build
cd service
gradle --no-daemon assemble        # -> service/build/libs/mod-licenses-<version>.jar
cd ..

# Optional runtime image (amd64 base image)
docker build -t mod-licenses:local .

# Run (needs PostgreSQL, see section 7)
java -jar service/build/libs/mod-licenses-<version>.jar
curl http://localhost:8080/admin/health
```
