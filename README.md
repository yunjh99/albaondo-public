# 알바온도

**알바온도는 아르바이트 근로자를 위한 애플리케이션입니다.** 근무 일정과 급여를 관리하고, 근무 후기로 일터를 비교하고, 커뮤니티에서 다른 근로자와 경험을 나눌 수 있습니다.

[구글 플레이 바로가기](https://play.google.com/store/apps/details?id=com.albaondo.app)

| 시작 화면 | 알바 후기 | 월 예상 급여 | 근무 캘린더 |
| :---: | :---: | :---: | :---: |
| [![회원가입과 로그인](docs/images/welcome.png)](docs/images/welcome.png) | [![업직종별 알바 후기 목록](docs/images/reviews.png)](docs/images/reviews.png) | [![월 예상 급여와 근무지별 금액](docs/images/pay-summary.png)](docs/images/pay-summary.png) | [![날짜별 근무 시간과 예상 급여](docs/images/work-calendar.png)](docs/images/work-calendar.png) |

이 저장소는 운영 백엔드에서 후기와 인증 흐름을 추려낸 Java 포트폴리오입니다. 제목·본문·근무 연도만으로 후기 등록 재시도, 작성자 권한 검사, 토큰 재발급을 테스트할 수 있습니다.

---

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | ![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.2-6DB33F?style=flat-square) ![Spring MVC](https://img.shields.io/badge/Spring_MVC-6DB33F?style=flat-square) ![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square) ![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=flat-square) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square) |
| Security | ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square) ![JJWT](https://img.shields.io/badge/JJWT-0.13.0-000000?style=flat-square) |
| Database | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square) |
| Mobile | ![React Native](https://img.shields.io/badge/React_Native-61DAFB?style=flat-square) ![Expo](https://img.shields.io/badge/Expo-000020?style=flat-square) |
| Web | ![React](https://img.shields.io/badge/React-61DAFB?style=flat-square) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square) |
| Test | ![JUnit](https://img.shields.io/badge/JUnit-25A162?style=flat-square) ![MockMvc](https://img.shields.io/badge/MockMvc-6DB33F?style=flat-square) ![Spring Boot Test](https://img.shields.io/badge/Spring_Boot_Test-6DB33F?style=flat-square) ![H2](https://img.shields.io/badge/H2-09476B?style=flat-square) |

운영 앱과 웹은 Spring 백엔드와 MySQL을 사용합니다. 공개 저장소에서는 H2로 후기 API를 테스트합니다.
