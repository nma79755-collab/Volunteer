package com.example.springai.Utils;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

@Component
public class OrcUtil {
    //图像识别
    public  String orc(MultipartFile file) throws TesseractException, IOException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("E:\\Orc\\tessdata");
        tesseract.setLanguage("chi_sim+eng");
        tesseract.setPageSegMode(3);  // 自动页面分割
        tesseract.setOcrEngineMode(3);  // 使用 LSTM 神经网络引擎（对复杂字体更友好）
        BufferedImage imageUnpro = ImageIO.read(file.getInputStream());
        BufferedImage image = autoPreprocess(imageUnpro);//分析需不需要预处理
        String s = tesseract.doOCR(image);
        System.out.println("识别内容为"+s);
        return s;
    }
    private BufferedImage processImage(BufferedImage image){
        int height = image.getHeight();
        int width = image.getWidth();
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);//创建一个灰度图像
        gray.getGraphics().drawImage(image, 0, 0, null);//将原图写入灰度图
        // 构造对比度/亮度 调整器
        RescaleOp rescaleOp = new RescaleOp(2.0f, 0, null);
        // 传入灰度图，生成增强后的新图片
        BufferedImage contrasted = rescaleOp.filter(gray, null);
        //
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        binary.getGraphics().drawImage(contrasted, 0, 0, null);//将黑色图再转为黑白图
        BufferedImage denoise = denoise(binary);//去除噪点
        return denoise;
    }
    // 中值滤波去噪
    private BufferedImage denoise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int count = 0;
                // 统计周围8个像素中有多少黑色
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (image.getRGB(x + dx, y + dy) == Color.BLACK.getRGB()) {
                            count++;
                        }
                    }
                }
                // 如果周围黑色点多于5个，保留黑色，否则设为白色
                result.setRGB(x, y, count > 5 ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return result;
    }
    /**
     * 自动判断是否需要预处理
     */
    private BufferedImage autoPreprocess(BufferedImage image) {
        double variance = calculateImageVariance(image);

        System.out.println("图片方差: " + variance);

        if (variance < 60) {
            System.out.println("→ 检测为清晰截图，进行简单预处理");
            return image;
        } else if (variance < 120) {
            // 略复杂，只轻微增强对比度，不二值化
            System.out.println("→ 检测为中等复杂图片，仅增强对比度");
            BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            gray.getGraphics().drawImage(image, 0, 0, null);
            RescaleOp rescaleOp = new RescaleOp(1.5f, 0, null);
            return rescaleOp.filter(gray, null);
        } else {
            // 很复杂，才用全套预处理
            System.out.println("→ 检测为复杂图片，进行全套预处理");
            return processImage(image);
        }
    }

    /**
     * 计算图片像素方差（判断复杂度）
     */
    private double calculateImageVariance(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double sum = 0;
        double sumSq = 0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = (image.getRGB(x, y) & 0xFF);  // 取灰度值
                sum += gray;
                sumSq += gray * gray;
            }
        }

        double mean = sum / totalPixels;
        return Math.sqrt(sumSq / totalPixels - mean * mean);
    }
}
