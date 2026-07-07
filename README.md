# oh-my-smp

Minecraft **26.1.2** SMP 테스트 네트워크를 위한 멀티 모듈 Gradle 빌드. DataGSM OAuth 인증 스택(SmpAuth)과 실제 SMP 게임플레이 플러그인(`smp-server` = oh-my-smp)을 한 저장소에 담고 있다.

전체 아키텍처와 모듈 구성은 [`CLAUDE.md`](CLAUDE.md)를 참고. 이 문서는 **`smp.sh`로 전체 스택을 띄우는 방법**을 다룬다.

## smp.sh 란

`smp.sh`는 4개 프로세스로 이뤄진 전체 스택(auth-server + Minestom 로비 + Velocity 프록시 + SmpAuth·oh-my-smp를 올린 Paper 컨텐츠 서버)을 관리하는 단일 CLI다.

동작 원리:

- **프로필이 유일한 진실의 원천(single source of truth)이다.** Spring Boot 스타일로 `profiles/<name>.env` 파일 하나가 모든 설정을 결정한다.
  - `profiles/local.env` — 커밋된 테스트 기본값. 그대로 바로 동작한다.
  - `profiles/production.env` — gitignore 대상. `profiles/production.env.example`을 복사해 실제 자격증명을 채운다.
- 생성되는 모든 설정 파일(`auth/config.properties`, `lobby/config.properties`, `velocity.toml`, `forwarding.secret`, smp-auth 플러그인 설정, `server.properties`, `paper-global.yml` 포워딩 패치)은 매 `start`마다 프로필에서 **다시 렌더링**된다. → **렌더링된 파일을 직접 수정하지 말 것.** 프로필을 고치고 재시작한다.
- 스택은 **스크립트를 실행한 디렉터리(스크래치 디렉터리)** 에 생성된다. 소스 트리를 어지럽히지 않도록 **저장소 루트에서는 실행이 거부된다.**

### 요구사항

- **Java 25** (전체 스택이 Java 25 툴체인을 타깃으로 함)
- **tmux** — 각 프로세스가 자기 tmux 세션에서 돌기 때문에 콘솔 접속에 필요 (`brew install tmux`)
- **python3** — 설정 다운로드와 `paper-global.yml` 패치에 사용

## 빠른 시작

```bash
# 1) 저장소 밖에 스크래치 디렉터리를 만들고 그 안에서 실행
mkdir ~/smp-test && cd ~/smp-test

# 2) 프로비저닝: jar 빌드 + Velocity·Paper 다운로드 + 설정 렌더링 + Paper 첫 부팅
/path/to/oh-my-smp/smp.sh setup                 # 기본 프로필: local
#   프로덕션 자격증명으로 띄우려면:
/path/to/oh-my-smp/smp.sh setup --profile production

# 3) 스택 실행 (설정 재렌더링 후 4개 프로세스를 tmux로 기동)
/path/to/oh-my-smp/smp.sh start

# 4) 마인크래프트 26.1.2 클라이언트로 127.0.0.1:25565 접속
#    로비에서: /login → 뜬 URL 열기 → DataGSM → /verify <key>
#    이후 컨텐츠 서버로: /server content   (oh-my-smp 게임플레이)

# 5) 종료
/path/to/oh-my-smp/smp.sh stop
```

## 명령어

| 명령 | 설명 |
|------|------|
| `setup [--profile NAME]` | 이 디렉터리에 스택을 프로비저닝: jar 빌드, Velocity·Paper 다운로드, 설정 렌더링, Paper 첫 부팅. 프로필을 `.smp-profile`에 기록 (기본값: `local`). |
| `update [--restart]` | 프로젝트 jar만 다시 빌드해 교체 (설정·DB·월드는 그대로). `--restart`로 스택도 함께 재시작. |
| `start [name...]` | 프로필에서 모든 설정을 재렌더링한 뒤 기동 (기본: 전체 `auth lobby paper velocity`). |
| `stop [name...]` | 서버를 정상 종료 (기본: 전체, 시작 역순). |
| `restart [name...]` | stop + start. |
| `console <name>` | 해당 서버의 tmux 콘솔에 접속 (분리: `Ctrl-B`, `D`). |
| `status` | 활성 프로필과 서버별 상태 표시. |
| `logs <name> [-f]` | 서버 로그 출력(또는 `-f`로 실시간 추적). |
| `render` | 기동 없이 프로필에서 설정만 재렌더링. |
| `help` | 사용법 출력. |

`<name>`은 `auth`, `lobby`, `velocity`, `paper` 중 하나.

**프로필 선택 우선순위:** `--profile NAME` > `$SMP_PROFILE` > `.smp-profile` 마커.

### 포트 (기본값, 프로필에서 변경 가능)

| 프로세스 | 포트 | 용도 |
|----------|------|------|
| Velocity | 25565 | 플레이어가 접속하는 프록시 |
| lobby | 25566 | Minestom 로그인 로비 |
| paper | 25567 | Paper 컨텐츠 서버 |
| auth | 8080 | 인증 웹 서버 |

## 코드 변경 후 워크플로

```bash
cd ~/smp-test
/path/to/oh-my-smp/smp.sh update            # jar만 다시 빌드·교체 (설정/DB/월드 유지)
/path/to/oh-my-smp/smp.sh restart           # 실행 중인 서버는 로드된 클래스를 유지하므로 재시작 필요
# 또는 한 번에:
/path/to/oh-my-smp/smp.sh update --restart
```

**프로필만 변경**한 경우엔 설정이 `start` 시 재렌더링되므로 `smp.sh restart`만으로 충분하다.

## 콘솔 & 로그

```bash
smp.sh console paper       # Paper 콘솔에 접속해 관리 명령 실행 (분리: Ctrl-B 후 D)
smp.sh logs velocity -f    # Velocity 로그 실시간 추적
smp.sh status              # 활성 프로필 + 서버별 상태
```

## 프로필 설정

프로필에서 조정 가능한 주요 항목 (전체 스키마는 [`profiles/local.env`](profiles/local.env) 참고):

- 포트: `SMP_VELOCITY_PORT`, `SMP_LOBBY_PORT`, `SMP_PAPER_PORT`, `SMP_AUTH_PORT`
- 시크릿: `SMP_FORWARDING_SECRET`(Velocity ↔ 백엔드 modern forwarding), `SMP_SHARED_SECRET`(auth ↔ lobby ↔ velocity 플러그인)
- DataGSM OAuth: `SMP_DATAGSM_CLIENT_ID`, `SMP_DATAGSM_CLIENT_SECRET`, `SMP_DATAGSM_SCOPE`, `SMP_PUBLIC_BASE_URL`
- 게이팅: `SMP_GATED_SERVERS`(비우면 로비 외 모든 서버 게이팅), `SMP_CONTENT_SERVER_NAME`
- 네트워크: `SMP_ONLINE_MODE`, `SMP_MOTD`, `SMP_MAX_PLAYERS`, `SMP_LEVEL_TYPE`
- 런타임/버전: `SMP_JAVA`, `SMP_PAPER_VERSION`, `SMP_VELOCITY_VERSION`, `SMP_PAPER_HEAP`, `SMP_VELOCITY_HEAP`

프로필은 오버라이드할 키만 적으면 된다 — 나머지는 `smp.sh` 내장 기본값을 사용한다.

> Linux에서는 `SMP_JAVA` 기본값(brew openjdk@25 경로)이 존재하지 않으므로, 프로필에 Java 25 바이너리 경로를 명시해야 한다 (빌드·런타임 모두에 사용됨).
