package com.sunxin.knowledge.document.storage;

import java.io.InputStream;

@FunctionalInterface
public interface MinioObjectReader {

    InputStream open(String bucket, String objectName) throws Exception;
}
