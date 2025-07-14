package com.coraybennett.spillway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for video decryption requests.
 * Used when a client needs to provide a decryption key for encrypted content.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDecryptionRequest {
    private String videoId;
    private String decryptionKey; // Base64 encoded symmetric key
    private String segmentName; // Optional: specific segment to decrypt
}