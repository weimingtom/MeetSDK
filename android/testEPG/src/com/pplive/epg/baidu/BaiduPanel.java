package com.pplive.epg.baidu;

import javax.swing.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.pplive.epg.boxcontroller.Code;

import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*; 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public class BaiduPanel extends JPanel {
	
	private String mbOauth 		= "23.7a098be1ccff9b906f8dc09a4811f2f2.2592000.1438324151.184740130-266719";
	private String mbRootPath 	=  "/我的视频";
	private String list_by 		= "time"; // "name" "size"
	private String list_order 	= "desc"; // "asc"
	
	private final static int ONE_KILOBYTE = 1024;
	private final static int ONE_MAGABYTE = (ONE_KILOBYTE * ONE_KILOBYTE);
	private final static int ONE_GIGABYTE = (ONE_MAGABYTE * ONE_KILOBYTE);
	
	private final static String exe_filepath = "D:/software/vlc-3.0.0/vlc.exe";
	private final static String exe_ffplay = "D:/software/ffmpeg/ffplay.exe";
	
	private String BAIDU_PCS_PREFIX = "https://pcs.baidu.com/rest/2.0/pcs/file";
	private String BAIDU_PCS_SERVICE_PREFIX = "https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl";
	
	private String BAIDU_PCS_LIST = BAIDU_PCS_PREFIX + 
			"?method=list" +
			"&access_token=" + mbOauth + 
			"&path=";
	private String BAIDU_PCS_META = BAIDU_PCS_PREFIX + 
			"?method=meta" +
			"&access_token=" + mbOauth + 
			"&path=";
	private String BAIDU_PCS_DOWNLOAD = BAIDU_PCS_PREFIX + 
			"?method=download" +
			"&access_token=" + mbOauth + 
			"&path=";
	private String BAIDU_PCS_STREAMING = BAIDU_PCS_PREFIX + 
			"?method=streaming" +
			"&access_token=" + mbOauth + 
			"&path=";
	private String BAIDU_PCS_CLOUD_DL = BAIDU_PCS_SERVICE_PREFIX + 
			"?method=add_task" +
			"&access_token=" + mbOauth;
	
	private List<Map<String, Object>> mFileList;

	JButton btnUp	 	= new JButton("上一级");
	JButton btnReset 	= new JButton("重置");
	
	JLabel lblRootPath = new JLabel("info");
	
	JList<String> listItem 				= null;
	DefaultListModel<String> listModel 	= null;
	JScrollPane scrollPane				= null;
	
	JTextPane editPath = new JTextPane();
	JButton btnCreateFolder = new JButton("添加");
	JButton btnDownload = new JButton("下载");
	
	boolean bDownloading = false;
	boolean bInterrupt = false;
	
	JCheckBox cbTranscode = new JCheckBox("转码");
	JCheckBox cbUseVLC = new JCheckBox("vlc");
	JLabel lblInfo = new JLabel("信息");
	
	Font f = new Font("宋体", 0, 18);
	
	public BaiduPanel() {
		super();

		this.setLayout(null);
		
		// Action
		lblRootPath.setFont(f);
		lblRootPath.setBounds(20, 40, 300, 30);
		this.add(lblRootPath);
		
		listItem = new JList<String>();
		listItem.setFont(f);
		listModel = new DefaultListModel<String>();
		listItem.setModel(listModel);
		listItem.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listItem.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent event) {
				// TODO Auto-generated method stub
				int index = listItem.getSelectedIndex();
				if (index == -1)
					return;
				
				if (event.getClickCount() == 2) {
					if (mFileList != null) {
						Map<String, Object> fileinfo = mFileList.get(index);
						boolean isdir = (Boolean) fileinfo.get("isdir");
						if (isdir) {
							mbRootPath = (String) fileinfo.get("path");
							System.out.println("go into folder: " + mbRootPath);
							init_combobox();
						}
						else {
							String path = (String) fileinfo.get("path");
							if (cbTranscode.isSelected() && streaming(path, "M3U8_640_480")) {
								String[] cmd = new String[] {exe_ffplay, "index.m3u8"};
								openExe(cmd);
								return;
							}
							else {
								try {
									String encoded_path = URLEncoder.encode(path, "utf-8");

									String url = BAIDU_PCS_DOWNLOAD + encoded_path;
									System.out.println("ready to play url: " + url);
									lblInfo.setText("ready to play url: " + url);
									
									String exe_path = exe_filepath;
									if (!cbUseVLC.isSelected())
										exe_path = exe_ffplay;
									String[] cmd = new String[] {exe_path, url};
									openExe(cmd);
								} catch (UnsupportedEncodingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
				else if (event.getClickCount() == 1) {
					Map<String, Object> fileinfo = mFileList.get(index);
					boolean isdir = (Boolean) fileinfo.get("isdir");
					if (!isdir) {
						String path = (String) fileinfo.get("path");
						List<Map<String, Object>> metaList = meta(path);
						if (metaList != null && metaList.size() > 0) {
							Map<String, Object> metainfo = metaList.get(0);
							long size = (Long) metainfo.get("filesize");
							lblInfo.setText("文件大小: " + getFileSize(size));
						}
					}
				}
			}
		});
		
		scrollPane = new JScrollPane(listItem);
		scrollPane.setBounds(20, 80, 450, 350);
		this.add(scrollPane);
		
		btnUp.setBounds(400, 30, 100, 40);
		btnUp.setFont(f);
		this.add(btnUp);

		btnUp.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (mbRootPath.equals("/"))
					return;
				
				int pos = mbRootPath.lastIndexOf("/");
				if (pos < 0)
					return;
				
				if (pos > 0)
					mbRootPath = mbRootPath.substring(0, pos);
				else 
					mbRootPath = "/";
				init_combobox();
			}
		});
		
		btnReset.setBounds(510, 30, 70, 40);
		btnReset.setFont(f);
		this.add(btnReset);

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mbRootPath = "/我的视频";
				init_combobox();
			}
		});
		
		editPath.setFont(f);
		editPath.setBounds(20, 450, 200, 40);
		editPath.setText("新建文件夹");
	    this.add(editPath);
	    
	    btnCreateFolder.setFont(f);
	    btnCreateFolder.setBounds(230, 450, 80, 40);
	    btnCreateFolder.setFont(f);
		this.add(btnCreateFolder);
		btnCreateFolder.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String src_url = editPath.getText();
			}
		});
		
		btnDownload.setFont(f);
		btnDownload.setBounds(320, 450, 80, 40);
		btnDownload.setFont(f);
		this.add(btnDownload);
		btnDownload.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (bDownloading) {
					bInterrupt = true;
					return;
				}
				
				int index = listItem.getSelectedIndex();
				Map<String, Object> fileinfo = mFileList.get(index);
				boolean isdir = (Boolean) fileinfo.get("isdir");
				if (isdir) {
					System.out.println("cannot download folder");
					lblInfo.setText("cannot download folder");
				}
				else {
					String path = (String) fileinfo.get("path");
					
					try {
						String encoded_path = URLEncoder.encode(path, "utf-8");

						String url = BAIDU_PCS_DOWNLOAD + encoded_path;
						System.out.println("ready to download url: " + url);
						lblInfo.setText("ready to download url: " + url);
						
						int pos = path.lastIndexOf("/");
						String filename = path.substring(pos + 1);
						downloadFile(url, filename);
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		
		cbTranscode.setBounds(20, 500, 80, 20);
		this.add(cbTranscode);
		
		cbUseVLC.setBounds(20, 520, 80, 20);
		cbUseVLC.setSelected(true);
		this.add(cbUseVLC);
		
		lblInfo.setBounds(100, 500, 500, 20);
		this.add(lblInfo);
		
		init_combobox();
	}
	
	private void downloadFile(String path, String filename) {
		try {
			URL url = new URL(path);
			URLConnection urlCon = url.openConnection();

			// 显示下载信息
			System.out.println("文件下载信息: ");
			System.out.println("host : " + url.getHost());
			System.out.println("port :" + url.getPort());
			System.out.println("Contenttype : " + urlCon.getContentType());
			System.out.println("Contentlength : " + urlCon.getContentLength());

			// 弹出"保存文件"对话框
			FileDialog fsave = new FileDialog(new Frame(), "保存文件",
					FileDialog.SAVE);
		
			FilenameFilter ff = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith("flac")) {
						return true;
					}
					return false;
				}
			};
			
			//fsave.setFilenameFilter(ff);
			fsave.setFile(filename);
			fsave.setVisible(true);
			
			final String save_path = fsave.getDirectory() + fsave.getFile();
			System.out.println("file save path: " + save_path);
			
			final FileOutputStream fos = new FileOutputStream(save_path);
			
			final InputStream is = urlCon.getInputStream();
		    Runnable r = new Runnable()
		    {
		    	public void run() {
		          try {
		        	  long downloaded = 0;
		        	  int readed;
		        	  byte []buf= new byte[4096];
		        	  long start_msec = System.currentTimeMillis();
		        	  long last_msec = 0;
		        	  
		        	  bDownloading = true;
		              while((readed = is.read(buf))!=-1 && !bInterrupt) {
		            	  fos.write(buf, 0, readed);
		            	  downloaded += readed;
		            	  
		            	  long cur_msec = System.currentTimeMillis();
		            	  if (cur_msec - last_msec > 300) {
		            		  long elapsed_msec = cur_msec - start_msec;
		            		  double speed = downloaded / (double)elapsed_msec;
			            	  lblInfo.setText(String.format("文件下载进度 %s, 速度 %.3f kB/s", 
			            			  getFileSize(downloaded), speed));  
			            	  last_msec = cur_msec;
		            	  }
		              }
		              
		              fos.close();
		              is.close();
		              if (bInterrupt)
		            	  lblInfo.setText("文件 " + save_path + " 下载取消");
		              else
		            	  lblInfo.setText("文件 " + save_path + " 下载成功!");
		              
		              bInterrupt = false;
		              bDownloading = false;
		              btnDownload.setText("下载");
		          }
		          catch (Exception ex) {
		        	  ex.printStackTrace();
		              lblInfo.setText("下载失败: " + ex.getMessage());
		          }
		      }
		  };
		  
		  Thread t = new Thread(r);
		  t.start();
		  
		  btnDownload.setText("取消");
		  
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init_combobox() {
		lblRootPath.setText(mbRootPath);
		
		mFileList = list(mbRootPath);
		if (mFileList == null)
			return;
		
		listModel.clear();
		
		int size = mFileList.size();
		for (int i=0;i<size;i++) {
			Map<String, Object> fileinfo = mFileList.get(i);
			String filename = (String) fileinfo.get("filename");
			listModel.addElement(filename);
		}
	}
	
	private boolean streaming(String path, String type) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		String url = BAIDU_PCS_STREAMING + encoded_path;
		url += "&type=";
		url += type;
		
		System.out.println("Java: streaming() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: code not 200: " + 
						response.getStatusLine().getStatusCode());
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			saveAsFileWriter(result);
			return true;
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private void saveAsFileWriter(String content) {

		FileWriter fwriter = null;
		String filename = "index.m3u8";
		try {
			fwriter = new FileWriter(filename);
			fwriter.write(content);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				fwriter.flush();
				fwriter.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private List<Map<String, Object>> list(String listPath) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(listPath, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = BAIDU_PCS_LIST + encoded_path;
		url += "&by=";
		url += list_by;
		url += "&order=";
		url += list_order;
		
		System.out.println("Java: list() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONArray list = root.getJSONArray("list");
			int cnt = list.length();
			
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i=0;i<cnt;i++) {
				JSONObject item = list.getJSONObject(i);
				String path = item.getString("path");
				long filesize = item.getLong("size");
				int isdir = item.getInt("isdir");
				
				String filename = path;
				int pos = filename.lastIndexOf("/");
				if (pos > -1) {
					filename = filename.substring(pos + 1);
				}
				
				Map<String, Object> fileinfo = new HashMap<String, Object>();
				fileinfo.put("path", path);
				fileinfo.put("filename", filename);
				fileinfo.put("filesize", filesize);
				fileinfo.put("isdir", isdir == 1 ? true : false);
				
				fileList.add(fileinfo);
			}
			
			return fileList;
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
	
	private List<Map<String, Object>> meta(String filepath) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(filepath, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String url = BAIDU_PCS_META + encoded_path;
		
		System.out.println("Java: meta() " + url);
		HttpGet request = new HttpGet(url);
		
		HttpResponse response;
		try {
			response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			JSONArray list = root.getJSONArray("list");
			int cnt = list.length();
			
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			for (int i=0;i<cnt;i++) {
				JSONObject item = list.getJSONObject(i);
				String path = item.getString("path");
				long filesize = item.getLong("size");
				int isdir = item.getInt("isdir");
				
				String filename = path;
				int pos = filename.lastIndexOf("/");
				if (pos > -1) {
					filename = filename.substring(pos + 1);
				}
				
				Map<String, Object> fileinfo = new HashMap<String, Object>();
				fileinfo.put("path", path);
				fileinfo.put("filename", filename);
				fileinfo.put("filesize", filesize);
				fileinfo.put("isdir", isdir == 1 ? true : false);
				
				fileList.add(fileinfo);
			}
			
			return fileList;
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
	
	private boolean add_cloud_dl(String source_url, String save_path) {
		String encoded_path = null;
		try {
			encoded_path = URLEncoder.encode(save_path, "utf-8");
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("method", "add_task"));
		params.add(new BasicNameValuePair("access_token", mbOauth));
		params.add(new BasicNameValuePair("source_url", source_url));
		params.add(new BasicNameValuePair("save_path", "/"));
		params.add(new BasicNameValuePair("type", "0"));
		/*params.add(new BasicNameValuePair("v", "1"));
		params.add(new BasicNameValuePair("rate_limit", "100"));
		params.add(new BasicNameValuePair("timeout", "100"));
		params.add(new BasicNameValuePair("callback", "www.baidu.com"));*/
		
		//String url = "https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl?" + 
		//		buildParams(params);
		String url = "https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl?" + 
				"method=add_task" + "&access_token=" + mbOauth +
				"&source_url=" + "http://6.jsdx3.crsky.com/201107/winzip150zh.exe" + 
				"&save_path=" + encoded_path + 
				"&type=0";

		System.out.println("Java: add_cloud_dl() " + url);

		// 把请求的数据，添加到NameValuePair中
		NameValuePair nameValuePair1 = new BasicNameValuePair("method",
				"add_task");
		NameValuePair nameValuePair2 = new BasicNameValuePair("source_url",
				"http://dlsw.baidu.com/sw-search-sp/soft/da/17519/BaiduYunGuanjia_5.2.7_setup.1430884921.exe");
		NameValuePair nameValuePair3 = new BasicNameValuePair("save_path", "/");
		NameValuePair nameValuePair4 = new BasicNameValuePair("type", "0");
		NameValuePair nameValuePair5 = new BasicNameValuePair("access_token", mbOauth);
		

		List<NameValuePair> list = new ArrayList<NameValuePair>();
		list.add(nameValuePair1);
		list.add(nameValuePair2);
		list.add(nameValuePair3);
		list.add(nameValuePair4);
		list.add(nameValuePair5);
		
		HttpResponse response;
		
		try {
			HttpEntity requesthttpEntity = new UrlEncodedFormEntity(list);
			
			HttpPost httppost = new HttpPost(
					"https://pcs.baidu.com/rest/2.0/pcs/services/cloud_dl");
			httppost.setEntity(requesthttpEntity);
			
			response = HttpClients.createDefault().execute(httppost);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println(String.format("Java: response is not ok: %d %s",
						response.getStatusLine().getStatusCode(),
						EntityUtils.toString(response.getEntity())));
				return false;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			if (root.has("error_code")) {
				int error_code = root.getInt("error_code");
				String error_msg = root.getString("error_msg");
				int request_id = root.getInt("request_id");
				System.out.println(String.format("Java: failed to add cloud job: %d(%s), request_id %d",
						error_code, error_msg, request_id));
				return false;
			}
			
			int task_id = root.getInt("task_id");
			int request_id = root.getInt("request_id");
			System.out.println(String.format("Java: new cloud job added: taskid: %d, request_id: %d",
					task_id, request_id));
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
	
	private String buildParams(List<BasicNameValuePair> urlParams) {
		String ret = null;

		if ((urlParams != null) && (urlParams.size() > 0))
			try {
				HttpEntity paramsEntity = new UrlEncodedFormEntity(urlParams,
						"utf8");
				ret = EntityUtils.toString(paramsEntity);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		return ret;
	}
	
	private String getFileSize(long size) {
	    String strSize;
	    if (size < 0)
	    	return "N/A";
	    
	    if (size > ONE_GIGABYTE)
			strSize = String.format("%.3f GB",
					(double) size / (double) ONE_GIGABYTE);
	    else if (size > ONE_MAGABYTE)
			strSize = String.format("%.3f MB",
					(double) size / (double) ONE_MAGABYTE);
		else if (size > ONE_KILOBYTE)
			strSize = String.format("%.3f kB",
					(double) size / (double) ONE_KILOBYTE);
		else
			strSize = String.format("%d Byte", size);
		return strSize;
    }
	
	private void openExe(String... params) {
		Runtime rn = Runtime.getRuntime();
		Process proc = null;
		try {
			proc = rn.exec(params);
			
			 StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "Error");            
             StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "Output");
             errorGobbler.start();
             outputGobbler.start();
             //proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("failed to run exec");
			lblInfo.setText("failed to run exec");
		}
	}
	
	class StreamGobbler extends Thread {
		InputStream is;

		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					if (type.equals("Error")) {
						System.out.println("[error] " + line);
						lblInfo.setText("[error] " + line);
					}
					else {
						System.out.println("[info] " + line);
						lblInfo.setText("[info] " + line);
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	
}