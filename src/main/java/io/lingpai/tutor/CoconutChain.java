package io.lingpai.tutor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class CoconutChain {
    //保存所有区块
    public ArrayList<Block> blockchain = new ArrayList<>();


    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<>(); //key是output的id，保存所有未花销的output
    //创世交易
    public static Transaction genesisTransaction;

    //当前的挖矿难度，难度可以调整
    private  int difficulty = 5;

    static {
        //必须设置才能使用BouncyCastle的椭圆算法来签名
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    //挖矿得到的金额从这个地址发出
    public static Wallet coinbase = new Wallet();

    @JsonIgnore
    Wallet miningWallet; //正在挖矿的钱包，用来纪录挖矿所得


    private  Boolean isValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');

        //验证创世块

        //保存当前区块高度上的所有的未花费 output

        //保存当前区块之前的所有UTXO，用来验证当前区块的input
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>();
        //创世块的交易只有一个
        genesisTransaction = blockchain.get(0).getTransactions().get(0);
        tempUTXOs.put(genesisTransaction.getOutputs().get(0).id, genesisTransaction.getOutputs().get(0));

        //遍历整个链来验证每个块的hash
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //块本身的数据不合法
            if(!currentBlock.isValid() ){
                log.error("block: {} is invalid", i);
                return false;
            }

            //前一个区块的不合法
            if(!previousBlock.getHash().equals(currentBlock.getPreviousHash()) ) {
                log.error("block: {} is invalid", i-1);
                return false;
            }

            //检查hash是挖矿出来的
            if(!currentBlock.getHash().substring(0, difficulty).equals(hashTarget)) {
                log.error("This block: {} hasn't been mined", currentBlock.getHash());
                return false;
            }

            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    log.error("#Signature on Transaction( {} ) is Invalid", t);
                    return false;
                }
             /*
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    log.error("#Inputs are not equal to outputs on Transaction(  {} )", t);
                    return false;
                }*/

                for(TransactionInput input: currentTransaction.getInputs()) {
                    tempOutput = tempUTXOs.get(input.getTransactionOutputId());

                    if(tempOutput == null) {
                        log.error("#Referenced input on Transaction({}) is Missing",t);
                        return false;
                    }

                    if(input.getUTXO().value != tempOutput.value) {
                        log.error("#Referenced input Transaction( {}) value is Invalid",t);
                        return false;
                    }

                    tempUTXOs.remove(input.getTransactionOutputId());
                }

                for(TransactionOutput output: currentTransaction.getOutputs()) {
                    tempUTXOs.put(output.id, output);
                }

                //验证转出地址
                if (currentTransaction.sender.equals(coinbase.publicKey)) {
                    if( ! Utils.isSameKey(currentTransaction.getOutputs().get(0).recipient, currentTransaction.recipient)) {
                        log.error("#Transaction(" + t + ") output recipient is not who it should be");
                        return false;
                    }
                } else {
                    //非挖矿交易，，第一个output是原地址 第二个output是转出地址
                    if( ! Utils.isSameKey(currentTransaction.getOutputs().get(0).recipient, currentTransaction.sender)) {
                        log.error("#Transaction(" + t + ") output 'change' is not sender");
                        return false;
                    }


                    if( ! Utils.isSameKey(currentTransaction.getOutputs().get(1).recipient, currentTransaction.recipient)) {
                        log.error("#Transaction(" + t + ") output recipient is not who it should be");
                        return false;
                    }
                }

            }
        }

        return true;
    }

    //把交易打包到区块中，并且加到链上
    public void mineBlock(List<Transaction> transactions) throws JsonProcessingException {
        int height = blockchain.size();
        //先构造区块，然后再挖矿，比特币的流程也是这样
        String previouesHash = "0"; //创世块没有前一个块的哈希，用0代替
        if (blockchain.size() > 0) {
            previouesHash = blockchain.get(height - 1).getHash();
        }

        Block block = new Block(height,previouesHash, difficulty);
        for(Transaction t : transactions) {
            block.addTransaction(t);
        }

        //系统奖励挖矿人100个币，也就是从coinbase钱包发送到我们自己的钱包
        Transaction rewardTransaction = new Transaction(coinbase.publicKey, miningWallet.publicKey, 100f);
        rewardTransaction.generateSignature(coinbase.privateKey);


        block.addTransaction(rewardTransaction);

        block.mine(difficulty);

        //挖矿成功后，更新链上的UTXO
        for (Transaction t : block.getTransactions()) {
            //交易的每笔output都是unspent的
            for(TransactionOutput o : t.getOutputs()) {
                CoconutChain.UTXOs.put(o.id , o);
            }

            //input关联的output需要从UTXO中删除，因为已经花费出去了
            for(TransactionInput i : t.getInputs()) {
                if(i.getUTXO() != null) {
                    CoconutChain.UTXOs.remove(i.getUTXO().id);
                }
            }
        }


        log.info("block: {} has been mined at height: {}, json: {}", block.getHash(), block.getHeight(), Utils.toJson(block));
        //必须先挖矿成功，才能添加到区块中
        blockchain.add(block);
    }

    public void increaseDifficulty() {
        this.difficulty ++;
    }

    public Wallet getMiningWallet() {
        return miningWallet;
    }

    public void setMiningWallet(Wallet wallet) {
        this.miningWallet = wallet;
    }


    public static void main(String[] args) throws JsonProcessingException {
        log.info("Coconut chain starting...");
        log.info("coinbase: {}", Utils.getStringFromKey(coinbase.publicKey));

        CoconutChain cocoChain = new CoconutChain();

        //模拟两个钱包
        Wallet walletA = new Wallet();
        Wallet walletB = new Wallet();

        //设置用A用户来挖矿，这样挖矿的奖励会发送A的钱包中
        cocoChain.setMiningWallet(walletA);
        log.info("mining wallet: {}", Utils.getStringFromKey(cocoChain.miningWallet.publicKey));

        //创世块，没有真正的交易
        cocoChain.mineBlock(new ArrayList<>());
        //检查出块后，整条链的合法性
        if (!cocoChain.isValid()) {
            log.info("chain status is invalid");
            return;
        }


        //在第2个块里生成一个交易，把币从我们的钱包转给B
        log.info("WalletA's balance is: {}, after genesis block  was mined", cocoChain.getMiningWallet().getBalance());

        Transaction transaction = walletA.sendFunds(walletB.publicKey, 40f);

        cocoChain.mineBlock(Arrays.asList(transaction));
        if (!cocoChain.isValid()) {
            log.info("chain status is invalid");
            return;
        }

        //挖矿成功后，再查看余额
        log.info("WalletA's balance is: {} after sending 40 coin to wallet B", walletA.getBalance());
        log.info("WalletB's balance is: {} after received 40 coin from wallet A",  walletB.getBalance());

        transaction = (walletA.sendFunds(walletB.publicKey, 1000f));
        cocoChain.mineBlock(Arrays.asList(transaction));
        //因为A的余额不足，所以转帐不成功，B的余额应该不变
        log.info("WalletA's balance is: " + walletA.getBalance());
        log.info("WalletB's balance is: " + walletB.getBalance());


        //钱包B转帐给A
        log.info("WalletB is Attempting to send funds (20) to WalletA...");
        transaction = walletB.sendFunds( walletA.publicKey, 20);

        cocoChain.mineBlock(Arrays.asList(transaction));
        if (!cocoChain.isValid()) {
            log.info("chain status is invalid");
            return;
        }

        log.info("WalletA's balance is: " + walletA.getBalance());
        log.info("WalletB's balance is: " + walletB.getBalance());



        log.info("chain details: " + Utils.toJson(cocoChain));
    }

}
