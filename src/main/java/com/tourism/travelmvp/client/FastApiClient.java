package com.tourism.travelmvp.client;

import com.tourism.travelmvp.dto.IntegrationDtos;
import com.tourism.travelmvp.enums.PaymentType;
import com.tourism.travelmvp.service.TripMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient fastApiWebClient;

    public IntegrationDtos.ReceiptOcrResponse analyzeReceipt(Path filePath) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(filePath));
            Map<?, ?> response = fastApiWebClient.post()
                    .uri("/api/v1/documents/ocr/receipt")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<?, ?> data = nestedMap(response, "data");
            return new IntegrationDtos.ReceiptOcrResponse(
                    PaymentType.valueOf(stringValue(data, "payment_type", "UNKNOWN").toUpperCase()),
                    stringValue(data, "payment_datetime", ""),
                    stringValue(data, "raw_text", ""),
                    TripMapper.stringList(data.get("candidates")));
        } catch (Exception exception) {
            return new IntegrationDtos.ReceiptOcrResponse(
                    PaymentType.UNKNOWN,
                    "",
                    "",
                    List.of("TODO: FastAPI unavailable, fallback mock used"));
        }
    }

    public IntegrationDtos.ReceiptAmountResponse extractReceiptAmount(Path filePath) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(filePath));
            Map<?, ?> response = fastApiWebClient.post()
                    .uri("/api/v1/documents/ocr/receipt-amount")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<?, ?> data = nestedMap(response, "data");
            Number amount = (Number) data.get("amount");
            return new IntegrationDtos.ReceiptAmountResponse(
                    amount == null ? null : amount.intValue(),
                    stringValue(data, "currency", "KRW"),
                    stringValue(data, "payment_datetime", ""),
                    stringValue(data, "raw_text", ""));
        } catch (Exception exception) {
            return new IntegrationDtos.ReceiptAmountResponse(null, "KRW", "", "TODO: mock amount extraction");
        }
    }

    public IntegrationDtos.LodgingExtractResponse extractLodgingInfo(Path filePath) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(filePath));
            Map<?, ?> response = fastApiWebClient.post()
                    .uri("/api/v1/documents/ocr/lodging")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<?, ?> data = nestedMap(response, "data");
            return new IntegrationDtos.LodgingExtractResponse(
                    TripMapper.nullableString(data.get("lodging_name")),
                    TripMapper.nullableString(data.get("representative_name")),
                    TripMapper.nullableString(data.get("phone_number")),
                    TripMapper.nullableString(data.get("address")),
                    TripMapper.stringList(data.get("warnings")));
        } catch (Exception exception) {
            return new IntegrationDtos.LodgingExtractResponse(
                    null,
                    null,
                    null,
                    null,
                    List.of("TODO: mock lodging extraction"));
        }
    }

    public IntegrationDtos.AuthPhotoReviewResponse analyzeAuthPhoto(
            Path filePath,
            int requiredPeopleCount
    ) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(filePath));
            bodyBuilder.part("required_people_count", Integer.toString(requiredPeopleCount));
            Map<?, ?> response = fastApiWebClient.post()
                    .uri("/api/v1/documents/photos/auth-review")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<?, ?> data = nestedMap(response, "data");
            return new IntegrationDtos.AuthPhotoReviewResponse(
                    booleanValue(data.get("approved")),
                    intValue(data.get("detected_people_count"), 0),
                    intValue(data.get("required_people_count"), requiredPeopleCount),
                    booleanValue(data.get("faces_clear")),
                    booleanValue(data.get("background_visible")),
                    stringValue(data, "reason", "")
            );
        } catch (Exception exception) {
            return new IntegrationDtos.AuthPhotoReviewResponse(
                    false,
                    0,
                    requiredPeopleCount,
                    false,
                    false,
                    "인증사진 AI 판정에 실패했습니다. 다시 시도해 주세요."
            );
        }
    }

    public byte[] mergePdfs(List<Path> filePaths) throws IOException {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            for (Path filePath : filePaths) {
                bodyBuilder.part("files", new FileSystemResource(filePath));
            }
            return fastApiWebClient.post()
                    .uri("/api/v1/documents/pdf/merge")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception exception) {
            throw new IOException("Failed to merge PDFs through FastAPI", exception);
        }
    }

    public byte[] renderLodgingFormPdf(Map<String, Object> requestBody) throws IOException {
        try {
            return fastApiWebClient.post()
                    .uri("/api/v1/documents/pdf/lodging-form")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception exception) {
            throw new IOException("Failed to render lodging form PDF through FastAPI", exception);
        }
    }

    public IntegrationDtos.TemplateAnalysisResponse analyzeLodgingTemplate(
            Map<String, Object> requestBody
    ) throws IOException {
        try {
            Map<?, ?> response = fastApiWebClient.post()
                    .uri("/api/v1/documents/templates/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<?, ?> data = nestedMap(response, "data");
            List<Map<String, Object>> fieldMaps = mapList(data.get("fields"));
            List<IntegrationDtos.TemplateAnalysisField> fields = fieldMaps.stream()
                    .map(field -> new IntegrationDtos.TemplateAnalysisField(
                            stringValue(field, "key", ""),
                            stringValue(field, "type", "text"),
                            numberValue(field.get("x")),
                            numberValue(field.get("y")),
                            numberValue(field.get("width")),
                            numberValue(field.get("height")),
                            booleanValue(field.get("editable")),
                            booleanValue(field.get("multiline"))
                    ))
                    .toList();
            return new IntegrationDtos.TemplateAnalysisResponse(
                    fields,
                    TripMapper.stringList(data.get("warnings")),
                    booleanValue(data.get("used_ai"))
            );
        } catch (Exception exception) {
            throw new IOException("Failed to analyze lodging template through FastAPI", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> nestedMap(Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> nested) {
            return nested;
        }
        return Map.of();
    }

    private String stringValue(Map<?, ?> source, String key, String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object source) {
        if (source == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) source;
    }
}
