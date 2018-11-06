package io.lingpai.tutor;

import static spark.Spark.get;
import static spark.Spark.port;

public class HttpServer {
    CoconutChain chain;
    public void serve(CoconutChain chain) {
        port(8888);

        //区块链当前状态
        get("/chain", (request, response) -> {
            response.type("application/json");
            int height = Integer.valueOf(request.params(":height"));
            return Utils.toJson(chain.getBlockByHeight(height));
        });


        //区块信息
        get("/blocks/:height", (request, response) -> {
            response.type("application/json");
            int height = Integer.valueOf(request.params(":height"));
            return Utils.toJson(chain.getBlockByHeight(height));
        });


        //交易信息
        get("/transactions/:hash", (request, response) -> {
            response.type("application/json");
            String transactionId = request.params(":hash");
            return Utils.toJson(chain.getTransactionByHash(transactionId));
        });


        //某个帐号的余额
        get("/balance", (request, response) -> {
            response.type("application/json");
            String publicKey = request.queryParams("key");
            return Utils.toJson(chain.getBalanceOf(publicKey));
        });
    }
}
