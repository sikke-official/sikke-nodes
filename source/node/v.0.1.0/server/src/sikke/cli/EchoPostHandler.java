/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikke.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import sikke.cli.helpers._System;

/**
 *
 * @author mumbucoglu
 */
public class EchoPostHandler implements HttpHandler {
	JsonRpc jsonrpc = new JsonRpc();

	public EchoPostHandler() {

	}

	@Override
	public void handle(HttpExchange he) throws IOException {
		try {
			Map<String, Object> parameters = new HashMap();
			InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			String query = "";
			String line;

			String hostAddress = he.getRemoteAddress().getAddress().getHostAddress();
			List<String> requestIPs = _System.getConfig("rpcallowip");
			if (!requestIPs.contains(hostAddress)) {
				he.close();
				return;
			}
			while ((line = br.readLine()) != null) {
				query += line;
			}
			Headers requestHeaders = he.getRequestHeaders();
			String requestMethod = he.getRequestMethod();
			String path = he.getRequestURI().getPath();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			if (!path.equals("/")) {
				Error error = new Error();
				error.error_code = 404;
				error.message = "Only base URL requests are allowed";
				error.status = "error";
				String c = gson.toJson(error);
				OutputStream os = he.getResponseBody();
				he.sendResponseHeaders(404, c.length());
				os.write(c.getBytes());
				os.close();
				return;
			}
			if (!requestMethod.equals(RequestMethods.POST.getUrl())) {
				Error error = new Error();
				error.error_code = 404;
				error.message = "Only POST requests are allowed via base URL";
				error.status = "error";
				String c = gson.toJson(error);
				OutputStream os = he.getResponseBody();
				he.sendResponseHeaders(404, c.length());
				os.write(c.getBytes());
				os.close();
				return;
			}
			if (query == null || query.isEmpty()) {
				Error error = new Error();
				error.error_code = 404;
				error.message = "Only base URL requests are allowed with request body";
				error.status = "error";
				String c = gson.toJson(error);
				OutputStream os = he.getResponseBody();
				he.sendResponseHeaders(404, c.length());
				os.write(c.getBytes());
				os.close();
				return;
			}
			rpcObj rpc = (rpcObj) gson.fromJson(query, rpcObj.class);
			String response = "";
			for (String key : parameters.keySet()) {
				response = response + key + " = " + parameters.get(key) + "\n";
			}
			he.sendResponseHeaders(200, response.length());
			OutputStream os = he.getResponseBody();

			rpcObj a = new rpcObj();
			a.id = rpc.id;
			a.jsonrpc = "2.0";
			a.method = rpc.method;

			List<String> list = new ArrayList(Arrays.asList(rpc.params));
			list.removeAll(Arrays.asList(new String[] { "", null }));

			a.result = this.jsonrpc.Methods(rpc.method, (String[]) list.toArray(new String[0]));
			String c = gson.toJson(a);
			os.write(c.getBytes());
			os.close();

		} catch (Exception ex) {
			// System.out.println("An error occured : " + ex.getMessage());
			Logger.getLogger(EchoPostHandler.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
		}
	}

	public static void parseQuery(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {

		if (query != null) {
			String pairs[] = query.split("[&]");
			for (String pair : pairs) {
				String param[] = pair.split("[=]");
				String key = null;
				String value = null;
				if (param.length > 0) {
					key = URLDecoder.decode(param[0], System.getProperty("file.encoding"));
				}

				if (param.length > 1) {
					value = URLDecoder.decode(param[1], System.getProperty("file.encoding"));
				}

				if (parameters.containsKey(key)) {
					Object obj = parameters.get(key);
					if (obj instanceof List<?>) {
						List<String> values = (List<String>) obj;
						values.add(value);

					} else if (obj instanceof String) {
						List<String> values = new ArrayList<String>();
						values.add((String) obj);
						values.add(value);
						parameters.put(key, values);
					}
				} else {
					parameters.put(key, value);
				}
			}
		}
	}
}

class rpcObj {
	String id;
	String jsonrpc;
	String method;
	JsonArray result;
	String[] params;
}

class Error {
	String status;
	String message;
	int error_code;
}
