package rocks.inspectit.ocelot.core.exporter;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.proto.agent.metrics.v1.ExportMetricsServiceRequest;
import io.opencensus.proto.agent.metrics.v1.ExportMetricsServiceResponse;
import io.opencensus.proto.agent.metrics.v1.MetricsServiceGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "inspectit.exporters.metrics.open-census-agent.address=localhost:55678",
        "inspectit.exporters.metrics.open-census-agent.use-insecure=true",
})
@DirtiesContext
public class OpenCensusAgentMetricsExporterServiceIntTest extends SpringTestBase {

    public static final String HOST = "localhost";
    public static final int PORT = 55678;
    private static Server agent;
    private static FakeOcAgentMetricsServiceGrpcImpl fakeOcAgentMetricsServiceGrpc = new FakeOcAgentMetricsServiceGrpcImpl();

    @BeforeAll
    public static void setUp() {
        agent = getServer("localhost:55678" ,fakeOcAgentMetricsServiceGrpc);
    }

    @AfterAll
    public static void tearDown() {
        agent.shutdown();
    }
    /**
     * To test the client, a fake GRPC server servers the fake class implementation {@link FakeOcAgentMetricsServiceGrpcImpl}.
     */
    @Test
    public void testGrpcRequest() {
        try {
            agent.start();
        } catch ( IOException e) {
            e.printStackTrace();
        }
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(fakeOcAgentMetricsServiceGrpc.getExportMetricsServiceRequests().size()).isGreaterThan(0);
        });
    }

    private static Server getServer(String endPoint, BindableService service) {
        ServerBuilder<?> builder = NettyServerBuilder.forAddress(new InetSocketAddress(HOST, PORT));
        Executor executor = MoreExecutors.directExecutor();
        builder.executor(executor);
        return builder.addService(service).build();
    }

    static class FakeOcAgentMetricsServiceGrpcImpl extends MetricsServiceGrpc.MetricsServiceImplBase {

        @GuardedBy("this")
        private final List<ExportMetricsServiceRequest> exportMetricsServiceRequests = new ArrayList<>();

        @GuardedBy("this")
        private final StreamObserver<ExportMetricsServiceRequest> exportRequestObserver =
                new StreamObserver<ExportMetricsServiceRequest>() {
                    @Override
                    public void onNext(ExportMetricsServiceRequest value) {
                        addExportRequest(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onCompleted() {}
                };

        @Override
        public synchronized StreamObserver<ExportMetricsServiceRequest> export(
                StreamObserver<ExportMetricsServiceResponse> responseObserver) {
            return exportRequestObserver;
        }

        private synchronized void addExportRequest(ExportMetricsServiceRequest request) {
            exportMetricsServiceRequests.add(request);
        }

        synchronized List<ExportMetricsServiceRequest> getExportMetricsServiceRequests() {
            return Collections.unmodifiableList(exportMetricsServiceRequests);
        }
    }

}
