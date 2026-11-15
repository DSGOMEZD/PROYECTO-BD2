package com.basesdatos2.backendBD2.config;

import com.mongodb.ConnectionString;
import com.mongodb.ServerAddress;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Configuracion de persistencia poliglota (MySQL en MV1 + MongoDB 7.0 con autenticacion en MV2)
// con connectTimeout de 10 segundos y resolucion automatica entre 127.0.0.1 (NAT local) y Tailscale.
@Configuration
public class MongoConfig {
    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);
    private static final int CONNECT_TIMEOUT_SEGUNDOS = 10;
    private static final int READ_TIMEOUT_SEGUNDOS = 15;
    private static final int SONDEO_LOCAL_MS = 250;
    private static final int SONDEO_TAILSCALE_MS = 2000;
    private static final String TAILSCALE_MYSQL_HOST = "100.70.144.4";
    private static final String TAILSCALE_MONGO_HOST = "100.70.143.38";

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientTimeoutCustomizer(
            @Value("${spring.mongodb.uri:mongodb://motogo_logger:MotoLog2026*@127.0.0.1:27017/alquiler_motos_logs?authSource=admin}") String mongoUri) {
        return builder -> {
            ConnectionString connString = new ConnectionString(mongoUri);
            builder.applyConnectionString(connString)
                    .applyToSocketSettings(socket -> socket
                            .connectTimeout(CONNECT_TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT_SEGUNDOS, TimeUnit.SECONDS))
                    .applyToClusterSettings(cluster -> {
                        cluster.serverSelectionTimeout(CONNECT_TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

                        List<String> hosts = connString.getHosts();
                        if (!hosts.isEmpty()) {
                            ServerAddress principal = parsearHost(hosts.get(0), 27017);
                            String hostConfigurado = principal.getHost();
                            int port = principal.getPort();

                            if (esLocal(hostConfigurado)) {
                                if (!esAlcanzable("127.0.0.1", port, SONDEO_LOCAL_MS)) {
                                    log.info("Puerto local MongoDB (127.0.0.1:{}) no disponible; "
                                            + "usando IP Tailscale ({}:{}) hacia MV2.", port, TAILSCALE_MONGO_HOST, port);
                                    cluster.hosts(List.of(new ServerAddress(TAILSCALE_MONGO_HOST, port)));
                                }
                            } else if (esAlcanzable("127.0.0.1", port, SONDEO_LOCAL_MS)
                                    && !esAlcanzable(hostConfigurado, port, SONDEO_TAILSCALE_MS)) {
                                log.info("IP Tailscale MongoDB ({}:{}) no alcanzable desde el host; "
                                        + "usando tunel local NAT (127.0.0.1:{}) hacia motogo-mongo (MV2).",
                                        hostConfigurado, port, port);
                                cluster.hosts(List.of(new ServerAddress("127.0.0.1", port)));
                            }
                        }
                    });
        };
    }

    @Bean
    public static BeanPostProcessor dataSourceTailscaleFallbackProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HikariDataSource hikari && hikari.getJdbcUrl() != null
                        && hikari.getJdbcUrl().startsWith("jdbc:mysql://")) {
                    hikari.setConnectionTimeout(TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT_SEGUNDOS));
                    hikari.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));
                    try {
                        URI uri = URI.create(hikari.getJdbcUrl().substring("jdbc:".length()));
                        String host = uri.getHost();
                        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                        if (host != null) {
                            if (esLocal(host) && !esAlcanzable("127.0.0.1", port, SONDEO_LOCAL_MS)) {
                                String urlTailscale = hikari.getJdbcUrl().replaceFirst(
                                        "jdbc:mysql://" + host + "(:" + port + ")?",
                                        "jdbc:mysql://" + TAILSCALE_MYSQL_HOST + ":" + port);
                                log.info("Puerto local MySQL (127.0.0.1:{}) no disponible; "
                                        + "usando IP Tailscale ({}:{}) hacia MV1.", port, TAILSCALE_MYSQL_HOST, port);
                                hikari.setJdbcUrl(urlTailscale);
                            } else if (!esLocal(host)
                                    && esAlcanzable("127.0.0.1", port, SONDEO_LOCAL_MS)
                                    && !esAlcanzable(host, port, SONDEO_TAILSCALE_MS)) {
                                String urlLocal = hikari.getJdbcUrl().replaceFirst(
                                        "jdbc:mysql://" + host + "(:" + port + ")?",
                                        "jdbc:mysql://127.0.0.1:" + port);
                                log.info("IP Tailscale MySQL ({}:{}) no alcanzable desde el host; "
                                        + "usando tunel local NAT (127.0.0.1:{}) hacia MV1.", host, port, port);
                                hikari.setJdbcUrl(urlLocal);
                            }
                        }
                    } catch (RuntimeException ignored) {
                        // Si el formato de la URL no es estandar, se mantiene intacta.
                    }
                }
                return bean;
            }
        };
    }

    private static boolean esLocal(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
    }

    private static ServerAddress parsearHost(String hostPort, int puertoPorDefecto) {
        int idx = hostPort.lastIndexOf(':');
        if (idx > 0 && idx < hostPort.length() - 1) {
            try {
                return new ServerAddress(hostPort.substring(0, idx), Integer.parseInt(hostPort.substring(idx + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return new ServerAddress(hostPort, puertoPorDefecto);
    }

    private static boolean esAlcanzable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
