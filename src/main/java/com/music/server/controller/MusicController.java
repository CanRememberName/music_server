package com.music.server.controller;

import com.music.server.model.ApiResponse;
import com.music.server.model.Music;
import com.music.server.repository.MusicRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/music")
public class MusicController {

    private final MusicRepository musicRepository;
    
    @Value("${app.music.files-dir}")
    private String filesDir;

    public MusicController(MusicRepository musicRepository) {
        this.musicRepository = musicRepository;
        // Suppress Jaudiotagger logs
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {

        List<Music> allMusic = musicRepository.search(keyword);
        int total = allMusic.size();
        
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        List<Music> pageList;
        if (start >= total) {
            pageList = List.of();
        } else {
            pageList = allMusic.subList(start, end);
        }

        // Decorate URLs
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
        // Since we are behind a proxy or local, we might need full URL.
        // Actually ServletUriComponentsBuilder.fromCurrentContextPath() gives http://localhost:8080
        
        List<Music> resultList = pageList.stream().map(m -> {
            Music dto = new Music();
            // Copy properties (simple clone or use BeanUtils)
            dto.setId(m.getId());
            dto.setTitle(m.getTitle());
            dto.setArtist(m.getArtist());
            dto.setAlbum(m.getAlbum());
            dto.setDuration(m.getDuration());
            dto.setFormat(m.getFormat());
            
            // Generate URLs
            String host = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
            dto.setAudio_url(host + "/music/stream/" + m.getId());
            dto.setCover_url(host + "/music/cover/" + m.getId());
            
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("list", resultList);

        return ApiResponse.success(data);
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "album", required = false) String album) {
        
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        File dir = new File(filesDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (file.isEmpty()) {
            return ApiResponse.error(400, "File is empty");
        }

        try {
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = StringUtils.getFilenameExtension(originalFilename);
            if (extension == null) extension = "mp3";
            
            String uuid = UUID.randomUUID().toString();
            String newFilename = uuid + "." + extension;
            Path targetPath = dir.toPath().resolve(newFilename);
            
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Parse Metadata
            Music music = new Music();
            music.setId(uuid);
            music.setFilePath(targetPath.toAbsolutePath().toString());
            music.setCreateTime(System.currentTimeMillis());
            music.setFormat(extension.toLowerCase());
            
            // Defaults
            music.setTitle(originalFilename);
            music.setArtist("Unknown Artist");
            music.setAlbum("Unknown Album");
            music.setDuration(0L);

            // 1. Try to parse from file first to get duration and defaults
            try {
                AudioFile f = AudioFileIO.read(targetPath.toFile());
                Tag tag = f.getTag();
                if (tag != null) {
                    String parsedTitle = tag.getFirst(FieldKey.TITLE);
                    if (StringUtils.hasText(parsedTitle)) music.setTitle(parsedTitle);
                    
                    String parsedArtist = tag.getFirst(FieldKey.ARTIST);
                    if (StringUtils.hasText(parsedArtist)) music.setArtist(parsedArtist);
                    
                    String parsedAlbum = tag.getFirst(FieldKey.ALBUM);
                    if (StringUtils.hasText(parsedAlbum)) music.setAlbum(parsedAlbum);
                }
                music.setDuration((long) f.getAudioHeader().getTrackLength());
            } catch (Exception e) {
                System.err.println("Failed to parse metadata for " + originalFilename + ": " + e.getMessage());
            }

            // 2. Override with user provided metadata if present
            if (StringUtils.hasText(title)) {
                music.setTitle(title);
            }
            if (StringUtils.hasText(artist)) {
                music.setArtist(artist);
            }
            if (StringUtils.hasText(album)) {
                music.setAlbum(album);
            }

            musicRepository.save(music);
            
            result.put("id", music.getId());
            result.put("title", music.getTitle());
            result.put("artist", music.getArtist());
            
        } catch (IOException e) {
            return ApiResponse.error(500, "Failed to upload file: " + e.getMessage());
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> stream(@PathVariable String id, @RequestHeader(value = "Range", required = false) String rangeHeader) {
        // Simple implementation relying on Spring's default Resource handling which supports Range if configured correctly.
        // For better control, we would use ResourceRegion, but for "simple" requirements, FileSystemResource is often enough
        // provided we don't disable standard converters.
        // However, to ensure 206 is returned, let's let Spring handle it or manually check.
        // A common pattern in Spring Controllers for media is returning ResponseEntity<Resource> directly.
        // If the client sends "Range", Spring's ResourceHttpMessageConverter SHOULD handle it.
        return serveFile(id, true);
    }

    @GetMapping("/cover/{id}")
    public ResponseEntity<Resource> cover(@PathVariable String id) {
        return serveFile(id, false);
    }

    private ResponseEntity<Resource> serveFile(String id, boolean isAudio) {
        return musicRepository.findById(id).map(music -> {
            String path = isAudio ? music.getFilePath() : music.getCoverPath();
            if (path == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>build();

            File file = new File(path);
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>build();
            }

            FileSystemResource resource = new FileSystemResource(file);
            MediaType mediaType;
            if (isAudio) {
                // Simple detection
                if (path.endsWith(".flac")) mediaType = MediaType.parseMediaType("audio/flac");
                else mediaType = MediaType.parseMediaType("audio/mpeg");
            } else {
                mediaType = MediaType.IMAGE_JPEG; // Assume jpg for now
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body((Resource) resource);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
