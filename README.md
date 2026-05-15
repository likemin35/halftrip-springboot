# HalfTrip Spring API

HalfTrip 프로토타입의 메인 비즈니스 API 레포입니다.  
Flutter 클라이언트가 쓰는 여행/지역/가맹점/영수증/숙박확인서/정산 API를 담당합니다.

## 관련 레포

- Flutter 클라이언트: [halftrip](https://github.com/likemin35/halftrip)
- FastAPI OCR/PDF: [halftrip-fastapi](https://github.com/likemin35/halftrip-fastapi)

## 이 레포에서 다루는 범위

- 사용자/로그인/여행 생성 및 조회
- 지역 목록, 지정관광지, 지역화폐 가맹점, 온라인몰 데이터 제공
- 플래너 장소 저장/정렬/삭제
- 업로드 파일 저장, 영수증 분석 요청, 숙박확인서 데이터 저장
- 정산 요약/정산 신청
- FastAPI OCR/PDF 서버와의 중간 연동
- Flyway 기반 MySQL 스키마 및 데이터 마이그레이션

## 주요 폴더

```text
src/main/java/com/tourism/travelmvp/
  client/        FastAPI 호출 클라이언트
  config/        WebClient, CORS, 설정
  controller/    REST API 엔드포인트
  dto/           요청/응답 DTO
  entity/        JPA 엔티티
  enums/         공통 enum
  exception/     예외 처리
  repository/    JPA repository
  service/       핵심 비즈니스 로직

src/main/resources/
  db/migration/  Flyway 마이그레이션
  merchant-seeds/ 지역 가맹점 CSV seed
  lodging-form-templates/ 숙박확인서 관련 템플릿/좌표 정의
```

## 파일/영역별 역할

### `controller`

- `AuthController`
  - 로그인/회원가입/mock 로그인
- `UserController`
  - 사용자 정보, 알림 설정, 관심 지역
- `RegionController`
  - 지역 목록, 지역 상세, 지정관광지/가맹점/온라인몰 조회
- `TripController`
  - 여행 상세, 업로드, 영수증 분석, 숙박확인서, 정산, PDF 통합

### `service`

- `TripService`
  - 가장 핵심적인 비즈니스 로직
  - 여행 생성/수정, 플래너, 파일 업로드, 영수증 심사, 숙박확인서 처리 담당
- `RegionService`
  - 지역 상세 조합 응답 구성
- `UserService`
  - 사용자/로그인 관련 처리
- `TripMapper`
  - 엔티티 -> DTO 매핑
- `StorageService`
  - 업로드 파일 저장/삭제/바이너리 읽기

### `client`

- `FastApiClient`
  - 영수증 OCR, 인증사진 AI 판정, 숙박확인서 추출, PDF 병합/렌더링 호출

### `entity`

- `Trip`, `TripPlace`, `UploadedFile`, `Receipt`, `LodgingInfo`
  - 여행과 증빙 도메인 핵심 엔티티
- `Region`, `Place`, `Merchant`, `OnlineMall`
  - 지역 탐색/지도 관련 엔티티

### `db/migration`

- `V1__init_schema.sql`
  - 기본 스키마 생성
- `V30__import_region_merchants.java`
  - 지역화폐 가맹점 대량 적재
- `V31__add_payment_datetime_to_receipts.sql`
  - 영수증 결제일시 저장용 컬럼 추가
- 그 외
  - 숙박확인서 템플릿 보정, 관광지 데이터 보강, 영수증 심사 확장 등

## 실행 방법

```powershell
cd C:\Users\Administrator\Desktop\관광\backend-spring
mvn spring-boot:run
```

또는 현재 프로젝트에서 사용하는 스크립트:

```powershell
cd C:\Users\Administrator\Desktop\관광
powershell -ExecutionPolicy Bypass -File .\scripts\start_spring.ps1
```

## 주요 환경변수

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `FASTAPI_BASE_URL`
- `APP_ALLOWED_ORIGINS`
- `APP_STORAGE_ROOT`

## 협업 시 참고

- Flutter 화면에서 보이는 데이터는 대부분 여기 DTO 구조를 그대로 사용합니다.
- 업로드/OCR/숙박확인서/PDF 기능은 FastAPI와 강하게 연결돼 있으니 함께 수정하는 편이 안전합니다.
- 프로토타입 단계라 규칙이 자주 바뀌므로, 지역별 정산 규칙과 마이그레이션 파일을 같이 보는 것이 좋습니다.

## 함께 보면 좋은 레포

- Flutter 클라이언트: [https://github.com/likemin35/halftrip](https://github.com/likemin35/halftrip)
- FastAPI OCR/PDF: [https://github.com/likemin35/halftrip-fastapi](https://github.com/likemin35/halftrip-fastapi)
