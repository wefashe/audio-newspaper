package org.example.newspaper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Newspaper {
    public static void main(String[] args) throws IOException, JavaLayerException {
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC+8"));
        ItemEnum itemEnum = ItemEnum.FM_367750;
        if (now.getHour() > 12) {
            itemEnum = ItemEnum.FM_456498;
        }
        int pageNum = 1;
        int pageSize = 100;
        writeMdFile(getJSONArray(itemEnum, pageNum, pageSize));
    }

    public static void play(String path) throws IOException, JavaLayerException {
        URL url = new URL(path);
        BufferedInputStream stream = new BufferedInputStream(url.openStream());
        Player player = new Player(stream);
        player.play();
    }

    private static JSONArray getJSONArray(ItemEnum item, int pageNum, int pageSize) throws IOException {
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize <= 0) {
            pageSize = 20;
        }
        String base_path = "https://d.fm.renbenai.com/fm/read/fmd/h5/getPayResourceList_714.html?pid=%s&isFree=2&pageNum=%s&pageSize=%s";
        String path = String.format(base_path, item.getPid(), pageNum, pageSize);
        String s = IOUtils.toString(URI.create(path), StandardCharsets.UTF_8);
        s = s.substring(s.indexOf("{"));
        s = s.substring(0, s.lastIndexOf("}") + 1);
        JSONObject obj = JSON.parseObject(s);
        String code = obj.getString("code");
        String msg = obj.getString("msg");
        if (!"0".equals(code)) {
            throw new IOException(msg);
        }
        return obj.getJSONObject("data").getJSONArray("list");
    }

    public static void writeMdFile(JSONArray array) throws IOException {
        Path readMe = Paths.get("README.md");
        if (!Files.exists(readMe)) {
            Files.createFile(readMe);
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0, size = array.size(); i < size; i++) {
            JSONObject obj = array.getJSONObject(i);
            if (i == 0) {
                String image = obj.getString("img640_640");
                context.append(String.format("![](%s)", image)).append(System.lineSeparator());
            }
            String title = obj.getString("title");
            String filePath = obj.getJSONArray("audiolist").getJSONObject(0).getString("filePath");
            context.append("- ").append(String.format("[%s](%s)", title, filePath)).append(System.lineSeparator());

            String publishTime = obj.getString("publishTime");
            Instant timestamp = Instant.ofEpochSecond(Long.parseLong(publishTime));
            ZonedDateTime zonedDateTime = timestamp.atZone(ZoneId.systemDefault());
            String dataTime = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println(i + 1 + " " + title + " " + dataTime);

        }
        Files.write(readMe, context.toString().getBytes());
    }
}
