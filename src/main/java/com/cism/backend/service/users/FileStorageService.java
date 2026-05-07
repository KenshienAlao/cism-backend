package com.cism.backend.service.users;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileStorageService {

    private final Cloudinary cloudinary;

    public FileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String customerAvatar(MultipartFile file, String userId) throws IOException {
        // Appending timestamp to userId ensures a unique URL every time, bypassing browser/CDN cache
        String publicId = userId + "_" + System.currentTimeMillis();
        
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "public_id",  publicId,
                "folder",     "customer_avatar",
                "overwrite",  true
            )
        );
        return (String) result.get("secure_url");
    }

    public String stallItemImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        
        // Using UUID ensures every upload is unique and never conflicts
        String uniqueId = java.util.UUID.randomUUID().toString();
        
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "public_id",  uniqueId,
                "folder",     "stall_item_image"
            )
        );
        return (String) result.get("secure_url");   
    }

    public String stallImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        
        // Using UUID ensures every upload is unique and never conflicts
        String uniqueId = java.util.UUID.randomUUID().toString();
        
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "public_id",  uniqueId,
                "folder",     "stall_image"
            )
        );
        return (String) result.get("secure_url");   
    }
}
