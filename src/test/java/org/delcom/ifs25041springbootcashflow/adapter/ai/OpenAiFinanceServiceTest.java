package org.delcom.ifs25041springbootcashflow.adapter.ai;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiFinanceServiceTest {

    private HttpServer server;
    private String serverUrl;
    private String mockResponseBody;
    private int mockResponseCode;
    private String lastAuthorization;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = mockResponseBody != null ? mockResponseBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(mockResponseCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();
        mockResponseCode = 200;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Membungkus teks konten AI menjadi respon chat completion yang valid (JSON di-escape). */
    private static String chat(String content) {
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
    }

    private OpenAiFinanceService service() {
        return new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
    }

    @Test
    void generatesTransactionsAndSendsBearerKey() throws Exception {
        mockResponseBody = chat("[{\"description\":\"Gaji\",\"amount\":5000000,\"type\":\"INCOME\"}]");

        List<AiGeneratedTransaction> result = service().generateTransactions("gajian 5 juta");

        assertEquals(1, result.size());
        assertEquals("Gaji", result.get(0).description());
        assertEquals(5_000_000L, result.get(0).amount());
        assertEquals(TransactionType.INCOME, result.get(0).type());
        assertEquals("Bearer valid-key", lastAuthorization);
    }

    @Test
    void toleratesStringAmountsIndonesianTypesAndSkipsInvalidItems() throws Exception {
        mockResponseBody = chat("""
                [
                  {"description":"Makan","amount":"25000","type":"pengeluaran"},
                  {"description":"Kopi","amount":15000.5,"type":"EXPENSE"},
                  {"amount":100,"type":"INCOME"},
                  {"description":null,"amount":100,"type":"INCOME"},
                  {"description":"Tanpa tipe","amount":100},
                  {"description":"Tipe aneh","amount":100,"type":"transfer"},
                  {"description":"Nol","amount":0,"type":"INCOME"},
                  {"description":"Negatif","amount":-5,"type":"INCOME"},
                  {"description":"Tanpa jumlah","type":"INCOME"},
                  {"description":"Bukan angka","amount":"abc","type":"INCOME"},
                  {"description":"Melebihi long","amount":"9999999999999999999","type":"INCOME"},
                  {"description":"Terlalu besar","amount":"99999999999999999999","type":"INCOME"},
                  {"description":"Eksponen ekstrem","amount":"1E999999999","type":"INCOME"}
                ]
                """);

        List<AiGeneratedTransaction> result = service().generateTransactions("catatan");

        assertEquals(2, result.size());
        assertEquals("Makan", result.get(0).description());
        assertEquals(25_000L, result.get(0).amount());
        assertEquals(TransactionType.EXPENSE, result.get(0).type());
        assertEquals("Kopi", result.get(1).description());
        assertEquals(15_001L, result.get(1).amount());
    }

    @Test
    void handlesMarkdownFencesTrailingSlashAndFallbackModel() throws Exception {
        mockResponseBody = chat("```json\n[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]\n```");
        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/", "valid-key", "   ");
        assertEquals(1, service.generateTransactions("x").size());
    }

    @Test
    void handlesFenceWithoutNewlineAndWithoutClosingFence() throws Exception {
        mockResponseBody = chat("```[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]");
        assertEquals(1, service().generateTransactions("x").size());

        mockResponseBody = chat("```\n[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]");
        assertEquals(1, service().generateTransactions("x").size());

        mockResponseBody = chat("```[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]```");
        assertEquals(1, service().generateTransactions("x").size());
    }

    @Test
    void acceptsEndpointAlreadyEndingWithChatCompletions() throws Exception {
        mockResponseBody = chat("[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]");
        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/chat/completions", "valid-key", null);
        assertEquals(1, service.generateTransactions("x").size());
    }

    @Test
    void rejectsInvalidInputs() {
        OpenAiFinanceService ok = new OpenAiFinanceService("http://localhost", "valid-key", "model");
        assertThrows(ValidationException.class, () -> ok.generateTransactions(null));
        assertThrows(ValidationException.class, () -> ok.generateTransactions("   "));
        assertThrows(ValidationException.class,
                () -> new OpenAiFinanceService(null, "k", "m").generateTransactions("p"));
        assertThrows(ValidationException.class,
                () -> new OpenAiFinanceService("   ", "k", "m").generateTransactions("p"));
        assertThrows(ValidationException.class,
                () -> new OpenAiFinanceService("http://localhost", null, "m").generateTransactions("p"));
        assertThrows(ValidationException.class,
                () -> new OpenAiFinanceService("http://localhost", "   ", "m").generateTransactions("p"));
    }

    @Test
    void reportsHttpErrorsAndAbbreviatesLongBodies() {
        mockResponseCode = 500;
        mockResponseBody = "Internal Server Error";
        DomainException shortError = assertThrows(DomainException.class, () -> service().generateTransactions("x"));
        assertTrue(shortError.getMessage().contains("500"));
        assertTrue(shortError.getMessage().contains("Internal Server Error"));

        mockResponseBody = "x".repeat(500);
        DomainException longError = assertThrows(DomainException.class, () -> service().generateTransactions("x"));
        assertTrue(longError.getMessage().endsWith("..."));
        assertTrue(longError.getMessage().length() < 300);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsHttpErrorWithNullBody() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(502);
        when(response.body()).thenReturn(null);
        doReturn(response).when(client).send(any(HttpRequest.class), any());

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1", "k", "m", client, new ObjectMapper());
        DomainException ex = assertThrows(DomainException.class, () -> service.generateTransactions("x"));
        assertTrue(ex.getMessage().contains("502"));
    }

    @Test
    void rejectsMissingChoicesOrContent() {
        OpenAiFinanceService service = service();
        for (String body : List.of("{}", "{\"choices\": []}", "{\"choices\": [{}]}", "{\"choices\": [{\"message\": {}}]}")) {
            mockResponseBody = body;
            assertThrows(DomainException.class, () -> service.generateTransactions("x"));
        }
    }

    @Test
    void rejectsNonArrayEmptyResultAndBrokenJson() {
        OpenAiFinanceService service = service();

        mockResponseBody = chat("{\"single\": 1}");
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));

        mockResponseBody = chat("[{\"description\":\"\",\"amount\":1,\"type\":\"INCOME\"}]");
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));

        mockResponseBody = chat("invalid json syntax");
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));
    }

    @Test
    void wrapsClientFailures() throws Exception {
        HttpClient client = mock(HttpClient.class);
        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "k", "m", client, new ObjectMapper());

        when(client.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection refused"));
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));

        when(client.send(any(HttpRequest.class), any())).thenThrow(new InterruptedException("Interrupted"));
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));
        assertTrue(Thread.interrupted()); // flag interrupt dipulihkan oleh service; bersihkan agar tidak bocor ke test lain
    }

    @Test
    void wrapsSerializationFailure() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("gagal serialisasi"));

        OpenAiFinanceService service = new OpenAiFinanceService(
                "http://localhost:1234", "k", "m", HttpClient.newHttpClient(), mapper);
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));
    }
}
