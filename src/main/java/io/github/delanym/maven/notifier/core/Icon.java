package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * An icon that can be rendered in a notification, backed by a classpath or
 * file-system URL. Lazily writes the image to a cache directory so that
 * external notification tools (PowerShell, terminal-notifier, etc.) can
 * reference it by path.
 */
@NullMarked
public final class Icon {

    private static final Path CACHE_DIR = Path.of(
            System.getProperty("user.home"),
            ".m2", "notifier-maven-extension", "cache");

    private final String id;
    private final URL content;
    private Path cachedPath;

    private Icon(String id, URL content) {
        this.id = id;
        this.content = content;
    }

    public static Icon of(URL url, String id) {
        return new Icon(id, url);
    }

    public String id() {
        return id;
    }

    public URL content() {
        return content;
    }

    /**
     * Returns the file-system path to a cached copy of this icon, creating
     * it on first call. External tools that require a file path use this.
     */
    public synchronized Path asPath() {
        if (cachedPath == null) {
            cachedPath = writeToCache();
        }
        return cachedPath;
    }

    public byte[] toByteArray() {
        try (InputStream in = content.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read icon: " + id, e);
        }
    }

    public RenderedImage toImage() {
        try (InputStream in = content.openStream()) {
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read icon image: " + id, e);
        }
    }

    private Path writeToCache() {
        String fileName = fileNameFromUrl(content);
        try {
            Files.createDirectories(CACHE_DIR);
            Path iconFile = CACHE_DIR.resolve(fileName);
            try (InputStream in = content.openStream()) {
                Files.write(iconFile, in.readAllBytes());
            }
            return iconFile;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to cache icon: " + id, e);
        }
    }

    private static String fileNameFromUrl(URL url) {
        String path = url.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof Icon other
                && Objects.equals(id, other.id)
                && Objects.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content);
    }

    @Override
    public String toString() {
        return "Icon{id='" + id + "'}";
    }
}
