package com.lethe.disconf.utils;


import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/14 15:19
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class LoadFileUtils {

    private static final String[] LOAD_FILE_EXTENSION = new String[]{"properties", "yml"};

    private static final String DELIMITER_COMMA = ",";
    private static final String DELIMITER_SLASH = "/";
    private static final String FILE_PREFIX = "file:";


    public static boolean canLoadFileExtension(String fileName) {
        return Arrays.stream(LOAD_FILE_EXTENSION)
                .anyMatch(fileExtension -> StringUtils.endsWithIgnoreCase(fileName, fileExtension));
    }

    public static String loadFileExtension() {
        return String.join(DELIMITER_COMMA, LOAD_FILE_EXTENSION);
    }

    public static Collection<String> loadFileExtension(Collection<String> collection) {
        return collection.stream()
                .filter(loadFile ->
                        Arrays.stream(LOAD_FILE_EXTENSION)
                                .anyMatch(fileExtension -> StringUtils.endsWithIgnoreCase(loadFile, fileExtension))
                ).collect(Collectors.toList());
    }

    public static String localDownloadDir(String userDefineDownloadDir) {
        return FILE_PREFIX + userDefineDownloadDir + DELIMITER_SLASH;
    }

    public static String commaJoinToString(Collection<String> collection) {
        return String.join(DELIMITER_COMMA, collection);
    }

}
