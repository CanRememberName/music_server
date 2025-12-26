package com.music.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Music implements Serializable {
    private String id;
    private String title;
    private String artist;
    private String album;
    
    // Relative path or URL suffix
    private String coverPath; 
    private String filePath;
    
    private Long duration; // Seconds
    private String format; // mp3, flac
    private Long createTime;
    
    // Transient fields for API response (full URLs)
    private String cover_url;
    private String audio_url;
}
