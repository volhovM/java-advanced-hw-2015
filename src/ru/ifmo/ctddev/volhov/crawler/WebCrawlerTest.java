package ru.ifmo.ctddev.volhov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author volhovm
 *         Created on 4/15/15
 */
public class WebCrawlerTest {
    public static void main(String[] args) throws IOException {
        WebCrawler crawler = new WebCrawler(new CachingDownloader(new File("./downloaded/")), 20, 20, 1);
        System.out.println(crawler.download("http://neerc.ifmo.ru/~sta/2014-2015/2-algorithms/", 2));
        crawler.close();
    }
}
