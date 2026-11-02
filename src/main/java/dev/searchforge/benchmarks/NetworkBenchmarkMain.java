package dev.searchforge.benchmarks;
import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.distributed.*;
import dev.searchforge.index.IndexSnapshot;
import dev.searchforge.query.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** End-to-end coordinator overhead on an otherwise idle, dedicated local cluster. */
public final class NetworkBenchmarkMain {
    private static <T> T post(HttpClient client, URI base, String path, Object body, Class<T> type) throws Exception {
        var request=HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(35)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(Protocol.JSON.writeValueAsBytes(body))).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
        if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());
        return Protocol.JSON.readValue(response.body(),type);
    }
    public static void main(String[] args) throws Exception {
        URI uri=URI.create(args[0]); var docs=BenchmarkMain.corpus(10000); var analyzer=new Analyzer(); var index=new IndexSnapshot(docs,analyzer,1);
        try(var client=HttpClient.newHttpClient()) {
            for(int from=0;from<docs.size();from+=600) {
                var result=post(client,uri,"/ingest",new Protocol.Batch(docs.subList(from,Math.min(from+600,docs.size())),Set.of()),Map.class);
                if(!"COMPLETE".equals(result.get("status")))throw new IllegalStateException("Incomplete ingestion");
            }
            String[] queries={"common","t511","t200 t300","t1 AND t400","\"common search\"","t17 OR t99","absent"};
            for(int i=0;i<35;i++) post(client,uri,"/search",new Protocol.QueryRequest(queries[i%7],0,10,5000),Protocol.DistributedResult.class);
            var trials=new ArrayList<Object>();
            for(int trial=0;trial<3;trial++) {
                var local=new ArrayList<Long>();var distributed=new ArrayList<Long>();
                for(int i=0;i<140;i++) {
                    String text=queries[i%7];var query=Query.parse(text,analyzer);
                    long start=System.nanoTime();var expected=Search.execute(index,query,0,10);local.add(System.nanoTime()-start);
                    start=System.nanoTime();var actual=post(client,uri,"/search",new Protocol.QueryRequest(text,0,10,5000),Protocol.DistributedResult.class);distributed.add(System.nanoTime()-start);
                    if(!actual.status().equals("COMPLETE") || actual.totalHits()!=expected.totalHits()
                            || !actual.hits().stream().map(Search.Hit::id).toList().equals(expected.hits().stream().map(Search.Hit::id).toList()))throw new AssertionError("Distributed benchmark equivalence failed");
                    for(int j=0;j<expected.hits().size();j++) if(Math.abs(expected.hits().get(j).score()-actual.hits().get(j).score())>1e-12)throw new AssertionError("Score mismatch");
                }
                trials.add(Map.of("localNanos",local,"distributedNanos",distributed));
            }
            Protocol.JSON.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(),Map.of("documents",10000,"shards",3,"warmupQueries",35,"samplesPerTrial",140,"trials",trials,
                    "scope","local parsed query vs full HTTP statistics/scoring/merge round trip; sequential calls on same host", "java",System.getProperty("java.runtime.version")));
            System.out.println("network_benchmark_complete");
        }
    }
}
