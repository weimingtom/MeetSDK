/**
 * Copyright (C) 2013 PPTV
 *
 */
package android.pplive.media.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 *
 * @author leoxie
 * @version 2013-2-27
 */
public class UrlUtil {
	
	private static final Pattern sRegNuMediaPlaySupportUrl;
	private static final Pattern sRegOnlinePlayUrl;
	private static final Pattern sRegLivePlayUrl;
	
	static {
		sRegNuMediaPlaySupportUrl = Pattern.compile("^(ppvod\\d*|pplive\\d*|ppfile)://.*", Pattern.CASE_INSENSITIVE);
		sRegOnlinePlayUrl = Pattern.compile("^(ppvod\\d*|pplive\\d*|rtsp|http(s)?)://.*", Pattern.CASE_INSENSITIVE);
		sRegLivePlayUrl = Pattern.compile("^(rtsp|pplive\\d*)://.*", Pattern.CASE_INSENSITIVE);
	}
	
	public static boolean isPPTVPlayUrl(String url) {
		return null == url ? false : sRegNuMediaPlaySupportUrl.matcher(url.trim()).matches();
	}
	
	public static boolean isUseSystemExtractor(String url) {
		return false;
		/*String url_lower = url.toLowerCase();
		if (url_lower.startsWith("http://")) {
			if (url_lower.indexOf(".m3u8") != -1)
				return false;
		}
		
		return true;*/
		//return null == url ? false : sRegNuMediaPlaySupportUrl.matcher(url.trim()).matches();
	}
	
	public static boolean isOnlinePlayUrl(String url) {
		
		return null == url ? false : sRegOnlinePlayUrl.matcher(url.trim()).matches();
	}
	
	public static boolean isLivePlayUrl(String url) {
		
		return null == url ? false : sRegLivePlayUrl.matcher(url.trim()).matches();
	}
	
	public static String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
}
