---
paths:
  - "common/**"
---

# common — 와이어 계약 모듈

REST(auth-server API)와 플러그인 메시징(`smpauth:data`) 양쪽에서 쓰는 DTO들이다.
전 스택(auth-server·velocity·lobby·content-lib·smp-server)이 **항상 한 번에 배포**되는
전제이므로 구버전 호환을 위한 필드 유지는 필요 없다 — 자유롭게 추가/변경/삭제해도 된다.

## 규칙

- 직렬화는 반드시 공유 인스턴스 `Json.GSON` 사용. 모듈별로 Gson 설정이 갈리면 와이어가 깨진다.
- **DataGSM SDK 타입을 이 모듈로 가져오지 말 것.** enum은 `name()` 문자열로 평탄화한다
  (`StudentData.role`, `sex`, `major` 등). SDK → `StudentData` 변환은 auth-server의
  `StudentMapper`가 담당한다.
- 의존성은 `api(gson)` 하나뿐이고, 그래야 한다. 이 모듈은 Ktor·Velocity·Minestom·Paper
  네 런타임 전부에 로드되므로 새 의존성 추가 금지.
- `StudentData`의 학생 전용 필드는 `isStudent == false`일 때 전부 null — 소비 코드는 항상 null 안전하게.

## StudentData 필드 추가 체크리스트

1. `StudentData` 레코드에 필드 추가 (`ClubInfoDto`처럼 중첩 DTO가 필요하면 여기 같이).
2. auth-server `StudentMapper.map()`에 SDK → DTO 매핑 추가.
3. 소비처(smp-server 등)에서 사용. SQLite에는 `student_json`으로 전체 스냅샷이 저장되므로
   `LinkRepository` 스키마 변경은 조회용 컬럼이 필요할 때만.

## 플러그인 메시지 방향 (MessageType)

- `AUTH_REQUEST`: 백엔드(Paper) → Velocity, "이 플레이어 인증 데이터 줘".
- `AUTH_RESPONSE`: Velocity → 백엔드. `linked=false`면 `student=null`.
- `LINK_UPDATED`: 로비 → Velocity, "/verify 성공했으니 auth-server에서 다시 읽어라".

타입을 추가하면 velocity-plugin의 `onPluginMessage` switch와 송신 측(content-lib 또는 lobby)을
같이 수정해야 한다.
