package com.osunji.melog.elk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class ElasticConfig {

	@Value("${elasticsearch.host:localhost}")
	private String host;

	@Value("${elasticsearch.port:9200}")
	private int port;

	@Value("${elasticsearch.username:melog_elk}")
	private String username;

	@Value("${elasticsearch.password:melog2598pw}")
	private String password;

	@Bean
	public RestClient restClient() {
		try {
			SSLContext sslContext = SSLContextBuilder
				.create()
				.loadTrustMaterial(null, (chains, authType) -> true)
				.build();

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
				AuthScope.ANY,
				new UsernamePasswordCredentials(username, password)
			);

			return RestClient.builder(
					new HttpHost(host, port, "https")
				)
				.setHttpClientConfigCallback(httpClientBuilder ->
					httpClientBuilder
						.setSSLContext(sslContext)
						.setSSLHostnameVerifier((hostname, session) -> true)
						.setDefaultCredentialsProvider(credentialsProvider)
				)
				.build();
		} catch (Exception e) {
			throw new RuntimeException("Elasticsearch RestClient 설정 실패", e);
		}
	}

	@Bean
	public ElasticsearchClient elasticsearchClient(RestClient restClient) {
		// 🎯 핵심: JavaTimeModule을 등록한 ObjectMapper 생성
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		RestClientTransport transport = new RestClientTransport(
			restClient,
			new JacksonJsonpMapper(objectMapper)  // 커스텀 ObjectMapper 사용
		);

		return new ElasticsearchClient(transport);
	}
}
