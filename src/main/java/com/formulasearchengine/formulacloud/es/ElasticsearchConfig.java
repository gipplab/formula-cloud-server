package com.formulasearchengine.formulacloud.es;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Andre Greiner-Petter
 */
public class ElasticsearchConfig {
    @Parameter(names = {"-esHost", "--elasticsearchHost"}, description = "Set the host of the elasticsearch server (default is localhost).")
    private String elasticsearchHost = "localhost";

    @Parameter(names = {"-esPort", "--elasticsearchPort"}, description = "Set the port of the elasticsearch server (default is 9200).")
    private int elasticsearchPort = 9200;

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help = false;

    public ElasticsearchConfig() {}

    public String getElasticsearchHost() {
        return elasticsearchHost;
    }

    public void setElasticsearchHost(String elasticsearchHost) {
        this.elasticsearchHost = elasticsearchHost;
    }

    public int getElasticsearchPort() {
        return elasticsearchPort;
    }

    public void setElasticsearchPort(int elasticsearchPort) {
        this.elasticsearchPort = elasticsearchPort;
    }

    public static ElasticsearchConfig loadConfig(String[] args) {
        ElasticsearchConfig conf = new ElasticsearchConfig();

        JCommander jCommander = JCommander.newBuilder()
                .addObject(conf)
                .build();

        jCommander.parse(args);

        if ( conf.help ){
            jCommander.usage();
            System.exit(0);
        }

        return conf;
    }
}
