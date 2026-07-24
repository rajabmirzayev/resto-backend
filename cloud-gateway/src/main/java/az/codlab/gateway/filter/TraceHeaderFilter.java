package az.codlab.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceHeaderFilter implements GlobalFilter, Ordered {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingTraceId = exchange.getRequest().getHeaders().getFirst(HEADER_TRACE_ID);
        String traceId = (incomingTraceId == null || incomingTraceId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingTraceId;
        String requestId = UUID.randomUUID().toString();

        var mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(HEADER_TRACE_ID, traceId);
                    headers.set(HEADER_REQUEST_ID, requestId);
                })
                .build();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(
                    org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer> body) {
                getHeaders().set(HEADER_TRACE_ID, traceId);
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(
                    org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer>> body) {
                getHeaders().set(HEADER_TRACE_ID, traceId);
                return super.writeAndFlushWith(body);
            }
        };

        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HEADER_TRACE_ID, traceId);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate()
                .request(mutatedRequest)
                .response(decoratedResponse)
                .build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
