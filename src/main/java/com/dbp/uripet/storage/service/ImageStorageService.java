package com.dbp.uripet.storage.service;

import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

	private static final String INVALID_IMAGE_MESSAGE =
			"Formato de imagen no valido. Solo se permiten gif, webp, png, jpg y jpeg.";

	private final S3StorageService s3StorageService;

	static {
		ImageIO.scanForPlugins();
	}

	public List<String> storePetImages(String petPid, List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return List.of();
		}

		List<String> uploadedUrls = new ArrayList<>();
		int imageIndex = 1;

		for (MultipartFile image : images) {
			if (image == null || image.isEmpty()) {
				continue;
			}

			uploadedUrls.add(storeSingleImage(petPid, image, imageIndex));
			imageIndex++;
		}

		return uploadedUrls;
	}

	private String storeSingleImage(String petPid, MultipartFile image, int imageIndex) {

		String contentType = normalizeContentType(image.getContentType());
		String extension = resolveExtension(image.getOriginalFilename());
		String format = resolveFormat(contentType, extension);

		if (format == null) {
			throw new ValidationException(INVALID_IMAGE_MESSAGE);
		}

		return switch (format) {
			case "gif" -> uploadAsIs(
					petPid,
					image,
					imageIndex,
					"gif",
					"image/gif"
			);

			case "webp" -> uploadAsIs(
					petPid,
					image,
					imageIndex,
					"webp",
					"image/webp"
			);

			case "png", "jpeg" -> uploadAsWebp(
					petPid,
					image,
					imageIndex
			);

			default -> throw new ValidationException(INVALID_IMAGE_MESSAGE);
		};
	}

	private String uploadAsIs(
			String petPid,
			MultipartFile image,
			int imageIndex,
			String extension,
			String contentType
	) {
		try {

			String key = buildKey(petPid, imageIndex, extension);

			return s3StorageService.upload(
					key,
					image.getBytes(),
					contentType
			);

		} catch (IOException ex) {
			throw new com.dbp.uripet.config.error.ServerErrorException(
					"Unable to read image content",
					ex
			);
		}
	}

	private String uploadAsWebp(
			String petPid,
			MultipartFile image,
			int imageIndex
	) {
		try {

			BufferedImage bufferedImage =
					ImageIO.read(new ByteArrayInputStream(image.getBytes()));

			if (bufferedImage == null) {
				throw new ValidationException(INVALID_IMAGE_MESSAGE);
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			boolean written =
					ImageIO.write(bufferedImage, "webp", outputStream);

			if (!written) {
				throw new InvalidOperationException(
						"WEBP encoder is not available"
				);
			}

			String key = buildKey(
					petPid,
					imageIndex,
					"webp"
			);

			return s3StorageService.upload(
					key,
					outputStream.toByteArray(),
					"image/webp"
			);

		} catch (IOException ex) {
			throw new com.dbp.uripet.config.error.ServerErrorException(
					"Unable to transform image",
					ex
			);
		}
	}

	/**
	 * Cada imagen obtiene un nombre único para evitar
	 * sobrescribir imágenes anteriores en S3.
	 */
	private String buildKey(
			String petPid,
			int imageIndex,
			String extension
	) {

		return "pet-"
				+ petPid
				+ "-images/imagen-"
				+ System.currentTimeMillis()
				+ "-"
				+ imageIndex
				+ "."
				+ extension;
	}

	private String resolveFormat(
			String contentType,
			String extension
	) {

		if ("image/gif".equals(contentType)
				|| "gif".equals(extension)) {
			return "gif";
		}

		if ("image/webp".equals(contentType)
				|| "webp".equals(extension)) {
			return "webp";
		}

		if ("image/png".equals(contentType)
				|| "image/x-png".equals(contentType)
				|| "png".equals(extension)) {
			return "png";
		}

		if ("image/jpeg".equals(contentType)
				|| "image/jpg".equals(contentType)
				|| "image/pjpeg".equals(contentType)
				|| "jpeg".equals(extension)
				|| "jpg".equals(extension)) {
			return "jpeg";
		}

		if (contentType != null && contentType.startsWith("image/")) {
			return switch (extension) {
				case "gif", "webp", "png", "jpeg", "jpg" ->
						resolveFormat(null, extension);
				default -> null;
			};
		}

		return null;
	}

	private String normalizeContentType(String contentType) {

		if (contentType == null) {
			return null;
		}

		return contentType.toLowerCase(Locale.ROOT);
	}

	private String resolveExtension(String originalFilename) {

		if (originalFilename == null
				|| originalFilename.isBlank()) {
			return null;
		}

		int lastDot = originalFilename.lastIndexOf('.');

		if (lastDot < 0
				|| lastDot == originalFilename.length() - 1) {
			return null;
		}

		return originalFilename
				.substring(lastDot + 1)
				.toLowerCase(Locale.ROOT);
	}
}