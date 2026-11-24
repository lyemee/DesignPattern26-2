# 기여 규칙

이 레포에서 작업하는 모든 팀원과 AI 에이전트(Claude Code, Cursor, Codex, Copilot 등)는 이 규칙을 따른다.

## 프로젝트
- Holub Life Game에 설계 패턴을 적용해 기능을 확장하고 설계를 개선하는 팀 프로젝트 (Java)
- 평가자가 **커밋 히스토리로 개발 과정과 팀원별 기여를 확인한다.** 히스토리가 곧 산출물이다.

## 빌드와 테스트
```
mvn compile                                   # 컴파일
mvn test                                      # JUnit 5 테스트
java -cp target/classes com.holub.life.Life   # 실행
```
- JDK 21, Maven, JUnit 5
- 소스는 `src/`, 테스트는 `test/` (Maven 기본 경로 `src/main/java`가 아님)
- 테스트는 소스와 같은 패키지 경로에 둔다. 예: `src/com/holub/life/game/GameSession.java` → `test/com/holub/life/game/GameSessionTest.java`

## 커밋 규칙

### 메시지 형식
```
<타입>: <한글 요약>
```
| 타입 | 언제 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로, 구조만 개선 (설계 패턴 적용 포함) |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 |
| `chore` | 빌드 설정, 레포 설정 등 |

- 요약은 한글, 명사형 또는 "~추가/~변경/~수정"으로 끝낸다. 마침표 없음.
- 좋은 예: `refactor: GenerationManager가 Plant 인터페이스를 쓰도록 변경`
- 나쁜 예: `update`, `수정`, `식물 기능 전부 구현 + 테스트 + 버그 수정`
- 왜 바꿨는지 설명이 필요하면 빈 줄 하나 뒤에 본문으로 쓴다.

### 커밋 단위
- **한 커밋 = 한 가지 의미 단위.** 작업이 끝날 때 한 번에 몰아서 커밋하지 않는다.
- 설계 패턴을 도입할 때는 단계가 히스토리에 보이도록 나눈다. 예:
  1. `feat: PlantFactory 인터페이스 추가`
  2. `feat: 식물 종류별 PlantFactory 구현`
  3. `refactor: Resident가 PlantFactory로 식물을 생성하도록 변경`
  4. `test: PlantFactory 생성 종류 테스트`
- 각 커밋 시점에 `mvn compile`이 성공해야 한다. 가능하면 `mvn test`도 통과시킨다.
- 관계없는 변경(포맷 정리, 다른 파일 리팩터링)을 끼워 넣지 않는다.
- **원본 Holub 코드를 전체 재포맷하거나 import 정리만 하는 변경을 하지 않는다.** diff가 커져서 실제 변경이 묻힌다.

### 작성자
- 커밋은 **작업한 팀원 본인의 git 계정**으로 한다. 에이전트가 커밋 명령을 실행해도 작성자는 사람이다.
- 다른 팀원의 작업을 대신 커밋하지 않는다.
- 커밋 전에 `git config user.name`이 본인인지 확인한다.
- **AI 공동작성자 표기를 넣지 않는다.** 커밋 메시지의 `Co-Authored-By: Claude ...` 같은 줄과 PR 본문의 `Generated with ...` 같은 문구를 모두 뺀다. 에이전트가 기본으로 붙이는 경우에도 지운다.

## 이슈, 브랜치, PR

### 이름 규칙 한눈에 보기
| 대상 | 형식 | 예 |
|---|---|---|
| 이슈 제목 | `[타입] 한글 설명` | `[Feat] 구체 식물 5종과 종류별 PlantFactory 구현` |
| 브랜치 | `타입/이슈번호-영문-설명` | `feat/4-plant-types` |
| 커밋 | `타입: 한글 요약` | `feat: Sunflower 구현` |
| PR 제목 | `타입: 한글 요약` | `feat: 구체 식물 5종과 종류별 PlantFactory 추가` |

- 이슈 제목의 타입은 첫 글자만 대문자: `[Feat]` `[Refactor]` `[Fix]` `[Test]` `[Docs]` `[Chore]`
- 커밋·PR 제목의 타입은 소문자: `feat` `refactor` `fix` `test` `docs` `chore`

### 이슈
- 모든 작업은 이슈에서 시작한다. 이슈가 없으면 먼저 만든다.
- `.github/ISSUE_TEMPLATE/`의 템플릿을 쓴다 (기능 추가 / 설계 개선 / 버그 / 기타).
- `gh`로 만들 때는 해당 템플릿의 섹션 구성을 그대로 따른다.

### 브랜치
```
<타입>/<이슈번호>-<짧은-영문-설명>
```
예: `feat/4-plant-types`, `refactor/7-generation-manager`
- **main에 직접 커밋하거나 푸시하지 않는다.**

### PR
- 본문은 `.github/PULL_REQUEST_TEMPLATE.md`를 따른다. `gh pr create`로 만들 때도 이 파일의 섹션을 모두 채운다.
- 해당 없는 섹션은 지우지 말고 "없음"이라고 쓴다.
- "적용한 설계 패턴/원칙과 이유"와 "고려한 대안"은 최종 보고서에 그대로 쓰이므로, 실제로 고민한 내용만 구체적으로 쓴다. 일반적인 패턴 설명을 늘어놓지 않는다.
- 본문 끝에 `Closes #이슈번호`를 넣는다.
- 리뷰 승인 1명 이상 받은 뒤 머지한다.
- **머지는 "Create a merge commit"으로 한다.** squash·rebase 머지는 커밋 이력을 뭉개므로 쓰지 않는다.

## 금지 사항
- `git push --force` (본인 브랜치라도 리뷰 시작 후에는 금지)
- main에 직접 커밋·푸시
- 다른 팀원 브랜치에 허락 없이 커밋
- 테스트를 통과시키려고 기존 테스트를 지우거나 약화시키기

## 코드 작성 원칙
- **기존 Life Game 기능은 그대로 동작해야 한다.** 원본 동작을 바꾸는 변경은 PR 본문에 명시한다.
- 모델(게임 로직)은 UI 없이 JUnit으로 테스트할 수 있게 분리한다. 모델 클래스가 Swing에 의존하지 않게 한다.
- 새 기능에는 테스트를 함께 추가한다.
