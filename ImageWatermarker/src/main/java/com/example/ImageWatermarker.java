package com.example;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ImageWatermarker {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入图片文件路径: ");
        String imagePath = scanner.nextLine();
        scanner.close();

        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            System.err.println("错误: 文件不存在或不是一个有效的文件。");
            return;
        }

        try {
            // 1. 读取拍摄时间作为水印
            String watermarkText = getCaptureDate(imageFile);
            if (watermarkText == null || watermarkText.isEmpty()) {
                System.out.println("未找到图片的拍摄日期，将使用当前日期作为水印。");
                watermarkText = new SimpleDateFormat("yyyy年MM月dd日").format(new Date());
            }

            // 2. 将文本水印绘制到图片上
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage == null) {
                System.err.println("错误: 无法读取图片文件。请确保它是有效的图片格式。");
                return;
            }
            BufferedImage watermarkedImage = addTextWatermark(originalImage, watermarkText);

            // 3. 保存为新的图片文件
            saveWatermarkedImage(imageFile, watermarkedImage);

            System.out.println("水印添加成功！");

        } catch (ImageProcessingException e) {
            System.err.println("处理图片元数据时发生错误: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("处理图片文件时发生I/O错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("发生未知错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getCaptureDate(File imageFile) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (directory != null && directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            Date date = directory.getDateOriginal();
            if (date != null) {
                return new SimpleDateFormat("yyyy年MM月dd日").format(date);
            }
        }
        return null;
    }

    private static BufferedImage addTextWatermark(BufferedImage originalImage, String watermarkText) {
        Graphics2D g2d = (Graphics2D) originalImage.getGraphics();

        // 设置水印字体和颜色
        Font font = new Font("SansSerif", Font.BOLD, 36);
        g2d.setFont(font);
        g2d.setColor(new Color(255, 255, 255, 128)); // 白色，半透明

        // 计算水印位置 (右下角)
        FontMetrics fontMetrics = g2d.getFontMetrics(font);
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();
        int padding = 20; // 边距

        int x = originalImage.getWidth() - textWidth - padding;
        int y = originalImage.getHeight() - padding; // 调整y坐标，使其在图片内部

        // 绘制水印
        g2d.drawString(watermarkText, x, y);
        g2d.dispose(); // 释放图形上下文资源
        return originalImage;
    }

    private static void saveWatermarkedImage(File originalFile, BufferedImage watermarkedImage) throws IOException {
        Path originalDirPath = originalFile.getParentFile().toPath();
        String originalDirName = originalFile.getParentFile().getName();
        Path outputDirPath = originalDirPath.resolve(originalDirName + "_watermark");

        if (!Files.exists(outputDirPath)) {
            Files.createDirectories(outputDirPath);
        }

        String fileName = originalFile.getName();
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            fileExtension = fileName.substring(dotIndex + 1).toLowerCase();
            fileName = fileName.substring(0, dotIndex);
        } else {
            // 如果没有扩展名，默认使用 jpg
            fileExtension = "jpg";
        }

        Path outputPath = outputDirPath.resolve(fileName + "_watermarked." + fileExtension);
        ImageIO.write(watermarkedImage, fileExtension, outputPath.toFile());
        System.out.println("水印图片已保存到: " + outputPath.toAbsolutePath());
    }
}
