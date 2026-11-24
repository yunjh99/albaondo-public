# 알바온도 | 후기 API

알바온도는 아르바이트 근무 경험을 후기로 남기고 다른 사람의 후기를 살펴보는 서비스입니다. 원본은 모바일 앱과 웹 클라이언트, Spring 백엔드로 구성됩니다. 이 공개본에는 제목·본문·근무 연도로 구성한 후기의 작성, 수정, 삭제, 목록 조회와 두 종류의 후기 반응에 사용되는 엔티티를 담았습니다.

## API

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| POST | `/api/reviews` | 후기 작성 성공 시 본문 없이 `201 Created` 반환 |
| GET | `/api/reviews` | 공개 후기 목록 조회 |
| GET | `/api/reviews/mine` | 로그인한 회원이 작성한 공개 후기 목록 조회 |
| PUT | `/api/reviews/{reviewId}` | 작성자가 제목·본문·근무 연도를 수정 |
| DELETE | `/api/reviews/{reviewId}` | 작성자가 후기를 숨김 처리 |

**등록 재시도.** 클라이언트는 새 후기 제출마다 UUID를 정해 `Idempotency-Key` 헤더에 넣습니다. 응답을 받지 못하면 처음 전송한 키와 본문으로 재시도합니다. 같은 회원의 같은 키·내용에는 첫 요청과 동일하게 빈 본문의 `201 Created`를 반환합니다. 같은 키로 제목·본문·근무 연도를 바꾸면 `409 IDEMPOTENCY_CONFLICT`, 헤더가 빠지거나 UUID로 변환할 수 없으면 `400`을 반환합니다.

서버는 회원 행을 잠근 후 요청 키를 조회하고, 최초 입력의 해시를 후기와 함께 한 트랜잭션에서 저장합니다. DB에는 작성자와 키의 조합에 유일 제약을 둡니다. 이후 후기를 수정하거나 숨겨도 최초 입력의 해시는 유지하므로 지연된 재시도가 내용을 되돌리거나 새 후기를 만들지 않습니다.

두 목록은 모두 후기 ID 내림차순으로 정렬합니다. `page`는 0부터 시작하고 `size`는 기본 20, 최대 50입니다. 클라이언트는 각 항목의 제목·본문·근무 연도 전체를 펼쳐 볼 수 있습니다. `mine`은 로그인한 회원의 후기인지 표시하고, `hasNext`는 다음 페이지가 있는지 알려줍니다.

```json
{
  "items": [
    {
      "reviewId": 42,
      "title": "카페 주말 근무 후기",
      "content": "오픈 준비와 주문 응대를 맡았습니다.",
      "workYear": 2025,
      "likeCount": 0,
      "mine": true
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

## 코드에서 볼 설계 결정

- **작성자 ID는 인증 정보에서 얻습니다.** 컨트롤러는 JWT 인증으로 확인된 회원 ID를 서비스에 전달합니다. 작성과 수정의 입력에는 제목·본문·근무 연도만 받습니다. 목록은 작성자 정보 대신 `mine`을 반환합니다. [인증 필터](src/main/java/com/example/albaondo_public/global/filter/JwtAuthenticationFilter.java) · [후기 컨트롤러](src/main/java/com/example/albaondo_public/review/controller/ReviewController.java)
- **수정과 삭제는 작성자 권한과 후기 상태를 확인한 뒤 반영합니다.** 두 요청 모두 회원 행을 먼저 잠근 뒤 후기 행을 잠가 상태 변경을 순서대로 처리합니다. 삭제한 후기는 `HIDDEN` 상태로 남기고 두 목록에서 제외합니다. 같은 삭제 요청을 반복해도 성공하며, 숨겨진 후기는 수정으로 다시 공개할 수 없습니다. [후기 서비스](src/main/java/com/example/albaondo_public/review/service/ReviewService.java) · [후기 엔티티](src/main/java/com/example/albaondo_public/review/domain/Review.java)
- **목록은 필요한 페이지 분량만 조회합니다.** 저장소가 전체 목록에는 공개 상태 조건을, 내 목록에는 공개 상태와 작성자 조건을 함께 적용합니다. `Slice`로 다음 페이지 유무를 확인하고 응답 DTO로 필요한 데이터만 전달합니다. [후기 저장소](src/main/java/com/example/albaondo_public/review/repository/ReviewRepository.java) · [목록 응답 DTO](src/main/java/com/example/albaondo_public/review/dto/ReviewListResponse.java)

## 기술 스택과 공개 범위

Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security, JJWT, Spring Data JPA, Hibernate를 사용합니다. 원본의 데이터베이스는 MySQL입니다. 공개본에는 운영 설정, 토큰 발급과 키 주입, 배포 파일, 사용자 데이터를 포함하지 않습니다.

## 로컬에서 확인하기

JDK 21에서 `./gradlew test`를 실행합니다. Windows에서는 `.\gradlew.bat test`를 사용합니다. 외부 계정이나 운영 DB 연결 없이 검증할 수 있습니다.

- [후기 API 테스트](src/test/java/com/example/albaondo_public/ReviewApiTest.java)는 인증된 사용자를 설정한 뒤 MockMvc와 H2로 저장부터 응답까지 확인합니다. 두 회원의 전체·내 목록이 어떻게 달라지는지, 다른 회원의 수정·삭제가 거절되는지, 삭제된 후기가 두 목록에서 사라지는지를 볼 수 있습니다.
- [후기 반응 도메인 테스트](src/test/java/com/example/albaondo_public/ReviewReactionTest.java)는 작성자와 요청자가 서로 다른 자바 객체여도 회원 ID가 같으면 두 반응을 모두 거절하는지 확인합니다.
