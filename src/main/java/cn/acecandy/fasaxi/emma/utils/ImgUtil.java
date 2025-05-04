package cn.acecandy.fasaxi.emma.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.io.resource.ResourceUtil;
import org.dromara.hutool.core.text.StrUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorConvertOp;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;

/**
 * 图片 工具类
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
public final class ImgUtil extends org.dromara.hutool.swing.img.ImgUtil {
    private ImgUtil() {
    }

    public static InputStream protectPic(File file) {
        try (InputStream input = addAdversarialNoise(FileUtil.getInputStream(file), 0.35f)) {
            return addProportionalWatermark(input,
                    "static/pic/protect.jpg", 0.25f);
            // return compressImage(input2, 0.5f);
        } catch (Exception e) {
            log.error("图片保护失败", e);
            return null;
        }
    }

    @SneakyThrows
    public static InputStream compressImage(InputStream inputStream, float compressionQuality) {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }

        // 从输入流中读取图像
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("无法读取图像，请检查图像文件是否存在、格式是否支持或是否损坏");
        }

        // 获取 JPEG 图像写入器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG image writer found.");
        }
        ImageWriter writer = writers.next();

        // 获取图像写入参数
        ImageWriteParam param = writer.getDefaultWriteParam();
        // 设置压缩模式为有损耗压缩
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // 设置压缩质量
        param.setCompressionQuality(compressionQuality);

        // 创建字节数组输出流，用于存储压缩后的图像数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // 将图像写入字节数组输出流
            writer.setOutput(ImageIO.createImageOutputStream(outputStream));
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        // 将字节数组输出流中的数据转换为字节数组输入流
        return new ByteArrayInputStream(outputStream.toByteArray());
    }


    @SneakyThrows
    public static InputStream addAdversarialNoise(InputStream input, float intensity) {
        BufferedImage originalImage = ImageIO.read(input);
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage noisyImage = new BufferedImage(width, height, originalImage.getType());
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = originalImage.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                // 为每个颜色通道生成独立噪声
                int deltaRed = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);
                int deltaGreen = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);
                int deltaBlue = (int) ((random.nextFloat() * 2 - 1) * intensity * 255);

                red = clamp(red + deltaRed, 0, 255);
                green = clamp(green + deltaGreen, 0, 255);
                blue = clamp(blue + deltaBlue, 0, 255);

                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                noisyImage.setRGB(x, y, newPixel);
            }
        }

        // 将处理后的图像写入内存流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(noisyImage, "jpg", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // 覆盖模式枚举
    public enum ScaleMode {
        WIDTH,       // 按宽度比例缩放
        HEIGHT,      // 按高度比例缩放
        CONTAIN,     // 保持比例最大适应
        COVER        // 保持比例完全覆盖
    }

    /**
     * 添加比例缩放水印（默认宽度比例50%）
     *
     * @param input         原始图片路径
     * @param watermarkPath 水印图片路径
     * @param opacity       透明度 0.0-1.0
     */
    public static InputStream addProportionalWatermark(InputStream input,
                                                       String watermarkPath,
                                                       float opacity) {
        return addProportionalWatermark(input, watermarkPath, opacity, 1, ScaleMode.COVER, null);
    }

    /**
     * 高级版比例水印
     *
     * @param scale  缩放比例 (0-1)
     * @param mode   缩放模式
     * @param offset 位置偏移量（基于居中后的坐标）
     */
    @SneakyThrows
    public static InputStream addProportionalWatermark(InputStream input, String watermarkPath,
                                                       float opacity, float scale, ScaleMode mode, Point offset) {
        BufferedImage original = ImageIO.read(input);
        BufferedImage watermark = ImageIO.read(ResourceUtil.getStream(watermarkPath));

        // 计算缩放后尺寸
        Dimension scaledSize = calculateScaledSize(
                new Dimension(original.getWidth(), original.getHeight()),
                new Dimension(watermark.getWidth(), watermark.getHeight()),
                scale,
                mode
        );

        // 缩放水印
        Image scaledWatermark = watermark.getScaledInstance(
                scaledSize.width,
                scaledSize.height,
                Image.SCALE_SMOOTH
        );

        // 转换为BufferedImage
        BufferedImage scaledBuffered = new BufferedImage(
                scaledSize.width,
                scaledSize.height,
                BufferedImage.TYPE_INT_ARGB
        );
        scaledBuffered.getGraphics().drawImage(scaledWatermark, 0, 0, null);

        // 创建画布
        BufferedImage combined = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = combined.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver.derive(opacity));

        // 计算居中坐标
        int x = (original.getWidth() - scaledSize.width) / 2;
        int y = (original.getHeight() - scaledSize.height) / 2;

        // 应用偏移量
        if (offset != null) {
            x += offset.x;
            y += offset.y;
        }

        // 边界保护
        x = Math.max(0, Math.min(x, original.getWidth() - scaledSize.width));
        y = Math.max(0, Math.min(y, original.getHeight() - scaledSize.height));

        // 绘制水印
        g.rotate(Math.toRadians(180), x + scaledSize.getWidth() / 2, y + scaledSize.getHeight() / 2);
        g.drawImage(scaledBuffered, x, y, null);
        g.dispose();

        // 输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    // 计算缩放尺寸的核心算法
    private static Dimension calculateScaledSize(Dimension original,
                                                 Dimension watermark,
                                                 float scale,
                                                 ScaleMode mode) {
        switch (mode) {
            case WIDTH:
                int targetWidth = (int) (original.width * scale);
                int height = (int) (watermark.height * ((float) targetWidth / watermark.width));
                return new Dimension(targetWidth, height);

            case HEIGHT:
                int targetHeight = (int) (original.height * scale);
                int width = (int) (watermark.width * ((float) targetHeight / watermark.height));
                return new Dimension(width, targetHeight);

            case CONTAIN:
                float widthRatio = (original.width * scale) / (float) watermark.width;
                float heightRatio = (original.height * scale) / (float) watermark.height;
                float ratio = Math.min(widthRatio, heightRatio);
                return new Dimension(
                        (int) (watermark.width * ratio),
                        (int) (watermark.height * ratio)
                );

            case COVER:
                float cWidthRatio = (original.width * scale) / (float) watermark.width;
                float cHeightRatio = (original.height * scale) / (float) watermark.height;
                float cRatio = Math.max(cWidthRatio, cHeightRatio);
                return new Dimension(
                        (int) (watermark.width * cRatio),
                        (int) (watermark.height * cRatio)
                );

            default:
                throw new IllegalArgumentException("不支持的缩放模式");
        }
    }

    /**
     * 转为简笔画画风
     *
     * @param filePath 文件路径
     * @return {@link InputStream }
     */
    @SneakyThrows
    public static File briefStrokes(String filePath) {
        return briefStrokes(FileUtil.file(filePath));
    }

    /**
     * 转为简笔画画风
     *
     * @param file 文件路径
     * @return {@link InputStream }
     */
    @SneakyThrows
    public static File briefStrokes(File file) {
        // 1. 读取原图
        BufferedImage src = read(file);

        int originalWidth = src.getWidth();
        int originalHeight = src.getHeight();
        if (originalWidth < 400 || originalHeight < 400) {
            // 计算目标尺寸
            double scaleFactor = Math.min(400.0 / originalWidth, 400.0 / originalHeight);
            int targetWidth = (int) (originalWidth * scaleFactor);
            int targetHeight = (int) (originalHeight * scaleFactor);
            src = resizeWithGraphics2D(src, targetWidth, targetHeight);
        }

        // 2. 灰度化
        BufferedImage gray = fastGray(src);

        // 3. 边缘检测
        BufferedImage edges = sobelEdge(gray);

        // 4. 反色处理
        BufferedImage inverted = invertColor(edges);

        // 5. 保存结果
        File outFile = FileUtil.file(StrUtil.replaceLast(FileUtil.getAbsolutePath(file),
                ".jpg", "-briefStrokes.jpg", true));
        ImageIO.write(inverted, "jpg", outFile);
        return outFile;
    }

    public static BufferedImage fastGray(BufferedImage src) {
        ColorConvertOp op = new ColorConvertOp(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(src, null);
    }

    public static BufferedImage sobelEdge(BufferedImage grayImg) {
        int[][] xKernel = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}}; // 水平方向
        int[][] yKernel = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}}; // 垂直方向
        int width = grayImg.getWidth();
        int height = grayImg.getHeight();
        BufferedImage edgeImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int gx = 0, gy = 0;
                // 计算梯度
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int pixel = grayImg.getRGB(x + i, y + j) & 0xFF;
                        gx += pixel * xKernel[i + 1][j + 1];
                        gy += pixel * yKernel[i + 1][j + 1];
                    }
                }
                int gradient = (int) Math.sqrt(gx * gx + gy * gy);
                edgeImg.setRGB(x, y, gradient > 128 ? 0xFFFFFF : 0x000000); // 阈值处理
            }
        }
        return edgeImg;
    }

    public static BufferedImage invertColor(BufferedImage img) {
        byte[] invertTable = new byte[256];
        for (int i = 0; i < 256; i++) {
            invertTable[i] = (byte) (255 - i);
        }
        LookupTable table = new ByteLookupTable(0, invertTable);
        LookupOp op = new LookupOp(table, null);
        return op.filter(img, null);
    }

    /**
     * 无损放大
     *
     * @param original     原来
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @return {@link BufferedImage }
     */
    public static BufferedImage resizeWithGraphics2D(BufferedImage original,
                                                     int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        // 设置高质量渲染参数
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }
}