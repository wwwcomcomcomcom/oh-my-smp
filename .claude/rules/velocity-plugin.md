---
paths:
  - "velocity-plugin/**"
---

# velocity-plugin — 프록시 인증 게이트 (Java/Velocity)

플러그인 클래스는 `SmpAuthVelocity` 하나. `@Plugin` 어노테이션 메타데이터는
`annotationProcessor(velocity-api)`가 빌드 시 생성하므로 velocity-plugin.json을 손으로 만들지 않는다.
생성자 주입(Guice `@Inject`) + `@Subscribe` 이벤트 패턴을 따른다.

## AuthState가 런타임 소스 오브 트루스

- `AuthState`(인메모리 `UUID → StudentData`)가 프록시 전역 인증 상태다. **접속 중인 플레이어만**
  들어 있다: `PostLoginEvent`에서 auth-server REST로 채우고 `DisconnectEvent`에서 비운다.
- 영속 데이터는 auth-server의 SQLite에 있고, 이 플러그인은 `AuthServerClient.fetchLink()`로만
  읽는다. `AuthServerClient`는 실패 시 예외 대신 null을 완성값으로 주므로 콜백에서 null 체크 필수.

## 게이팅 로직 (`VelocityConfig.isGated`)

- 로비 서버(`lobbyServerName`)는 항상 통과.
- `gatedServers`가 **비어 있으면 "로비 제외 전부 게이트"**, 채워져 있으면 그 목록만 게이트.
  기본값(빈 목록)이 안전한 쪽이라는 걸 유지할 것.
- 차단은 `ServerPreConnectEvent`에서 `denied()` — 이미 접속한 플레이어를 쫓아내지는 않는다.

## 플러그인 메시지 (`smpauth:data`) 보안 불변식

`onPluginMessage`에서 반드시 지켜야 하는 두 가지:

1. `event.getSource() instanceof ServerConnection`인 메시지만 신뢰한다.
   클라이언트가 위조한 메시지를 걸러내는 유일한 장치다.
2. 채널이 일치하면 무조건 `ForwardResult.handled()`로 전달을 끊는다.
   인증 데이터가 클라이언트로 새 나가면 안 된다.

새 `MessageType`을 처리할 때도 이 두 검사 **다음에** 분기한다. `AUTH_RESPONSE`는
프록시→백엔드 전용이므로 인바운드로 오면 무시(현행 default 분기 유지).

## 빌드

- 배포 jar는 classifier 없는 **shadow jar**(`velocity-plugin.jar`), 일반 jar는 `-thin`.
- Gson은 Velocity 런타임이 제공하므로 shadowJar에서 exclude — 의존성을 추가하면
  Velocity가 제공하는지 확인하고 shade 여부를 결정한다.
- 설정은 `<dataDir>/config.properties`, 없으면 기본값으로 자동 생성. 키를 추가하면
  로드 분기와 기본값 생성 분기 양쪽에 넣는다.
