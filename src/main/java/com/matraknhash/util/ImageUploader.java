package com.matraknhash.util;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Utility for uploading files to ImgBB cloud storage anonymously.
 * Uses Java 11 HttpClient and URLEncoder to convert image to Base64 and POST it.
 */
public final class ImageUploader {

    // A free public developer testing API key for ImgBB
    private static final String API_KEY = "29ea5b8396ec2fb5ea4a6c4293f0b2f1";

    private ImageUploader() {}

    /**
     * Uploads a local file to ImgBB and returns its secure direct HTTPS link.
     */
    public static String upload(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(bytes);

        // ImgBB API supports sending image parameter as url-encoded base64 string
        String body = "key=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8)
                + "&image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.imgbb.com/1/upload"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Upload failed (status " + response.statusCode() + "): " + response.body());
        }

        // Naive JSON parsing to retrieve the "url" from:
        // {"data":{"id":"...","url":"https:\/\/i.ibb.co\/...","display_url":"..."},"success":true,"status":200}
        String res = response.body();
        int urlIdx = res.indexOf("\"url\":\"");
        if (urlIdx == -1) {
            throw new RuntimeException("Invalid response format from ImgBB API");
        }
        int start = urlIdx + 7;
        int end = res.indexOf("\"", start);
        String url = res.substring(start, end);

        // Unescape escaped JSON slashes
        return url.replace("\\/", "/");
    }
}
