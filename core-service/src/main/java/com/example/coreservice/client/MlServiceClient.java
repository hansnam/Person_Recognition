package com.example.coreservice.client;

import com.example.coreservice.dto.MlMatchResponse;
import com.example.coreservice.dto.MlRegisterResponse;
import com.example.coreservice.dto.MlVideoMatchResponse;
import com.example.coreservice.exception.MlServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class MlServiceClient {

    private final RestClient restClient;
    private final RestClient videoRestClient;

    public MlServiceClient(
            @Value("${ml-service.base-url:http://localhost:8000}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        JdkClientHttpRequestFactory defaultFactory = new JdkClientHttpRequestFactory(httpClient);
        defaultFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(defaultFactory)
                .build();

        JdkClientHttpRequestFactory videoFactory = new JdkClientHttpRequestFactory(httpClient);
        videoFactory.setReadTimeout(Duration.ofMinutes(5));

        this.videoRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(videoFactory)
                .build();
    }

    /**
     * Gọi POST /ml/register-face để đăng ký khuôn mặt vào FAISS
     */
    public MlRegisterResponse registerFace(MultipartFile file, Long vectorId) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "face.jpg";
                }
            };
            body.add("file", resource);
            if (vectorId != null) {
                body.add("vector_id", vectorId);
            }

            return restClient.post()
                    .uri("/ml/register-face")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .body(MlRegisterResponse.class);

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu ảnh khi gửi tới ML Service: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service (FastAPI): " + e.getMessage(), e);
        }
    }

    /**
     * Gọi POST /ml/detect-and-match để so khớp khuôn mặt với cơ sở dữ liệu FAISS
     */
    public MlMatchResponse detectAndMatch(MultipartFile file, Double threshold) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "detect.jpg";
                }
            };
            body.add("file", resource);

            double finalThreshold = threshold != null ? threshold : 0.45;

            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ml/detect-and-match")
                            .queryParam("threshold", finalThreshold)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .body(MlMatchResponse.class);

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu ảnh khi gửi tới ML Service: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service (FastAPI): " + e.getMessage(), e);
        }
    }

    /**
     * Gọi DELETE /ml/face/{vectorId} để xoá vector khỏi FAISS
     */
    public void deleteFace(Long vectorId) {
        if (vectorId == null) return;
        try {
            restClient.delete()
                    .uri("/ml/face/{vectorId}", vectorId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi xoá vector ở ML Service (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .toBodilessEntity();
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service để xoá vector: " + e.getMessage(), e);
        }
    }

    /**
     * Gọi POST /ml/detect-and-match-video để nhận diện khuôn mặt từ file video
     */
    public MlVideoMatchResponse detectAndMatchVideo(MultipartFile file, Double threshold, Double frameIntervalSeconds) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.mp4";
                }
            };
            body.add("file", resource);

            double finalThreshold = threshold != null ? threshold : 0.45;
            double finalInterval = frameIntervalSeconds != null ? frameIntervalSeconds : 1.0;

            return videoRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ml/detect-and-match-video")
                            .queryParam("threshold", finalThreshold)
                            .queryParam("frame_interval_seconds", finalInterval)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service khi phân tích video (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .body(MlVideoMatchResponse.class);

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu video khi gửi tới ML Service: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service (FastAPI) để xử lý video: " + e.getMessage(), e);
        }
    }

    /**
     * Gọi POST /ml/register-body để đăng ký đặc trưng cơ thể vào Body FAISS
     */
    public void registerBody(MultipartFile file, Long vectorId) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "body.jpg";
                }
            };
            body.add("file", resource);
            if (vectorId != null) {
                body.add("vector_id", vectorId);
            }

            restClient.post()
                    .uri("/ml/register-body")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service khi đăng ký body (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .toBodilessEntity();

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu ảnh body khi gửi tới ML Service: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service khi đăng ký body: " + e.getMessage(), e);
        }
    }

    /**
     * Gọi DELETE /ml/body/{vector_id} để xoá vector body khỏi Body FAISS
     */
    public void deleteBody(Long vectorId) {
        try {
            restClient.delete()
                    .uri("/ml/body/{vector_id}", vectorId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        // Bỏ qua lỗi 404 nếu hồ sơ chưa đăng ký body
                        if (resp.getStatusCode().value() != 404) {
                            String errorBody = new String(resp.getBody().readAllBytes());
                            throw new MlServiceException("Lỗi khi xoá vector body (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                        }
                    })
                    .toBodilessEntity();
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            // Không chặn tiến trình nếu xoá body thất bại
            System.err.println("[MlServiceClient] Không thể xoá vector body id=" + vectorId + ": " + e.getMessage());
        }
    }

    /**
     * Gọi POST /ml/detect-and-match-fusion để nhận diện Fusion (Face + Body Re-ID)
     */
    public com.example.coreservice.dto.MlFusionResponse detectAndMatchFusion(MultipartFile file, Double faceThreshold, Double bodyThreshold) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "fusion_detect.jpg";
                }
            };
            body.add("file", resource);

            double finalFaceThresh = faceThreshold != null ? faceThreshold : 0.45;
            double finalBodyThresh = bodyThreshold != null ? bodyThreshold : 0.65;

            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ml/detect-and-match-fusion")
                            .queryParam("face_threshold", finalFaceThresh)
                            .queryParam("body_threshold", finalBodyThresh)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service (Fusion) (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .body(com.example.coreservice.dto.MlFusionResponse.class);

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu ảnh khi gửi tới ML Service Fusion: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service (Fusion): " + e.getMessage(), e);
        }
    }

    /**
     * Gọi POST /ml/detect-and-match-fusion-video để nhận diện Fusion từ file video
     */
    public com.example.coreservice.dto.MlFusionVideoResponse detectAndMatchFusionVideo(MultipartFile file, Double faceThreshold, Double bodyThreshold, Double frameIntervalSeconds) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "fusion_video.mp4";
                }
            };
            body.add("file", resource);

            double finalFaceThresh = faceThreshold != null ? faceThreshold : 0.45;
            double finalBodyThresh = bodyThreshold != null ? bodyThreshold : 0.65;
            double finalInterval = frameIntervalSeconds != null ? frameIntervalSeconds : 1.0;

            return videoRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ml/detect-and-match-fusion-video")
                            .queryParam("face_threshold", finalFaceThresh)
                            .queryParam("body_threshold", finalBodyThresh)
                            .queryParam("frame_interval_seconds", finalInterval)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes());
                        throw new MlServiceException("Lỗi từ ML Service khi phân tích fusion video (" + resp.getStatusCode() + "): " + errorBody, resp.getStatusCode().value());
                    })
                    .body(com.example.coreservice.dto.MlFusionVideoResponse.class);

        } catch (IOException e) {
            throw new MlServiceException("Lỗi đọc dữ liệu video khi gửi tới ML Service Fusion: " + e.getMessage(), e);
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new MlServiceException("Không thể kết nối tới ML Service Fusion Video: " + e.getMessage(), e);
        }
    }
}

