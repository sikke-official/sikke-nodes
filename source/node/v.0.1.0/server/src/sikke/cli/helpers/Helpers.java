/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikke.cli.helpers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;

import sikke.SikkeEnumContainer;

/**
 *
 * @author mumbucoglu
 */
public class Helpers {

	_System system = new _System();
	Gson g = new Gson();
	private final String USER_AGENT = "Mozilla/5.0";

	public String getBasicAuth(String Code) {
		try {
			String[] parts = Code.split(" ");
			String cd = parts[1];

			if (cd.substring(cd.length() - 1).equals("]")) {
				cd = cd.substring(0, cd.length() - 1);
				return cd;
			} else {
				return cd.substring(cd.length() - 1);
			}

		} catch (IndexOutOfBoundsException e) {
			return "err";
		}

	}

	public Boolean createTable(String sql) {
		_System system = new _System();
		String dbUrl = system.getDB();
		try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
			stmt.execute(sql);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	public String sendPost(String path, String urlParameters, String requestType) throws Exception {
		String url = getServerUrl();
		url += path;
		StringBuilder response = new StringBuilder();
		URL obj = new URL(url);

		try {
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod(
					requestType == null ? SikkeEnumContainer.HTTPRequestMethod.POST.getRequest() : requestType);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("Accept-Language", USER_AGENT);
			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();

			int responseCode = con.getResponseCode();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			_System.isAPIReachable = true;
			return response.toString();
		} catch (UnknownHostException e) {
			_System.isAPIReachable = false;
			throw e;
		} catch (Exception e) {
			_System.isAPIReachable = false;
			e.printStackTrace();
			throw e;
		}
	}

	public String sendGet(String path, String getQuery, String accessToken) throws Exception {
		String url = getServerUrl();
		url += path + getQuery;

		URL obj = new URL(url);
		try {
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			if (accessToken != null) {
				con.setRequestProperty("Authorization", "Bearer " + accessToken);
			}
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			_System.isAPIReachable = true;
			return response.toString();
		} catch (UnknownHostException e) {
			_System.isAPIReachable = false;
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			_System.isAPIReachable = false;
			throw e;
		}
	}

	public String getServerUrl() throws FileNotFoundException, IOException {
		String server = system.getConfig("server").get(0);
		String url = server.equals("1") ? "http://api.sikke.network" : "http://testnet.sikke.network";
		return url;
	}

	private static SSLSocketFactory createSslSocketFactory() throws Exception {
		TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}
		} };
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, byPassTrustManagers, new SecureRandom());
		return sslContext.getSocketFactory();
	}
}
