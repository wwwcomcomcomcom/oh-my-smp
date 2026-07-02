---
paths:
  - "content-lib/**"
---

# content-lib — Paper 플러그인 "SmpAuth" + 콘텐츠 API

주의: **모듈명은 content-lib, 플러그인 이름은 SmpAuth**다(plugin.yml `name: SmpAuth`).
smp-server의 `softdepend: [SmpAuth]`, `pm.getPlugin("SmpAuth")`가 이 이름에 묶여 있으므로
plugin.yml의 name을 바꾸면 안 된다.

## 공개 API — 소비자는 smp-server 하나뿐

노출 표면: `SmpAuth.get(player)` / `SmpAuth.isLinked(player)`(정적 캐시 조회),
`AuthDataLoadedEvent`, 그리고 `api(project(":common"))`로 재노출되는 `StudentData`.

이 API의 실소비자는 현재 **smp-server뿐**이므로 시그니처 변경은 자유 — 단 변경 시
smp-server, `docs/CONTENT-SERVER-GUIDE.md`, `sample-content-plugin`을 같이 갱신한다.

## 동작 모델 (수정 시 유지할 것)

- 플레이어 조인 → **다음 틱에** `AUTH_REQUEST` 전송(연결이 안정된 뒤 보내기 위한 지연).
- `AUTH_RESPONSE` 수신 → linked면 캐시 put, 아니면 remove → 어느 쪽이든
  `AuthDataLoadedEvent` 발화(미링크면 `data == null`).
- 따라서 **조인 직후 1~2틱은 캐시가 비어 있을 수 있다.** API 문서/가이드가 이 계약을
  명시하고 있으므로 "조인 즉시 데이터 보장"으로 바꾸려 하지 말 것.
- 퇴장 시 캐시 remove. put/remove는 패키지 내부 전용(static package-private) — 공개하지 않는다.

## 빌드/의존 규칙

- 이 플러그인 jar가 `common`을 셰이드해서 **`StudentData`/`AuthMessage`의 유일한 런타임
  소유자**가 된다. 소비 플러그인(smp-server 등)은 반드시 `compileOnly(project(":content-lib"))`로만
  참조하고 절대 shade하지 않는다 — 중복 로드되면 클래스 아이덴티티가 갈라진다.
- 배포 jar는 classifier 없는 shadow jar(`content-lib.jar`), 일반 jar는 `-thin`.
  Gson은 Paper가 제공하므로 exclude.
