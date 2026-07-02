---
paths:
  - "lobby-server/**"
---

# lobby-server — 로그인 로비 (Java/Minestom, 독립 실행 앱)

Bukkit/Velocity 플러그인이 **아니라** `Main.main()`에서 서버를 직접 조립하는 Minestom 앱이다.
배포물은 shadow fat jar(`lobby-server-all.jar`, `mergeServiceFiles()` 필요 — logback 등
서비스 파일 병합).

## 의도적으로 미니멀

로비의 역할은 "인증 전 플레이어가 대기하며 `/login`·`/verify`를 치는 곳" 뿐이다.
평지 월드(y=40 잔디) + 커맨드 2개가 전부이며, 게임 콘텐츠를 여기에 추가하지 않는다.
콘텐츠는 Paper 쪽(smp-server / content plugin)에 넣는다.

## 개발 시 알아둘 것

- **Velocity 포워딩 분기**: `config.properties`의 `velocitySecret`이 비어 있거나
  `CHANGE_ME`로 시작하면 standalone(개발) 모드로 뜬다. 이 분기 덕에 프록시 없이 단독 실행해
  커맨드를 테스트할 수 있다.
- 설정 파일은 `./config.properties` — 없으면 기본값으로 자동 생성된다. 키 추가 시
  `LobbyConfig` 생성자와 기본값 생성 블록 양쪽 수정.
- 커맨드는 Minestom `Command` 상속 클래스로 만들고 `MinecraftServer.getCommandManager().register()`.
  Bukkit과 달리 plugin.yml 같은 선언 파일이 없다.
- **`/verify` 성공은 2단계**: ① `AuthClient.bind()`(auth-server REST, 키→UUID 바인딩)
  ② 성공 시 `LINK_UPDATED` 플러그인 메시지로 Velocity에 상태 리로드 통지.
  ②를 빼먹으면 DB에는 링크됐는데 게이트는 안 열리는 상태가 된다.
- `AuthClient.bind()`는 실패(만료 키·네트워크 오류)를 null로 돌려준다 — 콜백에서 null이 "실패" 경로.
- Minestom은 스냅샷 버전(`26_2-SNAPSHOT`)이며 Sonatype snapshots 저장소에서 온다
  (settings.gradle.kts에 `net.minestom` 그룹 한정으로 등록돼 있음). 버전 갱신은
  `gradle/libs.versions.toml`의 `minestom`.
