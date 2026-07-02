---
paths:
  - "sample-content-plugin/**"
---

# sample-content-plugin — 참조 예제 (배포 안 함)

content-lib(SmpAuth) API 사용법을 보여주는 **레퍼런스 전용** 모듈이다.
`setup.sh`가 배포하지 않으며, 실서버에 올라가지 않는다.

## 규칙

- 유일한 관리 목표: **content-lib API가 바뀌어도 컴파일이 유지되도록** 따라 고친다.
  기능을 늘리지 말고, `docs/CONTENT-SERVER-GUIDE.md`의 코드 예제와 일치하게 유지한다.
- 콘텐츠 플러그인의 표준 패턴을 시연하는 곳이므로 패턴 자체를 지킬 것:
  - `plugin.yml`에 `depend: [SmpAuth]` (soft가 아닌 hard depend — 예제는 SmpAuth 전제).
  - Gradle에서 `compileOnly(project(":content-lib"))` + `compileOnly(paper-api)`.
    shadow 없이 plain jar — 런타임 클래스는 전부 SmpAuth 플러그인과 Paper가 제공한다.
