package com.osunji.melog.global.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;


@Configuration
public class NaverJwksConfig {

    @Bean(name = "naverJwkSource")
    public JWKSource<SecurityContext> naverJwkSource(
            // yml 키 케밥/카멜 혼용 방지용 fallback
            @Value("${oidc.providers.naver.jwksUri}")
            String jwksUri
    ) throws MalformedURLException {

        DefaultResourceRetriever retriever = new DefaultResourceRetriever(
                5_000,    // connect timeout ms
                5_000,    // read timeout ms
                1_024_000 // max content bytes
        );


        return JWKSourceBuilder
                .create(new URL(jwksUri), retriever)
                .cache(60 * 60 * 1000L, 5 * 60 * 1000L) // TTL=1h, refresh-ahead=5m
                .retrying(true)                         // (선택) 네트워크 재시도
                .outageTolerant(4 * 60 * 60 * 1000L)    // (선택) 장애 허용 캐시 4h
                .build();
    }
}
