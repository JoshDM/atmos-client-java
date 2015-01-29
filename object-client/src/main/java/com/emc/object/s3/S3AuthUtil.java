package com.emc.object.s3;

import com.emc.object.util.RestUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class S3AuthUtil {
    private static final Logger l4j = Logger.getLogger(S3AuthUtil.class);

    public static SortedSet<String> SIGNED_PARAMETERS;

    static {
        SIGNED_PARAMETERS = new TreeSet<>(Arrays.asList(
                "acl", "torrent", "logging", "location", "policy", "requestPayment", "versioning",
                "versions", "versionId", "notification", "uploadId", "uploads", "partNumber", "website",
                "delete", "lifecycle", "tagging", "cors", "restore",
                S3Constants.PARAM_RESPONSE_HEADER_CACHE_CONTROL,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_DISPOSITION,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_ENCODING,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_LANGUAGE,
                S3Constants.PARAM_RESPONSE_HEADER_CONTENT_TYPE,
                S3Constants.PARAM_RESPONSE_HEADER_EXPIRES,
                S3Constants.PARAM_ACCESS_MODE,
                S3Constants.PARAM_FILE_ACCESS,
                S3Constants.PARAM_ENDPOINT));
    }

    public static void sign(String method, String resource, Map<String, String> parameters, Map<String, List<Object>> headers,
                            String accessKey, String secretKey, long clockSkew) {
        String stringToSign = getStringToSign(method, resource, parameters, headers, clockSkew);
        String signature = getSignature(stringToSign, secretKey);
        RestUtil.putSingle(headers, "Authorization", "AWS " + accessKey + ":" + signature);
    }

    public static String getStringToSign(String method, String resource, Map<String, String> parameters,
                                         Map<String, List<Object>> headers, long clockSkew) {
        StringBuilder stringToSign = new StringBuilder();

        // method line
        stringToSign.append(method).append("\n");

        // MD5 line
        String contentMd5 = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_MD5);
        if (contentMd5 != null) stringToSign.append(contentMd5);
        stringToSign.append("\n");

        // content type line
        String contentType = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_TYPE);
        if (contentType != null) stringToSign.append(contentType);
        stringToSign.append("\n");

        // date line
        // use Date header by default
        String date = RestUtil.getFirstAsString(headers, RestUtil.HEADER_DATE);
        if (date == null) {
            // must have a date in the headers
            date = RestUtil.getRequestDate(clockSkew);
            RestUtil.putSingle(headers, RestUtil.HEADER_DATE, date);
        }
        // if x-amz-date is specified, date line should be blank
        if (headers.containsKey(S3Constants.AMZ_DATE))
            date = "";
        // if expires parameter is set, use that instead
        if (parameters.containsKey(S3Constants.PARAM_EXPIRES))
            date = parameters.get(S3Constants.PARAM_EXPIRES);
        stringToSign.append(date);
        stringToSign.append("\n");

        // canonicalized headers
        SortedMap<String, String> canonicalizedHeaders = getCanonicalizedHeaders(headers, parameters);
        for (String name : canonicalizedHeaders.keySet()) {
            stringToSign.append(name).append(":").append(canonicalizedHeaders.get(name));
            stringToSign.append("\n");
        }

        // resource path (includes signed parameters)
        stringToSign.append(resource);
        boolean firstParameter = true;
        for (String parameter : SIGNED_PARAMETERS) {
            if (parameters.containsKey(parameter)) {
                stringToSign.append(firstParameter ? "?" : "&").append(parameter);
                String value = parameters.get(parameter);
                if (value != null) stringToSign.append("=").append(value);
                firstParameter = false;
            }
        }

        String stringToSignStr = stringToSign.toString();
        l4j.debug("stringToSign:\n" + stringToSignStr);
        return stringToSignStr;
    }

    public static SortedMap<String, String> getCanonicalizedHeaders(Map<String, List<Object>> headers, Map<String, String> parameters) {
        SortedMap<String, String> canonicalizedHeaders = new TreeMap<>();

        // add x-emc- and x-amz- headers
        for (String header : headers.keySet()) {
            String lcHeader = header.toLowerCase();
            if (lcHeader.startsWith(S3Constants.AMZ_PREFIX) || lcHeader.startsWith(RestUtil.EMC_PREFIX)) {
                canonicalizedHeaders.put(lcHeader, RestUtil.delimit(headers.get(header), ","));
            }
        }

        // add x-amz- parameters
        for (String parameter : parameters.keySet()) {
            String lcParameter = parameter.toLowerCase();
            if (lcParameter.startsWith(S3Constants.AMZ_PREFIX)) {
                canonicalizedHeaders.put(lcParameter, parameters.get(parameter));
            }
        }

        return canonicalizedHeaders;
    }

    public static String getSignature(String stringToSign, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA1")); // AWS does not B64-decode the secret key!
            String signature = new String(Base64.encodeBase64(mac.doFinal(stringToSign.getBytes("UTF-8"))));
            l4j.debug("signature:\n" + signature);
            return signature;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HmacSHA1 algorithm is not supported on this platform", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported on this platform", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("The secret key \"" + secretKey + "\" is not valid", e);
        }
    }

    private S3AuthUtil() {
    }
}
