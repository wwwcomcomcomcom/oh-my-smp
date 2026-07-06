---
paths:
  - "smp-server/**"
---

# smp-server — oh-my-smp 게임플레이 플러그인 (Kotlin/Paper)

모듈 구조·빌드·배포 산출물은 CLAUDE.md 참고. 여기는 코드 작업 시 지켜야 할 세부 규칙만.
이 모듈은 주석·로그·유저 메시지 전부 **한국어** 컨벤션이다(인증 스택 모듈들과 다름).

## 게임 규칙 수치는 건드리지 않는다

보더 반경·컴뱃 시간·드롭 확률·드래곤 체력 같은 수치는 서버 주인이 직접 정한 규칙이고
`config.yml`로 관리된다. 코드에 수치를 하드코딩하지 말고, 기존 수치의 **의미**(아래 도메인
의미론)를 임의로 바꾸지 말 것. 새 수치가 필요하면 `PluginConfig` 프로퍼티 +
`resources/config.yml` 양쪽에 추가하고 **두 곳의 기본값을 일치**시킨다.

## 도메인 의미론 (주석에만 있는 의도들)

- **death.natural-drop-chance**: 자연사 시 각 아이템 스택이 *손실(바닥 드롭)*될 확률이다.
  유지 확률이 아니다. 플레이어 킬 또는 컴뱃 중 사망은 바닐라 전체 드롭(drops를 건드리지 않음).
- **컴뱃 태깅**: 최초 발동은 플레이어(또는 플레이어가 쏜 발사체)에 의한 피해뿐.
  이미 태깅된 상태에서는 낙하·용암 등 **모든** 피해가 타이머를 갱신한다
  (`CombatListener`의 두 핸들러가 이 비대칭을 구현). 가해자·피해자 양쪽 다 태깅된다.
- **컴뱃로그**: 컴뱃 중 접속 종료 = 전체 인벤토리 드롭 + `health = 0` 사망 처리.
- **보더**: config의 `radius`는 반지름, Bukkit `WorldBorder.size`는 지름 — 적용 시 ×2.
- **이름표**: 메인 스코어보드 팀 prefix 방식(패킷 라이브러리 없음). 팀명 16자 제한 때문에
  `"oms_" + uuid 앞 12자`. 학생 계정(`isStudent`)만 실명 표시.
- **첫 스폰**: PDC 마커(`first-spawn-done`)로 최초 접속 판별, `PlayerJoinEvent`(메인 스레드)에서
  텔레포트. Paper의 비동기 스폰 이벤트는 청크 접근이 안 돼서 일부러 안 쓴다(주석 참고).
- **학번 포맷**: `/student-info`의 학번은 `학년(1)+반(1)+번호(2자리 패딩)` = 4자리(예: 2304).

## SmpAuth 연동 코드의 격리 규칙

`nametag/` 패키지는 SmpAuth 클래스(`AuthDataLoadedEvent`, `SmpAuth`)를 직접 참조한다.
SmpAuth는 softdepend라 없을 수 있으므로, 이 패키지의 클래스 **인스턴스화·참조는 반드시
`onEnable()`의 `pm.getPlugin("SmpAuth") != null` 가드 안에서만** 한다. 가드 밖에서 만지면
SmpAuth 부재 시 `NoClassDefFoundError`로 플러그인 전체가 죽는다. 새 SmpAuth 의존 기능도
같은 패턴(전용 패키지 + 가드 안 등록 + nullable 필드)을 따른다.

## 수명주기·검증

- 스케줄러 태스크를 가진 매니저(`DragonManager`, `CombatDisplay`)는 `start()`/`stop()`을
  노출하고 `onDisable()`에서 반드시 해제. `NametagManager`도 `clearAll()`로 팀을 정리한다.
- 상태 맵은 `ConcurrentHashMap` + 지연 만료(조회 시 정리) 패턴(`CombatManager` 참고).
- 검증은 서버 실행으로: 단독 기능은 `:smp-server:runServer`(SmpAuth 없으면 이름표 기능은
  자동 스킵), 인증 연동 기능은 스크래치 디렉토리에서 `smp.sh` 풀스택. 유닛테스트는 두지 않는다.
- config.yml 변경은 서버 재시작 필요(리로드 커맨드 없음) — 기능 검증 시 잊지 말 것.
