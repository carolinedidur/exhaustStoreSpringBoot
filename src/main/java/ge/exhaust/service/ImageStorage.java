package ge.exhaust.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ImageStorage {
    private final Path root;
    public ImageStorage(@Value("${app.upload-dir}") String directory) throws IOException {
        root=Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }
    public String store(MultipartFile file) {
        if(file==null || file.isEmpty()) throw new IllegalArgumentException("Choose a JPG or PNG photo.");
        if(file.getSize()>5*1024*1024) throw new IllegalArgumentException("The photo must be smaller than 5 MB.");
        try (ImageInputStream input=ImageIO.createImageInputStream(file.getInputStream())) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IllegalArgumentException("Please upload a valid JPG or PNG image.");
            var reader=readers.next();
            try {
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("jpeg","jpg","png").contains(format)) throw new IllegalArgumentException("Only JPG and PNG photos are supported.");
                reader.setInput(input);
                if((long)reader.getWidth(0)*reader.getHeight(0)>24_000_000) throw new IllegalArgumentException("Use a photo smaller than 24 megapixels.");
                var image=reader.read(0);
                String extension=format.equals("png") ? "png":"jpg";
                Path destination=root.resolve(UUID.randomUUID()+"."+extension);
                // Re-encode the decoded picture; original filenames and metadata are not persisted.
                ImageIO.write(image,extension,destination.toFile());
                return "/uploads/"+destination.getFileName();
            } finally {reader.dispose();}
        } catch(IOException e) {throw new IllegalArgumentException("The photo could not be saved. Please try another image.");}
    }
}
