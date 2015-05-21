package com.pplive.epg.bestv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BestvUtil {

	private final static String getkey_url = "http://wechat.bestv.com.cn/LiveDoorChain/getkey?view=json";
	
	// 2 卫视, 3 上海
	private final static String find_channel_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindOnlineSrcChannels?view=json&OnlineStatus=%d";
	
	private final static String playinfo_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindSchedulePlayInfo?view=json&" +
			"channelCode=%s&tableName=src_schedule";

	
	private final static String scheduleplayinfo_url_fmt = "http://wechat.bestv.com.cn/" +
			"BackWatching/FindSchedulePlayInfo?" +
			"view=json&channelCode=Umai:CHAN/%d@BESTV.SMG.SMG&tableName=src_schedule";
	
	private List<BestvChannel> mChannelList;
	
	public BestvUtil() {
		mChannelList = new ArrayList<BestvChannel>();
	}
	
	public List<BestvChannel> getChannelList() {
		return mChannelList;
	}
	
	public BestvPlayInfo playInfo(String channelCode) {
		String url = String.format(playinfo_url_fmt, channelCode);
		System.out.println("Java: playInfo() url: " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: playInfo() result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
 			JSONObject root = (JSONObject) jsonParser.nextValue();
 			String businessCode = root.getString("businessCode");
 			if (!businessCode.equals("success")) {
 				System.out.println("Java: businessCode is not success");
 				return null;
 			}
 			
 			String count = root.getString("count");
			int c = Integer.valueOf(count);
			System.out.println("Java: key count: " + c);
			JSONArray resultSetArray = root.getJSONArray("resultSet");
			
			if (resultSetArray.length() > 0) {
				JSONObject resultSet = resultSetArray.getJSONObject(0);
				
				String now = resultSet.getString("nowPlay");
				String will = resultSet.getString("toPlay");
				return new BestvPlayInfo(now, will);
			}
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean channel(int type) {
		String url = String.format(find_channel_url_fmt, type);
		System.out.println("Java: getChannel() url: " + url);
		
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return false;
			};
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: getChannel() result: " + result.substring(0, 256));
			
			JSONTokener jsonParser = new JSONTokener(result);
 			JSONObject root = (JSONObject) jsonParser.nextValue();
 			String businessCode = root.getString("businessCode");
 			if (!businessCode.equals("success")) {
 				System.out.println("Java: businessCode is not success");
 				return false;
 			}
 			
 			String count = root.getString("count");
			int c = Integer.valueOf(count);
			System.out.println("Java: key count: " + c);
			JSONArray resultSetArray = root.getJSONArray("resultSet");
			
			mChannelList.clear();
			
			for (int i=0;i<resultSetArray.length();i++) {
				JSONObject resultSet = resultSetArray.getJSONObject(i);
				
				String title = resultSet.getString("CallSign");
				String abbr = resultSet.getString("ChannelAbbr");
				String code = resultSet.getString("ChannelCode");
				String id = resultSet.getString("ChannelID");
				String number = resultSet.getString("ChannelNumber");
				String ImgUrl = resultSet.getString("ImgUrl"); // img/pic/s_dycj
				String PlayUrl = resultSet.getString("PlayUrl"); // http://wx.live.bestvcdn.com.cn/live/program/live991/weixindycj/index.m3u8?se=weixin&ct=2
			
				String img_url_prefix = "http://wechat.bestv.com.cn/backwatching/";
				String img_url = img_url_prefix + ImgUrl;
				BestvChannel channel = new BestvChannel(title, abbr, code, id, number, img_url, PlayUrl);
				
				mChannelList.add(channel);
				System.out.println("Java: add channel " + channel.toString());
			}
			
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return false;
		
	}
	
	public BestvKey getLiveKey() {
		
		BestvKey key = null;
		
		// 创建HttpClientBuilder  
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();  
        // HttpClient  
        //CloseableHttpClient closeableHttpClient = httpClientBuilder.build();  
        // 依次是目标请求地址，端口号,协议类型  
        //HttpHost target = new HttpHost(getkey_url, 80, "http");
        // 依次是代理地址，代理端口号，协议类型  
        //HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");  
        //RequestConfig config = RequestConfig.custom().setProxy(proxy).build();  
        
        // 请求地址  
        HttpPost httpPost = new HttpPost(getkey_url);  
        //httpPost.setConfig(config);  
        
        httpPost.setHeader("Accept-Language", "zh-CN");
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
        httpPost.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
        httpPost.setHeader("Referer", "http://wechat.bestv.com.cn/backwatching/backwatchingdetail.jsp?" +
				"type=0&ChannelAbbr=typd");
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; " +
				"Android 4.4.4; zh-cn; MI 3W Build/KTU84P) " +
				"AppleWebKit/533.1 (KHTML, like Gecko)Version/4.0 " +
				"MQQBrowser/5.4 TBS/025411 Mobile Safari/533.1 " +
				"MicroMessenger/6.1.0.66_r1062275.542 NetType/WIFI");
        httpPost.setHeader("origin", "http://wechat.bestv.com.cn");
        httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        httpPost.setHeader("Accept-Encoding", "gzip");
        httpPost.setHeader("Cookie", "Hm_lvt_1e35bd6ba34fe8a6a160ab272fc892db=1427119654,1429530935; " +
        		"JSESSIONID=449CAEB76C12FBD93553531EDEBF3A89; " +
        		"lzstat_ss=3807293619_1_1432143406_3593490; " +
        		"lzstat_uv=1282489934416223788|3593490");
        
        //httpPost.setHeader("Cookie", "Hm_lvt_1e35bd6ba34fe8a6a160ab272fc892db=1427119654,1429530935; " +
        //		"JSESSIONID=3020FA3B781E4576013715A98C27DACC; " +
        //		"lzstat_ss=219980764_2_1432208319_3593490; " +
        //		"lzstat_uv=1282489934416223788|3593490; ");
		
        // 创建参数队列  
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();  
        // 参数名为platform_account，值是o47fhtx0JJ7Hocxaz0XRHTDdutCI  
        formparams.add(new BasicNameValuePair("platform_account", "o47fhtx0JJ7Hocxaz0XRHTDdutCI"));
        formparams.add(new BasicNameValuePair("userid", "379704"));
        formparams.add(new BasicNameValuePair("type", "0"));
        
        UrlEncodedFormEntity entity;  
        try {  
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");  
            httpPost.setEntity(entity);  
            //CloseableHttpResponse response = closeableHttpClient.execute(  
            //        target, httpPost);  
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpPost);
            // getEntity()  
            HttpEntity httpEntity = response.getEntity();  
            if (httpEntity != null) {  
                if (response.getStatusLine().getStatusCode() == 200){
	    			// 打印响应内容
	                String result = EntityUtils.toString(httpEntity, "UTF-8");
	                System.out.println("Java: response: " + result);
	                
	                JSONTokener jsonParser = new JSONTokener(result);
	    			JSONObject root = (JSONObject) jsonParser.nextValue();
	    			String businessCode = root.getString("businessCode");
	    			if (businessCode.equals("success")) {
		    			String count = root.getString("count");
		    			int c = Integer.valueOf(count);
		    			System.out.println("Java: key count: " + c);
		    			JSONArray resultSetArray = root.getJSONArray("resultSet");
		    			
		    			for (int i=0;i<resultSetArray.length();i++) {
		    				JSONObject resultSet = resultSetArray.getJSONObject(i);
		    				// "expiration_time": 1432150607702,
		    	            // "live_key": "3910F85C71F674CECF7742D8C3054FAE330FC5A3A62C519E127559318202683D",
		    	            // "update_time": "2015-05-20 17:36:47",
		    	            // "x_user_id": 379704
		    				int expiration_time = 0;
		    				if (resultSet.has("expiration_time"))
		    					expiration_time = resultSet.getInt("expiration_time");
		    				String live_key = resultSet.getString("live_key");
		    				String update_time = "N/A";
		    				if (resultSet.has("update_time"))
		    					update_time = resultSet.getString("update_time");
		    				int x_user_id = 0;
		    				if (resultSet.has("x_user_id"))
		    					x_user_id = resultSet.getInt("x_user_id");
		    				
		    				if (i==0) {
		    					key = new BestvKey(expiration_time, live_key, update_time, x_user_id);
		    				}
		    			}
	    			}
	    			else {
		    			System.out.println("Java: failed to get live key, businessCode: " + businessCode);
	    			}
                }
	            else {
	            	System.out.println("Java: failed to get live key, response is not 200");
	            }  
            }
            
            // 释放资源  
            //closeableHttpClient.close();  
            return key;
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        
		return key;  
	}
}
