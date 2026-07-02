---
paths:
  - "auth-server/**"
---

# auth-server — DataGSM OAuth 웹 서버 (Kotlin/Ktor)

진입점은 `Application.kt`의 `main()`. 로컬 실행: `./gradlew :auth-server:run`
(application 플러그인). 코드 주석·로그는 영어, 유저에게 보이는 HTML 페이지는 한국어 컨벤션.

## 설정 (`Config`)

우선순위: **환경변수 > properties 파일(`SMP_AUTH_CONFIG`, 기본 `./config.properties`) > 기본값**.
새 설정을 추가할 때는 `Config` 데이터 클래스 + `load()`의 `get(key, env, default)` 호출을
같이 추가한다. 시크릿(clientSecret, sharedSecret)은 기본값 없이 필수로 두고 절대 하드코딩하지 않는다.

## 저장소 3종 — 역할이 다르다

| 클래스 | 저장 | 성격 |
|---|---|---|
| `KeyStore` | 인메모리 | 발급된 인증 키. 단일 사용, TTL 만료. 유실돼도 재발급하면 됨 |
| `PendingStore` | 인메모리 | OAuth `state` → PKCE verifier. 5분 TTL, 단일 사용 |
| `LinkRepository` | SQLite | `uuid → StudentData` 영속 링크 |

- `LinkRepository`의 스냅샷은 **bind 시점에 한 번 기록하고 이후 갱신하지 않는 게 의도된 설계**다
  (재인증 `/verify` 시에만 upsert로 덮어씀). 코드의 "per spec §5.3" 주석은 삭제된 문서의
  잔재이니 근거로 삼지 말 것 — 근처를 수정할 때 주석을 정리해도 된다.
- 인메모리 스토어의 만료 정리는 `main()`의 1분 주기 스케줄러가 담당. 새 TTL 스토어를 만들면
  거기에 `evictExpired()`를 등록한다.

## 라우팅 규칙

- 브라우저용(`/login`, `/callback`)은 HTML 응답. 사용자 데이터를 HTML에 보간할 때는 반드시 `esc()`.
- 서버 간 REST(`/api/*`)는 반드시 첫 줄에서 `call.requireAuth(cfg.sharedSecret)` 검사.
  요청/응답 바디 DTO는 common의 `RestDtos`에 추가한다.

## 기타

- DataGSM SDK는 jitpack의 `com.github.themoment-team:datagsm-oauth-sdk-java`
  (`gradle/libs.versions.toml`의 `datagsm`). OAuth URL 빌드·토큰 교환·UserInfo 조회를 전부 SDK가 처리.
- `KeyStore.issue()`의 `val keyLength = length` 로컬 캡처는 `buildString {}` 리시버의
  `StringBuilder.length`(0)에 셰도잉되는 걸 막기 위한 것 — 제거하면 무한 루프성 버그가 된다.
- 전체 플로우 검증은 스크래치 디렉토리에서 `setup.sh` 스택으로 한다. 유닛테스트는 두지 않는다.
